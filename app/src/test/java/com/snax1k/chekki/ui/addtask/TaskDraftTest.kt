package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskDraftTest {

    // 22 октября 2026 — четверг
    private val thursday = LocalDate.of(2026, 10, 22)

    private val weekly = RepeatRule(RepeatUnit.Week, every = 1, weekdays = 0)
    private val daily = RepeatRule(RepeatUnit.Day, every = 1, weekdays = 0)

    private val fullDraft = TaskDraft(
        title = "Созвон",
        quickDate = QuickDate.Tomorrow,
        time = LocalTime.of(10, 30),
        repeat = weekly,
        reminderMinutesBefore = 15
    )

    @Test
    fun безДатыСбрасываетВремяПовторИНапоминание() {
        val draft = fullDraft.withQuickDate(QuickDate.NoDate)
        assertFalse(draft.hasDate)
        assertNull(draft.time)
        assertEquals(RepeatRule.None, draft.repeat)
        assertNull(draft.reminderMinutesBefore)
    }

    @Test
    fun другаяПлашкаВремяИПовторНеТрогает() {
        val draft = fullDraft.withQuickDate(QuickDate.Weekend)
        assertEquals(LocalTime.of(10, 30), draft.time)
        assertEquals(weekly, draft.repeat)
        assertEquals(15, draft.reminderMinutesBefore)
    }

    @Test
    fun повторИлиНапоминаниеБезДатыСтавятСегодня() {
        val noDate = TaskDraft(quickDate = QuickDate.NoDate)
        assertEquals(QuickDate.Today, noDate.withRepeat(daily).quickDate)
        assertEquals(QuickDate.Today, noDate.withReminder(0).quickDate)
        // «Не повторять» и «Без напоминания» дату не трогают
        assertEquals(QuickDate.NoDate, noDate.withRepeat(RepeatRule.None).quickDate)
        assertEquals(QuickDate.NoDate, noDate.withReminder(null).quickDate)
    }

    @Test
    fun наВесьДеньНапоминаниеПодстраивается() {
        assertEquals(0, fullDraft.withTime(null).reminderMinutesBefore)
        assertEquals(24 * 60, fullDraft.withReminder(24 * 60).withTime(null).reminderMinutesBefore)
        assertNull(fullDraft.withReminder(null).withTime(null).reminderMinutesBefore)
        // Со временем напоминание остаётся как было
        assertEquals(15, fullDraft.withTime(LocalTime.of(18, 0)).reminderMinutesBefore)
    }

    @Test
    fun датаИзКалендаря() {
        val saturday = LocalDate.of(2026, 10, 24)
        val nextWednesday = LocalDate.of(2026, 10, 28)
        assertEquals(QuickDate.Weekend, fullDraft.withDate(saturday, thursday).quickDate)
        val exact = fullDraft.withDate(nextWednesday, thursday)
        assertNull(exact.quickDate)
        assertEquals(nextWednesday, exact.dateFor(thursday))
    }

    @Test
    fun следующаяЗадачаИзОкнаПовтораСтановитсяДатой() {
        val nextTuesday = LocalDate.of(2026, 10, 27)
        val tuesdays = RepeatRule(RepeatUnit.Week, every = 1, weekdays = 0b0000010)
        val draft = fullDraft.withRepeat(tuesdays, nextDate = nextTuesday, today = thursday)
        assertEquals(tuesdays, draft.repeat)
        assertEquals(nextTuesday, draft.dateFor(thursday))
        // Дата, у которой есть своя плашка, становится плашкой
        assertEquals(QuickDate.Today, fullDraft.withRepeat(daily, nextDate = thursday, today = thursday).quickDate)
        // «Не повторять» дату не меняет
        assertEquals(QuickDate.Tomorrow, fullDraft.withRepeat(RepeatRule.None, nextDate = nextTuesday, today = thursday).quickDate)
    }
}
