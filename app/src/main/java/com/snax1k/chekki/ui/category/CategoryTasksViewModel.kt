package com.snax1k.chekki.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.ui.group.TaskSection
import com.snax1k.chekki.ui.group.sectionsByPriorityOrDay
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.tasks.toTaskUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Что показывает экран задач категории. */
data class CategoryTasksUiState(
    /** null — экран «Без категории». */
    val categoryId: Long?,
    /** null у «Без категории», а также пока база не ответила или если категорию удалили. */
    val category: CategoryEntity?,
    val today: LocalDate,
    /** Задачи по разделам приоритета или, если приоритетов нет, по дням: просроченные, сегодня, дальше, в конце — «Без даты». */
    val sections: List<TaskSection>,
    val isLoading: Boolean = false
) {
    val tasks: List<TaskUi> get() = sections.flatMap { it.tasks }

    /** Категорию удалили, пока экран был открыт: его нужно закрыть. */
    val isCategoryMissing: Boolean get() = !isLoading && categoryId != null && category == null
}

/**
 * Невыполненные задачи одной категории ([categoryId] null — задачи без категории). Как и в группах,
 * отмеченная на экране задача остаётся в списке, пока экран открыт.
 */
class CategoryTasksViewModel(
    private val categoryId: Long?,
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    private val toggledIds = MutableStateFlow<Set<Long>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoryTasksUiState> = combine(currentDate, toggledIds, ::Pair)
        .flatMapLatest { (today, keepIds) ->
            val category = if (categoryId == null) flowOf(null) else repository.observeCategory(categoryId)
            combine(category, repository.observeOpenTasksOfCategory(categoryId, keepIds)) { entity, tasks ->
                CategoryTasksUiState(
                    categoryId = categoryId,
                    category = entity,
                    today = today,
                    sections = sectionsByPriorityOrDay(tasks.map { it.task.date to it.toTaskUi() }, noDateLast = true)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryTasksUiState(categoryId, null, LocalDate.now(), emptyList(), isLoading = true)
        )

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
}
