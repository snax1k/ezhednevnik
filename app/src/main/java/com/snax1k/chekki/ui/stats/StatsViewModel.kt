package com.snax1k.chekki.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.StatsPeriod
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.TaskStats
import com.snax1k.chekki.data.taskStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** Что показывает экран «Статистика». [stats] null — ещё считается. */
data class StatsUiState(
    val today: LocalDate,
    val period: StatsPeriod = StatsPeriod.Week,
    /** На сколько периодов назад от нынешнего: 0 — эта неделя (месяц, год). */
    val offset: Int = 0,
    val stats: TaskStats? = null,
    /** Невыполненные задачи раньше сегодня — сейчас, от периода не зависит. */
    val overdue: Int = 0
)

/**
 * Статистика задач (решено 29.10.2026): период — неделя, месяц или год, листается назад.
 * Считается в приложении из всех выполненных задач (`TaskRepository.observeCompletedTasks`) — после
 * переноса это около 8,4 тыс. строк, пересчёт занимает миллисекунды.
 */
class StatsViewModel(
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>,
    private val zone: () -> ZoneId = ZoneId::systemDefault
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.Week)
    private val offset = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = currentDate
        .flatMapLatest { today ->
            combine(
                repository.observeCompletedTasks(),
                repository.observeGroupCounts(today),
                period,
                offset
            ) { completed, counts, chosenPeriod, chosenOffset ->
                StatsUiState(
                    today = today,
                    period = chosenPeriod,
                    offset = chosenOffset,
                    stats = taskStats(completed, chosenPeriod, chosenOffset, today, zone()),
                    overdue = counts.overdue
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState(today = LocalDate.now()))

    /** Неделя, месяц или год — с нынешнего периода. */
    fun selectPeriod(newPeriod: StatsPeriod) {
        if (period.value == newPeriod) return
        period.value = newPeriod
        offset.value = 0
    }

    fun previous() {
        offset.value += 1
    }

    /** Вперёд — не дальше нынешнего периода. */
    fun next() {
        if (offset.value > 0) offset.value -= 1
    }
}
