package com.snax1k.chekki.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeasonTest {

    @Test
    fun сезоныПоКалендарю() {
        assertEquals(Season.Winter, Season.of(LocalDate.of(2026, 12, 1)))
        assertEquals(Season.Winter, Season.of(LocalDate.of(2027, 1, 15)))
        assertEquals(Season.Winter, Season.of(LocalDate.of(2028, 2, 29)))
        assertEquals(Season.Spring, Season.of(LocalDate.of(2027, 3, 1)))
        assertEquals(Season.Spring, Season.of(LocalDate.of(2027, 5, 31)))
        assertEquals(Season.Summer, Season.of(LocalDate.of(2027, 6, 1)))
        assertEquals(Season.Summer, Season.of(LocalDate.of(2027, 8, 31)))
        assertEquals(Season.Autumn, Season.of(LocalDate.of(2026, 9, 1)))
        assertEquals(Season.Autumn, Season.of(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun выключеннаяСезоннаяИконкаОбычная() {
        val december = LocalDate.of(2026, 12, 5)
        assertEquals(Season.Winter, shownSeason(enabled = true, today = december))
        // Выключена — обычная иконка без сезона
        assertNull(shownSeason(enabled = false, today = december))
    }
}
