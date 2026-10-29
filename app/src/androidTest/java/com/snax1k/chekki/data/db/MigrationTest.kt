package com.snax1k.chekki.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * Миграции базы: данные, записанные в схему версии 5 (первую, с которой база больше не пересоздаётся),
 * должны дойти до текущей версии. Схемы старых версий MigrationTestHelper берёт из `app/schemas`.
 * Когда появится новая миграция, сюда добавляется проверка именно её изменений.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ChekkiDatabase::class.java)

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun данныеВерсии5ДоходятДоТекущей() {
        val day = LocalDate.of(2026, 10, 25).toEpochDay()
        helper.createDatabase(DB_NAME, ChekkiDatabase.FIRST_KEPT_VERSION).apply {
            execSQL("INSERT INTO categories (id, name, color_index, icon, position, is_archived) VALUES (1, 'Работа', 0, 1, 0, 0)")
            execSQL(
                """
                INSERT INTO tasks (id, title, note, date, time, category_id, priority, reminder_minutes_before, is_done,
                    completed_at, created_at, repeated_from_id, repeat_unit, repeat_every, repeat_weekdays)
                VALUES (1, 'Отчёт за квартал', 'Копию — бухгалтеру', $day, 840, 1, 3, 15, 0, NULL, 0, NULL, 2, 1, 0)
                """.trimIndent()
            )
            execSQL("INSERT INTO subtasks (id, task_id, title, is_done, position) VALUES (1, 1, 'Собрать цифры', 1, 0)")
            execSQL(
                """
                INSERT INTO habits (id, title, icon, color_index, daily_target, weekdays, position, is_archived, created_at)
                VALUES (1, 'Пить воду', 2, 1, 6, 127, 0, 0, 0)
                """.trimIndent()
            )
            execSQL("INSERT INTO habit_checks (habit_id, date, count, target) VALUES (1, $day, 4, 6)")
            close()
        }

        // Миграции проходят, и получившаяся схема совпадает с той, что ждёт Room
        helper.runMigrationsAndValidate(DB_NAME, ChekkiDatabase.VERSION, true, *Migrations.ALL).close()

        val database = Room.databaseBuilder(context, ChekkiDatabase::class.java, DB_NAME)
            .addMigrations(*Migrations.ALL)
            .build()
        try {
            runBlocking {
                val task = database.taskDao().getTaskWithDetails(1)!!
                assertEquals("Отчёт за квартал", task.task.title)
                assertEquals(LocalTime.of(14, 0), task.task.time)
                assertEquals("Работа", task.categoryName)
                assertEquals(1, task.subtasksDone)
                assertEquals(15, task.task.reminderMinutesBefore)
                assertEquals(4, database.habitDao().observeChecks(1).first().single().count)
                // 5 → 6: прежний высокий приоритет (код 3) — уровень «Высокий», самый важный
                assertEquals(PriorityLevels.HIGH, task.task.priority)
                assertEquals("Высокий", task.priorityName)
                assertEquals(0, task.priorityPosition)
                assertEquals(
                    listOf("Высокий", "Средний", "Низкий"),
                    database.priorityLevelDao().getAll().map { it.name }
                )
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun уКаждойВерсииПослеПятойЕстьМиграция() {
        val expected = (ChekkiDatabase.FIRST_KEPT_VERSION until ChekkiDatabase.VERSION).map { it to it + 1 }
        assertEquals(expected, Migrations.ALL.map { it.startVersion to it.endVersion })
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
