package com.snax1k.chekki.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormattersTest {

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun диапазонВнутриМесяца() {
        assertEquals("24–25 октября", formatDateRange(LocalDate.of(2026, 10, 24)..LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun диапазонЧерезГраницуМесяца() {
        assertEquals("31 октября – 1 ноября", formatDateRange(LocalDate.of(2026, 10, 31)..LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun диапазонИзОдногоДня() {
        assertEquals("31 октября", formatDateRange(LocalDate.of(2026, 10, 31)..LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun короткаяДатаСГодомТолькоВДругомГоду() {
        assertEquals("Пн, 26 окт", formatShortDate(LocalDate.of(2026, 10, 26), today))
        assertEquals("Ср, 20 янв 2027", formatShortDate(LocalDate.of(2027, 1, 20), today))
    }

    @Test
    fun деньИМесяцСГодомТолькоВДругомГоду() {
        assertEquals("1 ноября", formatDayMonth(LocalDate.of(2026, 11, 1), today))
        assertEquals("1 января 2027", formatDayMonth(LocalDate.of(2027, 1, 1), today))
    }
}
