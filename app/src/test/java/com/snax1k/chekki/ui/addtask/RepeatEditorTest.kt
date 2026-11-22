package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RepeatEditorTest {

    // 23.11.2026 — понедельник нечётной недели
    private val today = LocalDate.of(2026, 11, 23)
    private val tuesday = LocalDate.of(2026, 11, 24)

    @Test
    fun безПовтораОткрываетсяНеделяСДнёмЗадачи() {
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = tuesday, today = today)
        assertEquals(RepeatTab.Week, editor.tab)
        assertTrue(editor.isWeekdayOn(DayOfWeek.TUESDAY))
        assertEquals(RepeatRule(RepeatUnit.Week, 1, 0b0000010), editor.rule)
        assertEquals(tuesday, editor.nextDate)
        assertTrue(editor.canSave)
    }

    @Test
    fun следующаяЗадачаПересчитываетсяПоДням() {
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = today, today = today)
            .toggleWeekday(DayOfWeek.MONDAY)
            .toggleWeekday(DayOfWeek.THURSDAY)
        assertEquals(LocalDate.of(2026, 11, 26), editor.nextDate)
        // Дни не выбраны — сохранить нельзя
        val empty = editor.toggleWeekday(DayOfWeek.THURSDAY)
        assertNull(empty.rule)
        assertFalse(empty.canSave)
    }

    @Test
    fun двухнедельныйРежимТудаИОбратно() {
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = tuesday, today = today)
            .withTwoWeeks(true)
            .toggleWeekday(DayOfWeek.MONDAY, nextWeek = true)
        // Эта неделя нечётная: её дни (Вт) — во второй маске, следующей (Пн) — в первой
        val rule = editor.rule!!
        assertEquals(RepeatRule(RepeatUnit.TwoWeeks, 2, 0b0000001, weekdays2 = 0b0000010), rule)
        assertEquals(tuesday, editor.nextDate)

        val reopened = RepeatEditor.of(rule, taskDate = tuesday, today = today)
        assertTrue(reopened.twoWeeks)
        assertTrue(reopened.isWeekdayOn(DayOfWeek.TUESDAY))
        assertTrue(reopened.isWeekdayOn(DayOfWeek.MONDAY, nextWeek = true))
        assertEquals(rule, reopened.rule)

        // Одинаковые дни в обе недели — просто каждую неделю
        val same = editor.toggleWeekday(DayOfWeek.MONDAY, nextWeek = true).toggleWeekday(DayOfWeek.TUESDAY, nextWeek = true)
        assertEquals(RepeatRule(RepeatUnit.Week, 1, 0b0000010), same.rule)
    }

    @Test
    fun разВДвеНеделиПоДнямОткрываетсяДвухнедельнымРежимом() {
        val editor = RepeatEditor.of(RepeatRule(RepeatUnit.Week, 2, 0b0000100), taskDate = LocalDate.of(2026, 12, 2), today = today)
        assertTrue(editor.twoWeeks)
        // Задача на следующей неделе — её день во втором ряду
        assertTrue(editor.isWeekdayOn(DayOfWeek.WEDNESDAY, nextWeek = true))
        assertFalse(editor.isWeekdayOn(DayOfWeek.WEDNESDAY))
        assertEquals(LocalDate.of(2026, 12, 2), editor.nextDate)
    }

    @Test
    fun числаМесяца() {
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = today, today = today)
            .withTab(RepeatTab.Month)
        // Сразу отмечено число даты задачи
        assertTrue(editor.isMonthDayOn(23))
        val changed = editor.toggleMonthDay(23).toggleMonthDay(2).toggleMonthDay(31)
        assertEquals(RepeatRule(RepeatUnit.Month, 1, 0, monthDays = (1 shl 1) or RepeatRule.LAST_MONTH_DAY_BIT), changed.rule)
        assertEquals(LocalDate.of(2026, 11, 30), changed.nextDate)
    }

    @Test
    fun свойПериод() {
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = today, today = today)
            .withTab(RepeatTab.Custom)
            .withEvery(3)
            .withUnit(RepeatUnit.Month)
        assertEquals(RepeatRule(RepeatUnit.Month, 3, 0), editor.rule)
        assertEquals(today, editor.nextDate)
        assertEquals(RepeatEditor.MAX_EVERY, editor.withEvery(1000).every)
        assertEquals(RepeatEditor.MIN_EVERY, editor.withEvery(0).every)
        // Квартал из старого списка открывается «своим периодом»
        val quarterly = RepeatEditor.of(RepeatRule(RepeatUnit.Month, 3, 0), taskDate = tuesday, today = today)
        assertEquals(RepeatTab.Custom, quarterly.tab)
        assertEquals(tuesday, quarterly.nextDate)
    }

    @Test
    fun срокПовтора() {
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = tuesday, today = today)
        val until = editor.withUntil(LocalDate.of(2026, 12, 31))
        assertEquals(LocalDate.of(2026, 12, 31), until.rule!!.until)
        assertTrue(until.canSave)
        // Срок раньше следующей задачи — сохранить нельзя
        val tooEarly = editor.withNextDate(LocalDate.of(2027, 1, 5)).withUntil(LocalDate.of(2026, 12, 31))
        assertTrue(tooEarly.untilTooEarly)
        assertFalse(tooEarly.canSave)
    }

    @Test
    fun прошлаяДатаЗадачиНеСтановитсяСледующей() {
        // Задача без повтора, просроченная на прошлую неделю: следующая — не раньше сегодня
        val editor = RepeatEditor.of(RepeatRule.None, taskDate = LocalDate.of(2026, 11, 17), today = today)
        assertEquals(tuesday, editor.nextDate)
    }
}
