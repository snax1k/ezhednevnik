package com.snax1k.chekki.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Тестовые привычки для отладочной сборки, как круги в макете: «Вода 2/6» и другие.
 * Пять привычек на каждый день — чтобы ряд наверху прокручивался, и одна только на выходные.
 * В выпускной сборке вместо этого файла пустой из src/release.
 */
internal object SampleHabits {

    fun insert(db: SupportSQLiteDatabase, today: LocalDate) {
        val converters = Converters()
        var position = 0

        fun habit(title: String, icon: HabitIcon, colorIndex: Int, target: Int, weekdays: Int = HabitEntity.EVERY_DAY): Long =
            db.insert(
                "habits",
                SQLiteDatabase.CONFLICT_ABORT,
                contentValuesOf(
                    "title" to title,
                    "icon" to converters.habitIconToCode(icon),
                    "color_index" to colorIndex,
                    "daily_target" to target,
                    "weekdays" to weekdays,
                    "position" to position++,
                    "is_archived" to false,
                    "created_at" to converters.instantToMillis(Instant.now())
                )
            )

        fun check(habitId: Long, date: LocalDate, count: Int, target: Int) {
            db.insert(
                "habit_checks",
                SQLiteDatabase.CONFLICT_ABORT,
                contentValuesOf(
                    "habit_id" to habitId,
                    "date" to converters.localDateToEpochDay(date),
                    "count" to count,
                    "target" to target
                )
            )
        }

        habit("Утренняя рутина", HabitIcon.Sprout, colorIndex = 0, target = 1)
        val water = habit("Пить воду", HabitIcon.Water, colorIndex = 1, target = 6)
        val english = habit("Английский", HabitIcon.Goal, colorIndex = 2, target = 2)
        habit("Растяжка", HabitIcon.Stretch, colorIndex = 3, target = 1)
        habit("Витамины", HabitIcon.Pill, colorIndex = 4, target = 1)
        habit("Бассейн", HabitIcon.Run, colorIndex = 5, target = 1, weekdays = DayOfWeek.SATURDAY.bit or DayOfWeek.SUNDAY.bit)

        check(water, today, count = 2, target = 6)
        check(english, today, count = 1, target = 2)
        // Вчера воду выпили полностью — пригодится истории в карточке привычки
        check(water, today.minusDays(1), count = 6, target = 6)
    }
}
