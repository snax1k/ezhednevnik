package com.snax1k.chekki.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Как типы приложения хранятся в базе. Пустые значения (null) Room обрабатывает сам. */
class Converters {
    // Дата — число дней от 1 января 1970: удобно сравнивать и искать по диапазону
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    // Время — минуты от полуночи, секунды не храним
    @TypeConverter
    fun localTimeToMinutes(time: LocalTime): Int = time.toSecondOfDay() / 60

    @TypeConverter
    fun minutesToLocalTime(minutes: Int): LocalTime = LocalTime.ofSecondOfDay(minutes * 60L)

    @TypeConverter
    fun instantToMillis(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun millisToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

    @TypeConverter
    fun priorityToCode(priority: TaskPriority): Int = priority.code

    @TypeConverter
    fun codeToPriority(code: Int): TaskPriority = TaskPriority.fromCode(code)

    @TypeConverter
    fun repeatUnitToCode(unit: RepeatUnit): Int = unit.code

    @TypeConverter
    fun codeToRepeatUnit(code: Int): RepeatUnit = RepeatUnit.fromCode(code)

    @TypeConverter
    fun habitIconToCode(icon: HabitIcon): Int = icon.code

    @TypeConverter
    fun codeToHabitIcon(code: Int): HabitIcon = HabitIcon.fromCode(code)

    @TypeConverter
    fun categoryIconToCode(icon: CategoryIcon): Int = icon.code

    @TypeConverter
    fun codeToCategoryIcon(code: Int): CategoryIcon = CategoryIcon.fromCode(code)
}
