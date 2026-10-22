package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Задача для списка: вместе с категорией и счётчиком подзадач. */
data class TaskWithDetails(
    @Embedded val task: TaskEntity,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "category_color_index") val categoryColorIndex: Int?,
    @ColumnInfo(name = "subtasks_done") val subtasksDone: Int,
    @ColumnInfo(name = "subtasks_total") val subtasksTotal: Int
)

/** Число невыполненных задач в группах главного экрана. */
data class GroupCounts(
    @ColumnInfo(name = "no_date") val noDate: Int,
    val overdue: Int,
    val tomorrow: Int,
    @ColumnInfo(name = "this_week") val thisWeek: Int,
    @ColumnInfo(name = "this_month") val thisMonth: Int,
    val later: Int
)

@Dao
interface TaskDao {

    /** Задачи на дату, включая выполненные: сначала со временем по порядку, потом «на весь день». */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.date = :date
        ORDER BY t.time IS NULL, t.time, t.created_at, t.id
        """
    )
    fun observeTasksOn(date: LocalDate): Flow<List<TaskWithDetails>>

    /**
     * Невыполненные задачи с [from] по [to] включительно: по дате, внутри дня по времени, «на весь день» в конце.
     * Задачи из [keepIds] попадают в список, даже если выполнены: так отмеченная на экране задача не пропадает сразу.
     */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.date BETWEEN :from AND :to AND (t.is_done = 0 OR t.id IN (:keepIds))
        ORDER BY t.date, t.time IS NULL, t.time, t.created_at, t.id
        """
    )
    fun observeOpenTasksBetween(from: LocalDate, to: LocalDate, keepIds: List<Long>): Flow<List<TaskWithDetails>>

    /** Невыполненные задачи без даты, новые сверху. [keepIds] — как в [observeOpenTasksBetween]. */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.date IS NULL AND (t.is_done = 0 OR t.id IN (:keepIds))
        ORDER BY t.created_at DESC, t.id DESC
        """
    )
    fun observeOpenTasksWithoutDate(keepIds: List<Long>): Flow<List<TaskWithDetails>>

    /**
     * Последние [limit] выполненных задач, свежие сверху, и задачи из [keepIds], даже если с них уже сняли
     * отметку: так задача, которую вернули в работу на экране «Выполненное», не пропадает сразу.
     * У снятой задачи время выполнения стёрто, поэтому она оказывается в конце — порядок восстанавливает экран.
     */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.id IN (SELECT id FROM tasks WHERE is_done = 1 ORDER BY completed_at DESC, id DESC LIMIT :limit)
            OR t.id IN (:keepIds)
        ORDER BY t.completed_at DESC, t.id DESC
        """
    )
    fun observeDoneTasks(limit: Int, keepIds: List<Long>): Flow<List<TaskWithDetails>>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_done = 1")
    fun observeDoneCount(): Flow<Int>

    /**
     * Счётчики групп одним запросом. Границы считает [com.snax1k.chekki.data.GroupRanges];
     * если начало диапазона позже конца, BETWEEN ничего не находит и группа получает 0.
     */
    @Query(
        """
        SELECT
            COALESCE(SUM(date IS NULL), 0) AS no_date,
            COALESCE(SUM(date < :today), 0) AS overdue,
            COALESCE(SUM(date = :tomorrow), 0) AS tomorrow,
            COALESCE(SUM(date BETWEEN :weekStart AND :weekEnd), 0) AS this_week,
            COALESCE(SUM(date BETWEEN :monthStart AND :monthEnd), 0) AS this_month,
            COALESCE(SUM(date >= :laterFrom), 0) AS later
        FROM tasks
        WHERE is_done = 0
        """
    )
    fun observeGroupCounts(
        today: LocalDate,
        tomorrow: LocalDate,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        laterFrom: LocalDate
    ): Flow<GroupCounts>

    /** Названия первых просроченных задач, самые старые сверху. */
    @Query(
        """
        SELECT title FROM tasks
        WHERE is_done = 0 AND date < :today
        ORDER BY date, time IS NULL, time, created_at
        LIMIT :limit
        """
    )
    fun observeOverdueTitles(today: LocalDate, limit: Int): Flow<List<String>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    /** Одна задача с категорией и подзадачами — для текста уведомления. */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.id = :id
        """
    )
    suspend fun getTaskWithDetails(id: Long): TaskWithDetails?

    /** То же для карточки задачи: следит за изменениями; null — задачи нет (например, её удалили). */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.id = :id
        """
    )
    fun observeTaskWithDetails(id: Long): Flow<TaskWithDetails?>

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY position")
    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>>

    /** Меняет поля, которые правятся в карточке задачи. Отметку выполнения и время создания не трогает. */
    @Query(
        """
        UPDATE tasks SET
            title = :title,
            note = :note,
            date = :date,
            time = :time,
            category_id = :categoryId,
            priority = :priority,
            repeat_unit = :repeatUnit,
            repeat_every = :repeatEvery,
            repeat_weekdays = :repeatWeekdays,
            reminder_minutes_before = :reminderMinutesBefore
        WHERE id = :id
        """
    )
    suspend fun updateEditableFields(
        id: Long,
        title: String,
        note: String,
        date: LocalDate?,
        time: LocalTime?,
        categoryId: Long?,
        priority: TaskPriority,
        repeatUnit: RepeatUnit,
        repeatEvery: Int,
        repeatWeekdays: Int,
        reminderMinutesBefore: Int?
    )

    /** Переносит задачу на другую дату; время, повтор и напоминание не трогает. */
    @Query("UPDATE tasks SET date = :date WHERE id = :id")
    suspend fun setDate(id: Long, date: LocalDate?)

    /** Удаляет задачу; подзадачи удаляются вместе с ней, у копий повтора связь обнуляется. */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    /** id всех копий повтора, созданных из задачи [taskId], включая выполненные. */
    @Query("SELECT id FROM tasks WHERE repeated_from_id = :taskId")
    suspend fun getRepeatCopyIds(taskId: Long): List<Long>

    /** Возвращает копиям повтора [ids] связь с исходной задачей — после отмены её удаления. */
    @Query("UPDATE tasks SET repeated_from_id = :taskId WHERE id IN (:ids)")
    suspend fun setRepeatedFrom(ids: List<Long>, taskId: Long)

    @Query("UPDATE subtasks SET is_done = :isDone WHERE id = :id")
    suspend fun setSubtaskDone(id: Long, isDone: Boolean)

    @Insert
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    /** Место для новой подзадачи — после последней; у задачи без подзадач — 0. */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM subtasks WHERE task_id = :taskId")
    suspend fun nextSubtaskPosition(taskId: Long): Int

    @Query("UPDATE subtasks SET title = :title WHERE id = :id")
    suspend fun renameSubtask(id: Long, title: String)

    /** Удаляет подзадачу. Номера мест у остальных не сдвигаются: порядок держится и с пропусками. */
    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtask(id: Long)

    /**
     * Невыполненные задачи с напоминанием на сегодня и дальше: им нужны будильники.
     * У более ранних задач время напоминания уже прошло.
     */
    @Query("SELECT * FROM tasks WHERE is_done = 0 AND reminder_minutes_before IS NOT NULL AND date >= :today")
    suspend fun getUpcomingReminders(today: LocalDate): List<TaskEntity>

    /** id невыполненных копий повтора, созданных из задачи [taskId]. */
    @Query("SELECT id FROM tasks WHERE repeated_from_id = :taskId AND is_done = 0")
    suspend fun getOpenRepeatIds(taskId: Long): List<Long>

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY position")
    suspend fun getSubtasks(taskId: Long): List<SubtaskEntity>

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Insert
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("UPDATE tasks SET is_done = :isDone, completed_at = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean, completedAt: Instant?)

    /** Удаляет копии повтора, созданные из задачи [taskId], если их ещё не выполнили. */
    @Query("DELETE FROM tasks WHERE repeated_from_id = :taskId AND is_done = 0")
    suspend fun deleteOpenRepeatsOf(taskId: Long)
}
