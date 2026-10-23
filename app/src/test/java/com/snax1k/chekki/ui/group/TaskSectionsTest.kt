package com.snax1k.chekki.ui.group

import com.snax1k.chekki.ui.tasks.TaskUi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaskSectionsTest {

    private fun task(id: Long) = TaskUi(id = id, title = "Задача $id", time = null, category = null, priority = null)

    @Test
    fun задачиОдногоДняСобираютсяВОднуСекцию() {
        val monday = LocalDate.of(2026, 10, 26)
        val tuesday = monday.plusDays(1)
        val sections = sectionsByDay(
            listOf(monday to task(1), monday to task(2), tuesday to task(3))
        )
        assertEquals(listOf(monday, tuesday), sections.map { it.day })
        assertEquals(listOf(listOf(1L, 2L), listOf(3L)), sections.map { section -> section.tasks.map { it.id } })
    }

    @Test
    fun порядокНеМеняется() {
        val day = LocalDate.of(2026, 10, 26)
        val sections = sectionsByDay(listOf(day to task(3), day to task(1), day to task(2)))
        assertEquals(listOf(3L, 1L, 2L), sections.single().tasks.map { it.id })
    }

    @Test
    fun пустойСписокДаётПустыеСекции() {
        assertEquals(emptyList<TaskSection>(), sectionsByDay(emptyList()))
    }

    @Test
    fun задачиБезДатыИдутПодЗаголовкомВКонце() {
        val today = LocalDate.of(2026, 10, 23)
        val sections = sectionsWithNoDateLast(
            listOf(today.minusDays(2) to task(1), today to task(2), null to task(3), null to task(4))
        )
        assertEquals(listOf(today.minusDays(2), today, null), sections.map { it.day })
        assertEquals(listOf(false, false, true), sections.map { it.isNoDate })
        assertEquals(listOf(3L, 4L), sections.last().tasks.map { it.id })
    }
}
