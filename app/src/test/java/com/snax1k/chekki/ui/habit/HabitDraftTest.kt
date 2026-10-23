package com.snax1k.chekki.ui.habit

import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.bit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class HabitDraftTest {

    @Test
    fun безНазванияСохранитьНельзя() {
        assertFalse(HabitDraft().canSave)
        assertFalse(HabitDraft(title = "   ").canSave)
        assertTrue(HabitDraft(title = "Пить воду").canSave)
    }

    @Test
    fun цельОтОдногоДоДевяностоДевяти() {
        assertEquals(1, HabitDraft().withTarget(0).dailyTarget)
        assertEquals(7, HabitDraft().withTarget(7).dailyTarget)
        assertEquals(99, HabitDraft().withTarget(150).dailyTarget)
    }

    @Test
    fun последнийДеньВыключитьНельзя() {
        val mondayOnly = HabitDraft(weekdays = DayOfWeek.MONDAY.bit)
        assertEquals(mondayOnly, mondayOnly.withDayToggled(DayOfWeek.MONDAY))
        val withTuesday = mondayOnly.withDayToggled(DayOfWeek.TUESDAY)
        assertTrue(withTuesday.isOn(DayOfWeek.TUESDAY))
        assertFalse(withTuesday.withDayToggled(DayOfWeek.MONDAY).isOn(DayOfWeek.MONDAY))
    }

    @Test
    fun подписиДней() {
        assertEquals(HabitDaysPreset.EveryDay, HabitDaysPreset.of(HabitEntity.EVERY_DAY))
        assertEquals(HabitDaysPreset.Weekdays, HabitDaysPreset.of(0b0011111))
        assertEquals(HabitDaysPreset.Weekend, HabitDaysPreset.of(DayOfWeek.SATURDAY.bit or DayOfWeek.SUNDAY.bit))
        assertNull(HabitDaysPreset.of(DayOfWeek.MONDAY.bit or DayOfWeek.WEDNESDAY.bit))
    }
}
