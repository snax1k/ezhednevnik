package com.snax1k.chekki.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Статистика задач. «Сегодня» — четверг 29 октября 2026, часовой пояс телефона — UTC+3. */
class TaskStatsTest {

    private val zone = ZoneOffset.ofHours(3)
    private val today = LocalDate.of(2026, 10, 29)

    /** Задача, выполненная в [day] в [time] по часам телефона, на день [date]. */
    private fun done(day: LocalDate, date: LocalDate? = day, time: LocalTime = LocalTime.NOON) =
        CompletedTask(day.atTime(time).atZone(zone).toInstant(), date)

    private fun stats(tasks: List<CompletedTask>, period: StatsPeriod = StatsPeriod.Week, offset: Int = 0) =
        taskStats(tasks, period, offset, today, zone)

    @Test
    fun границыПериодов() {
        assertEquals(LocalDate.of(2026, 10, 26)..LocalDate.of(2026, 11, 1), statsRange(StatsPeriod.Week, today))
        assertEquals(LocalDate.of(2026, 10, 19)..LocalDate.of(2026, 10, 25), statsRange(StatsPeriod.Week, today, offset = 1))
        assertEquals(LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 9, 30), statsRange(StatsPeriod.Month, today, offset = 1))
        assertEquals(LocalDate.of(2025, 1, 1)..LocalDate.of(2025, 12, 31), statsRange(StatsPeriod.Year, today, offset = 1))
    }

    @Test
    fun выполненоЗаНеделюИЗаПрошлую() {
        val result = stats(
            listOf(
                done(LocalDate.of(2026, 10, 26)),
                done(LocalDate.of(2026, 10, 26)),
                done(LocalDate.of(2026, 10, 29)),
                done(LocalDate.of(2026, 10, 20)),
                // Воскресенье 25 октября, поздно вечером — ещё прошлая неделя
                done(LocalDate.of(2026, 10, 25), time = LocalTime.of(23, 30))
            )
        )
        assertEquals(3, result.done)
        assertEquals(2, result.previousDone)
        assertEquals(listOf(2, 0, 0, 1, 0, 0, 0), result.bars.map { it.count })
        assertEquals(LocalDate.of(2026, 10, 26), result.bars.first().start)
    }

    @Test
    fun деньОтметкиПоЧасамТелефона() {
        // 26 октября 00:30 по UTC+3 — по UTC ещё 25-е, но для телефона это понедельник новой недели
        val result = stats(listOf(done(LocalDate.of(2026, 10, 26), time = LocalTime.of(0, 30))))
        assertEquals(1, result.done)
        assertEquals(1, result.bars.first().count)
    }

    @Test
    fun вСрокСредиЗадачСДатой() {
        val result = stats(
            listOf(
                done(LocalDate.of(2026, 10, 27), date = LocalDate.of(2026, 10, 27)),
                done(LocalDate.of(2026, 10, 27), date = LocalDate.of(2026, 10, 30)),
                done(LocalDate.of(2026, 10, 28), date = LocalDate.of(2026, 10, 20)),
                done(LocalDate.of(2026, 10, 28), date = null)
            )
        )
        // Две из трёх с датой — в срок, без даты не считается
        assertEquals(67, result.onTimePercent)
        assertNull(stats(listOf(done(LocalDate.of(2026, 10, 28), date = null))).onTimePercent)
        assertNull(stats(emptyList()).onTimePercent)
    }

    @Test
    fun месяцПоДнямГодПоМесяцам() {
        val tasks = listOf(done(LocalDate.of(2026, 10, 1)), done(LocalDate.of(2026, 10, 31)), done(LocalDate.of(2026, 2, 14)))
        val month = stats(tasks, StatsPeriod.Month)
        assertEquals(31, month.bars.size)
        assertEquals(2, month.done)
        val year = stats(tasks, StatsPeriod.Year)
        assertEquals(12, year.bars.size)
        assertEquals(1, year.bars[1].count)
        assertEquals(2, year.bars[9].count)
        assertEquals(LocalDate.of(2026, 10, 1), year.bars[9].start)
        assertEquals(3, year.done)
    }

    @Test
    fun серииПоДнямСВыполненнымиЗадачами() {
        val tasks = listOf(
            // Лучшая серия — 4 дня в сентябре
            done(LocalDate.of(2026, 9, 1)), done(LocalDate.of(2026, 9, 2)), done(LocalDate.of(2026, 9, 3)), done(LocalDate.of(2026, 9, 4)),
            // Нынешняя — 26, 27, 28 октября; сегодня ещё ничего, но день идёт — серия не рвётся
            done(LocalDate.of(2026, 10, 26)), done(LocalDate.of(2026, 10, 27)), done(LocalDate.of(2026, 10, 28)), done(LocalDate.of(2026, 10, 28))
        )
        val result = stats(tasks)
        assertEquals(3, result.currentStreak)
        assertEquals(4, result.bestStreak)
        assertEquals(4, stats(tasks + done(today)).currentStreak)
        // Вчера пропущено — серии нет
        assertEquals(0, stats(listOf(done(LocalDate.of(2026, 10, 27)))).currentStreak)
    }
}
