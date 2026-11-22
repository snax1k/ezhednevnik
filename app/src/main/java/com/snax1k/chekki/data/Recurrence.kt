package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Дата следующего повтора: первая дата по правилу позже [after], но не раньше [notBefore].
 * [notBefore] нужен для просроченных задач: если ежедневную задачу с понедельника выполнили в четверг,
 * следующая копия встанет на четверг, а не на вторник. null — задача не повторяется
 * или следующая дата позже [RepeatRule.until].
 *
 * Ограничение: ежемесячная задача «свой период» на 31-е после февраля переедет на 28-е и дальше останется на 28-м,
 * потому что каждая копия считается от даты предыдущей. Чтобы всегда был последний день — выбрать 31-е в «По числам».
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
        RepeatUnit.TwoWeeks -> generateSequence(nextTwoWeeksDay(after)) { nextTwoWeeksDay(it) }
        RepeatUnit.Month ->
            if (monthDays == 0) multiples().map { after.plusMonths(step * it) }
            else generateSequence(nextSelectedMonthDay(after, step)) { nextSelectedMonthDay(it, step) }
        RepeatUnit.Year -> multiples().map { after.plusYears(step * it) }
    }
    val next = occurrences.first { it >= notBefore }
    return next.takeIf { until == null || it <= until }
}

/**
 * Подходит ли [date] под дни правила. У «своего периода» (каждые N дней и т. п.) подходит любая дата:
 * отсчёт идёт от даты задачи.
 */
fun RepeatRule.matches(date: LocalDate): Boolean = when (unit) {
    RepeatUnit.None -> false
    RepeatUnit.Week -> weekdays == 0 || isSelected(weekdays, date.dayOfWeek)
    RepeatUnit.TwoWeeks -> isSelected(twoWeeksMask(date), date.dayOfWeek)
    RepeatUnit.Month -> monthDays == 0 || isSelectedMonthDay(date)
    RepeatUnit.Day, RepeatUnit.Year -> true
}

/**
 * Ближайшая дата задачи по правилу, начиная с [from] включительно: сама [from], если подходит, иначе следующая.
 * Это «Следующая задача» в окне «Повтор». Срок [RepeatRule.until] здесь не учитывается.
 */
fun RepeatRule.firstOnOrAfter(from: LocalDate): LocalDate? {
    if (unit == RepeatUnit.None) return null
    if (matches(from)) return from
    return copy(until = null).nextOccurrence(after = from, notBefore = from)
}

/**
 * Чётность недели: 0 или 1. Недели считаются с понедельника 29.12.1969 (день −3 от 1 января 1970),
 * поэтому у одной и той же даты чётность всегда одна, где бы ни была задача.
 */
fun weekParity(date: LocalDate): Int = Math.floorMod(Math.floorDiv(date.toEpochDay() + 3, 7L), 2L).toInt()

private fun multiples(): Sequence<Long> = generateSequence(1L) { it + 1 }

/** Следующий отмеченный день недели после [from]; недели без отметок пропускаются по [everyWeeks]. */
private fun RepeatRule.nextSelectedWeekday(from: LocalDate, everyWeeks: Long): LocalDate {
    // Сначала ищем до конца текущей недели
    var day = from.plusDays(1)
    while (day.dayOfWeek != DayOfWeek.MONDAY) {
        if (isSelected(weekdays, day.dayOfWeek)) return day
        day = day.plusDays(1)
    }
    // Иначе первый отмеченный день через everyWeeks недель; day сейчас — понедельник следующей недели
    val weekStart = day.plusWeeks(everyWeeks - 1)
    for (offset in 0L until 7L) {
        val candidate = weekStart.plusDays(offset)
        if (isSelected(weekdays, candidate.dayOfWeek)) return candidate
    }
    // В маске нет ни одного дня недели — повторяем раз в everyWeeks недель
    return from.plusWeeks(everyWeeks)
}

/** Две недели по-разному: следующий день после [from], отмеченный в маске своей недели. */
private fun RepeatRule.nextTwoWeeksDay(from: LocalDate): LocalDate {
    // За две недели встретятся обе маски; если обе пустые — раз в две недели
    for (offset in 1L..14L) {
        val candidate = from.plusDays(offset)
        if (isSelected(twoWeeksMask(candidate), candidate.dayOfWeek)) return candidate
    }
    return from.plusWeeks(2)
}

private fun RepeatRule.twoWeeksMask(date: LocalDate): Int = if (weekParity(date) == 0) weekdays else weekdays2

/**
 * По числам месяца: следующее отмеченное число после [from]. Сначала в месяце [from],
 * потом через каждые [everyMonths] месяцев.
 */
private fun RepeatRule.nextSelectedMonthDay(from: LocalDate, everyMonths: Long): LocalDate {
    val start = YearMonth.from(from)
    // Предел — четыре года: за это время любое число 1–31 встретится
    var month = start
    while (ChronoUnit.MONTHS.between(start, month) <= 48 * everyMonths) {
        val candidate = selectedDays(month).firstOrNull { it > from }
        if (candidate != null) return candidate
        month = month.plusMonths(everyMonths)
    }
    return from.plusMonths(everyMonths)
}

/** Отмеченные числа месяца по порядку; 31-е — последний день, даже если в месяце 28–30 дней. */
private fun RepeatRule.selectedDays(month: YearMonth): List<LocalDate> =
    (1..31).filter { day -> monthDays and (1 shl (day - 1)) != 0 }
        .mapNotNull { day ->
            when {
                day == 31 -> month.atEndOfMonth()
                day <= month.lengthOfMonth() -> month.atDay(day)
                else -> null
            }
        }
        .distinct()
        .sorted()

private fun RepeatRule.isSelectedMonthDay(date: LocalDate): Boolean = date in selectedDays(YearMonth.from(date))

/** Бит 0 — понедельник, …, бит 6 — воскресенье. */
private fun isSelected(mask: Int, dayOfWeek: DayOfWeek): Boolean = mask and (1 shl (dayOfWeek.value - 1)) != 0
