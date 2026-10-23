package com.snax1k.chekki.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Сколько просроченных задач называть в подписи карточки. */
private const val OVERDUE_TITLES_SHOWN = 2

/** Сколько ближайших задач показывать в карточке «Сегодня». */
private const val TODAY_UPCOMING_SHOWN = 2

/**
 * Главный экран из базы: привычки на сегодня, счётчики групп, первые просроченные задачи и карточка «Сегодня».
 * При смене даты в полночь всё пересчитывается для нового дня.
 */
class MainViewModel(
    private val repository: TaskRepository,
    private val habitRepository: HabitRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainUiState> = currentDate
        .flatMapLatest { today ->
            combine(
                repository.observeGroupCounts(today),
                repository.observeOverdueTitles(today, limit = OVERDUE_TITLES_SHOWN),
                repository.observeTasksOn(today),
                habitRepository.observeHabitsOn(today)
            ) { counts, overdueTitles, todayTasks, habits ->
                MainUiState(
                    date = today,
                    trackers = habits.map { (habit, count) ->
                        TrackerUi(
                            habitId = habit.id,
                            name = habit.title,
                            icon = habit.icon,
                            colorIndex = habit.colorIndex,
                            done = count,
                            total = habit.dailyTarget
                        )
                    },
                    noDateCount = counts.noDate,
                    overdueCount = counts.overdue,
                    overdueTitles = overdueTitles,
                    todayDone = todayTasks.count { it.task.isDone },
                    todayTotal = todayTasks.size,
                    todayUpcoming = todayTasks
                        .filter { !it.task.isDone }
                        .take(TODAY_UPCOMING_SHOWN)
                        .map { TaskPreviewUi(title = it.task.title, time = it.task.time) },
                    tomorrowCount = counts.tomorrow,
                    thisWeekCount = counts.thisWeek,
                    thisMonthCount = counts.thisMonth,
                    laterCount = counts.later
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            // Пока экран не виден, базу не слушаем; 5 секунд запаса, чтобы не перезапускаться при повороте
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = loadingState(LocalDate.now())
        )

    /** Нажатие на круг привычки: ещё одна отметка за сегодня, но не больше цели. */
    fun checkHabit(habitId: Long) {
        val today = uiState.value.date
        viewModelScope.launch { habitRepository.check(habitId, today) }
    }

    private fun loadingState(today: LocalDate) = MainUiState(
        date = today,
        trackers = emptyList(),
        noDateCount = 0,
        overdueCount = 0,
        overdueTitles = emptyList(),
        todayDone = 0,
        todayTotal = 0,
        todayUpcoming = emptyList(),
        tomorrowCount = 0,
        thisWeekCount = 0,
        thisMonthCount = 0,
        laterCount = 0,
        isLoading = true
    )
}
