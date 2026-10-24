package com.snax1k.chekki.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Фильтр поиска по дате задачи («Любая дата ▾»). Неделя — с понедельника, как везде в приложении. */
sealed interface DateFilter {
    data object Any : DateFilter

    /** Не выполнены, дата раньше сегодня — как группа «Просроченные». */
    data object Overdue : DateFilter
    data object Today : DateFilter

    /** С понедельника по воскресенье этой недели, включая прошедшие дни. */
    data object ThisWeek : DateFilter

    /** С 1-го числа до конца месяца, включая прошедшие дни. */
    data object ThisMonth : DateFilter
    data object NoDate : DateFilter

    /** Свой период, оба дня включительно. */
    data class Period(val from: LocalDate, val to: LocalDate) : DateFilter

    /** Подходит ли задача с датой [date] (null — без даты). */
    fun accepts(date: LocalDate?, isDone: Boolean, today: LocalDate): Boolean = when (this) {
        Any -> true
        Overdue -> date != null && date < today && !isDone
        Today -> date == today
        ThisWeek -> date != null && date in weekOf(today)
        ThisMonth -> date != null && date.year == today.year && date.month == today.month
        NoDate -> date == null
        is Period -> date != null && date in from..to
    }

    private fun weekOf(today: LocalDate): ClosedRange<LocalDate> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday..monday.plusDays(6)
    }
}
