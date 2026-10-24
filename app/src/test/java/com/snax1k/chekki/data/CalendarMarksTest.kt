package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.CalendarTaskMark
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarMarksTest {

    private val day = LocalDate.of(2026, 10, 24)

    private fun mark(date: LocalDate, done: Boolean, color: Int?) = CalendarTaskMark(date, done, color)

    @Test
    fun точкиПоКатегориямНевыполненныхБезПовторовНеБольшеТрёх() {
        val marks = calendarMarks(
            listOf(
                mark(day, done = false, color = 2),
                mark(day, done = true, color = 0),
                mark(day, done = false, color = 2),
                mark(day, done = false, color = null),
                mark(day, done = false, color = 4),
                mark(day, done = false, color = 5)
            )
        )
        // Выполненная «Работа» (0) не даёт точку, «Дом» (2) — одна точка, четвёртый цвет не помещается
        assertEquals(DayMarks(openColors = listOf(2, null, 4), allDone = false), marks.getValue(day))
    }

    @Test
    fun всёВыполненоСераяТочка() {
        val marks = calendarMarks(listOf(mark(day, done = true, color = 1), mark(day, done = true, color = 3)))
        assertTrue(marks.getValue(day).allDone)
        assertTrue(marks.getValue(day).openColors.isEmpty())
    }

    @Test
    fun днейБезЗадачНет() {
        val marks = calendarMarks(listOf(mark(day, done = false, color = 0)))
        assertEquals(setOf(day), marks.keys)
        assertFalse(marks.getValue(day).allDone)
    }

    @Test
    fun сеткаМесяцаШестьНедельСПонедельника() {
        val grid = monthGrid(YearMonth.of(2026, 10))
        assertEquals(42, grid.size)
        // 1 октября 2026 — четверг, сетка начинается с понедельника 28 сентября
        assertEquals(LocalDate.of(2026, 9, 28), grid.first())
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 8), grid.last())
    }

    @Test
    fun месяцСПонедельникаНачинаетсяСПервогоЧисла() {
        // 1 июня 2026 — понедельник
        assertEquals(LocalDate.of(2026, 6, 1), monthGrid(YearMonth.of(2026, 6)).first())
    }

    private val monday = LocalDate.of(2026, 10, 26)

    @Test
    fun еженедельныйПовторВПределахПериода() {
        val weekly = RepeatRule(RepeatUnit.Week, every = 1, weekdays = 0)
        assertEquals(
            listOf(monday.plusWeeks(1), monday.plusWeeks(2), monday.plusWeeks(3)),
            repeatDates(weekly, monday, from = monday, to = monday.plusWeeks(3), today = monday.minusDays(2))
        )
    }

    @Test
    fun повторыДоНачалаПериодаНеВходят() {
        val daily = RepeatRule(RepeatUnit.Day, every = 1, weekdays = 0)
        val from = monday.plusDays(10)
        val dates = repeatDates(daily, monday, from = from, to = from.plusDays(2), today = monday)
        assertEquals(listOf(from, from.plusDays(1), from.plusDays(2)), dates)
    }

    @Test
    fun просроченнаяЗадачаПовторяетсяНеРаньшеСегодня() {
        // Ежедневная задача с понедельника не выполнена, сегодня четверг: следующий повтор — сегодня, а не вторник
        val daily = RepeatRule(RepeatUnit.Day, every = 1, weekdays = 0)
        val thursday = monday.plusDays(3)
        assertEquals(
            listOf(thursday, thursday.plusDays(1)),
            repeatDates(daily, monday, from = monday, to = thursday.plusDays(1), today = thursday)
        )
    }

    @Test
    fun безПовтораДатНет() {
        assertEquals(emptyList<LocalDate>(), repeatDates(RepeatRule.None, monday, monday, monday.plusYears(1), monday))
    }
}
