package com.snax1k.chekki.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.GroupRanges
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.TaskWithDetails
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.tasks.toTaskUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Что показывает экран группы. */
data class GroupUiState(
    val group: TaskGroup,
    /** Сегодняшняя дата: от неё считаются даты группы в шапке и подзаголовки дней. */
    val today: LocalDate,
    /** Задачи по дням. У групп на один день и «Без даты» одна секция без подзаголовка. */
    val sections: List<TaskSection>,
    /** В «Выполненном» есть задачи сверх показанных. */
    val canShowMore: Boolean = false,
    /** База ещё не ответила: счётчик и пустой список пока не показываем. */
    val isLoading: Boolean = false
) {
    val tasks: List<TaskUi> get() = sections.flatMap { it.tasks }
}

/**
 * Задачи одной группы из базы. При смене даты в полночь список пересчитывается.
 *
 * В группах, кроме «Сегодня», показываются только невыполненные задачи, а в «Выполненном» — только
 * выполненные. Чтобы задача не пропадала из-под пальца, id отмеченных на экране задач запоминается:
 * они остаются в списке, пока экран открыт.
 */
class GroupViewModel(
    private val group: TaskGroup,
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    private val toggledIds = MutableStateFlow<Set<Long>>(emptySet())
    private val doneLimit = MutableStateFlow(DONE_PAGE_SIZE)

    /**
     * Когда задачи были выполнены. Если с задачи сняли отметку на экране «Выполненное», база стирает
     * время выполнения, а задача должна остаться на своём месте.
     */
    private val knownCompletedAt = mutableMapOf<Long, Instant>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GroupUiState> = combine(currentDate, toggledIds, doneLimit, ::Triple)
        .flatMapLatest { (today, keepIds, limit) -> stateOf(today, keepIds, limit) }
        .stateIn(
            scope = viewModelScope,
            // Пока экран свёрнут, базу не слушаем; 5 секунд запаса, чтобы не перезапускаться при повороте
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupUiState(group, today = LocalDate.now(), sections = emptyList(), isLoading = true)
        )

    private fun stateOf(today: LocalDate, keepIds: Set<Long>, limit: Int): Flow<GroupUiState> {
        val ranges = GroupRanges.of(today)
        fun oneSection(tasks: Flow<List<TaskWithDetails>>) = tasks.map { list ->
            val section = TaskSection(day = null, tasks = list.map { it.toTaskUi() })
            GroupUiState(group, today, sections = if (list.isEmpty()) emptyList() else listOf(section))
        }
        fun byTaskDate(tasks: Flow<List<TaskWithDetails>>) = tasks.map { list ->
            GroupUiState(group, today, sections = sectionsByPriorityOrDay(list.map { it.task.date to it.toTaskUi() }))
        }
        return when (group) {
            // Выполненные уезжают в свёрнутый раздел внизу: там же всё, что выполнено сегодня из других дней
            TaskGroup.Today -> combine(repository.observeTasksOn(today), repository.observeDoneToday(today)) { all, done ->
                val open = all.filter { !it.task.isDone }.map { it.toTaskUi() }
                val sections = buildList {
                    if (open.isNotEmpty()) add(TaskSection(day = null, tasks = open))
                    // У задач других дней — дата в строке, чтобы было видно, откуда задача
                    val doneUi = done.map { it.toTaskUi().run { if (date != today) copy(showDate = true) else this } }
                    if (doneUi.isNotEmpty()) add(TaskSection(day = null, tasks = doneUi, isDone = true))
                }
                GroupUiState(group, today, sections = sections)
            }
            TaskGroup.Tomorrow -> oneSection(repository.observeOpenTasksBetween(ranges.tomorrow..ranges.tomorrow, keepIds))
            TaskGroup.NoDate -> oneSection(repository.observeOpenTasksWithoutDate(keepIds))
            TaskGroup.Overdue -> byTaskDate(repository.observeOpenTasksBetween(LocalDate.MIN..today.minusDays(1), keepIds))
            TaskGroup.ThisWeek -> byTaskDate(repository.observeOpenTasksBetween(ranges.thisWeek, keepIds))
            TaskGroup.ThisMonth -> byTaskDate(repository.observeOpenTasksBetween(ranges.thisMonth, keepIds))
            TaskGroup.Later -> byTaskDate(repository.observeOpenTasksBetween(ranges.laterFrom..LocalDate.MAX, keepIds))
            TaskGroup.Done -> combine(
                repository.observeDoneTasks(limit, keepIds),
                repository.observeDoneCount()
            ) { list, doneCount ->
                GroupUiState(group, today, sections = doneSections(list), canShowMore = doneCount > limit)
            }
        }
    }

    /** «Выполненное»: свежие сверху, подзаголовки — по дню выполнения в часовом поясе телефона. */
    private fun doneSections(tasks: List<TaskWithDetails>): List<TaskSection> {
        tasks.forEach { details -> details.task.completedAt?.let { knownCompletedAt[details.task.id] = it } }
        val zone = ZoneId.systemDefault()
        return tasks
            .map { details -> (details.task.completedAt ?: knownCompletedAt[details.task.id]) to details }
            .sortedWith(compareByDescending<Pair<Instant?, TaskWithDetails>> { it.first }.thenByDescending { it.second.task.id })
            .map { (completedAt, details) -> completedAt?.atZone(zone)?.toLocalDate() to details.toTaskUi() }
            .let(::sectionsByDay)
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

    fun showMore() {
        doneLimit.update { it + DONE_PAGE_SIZE }
    }

    private companion object {
        /** Сколько выполненных задач показывать за раз: после переноса из старого приложения их тысячи. */
        const val DONE_PAGE_SIZE = 50
    }
}
