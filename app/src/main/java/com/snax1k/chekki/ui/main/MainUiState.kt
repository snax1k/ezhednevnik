package com.snax1k.chekki.ui.main

import com.snax1k.chekki.data.db.HabitIcon
import java.time.LocalDate
import java.time.LocalTime

/** Группы задач на главном экране. */
enum class TaskGroup { NoDate, Overdue, Today, Done, Tomorrow, ThisWeek, ThisMonth, Later }

/** Быстрые входы под заголовком. */
enum class QuickEntry { Categories, Calendar, Search }

/** Круг привычки в верхней панели: сколько раз отмечено сегодня из цели. */
data class TrackerUi(
    val habitId: Long,
    val name: String,
    val icon: HabitIcon,
    /** Номер цвета в палитре привычек темы. */
    val colorIndex: Int,
    val done: Int,
    val total: Int
) {
    val isComplete: Boolean get() = total in 1..done
}

/** Задача в карточке «Сегодня». */
data class TaskPreviewUi(
    val title: String,
    val time: LocalTime?
)

/** Всё, что показывает главный экран. */
data class MainUiState(
    val date: LocalDate,
    val trackers: List<TrackerUi>,
    val noDateCount: Int,
    val overdueCount: Int,
    /** Названия первых просроченных задач для подписи карточки. */
    val overdueTitles: List<String>,
    val todayDone: Int,
    val todayTotal: Int,
    /** Ближайшие задачи на сегодня, карточка показывает первые две. */
    val todayUpcoming: List<TaskPreviewUi>,
    val tomorrowCount: Int,
    val thisWeekCount: Int,
    val thisMonthCount: Int,
    val laterCount: Int,
    /** База ещё не ответила: группы пока не показываем, чтобы не мелькали нули. */
    val isLoading: Boolean = false
)
