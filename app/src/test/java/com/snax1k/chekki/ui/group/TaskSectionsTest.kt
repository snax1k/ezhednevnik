package com.snax1k.chekki.ui.group

import com.snax1k.chekki.ui.tasks.Priority
import com.snax1k.chekki.ui.tasks.TaskUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskSectionsTest {

    private fun task(id: Long, priority: Priority? = null) =
        TaskUi(id = id, title = "Задача $id", time = null, category = null, priority = priority)

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

    @Test
    fun сПриоритетамиРазделыПоПриоритетуСДатойВСтроке() {
        val monday = LocalDate.of(2026, 10, 26)
        val sections = sectionsByPriorityOrDay(
            listOf(
                monday.plusDays(2) to task(1, Priority.High),
                monday.plusDays(4) to task(2, Priority.High),
                monday to task(3, Priority.Medium),
                monday to task(4),
                monday.plusDays(3) to task(5)
            )
        )
        assertTrue(sections.all { it.isPriority && it.day == null })
        assertEquals(listOf(Priority.High, Priority.Medium, null), sections.map { it.priority })
        assertEquals(listOf(listOf(1L, 2L), listOf(3L), listOf(4L, 5L)), sections.map { section -> section.tasks.map { it.id } })
        assertTrue(sections.flatMap { it.tasks }.all { it.showDate })
    }

    @Test
    fun безПриоритетовПоДнямКакРаньше() {
        val today = LocalDate.of(2026, 10, 23)
        val tasks = listOf(today to task(1), today.plusDays(1) to task(2), null to task(3))
        val sections = sectionsByPriorityOrDay(tasks, noDateLast = true)
        assertEquals(sectionsWithNoDateLast(tasks), sections)
        assertFalse(sections.any { it.isPriority })
        assertEquals(sectionsByDay(tasks.take(2)), sectionsByPriorityOrDay(tasks.take(2)))
    }
}
