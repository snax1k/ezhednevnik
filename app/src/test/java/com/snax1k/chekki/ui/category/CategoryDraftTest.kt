package com.snax1k.chekki.ui.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDraftTest {

    @Test
    fun новойКатегорииПервыйСвободныйЦвет() {
        // Шесть начальных категорий заняли цвета 0–5
        assertEquals(6, CategoryDraft.suggestedColor((0..5).toList()))
        // Цвет освободился после удаления категории
        assertEquals(2, CategoryDraft.suggestedColor(listOf(0, 1, 3, 4)))
        assertEquals(0, CategoryDraft.suggestedColor(emptyList()))
    }

    @Test
    fun всеЦветаЗанятыИдёмПоКругу() {
        val all = (0 until CategoryDraft.COLOR_COUNT).toList()
        assertEquals(0, CategoryDraft.suggestedColor(all))
        assertEquals(1, CategoryDraft.suggestedColor(all + 5))
    }

    @Test
    fun безНазванияНеСохраняется() {
        assertFalse(CategoryDraft(name = "  ").canSave)
        assertTrue(CategoryDraft(name = "Поездки").canSave)
    }
}
