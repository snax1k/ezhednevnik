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
 * Правило повтора: колонки repeat_unit, repeat_every, repeat_weekdays, repeat_weekdays2, repeat_month_days, repeat_until.
 * Когда повторяющуюся задачу выполняют, она остаётся выполненной, а рядом создаётся копия на следующую дату.
 *
 * Виды правил (окно «Повтор»):
 * - по дням недели — [RepeatUnit.Week] и [weekdays] (каждые [every] недель);
 * - две недели по-разному — [RepeatUnit.TwoWeeks]: [weekdays] — дни чётных недель, [weekdays2] — нечётных
 *   (номер недели с понедельника 29.12.1969, см. `weekParity`), так правило не зависит от даты задачи;
 * - по числам месяца — [RepeatUnit.Month] и [monthDays] (каждые [every] месяцев);
 * - свой период — «каждые [every] дней / недель / месяцев / лет» от даты задачи, без дней и чисел.
 */
data class RepeatRule(
    val unit: RepeatUnit,
    /** Каждые сколько единиц: 1 — каждый день (неделю, …), квартал — 3 месяца. */
    val every: Int,
    /**
     * Дни недели, битовая маска: бит 0 — понедельник, …, бит 6 — воскресенье.
     * 0 — в тот же день недели, что и дата задачи. У [RepeatUnit.TwoWeeks] — дни чётных недель.
     */
    val weekdays: Int,
    /** Только у [RepeatUnit.TwoWeeks]: дни нечётных недель, та же маска. С 8-й схемы. */
    @ColumnInfo(defaultValue = "0") val weekdays2: Int = 0,
    /**
     * Числа месяца у [RepeatUnit.Month]: бит 0 — 1-е, …, бит 30 — 31-е, которое значит «последний день месяца».
     * 29-го и 30-го в коротких месяцах нет — такие дни пропускаются. 0 — то же число, что у даты задачи. С 8-й схемы.
     */
    @ColumnInfo(name = "month_days", defaultValue = "0") val monthDays: Int = 0,
    /** Последний день, на который ещё создаётся копия; null — бессрочно. С 8-й схемы. */
    val until: LocalDate? = null
) {
    companion object {
        val None = RepeatRule(unit = RepeatUnit.None, every = 1, weekdays = 0)

        /** Все семь дней недели. */
        const val ALL_WEEKDAYS = 0b1111111

        /** Бит 31-го числа: последний день месяца. */
        const val LAST_MONTH_DAY_BIT = 1 shl 30
    }
}

// У значений фиксированные коды для базы, чтобы переименование в коде её не портило.
// Коды менять нельзя, новые значения — только с новыми кодами.

enum class RepeatUnit(val code: Int) {
    None(0),
    Day(1),
    Week(2),
    Month(3),
    Year(4),

    /** Две недели по-разному (с 8-й схемы). */
    TwoWeeks(5);

    companion object {
        fun fromCode(code: Int): RepeatUnit = entries.firstOrNull { it.code == code } ?: None
    }
}
