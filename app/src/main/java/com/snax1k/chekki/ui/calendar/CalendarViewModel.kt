package com.snax1k.chekki.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DayMarks
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.monthGrid
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.tasks.toTaskUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Что показывает календарь. */
data class CalendarUiState(
    val today: LocalDate,
    /** Месяц, который сейчас открыт. */
    val month: YearMonth,
    val selectedDate: LocalDate,
    /** Точки под числами: открытый месяц и соседние, чтобы при листании они были уже на месте. */
    val marks: Map<LocalDate, DayMarks>,
    /** Задачи выбранного дня вместе с выполненными: по времени, «на весь день» в конце. */
    val dayTasks: List<TaskUi>,
    val isLoading: Boolean = false
)

/**
 * Календарь: сетка месяца с точками и задачи выбранного дня. Пока пользователь ничего не выбирал,
 * открыт текущий месяц и выбрано сегодня — и после полуночи это будет уже новый «сегодня».
 */
class CalendarViewModel(
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    /** Открытый месяц; null — месяц сегодняшнего дня. */
    private val month = MutableStateFlow<YearMonth?>(null)

    /** Выбранный день; null — сегодня. */
    private val selected = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = combine(currentDate, month, selected, ::Triple)
        .flatMapLatest { (today, chosenMonth, chosenDate) ->
            val shown = chosenMonth ?: YearMonth.from(today)
            val date = chosenDate ?: today
            combine(
                repository.observeCalendarMarks(monthGrid(shown.minusMonths(1)).first(), monthGrid(shown.plusMonths(1)).last()),
                repository.observeTasksOn(date)
            ) { marks, tasks ->
                CalendarUiState(
                    today = today,
                    month = shown,
                    selectedDate = date,
                    marks = marks,
                    dayTasks = tasks.map { it.toTaskUi() }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocalDate.now().let { today ->
                CalendarUiState(today, YearMonth.from(today), today, emptyMap(), emptyList(), isLoading = true)
            }
        )

    /**
     * Открыт другой месяц (свайп или стрелка). В месяце сегодняшнего дня выбирается сегодня,
     * в остальных — 1-е число.
     */
    fun showMonth(newMonth: YearMonth) {
        val today = uiState.value.today
        if (newMonth == (month.value ?: YearMonth.from(today))) return
        val isCurrent = newMonth == YearMonth.from(today)
        month.value = if (isCurrent) null else newMonth
        selected.value = if (isCurrent) null else newMonth.atDay(1)
    }

    /** Нажатие на день; день соседнего месяца открывает его месяц. */
    fun selectDate(date: LocalDate) {
        val today = uiState.value.today
        val dateMonth = YearMonth.from(date)
        month.value = if (dateMonth == YearMonth.from(today)) null else dateMonth
        selected.value = if (date == today) null else date
    }

    /** «Сегодня» в шапке. */
    fun goToday() {
        month.value = null
        selected.value = null
    }

    fun toggleDone(taskId: Long) {
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
