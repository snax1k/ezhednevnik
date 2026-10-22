package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {

    private val thursday = LocalDate.of(2026, 10, 22)

    private fun rule(unit: RepeatUnit, every: Int = 1, weekdays: Int = 0) = RepeatRule(unit, every, weekdays)

    @Test
    fun безПовтораСледующейДатыНет() {
        assertNull(RepeatRule.None.nextOccurrence(after = thursday, notBefore = thursday))
    }

    @Test
    fun простыеПовторы() {
        assertEquals(LocalDate.of(2026, 10, 23), rule(RepeatUnit.Day).nextOccurrence(thursday, thursday))
        assertEquals(LocalDate.of(2026, 10, 25), rule(RepeatUnit.Day, every = 3).nextOccurrence(thursday, thursday))
        assertEquals(LocalDate.of(2026, 10, 29), rule(RepeatUnit.Week).nextOccurrence(thursday, thursday))
        assertEquals(LocalDate.of(2027, 1, 22), rule(RepeatUnit.Month, every = 3).nextOccurrence(thursday, thursday))
        assertEquals(LocalDate.of(2027, 10, 22), rule(RepeatUnit.Year).nextOccurrence(thursday, thursday))
    }

    @Test
    fun просроченнаяЕжедневнаяПереходитНаСегодня() {
        val monday = LocalDate.of(2026, 10, 19)
        assertEquals(thursday, rule(RepeatUnit.Day).nextOccurrence(after = monday, notBefore = thursday))
    }

    @Test
    fun месяцСчитаетсяОтИсходнойДаты() {
        val january31 = LocalDate.of(2026, 1, 31)
        // Задачу выполнили в начале марта: февраль пропущен, копия встаёт на 31 марта, а не на 28-е
        assertEquals(
            LocalDate.of(2026, 3, 31),
            rule(RepeatUnit.Month).nextOccurrence(after = january31, notBefore = LocalDate.of(2026, 3, 2))
        )
    }

    @Test
    fun поДнямНедели() {
        val mondayAndThursday = 0b0001001 // понедельник и четверг
        val monday = LocalDate.of(2026, 10, 19)
        // С понедельника — на четверг той же недели
        assertEquals(thursday, rule(RepeatUnit.Week, weekdays = mondayAndThursday).nextOccurrence(monday, monday))
        // С четверга — на понедельник следующей недели
        assertEquals(
            LocalDate.of(2026, 10, 26),
            rule(RepeatUnit.Week, weekdays = mondayAndThursday).nextOccurrence(thursday, thursday)
        )
        // Раз в две недели: с четверга — на понедельник через неделю
        assertEquals(
            LocalDate.of(2026, 11, 2),
            rule(RepeatUnit.Week, every = 2, weekdays = mondayAndThursday).nextOccurrence(thursday, thursday)
        )
    }

    @Test
    fun поДнямНеделиСВоскресенья() {
        val sunday = LocalDate.of(2026, 10, 25)
        val onlySunday = 0b1000000
        assertEquals(LocalDate.of(2026, 11, 1), rule(RepeatUnit.Week, weekdays = onlySunday).nextOccurrence(sunday, sunday))
    }
}
