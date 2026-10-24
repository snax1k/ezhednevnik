package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.CalendarTaskMark
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Точки под числом в календаре. [openColors] — цвета категорий невыполненных задач дня по порядку,
 * без повторов, не больше [MAX_DOTS]; null в списке — задача без категории.
 * [allDone] — задачи в этот день были, и все выполнены: одна серая точка.
 */
data class DayMarks(val openColors: List<Int?>, val allDone: Boolean) {
    companion object {
        const val MAX_DOTS = 3
    }
}

/** Точки по дням из задач периода. Дни без задач в ответ не попадают. */
fun calendarMarks(tasks: List<CalendarTaskMark>): Map<LocalDate, DayMarks> =
    tasks.groupBy { it.date }.mapValues { (_, dayTasks) ->
        val open = dayTasks.filter { !it.isDone }
        DayMarks(
            openColors = open.map { it.categoryColorIndex }.distinct().take(DayMarks.MAX_DOTS),
            allDone = open.isEmpty()
        )
    }

/**
 * Дни сетки месяца: всегда шесть недель по семь дней, с понедельника, чтобы высота сетки не прыгала
 * при листании. Начало — понедельник на неделе 1-го числа, лишние дни — из соседних месяцев.
 */
fun monthGrid(month: YearMonth): List<LocalDate> {
    val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until 42).map { start.plusDays(it.toLong()) }
}
