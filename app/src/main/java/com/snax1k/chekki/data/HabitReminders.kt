package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.pauseOn
import java.time.LocalDateTime
import java.time.LocalTime

/** С какого и до какого времени «Распределить по дню» расставляет напоминания. */
val HabitReminderDayStart: LocalTime = LocalTime.of(9, 0)
val HabitReminderDayEnd: LocalTime = LocalTime.of(21, 0)

/**
 * Ближайшее напоминание привычки после [now]. Пропускаются дни не по расписанию и дни паузы, а сегодняшний
 * день — если цель на сегодня уже набрана ([todayComplete]). null — напоминаний нет, привычка в архиве
 * или на паузе без срока.
 */
fun nextHabitReminder(
    habit: HabitEntity,
    times: List<LocalTime>,
    now: LocalDateTime,
    todayComplete: Boolean,
    pauses: List<HabitPauseEntity> = emptyList()
): LocalDateTime? {
    if (times.isEmpty() || habit.isArchived) return null
    val sorted = times.sorted()
    val today = now.toLocalDate()
    var date = today
    var daysChecked = 0
    // Через неделю расписание повторяется: восьмой день вне пауз проверять незачем
    while (daysChecked < 8) {
        val pause = pauses.pauseOn(date)
        if (pause != null) {
            // Пауза без срока — напоминаний нет, пока не нажмут «Продолжить»
            date = pause.resumeDate ?: return null
            continue
        }
        daysChecked++
        if (habit.isScheduledOn(date)) {
            if (date != today) return date.atTime(sorted.first())
            if (!todayComplete) sorted.firstOrNull { it > now.toLocalTime() }?.let { return date.atTime(it) }
        }
        date = date.plusDays(1)
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
