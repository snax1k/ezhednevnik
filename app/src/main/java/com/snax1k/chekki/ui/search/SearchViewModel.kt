package com.snax1k.chekki.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.SearchableTask
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.data.matchTask
import com.snax1k.chekki.data.searchWords
import com.snax1k.chekki.ui.addtask.CategoryOption
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.task.taskGroupOf
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.tasks.toTaskUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Фильтр по категории. */
sealed interface CategoryFilter {
    data object Any : CategoryFilter
    data object None : CategoryFilter
    data class Of(val id: Long) : CategoryFilter
}

/** Плашки-фильтры поиска. «Невыполненные» включены сразу, как в макете. */
data class SearchFilters(
    val openOnly: Boolean = true,
    val category: CategoryFilter = CategoryFilter.Any,
    val withReminder: Boolean = false,
    val highPriority: Boolean = false
) {
    /** Сужает ли что-то список кроме «Невыполненных»: тогда результаты видны и без текста запроса. */
    val narrows: Boolean get() = category != CategoryFilter.Any || withReminder || highPriority
}

/** Найденные задачи одной группы под цветным подзаголовком. */
data class SearchSection(val group: TaskGroup, val tasks: List<TaskUi>)

/** Что показывает поиск. */
data class SearchUiState(
    val today: LocalDate,
    val filters: SearchFilters,
    val sections: List<SearchSection>,
    /** Сколько всего нашлось (показаны первые, см. [canShowMore]). */
    val total: Int,
    val canShowMore: Boolean,
    /** Категории для меню фильтра (без архивных). */
    val categories: List<CategoryOption>,
    /** Запрос пуст и ничего не сужает: результатов нет, только подсказка. */
    val isIdle: Boolean,
    val isLoading: Boolean = false
)

/**
 * Поиск задач: в названии, заметке и подзадачах, без учёта регистра и ё/е (`data/TaskSearch.kt`).
 * Результаты по группам, как на главном экране, порциями по [PAGE_SIZE]. Отмеченная на экране задача
 * остаётся в результатах, пока экран открыт, даже при включённых «Невыполненных».
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    // Состояние Compose, а не StateFlow: поле ввода обновляется сразу, и курсор не прыгает при наборе
    var query by mutableStateOf("")
        private set

    private val filters = MutableStateFlow(SearchFilters())
    private val toggledIds = MutableStateFlow<Set<Long>>(emptySet())
    private val limit = MutableStateFlow(PAGE_SIZE)

    /** Запрос с задержкой: ищем, когда пауза после последней буквы — 0,2 секунды. */
    private val debouncedQuery = snapshotFlow { query }.debounce(DEBOUNCE_MILLIS).distinctUntilChanged()

    private val categories = repository.observeCategories().map { list ->
        list.filter { !it.isArchived }
            .map { CategoryOption(id = it.id, name = it.name, colorIndex = it.colorIndex, icon = it.icon) }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        combine(debouncedQuery, filters, toggledIds, limit, ::SearchParams),
        repository.observeSearchableTasks(),
        currentDate,
        categories
    ) { params, tasks, today, categoryOptions -> search(params, tasks, today, categoryOptions) }
        // Тысячи задач после переноса — считаем не в главном потоке
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState(
                today = LocalDate.now(),
                filters = SearchFilters(),
                sections = emptyList(),
                total = 0,
                canShowMore = false,
                categories = emptyList(),
                isIdle = true,
                isLoading = true
            )
        )

    private data class SearchParams(
        val query: String,
        val filters: SearchFilters,
        val keepIds: Set<Long>,
        val limit: Int
    )

    private fun search(
        params: SearchParams,
        tasks: List<SearchableTask>,
        today: LocalDate,
        categoryOptions: List<CategoryOption>
    ): SearchUiState {
        val words = searchWords(params.query)
        val filters = params.filters
        val isIdle = words.isEmpty() && !filters.narrows
        if (isIdle) {
            return SearchUiState(today, filters, emptyList(), 0, false, categoryOptions, isIdle = true)
        }
        val found = tasks.mapNotNull { searchable ->
            val task = searchable.task
            if (filters.openOnly && task.isDone && task.id !in params.keepIds) return@mapNotNull null
            if (filters.withReminder && task.reminderMinutesBefore == null) return@mapNotNull null
            if (filters.highPriority && task.priority != TaskPriority.High) return@mapNotNull null
            when (val category = filters.category) {
                CategoryFilter.Any -> Unit
                CategoryFilter.None -> if (task.categoryId != null) return@mapNotNull null
                is CategoryFilter.Of -> if (task.categoryId != category.id) return@mapNotNull null
            }
            val match = matchTask(words, task.title, task.note, searchable.subtasks) ?: return@mapNotNull null
            searchable to match
        }
        val ordered = found
            .groupBy { (searchable, _) -> taskGroupOf(searchable.task.date, searchable.task.isDone, today) }
            .toSortedMap(compareBy { GroupOrder.indexOf(it) })
            .flatMap { (group, items) -> items.sortedWith(orderIn(group)).map { group to it } }
        val shown = ordered.take(params.limit)
        val sections = shown
            .groupBy({ it.first }) { (group, item) ->
                val (searchable, match) = item
                searchable.details.toTaskUi().copy(
                    // В «Сегодня» дата и так понятна
                    showDate = group != TaskGroup.Today,
                    titleHighlight = match.titleRange,
                    matchSnippet = match.snippet
                )
            }
            .map { (group, groupTasks) -> SearchSection(group, groupTasks) }
        return SearchUiState(
            today = today,
            filters = filters,
            sections = sections,
            total = ordered.size,
            canShowMore = ordered.size > shown.size,
            categories = categoryOptions,
            isIdle = false
        )
    }

    fun onQueryChange(text: String) {
        query = text
        limit.value = PAGE_SIZE
    }

    fun onFiltersChange(newFilters: SearchFilters) {
        filters.value = newFilters
        limit.value = PAGE_SIZE
    }

    fun showMore() {
        limit.update { it + PAGE_SIZE }
    }

    fun toggleDone(taskId: Long) {
        toggledIds.update { it + taskId }
        viewModelScope.launch { repository.toggleDone(taskId) }
    }

    /** Свайп «Удалить»: удаляет задачу и передаёт в [onDeleted] всё, что нужно для отмены. */
    fun delete(taskId: Long, onDeleted: (DeletedTask) -> Unit) {
        viewModelScope.launch { repository.deleteTask(taskId)?.let(onDeleted) }
    }

    /** Свайп «Отложить»: переносит задачу на [date] и передаёт в [onPostponed] прежнюю дату для отмены. */
    fun postpone(taskId: Long, date: LocalDate, onPostponed: (MovedTask) -> Unit) {
        viewModelScope.launch { repository.moveTask(taskId, date)?.let(onPostponed) }
    }

    private companion object {
        /** Сколько результатов показывать за раз: запрос из одной буквы после переноса найдёт тысячи. */
        const val PAGE_SIZE = 100
        const val DEBOUNCE_MILLIS = 200L

        /** Порядок групп в результатах — как на главном экране, выполненное в конце. */
        val GroupOrder = listOf(
            TaskGroup.Overdue,
            TaskGroup.Today,
            TaskGroup.Tomorrow,
            TaskGroup.ThisWeek,
            TaskGroup.ThisMonth,
            TaskGroup.Later,
            TaskGroup.NoDate,
            TaskGroup.Done
        )

        /** Порядок внутри группы: выполненные — свежие сверху, без даты — новые сверху, остальные — по дате и времени. */
        fun orderIn(group: TaskGroup): Comparator<Pair<SearchableTask, Any>> = when (group) {
            TaskGroup.Done -> compareByDescending { it.first.task.completedAt }
            TaskGroup.NoDate -> compareByDescending { it.first.task.createdAt }
            else -> compareBy<Pair<SearchableTask, Any>> { it.first.task.date }
                .thenBy { it.first.task.time == null }
                .thenBy { it.first.task.time }
                .thenBy { it.first.task.createdAt }
        }
    }
}
