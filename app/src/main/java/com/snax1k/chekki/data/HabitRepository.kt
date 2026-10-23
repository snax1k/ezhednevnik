package com.snax1k.chekki.data

import androidx.room.withTransaction
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.HabitCheckEntity
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.HabitWithCount
import com.snax1k.chekki.data.db.bit
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** Привычки и их отметки по дням. */
class HabitRepository(private val database: ChekkiDatabase) {

    private val habitDao = database.habitDao()

    /** Привычки на [date] по расписанию, в порядке кругов, с числом отметок за этот день. */
    fun observeHabitsOn(date: LocalDate): Flow<List<HabitWithCount>> =
        habitDao.observeHabitsOn(date, date.dayOfWeek.bit)

    suspend fun getHabit(id: Long): HabitEntity? = habitDao.getHabit(id)

    /** Привычка для карточки; null — её удалили. */
    fun observeHabit(id: Long): Flow<HabitEntity?> = habitDao.observeHabit(id)

    /** Все отметки привычки по дням. */
    fun observeChecks(habitId: Long): Flow<List<HabitCheckEntity>> = habitDao.observeChecks(habitId)

    /** Новая привычка в конце ряда. Возвращает её id. */
    suspend fun addHabit(
        title: String,
        icon: HabitIcon,
        colorIndex: Int,
        dailyTarget: Int,
        weekdays: Int,
        now: Instant = Instant.now()
    ): Long = database.withTransaction {
        habitDao.insertHabit(
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
    }

    /**
     * Меняет привычку. Новая цель действует с [today]: сегодняшние отметки пересчитываются по ней,
     * прошлые дни остаются со своей целью.
     */
    suspend fun updateHabit(
        id: Long,
        title: String,
        icon: HabitIcon,
        colorIndex: Int,
        dailyTarget: Int,
        weekdays: Int,
        today: LocalDate = LocalDate.now()
    ) = database.withTransaction {
        habitDao.updateHabit(id, title, icon, colorIndex, dailyTarget, weekdays)
        habitDao.retargetCheck(id, today, dailyTarget)
    }

    /**
     * Ещё одна отметка за [date] — сегодня или прошлый день. Больше цели не отмечается: возвращает число
     * отметок после нажатия, а если цель уже достигнута — то же число, что было. null — привычки нет.
     * У дня, где уже есть отметки, цель своя (какой была тогда), у нового дня — нынешняя цель привычки.
     */
    suspend fun check(habitId: Long, date: LocalDate): Int? = database.withTransaction {
        val habit = habitDao.getHabit(habitId) ?: return@withTransaction null
        val existing = habitDao.getCheck(habitId, date)
        val target = existing?.target ?: habit.dailyTarget
        val count = minOf((existing?.count ?: 0) + 1, target)
        habitDao.upsertCheck(HabitCheckEntity(habitId, date, count = count, target = target))
        count
    }

    /** Убирает одну отметку за [date]; последняя убранная удаляет строку дня. Возвращает, сколько осталось. */
    suspend fun uncheck(habitId: Long, date: LocalDate): Int = database.withTransaction {
        val existing = habitDao.getCheck(habitId, date) ?: return@withTransaction 0
        val count = existing.count - 1
        if (count > 0) habitDao.upsertCheck(existing.copy(count = count)) else habitDao.deleteCheck(habitId, date)
        maxOf(count, 0)
    }

    /** Удаляет привычку вместе с отметками. Возвращает всё, что нужно для [restoreHabit]; null — привычки нет. */
    suspend fun deleteHabit(id: Long): DeletedHabit? = database.withTransaction {
        val habit = habitDao.getHabit(id) ?: return@withTransaction null
        val deleted = DeletedHabit(habit, habitDao.getChecks(id))
        habitDao.deleteHabit(id)
        deleted
    }

    /** Возвращает удалённую привычку как была: с тем же id, местом в ряду и всеми отметками. */
    suspend fun restoreHabit(deleted: DeletedHabit) = database.withTransaction {
        habitDao.insertHabit(deleted.habit)
        if (deleted.checks.isNotEmpty()) habitDao.insertChecks(deleted.checks)
    }
}

/** Удалённая привычка со всеми отметками — чтобы вернуть её кнопкой «Отменить». */
data class DeletedHabit(
    val habit: HabitEntity,
    val checks: List<HabitCheckEntity>
)
