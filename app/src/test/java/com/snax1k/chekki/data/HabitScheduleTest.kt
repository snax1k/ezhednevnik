package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.bit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class HabitScheduleTest {

    private fun habit(weekdays: Int) = HabitEntity(
        title = "Бассейн",
        icon = HabitIcon.Run,
        colorIndex = 0,
        dailyTarget = 1,
        weekdays = weekdays,
        position = 0,
        createdAt = Instant.EPOCH
    )

    @Test
    fun битыДнейНедели() {
        assertEquals(1, DayOfWeek.MONDAY.bit)
        assertEquals(64, DayOfWeek.SUNDAY.bit)
        assertEquals(HabitEntity.EVERY_DAY, DayOfWeek.entries.sumOf { it.bit })
    }

    @Test
    fun привычкаПоВыходным() {
        val weekend = habit(DayOfWeek.SATURDAY.bit or DayOfWeek.SUNDAY.bit)
        assertFalse(weekend.isScheduledOn(LocalDate.of(2026, 10, 23)))
        assertTrue(weekend.isScheduledOn(LocalDate.of(2026, 10, 24)))
        assertTrue(weekend.isScheduledOn(LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun незнакомыйКодИконкиСтановитсяРостком() {
        assertEquals(HabitIcon.Water, HabitIcon.fromCode(2))
        assertEquals(HabitIcon.Sprout, HabitIcon.fromCode(999))
    }
}
