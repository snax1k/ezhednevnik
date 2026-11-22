package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // 23.11.2026 — понедельник нечётной недели, 30.11.2026 — понедельник чётной
    private val oddMonday = LocalDate.of(2026, 11, 23)
    private val evenMonday = LocalDate.of(2026, 11, 30)

    @Test
    fun чётностьНедели() {
        assertEquals(1, weekParity(oddMonday))
        assertEquals(1, weekParity(oddMonday.plusDays(6)))
        assertEquals(0, weekParity(evenMonday))
        assertEquals(0, weekParity(LocalDate.of(1970, 1, 1)))
    }

    @Test
    fun двеНеделиПоРазному() {
        // Чётные недели — Вт и Чт, нечётные — Пн
        val rule = RepeatRule(RepeatUnit.TwoWeeks, every = 2, weekdays = 0b0001010, weekdays2 = 0b0000001)
        // С понедельника нечётной недели — на вторник следующей (чётной): Вт этой недели не отмечен
        assertEquals(LocalDate.of(2026, 12, 1), rule.nextOccurrence(oddMonday, oddMonday))
        assertEquals(LocalDate.of(2026, 12, 3), rule.nextOccurrence(LocalDate.of(2026, 12, 1), oddMonday))
        // После четверга чётной недели — понедельник нечётной
        assertEquals(LocalDate.of(2026, 12, 7), rule.nextOccurrence(LocalDate.of(2026, 12, 3), oddMonday))
        assertTrue(rule.matches(oddMonday))
        assertFalse(rule.matches(evenMonday))
    }

    @Test
    fun поЧисламМесяца() {
        val days = RepeatRule(RepeatUnit.Month, every = 1, weekdays = 0, monthDays = (1 shl 1) or (1 shl 3) or (1 shl 6))
        val longAgo = LocalDate.of(2026, 10, 1)
        assertEquals(LocalDate.of(2026, 11, 7), days.nextOccurrence(LocalDate.of(2026, 11, 4), longAgo))
        assertEquals(LocalDate.of(2026, 12, 2), days.nextOccurrence(LocalDate.of(2026, 11, 7), longAgo))
        // Просроченная: не раньше сегодня
        assertEquals(LocalDate.of(2026, 12, 2), days.nextOccurrence(LocalDate.of(2026, 11, 2), oddMonday))
    }

    @Test
    fun тридцатьПервоеЭтоПоследнийДень() {
        val last = RepeatRule(RepeatUnit.Month, every = 1, weekdays = 0, monthDays = RepeatRule.LAST_MONTH_DAY_BIT)
        val feb10 = LocalDate.of(2026, 2, 10)
        assertEquals(LocalDate.of(2026, 2, 28), last.nextOccurrence(feb10, feb10))
        assertEquals(LocalDate.of(2028, 2, 29), last.nextOccurrence(LocalDate.of(2028, 2, 10), feb10))
        assertEquals(LocalDate.of(2026, 3, 31), last.nextOccurrence(LocalDate.of(2026, 2, 28), feb10))
        // 30-го в феврале нет — пропуск до марта
        val thirtieth = RepeatRule(RepeatUnit.Month, every = 1, weekdays = 0, monthDays = 1 shl 29)
        assertEquals(LocalDate.of(2026, 3, 30), thirtieth.nextOccurrence(feb10, feb10))
        // 30-е и 31-е в 30-дневном месяце — один день
        val both = thirtieth.copy(monthDays = (1 shl 29) or RepeatRule.LAST_MONTH_DAY_BIT)
        assertEquals(LocalDate.of(2026, 5, 30), both.nextOccurrence(LocalDate.of(2026, 4, 30), feb10))
    }

    @Test
    fun числаРазВДваМесяца() {
        val fifth = RepeatRule(RepeatUnit.Month, every = 2, weekdays = 0, monthDays = 1 shl 4)
        val jan5 = LocalDate.of(2026, 1, 5)
        assertEquals(LocalDate.of(2026, 3, 5), fifth.nextOccurrence(jan5, jan5))
    }

    @Test
    fun срокПовтора() {
        val untilFriday = rule(RepeatUnit.Day).copy(until = LocalDate.of(2026, 10, 23))
        assertEquals(LocalDate.of(2026, 10, 23), untilFriday.nextOccurrence(thursday, thursday))
        assertNull(untilFriday.nextOccurrence(LocalDate.of(2026, 10, 23), thursday))
    }

    @Test
    fun ближайшаяДатаПоПравилу() {
        val tuesdays = rule(RepeatUnit.Week, weekdays = 0b0000010)
        assertEquals(LocalDate.of(2026, 11, 24), tuesdays.firstOnOrAfter(oddMonday))
        assertEquals(LocalDate.of(2026, 11, 24), tuesdays.firstOnOrAfter(LocalDate.of(2026, 11, 24)))
        // У «своего периода» подходит любая дата
        assertEquals(oddMonday, rule(RepeatUnit.Day, every = 3).firstOnOrAfter(oddMonday))
        assertNull(RepeatRule.None.firstOnOrAfter(oddMonday))
    }
}
