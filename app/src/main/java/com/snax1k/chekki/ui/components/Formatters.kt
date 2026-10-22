package com.snax1k.chekki.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RussianLocale: Locale = Locale.forLanguageTag("ru")
private val DayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", RussianLocale)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ShortWeekdayFormatter = DateTimeFormatter.ofPattern("EEE", RussianLocale)
private val DayMonthShortFormatter = DateTimeFormatter.ofPattern("d MMM", RussianLocale)
private val DayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", RussianLocale)

/** «Четверг, 22 октября». */
fun formatDayTitle(date: LocalDate): String =
    date.format(DayTitleFormatter).replaceFirstChar { it.titlecase(RussianLocale) }

/** «07:30». */
fun formatTime(time: LocalTime): String = time.format(TimeFormatter)

/** «Сб». */
fun formatShortWeekday(date: LocalDate): String =
    date.format(ShortWeekdayFormatter).replaceFirstChar { it.titlecase(RussianLocale) }

/** «24 окт» — без точки сокращения, как в макете. */
fun formatDayMonthShort(date: LocalDate): String = date.format(DayMonthShortFormatter).removeSuffix(".")

/** «Пн, 26 окт», в другом году — «Пн, 26 окт 2027». */
fun formatShortDate(date: LocalDate, today: LocalDate): String {
    val text = "${formatShortWeekday(date)}, ${formatDayMonthShort(date)}"
    return if (date.year == today.year) text else "$text ${date.year}"
}

/** «1 ноября», в другом году — «1 января 2027». */
fun formatDayMonth(date: LocalDate, today: LocalDate): String {
    val text = date.format(DayMonthFormatter)
    return if (date.year == today.year) text else "$text ${date.year}"
}

/** «24–25 октября», через границу месяца — «31 октября – 1 ноября», один день — «24 октября». */
fun formatDateRange(range: ClosedRange<LocalDate>): String {
    val start = range.start
    val end = range.endInclusive
    return when {
        start == end -> end.format(DayMonthFormatter)
        start.year == end.year && start.month == end.month -> "${start.dayOfMonth}–${end.format(DayMonthFormatter)}"
        else -> "${start.format(DayMonthFormatter)} – ${end.format(DayMonthFormatter)}"
    }
}
