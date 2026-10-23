package com.snax1k.chekki.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snax1k.chekki.ChekkiApplication
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * Будильник привычки ([ACTION_REMIND]) и кнопка «Отметить» в её уведомлении ([ACTION_CHECK]).
 * По будильнику показывает уведомление, если напоминание ещё актуально, и ставит следующий будильник.
 */
class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        val container = (context.applicationContext as ChekkiApplication).container
        val repository = container.habitRepository
        // Чтение базы — не в главном потоке; goAsync даёт приёмнику дождаться конца работы
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_REMIND -> {
                        val firedAt = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis()))
                        repository.reminderToShow(habitId, firedAt)?.let { (habit, count) ->
                            HabitNotifications.show(context, habit, count)
                        }
                    }
                    ACTION_CHECK -> {
                        val count = repository.check(habitId, LocalDate.now()) ?: return@launch
                        val habit = repository.getHabit(habitId) ?: return@launch
                        // Цель набрана — уведомление уже убрано, иначе показываем новое «3 из 6»
                        if (count < habit.dailyTarget) HabitNotifications.show(context, habit, count)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.snax1k.chekki.action.HABIT_REMIND"
        const val ACTION_CHECK = "com.snax1k.chekki.action.HABIT_CHECK"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
