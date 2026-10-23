package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.bit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** «Сегодня» — пятница 23 октября 2026. */
class HabitRemindersTest {

    private val friday = LocalDate.of(2026, 10, 23)
    private val times = listOf(LocalTime.of(19, 0), LocalTime.of(9, 0), LocalTime.of(13, 0))

    private fun habit(weekdays: Int = HabitEntity.EVERY_DAY, archived: Boolean = false) = HabitEntity(
        title = "Пить воду",
        icon = HabitIcon.Water,
        colorIndex = 0,
        dailyTarget = 6,
        weekdays = weekdays,
        position = 0,
        isArchived = archived,
        createdAt = Instant.EPOCH
    )

    @Test
    fun ближайшееВремяСегодня() {
        assertEquals(
            friday.atTime(13, 0),
            nextHabitReminder(habit(), times, friday.atTime(10, 30), todayComplete = false)
        )
    }

    @Test
    fun времяРовноСейчасУжеПрошло() {
        // Будильник на 13:00 сработал — следующее уже 19:00
        assertEquals(
            friday.atTime(19, 0),
            nextHabitReminder(habit(), times, friday.atTime(13, 0), todayComplete = false)
        )
    }

    @Test
    fun послеПоследнегоВремениЗавтраУтром() {
        assertEquals(
            friday.plusDays(1).atTime(9, 0),
            nextHabitReminder(habit(), times, friday.atTime(20, 0), todayComplete = false)
        )
    }

    @Test
    fun набраннаяЦельПереноситНаСледующийДень() {
        assertEquals(
            friday.plusDays(1).atTime(9, 0),
            nextHabitReminder(habit(), times, friday.atTime(10, 0), todayComplete = true)
        )
    }

    @Test
    fun поБуднямЧерезВыходные() {
        val weekdays = DayOfWeek.entries.take(5).sumOf { it.bit }
        assertEquals(
            LocalDate.of(2026, 10, 26).atTime(9, 0),
            nextHabitReminder(habit(weekdays), times, friday.atTime(20, 0), todayComplete = false)
        )
    }

    @Test
    fun безВремёнИВАрхивеНапоминанийНет() {
        assertNull(nextHabitReminder(habit(), emptyList(), friday.atTime(10, 0), todayComplete = false))
        assertNull(nextHabitReminder(habit(archived = true), times, friday.atTime(10, 0), todayComplete = false))
    }

    @Test
    fun распределениеПоДню() {
        assertEquals(
            listOf("09:00", "11:25", "13:50", "16:10", "18:35", "21:00").map(LocalTime::parse),
            distributeReminders(6)
        )
        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)), distributeReminders(2))
        assertEquals(listOf(LocalTime.of(9, 0)), distributeReminders(1))
        // 99 раз в день — почти каждые 7 минут, совпадений после округления нет
        assertEquals(99, distributeReminders(99).size)
    }

    @Test
    fun паузаПереноситНапоминаниеНаДеньПослеНеё() {
        // На паузе с сегодня на 10 дней — дальше недели, которую смотрит поиск без пауз
        val pause = HabitPauseEntity(habitId = 1, startDate = friday, resumeDate = friday.plusDays(10))
        assertEquals(
            friday.plusDays(10).atTime(9, 0),
            nextHabitReminder(habit(), times, friday.atTime(10, 0), todayComplete = false, pauses = listOf(pause))
        )
    }

    @Test
    fun паузаБезСрокаСнимаетНапоминания() {
        val pause = HabitPauseEntity(habitId = 1, startDate = friday.minusDays(3), resumeDate = null)
        assertNull(nextHabitReminder(habit(), times, friday.atTime(10, 0), todayComplete = false, pauses = listOf(pause)))
    }

    @Test
    fun прошедшаяПаузаНеМешает() {
        val pause = HabitPauseEntity(habitId = 1, startDate = friday.minusDays(7), resumeDate = friday)
        assertEquals(
            friday.atTime(13, 0),
            nextHabitReminder(habit(), times, friday.atTime(10, 30), todayComplete = false, pauses = listOf(pause))
        )
    }
}
