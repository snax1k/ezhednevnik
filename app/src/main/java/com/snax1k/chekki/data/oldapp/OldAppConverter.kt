package com.snax1k.chekki.data.oldapp

import com.snax1k.chekki.data.db.CategoryIcon
import com.snax1k.chekki.data.db.HabitCheckEntity
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.data.db.PriorityLevels
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Что записать в базу: категории, задачи с подзадачами и привычки — ещё без наших id. */
data class ImportPlan(
    val categories: List<PlannedCategory>,
    val tasks: List<PlannedTask>,
    val habits: List<PlannedHabit>,
    val report: ImportReport
)

data class PlannedCategory(val oldUid: String, val name: String, val icon: CategoryIcon, val colorIndex: Int)

/** Задача: [task] без id и категории — категорию ставит запись по [categoryUid]. */
data class PlannedTask(val task: TaskEntity, val categoryUid: String?, val subtasks: List<PlannedSubtask>)

data class PlannedSubtask(val title: String, val isDone: Boolean, val position: Int)

/** Привычка: [habit] без id и места в ряду, отметки по дням ([checks] без habitId), времена напоминаний. */
data class PlannedHabit(
    val habit: HabitEntity,
    val checks: List<HabitCheckEntity>,
    val reminderTimes: List<LocalTime>,
    val paused: Boolean
)

/** Итог переноса для экрана: сколько чего перенесено и что перенесено не полностью. */
data class ImportReport(
    val categories: Int,
    val openTasks: Int,
    val doneTasks: Int,
    val subtasks: Int,
    val habits: Int,
    val habitChecks: Int,
    val habitReminders: Int,
    /** Невыполненные задачи, которые были в копии дважды (основная строка серии и копия того же дня): сведены в одну. */
    val mergedDuplicates: Int,
    /** Пропущенные повторы: ранние невыполненные копии повторяющейся задачи — перенесены без повтора. */
    val missedRepeats: Int,
    /** Невыполненные задачи, чей повтор у нас записан приблизительно (несколько чисел месяца, разные дни двух недель). */
    val approximatedRepeats: List<String>,
    /** Задачи со второй категорией: она дописана в заметку. */
    val extraCategories: Int,
    /** Задачи со сроком «до»: он дописан в заметку. */
    val deadlines: Int,
    val photos: Int,
    val audio: Int
)

/**
 * Перевод копии старого ежедневника в наши данные. Правила (утверждены 24.10.2026):
 * - у задачи одна категория — первая, остальные дописываются в заметку («Также: Работа»), срок «до» — тоже («Срок: 30 окт 2026»);
 * - повтор — в наше правило; чего у нас нет (несколько чисел месяца, разные дни двух недель) — ближайшее правило и строка в отчёте;
 * - моменты из UTC переводятся в часовой пояс телефона [zone];
 * - фото, аудио и настойчивые напоминания не переносятся.
 *
 * Особенности копии: у повторяющейся задачи бывает и основная строка серии, и её копия на ту же дату — они сводятся
 * в одну; повтор достаётся только самой поздней невыполненной копии, ранние (пропущенные) переносятся без повтора,
 * иначе каждая при выполнении создала бы свою цепочку. У выполненных повтора нет: это история.
 */
object OldAppConverter {

    fun convert(backup: OldBackup, zone: ZoneId, now: Instant = Instant.now()): ImportPlan {
        val today = now.atZone(zone).toLocalDate()
        val categories = backup.categories
            .sortedWith(compareBy({ it.order }, { it.title }))
            .map { PlannedCategory(it.uid, it.title.trim(), categoryIcon(it.icon), nearest(it.color, CategoryPalette)) }
        val categoryNames = backup.categories.associate { it.uid to it.title.trim() }
        val categoriesByTask = backup.taskCategories
            .filter { it.categoryUid in categoryNames }
            .groupBy { it.taskUid }
            .mapValues { (_, links) -> links.sortedBy { it.order }.map { it.categoryUid }.distinct() }
        val subtasksByTask = backup.subtasks.groupBy { it.taskUid }
        val regularities = backup.regularities.associateBy { it.uid }

        // Дубли невыполненных: одна задача, одна дата и время, одно правило — оставляем ту, где больше подзадач
        val (open, done) = backup.tasks.partition { !it.completed }
        val openKept = open
            .groupBy { listOf(it.title.trim(), it.nextDate, it.time, it.regularityUid) }
            .values
            .map { group ->
                group.sortedWith(
                    compareByDescending<OldTask> { subtasksByTask[it.uid]?.size ?: 0 }.thenBy { it.created.orEmpty() }
                ).first()
            }
        val mergedDuplicates = open.size - openKept.size
        // Повтор — только самой поздней невыполненной копии серии
        val repeatHeads = openKept
            .filter { it.regularityUid != null && it.nextDate != null }
            .groupBy { it.title.trim() to it.regularityUid }
            .values
            .map { series -> series.maxBy { it.nextDate!! }.uid }
            .toSet()
        val missedRepeats = openKept.count { it.regularityUid != null && it.nextDate != null && it.uid !in repeatHeads }

        val approximated = mutableListOf<String>()
        var extraCategories = 0
        var deadlines = 0
        val tasks = (openKept + done).map { old ->
            val date = parseDate(old.nextDate)
            val time = parseTime(old.time)
            val taskCategories = categoriesByTask[old.uid].orEmpty()
            if (taskCategories.size > 1) extraCategories++
            val deadline = parseInstant(old.deadline)?.atZone(zone)?.toLocalDate()
            if (deadline != null) deadlines++
            val repeat = if (old.uid in repeatHeads) {
                val (rule, exact) = repeatRule(regularities[old.regularityUid])
                if (!exact) approximated += old.title.trim()
                rule
            } else {
                RepeatRule.None
            }
            val note = buildNote(old.text, taskCategories.drop(1).mapNotNull { categoryNames[it] }, deadline)
            PlannedTask(
                task = TaskEntity(
                    title = old.title.trim().ifEmpty { "Без названия" },
                    note = note,
                    date = date,
                    time = time,
                    categoryId = null,
                    priority = priority(old.importance),
                    repeat = repeat,
                    reminderMinutesBefore = if (old.completed) null else reminderMinutes(date, time, parseInstant(old.notifyDate), zone),
                    isDone = old.completed,
                    completedAt = if (old.completed) parseInstant(old.completedDate) ?: parseInstant(old.created) ?: now else null,
                    createdAt = parseInstant(old.created) ?: now
                ),
                categoryUid = taskCategories.firstOrNull(),
                subtasks = subtasksByTask[old.uid].orEmpty()
                    .sortedBy { it.position }
                    .mapIndexed { index, sub -> PlannedSubtask(sub.title.trim(), sub.completed, index) }
                    .filter { it.title.isNotEmpty() }
            )
        }

        val implementationsByHabit = backup.implementations.groupBy { it.habitUid }
        val recordsByHabit = backup.habitRecords.groupBy { it.habitUid }
        val habits = backup.habits.sortedBy { it.position }.map { old ->
            val implementations = implementationsByHabit[old.uid].orEmpty()
            val target = implementations.size.coerceIn(1, MAX_HABIT_TARGET)
            PlannedHabit(
                habit = HabitEntity(
                    title = old.title.trim().ifEmpty { "Привычка" },
                    icon = habitIcon(old.icon),
                    colorIndex = nearest(old.color, HabitPalette),
                    dailyTarget = target,
                    weekdays = habitWeekdays(regularities[old.regularityUid]),
                    position = 0,
                    isArchived = old.completed,
                    createdAt = parseInstant(old.startDate) ?: now
                ),
                checks = habitChecks(recordsByHabit[old.uid].orEmpty(), zone),
                reminderTimes = implementations.mapNotNull { parseTime(it.notifyTime) }.distinct().sorted(),
                paused = old.paused
            )
        }

        return ImportPlan(
            categories = categories,
            tasks = tasks,
            habits = habits,
            report = ImportReport(
                categories = categories.size,
                openTasks = tasks.count { !it.task.isDone },
                doneTasks = tasks.count { it.task.isDone },
                subtasks = tasks.sumOf { it.subtasks.size },
                habits = habits.size,
                habitChecks = habits.sumOf { it.checks.size },
                habitReminders = habits.sumOf { it.reminderTimes.size },
                mergedDuplicates = mergedDuplicates,
                missedRepeats = missedRepeats,
                approximatedRepeats = approximated.distinct().sorted(),
                extraCategories = extraCategories,
                deadlines = deadlines,
                photos = backup.photoCount,
                audio = backup.audioCount
            )
        )
    }

    /** Заметка и дописанное к ней: вторая категория и срок «до». */
    private fun buildNote(text: String?, otherCategories: List<String>, deadline: LocalDate?): String {
        val extras = buildList {
            if (otherCategories.isNotEmpty()) add("Также: " + otherCategories.joinToString(", "))
            if (deadline != null) add("Срок: " + deadline.format(DeadlineFormatter).replace(".", ""))
        }
        val base = text.orEmpty().trim()
        return when {
            extras.isEmpty() -> base
            base.isEmpty() -> extras.joinToString("\n")
            else -> base + "\n\n" + extras.joinToString("\n")
        }
    }

    /**
     * Важность старого приложения как ранг: 3 — высокая, 2 — средняя, 1 — низкая (совпадает с id уровней по умолчанию).
     * Уровни у пользователя могут быть свои — настоящий id подставляет [OldAppImporter] по порядку уровней.
     */
    private fun priority(importance: String?): Long = when (importance) {
        "small" -> PriorityLevels.LOW
        "medium" -> PriorityLevels.MEDIUM
        "large" -> PriorityLevels.HIGH
        else -> PriorityLevels.NONE
    }

    /**
     * Напоминание «за N минут» от момента напоминания [notify] до начала задачи (у задачи на весь день — 09:00
     * её дня, как у нас). Напоминание после начала или раньше чем за неделю — устаревшее, не переносится.
     */
    private fun reminderMinutes(date: LocalDate?, time: LocalTime?, notify: Instant?, zone: ZoneId): Int? {
        if (date == null || notify == null) return null
        val start = date.atTime(time ?: ALL_DAY_REMINDER).atZone(zone).toInstant()
        val minutes = Duration.between(notify, start).toMinutes()
        return minutes.takeIf { it in 0..MAX_REMINDER_MINUTES }?.toInt()
    }

    /** Правило повтора и точно ли оно совпадает со старым. Нет правила — без повтора (тоже «не точно»). */
    internal fun repeatRule(regularity: OldRegularity?): Pair<RepeatRule, Boolean> {
        if (regularity == null) return RepeatRule.None to false
        val every = regularity.period.coerceAtLeast(1)
        val mask = regularity.daysMask
        return when (regularity.type) {
            "day" -> RepeatRule(RepeatUnit.Day, every, 0) to true
            "week" -> {
                if (!regularity.byTwoWeeks) {
                    RepeatRule(RepeatUnit.Week, every, weekBits(mask, firstBit = 1)) to true
                } else {
                    val first = weekBits(mask, firstBit = 1)
                    val second = weekBits(mask, firstBit = 8)
                    when {
                        first == second -> RepeatRule(RepeatUnit.Week, 1, first) to true
                        second == 0 -> RepeatRule(RepeatUnit.Week, 2, first) to true
                        first == 0 -> RepeatRule(RepeatUnit.Week, 2, second) to true
                        // Разные дни в первую и вторую неделю: у нас так нельзя — каждую неделю по всем этим дням
                        else -> RepeatRule(RepeatUnit.Week, 1, first or second) to false
                    }
                }
            }
            // Одно число месяца — это и есть «каждый месяц» от даты задачи; несколько чисел у нас не бывает
            "month" -> RepeatRule(RepeatUnit.Month, every, 0) to (java.lang.Long.bitCount(mask) <= 1)
            else -> RepeatRule.None to false
        }
    }

    /** Дни недели привычки: «неделя» — по битам, остальное — каждый день. */
    private fun habitWeekdays(regularity: OldRegularity?): Int {
        if (regularity?.type != "week") return HabitEntity.EVERY_DAY
        val bits = if (regularity.byTwoWeeks) {
            weekBits(regularity.daysMask, firstBit = 1) or weekBits(regularity.daysMask, firstBit = 8)
        } else {
            weekBits(regularity.daysMask, firstBit = 1)
        }
        return if (bits == 0) HabitEntity.EVERY_DAY else bits
    }

    /** Семь битов старой маски с [firstBit] (понедельник) → наши: бит 0 — понедельник. */
    private fun weekBits(mask: Long, firstBit: Int): Int = ((mask shr firstBit) and 0b1111111L).toInt()

    /** Отметки по дням: момент → день в часовом поясе телефона; два раза за день — берём больше. */
    private fun habitChecks(records: List<OldHabitRecord>, zone: ZoneId): List<HabitCheckEntity> =
        records.mapNotNull { record ->
            val day = parseInstant(record.date)?.atZone(zone)?.toLocalDate() ?: parseDate(record.date) ?: return@mapNotNull null
            day to record
        }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (day, dayRecords) ->
                val target = dayRecords.maxOf { it.maxCount }.coerceIn(1, MAX_HABIT_TARGET)
                val count = dayRecords.maxOf { it.count }.coerceIn(0, target)
                if (count == 0) null else HabitCheckEntity(habitId = 0, date = day, count = count, target = target)
            }
            .sortedBy { it.date }

    private fun categoryIcon(name: String?): CategoryIcon = when (name) {
        "briefcase" -> CategoryIcon.Work
        "home", "broom" -> CategoryIcon.Home
        "cart", "pizza" -> CategoryIcon.Shopping
        "sport_1", "yoga_1", "patch", "heartbeat" -> CategoryIcon.Health
        "education" -> CategoryIcon.Study
        "banknotes", "balance" -> CategoryIcon.Finance
        "heart", "face_cream" -> CategoryIcon.Heart
        "reward", "flame" -> CategoryIcon.Star
        "car_1" -> CategoryIcon.Car
        else -> CategoryIcon.Tag
    }

    private fun habitIcon(name: String?): HabitIcon = when (name) {
        "drop" -> HabitIcon.Water
        "shower" -> HabitIcon.Shower
        "fitness_center" -> HabitIcon.Run
        "vitamins_2" -> HabitIcon.Pill
        "hotel" -> HabitIcon.Sleep
        "heartbeat", "life-ring" -> HabitIcon.Heart
        "wb_sunny" -> HabitIcon.Sun
        "money", "fastfood" -> HabitIcon.Goal
        else -> HabitIcon.Sprout
    }

    /** Номер ближайшего цвета палитры к ARGB [color] — по расстоянию в RGB. */
    internal fun nearest(color: Int, palette: List<Int>): Int = palette.indices.minBy { index ->
        val other = palette[index]
        val dr = ((color shr 16) and 0xFF) - ((other shr 16) and 0xFF)
        val dg = ((color shr 8) and 0xFF) - ((other shr 8) and 0xFF)
        val db = (color and 0xFF) - (other and 0xFF)
        dr * dr + dg * dg + db * db
    }

    private fun parseDate(text: String?): LocalDate? =
        text?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** «16:0», «9:30» → время. */
    private fun parseTime(text: String?): LocalTime? {
        val parts = text?.split(':') ?: return null
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
    }

    private fun parseInstant(text: String?): Instant? = text?.let { runCatching { Instant.parse(it) }.getOrNull() }

    /** Цвета палитры категорий темы (`ChekkiTheme.colors.categories`) — в том же порядке. */
    private val CategoryPalette = listOf(
        0xFF8AB4FF, 0xFFF29BC1, 0xFFF5BE6A, 0xFF7BD49A, 0xFFB89CFF, 0xFF6FD3CF,
        0xFFFF9A7B, 0xFFC8E07A, 0xFF6FC6F2, 0xFFE58BEA, 0xFFD7B48C, 0xFFB3B9C5
    ).map { it.toInt() }

    /** Заливки палитры привычек темы (`ChekkiTheme.colors.habits`) — в том же порядке. */
    private val HabitPalette = listOf(
        0xFF2C7FA8, 0xFF4A55C9, 0xFF16857C, 0xFFF0924A, 0xFFC2477A, 0xFF7E4FC9, 0xFF3F8F45, 0xFFD9A93A
    ).map { it.toInt() }

    private val ALL_DAY_REMINDER: LocalTime = LocalTime.of(9, 0)
    private const val MAX_REMINDER_MINUTES = 7L * 24 * 60
    private const val MAX_HABIT_TARGET = 99
    private val DeadlineFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru"))
}
