package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Дата следующего повтора: первая дата по правилу позже [after], но не раньше [notBefore].
 * [notBefore] нужен для просроченных задач: если ежедневную задачу с понедельника выполнили в четверг,
 * следующая копия встанет на четверг, а не на вторник. null — задача не повторяется.
 *
 * Ограничение: ежемесячная задача на 31-е после февраля переедет на 28-е и дальше останется на 28-м,
 * потому что каждая копия считается от даты предыдущей.
 */
fun RepeatRule.nextOccurrence(after: LocalDate, notBefore: LocalDate): LocalDate? {
    val step = every.coerceAtLeast(1).toLong()
    // Даты считаются от исходной, а не друг от друга: 31 января + 2 месяца = 31 марта, а не 28-е
    val occurrences: Sequence<LocalDate> = when (unit) {
        RepeatUnit.None -> return null
        RepeatUnit.Day -> multiples().map { after.plusDays(step * it) }
        RepeatUnit.Week ->
            if (weekdays == 0) multiples().map { after.plusWeeks(step * it) }
            else generateSequence(nextSelectedWeekday(after, step)) { nextSelectedWeekday(it, step) }
        RepeatUnit.Month -> multiples().map { after.plusMonths(step * it) }
        RepeatUnit.Year -> multiples().map { after.plusYears(step * it) }
    }
    return occurrences.first { it >= notBefore }
}

private fun multiples(): Sequence<Long> = generateSequence(1L) { it + 1 }

/** Следующий отмеченный день недели после [from]; недели без отметок пропускаются по [everyWeeks]. */
private fun RepeatRule.nextSelectedWeekday(from: LocalDate, everyWeeks: Long): LocalDate {
    // Сначала ищем до конца текущей недели
    var day = from.plusDays(1)
    while (day.dayOfWeek != DayOfWeek.MONDAY) {
        if (isSelected(day.dayOfWeek)) return day
        day = day.plusDays(1)
    }
    // Иначе первый отмеченный день через everyWeeks недель; day сейчас — понедельник следующей недели
    val weekStart = day.plusWeeks(everyWeeks - 1)
    for (offset in 0L until 7L) {
        val candidate = weekStart.plusDays(offset)
        if (isSelected(candidate.dayOfWeek)) return candidate
    }
    // В маске нет ни одного дня недели — повторяем раз в everyWeeks недель
    return from.plusWeeks(everyWeeks)
}

/** Бит 0 — понедельник, …, бит 6 — воскресенье. */
private fun RepeatRule.isSelected(dayOfWeek: DayOfWeek): Boolean =
    weekdays and (1 shl (dayOfWeek.value - 1)) != 0
