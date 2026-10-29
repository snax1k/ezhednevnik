package com.snax1k.chekki.data.oldapp

import androidx.room.withTransaction
import com.snax1k.chekki.data.AttachmentStore
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.StoredFile
import com.snax1k.chekki.data.db.AttachmentEntity
import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.HabitPauseEntity
import com.snax1k.chekki.data.db.HabitReminderEntity
import com.snax1k.chekki.data.db.PriorityLevels
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
 * и записывается план — либо всё, либо ничего. Фото и аудио копии [read] распаковывает в папку переноса
 * [attachmentStore]; при записи они становятся вложениями задач. Без [attachmentStore] (в тестах) — только считаются.
 */
class OldAppImporter(
    private val database: ChekkiDatabase,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val attachmentStore: AttachmentStore? = null,
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
        // Фото и аудио прошлой, брошенной копии больше не нужны
        attachmentStore?.clearImportDirectory()
        val backup = try {
            OldBackupReader.read(input, workDir, attachmentStore?.importDirectory)
        } catch (e: Throwable) {
            attachmentStore?.clearImportDirectory()
            throw e
        }
        return backup to OldAppConverter.convert(backup, clock.zone, Instant.now(clock))
    }

    /** Перенос не состоялся (ушли с экрана, выбрали другой файл): распакованные фото и аудио удаляются. */
    fun discard() {
        attachmentStore?.clearImportDirectory()
    }

    suspend fun import(plan: ImportPlan) {
        val today = LocalDate.now(clock)
        backup()
        // Будильники и уведомления удаляемых задач и привычек больше не нужны
        taskRepository.cancelAllReminders()
        habitRepository.cancelAllReminders()
        // Файлы — к вложениям до записи: не запишется — останутся без строк и удалятся при следующем запуске
        val storedMedia: List<List<Pair<StoredFile, Instant>>> = plan.tasks.map { planned ->
            val store = attachmentStore ?: return@map emptyList()
            planned.attachments.mapNotNull { media ->
                store.adopt(File(store.importDirectory, media.fileName), media.displayName, media.mimeType)?.let { it to media.createdAt }
            }
        }
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

            // Ранг важности старого приложения (3 — высокая) → уровень пользователя на том же месте по важности
            val levels = database.priorityLevelDao().getAll()
            val levelByRank = mapOf(
                PriorityLevels.HIGH to levels.getOrNull(0)?.id,
                PriorityLevels.MEDIUM to levels.getOrNull(1)?.id,
                PriorityLevels.LOW to levels.getOrNull(2)?.id
            )
            val attachmentDao = database.attachmentDao()
            plan.tasks.forEachIndexed { index, planned ->
                val taskId = taskDao.insertTask(
                    planned.task.copy(
                        categoryId = planned.categoryUid?.let(categoryIds::get),
                        priority = levelByRank[planned.task.priority] ?: PriorityLevels.NONE
                    )
                )
                if (planned.subtasks.isNotEmpty()) {
                    taskDao.insertSubtasks(
                        planned.subtasks.map { SubtaskEntity(taskId = taskId, title = it.title, isDone = it.isDone, position = it.position) }
                    )
                }
                val media = storedMedia[index]
                if (media.isNotEmpty()) {
                    attachmentDao.insertAll(
                        media.mapIndexed { position, (file, createdAt) ->
                            AttachmentEntity(
                                taskId = taskId,
                                kind = file.kind,
                                fileName = file.fileName,
                                displayName = file.displayName,
                                mimeType = file.mimeType,
                                size = file.size,
                                durationMs = file.durationMs,
                                createdAt = createdAt,
                                position = position
                            )
                        }
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
        attachmentStore?.clearImportDirectory()
        // Будильники перенесённых напоминаний
        taskRepository.rescheduleAllReminders(today)
        habitRepository.rescheduleAllReminders()
    }
}
