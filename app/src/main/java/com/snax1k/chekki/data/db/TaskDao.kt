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

/** Задача для поиска: как в списках, плюс названия подзадач через перевод строки. */
data class SearchableTask(
    @Embedded val task: TaskEntity,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "category_color_index") val categoryColorIndex: Int?,
    @ColumnInfo(name = "subtasks_done") val subtasksDone: Int,
    @ColumnInfo(name = "subtasks_total") val subtasksTotal: Int,
    @ColumnInfo(name = "subtask_titles") val subtaskTitles: String?
) {
    val details: TaskWithDetails get() = TaskWithDetails(task, categoryName, categoryColorIndex, subtasksDone, subtasksTotal)
    val subtasks: List<String> get() = subtaskTitles?.split('\n').orEmpty()
}

/** Задача для точек в календаре: день, выполнена ли и цвет её категории. */
data class CalendarTaskMark(
    val date: LocalDate,
    @ColumnInfo(name = "is_done") val isDone: Boolean,
    @ColumnInfo(name = "category_color_index") val categoryColorIndex: Int?
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

    /** Задачи на дату, включая выполненные: по приоритету (высокий сверху), внутри — по времени, «на весь день» в конце. */
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
        ORDER BY t.priority DESC, t.time IS NULL, t.time, t.created_at, t.id
        """
    )
    fun observeTasksOn(date: LocalDate): Flow<List<TaskWithDetails>>

    /**
     * Раздел «Выполнено сегодня»: выполненные задачи на дату [date] и все задачи, выполненные с [from] до [to]
     * (сегодня по часам телефона), даже если их дата другая — например, вчерашняя просроченная. Свежие сверху.
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
        WHERE t.is_done = 1 AND (t.date = :date OR (t.completed_at >= :from AND t.completed_at < :to))
        ORDER BY t.completed_at DESC, t.id DESC
        """
    )
    fun observeDoneToday(date: LocalDate, from: Instant, to: Instant): Flow<List<TaskWithDetails>>

    /**
     * Невыполненные задачи с [from] по [to] включительно: по приоритету (высокий сверху), внутри — по дате и времени,
     * «на весь день» в конце дня.
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
        ORDER BY t.priority DESC, t.date, t.time IS NULL, t.time, t.created_at, t.id
        """
    )
    fun observeOpenTasksBetween(from: LocalDate, to: LocalDate, keepIds: List<Long>): Flow<List<TaskWithDetails>>

    /**
     * Невыполненные задачи категории ([categoryId] null — задачи без категории): по приоритету, внутри — по дате
     * и времени, задачи без даты в конце, новые сверху. [keepIds] — как в [observeOpenTasksBetween].
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
        WHERE t.category_id IS :categoryId AND (t.is_done = 0 OR t.id IN (:keepIds))
        ORDER BY t.priority DESC, t.date IS NULL, t.date, t.time IS NULL, t.time,
            CASE WHEN t.date IS NULL THEN -t.created_at ELSE t.created_at END, t.id
        """
    )
    fun observeOpenTasksOfCategory(categoryId: Long?, keepIds: List<Long>): Flow<List<TaskWithDetails>>

    /** Невыполненные задачи без даты: по приоритету, внутри — новые сверху. [keepIds] — как в [observeOpenTasksBetween]. */
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
        ORDER BY t.priority DESC, t.created_at DESC, t.id DESC
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
     * Все задачи для поиска с названиями подзадач. Совпадения ищет приложение: LIKE в SQLite
     * не различает регистр только у латиницы. После переноса это около 8,6 тыс. строк — немного.
     */
    @Query(
        """
        SELECT t.*,
            c.name AS category_name,
            c.color_index AS category_color_index,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id AND s.is_done = 1) AS subtasks_done,
            (SELECT COUNT(*) FROM subtasks s WHERE s.task_id = t.id) AS subtasks_total,
            (SELECT GROUP_CONCAT(s.title, char(10)) FROM subtasks s WHERE s.task_id = t.id) AS subtask_titles
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        """
    )
    fun observeSearchableTasks(): Flow<List<SearchableTask>>

    /** Задачи с [from] по [to] для точек в календаре в порядке списка дня (приоритет, время) — чтобы цвета шли в порядке задач. */
    @Query(
        """
        SELECT t.date, t.is_done, c.color_index AS category_color_index
        FROM tasks t
        LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.date BETWEEN :from AND :to
        ORDER BY t.date, t.priority DESC, t.time IS NULL, t.time, t.created_at, t.id
        """
    )
    fun observeCalendarMarks(from: LocalDate, to: LocalDate): Flow<List<CalendarTaskMark>>

    /**
     * Невыполненные повторяющиеся задачи с датой — от них календарь считает будущие повторы.
     * Выполненные не нужны: при выполнении рядом уже создана невыполненная копия.
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
        WHERE t.is_done = 0 AND t.repeat_unit != 0 AND t.date IS NOT NULL
        ORDER BY t.priority DESC, t.time IS NULL, t.time, t.created_at, t.id
        """
    )
    fun observeOpenRepeatingTasks(): Flow<List<TaskWithDetails>>

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

    /** Названия первых просроченных задач в порядке экрана «Просроченные»: по приоритету, внутри — старые сверху. */
    @Query(
        """
        SELECT title FROM tasks
        WHERE is_done = 0 AND date < :today
        ORDER BY priority DESC, date, time IS NULL, time, created_at
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
