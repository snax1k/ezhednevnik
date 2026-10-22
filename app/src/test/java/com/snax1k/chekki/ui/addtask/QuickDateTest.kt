package com.snax1k.chekki.ui.addtask

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class QuickDateTest {

    // 22 октября 2026 — четверг
    private val thursday = LocalDate.of(2026, 10, 22)

    @Test
    fun сегодняЗавтраИБезДаты() {
        assertEquals(thursday, QuickDate.Today.resolve(thursday))
        assertEquals(LocalDate.of(2026, 10, 23), QuickDate.Tomorrow.resolve(thursday))
        assertNull(QuickDate.NoDate.resolve(thursday))
    }

    @Test
    fun наВыходныхВБудниБлижайшаяСуббота() {
        assertEquals(LocalDate.of(2026, 10, 24), QuickDate.Weekend.resolve(thursday))
        // Понедельник — суббота этой же недели
        assertEquals(LocalDate.of(2026, 10, 24), QuickDate.Weekend.resolve(LocalDate.of(2026, 10, 19)))
        // Пятница — завтра
        assertEquals(LocalDate.of(2026, 10, 24), QuickDate.Weekend.resolve(LocalDate.of(2026, 10, 23)))
    }

    @Test
    fun датаИзКалендаряПодсвечиваетСвоюПлашку() {
        assertEquals(QuickDate.Today, QuickDate.matching(thursday, thursday))
        assertEquals(QuickDate.Tomorrow, QuickDate.matching(LocalDate.of(2026, 10, 23), thursday))
        assertEquals(QuickDate.Weekend, QuickDate.matching(LocalDate.of(2026, 10, 24), thursday))
        assertNull(QuickDate.matching(LocalDate.of(2026, 10, 25), thursday))
        assertNull(QuickDate.matching(LocalDate.of(2026, 10, 21), thursday))
    }

    @Test
    fun приСовпаденииПобеждаетПерваяПлашка() {
        val friday = LocalDate.of(2026, 10, 23)
        val saturday = LocalDate.of(2026, 10, 24)
        // В пятницу суббота — это «Завтра», в субботу — «Сегодня»
        assertEquals(QuickDate.Tomorrow, QuickDate.matching(saturday, friday))
        assertEquals(QuickDate.Today, QuickDate.matching(saturday, saturday))
    }

    @Test
    fun наВыходныхВСубботуИВоскресеньеСегодня() {
        val saturday = LocalDate.of(2026, 10, 24)
        val sunday = LocalDate.of(2026, 10, 25)
        assertEquals(saturday, QuickDate.Weekend.resolve(saturday))
        assertEquals(sunday, QuickDate.Weekend.resolve(sunday))
    }
}
