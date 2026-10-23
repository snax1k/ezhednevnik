package com.snax1k.chekki.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.HabitEntity

/**
 * Уведомления о привычках: свой канал «Напоминания о привычках» — его можно приглушить, не трогая задачи.
 * В уведомлении кнопка «Отметить» (+1 прямо из шторки).
 */
object HabitNotifications {

    private const val CHANNEL_HABITS = "habit_reminders"

    /** У задач и привычек номера пересекаются, поэтому уведомления привычек — со своей меткой. */
    private const val TAG = "habit"

    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_HABITS, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.notification_channel_habits))
            .setDescription(context.getString(R.string.notification_channel_habits_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** «Пить воду · 2 из 6 сегодня» с кнопкой «Отметить». Повторный показ обновляет то же уведомление. */
    // Разрешение проверяется в canPost
    @SuppressLint("MissingPermission")
    fun show(context: Context, habit: HabitEntity, count: Int) {
        if (!TaskNotifications.canPost(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_HABITS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.chekki_accent))
            .setContentTitle(habit.title)
            .setContentText(context.getString(R.string.habit_notification_progress, count, habit.dailyTarget))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Обновление после «Отметить» не звенит повторно
            .setOnlyAlertOnce(true)
            .setContentIntent(TaskNotifications.openAppIntent(context))
            .addAction(0, context.getString(R.string.habit_notification_check), checkIntent(context, habit.id))
            .build()
        NotificationManagerCompat.from(context).notify(TAG, notificationId(habit.id), notification)
    }

    fun cancel(context: Context, habitId: Long) {
        NotificationManagerCompat.from(context).cancel(TAG, notificationId(habitId))
    }

    private fun notificationId(habitId: Long): Int = habitId.toInt()

    private fun checkIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java)
            .setAction(HabitReminderReceiver.ACTION_CHECK)
            .setData(Uri.parse("chekki://habit/$habitId"))
            .putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
