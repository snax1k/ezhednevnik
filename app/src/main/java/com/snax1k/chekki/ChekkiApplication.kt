package com.snax1k.chekki

import android.app.Application
import android.content.Context
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.RecentSearches
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.currentDateFlow
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.DatabaseBackups
import com.snax1k.chekki.data.oldapp.ImportHistory
import com.snax1k.chekki.data.oldapp.OldAppImporter
import com.snax1k.chekki.reminders.AlarmHabitReminderScheduler
import com.snax1k.chekki.reminders.AlarmReminderScheduler
import com.snax1k.chekki.reminders.HabitNotifications
import com.snax1k.chekki.reminders.ReminderAccess
import com.snax1k.chekki.reminders.ReminderScheduler
import com.snax1k.chekki.reminders.TaskNotifications
import com.snax1k.chekki.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ChekkiApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        TaskNotifications.createChannels(this)
        HabitNotifications.createChannel(this)
        // Будильники могли пропасть (например, после очистки данных или если система их сбросила) —
        // при каждом запуске ставим их заново, это быстро
        container.applicationScope.launch {
            container.taskRepository.rescheduleAllReminders()
            container.habitRepository.rescheduleAllReminders()
        }
    }
}

/** Общие объекты приложения. Создаются один раз, при первом обращении; база открывается тогда же. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Фоновая работа, которая не привязана к экрану: будильники, уведомления. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ChekkiDatabase by lazy { ChekkiDatabase.create(appContext) }
    val reminderScheduler: ReminderScheduler by lazy { AlarmReminderScheduler(appContext) }
    val reminderAccess: ReminderAccess by lazy { ReminderAccess(appContext) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database, reminderScheduler) }
    val habitRepository: HabitRepository by lazy { HabitRepository(database, AlarmHabitReminderScheduler(appContext)) }

    /** Перенос из старого ежедневника и копии, которые уже переносились. */
    val oldAppImporter: OldAppImporter by lazy {
        OldAppImporter(database, taskRepository, habitRepository) {
            DatabaseBackups.backupOpen(appContext, database, "before-import")
        }
    }
    val importHistory: ImportHistory by lazy { ImportHistory(appContext) }

    /** Последние запросы поиска. */
    val recentSearches: RecentSearches by lazy { RecentSearches(appContext) }

    /** Обновление приложения из выпусков на GitHub. */
    val updateManager: UpdateManager by lazy { UpdateManager(appContext, applicationScope) { database } }

    /** Сегодняшняя дата, меняется в полночь. */
    val currentDate: Flow<LocalDate> = currentDateFlow()
}
