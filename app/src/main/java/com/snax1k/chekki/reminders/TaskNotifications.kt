package com.snax1k.chekki.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.TaskWithDetails
import com.snax1k.chekki.ui.components.formatDayMonthShort
import com.snax1k.chekki.ui.components.formatShortWeekday
import com.snax1k.chekki.ui.components.formatTime
import java.time.LocalDate

/** Уведомления о задачах: канал «Напоминания о задачах» и само уведомление. */
object TaskNotifications {

    private const val CHANNEL_REMINDERS = "task_reminders"

    /** Каналы создаются при запуске приложения; повторное создание ничего не меняет. */
    fun createChannels(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_REMINDERS, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.notification_channel_reminders))
            .setDescription(context.getString(R.string.notification_channel_reminders_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** Можно ли показывать уведомления: с Android 13 нужно разрешение, и пользователь мог выключить их в настройках. */
    fun canPost(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notificationId(taskId: Long): Int = taskId.toInt()

    // Разрешение проверяется в canPost
    @SuppressLint("MissingPermission")
    fun show(context: Context, details: TaskWithDetails, today: LocalDate) {
        if (!canPost(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.chekki_accent))
            .setContentTitle(details.task.title)
            .setContentText(whenText(context, details, today))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(details.task.id), notification)
    }

    /** «Сегодня в 19:00 · Работа», «Завтра, весь день», «Сб, 24 окт в 10:00». */
    private fun whenText(context: Context, details: TaskWithDetails, today: LocalDate): String {
        val date = details.task.date ?: return details.categoryName.orEmpty()
        val day = when (date) {
            today -> context.getString(R.string.quick_date_today)
            today.plusDays(1) -> context.getString(R.string.quick_date_tomorrow)
            else -> "${formatShortWeekday(date)}, ${formatDayMonthShort(date)}"
        }
        val time = details.task.time
        val dayAndTime = if (time != null) {
            context.getString(R.string.notification_when_time, day, formatTime(time))
        } else {
            context.getString(R.string.notification_when_all_day, day)
        }
        return details.categoryName?.let { context.getString(R.string.notification_with_category, dayAndTime, it) }
            ?: dayAndTime
    }

    /** Нажатие открывает приложение так же, как значок на рабочем столе. Общее с уведомлениями привычек. */
    internal fun openAppIntent(context: Context): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
