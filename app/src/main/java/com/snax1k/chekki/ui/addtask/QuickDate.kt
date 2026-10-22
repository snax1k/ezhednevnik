package com.snax1k.chekki.ui.addtask

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Быстрый выбор даты в панели новой задачи. */
enum class QuickDate {
    Today,
    Tomorrow,
    Weekend,
    NoDate;

    /** Дата задачи для дня [today]; null — без даты. */
    fun resolve(today: LocalDate): LocalDate? = when (this) {
        Today -> today
        Tomorrow -> today.plusDays(1)
        // Ближайшая суббота; если выходные уже идут — сегодня
        Weekend ->
            if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY) today
            else today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        NoDate -> null
    }

    companion object {
        /**
         * Плашка для даты из календаря. Если дата подходит под несколько плашек
         * (в пятницу завтра — это и выходные), побеждает первая: «Сегодня», «Завтра», «На выходных».
         */
        fun matching(date: LocalDate, today: LocalDate): QuickDate? =
            listOf(Today, Tomorrow, Weekend).firstOrNull { it.resolve(today) == date }
    }
}
