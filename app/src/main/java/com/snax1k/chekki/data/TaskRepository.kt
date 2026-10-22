package com.snax1k.chekki.data

import androidx.room.withTransaction
import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.GroupCounts
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.SubtaskEntity
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.data.db.TaskWithDetails
import com.snax1k.chekki.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Задачи для экранов. Все списки и счётчики — потоки: при изменении базы экраны обновятся сами.
 * Об изменениях задач с напоминаниями сообщает [reminders], чтобы будильники совпадали с базой.
 */
class TaskRepository(
    private val database: ChekkiDatabase,
    private val reminders: ReminderScheduler = ReminderScheduler.None
) {

    private val taskDao = database.taskDao()
    private val categoryDao = database.categoryDao()

    /** Все категории в порядке списка. */
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    /**
     * Новая задача. [time] null — на весь день; [reminderMinutesBefore] null — без напоминания.
     * Подзадачи сохраняются в том же порядке, неотмеченными. Возвращает id задачи.
     */
    suspend fun addTask(
        title: String,
        note: String,
        date: LocalDate?,
        time: LocalTime?,
        categoryId: Long?,
        priority: TaskPriority,
        repeat: RepeatRule = RepeatRule.None,
        reminderMinutesBefore: Int? = null,
        subtasks: List<String> = emptyList(),
        now: Instant = Instant.now()
    ): Long {
        val task = TaskEntity(
            title = title,
            note = note,
            date = date,
            time = time,
            categoryId = categoryId,
            priority = priority,
            repeat = repeat,
            reminderMinutesBefore = reminderMinutesBefore,
            createdAt = now
        )
        val taskId = database.withTransaction {
            val taskId = taskDao.insertTask(task)
            if (subtasks.isNotEmpty()) {
                taskDao.insertSubtasks(
                    subtasks.mapIndexed { index, subtaskTitle ->
                        SubtaskEntity(taskId = taskId, title = subtaskTitle, position = index)
                    }
                )
            }
            taskId
        }
        reminders.update(task.copy(id = taskId))
        return taskId
    }

    /** Задача с категорией и подзадачами; null — задачи уже нет. */
    suspend fun getTaskWithDetails(id: Long): TaskWithDetails? = taskDao.getTaskWithDetails(id)

    /** Задача для карточки: поток обновляется при любом её изменении; null — задачи нет. */
    fun observeTask(id: Long): Flow<TaskWithDetails?> = taskDao.observeTaskWithDetails(id)

    /** Подзадачи по порядку. */
    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>> = taskDao.observeSubtasks(taskId)

    /**
     * Меняет поля задачи из карточки и переставляет будильник. Отметку выполнения не трогает:
     * её меняет только [toggleDone], поэтому запоздавшее сохранение названия её не перезапишет.
     */
    suspend fun updateTask(
        id: Long,
        title: String,
        note: String,
        date: LocalDate?,
        time: LocalTime?,
        categoryId: Long?,
        priority: TaskPriority,
        repeat: RepeatRule,
        reminderMinutesBefore: Int?
    ) {
        val updated = database.withTransaction {
            taskDao.updateEditableFields(
                id = id,
                title = title,
                note = note,
                date = date,
                time = time,
                categoryId = categoryId,
                priority = priority,
                repeatUnit = repeat.unit,
                repeatEvery = repeat.every,
                repeatWeekdays = repeat.weekdays,
                reminderMinutesBefore = reminderMinutesBefore
            )
            taskDao.getTask(id)
        } ?: return
        reminders.update(updated)
    }

    /**
     * Переносит задачу на [date] («Отложить»): время, повтор и напоминание остаются, будильник
     * переставляется на новую дату. Возвращает прежнюю дату для отмены; null — задачи нет.
     */
    suspend fun moveTask(id: Long, date: LocalDate?): MovedTask? {
        val moved = database.withTransaction {
            val task = taskDao.getTask(id) ?: return@withTransaction null
            taskDao.setDate(id, date)
            task.copy(date = date) to MovedTask(id, previousDate = task.date, newDate = date)
        } ?: return null
        reminders.update(moved.first)
        return moved.second
    }

    /**
     * Удаляет задачу вместе с подзадачами и будильником. Возвращает всё, что нужно для [restoreTask];
     * null — задачи уже не было.
     */
    suspend fun deleteTask(id: Long): DeletedTask? {
        val deleted = database.withTransaction {
            val task = taskDao.getTask(id) ?: return@withTransaction null
            val deleted = DeletedTask(task, taskDao.getSubtasks(id), taskDao.getRepeatCopyIds(id))
            taskDao.deleteTask(id)
            deleted
        } ?: return null
        reminders.cancel(id)
        return deleted
    }

    /** Возвращает удалённую задачу как была: с тем же id, подзадачами, связью с копиями повтора и будильником. */
    suspend fun restoreTask(deleted: DeletedTask) {
        database.withTransaction {
            taskDao.insertTask(deleted.task)
            if (deleted.subtasks.isNotEmpty()) taskDao.insertSubtasks(deleted.subtasks)
            if (deleted.repeatCopyIds.isNotEmpty()) taskDao.setRepeatedFrom(deleted.repeatCopyIds, deleted.task.id)
        }
        reminders.update(deleted.task)
    }

    suspend fun setSubtaskDone(id: Long, isDone: Boolean) = taskDao.setSubtaskDone(id, isDone)

    /** Новая неотмеченная подзадача в конце списка. Возвращает её id. */
    suspend fun addSubtask(taskId: Long, title: String): Long = database.withTransaction {
        taskDao.insertSubtask(SubtaskEntity(taskId = taskId, title = title, position = taskDao.nextSubtaskPosition(taskId)))
    }

    suspend fun renameSubtask(id: Long, title: String) = taskDao.renameSubtask(id, title)

    suspend fun deleteSubtask(id: Long) = taskDao.deleteSubtask(id)

    /** Задачи на дату, включая выполненные. */
    fun observeTasksOn(date: LocalDate): Flow<List<TaskWithDetails>> = taskDao.observeTasksOn(date)

    /**
     * Невыполненные задачи в диапазоне дат. Задачи из [keepIds] остаются в списке, даже если их выполнили.
     * Для открытых диапазонов («Просроченные», «Потом») границей служат `LocalDate.MIN` и `LocalDate.MAX`:
     * в базе это просто очень большие числа дней.
     */
    fun observeOpenTasksBetween(
        dates: ClosedRange<LocalDate>,
        keepIds: Set<Long> = emptySet()
    ): Flow<List<TaskWithDetails>> =
        taskDao.observeOpenTasksBetween(dates.start, dates.endInclusive, keepIds.toList())

    /** Невыполненные задачи без даты, новые сверху. [keepIds] — как в [observeOpenTasksBetween]. */
    fun observeOpenTasksWithoutDate(keepIds: Set<Long> = emptySet()): Flow<List<TaskWithDetails>> =
        taskDao.observeOpenTasksWithoutDate(keepIds.toList())

    /** Последние [limit] выполненных задач, свежие сверху, и задачи из [keepIds], даже если с них сняли отметку. */
    fun observeDoneTasks(limit: Int, keepIds: Set<Long> = emptySet()): Flow<List<TaskWithDetails>> =
        taskDao.observeDoneTasks(limit, keepIds.toList())

    /** Сколько всего выполненных задач: по нему экран решает, показывать ли «Показать ещё». */
    fun observeDoneCount(): Flow<Int> = taskDao.observeDoneCount()

    /** Число невыполненных задач по группам главного экрана для дня [today]. */
    fun observeGroupCounts(today: LocalDate): Flow<GroupCounts> {
        val ranges = GroupRanges.of(today)
        return taskDao.observeGroupCounts(
            today = today,
            tomorrow = ranges.tomorrow,
            weekStart = ranges.thisWeek.start,
            weekEnd = ranges.thisWeek.endInclusive,
            monthStart = ranges.thisMonth.start,
            monthEnd = ranges.thisMonth.endInclusive,
            laterFrom = ranges.laterFrom
        )
    }

    /** Названия первых [limit] просроченных задач. */
    fun observeOverdueTitles(today: LocalDate, limit: Int): Flow<List<String>> =
        taskDao.observeOverdueTitles(today, limit)

    /**
     * Ставит или снимает отметку выполнения.
     * Выполненная повторяющаяся задача остаётся выполненной, а рядом создаётся копия на следующую дату
     * с неотмеченными подзадачами. Если отметку снять, эта копия удаляется, пока её саму не выполнили.
     */
    suspend fun toggleDone(taskId: Long, today: LocalDate = LocalDate.now(), now: Instant = Instant.now()) {
        // Будильники трогаем после транзакции, когда изменения уже в базе
        val remindersToUpdate = mutableListOf<TaskEntity>()
        val remindersToCancel = mutableListOf<Long>()
        database.withTransaction {
            val task = taskDao.getTask(taskId) ?: return@withTransaction
            if (task.isDone) {
                taskDao.setDone(taskId, isDone = false, completedAt = null)
                remindersToCancel += taskDao.getOpenRepeatIds(taskId)
                taskDao.deleteOpenRepeatsOf(taskId)
                remindersToUpdate += task.copy(isDone = false, completedAt = null)
                return@withTransaction
            }
            taskDao.setDone(taskId, isDone = true, completedAt = now)
            remindersToCancel += taskId
            val nextDate = task.date?.let { task.repeat.nextOccurrence(after = it, notBefore = today) }
                ?: return@withTransaction
            val copy = task.copy(
                id = 0,
                date = nextDate,
                isDone = false,
                completedAt = null,
                createdAt = now,
                repeatedFromId = task.id
            )
            val copyId = taskDao.insertTask(copy)
            remindersToUpdate += copy.copy(id = copyId)
            val subtasks = taskDao.getSubtasks(taskId)
            if (subtasks.isNotEmpty()) {
                taskDao.insertSubtasks(subtasks.map { it.copy(id = 0, taskId = copyId, isDone = false) })
            }
        }
        remindersToCancel.forEach(reminders::cancel)
        remindersToUpdate.forEach(reminders::update)
    }

    /**
     * Ставит будильники всех будущих напоминаний заново: при запуске приложения, после перезагрузки,
     * смены времени или часового пояса.
     */
    suspend fun rescheduleAllReminders(today: LocalDate = LocalDate.now()) {
        taskDao.getUpcomingReminders(today).forEach(reminders::update)
    }
}

/** Удалённая задача со всем, что нужно, чтобы вернуть её кнопкой «Отменить». */
data class DeletedTask(
    val task: TaskEntity,
    val subtasks: List<SubtaskEntity>,
    /** Копии повтора, созданные из этой задачи: при удалении их связь обнуляется, при отмене возвращается. */
    val repeatCopyIds: List<Long>
)

/** Перенесённая задача: прежняя дата нужна, чтобы вернуть её кнопкой «Отменить». */
data class MovedTask(
    val taskId: Long,
    val previousDate: LocalDate?,
    val newDate: LocalDate?
)
