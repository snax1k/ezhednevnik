package com.snax1k.chekki.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    /**
     * Привычки на день: не в архиве и по расписанию на этот день недели ([dayBit] — бит дня, см. `DayOfWeek.bit`),
     * в порядке кругов, вместе с числом отметок за этот день.
     */
    @Query(
        """
        SELECT h.*, COALESCE(c.count, 0) AS day_count
        FROM habits h
        LEFT JOIN habit_checks c ON c.habit_id = h.id AND c.date = :date
        WHERE h.is_archived = 0 AND (h.weekdays & :dayBit) != 0
        ORDER BY h.position, h.id
        """
    )
    fun observeHabitsOn(date: LocalDate, dayBit: Int): Flow<List<HabitWithCount>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): HabitEntity?

    /** Привычка для карточки; null — её удалили. */
    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeHabit(id: Long): Flow<HabitEntity?>

    /** Все отметки привычки по дням — для истории и серий в карточке. */
    @Query("SELECT * FROM habit_checks WHERE habit_id = :habitId ORDER BY date")
    fun observeChecks(habitId: Long): Flow<List<HabitCheckEntity>>

    @Query("DELETE FROM habit_checks WHERE habit_id = :habitId AND date = :date")
    suspend fun deleteCheck(habitId: Long, date: LocalDate)

    @Query("SELECT * FROM habit_checks WHERE habit_id = :habitId AND date = :date")
    suspend fun getCheck(habitId: Long, date: LocalDate): HabitCheckEntity?

    @Query("SELECT * FROM habit_checks WHERE habit_id = :habitId ORDER BY date")
    suspend fun getChecks(habitId: Long): List<HabitCheckEntity>

    /** Место для новой привычки — после последней. */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM habits")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Query(
        """
        UPDATE habits SET title = :title, icon = :icon, color_index = :colorIndex,
            daily_target = :dailyTarget, weekdays = :weekdays
        WHERE id = :id
        """
    )
    suspend fun updateHabit(id: Long, title: String, icon: HabitIcon, colorIndex: Int, dailyTarget: Int, weekdays: Int)

    /**
     * Новая цель для отметок за [date]: сегодняшний день считается по новой цели,
     * прошлые дни не трогаем. Отметок больше цели не бывает, поэтому лишние срезаются.
     */
    @Query("UPDATE habit_checks SET target = :target, count = MIN(count, :target) WHERE habit_id = :habitId AND date = :date")
    suspend fun retargetCheck(habitId: Long, date: LocalDate, target: Int)

    @Upsert
    suspend fun upsertCheck(check: HabitCheckEntity)

    @Insert
    suspend fun insertChecks(checks: List<HabitCheckEntity>)

    /** Удаляет привычку; её отметки удаляются вместе с ней. */
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)
}
