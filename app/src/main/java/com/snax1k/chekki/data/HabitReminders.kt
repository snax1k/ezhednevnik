package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.HabitEntity
import java.time.LocalDateTime
import java.time.LocalTime

/** С какого и до какого времени «Распределить по дню» расставляет напоминания. */
val HabitReminderDayStart: LocalTime = LocalTime.of(9, 0)
val HabitReminderDayEnd: LocalTime = LocalTime.of(21, 0)

/**
 * Ближайшее напоминание привычки после [now]. Пропускаются дни не по расписанию, а сегодняшний день —
 * если цель на сегодня уже набрана ([todayComplete]). null — напоминаний нет или привычка в архиве.
 */
fun nextHabitReminder(
    habit: HabitEntity,
    times: List<LocalTime>,
    now: LocalDateTime,
    todayComplete: Boolean
): LocalDateTime? {
    if (times.isEmpty() || habit.isArchived) return null
    val sorted = times.sorted()
    // Через неделю расписание повторяется, восьмой день проверять незачем
    for (offset in 0L..7L) {
        val date = now.toLocalDate().plusDays(offset)
        if (!habit.isScheduledOn(date)) continue
        if (offset == 0L) {
            if (todayComplete) continue
            sorted.firstOrNull { it > now.toLocalTime() }?.let { return date.atTime(it) }
        } else {
            return date.atTime(sorted.first())
        }
    }
    return null
}

/**
 * [count] времён равномерно с [from] до [to], округлённых до [roundToMinutes] минут.
 * Одно время — [from]. Совпавшие после округления схлопываются.
 */
fun distributeReminders(
    count: Int,
    from: LocalTime = HabitReminderDayStart,
    to: LocalTime = HabitReminderDayEnd,
    roundToMinutes: Int = 5
): List<LocalTime> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(from)
    val start = from.toSecondOfDay() / 60
    val span = to.toSecondOfDay() / 60 - start
    return (0 until count).map { index ->
        val minutes = start + span * index.toDouble() / (count - 1)
        val rounded = (Math.round(minutes / roundToMinutes) * roundToMinutes).toInt()
        LocalTime.of(rounded / 60 % 24, rounded % 60)
    }.distinct()
}
