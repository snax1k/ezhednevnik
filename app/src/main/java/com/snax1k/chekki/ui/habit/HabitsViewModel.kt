package com.snax1k.chekki.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedHabit
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.db.HabitListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Что показывает экран «Все привычки». */
data class HabitsUiState(
    /** Действующие привычки в порядке кругов наверху главного экрана. */
    val active: List<HabitListItem>,
    val archived: List<HabitListItem>,
    /** База ещё не ответила: пустой список пока не показываем. */
    val isLoading: Boolean = false
)

/** Экран «Все привычки»: порядок, архив, удаление. */
class HabitsViewModel(private val repository: HabitRepository) : ViewModel() {

    val uiState: StateFlow<HabitsUiState> = repository.observeAllHabits()
        .map { all ->
            val (archived, active) = all.partition { it.habit.isArchived }
            HabitsUiState(active = active, archived = archived)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitsUiState(active = emptyList(), archived = emptyList(), isLoading = true)
        )

    /** Порядок после перетаскивания: id действующих привычек сверху вниз. */
    fun reorder(habitIds: List<Long>) {
        viewModelScope.launch { repository.reorder(habitIds) }
    }

    fun setArchived(habitId: Long, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(habitId, archived) }
    }

    /** Удаляет привычку и передаёт в [onDeleted] всё, что нужно для «Отменить». */
    fun delete(habitId: Long, onDeleted: (DeletedHabit) -> Unit) {
        viewModelScope.launch { repository.deleteHabit(habitId)?.let(onDeleted) }
    }
}
