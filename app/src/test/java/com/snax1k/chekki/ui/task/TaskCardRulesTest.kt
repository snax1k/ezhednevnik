package com.snax1k.chekki.ui.task

import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.data.EditedSubtask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** «Сегодня» — четверг 22 октября 2026: завтра 23.10, неделя 24–25.10, месяц 26–31.10, потом с 1.11. */
class TaskCardRulesTest {

    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun группаЗадачиПоДатеИОтметке() {
        assertEquals(TaskGroup.NoDate, taskGroupOf(null, isDone = false, today))
        assertEquals(TaskGroup.Overdue, taskGroupOf(today.minusDays(1), isDone = false, today))
        assertEquals(TaskGroup.Today, taskGroupOf(today, isDone = false, today))
        assertEquals(TaskGroup.Tomorrow, taskGroupOf(today.plusDays(1), isDone = false, today))
        assertEquals(TaskGroup.ThisWeek, taskGroupOf(today.plusDays(3), isDone = false, today))
        assertEquals(TaskGroup.ThisMonth, taskGroupOf(today.plusDays(5), isDone = false, today))
        assertEquals(TaskGroup.Later, taskGroupOf(today.plusDays(10), isDone = false, today))
    }

    @Test
    fun выполненнаяЗадачаНаСегодняОстаётсяВСегодня() {
        assertEquals(TaskGroup.Today, taskGroupOf(today, isDone = true, today))
        assertEquals(TaskGroup.Done, taskGroupOf(today.minusDays(1), isDone = true, today))
        assertEquals(TaskGroup.Done, taskGroupOf(null, isDone = true, today))
    }

    @Test
    fun черновикКарточкиДержитКонкретнуюДату() {
        val task = TaskEntity(
            title = "Стоматолог",
            date = today.plusDays(1),
            time = LocalTime.of(9, 0),
            categoryId = null,
            reminderMinutesBefore = 60,
            createdAt = Instant.EPOCH
        )
        val draft = task.toDraft()
        assertNull(draft.quickDate)
        assertEquals(today.plusDays(1), draft.dateFor(today))
        // Дата не «плывёт», даже если открыть карточку на следующий день
        assertEquals(today.plusDays(1), draft.dateFor(today.plusDays(1)))
    }

    private val savedSubtasks = listOf(
        CardSubtask(key = 10, id = 10, title = "Молоко", done = true),
        CardSubtask(key = 11, id = 11, title = "Хлеб", done = false)
    )

    private val savedDraft = TaskEntity(
        title = "Купить продукты", date = today, time = null, categoryId = null, createdAt = Instant.EPOCH
    ).toDraft()

    @Test
    fun безПравокСохранятьНечего() {
        assertFalse(hasUnsavedChanges(savedDraft, savedDraft, savedSubtasks, savedSubtasks))
        // Пробелы по краям и стёртое (не сохраняемое) название — не правка
        assertFalse(hasUnsavedChanges(savedDraft.copy(title = "  Купить продукты "), savedDraft, savedSubtasks, savedSubtasks))
        assertFalse(hasUnsavedChanges(savedDraft.copy(title = ""), savedDraft, savedSubtasks, savedSubtasks))
        // Пустая новая подзадача — тоже
        val withEmpty = savedSubtasks + CardSubtask(key = -1, id = null, title = " ", done = false)
        assertFalse(hasUnsavedChanges(savedDraft, savedDraft, withEmpty, savedSubtasks))
    }

    @Test
    fun правкиПолейИПодзадачЗамечаются() {
        assertTrue(hasUnsavedChanges(savedDraft.copy(title = "Купить продукты на неделю"), savedDraft, savedSubtasks, savedSubtasks))
        assertTrue(hasUnsavedChanges(savedDraft.copy(priority = 3), savedDraft, savedSubtasks, savedSubtasks))
        assertTrue(hasUnsavedChanges(savedDraft.fixedTo(today).withDate(today.plusDays(1), today), savedDraft, savedSubtasks, savedSubtasks))
        // Отметка, удаление и новая подзадача
        assertTrue(hasUnsavedChanges(savedDraft, savedDraft, savedSubtasks.map { it.copy(done = true) }, savedSubtasks))
        assertTrue(hasUnsavedChanges(savedDraft, savedDraft, savedSubtasks.take(1), savedSubtasks))
        assertTrue(hasUnsavedChanges(savedDraft, savedDraft, savedSubtasks + CardSubtask(-1, null, "Сыр", false), savedSubtasks))
    }

    @Test
    fun вБазуУходятПодзадачиБезПустых() {
        val rows = listOf(
            CardSubtask(key = 10, id = 10, title = "", done = true),
            CardSubtask(key = 11, id = 11, title = " Хлеб ржаной ", done = false),
            CardSubtask(key = -1, id = null, title = "Сыр", done = false),
            CardSubtask(key = -2, id = null, title = "", done = false)
        )
        assertEquals(
            listOf(
                // Стёртое название прежней подзадачи не сохраняется — остаётся «Молоко»
                EditedSubtask(10, "Молоко", true),
                EditedSubtask(11, "Хлеб ржаной", false),
                EditedSubtask(null, "Сыр", false)
            ),
            subtasksForSave(rows, savedSubtasks)
        )
    }

    @Test
    fun стёртоеНазваниеЗадачиНеСохраняется() {
        assertEquals("Купить продукты", savedDraft.copy(title = "   ").forSave(savedDraft).title)
        assertEquals("Хлеб", savedDraft.copy(title = " Хлеб ", note = " к ужину ").forSave(savedDraft).title)
        assertEquals("к ужину", savedDraft.copy(note = " к ужину ").forSave(savedDraft).note)
    }

    @Test
    fun строкиПодзадачОтмечаютПравящуюся() {
        val rows = subtaskRows(savedSubtasks, editingKey = 11)
        assertEquals(listOf(10L, 11L), rows.map { it.key })
        assertEquals(listOf(false, true), rows.map { it.isEditing })
    }

    @Test
    fun быстраяДатаСразуСтановитсяКонкретной() {
        val draft = TaskEntity(title = "Разобрать антресоль", date = null, time = null, categoryId = null, createdAt = Instant.EPOCH)
            .toDraft()
        assertEquals(QuickDate.NoDate, draft.quickDate)

        // Повтор у задачи без даты ставит «Сегодня», а карточка тут же запоминает конкретный день
        val withRepeat = draft.withRepeat(RepeatRule(RepeatUnit.Day, every = 1, weekdays = 0)).fixedTo(today)
        assertNull(withRepeat.quickDate)
        assertEquals(today, withRepeat.exactDate)

        val noDate = withRepeat.withQuickDate(QuickDate.NoDate).fixedTo(today)
        assertEquals(QuickDate.NoDate, noDate.quickDate)
        assertNull(noDate.dateFor(today))
    }
}
