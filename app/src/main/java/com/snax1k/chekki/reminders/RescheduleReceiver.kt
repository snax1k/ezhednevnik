package com.snax1k.chekki.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snax1k.chekki.ChekkiApplication
import kotlinx.coroutines.launch

/**
 * Ставит все будильники напоминаний заново. Android стирает их при выключении телефона,
 * а при смене времени или часового пояса их время нужно пересчитать.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = (context.applicationContext as ChekkiApplication).container
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                container.taskRepository.rescheduleAllReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Пользователь разрешил точные будильники — переставляем их точными
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
