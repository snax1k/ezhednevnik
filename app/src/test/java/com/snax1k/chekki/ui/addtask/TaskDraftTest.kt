package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.nextOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskDraftTest {

    // 22 октября 2026 — четверг
    private val thursday = LocalDate.of(2026, 10, 22)

    private val fullDraft = TaskDraft(
        title = "Созвон",
        quickDate = QuickDate.Tomorrow,
        time = LocalTime.of(10, 30),
        repeat = RepeatPreset.Weekly.rule,
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
        assertEquals(RepeatPreset.Weekly.rule, draft.repeat)
        assertEquals(15, draft.reminderMinutesBefore)
    }

    @Test
    fun повторИлиНапоминаниеБезДатыСтавятСегодня() {
        val noDate = TaskDraft(quickDate = QuickDate.NoDate)
        assertEquals(QuickDate.Today, noDate.withRepeat(RepeatPreset.Daily.rule).quickDate)
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
    fun готовыеВариантыПовтора() {
        RepeatPreset.entries.forEach { assertEquals(it, RepeatPreset.of(it.rule)) }
        // По будням: с пятницы — на понедельник
        val friday = LocalDate.of(2026, 10, 23)
        assertEquals(LocalDate.of(2026, 10, 26), RepeatPreset.Weekdays.rule.nextOccurrence(friday, friday))
        // Раз в две недели — через 14 дней
        assertEquals(LocalDate.of(2026, 11, 5), RepeatPreset.Biweekly.rule.nextOccurrence(thursday, thursday))
    }
}
