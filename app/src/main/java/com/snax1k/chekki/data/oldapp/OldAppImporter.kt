package com.snax1k.chekki.data.oldapp

import androidx.room.withTransaction
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.HabitReminderEntity
import com.snax1k.chekki.data.db.SubtaskEntity
import java.io.File
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Что сейчас есть в приложении — перенос это заменит. */
data class ExistingData(val tasks: Int, val categories: Int, val habits: Int)

/**
 * Перенос из старого ежедневника: [read] читает файл `.plnb` и готовит план с отчётом (ничего не меняя),
 * [import] заменяет всё, что есть в приложении, данными копии (решено 29.10.2026, раньше перенос добавлял).
 * Сначала [backup] сохраняет копию базы, потом одной транзакцией удаляются задачи, категории и привычки
 * и записывается план — либо всё, либо ничего.
 */
class OldAppImporter(
    private val database: ChekkiDatabase,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    /** Копия базы перед заменой; не вышло — перенос не начинается. */
    private val backup: () -> Unit = {}
) {

    /** Сколько задач, категорий и привычек удалит перенос. */
    suspend fun existing(): ExistingData = ExistingData(
        tasks = database.taskDao().countTasks(),
        categories = database.categoryDao().countCategories(),
        habits = database.habitDao().countHabits()
    )

    /** Читает копию и переводит её в наши данные. Временные файлы — в [workDir], после чтения удаляются. */
    fun read(input: InputStream, workDir: File): Pair<OldBackup, ImportPlan> {
        val backup = OldBackupReader.read(input, workDir)
        return backup to OldAppConverter.convert(backup, clock.zone, Instant.now(clock))
    }

    suspend fun import(plan: ImportPlan) {
        val today = LocalDate.now(clock)
        backup()
        // Будильники и уведомления удаляемых задач и привычек больше не нужны
        taskRepository.cancelAllReminders()
        habitRepository.cancelAllReminders()
        database.withTransaction {
            val categoryDao = database.categoryDao()
            val taskDao = database.taskDao()
            val habitDao = database.habitDao()

            taskDao.deleteAllSubtasks()
            taskDao.deleteAllTasks()
            categoryDao.deleteAllCategories()
            habitDao.deleteAllHabits()

            var categoryPosition = categoryDao.nextPosition()
            val categoryIds = plan.categories.associate { category ->
                category.oldUid to categoryDao.insertCategory(
                    CategoryEntity(
                        name = category.name,
                        colorIndex = category.colorIndex,
                        icon = category.icon,
                        position = categoryPosition++
                    )
                )
            }

            plan.tasks.forEach { planned ->
                val taskId = taskDao.insertTask(planned.task.copy(categoryId = planned.categoryUid?.let(categoryIds::get)))
                if (planned.subtasks.isNotEmpty()) {
                    taskDao.insertSubtasks(
                        planned.subtasks.map { SubtaskEntity(taskId = taskId, title = it.title, isDone = it.isDone, position = it.position) }
                    )
                }
            }

            var habitPosition = habitDao.nextPosition()
            plan.habits.forEach { planned ->
                val habitId = habitDao.insertHabit(planned.habit.copy(position = habitPosition++))
                if (planned.checks.isNotEmpty()) habitDao.insertChecks(planned.checks.map { it.copy(habitId = habitId) })
                if (planned.reminderTimes.isNotEmpty()) {
                    habitDao.insertReminders(planned.reminderTimes.map { HabitReminderEntity(habitId, it) })
                }
                if (planned.paused) habitDao.insertPause(HabitPauseEntity(habitId = habitId, startDate = today, resumeDate = null))
            }
        }
        // Будильники перенесённых напоминаний
        taskRepository.rescheduleAllReminders(today)
        habitRepository.rescheduleAllReminders()
    }
}
