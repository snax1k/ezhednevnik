package com.snax1k.chekki.ui.task

import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.addtask.RepeatPreset
import com.snax1k.chekki.ui.main.TaskGroup
import org.junit.Assert.assertEquals
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

    @Test
    fun быстраяДатаСразуСтановитсяКонкретной() {
        val draft = TaskEntity(title = "Разобрать антресоль", date = null, time = null, categoryId = null, createdAt = Instant.EPOCH)
            .toDraft()
        assertEquals(QuickDate.NoDate, draft.quickDate)

        // Повтор у задачи без даты ставит «Сегодня», а карточка тут же запоминает конкретный день
        val withRepeat = draft.withRepeat(RepeatPreset.Daily.rule).fixedTo(today)
        assertNull(withRepeat.quickDate)
        assertEquals(today, withRepeat.exactDate)

        val noDate = withRepeat.withQuickDate(QuickDate.NoDate).fixedTo(today)
        assertEquals(QuickDate.NoDate, noDate.quickDate)
        assertNull(noDate.dateFor(today))
    }
}
