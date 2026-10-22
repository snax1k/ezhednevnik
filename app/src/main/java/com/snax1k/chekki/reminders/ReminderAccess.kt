package com.snax1k.chekki.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit

/** Разрешения напоминаний для панели новой задачи и переходы в системные настройки. */
class ReminderAccess(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager: AlarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarms(alarmManager)

    /** Объяснение про точные будильники показываем один раз, что бы пользователь ни ответил. */
    var exactAlarmExplained: Boolean
        get() = preferences.getBoolean(KEY_EXACT_ALARM_EXPLAINED, false)
        set(value) = preferences.edit { putBoolean(KEY_EXACT_ALARM_EXPLAINED, value) }

    private companion object {
        const val PREFERENCES = "reminders"
        const val KEY_EXACT_ALARM_EXPLAINED = "exact_alarm_explained"
    }
}

/** Экран «Будильники и напоминания» для приложения (Android 12+). */
fun exactAlarmSettingsIntent(context: Context): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    } else {
        null
    }

/** Настройки уведомлений приложения. */
fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
