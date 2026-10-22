package com.snax1k.chekki.reminders

import com.snax1k.chekki.data.AllDayReminderTime
import com.snax1k.chekki.data.db.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Когда сработает напоминание: время задачи минус [minutesBefore] минут, в часовом поясе [zone].
 * У задачи на весь день отсчёт идёт от [AllDayReminderTime]. null — у задачи нет даты или напоминания.
 */
fun reminderTrigger(date: LocalDate?, time: LocalTime?, minutesBefore: Int?, zone: ZoneId): Instant? {
    if (date == null || minutesBefore == null) return null
    return date.atTime(time ?: AllDayReminderTime)
        .minusMinutes(minutesBefore.toLong())
        .atZone(zone)
        .toInstant()
}

/** Время напоминания задачи; у выполненной задачи напоминания нет. */
fun TaskEntity.reminderTrigger(zone: ZoneId): Instant? =
    if (isDone) null else reminderTrigger(date, time, reminderMinutesBefore, zone)
