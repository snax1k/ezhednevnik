package com.snax1k.chekki.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PostponeOptionTest {

    // Четверг 22 октября 2026
    private val thursday = LocalDate.of(2026, 10, 22)

    @Test
    fun вариантыСчитаютсяОтСегодня() {
        assertEquals(LocalDate.of(2026, 10, 23), PostponeOption.Tomorrow.dateFor(thursday))
        assertEquals(LocalDate.of(2026, 10, 24), PostponeOption.Weekend.dateFor(thursday))
        assertEquals(LocalDate.of(2026, 10, 29), PostponeOption.NextWeek.dateFor(thursday))
    }

    @Test
    fun наВыходныхВСубботуИВоскресенье() {
        val saturday = LocalDate.of(2026, 10, 24)
        val sunday = LocalDate.of(2026, 10, 25)
        assertEquals(saturday, PostponeOption.Weekend.dateFor(saturday))
        assertEquals(sunday, PostponeOption.Weekend.dateFor(sunday))
    }

    @Test
    fun вМенюНетВариантаНаТуЖеДату() {
        // Задача уже на завтра: «Завтра» не показываем
        val options = PostponeOption.available(thursday, currentDate = thursday.plusDays(1)).map { it.first }
        assertEquals(listOf(PostponeOption.Weekend, PostponeOption.NextWeek), options)
    }

    @Test
    fun вПятницуЗавтраИВыходныеОдинДень() {
        val friday = LocalDate.of(2026, 10, 23)
        val options = PostponeOption.available(friday, currentDate = friday).map { it.first }
        assertEquals(listOf(PostponeOption.Tomorrow, PostponeOption.NextWeek), options)
    }
}
