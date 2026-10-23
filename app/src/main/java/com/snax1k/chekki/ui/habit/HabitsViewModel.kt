package com.snax1k.chekki.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedHabit
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.db.HabitListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Что показывает экран «Все привычки». */
data class HabitsUiState(
    /** Действующие привычки в порядке кругов наверху главного экрана. */
    val active: List<HabitListItem>,
    /** На паузе сегодня (кроме архивных). */
    val paused: List<HabitListItem>,
    val archived: List<HabitListItem>,
    val today: LocalDate,
    /** База ещё не ответила: пустой список пока не показываем. */
    val isLoading: Boolean = false
)

/** Экран «Все привычки»: порядок, пауза, архив, удаление. */
class HabitsViewModel(
    private val repository: HabitRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HabitsUiState> = currentDate
        .flatMapLatest { today ->
            repository.observeAllHabits(today).map { all ->
                val (archived, rest) = all.partition { it.habit.isArchived }
                val (paused, active) = rest.partition { it.isPaused }
                HabitsUiState(active = active, paused = paused, archived = archived, today = today)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitsUiState(
                active = emptyList(),
                paused = emptyList(),
                archived = emptyList(),
                today = LocalDate.now(),
                isLoading = true
            )
        )

    /** Порядок после перетаскивания: id действующих привычек сверху вниз. */
    fun reorder(habitIds: List<Long>) {
        viewModelScope.launch { repository.reorder(habitIds) }
    }

    /** Пауза с сегодня до [resumeDate]; null — без срока. */
    fun pause(habitId: Long, resumeDate: LocalDate?) {
        viewModelScope.launch { repository.pause(habitId, resumeDate, uiState.value.today) }
    }

    fun resume(habitId: Long) {
        viewModelScope.launch { repository.resume(habitId, uiState.value.today) }
    }

    fun setArchived(habitId: Long, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(habitId, archived) }
    }

    /** Удаляет привычку и передаёт в [onDeleted] всё, что нужно для «Отменить». */
    fun delete(habitId: Long, onDeleted: (DeletedHabit) -> Unit) {
        viewModelScope.launch { repository.deleteHabit(habitId)?.let(onDeleted) }
    }
}
