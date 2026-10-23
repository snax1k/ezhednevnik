package com.snax1k.chekki.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.HabitDay
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.HabitStreaks
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.habitDay
import com.snax1k.chekki.data.habitStreaks
import com.snax1k.chekki.data.habitWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Что показывает карточка привычки. */
data class HabitCardState(
    val habit: HabitEntity,
    val today: LocalDate,
    /** День, который меняют «−» и «+»: сегодня или выбранный в полоске прошлый день. */
    val selectedDay: HabitDay,
    /** Последние 7 дней, сегодня — последним. */
    val week: List<HabitDay>,
    val streaks: HabitStreaks
)

/**
 * Карточка привычки (нижняя панель по долгому нажатию на круг). Живёт, пока открыто приложение,
 * [open] переключает её на другую привычку. Прошлые дни можно исправлять только в пределах полоски — 7 дней.
 */
class HabitCardViewModel(
    private val repository: HabitRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    private val habitId = MutableStateFlow<Long?>(null)

    /** Выбранный день; null — сегодня (и после полуночи тоже «сегодня», а не вчерашняя дата). */
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    /** null — привычка не выбрана или её удалили: карточку нужно закрыть. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HabitCardState?> = combine(habitId, currentDate, selectedDate, ::Triple)
        .flatMapLatest { (id, today, selected) ->
            if (id == null) {
                flowOf(null)
            } else {
                combine(repository.observeHabit(id), repository.observeChecks(id)) { habit, checks ->
                    habit?.let {
                        val week = habitWeek(it, checks, today)
                        val date = selected?.takeIf { day -> week.any { d -> d.date == day && d.isScheduled } } ?: today
                        HabitCardState(
                            habit = it,
                            today = today,
                            selectedDay = habitDay(it, date, checks.associateBy { check -> check.date }),
                            week = week,
                            streaks = habitStreaks(it, checks, today)
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun open(id: Long) {
        habitId.value = id
        selectedDate.value = null
    }

    /** Карточка снова открыта после выгрузки приложения из памяти, а ViewModel новая: открываем ту же привычку. */
    fun restoreIfNeeded(id: Long) {
        if (habitId.value == null) open(id)
    }

    /** Выбор дня в полоске; сегодняшний — снова «сегодня». */
    fun selectDay(date: LocalDate) {
        val today = state.value?.today
        selectedDate.value = if (date == today) null else date
    }

    fun increment() = changeSelected { id, date -> repository.check(id, date) }

    fun decrement() = changeSelected { id, date -> repository.uncheck(id, date) }

    private fun changeSelected(change: suspend (Long, LocalDate) -> Unit) {
        val current = state.value ?: return
        viewModelScope.launch { change(current.habit.id, current.selectedDay.date) }
    }
}
