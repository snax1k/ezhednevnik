package com.snax1k.chekki.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Что показывает экран группы. */
data class GroupUiState(
    val group: TaskGroup,
    /** Сегодняшняя дата: от неё считаются даты группы в шапке. */
    val today: LocalDate,
    val tasks: List<TaskUi>,
    /** База ещё не ответила: счётчик и пустой список пока не показываем. */
    val isLoading: Boolean = false
)

/**
 * Задачи одной группы из базы. При смене даты в полночь список пересчитывается.
 *
 * В группах, кроме «Сегодня», показываются только невыполненные задачи. Чтобы отмеченная задача
 * не пропадала из-под пальца, её id запоминается: она остаётся в списке зачёркнутой, пока экран открыт.
 */
class GroupViewModel(
    private val group: TaskGroup,
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    private val toggledIds = MutableStateFlow<Set<Long>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GroupUiState> = combine(currentDate, toggledIds, ::Pair)
        .flatMapLatest { (today, keepIds) ->
            tasksOf(today, keepIds).map { tasks -> GroupUiState(group, today, tasks.map { it.toTaskUi() }) }
        }
        .stateIn(
            scope = viewModelScope,
            // Пока экран свёрнут, базу не слушаем; 5 секунд запаса, чтобы не перезапускаться при повороте
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupUiState(group, today = LocalDate.now(), tasks = emptyList(), isLoading = true)
        )

    private fun tasksOf(today: LocalDate, keepIds: Set<Long>): Flow<List<TaskWithDetails>> = when (group) {
        TaskGroup.Today -> repository.observeTasksOn(today)
        TaskGroup.Tomorrow -> today.plusDays(1).let { repository.observeOpenTasksBetween(it..it, keepIds) }
        TaskGroup.NoDate -> repository.observeOpenTasksWithoutDate(keepIds)
        // Группы на несколько дней и «Выполненное» — следующий шаг, с главного экрана они пока не открываются
        TaskGroup.Overdue, TaskGroup.ThisWeek, TaskGroup.ThisMonth, TaskGroup.Later, TaskGroup.Done ->
            flowOf(emptyList())
    }

    fun toggleDone(taskId: Long) {
        toggledIds.update { it + taskId }
        viewModelScope.launch { repository.toggleDone(taskId) }
    }
}
