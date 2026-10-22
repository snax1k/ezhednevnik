package com.snax1k.chekki.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Границы групп главного экрана для дня [today]. Неделя начинается с понедельника.
 * Каждая дата после сегодняшней попадает ровно в одну группу: «Завтра», «На неделе», «В этом месяце» или «Потом».
 * Диапазон может быть пустым (начало позже конца): например, в субботу и воскресенье группа «На неделе» пустая.
 */
data class GroupRanges(
    val today: LocalDate,
    val tomorrow: LocalDate,
    /** С послезавтра до воскресенья этой недели. */
    val thisWeek: ClosedRange<LocalDate>,
    /** После этой недели (но не раньше послезавтра) до конца месяца. */
    val thisMonth: ClosedRange<LocalDate>,
    /** Всё, что дальше, начиная с этой даты. */
    val laterFrom: LocalDate
) {
    companion object {
        fun of(today: LocalDate): GroupRanges {
            val tomorrow = today.plusDays(1)
            val afterTomorrow = today.plusDays(2)
            val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
            val monthStart = maxOf(afterTomorrow, weekEnd.plusDays(1))
            return GroupRanges(
                today = today,
                tomorrow = tomorrow,
                thisWeek = afterTomorrow..weekEnd,
                thisMonth = monthStart..monthEnd,
                laterFrom = maxOf(monthStart, monthEnd.plusDays(1))
            )
        }
    }
}
