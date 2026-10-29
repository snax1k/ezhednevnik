package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Задача. */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        // Если удалить категорию, задачи остаются, просто без категории
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["repeated_from_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("date"), Index("is_done"), Index("category_id"), Index("repeated_from_id")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** Дата задачи; null — группа «Без даты». */
    val date: LocalDate?,
    /** Время задачи; null — на весь день. */
    val time: LocalTime?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    /** Уровень важности: id в `priority_levels`, [PriorityLevels.NONE] — без приоритета. */
    val priority: Long = PriorityLevels.NONE,
    @Embedded(prefix = "repeat_") val repeat: RepeatRule = RepeatRule.None,
    /** За сколько минут до времени задачи напомнить; null — без напоминания. */
    @ColumnInfo(name = "reminder_minutes_before") val reminderMinutesBefore: Int? = null,
    @ColumnInfo(name = "is_done") val isDone: Boolean = false,
    /** Когда задачу отметили выполненной: для группы «Выполненное». */
    @ColumnInfo(name = "completed_at") val completedAt: Instant? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    /**
     * Из какой задачи создана эта копия при выполнении повтора. Нужно, чтобы убрать копию,
     * если с исходной задачи сняли отметку.
     */
    @ColumnInfo(name = "repeated_from_id") val repeatedFromId: Long? = null
)

/**
 * Правило повтора: колонки repeat_unit, repeat_every, repeat_weekdays.
 * Когда повторяющуюся задачу выполняют, она остаётся выполненной, а рядом создаётся копия на следующую дату.
 */
data class RepeatRule(
    val unit: RepeatUnit,
    /** Каждые сколько единиц: 1 — каждый день (неделю, …), квартал — 3 месяца. */
    val every: Int,
    /**
     * Дни недели для еженедельного повтора, битовая маска: бит 0 — понедельник, …, бит 6 — воскресенье.
     * 0 — в тот же день недели, что и дата задачи.
     */
    val weekdays: Int
) {
    companion object {
        val None = RepeatRule(unit = RepeatUnit.None, every = 1, weekdays = 0)
    }
}

// У значений фиксированные коды для базы, чтобы переименование в коде её не портило.
// Коды менять нельзя, новые значения — только с новыми кодами.

enum class RepeatUnit(val code: Int) {
    None(0),
    Day(1),
    Week(2),
    Month(3),
    Year(4);

    companion object {
        fun fromCode(code: Int): RepeatUnit = entries.firstOrNull { it.code == code } ?: None
    }
}
