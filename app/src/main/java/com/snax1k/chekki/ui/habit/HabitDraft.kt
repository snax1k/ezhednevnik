package com.snax1k.chekki.ui.habit

import com.snax1k.chekki.data.distributeReminders
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.bit
import java.time.DayOfWeek
import java.time.LocalTime

/** Поля панели привычки. Правила (цель от 1 до 99, хотя бы один день) — в методах with…, они проверяются тестами. */
data class HabitDraft(
    val title: String = "",
    val icon: HabitIcon = HabitIcon.Sprout,
    val colorIndex: Int = 0,
    val dailyTarget: Int = 1,
    val weekdays: Int = HabitEntity.EVERY_DAY,
    /** Времена напоминаний по порядку, без повторов. */
    val reminderTimes: List<LocalTime> = emptyList()
) {
    val canSave: Boolean get() = title.isNotBlank() && weekdays != 0

    fun isOn(day: DayOfWeek): Boolean = weekdays and day.bit != 0

    fun withTarget(target: Int): HabitDraft = copy(dailyTarget = target.coerceIn(MIN_TARGET, MAX_TARGET))

    /** Включает или выключает день. Последний оставшийся день выключить нельзя: привычка без дней не нужна. */
    fun withDayToggled(day: DayOfWeek): HabitDraft {
        val toggled = weekdays xor day.bit
        return if (toggled == 0) this else copy(weekdays = toggled)
    }

    /** Новое время напоминания; если такое уже есть, ничего не меняется. */
    fun withReminderAdded(time: LocalTime): HabitDraft = copy(reminderTimes = (reminderTimes + time.atMinute()).distinct().sorted())

    /** Время [old] заменяется на [new]; совпавшие схлопываются. */
    fun withReminderChanged(old: LocalTime, new: LocalTime): HabitDraft =
        copy(reminderTimes = (reminderTimes - old + new.atMinute()).distinct().sorted())

    fun withReminderRemoved(time: LocalTime): HabitDraft = copy(reminderTimes = reminderTimes - time)

    /** «Распределить по дню»: столько напоминаний, сколько раз в день, равномерно с 9:00 до 21:00. Прежние заменяются. */
    fun withRemindersDistributed(): HabitDraft = copy(reminderTimes = distributeReminders(dailyTarget))

    companion object {
        const val MIN_TARGET = 1
        const val MAX_TARGET = 99

        fun of(habit: HabitEntity, reminderTimes: List<LocalTime>) = HabitDraft(
            title = habit.title,
            icon = habit.icon,
            colorIndex = habit.colorIndex,
            dailyTarget = habit.dailyTarget,
            weekdays = habit.weekdays,
            reminderTimes = reminderTimes.sorted()
        )
    }
}

/** Подпись к дням недели: «Каждый день», «По будням», «По выходным»; null — свой набор дней. */
enum class HabitDaysPreset(val mask: Int) {
    EveryDay(HabitEntity.EVERY_DAY),
    Weekdays(DayOfWeek.entries.take(5).sumOf { it.bit }),
    Weekend(DayOfWeek.SATURDAY.bit or DayOfWeek.SUNDAY.bit);

    companion object {
        fun of(mask: Int): HabitDaysPreset? = entries.firstOrNull { it.mask == mask }
    }
}

/** Напоминания с точностью до минуты: секунды из циферблата не приходят, но на всякий случай. */
private fun LocalTime.atMinute(): LocalTime = withSecond(0).withNano(0)
