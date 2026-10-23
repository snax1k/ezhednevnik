package com.snax1k.chekki.data

import com.snax1k.chekki.data.db.HabitCheckEntity
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.bit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/** «Сегодня» — пятница 23 октября 2026. */
class HabitStatsTest {

    private val today = LocalDate.of(2026, 10, 23)
    private val zone = ZoneOffset.UTC

    private fun habit(weekdays: Int = HabitEntity.EVERY_DAY, target: Int = 2, createdDaysAgo: Long = 30) = HabitEntity(
        id = 1,
        title = "Пить воду",
        icon = HabitIcon.Water,
        colorIndex = 0,
        dailyTarget = target,
        weekdays = weekdays,
        position = 0,
        createdAt = today.minusDays(createdDaysAgo).atStartOfDay(zone).toInstant()
    )

    /** Выполненные дни: сколько дней назад от сегодня. */
    private fun done(vararg daysAgo: Long, target: Int = 2) =
        daysAgo.map { HabitCheckEntity(1, today.minusDays(it), count = target, target = target) }

    private fun streaks(habit: HabitEntity, checks: List<HabitCheckEntity>, pauses: List<HabitPauseEntity> = emptyList()) =
        habitStreaks(habit, checks, today, zone, pauses)

    /** Пауза с [startDaysAgo] дней назад до [resumeDaysAgo] (null — без срока). */
    private fun pause(startDaysAgo: Long, resumeDaysAgo: Long?) =
        HabitPauseEntity(habitId = 1, startDate = today.minusDays(startDaysAgo), resumeDate = resumeDaysAgo?.let(today::minusDays))

    @Test
    fun серияИдётДоВчераПокаСегодняНеЗакончено() {
        assertEquals(HabitStreaks(current = 3, best = 3), streaks(habit(), done(1, 2, 3)))
        // Сегодня сделана половина — серия не рвётся и не растёт
        val half = done(1, 2, 3) + HabitCheckEntity(1, today, count = 1, target = 2)
        assertEquals(3, streaks(habit(), half).current)
        // Сегодня выполнено — серия растёт
        assertEquals(4, streaks(habit(), done(0, 1, 2, 3)).current)
    }

    @Test
    fun пропущенныйДеньРвётСериюНоЛучшаяОстаётся() {
        val checks = done(1, 2) + done(4, 5, 6, 7, 8)
        assertEquals(HabitStreaks(current = 2, best = 5), streaks(habit(), checks))
    }

    @Test
    fun выходнойПоРасписаниюСериюНеРвёт() {
        val weekdays = DayOfWeek.entries.take(5).sumOf { it.bit }
        // Пт 23 сегодня, Чт 22, Ср 21, Вт 20, Пн 19 выполнены; Сб 17 и Вс 18 не по расписанию; Пт 16 выполнена
        val checks = done(1, 2, 3, 4, 7)
        assertEquals(5, streaks(habit(weekdays = weekdays), checks).current)
    }

    @Test
    fun отметкаПомнитЦельСвоегоДня() {
        // Вчера цель была 1 и её выполнили; потом цель подняли до 3 — вчерашний день остаётся выполненным
        val checks = listOf(HabitCheckEntity(1, today.minusDays(1), count = 1, target = 1))
        assertEquals(1, streaks(habit(target = 3), checks).current)
    }

    @Test
    fun безОтметокСерийНет() {
        assertEquals(HabitStreaks(0, 0), streaks(habit(), emptyList()))
    }

    @Test
    fun отметкиРаньшеСозданияТожеСчитаются() {
        // Привычку перенесли из старого приложения: создана сегодня, а отметки старше
        assertEquals(3, streaks(habit(createdDaysAgo = 0), done(1, 2, 3)).current)
    }

    @Test
    fun неделяПоПорядкуСДнямиВнеРасписания() {
        val weekend = DayOfWeek.SATURDAY.bit or DayOfWeek.SUNDAY.bit
        val week = habitWeek(habit(weekdays = weekend), done(5), today)
        assertEquals(7, week.size)
        assertEquals(today.minusDays(6), week.first().date)
        assertEquals(today, week.last().date)
        // Сб 17 и Вс 18 — по расписанию, остальные нет
        assertEquals(listOf(true, true, false, false, false, false, false), week.map { it.isScheduled })
        assertTrue(week[1].isComplete)
        assertFalse(week[0].isComplete)
    }

    @Test
    fun дниПаузыНеРвутСерию() {
        // Выполнены 7 и 6 дней назад, потом пауза на 3 дня (5, 4, 3 дня назад), потом выполнены вчера и позавчера
        val checks = done(1, 2, 6, 7)
        assertEquals(2, streaks(habit(), checks).current)
        assertEquals(4, streaks(habit(), checks, listOf(pause(startDaysAgo = 5, resumeDaysAgo = 2))).current)
    }

    @Test
    fun выполненныйДеньПаузыПродолжаетСерию() {
        // Позавчера был на паузе, но привычку всё равно выполнили — серия растёт
        val checks = done(1, 2, 3)
        assertEquals(3, streaks(habit(), checks, listOf(pause(startDaysAgo = 2, resumeDaysAgo = 1))).current)
    }

    @Test
    fun неделяПоказываетДниПаузыКакДниВнеРасписания() {
        // На паузе с позавчера без срока; позавчера успели отметить
        val week = habitWeek(habit(), done(2), today, listOf(pause(startDaysAgo = 2, resumeDaysAgo = null)))
        assertEquals(listOf(true, true, true, true, true, false, false), week.map { it.isScheduled })
        assertEquals(listOf(false, false, false, false, true, true, true), week.map { it.isPaused })
    }
}
