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
