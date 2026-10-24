package com.snax1k.chekki.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** «Сегодня» — суббота 24 октября 2026. */
class SearchDatesTest {

    private val today = LocalDate.of(2026, 10, 24)

    @Test
    fun просроченныеТолькоНевыполненныеРаньшеСегодня() {
        assertTrue(DateFilter.Overdue.accepts(today.minusDays(1), isDone = false, today))
        assertFalse(DateFilter.Overdue.accepts(today.minusDays(1), isDone = true, today))
        assertFalse(DateFilter.Overdue.accepts(today, isDone = false, today))
        assertFalse(DateFilter.Overdue.accepts(null, isDone = false, today))
    }

    @Test
    fun этаНеделяСПонедельникаПоВоскресенье() {
        // Неделя 19–25 октября
        assertTrue(DateFilter.ThisWeek.accepts(LocalDate.of(2026, 10, 19), isDone = true, today))
        assertTrue(DateFilter.ThisWeek.accepts(LocalDate.of(2026, 10, 25), isDone = false, today))
        assertFalse(DateFilter.ThisWeek.accepts(LocalDate.of(2026, 10, 26), isDone = false, today))
        assertFalse(DateFilter.ThisWeek.accepts(LocalDate.of(2026, 10, 18), isDone = false, today))
    }

    @Test
    fun этотМесяцИБезДатыИПериод() {
        assertTrue(DateFilter.ThisMonth.accepts(LocalDate.of(2026, 10, 1), isDone = false, today))
        assertFalse(DateFilter.ThisMonth.accepts(LocalDate.of(2025, 10, 1), isDone = false, today))
        assertTrue(DateFilter.NoDate.accepts(null, isDone = false, today))
        assertFalse(DateFilter.NoDate.accepts(today, isDone = false, today))
        val period = DateFilter.Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3))
        assertTrue(period.accepts(LocalDate.of(2026, 11, 3), isDone = false, today))
        assertFalse(period.accepts(LocalDate.of(2026, 11, 4), isDone = false, today))
    }

    @Test
    fun последниеЗапросыНовыеСверхуБезПовторов() {
        val list = listOf("отчёт", "врач", "молоко")
        assertEquals(listOf("Врач", "отчёт", "молоко"), withRecent(list, " Врач "))
        assertEquals(listOf("отчет", "врач", "молоко"), withRecent(list, "отчет"))
        assertEquals(list, withRecent(list, "   "))
        assertEquals(MAX_RECENT, withRecent((1..MAX_RECENT).map { "запрос $it" }, "новый").size)
    }
}
