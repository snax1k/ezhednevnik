package com.snax1k.chekki.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snax1k.chekki.ChekkiApplication
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Срабатывает по будильнику задачи и показывает уведомление, если напоминание ещё актуально. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, -1L)
        if (taskId < 0) return
        val container = (context.applicationContext as ChekkiApplication).container
        // Чтение базы — не в главном потоке; goAsync даёт приёмнику дождаться конца работы
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                val details = container.taskRepository.getTaskWithDetails(taskId) ?: return@launch
                // Задачу могли выполнить или перенести, а будильник остался — тогда молчим
                val actualTrigger = details.task.reminderTrigger(ZoneId.systemDefault())?.toEpochMilli()
                if (actualTrigger == triggerAt) {
                    TaskNotifications.show(context, details, LocalDate.now())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
