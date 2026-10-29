package com.snax1k.chekki.data

import androidx.room.ColumnInfo
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Выполненная задача для статистики: когда отметили ([completedAt]) и на какой день она была ([date], null — без даты). */
data class CompletedTask(@ColumnInfo(name = "completed_at") val completedAt: Instant, val date: LocalDate?)

/** Период статистики: неделя (Пн–Вс), календарный месяц или год. */
enum class StatsPeriod { Week, Month, Year }

/** Дни периода [period], в который попадает [today], сдвинутого на [offset] периодов назад (0 — нынешний). */
fun statsRange(period: StatsPeriod, today: LocalDate, offset: Int = 0): ClosedRange<LocalDate> = when (period) {
    StatsPeriod.Week -> {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
        start..start.plusDays(6)
    }
    StatsPeriod.Month -> {
        val start = today.withDayOfMonth(1).minusMonths(offset.toLong())
        start..start.with(TemporalAdjusters.lastDayOfMonth())
    }
    StatsPeriod.Year -> {
        val start = today.withDayOfYear(1).minusYears(offset.toLong())
        start..start.with(TemporalAdjusters.lastDayOfYear())
    }
}

/** Столбик: начало его дня или месяца ([start]) и сколько задач выполнено. */
data class StatsBar(val start: LocalDate, val count: Int)

/**
 * Статистика задач за период [range].
 * - [done] — выполнено за период (по дню отметки в часовом поясе телефона), [previousDone] — за предыдущий такой же период;
 * - [onTimePercent] — доля выполненных не позже своего дня среди выполненных за период задач с датой; null — таких нет;
 * - [bars] — по дням (неделя, месяц) или по месяцам (год);
 * - [currentStreak] — дней подряд с хотя бы одной выполненной задачей, до сегодня (сегодня без отметок серию не рвёт:
 *   день ещё идёт), [bestStreak] — самая длинная такая серия за всё время.
 */
data class TaskStats(
    val range: ClosedRange<LocalDate>,
    val done: Int,
    val previousDone: Int,
    val onTimePercent: Int?,
    val bars: List<StatsBar>,
    val currentStreak: Int,
    val bestStreak: Int
)

fun taskStats(
    completed: List<CompletedTask>,
    period: StatsPeriod,
    offset: Int,
    today: LocalDate,
    zone: ZoneId
): TaskStats {
    val range = statsRange(period, today, offset)
    val previous = statsRange(period, today, offset + 1)
    val days = completed.map { it.completedAt.atZone(zone).toLocalDate() }
    val inRange = completed.indices.filter { days[it] in range }

    val dated = inRange.filter { completed[it].date != null }
    val onTime = dated.count { days[it] <= completed[it].date!! }
    val perDay = inRange.groupingBy { days[it] }.eachCount()

    val bars = when (period) {
        StatsPeriod.Week, StatsPeriod.Month -> generateSequence(range.start) { it.plusDays(1) }
            .takeWhile { it <= range.endInclusive }
            .map { StatsBar(it, perDay[it] ?: 0) }
            .toList()
        StatsPeriod.Year -> (0 until 12).map { month ->
            val start = range.start.plusMonths(month.toLong())
            StatsBar(start, perDay.entries.sumOf { (day, count) -> if (day.year == start.year && day.month == start.month) count else 0 })
        }
    }

    val activeDays = days.toSortedSet()
    var best = 0
    var run = 0
    var previousDay: LocalDate? = null
    for (day in activeDays) {
        run = if (previousDay != null && day == previousDay.plusDays(1)) run + 1 else 1
        best = maxOf(best, run)
        previousDay = day
    }
    var current = 0
    var day = if (today in activeDays) today else today.minusDays(1)
    while (day in activeDays) {
        current++
        day = day.minusDays(1)
    }

    return TaskStats(
        range = range,
        done = inRange.size,
        previousDone = days.count { it in previous },
        onTimePercent = if (dated.isEmpty()) null else Math.round(onTime * 100f / dated.size),
        bars = bars,
        currentStreak = current,
        bestStreak = best
    )
}
