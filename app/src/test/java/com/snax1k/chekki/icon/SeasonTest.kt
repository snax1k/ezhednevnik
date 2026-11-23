package com.snax1k.chekki.icon

import org.junit.Assert.assertEquals
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
    fun выключеннаяСезоннаяИконкаНеМеняется() {
        val december = LocalDate.of(2026, 12, 5)
        // Включена — по дате
        assertEquals(Season.Winter, shownSeason(enabled = true, frozen = Season.Autumn, today = december))
        // Выключена осенью — и зимой осенняя
        assertEquals(Season.Autumn, shownSeason(enabled = false, frozen = Season.Autumn, today = december))
        // Выключена, но сезон не запомнился — по дате
        assertEquals(Season.Winter, shownSeason(enabled = false, frozen = null, today = december))
    }

    @Test
    fun кодыСезоновНеМеняются() {
        // Коды хранятся в настройках: менять их нельзя
        assertEquals(listOf(1, 2, 3, 4), Season.entries.map { it.code })
        Season.entries.forEach { assertEquals(it, Season.fromCode(it.code)) }
    }
}
