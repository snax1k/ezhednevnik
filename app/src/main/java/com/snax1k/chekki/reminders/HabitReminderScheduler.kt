package com.snax1k.chekki.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.Instant

/** Будильники напоминаний привычек. Репозиторий привычек сам считает время и сообщает его сюда. */
interface HabitReminderScheduler {
    /** Ставит единственный будильник привычки на [trigger]; null — снимает его. */
    fun update(habitId: Long, trigger: Instant?)

    /** Снимает будильник и убирает уведомление привычки (её удалили). */
    fun cancel(habitId: Long)

    /** Убирает показанное уведомление привычки (цель на сегодня набрана). */
    fun dismiss(habitId: Long)

    companion object {
        /** Ничего не делает: для тестов базы. */
        val None: HabitReminderScheduler = object : HabitReminderScheduler {
            override fun update(habitId: Long, trigger: Instant?) = Unit
            override fun cancel(habitId: Long) = Unit
            override fun dismiss(habitId: Long) = Unit
        }
    }
}

/**
 * Один системный будильник на привычку — на её ближайшее напоминание; сработал — ставится следующий.
 * Так у привычки с шестью напоминаниями в день занят один будильник, а не шесть.
 * Точность — как у задач: точные будильники, если разрешены, иначе Android может задержать.
 */
class AlarmHabitReminderScheduler(private val context: Context) : HabitReminderScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun update(habitId: Long, trigger: Instant?) {
        if (trigger == null) {
            cancelAlarm(habitId)
            return
        }
        val alarmIntent = alarmIntent(habitId, trigger, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), alarmIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), alarmIntent)
        }
    }

    override fun cancel(habitId: Long) {
        cancelAlarm(habitId)
        dismiss(habitId)
    }

    override fun dismiss(habitId: Long) {
        HabitNotifications.cancel(context, habitId)
    }

    private fun cancelAlarm(habitId: Long) {
        alarmIntent(habitId, trigger = null, PendingIntent.FLAG_NO_CREATE)?.let { alarmIntent ->
            alarmManager.cancel(alarmIntent)
            alarmIntent.cancel()
        }
    }

    private fun alarmIntent(habitId: Long, trigger: Instant?, flag: Int): PendingIntent? {
        val intent = Intent(context, HabitReminderReceiver::class.java)
            .setAction(HabitReminderReceiver.ACTION_REMIND)
            // Адрес привычки делает будильники разных привычек различимыми для системы
            .setData(Uri.parse("chekki://habit/$habitId"))
            .putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        if (trigger != null) {
            intent.putExtra(HabitReminderReceiver.EXTRA_TRIGGER_AT, trigger.toEpochMilli())
        }
        return PendingIntent.getBroadcast(context, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }
}
