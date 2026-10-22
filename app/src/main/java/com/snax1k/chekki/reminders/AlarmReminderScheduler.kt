package com.snax1k.chekki.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.snax1k.chekki.data.db.TaskEntity
import java.time.Clock
import java.time.Instant

/**
 * Один системный будильник на задачу. Если разрешены точные будильники — срабатывает минута в минуту,
 * иначе Android может задержать напоминание (обычно до часа, в режиме сна телефона дольше).
 */
class AlarmReminderScheduler(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone()
) : ReminderScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun update(task: TaskEntity) {
        val trigger = task.reminderTrigger(clock.zone)
        if (trigger == null || !trigger.isAfter(clock.instant())) {
            // Напоминать не о чем или время уже прошло. Уведомление не трогаем: оно могло только что появиться
            cancelAlarm(task.id)
            return
        }
        val alarmIntent = alarmIntent(task.id, trigger, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), alarmIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), alarmIntent)
        }
    }

    override fun cancel(taskId: Long) {
        cancelAlarm(taskId)
        NotificationManagerCompat.from(context).cancel(TaskNotifications.notificationId(taskId))
    }

    private fun cancelAlarm(taskId: Long) {
        alarmIntent(taskId, trigger = null, PendingIntent.FLAG_NO_CREATE)?.let { alarmIntent ->
            alarmManager.cancel(alarmIntent)
            alarmIntent.cancel()
        }
    }

    private fun alarmIntent(taskId: Long, trigger: Instant?, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            // Адрес задачи делает будильники разных задач различимыми для системы
            .setData(Uri.parse("chekki://task/$taskId"))
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        if (trigger != null) {
            intent.putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, trigger.toEpochMilli())
        }
        return PendingIntent.getBroadcast(context, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }
}

/** Можно ли ставить точные будильники. До Android 12 отдельного разрешения нет. */
fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
