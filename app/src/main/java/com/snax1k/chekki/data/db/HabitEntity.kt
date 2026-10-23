package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Привычка: круг наверху главного экрана. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val icon: HabitIcon,
    /** Номер цвета в палитре привычек темы (`ChekkiTheme.colors.habits`), а не сам цвет. */
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    /** Сколько раз в день: 1 — один раз, 7 — например, семь стаканов воды. */
    @ColumnInfo(name = "daily_target") val dailyTarget: Int,
    /** Дни недели битами, как у повтора задач: бит 0 — понедельник, …, бит 6 — воскресенье. */
    val weekdays: Int = EVERY_DAY,
    /** Порядок кругов наверху. */
    val position: Int,
    /** В архиве: наверху не показывается, история отметок остаётся. */
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant
) {
    fun isScheduledOn(date: LocalDate): Boolean = weekdays and date.dayOfWeek.bit != 0

    companion object {
        /** Все семь дней недели. */
        const val EVERY_DAY = 0b1111111
    }
}

/** Бит дня недели в маске [HabitEntity.weekdays]: понедельник — 1, вторник — 2, …, воскресенье — 64. */
val DayOfWeek.bit: Int get() = 1 shl (value - 1)

/**
 * Отметки привычки за один день. Строка появляется при первой отметке.
 * [target] — цель того дня: если цель привычки потом поменяют, прошлые дни останутся как были.
 */
@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habit_id", "date"],
    foreignKeys = [
        // Удалили привычку — удаляются и её отметки
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class HabitCheckEntity(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val date: LocalDate,
    /** Сколько раз отмечено, не больше [target]. */
    val count: Int,
    val target: Int
)

/** Привычка вместе с отметками за день. */
data class HabitWithCount(
    @Embedded val habit: HabitEntity,
    @ColumnInfo(name = "day_count") val count: Int
)

/**
 * Иконки привычек. У значений фиксированные коды для базы, как у приоритета задач:
 * коды менять нельзя, новые иконки — только с новыми кодами.
 */
enum class HabitIcon(val code: Int) {
    Sprout(1),
    Water(2),
    Goal(3),
    Stretch(4),
    Run(5),
    Book(6),
    Pill(7),
    Sleep(8),
    Tooth(9),
    Shower(10),
    Heart(11),
    Sun(12);

    companion object {
        /** Иконка по коду; незнакомый код (например, из более новой версии) — росток. */
        fun fromCode(code: Int): HabitIcon = entries.firstOrNull { it.code == code } ?: Sprout
    }
}

/** Время напоминания привычки; у привычки их может быть несколько в день. */
@Entity(
    tableName = "habit_reminders",
    primaryKeys = ["habit_id", "time"],
    foreignKeys = [
        // Удалили привычку — удаляются и её напоминания
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitReminderEntity(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val time: LocalTime
)
