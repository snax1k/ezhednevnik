package com.snax1k.chekki.reminders

import com.snax1k.chekki.data.db.TaskEntity

/** Будильники напоминаний. Репозиторий сообщает сюда, когда задачи добавляют или отмечают. */
interface ReminderScheduler {
    /** Ставит будильник задачи на время её напоминания или снимает его, если напоминать не о чем. */
    fun update(task: TaskEntity)

    /** Снимает будильник задачи и убирает её уведомление, если оно уже показано. */
    fun cancel(taskId: Long)

    companion object {
        /** Ничего не делает: для тестов базы. */
        val None: ReminderScheduler = object : ReminderScheduler {
            override fun update(task: TaskEntity) = Unit
            override fun cancel(taskId: Long) = Unit
        }
    }
}
