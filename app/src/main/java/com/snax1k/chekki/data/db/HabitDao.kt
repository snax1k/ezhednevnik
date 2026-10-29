package com.snax1k.chekki.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface HabitDao {

    /**
     * Привычки на день: не в архиве, не на паузе и по расписанию на этот день недели ([dayBit] — бит дня,
     * см. `DayOfWeek.bit`), в порядке кругов, вместе с числом отметок за этот день.
     */
    @Query(
        """
        SELECT h.*, COALESCE(c.count, 0) AS day_count
        FROM habits h
        LEFT JOIN habit_checks c ON c.habit_id = h.id AND c.date = :date
        WHERE h.is_archived = 0 AND (h.weekdays & :dayBit) != 0
            AND NOT EXISTS (
                SELECT 1 FROM habit_pauses p
                WHERE p.habit_id = h.id AND p.start_date <= :date AND (p.resume_date IS NULL OR p.resume_date > :date)
            )
        ORDER BY h.position, h.id
        """
    )
    fun observeHabitsOn(date: LocalDate, dayBit: Int): Flow<List<HabitWithCount>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): HabitEntity?

    /**
     * Все привычки для экрана «Все привычки»: сначала не в архиве, потом архив, внутри — по порядку кругов.
     * Пауза — та, в которую попадает [today].
     */
    @Query(
        """
        SELECT h.*, (SELECT COUNT(*) FROM habit_reminders r WHERE r.habit_id = h.id) AS reminder_count,
            p.id IS NOT NULL AS is_paused, p.resume_date AS pause_resume_date
        FROM habits h
        LEFT JOIN habit_pauses p ON p.id = (
            SELECT p2.id FROM habit_pauses p2
            WHERE p2.habit_id = h.id AND p2.start_date <= :today AND (p2.resume_date IS NULL OR p2.resume_date > :today)
            LIMIT 1
        )
        ORDER BY h.is_archived, h.position, h.id
        """
    )
    fun observeAllHabits(today: LocalDate): Flow<List<HabitListItem>>

    @Query("UPDATE habits SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("UPDATE habits SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

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

    /** Паузы привычки по порядку — для серий и ближайшего напоминания. */
    @Query("SELECT * FROM habit_pauses WHERE habit_id = :habitId ORDER BY start_date")
    fun observePauses(habitId: Long): Flow<List<HabitPauseEntity>>

    @Query("SELECT * FROM habit_pauses WHERE habit_id = :habitId ORDER BY start_date")
    suspend fun getPauses(habitId: Long): List<HabitPauseEntity>

    @Insert
    suspend fun insertPause(pause: HabitPauseEntity): Long

    @Insert
    suspend fun insertPauses(pauses: List<HabitPauseEntity>)

    @Query("UPDATE habit_pauses SET resume_date = :resumeDate WHERE id = :id")
    suspend fun setPauseResumeDate(id: Long, resumeDate: LocalDate?)

    @Query("DELETE FROM habit_pauses WHERE id = :id")
    suspend fun deletePause(id: Long)

    @Query("SELECT time FROM habit_reminders WHERE habit_id = :habitId ORDER BY time")
    suspend fun getReminderTimes(habitId: Long): List<LocalTime>

    /** Привычки, у которых есть напоминания: им нужны будильники. */
    @Query("SELECT DISTINCT habit_id FROM habit_reminders")
    suspend fun getHabitIdsWithReminders(): List<Long>

    @Query("DELETE FROM habit_reminders WHERE habit_id = :habitId")
    suspend fun deleteReminders(habitId: Long)

    @Insert
    suspend fun insertReminders(reminders: List<HabitReminderEntity>)

    /** Удаляет привычку; её отметки, напоминания и паузы удаляются вместе с ней. */
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT id FROM habits")
    suspend fun getAllHabitIds(): List<Long>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun countHabits(): Int

    /** Перенос с заменой: привычки вместе с отметками, напоминаниями и паузами. */
    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}
