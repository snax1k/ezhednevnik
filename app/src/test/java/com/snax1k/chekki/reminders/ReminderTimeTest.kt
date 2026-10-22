package com.snax1k.chekki.reminders

import com.snax1k.chekki.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderTimeTest {

    private val moscow = ZoneId.of("Europe/Moscow")
    private val thursday = LocalDate.of(2026, 10, 22)

    @Test
    fun задачаСоВременем() {
        // 19:00 по Москве минус 10 минут = 18:50 MSK = 15:50 UTC
        assertEquals(
            Instant.parse("2026-10-22T15:50:00Z"),
            reminderTrigger(thursday, LocalTime.of(19, 0), minutesBefore = 10, zone = moscow)
        )
        assertEquals(
            Instant.parse("2026-10-22T16:00:00Z"),
            reminderTrigger(thursday, LocalTime.of(19, 0), minutesBefore = 0, zone = moscow)
        )
    }

    @Test
    fun заДеньПереходитНаПредыдущийДень() {
        assertEquals(
            Instant.parse("2026-10-21T16:00:00Z"),
            reminderTrigger(thursday, LocalTime.of(19, 0), minutesBefore = 24 * 60, zone = moscow)
        )
    }

    @Test
    fun задачаНаВесьДеньОтсчитываетсяОтДевяти() {
        // В день задачи — 09:00 MSK = 06:00 UTC, накануне — днём раньше
        assertEquals(
            Instant.parse("2026-10-22T06:00:00Z"),
            reminderTrigger(thursday, time = null, minutesBefore = 0, zone = moscow)
        )
        assertEquals(
            Instant.parse("2026-10-21T06:00:00Z"),
            reminderTrigger(thursday, time = null, minutesBefore = 24 * 60, zone = moscow)
        )
    }

    @Test
    fun безДатыИлиНапоминанияНичегоНет() {
        assertNull(reminderTrigger(null, LocalTime.of(19, 0), minutesBefore = 10, zone = moscow))
        assertNull(reminderTrigger(thursday, LocalTime.of(19, 0), minutesBefore = null, zone = moscow))
    }

    @Test
    fun уВыполненнойЗадачиНапоминанияНет() {
        val task = TaskEntity(
            title = "Созвон",
            date = thursday,
            time = LocalTime.of(10, 30),
            categoryId = null,
            reminderMinutesBefore = 15,
            createdAt = Instant.EPOCH
        )
        assertEquals(Instant.parse("2026-10-22T07:15:00Z"), task.reminderTrigger(moscow))
        assertNull(task.copy(isDone = true).reminderTrigger(moscow))
    }
}
