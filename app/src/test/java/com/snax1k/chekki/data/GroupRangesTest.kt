package com.snax1k.chekki.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupRangesTest {

    @Test
    fun обычныйЧетверг() {
        // 22 октября 2026 — четверг
        val ranges = GroupRanges.of(LocalDate.of(2026, 10, 22))
        assertEquals(LocalDate.of(2026, 10, 23), ranges.tomorrow)
        assertEquals(LocalDate.of(2026, 10, 24)..LocalDate.of(2026, 10, 25), ranges.thisWeek)
        assertEquals(LocalDate.of(2026, 10, 26)..LocalDate.of(2026, 10, 31), ranges.thisMonth)
        assertEquals(LocalDate.of(2026, 11, 1), ranges.laterFrom)
    }

    @Test
    fun вСубботуНеделяПустая() {
        val ranges = GroupRanges.of(LocalDate.of(2026, 10, 24))
        assertEquals(LocalDate.of(2026, 10, 25), ranges.tomorrow)
        assertTrue(ranges.thisWeek.isEmpty())
        assertEquals(LocalDate.of(2026, 10, 26)..LocalDate.of(2026, 10, 31), ranges.thisMonth)
    }

    @Test
    fun вВоскресеньеНеделяПустаяАМесяцСПослезавтра() {
        val ranges = GroupRanges.of(LocalDate.of(2026, 10, 25))
        assertEquals(LocalDate.of(2026, 10, 26), ranges.tomorrow)
        assertTrue(ranges.thisWeek.isEmpty())
        assertEquals(LocalDate.of(2026, 10, 27)..LocalDate.of(2026, 10, 31), ranges.thisMonth)
    }

    @Test
    fun неделяПереходитВСледующийМесяц() {
        // Четверг 29 октября: неделя заканчивается 1 ноября, на «В этом месяце» дней не остаётся
        val ranges = GroupRanges.of(LocalDate.of(2026, 10, 29))
        assertEquals(LocalDate.of(2026, 10, 31)..LocalDate.of(2026, 11, 1), ranges.thisWeek)
        assertTrue(ranges.thisMonth.isEmpty())
        assertEquals(LocalDate.of(2026, 11, 2), ranges.laterFrom)
    }

    @Test
    fun последнийДеньГода() {
        // 31 декабря 2026 — четверг
        val ranges = GroupRanges.of(LocalDate.of(2026, 12, 31))
        assertEquals(LocalDate.of(2027, 1, 1), ranges.tomorrow)
        assertEquals(LocalDate.of(2027, 1, 2)..LocalDate.of(2027, 1, 3), ranges.thisWeek)
        assertTrue(ranges.thisMonth.isEmpty())
        assertEquals(LocalDate.of(2027, 1, 4), ranges.laterFrom)
    }

    @Test
    fun каждаяДатаПопадаетРовноВОднуГруппу() {
        var today = LocalDate.of(2026, 1, 1)
        while (today.year == 2026) {
            val ranges = GroupRanges.of(today)
            for (offset in 1L..70L) {
                val date = today.plusDays(offset)
                val groups = listOf(
                    date == ranges.tomorrow,
                    date in ranges.thisWeek,
                    date in ranges.thisMonth,
                    date >= ranges.laterFrom
                ).count { it }
                assertEquals("$date при сегодня $today", 1, groups)
            }
            today = today.plusDays(1)
        }
    }
}
