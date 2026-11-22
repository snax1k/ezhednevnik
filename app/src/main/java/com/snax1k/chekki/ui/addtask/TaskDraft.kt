package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.data.AllDayReminderTime
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.PriorityLevels
import java.time.LocalDate
import java.time.LocalTime

/**
 * Черновик новой задачи. Как одно поле влияет на другие (например, «Без даты» сбрасывает время),
 * описано в методах with…, чтобы правила были в одном месте и проверялись тестами.
 */
data class TaskDraft(
    val title: String = "",
    val note: String = "",
    /** Выбранная плашка даты; null — дата выбрана в календаре и лежит в [exactDate]. */
    val quickDate: QuickDate? = QuickDate.Today,
    val exactDate: LocalDate? = null,
    /** Время задачи; null — на весь день. */
    val time: LocalTime? = null,
    val categoryId: Long? = null,
    /** Уровень важности: id в `priority_levels`, [PriorityLevels.NONE] — без приоритета. */
    val priority: Long = PriorityLevels.NONE,
    val repeat: RepeatRule = RepeatRule.None,
    /**
     * За сколько минут напомнить; null — без напоминания.
     * У задачи на весь день отсчёт от [AllDayReminderTime], варианты — из [ReminderOptions.allDay].
     */
    val reminderMinutesBefore: Int? = null,
    val subtasks: List<SubtaskDraft> = emptyList()
) {
    val canSave: Boolean get() = title.isNotBlank()

    val hasDate: Boolean get() = quickDate != QuickDate.NoDate

    /** Подзадачи, которые попадут в базу: без пустых и без пробелов по краям. */
    val filledSubtasks: List<String> get() = subtasks.map { it.title.trim() }.filter { it.isNotEmpty() }

    /** Дата задачи для дня [today]; null — без даты. */
    fun dateFor(today: LocalDate): LocalDate? = quickDate?.resolve(today) ?: exactDate

    /** Плашка даты. «Без даты» сбрасывает время, повтор и напоминание: без даты они не имеют смысла. */
    fun withQuickDate(quickDate: QuickDate): TaskDraft =
        if (quickDate == QuickDate.NoDate) {
            copy(
                quickDate = quickDate,
                exactDate = null,
                time = null,
                repeat = RepeatRule.None,
                reminderMinutesBefore = null
            )
        } else {
            copy(quickDate = quickDate, exactDate = null)
        }

    /** Дата из календаря. Сегодня, завтра и ближайшая суббота превращаются в свои плашки. */
    fun withDate(date: LocalDate, today: LocalDate): TaskDraft {
        val quickDate = QuickDate.matching(date, today)
        return copy(quickDate = quickDate, exactDate = if (quickDate == null) date else null)
    }

    /**
     * Время; null — на весь день. У задачи на весь день напоминание «за 15 минут» теряет смысл,
     * поэтому оно становится «в день задачи».
     */
    fun withTime(time: LocalTime?): TaskDraft {
        val reminder = reminderMinutesBefore
        val fitted = if (time == null && reminder != null && reminder !in ReminderOptions.allDay) 0 else reminder
        return copy(time = time, reminderMinutesBefore = fitted)
    }

    /**
     * Повтор из окна «Повтор». [nextDate] — «Следующая задача», она становится датой задачи.
     * Без даты повторять нечего, поэтому если даты нет и [nextDate] не дан — «Сегодня».
     * «Не повторять» дату не меняет.
     */
    fun withRepeat(repeat: RepeatRule, nextDate: LocalDate? = null, today: LocalDate = LocalDate.now()): TaskDraft {
        val withRule = copy(repeat = repeat)
        return when {
            repeat == RepeatRule.None -> withRule
            nextDate != null -> withRule.withDate(nextDate, today)
            else -> withRule.withTodayIfNoDate(needed = true)
        }
    }

    /** Напоминание. Без даты напоминать некогда, поэтому задача без даты получает дату «Сегодня». */
    fun withReminder(minutesBefore: Int?): TaskDraft =
        copy(reminderMinutesBefore = minutesBefore).withTodayIfNoDate(needed = minutesBefore != null)

    private fun withTodayIfNoDate(needed: Boolean): TaskDraft =
        if (needed && !hasDate) copy(quickDate = QuickDate.Today) else this
}

/** Подзадача в черновике. [id] нужен только панели: чтобы строки не путались и курсор вставал в новую. */
data class SubtaskDraft(
    val id: Long,
    val title: String = ""
)

/** Варианты напоминания: за сколько минут до времени задачи. */
object ReminderOptions {
    /** Для задачи со временем: в момент начала, за 5, 15, 30 минут, за час, за день. */
    val timed: List<Int> = listOf(0, 5, 15, 30, 60, 24 * 60)

    /** Для задачи на весь день: в день задачи и накануне, оба в [AllDayReminderTime]. */
    val allDay: List<Int> = listOf(0, 24 * 60)
}
