package com.snax1k.chekki.data

import androidx.room.withTransaction
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.HabitCheckEntity
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.HabitListItem
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.HabitReminderEntity
import com.snax1k.chekki.data.db.HabitWithCount
import com.snax1k.chekki.data.db.bit
import com.snax1k.chekki.data.db.pauseOn
import com.snax1k.chekki.reminders.HabitReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Привычки, их отметки по дням и напоминания. После каждого изменения, от которого зависит ближайшее
 * напоминание (времена, расписание, отметки за сегодня), будильник привычки переставляется через [reminders].
 */
class HabitRepository(
    private val database: ChekkiDatabase,
    private val reminders: HabitReminderScheduler = HabitReminderScheduler.None,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    private val habitDao = database.habitDao()

    /** Привычки на [date] по расписанию, в порядке кругов, с числом отметок за этот день. */
    fun observeHabitsOn(date: LocalDate): Flow<List<HabitWithCount>> =
        habitDao.observeHabitsOn(date, date.dayOfWeek.bit)

    suspend fun getHabit(id: Long): HabitEntity? = habitDao.getHabit(id)

    /** Привычка для карточки; null — её удалили. */
    fun observeHabit(id: Long): Flow<HabitEntity?> = habitDao.observeHabit(id)

    /** Все отметки привычки по дням. */
    fun observeChecks(habitId: Long): Flow<List<HabitCheckEntity>> = habitDao.observeChecks(habitId)

    /** Все привычки: не в архиве по порядку кругов, потом архив. Пауза — та, что идёт в [today]. */
    fun observeAllHabits(today: LocalDate): Flow<List<HabitListItem>> = habitDao.observeAllHabits(today)

    /** Паузы привычки, прошлые и нынешняя. */
    fun observePauses(habitId: Long): Flow<List<HabitPauseEntity>> = habitDao.observePauses(habitId)

    /** Новый порядок действующих привычек (перетаскивание на экране «Все привычки»): места по порядку списка. */
    suspend fun reorder(habitIds: List<Long>) = database.withTransaction {
        habitIds.forEachIndexed { index, id -> habitDao.setPosition(id, index) }
    }

    /**
     * Привычка уходит в архив или возвращается из него. В архиве она не видна наверху и не напоминает,
     * история отметок остаётся. Вернувшаяся встаёт в конец ряда.
     */
    suspend fun setArchived(habitId: Long, archived: Boolean) {
        database.withTransaction {
            if (!archived) habitDao.setPosition(habitId, habitDao.nextPosition())
            habitDao.setArchived(habitId, archived)
        }
        if (archived) reminders.dismiss(habitId)
        reschedule(habitId)
    }

    /**
     * Ставит привычку на паузу с [today] до [resumeDate] — дня, когда она снова начнёт действовать
     * (null — без срока). Если она уже на паузе, у этой паузы меняется срок. Пока пауза идёт,
     * привычки нет наверху, напоминаний не приходит, а дни паузы не рвут серию.
     */
    suspend fun pause(habitId: Long, resumeDate: LocalDate?, today: LocalDate = LocalDate.now(clock)) {
        if (resumeDate != null && resumeDate <= today) return
        database.withTransaction {
            val current = habitDao.getPauses(habitId).pauseOn(today)
            if (current != null) {
                habitDao.setPauseResumeDate(current.id, resumeDate)
            } else {
                habitDao.insertPause(HabitPauseEntity(habitId = habitId, startDate = today, resumeDate = resumeDate))
            }
        }
        reminders.dismiss(habitId)
        reschedule(habitId)
    }

    /** «Продолжить»: пауза кончается сегодня, привычка сразу снова наверху и напоминает. */
    suspend fun resume(habitId: Long, today: LocalDate = LocalDate.now(clock)) {
        database.withTransaction {
            val current = habitDao.getPauses(habitId).pauseOn(today) ?: return@withTransaction
            // Пауза, начатая сегодня, следа не оставляет; более ранняя остаётся в истории до вчера
            if (current.startDate >= today) {
                habitDao.deletePause(current.id)
            } else {
                habitDao.setPauseResumeDate(current.id, today)
            }
        }
        reschedule(habitId)
    }

    /** Времена напоминаний привычки по порядку. */
    suspend fun getReminderTimes(habitId: Long): List<LocalTime> = habitDao.getReminderTimes(habitId)

    /** Новая привычка в конце ряда. Возвращает её id. */
    suspend fun addHabit(
        title: String,
        icon: HabitIcon,
        colorIndex: Int,
        dailyTarget: Int,
        weekdays: Int,
        reminderTimes: List<LocalTime> = emptyList(),
        now: Instant = clock.instant()
    ): Long {
        val id = database.withTransaction {
            val id = habitDao.insertHabit(
                HabitEntity(
                    title = title,
                    icon = icon,
                    colorIndex = colorIndex,
                    dailyTarget = dailyTarget,
                    weekdays = weekdays,
                    position = habitDao.nextPosition(),
                    createdAt = now
                )
            )
            replaceReminders(id, reminderTimes)
            id
        }
        reschedule(id)
        return id
    }

    /**
     * Меняет привычку. Новая цель действует с [today]: сегодняшние отметки пересчитываются по ней,
     * прошлые дни остаются со своей целью. Времена напоминаний заменяются целиком.
     */
    suspend fun updateHabit(
        id: Long,
        title: String,
        icon: HabitIcon,
        colorIndex: Int,
        dailyTarget: Int,
        weekdays: Int,
        reminderTimes: List<LocalTime>? = null,
        today: LocalDate = LocalDate.now(clock)
    ) {
        database.withTransaction {
            habitDao.updateHabit(id, title, icon, colorIndex, dailyTarget, weekdays)
            habitDao.retargetCheck(id, today, dailyTarget)
            if (reminderTimes != null) replaceReminders(id, reminderTimes)
        }
        reschedule(id)
    }

    /**
     * Ещё одна отметка за [date] — сегодня или прошлый день. Больше цели не отмечается: возвращает число
     * отметок после нажатия, а если цель уже достигнута — то же число, что было. null — привычки нет.
     * У дня, где уже есть отметки, цель своя (какой была тогда), у нового дня — нынешняя цель привычки.
     * Набрали сегодняшнюю цель — уведомление убирается, а сегодняшние напоминания больше не придут.
     */
    suspend fun check(habitId: Long, date: LocalDate): Int? {
        val (count, target) = database.withTransaction {
            val habit = habitDao.getHabit(habitId) ?: return@withTransaction null
            val existing = habitDao.getCheck(habitId, date)
            val target = existing?.target ?: habit.dailyTarget
            val count = minOf((existing?.count ?: 0) + 1, target)
            habitDao.upsertCheck(HabitCheckEntity(habitId, date, count = count, target = target))
            count to target
        } ?: return null
        if (date == LocalDate.now(clock)) {
            if (count >= target) reminders.dismiss(habitId)
            reschedule(habitId)
        }
        return count
    }

    /**
     * Убирает одну отметку за [date]; последняя убранная удаляет строку дня. Возвращает, сколько осталось.
     * Если сегодняшняя цель снова не набрана, сегодняшние напоминания возвращаются.
     */
    suspend fun uncheck(habitId: Long, date: LocalDate): Int {
        val count = database.withTransaction {
            val existing = habitDao.getCheck(habitId, date) ?: return@withTransaction 0
            val count = existing.count - 1
            if (count > 0) habitDao.upsertCheck(existing.copy(count = count)) else habitDao.deleteCheck(habitId, date)
            maxOf(count, 0)
        }
        if (date == LocalDate.now(clock)) reschedule(habitId)
        return count
    }

    /** Удаляет привычку вместе с отметками, напоминаниями и паузами. Возвращает всё, что нужно для [restoreHabit]. */
    suspend fun deleteHabit(id: Long): DeletedHabit? {
        val deleted = database.withTransaction {
            val habit = habitDao.getHabit(id) ?: return@withTransaction null
            val deleted = DeletedHabit(habit, habitDao.getChecks(id), habitDao.getReminderTimes(id), habitDao.getPauses(id))
            habitDao.deleteHabit(id)
            deleted
        } ?: return null
        reminders.cancel(id)
        return deleted
    }

    /** Возвращает удалённую привычку как была: с тем же id, местом в ряду, отметками, напоминаниями и паузами. */
    suspend fun restoreHabit(deleted: DeletedHabit) {
        database.withTransaction {
            habitDao.insertHabit(deleted.habit)
            if (deleted.checks.isNotEmpty()) habitDao.insertChecks(deleted.checks)
            if (deleted.pauses.isNotEmpty()) habitDao.insertPauses(deleted.pauses)
            replaceReminders(deleted.habit.id, deleted.reminderTimes)
        }
        reschedule(deleted.habit.id)
    }

    /**
     * Сработал будильник привычки, поставленный на [firedAt]. Возвращает привычку и число отметок за сегодня,
     * если уведомление нужно показать: время напоминания ещё есть у привычки, сегодня день по расписанию,
     * привычка не на паузе и цель не набрана. В любом случае ставит будильник на следующее напоминание.
     */
    suspend fun reminderToShow(habitId: Long, firedAt: Instant): Pair<HabitEntity, Int>? {
        val today = LocalDate.now(clock)
        val habit = habitDao.getHabit(habitId) ?: return null
        val count = habitDao.getCheck(habitId, today)?.count ?: 0
        val firedTime = LocalDateTime.ofInstant(firedAt, clock.zone)
        val stillActual = !habit.isArchived && habit.isScheduledOn(today) && count < habit.dailyTarget &&
            habitDao.getPauses(habitId).pauseOn(today) == null &&
            firedTime.toLocalDate() == today && firedTime.toLocalTime() in habitDao.getReminderTimes(habitId)
        reschedule(habitId)
        return if (stillActual) habit to count else null
    }

    /** Ставит будильники всех привычек с напоминаниями заново: при запуске, после перезагрузки и смены времени. */
    suspend fun rescheduleAllReminders() {
        habitDao.getHabitIdsWithReminders().forEach { reschedule(it) }
    }

    /** Будильник привычки — на её ближайшее напоминание после текущего момента; нет напоминаний — снимается. */
    private suspend fun reschedule(habitId: Long) {
        val habit = habitDao.getHabit(habitId)
        if (habit == null) {
            reminders.update(habitId, null)
            return
        }
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val todayCheck = habitDao.getCheck(habitId, today)
        val todayComplete = todayCheck != null && todayCheck.count >= todayCheck.target
        val next = nextHabitReminder(habit, habitDao.getReminderTimes(habitId), now, todayComplete, habitDao.getPauses(habitId))
        reminders.update(habitId, next?.atZone(clock.zone)?.toInstant())
    }

    private suspend fun replaceReminders(habitId: Long, times: List<LocalTime>) {
        habitDao.deleteReminders(habitId)
        val distinct = times.map { it.withSecond(0).withNano(0) }.distinct()
        if (distinct.isNotEmpty()) habitDao.insertReminders(distinct.map { HabitReminderEntity(habitId, it) })
    }
}

/** Удалённая привычка со всеми отметками, напоминаниями и паузами — чтобы вернуть её кнопкой «Отменить». */
data class DeletedHabit(
    val habit: HabitEntity,
    val checks: List<HabitCheckEntity>,
    val reminderTimes: List<LocalTime>,
    val pauses: List<HabitPauseEntity> = emptyList()
)
