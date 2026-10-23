package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.HabitCheckEntity
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.pauseOn
import java.time.LocalDate
import java.time.ZoneId

/** Один день привычки: по расписанию ли он и сколько отмечено из цели того дня. */
data class HabitDay(
    val date: LocalDate,
    /** День по расписанию и не на паузе — или в нём есть отметки (расписание могли поменять позже). */
    val isScheduled: Boolean,
    val count: Int,
    val target: Int,
    /** День попал в паузу привычки. */
    val isPaused: Boolean = false
) {
    val isComplete: Boolean get() = target in 1..count
}

/**
 * День привычки по её отметкам. Если отметок за день нет, цель — нынешняя цель привычки.
 * День паузы без отметок — как день не по расписанию.
 */
fun habitDay(
    habit: HabitEntity,
    date: LocalDate,
    checksByDate: Map<LocalDate, HabitCheckEntity>,
    pauses: List<HabitPauseEntity> = emptyList()
): HabitDay {
    val check = checksByDate[date]
    val isPaused = pauses.pauseOn(date) != null
    return HabitDay(
        date = date,
        isScheduled = check != null || (habit.isScheduledOn(date) && !isPaused),
        count = check?.count ?: 0,
        target = check?.target ?: habit.dailyTarget,
        isPaused = isPaused
    )
}

/** Последние [days] дней до [today] включительно, по порядку. */
fun habitWeek(
    habit: HabitEntity,
    checks: List<HabitCheckEntity>,
    today: LocalDate,
    pauses: List<HabitPauseEntity> = emptyList(),
    days: Int = 7
): List<HabitDay> {
    val byDate = checks.associateBy { it.date }
    return (days - 1 downTo 0).map { habitDay(habit, today.minusDays(it.toLong()), byDate, pauses) }
}

/** Серии выполненных дней подряд. */
data class HabitStreaks(val current: Int, val best: Int)

/**
 * Серии считаются только по дням расписания: выходной у привычки «по будням» серию не рвёт, дни паузы тоже
 * (а выполненный день паузы серию продолжает). Сегодняшний день, пока цель не набрана, серию не рвёт —
 * она идёт до вчера. Отсчёт — с создания привычки или с самой ранней отметки, если она раньше
 * (например, после переноса).
 */
fun habitStreaks(
    habit: HabitEntity,
    checks: List<HabitCheckEntity>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    pauses: List<HabitPauseEntity> = emptyList()
): HabitStreaks {
    val byDate = checks.associateBy { it.date }
    val created = habit.createdAt.atZone(zone).toLocalDate()
    var date = minOf(created, checks.minOfOrNull { it.date } ?: created)
    var run = 0
    var best = 0
    while (date <= today) {
        val day = habitDay(habit, date, byDate, pauses)
        if (day.isScheduled) {
            when {
                day.isComplete -> {
                    run++
                    best = maxOf(best, run)
                }
                // Незаконченный сегодняшний день серию не рвёт, незаконченный день паузы — тоже
                date == today || day.isPaused -> Unit
                else -> run = 0
            }
        }
        date = date.plusDays(1)
    }
    return HabitStreaks(current = run, best = best)
}
