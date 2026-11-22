package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.db.bit
import com.snax1k.chekki.data.firstOnOrAfter
import com.snax1k.chekki.data.weekParity
import java.time.DayOfWeek
import java.time.LocalDate

/** Вкладки окна «Повтор», как в старом приложении: по дням недели, по числам месяца, свой период. */
enum class RepeatTab { Week, Month, Custom }

/** Единицы «своего периода» по порядку на экране. */
val CustomRepeatUnits: List<RepeatUnit> = listOf(RepeatUnit.Day, RepeatUnit.Week, RepeatUnit.Month, RepeatUnit.Year)

/**
 * Состояние окна «Повтор» (вариант 1: одна панель с тремя вкладками, решено 23.11.2026).
 * Логика без Compose, чтобы её проверяли модульные тесты.
 *
 * [base] — с какого дня искать «Следующую задачу»: дата задачи, но не раньше сегодня.
 * Любое изменение дней или периода заново считает [nextDate] от [base]; выбранную вручную дату
 * меняет только следующее изменение правила.
 */
data class RepeatEditor(
    val tab: RepeatTab,
    /** Дни недели (вкладка «Неделя»); в двухнедельном режиме — дни этой недели. */
    val weekdays: Int,
    val twoWeeks: Boolean,
    /** Дни следующей недели в двухнедельном режиме. */
    val nextWeekdays: Int,
    /** Каждые сколько недель для дней недели: в окне не меняется, сохраняется из прежнего правила. */
    val weekEvery: Int,
    /** Числа месяца, маска как в [RepeatRule.monthDays]. */
    val monthDays: Int,
    /** Каждые сколько месяцев для чисел: в окне не меняется, сохраняется из прежнего правила. */
    val monthEvery: Int,
    /** «Свой период»: каждые [every] [unit]. */
    val every: Int,
    val unit: RepeatUnit,
    /** Повторять до этого дня включительно; null — бессрочно. */
    val until: LocalDate?,
    /** «Следующая задача» — станет датой задачи. */
    val nextDate: LocalDate,
    val base: LocalDate,
    /** Сегодня: от него считаются «эта» и «следующая» неделя в двухнедельном режиме. */
    val today: LocalDate
) {
    /** Правило, которое получится по «Готово»; null — ничего не выбрано (нет дней или чисел). */
    val rule: RepeatRule?
        get() = when (tab) {
            RepeatTab.Week -> weekRule()
            RepeatTab.Month ->
                if (monthDays == 0) null
                else RepeatRule(RepeatUnit.Month, every = monthEvery, weekdays = 0, monthDays = monthDays)
            RepeatTab.Custom -> RepeatRule(unit, every = every, weekdays = 0)
        }?.copy(until = until)

    /** Срок раньше следующей задачи — повторять было бы нечего. */
    val untilTooEarly: Boolean get() = until != null && until < nextDate

    val canSave: Boolean get() = rule != null && !untilTooEarly

    fun withTab(tab: RepeatTab): RepeatEditor = copy(tab = tab).recalculated()

    fun toggleWeekday(day: DayOfWeek, nextWeek: Boolean = false): RepeatEditor {
        val bit = day.bit
        return if (nextWeek) copy(nextWeekdays = nextWeekdays xor bit).recalculated()
        else copy(weekdays = weekdays xor bit).recalculated()
    }

    fun withTwoWeeks(enabled: Boolean): RepeatEditor = copy(twoWeeks = enabled).recalculated()

    /** Число месяца 1–31; 31 — последний день. */
    fun toggleMonthDay(day: Int): RepeatEditor {
        require(day in 1..31)
        return copy(monthDays = monthDays xor (1 shl (day - 1))).recalculated()
    }

    fun withEvery(every: Int): RepeatEditor = copy(every = every.coerceIn(MIN_EVERY, MAX_EVERY)).recalculated()

    fun withUnit(unit: RepeatUnit): RepeatEditor {
        require(unit in CustomRepeatUnits)
        return copy(unit = unit).recalculated()
    }

    /** Дата выбрана вручную: она станет датой задачи, даже если не совпадает с днями правила. */
    fun withNextDate(date: LocalDate): RepeatEditor = copy(nextDate = date)

    fun withUntil(until: LocalDate?): RepeatEditor = copy(until = until)

    fun isWeekdayOn(day: DayOfWeek, nextWeek: Boolean = false): Boolean =
        (if (nextWeek) nextWeekdays else weekdays) and day.bit != 0

    fun isMonthDayOn(day: Int): Boolean = monthDays and (1 shl (day - 1)) != 0

    private fun weekRule(): RepeatRule? {
        if (!twoWeeks) {
            return if (weekdays == 0) null else RepeatRule(RepeatUnit.Week, every = weekEvery, weekdays = weekdays)
        }
        if (weekdays == 0 && nextWeekdays == 0) return null
        // Одинаковые дни в обе недели — это просто каждую неделю
        if (weekdays == nextWeekdays) return RepeatRule(RepeatUnit.Week, every = 1, weekdays = weekdays)
        // В базе — по чётности недель, чтобы правило не зависело от того, когда его выбрали
        val thisWeekIsEven = weekParity(today) == 0
        return RepeatRule(
            RepeatUnit.TwoWeeks,
            every = 2,
            weekdays = if (thisWeekIsEven) weekdays else nextWeekdays,
            weekdays2 = if (thisWeekIsEven) nextWeekdays else weekdays
        )
    }

    private fun recalculated(): RepeatEditor {
        val next = rule?.firstOnOrAfter(base) ?: return this
        return copy(nextDate = next)
    }

    companion object {
        const val MIN_EVERY = 1
        const val MAX_EVERY = 99

        /**
         * Окно для правила задачи [rule] с датой [taskDate] (null — без даты). Без повтора открывается вкладка
         * «Неделя» с днём недели задачи, чтобы «Готово» сразу работало.
         */
        fun of(rule: RepeatRule, taskDate: LocalDate?, today: LocalDate): RepeatEditor {
            val base = maxOf(taskDate ?: today, today)
            val dayBit = (taskDate ?: today).dayOfWeek.bit
            val empty = RepeatEditor(
                tab = RepeatTab.Week,
                weekdays = dayBit,
                twoWeeks = false,
                nextWeekdays = 0,
                weekEvery = 1,
                monthDays = 1 shl ((taskDate ?: today).dayOfMonth - 1),
                monthEvery = 1,
                every = 1,
                unit = RepeatUnit.Day,
                until = rule.until,
                nextDate = base,
                base = base,
                today = today
            )
            val editor = when (rule.unit) {
                RepeatUnit.None -> empty
                RepeatUnit.Week ->
                    when {
                        rule.weekdays == 0 -> empty.copy(tab = RepeatTab.Custom, unit = RepeatUnit.Week, every = rule.every)
                        // Раз в две недели по дням — это двухнедельный режим с пустой второй неделей
                        rule.every == 2 -> {
                            val taskWeekIsThisWeek = weekParity(taskDate ?: today) == weekParity(today)
                            empty.copy(
                                twoWeeks = true,
                                weekdays = if (taskWeekIsThisWeek) rule.weekdays else 0,
                                nextWeekdays = if (taskWeekIsThisWeek) 0 else rule.weekdays
                            )
                        }
                        else -> empty.copy(weekdays = rule.weekdays, weekEvery = rule.every)
                    }
                RepeatUnit.TwoWeeks -> {
                    val thisWeekIsEven = weekParity(today) == 0
                    empty.copy(
                        twoWeeks = true,
                        weekdays = if (thisWeekIsEven) rule.weekdays else rule.weekdays2,
                        nextWeekdays = if (thisWeekIsEven) rule.weekdays2 else rule.weekdays
                    )
                }
                RepeatUnit.Month ->
                    if (rule.monthDays != 0) empty.copy(tab = RepeatTab.Month, monthDays = rule.monthDays, monthEvery = rule.every)
                    else empty.copy(tab = RepeatTab.Custom, unit = RepeatUnit.Month, every = rule.every)
                RepeatUnit.Day, RepeatUnit.Year -> empty.copy(tab = RepeatTab.Custom, unit = rule.unit, every = rule.every)
            }
            // У задачи с повтором дата уже выбрана — её и показываем; без повтора считаем ближайшую по правилу
            return if (rule.unit != RepeatUnit.None && taskDate != null) editor.copy(nextDate = taskDate) else editor.recalculated()
        }
    }
}
