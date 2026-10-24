package com.snax1k.chekki.data.oldapp

import com.snax1k.chekki.data.db.CategoryIcon
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.db.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Перевод копии старого ежедневника. Часовой пояс телефона — UTC+3, «сейчас» — 25 октября 2026. */
class OldAppConverterTest {

    private val zone = ZoneOffset.ofHours(3)
    private val now = Instant.parse("2026-10-25T09:00:00Z")

    private fun task(
        uid: String,
        title: String = "Задача $uid",
        date: String? = "2026-10-30",
        time: String? = null,
        completed: Boolean = false,
        regularity: String? = null,
        created: String = "2026-01-01T10:00:00Z",
        importance: String = "not",
        text: String? = null,
        deadline: String? = null,
        notify: String? = null
    ) = OldTask(
        uid = uid,
        title = title,
        text = text,
        completed = completed,
        importance = importance,
        created = created,
        nextDate = date,
        time = time,
        completedDate = if (completed) "2026-10-20T15:00:00.123456Z" else null,
        notifyDate = notify,
        deadline = deadline,
        regularityUid = regularity
    )

    private fun backup(
        tasks: List<OldTask> = emptyList(),
        subtasks: List<OldSubtask> = emptyList(),
        categories: List<OldCategory> = emptyList(),
        links: List<OldTaskCategory> = emptyList(),
        regularities: List<OldRegularity> = emptyList(),
        habits: List<OldHabit> = emptyList(),
        records: List<OldHabitRecord> = emptyList(),
        implementations: List<OldImplementation> = emptyList()
    ) = OldBackup(categories, tasks, subtasks, links, regularities, habits, records, implementations, photoCount = 3, audioCount = 1)

    private fun convert(backup: OldBackup) = OldAppConverter.convert(backup, zone, now)

    @Test
    fun приоритетКатегорииИЗаметка() {
        val plan = convert(
            backup(
                tasks = listOf(task("t1", importance = "large", text = "Взять паспорт", deadline = "2026-10-31T21:00:00Z")),
                categories = listOf(
                    OldCategory("c1", "Работа", "briefcase", 0xFF8AB4FF.toInt(), 0),
                    OldCategory("c2", "Дом", "broom", 0xFFF5BE6A.toInt(), 1)
                ),
                links = listOf(OldTaskCategory("t1", "c2", 1), OldTaskCategory("t1", "c1", 0))
            )
        )
        val planned = plan.tasks.single()
        assertEquals(TaskPriority.High, planned.task.priority)
        // Первая категория — по порядку в задаче, вторая дописана в заметку вместе со сроком (срок — по местному дню)
        assertEquals("c1", planned.categoryUid)
        assertEquals("Взять паспорт\n\nТакже: Дом\nСрок: 1 нояб 2026", planned.task.note)
        assertEquals(listOf(CategoryIcon.Work, CategoryIcon.Home), plan.categories.map { it.icon })
        assertEquals(listOf(0, 2), plan.categories.map { it.colorIndex })
        assertEquals(1, plan.report.extraCategories)
        assertEquals(1, plan.report.deadlines)
        assertEquals(3, plan.report.photos)
    }

    @Test
    fun дублиСводятсяАПовторТолькоУПоследнейКопии() {
        val plan = convert(
            backup(
                tasks = listOf(
                    // Основная строка серии и её копия на ту же дату — у копии нет подзадач
                    task("old", title = "Зарядка", date = "2026-10-26", regularity = "day_1_0_0", created = "2024-01-01T00:00:00Z"),
                    task("copy", title = "Зарядка", date = "2026-10-26", regularity = "day_1_0_0", created = "2026-10-25T21:00:00Z"),
                    // Пропущенная ранняя копия
                    task("missed", title = "Зарядка", date = "2026-10-20", regularity = "day_1_0_0"),
                    // Выполненная — история, без повтора
                    task("done", title = "Зарядка", date = "2026-10-19", regularity = "day_1_0_0", completed = true)
                ),
                subtasks = listOf(OldSubtask("old", "Разминка", 0, false)),
                regularities = listOf(OldRegularity("day_1_0_0", "day", 1, 0, false))
            )
        )
        assertEquals(1, plan.report.mergedDuplicates)
        assertEquals(1, plan.report.missedRepeats)
        val byDate = plan.tasks.associateBy { it.task.date }
        val head = byDate.getValue(LocalDate.of(2026, 10, 26))
        assertEquals(RepeatRule(RepeatUnit.Day, 1, 0), head.task.repeat)
        assertEquals(listOf("Разминка"), head.subtasks.map { it.title })
        assertEquals(RepeatRule.None, byDate.getValue(LocalDate.of(2026, 10, 20)).task.repeat)
        val done = byDate.getValue(LocalDate.of(2026, 10, 19)).task
        assertTrue(done.isDone)
        assertEquals(RepeatRule.None, done.repeat)
        assertEquals(Instant.parse("2026-10-20T15:00:00.123456Z"), done.completedAt)
    }

    @Test
    fun правилаПовтора() {
        fun rule(type: String, period: Int, mask: Long, twoWeeks: Boolean = false) =
            OldAppConverter.repeatRule(OldRegularity("r", type, period, mask, twoWeeks))
        // Пн, Ср, Пт: биты 1, 3, 5 старой маски → биты 0, 2, 4 нашей
        assertEquals(RepeatRule(RepeatUnit.Week, 1, 0b10101) to true, rule("week", 1, 0b101010))
        assertEquals(RepeatRule(RepeatUnit.Week, 2, 0) to true, rule("week", 2, 0))
        // Две недели с одинаковыми днями — каждую неделю
        assertEquals(RepeatRule(RepeatUnit.Week, 1, 0b1111111) to true, rule("week", 1, 32766, twoWeeks = true))
        // Разные дни недель — объединение и «не точно»
        assertEquals(RepeatRule(RepeatUnit.Week, 1, 0b1111111) to false, rule("week", 1, 10922, twoWeeks = true))
        assertEquals(RepeatRule(RepeatUnit.Month, 6, 0) to true, rule("month", 6, 0))
        assertEquals(RepeatRule(RepeatUnit.Month, 1, 0) to true, rule("month", 1, 1L shl 20))
        assertEquals(RepeatRule(RepeatUnit.Month, 1, 0) to false, rule("month", 1, (1L shl 15) or (1L shl 30)))
        assertEquals(RepeatRule.None to false, OldAppConverter.repeatRule(null))
    }

    @Test
    fun приблизительныеПовторыВОтчёте() {
        val plan = convert(
            backup(
                tasks = listOf(task("t", title = "Счётчики", regularity = "month_1_x")),
                regularities = listOf(OldRegularity("month_1_x", "month", 1, (1L shl 15) or (1L shl 30), false))
            )
        )
        assertEquals(listOf("Счётчики"), plan.report.approximatedRepeats)
    }

    @Test
    fun напоминаниеЗаНМинутИУстаревшееНеПереносится() {
        val plan = convert(
            backup(
                tasks = listOf(
                    // 16:00 по местному = 13:00 UTC: напоминание в момент начала
                    task("on-time", time = "16:0", notify = "2026-10-30T13:00:00Z"),
                    task("early", time = "16:0", notify = "2026-10-30T12:45:00Z"),
                    // Напоминание за 9 месяцев — устаревшее
                    task("stale", time = "16:0", notify = "2026-01-23T13:00:00Z"),
                    // На весь день: напоминание накануне в 09:00
                    task("all-day", notify = "2026-10-29T06:00:00Z")
                )
            )
        )
        val minutes = plan.tasks.associate { it.task.title.removePrefix("Задача ") to it.task.reminderMinutesBefore }
        assertEquals(0, minutes["on-time"])
        assertEquals(15, minutes["early"])
        assertNull(minutes["stale"])
        assertEquals(24 * 60, minutes["all-day"])
        assertEquals(LocalTime.of(16, 0), plan.tasks.first().task.time)
    }

    @Test
    fun привычкаЦельНапоминанияИОтметкиПоМестномуДню() {
        val plan = convert(
            backup(
                habits = listOf(OldHabit("h", "Пить воду", "drop", 0xFF5C85F0.toInt(), "week_1_254_0", 1, "2024-07-29T11:15:11.749547Z", false, false)),
                regularities = listOf(OldRegularity("week_1_254_0", "week", 1, 254, false)),
                implementations = listOf(
                    OldImplementation("h", "20:0"),
                    OldImplementation("h", "14:0"),
                    OldImplementation("h", null)
                ),
                records = listOf(
                    // 22:30 UTC — это уже 21 октября по местному
                    OldHabitRecord("h", 2, 3, "2026-10-20T22:30:00Z"),
                    OldHabitRecord("h", 3, 3, "2026-10-21T08:00:00Z"),
                    OldHabitRecord("h", 0, 3, "2026-10-22T08:00:00Z")
                )
            )
        )
        val habit = plan.habits.single()
        assertEquals(HabitIcon.Water, habit.habit.icon)
        assertEquals(3, habit.habit.dailyTarget)
        assertEquals(HabitEntity.EVERY_DAY, habit.habit.weekdays)
        assertEquals(listOf(LocalTime.of(14, 0), LocalTime.of(20, 0)), habit.reminderTimes)
        // Две отметки 21-го слились (берётся больше), пустая 22-го не переносится
        assertEquals(listOf(LocalDate.of(2026, 10, 21) to 3), habit.checks.map { it.date to it.count })
        assertFalse(habit.habit.isArchived)
        assertEquals(1, plan.report.habits)
        assertEquals(2, plan.report.habitReminders)
    }
}
