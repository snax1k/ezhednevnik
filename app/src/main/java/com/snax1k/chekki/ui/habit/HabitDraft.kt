package com.snax1k.chekki.ui.habit

import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.bit
import java.time.DayOfWeek

/** Поля панели привычки. Правила (цель от 1 до 99, хотя бы один день) — в методах with…, они проверяются тестами. */
data class HabitDraft(
    val title: String = "",
    val icon: HabitIcon = HabitIcon.Sprout,
    val colorIndex: Int = 0,
    val dailyTarget: Int = 1,
    val weekdays: Int = HabitEntity.EVERY_DAY
) {
    val canSave: Boolean get() = title.isNotBlank() && weekdays != 0

    fun isOn(day: DayOfWeek): Boolean = weekdays and day.bit != 0

    fun withTarget(target: Int): HabitDraft = copy(dailyTarget = target.coerceIn(MIN_TARGET, MAX_TARGET))

    /** Включает или выключает день. Последний оставшийся день выключить нельзя: привычка без дней не нужна. */
    fun withDayToggled(day: DayOfWeek): HabitDraft {
        val toggled = weekdays xor day.bit
        return if (toggled == 0) this else copy(weekdays = toggled)
    }

    companion object {
        const val MIN_TARGET = 1
        const val MAX_TARGET = 99

        fun of(habit: HabitEntity) = HabitDraft(
            title = habit.title,
            icon = habit.icon,
            colorIndex = habit.colorIndex,
            dailyTarget = habit.dailyTarget,
            weekdays = habit.weekdays
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
