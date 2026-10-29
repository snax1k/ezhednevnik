package com.snax1k.chekki.data.oldapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.InitialDataCallback
import com.snax1k.chekki.data.db.PriorityLevels
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Перенос целиком на устройстве: искусственная копия `.plnb` (8 лишних байт, zip с `Planner.db` старой схемы,
 * `backup_meta` и фото) → чтение → запись в базу с тестовыми данными отладочной сборки.
 * Настоящая копия пользователя в тестах не используется: в ней личные данные.
 */
@RunWith(AndroidJUnit4::class)
class OldAppImporterTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 10, 25)
    private val clock = Clock.fixed(Instant.parse("2026-10-25T09:00:00Z"), ZoneOffset.ofHours(3))
    private lateinit var database: ChekkiDatabase
    private lateinit var importer: OldAppImporter
    private var backups = 0

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, ChekkiDatabase::class.java)
            .addCallback(InitialDataCallback(today = { today }))
            .build()
        importer = OldAppImporter(database, TaskRepository(database), HabitRepository(database, clock = clock), clock) { backups++ }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun копияЧитаетсяИЗаменяетВсё() {
        runBlocking {
            // То, что было в приложении до переноса, должно исчезнуть
            TaskRepository(database).addTask("Старая задача", "", today, null, null, PriorityLevels.HIGH)
            val before = importer.existing()
            assertTrue(before.tasks >= 1 && before.categories >= 6)

            val (backup, plan) = importer.read(ByteArrayInputStream(plnb()), context.cacheDir)
            assertEquals("abc123", backup.hash)
            assertEquals(1, backup.photoCount)
            // Удалённые строки не читаются
            assertEquals(listOf("Поездки"), backup.categories.map { it.title })
            assertEquals(2, backup.tasks.size)

            importer.import(plan)
            assertEquals(1, backups)

            val categories = database.categoryDao().observeAll().first()
            assertEquals(listOf("Поездки"), categories.map { it.name })
            val trips = categories.single()
            assertEquals("Поездки", trips.name)

            val open = database.taskDao().observeOpenTasksOfCategory(trips.id, emptyList()).first().single()
            assertEquals("Купить билеты", open.task.title)
            assertEquals(LocalDate.of(2026, 10, 30), open.task.date)
            assertEquals(2, open.subtasksTotal)
            assertEquals(1, open.subtasksDone)
            assertEquals(1, database.taskDao().observeDoneCount().first())
            assertEquals(ExistingData(tasks = 2, categories = 1, habits = 1), importer.existing())
            assertTrue(database.taskDao().observeTasksOn(today).first().none { it.task.title == "Старая задача" })

            val habits = database.habitDao().observeAllHabits(today).first()
            val water = habits.single()
            assertEquals("Пить воду", water.habit.title)
            assertEquals(2, water.habit.dailyTarget)
            assertEquals(1, water.reminderCount)
            assertEquals(1, database.habitDao().getChecks(water.habit.id).single().count)
            assertTrue(plan.report.openTasks == 1 && plan.report.doneTasks == 1)
        }
    }

    /** Копия `.plnb`, собранная в памяти: 8 лишних байт, затем zip. */
    private fun plnb(): ByteArray {
        val dbFile = File(context.cacheDir, "test-planner.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            OldSchema.forEach(db::execSQL)
            db.execSQL("INSERT INTO category VALUES ('c1', 'Поездки', 'car_1', ${0xFF6FC6F2.toInt()}, 0, 0, 0)")
            db.execSQL("INSERT INTO category VALUES ('c2', 'Старая', 'home', 0, 1, 1, 0)")
            db.execSQL(
                """INSERT INTO task VALUES ('t1', '2026-10-01T10:00:00Z', 'Купить билеты', '', 0, 'medium', NULL, NULL,
                '2026-10-30', NULL, '12:0', 0, NULL, 0)"""
            )
            db.execSQL(
                """INSERT INTO task VALUES ('t2', '2026-10-01T10:00:00Z', 'Сдать отчёт', NULL, 1, 'not', NULL, NULL,
                '2026-10-20', '2026-10-20T12:00:00Z', NULL, 0, NULL, 0)"""
            )
            db.execSQL(
                """INSERT INTO task VALUES ('t3', '2026-10-01T10:00:00Z', 'Удалённая', NULL, 0, 'not', NULL, NULL,
                '2026-10-30', NULL, NULL, 0, NULL, 1)"""
            )
            db.execSQL("INSERT INTO subTask VALUES ('t1', 'Паспорт', 0, 1, 0)")
            db.execSQL("INSERT INTO subTask VALUES ('t1', 'Деньги', 1, 0, 0)")
            db.execSQL("INSERT INTO categoryInTask VALUES ('t1', 'c1', 0, 0)")
            db.execSQL("INSERT INTO regularity VALUES ('week_1_254_0', 'week', 1, 254, 0, 0)")
            db.execSQL(
                """INSERT INTO habit VALUES ('h1', 'Пить воду', ${0xFF5C85F0.toInt()}, 'drop', 'week_1_254_0', 0,
                '2024-07-29T11:15:11Z', 0, 0, 0)"""
            )
            db.execSQL("INSERT INTO implementation VALUES ('h1', '20:0', 0)")
            db.execSQL("INSERT INTO implementation VALUES ('h1', NULL, 0)")
            db.execSQL("INSERT INTO habit_record VALUES ('h1', 1, 2, '2026-10-24T08:00:00Z', 0)")
        }
        val bytes = ByteArrayOutputStream()
        bytes.write(ByteArray(8) { 7 })
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("Planner.db"))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.putNextEntry(ZipEntry("backup_meta"))
            zip.write("""{"version":1,"hash":"abc123"}""".toByteArray())
            zip.putNextEntry(ZipEntry("photo/1.jpg"))
            zip.write(byteArrayOf(1, 2, 3))
        }
        dbFile.delete()
        return bytes.toByteArray()
    }

    private companion object {
        /** Таблицы старой базы — только колонки, которые читает перенос, в том же порядке, что в INSERT выше. */
        val OldSchema = listOf(
            "CREATE TABLE category (uid TEXT, title TEXT, icon TEXT, color INTEGER, orderPosition INTEGER, isRemoved INTEGER, isHidden INTEGER)",
            """CREATE TABLE task (uid TEXT, created TEXT, title TEXT, text TEXT, completed INTEGER, importanceType TEXT, deadline TEXT,
                notifyDate TEXT, nextDate TEXT, completedDate TEXT, time TEXT, notifyInsistent INTEGER, regularityUid TEXT, isRemoved INTEGER)""",
            "CREATE TABLE subTask (taskId TEXT, title TEXT, position INTEGER, completed INTEGER, isRemoved INTEGER)",
            "CREATE TABLE categoryInTask (taskId TEXT, categoryId TEXT, orderPosition INTEGER, isRemoved INTEGER)",
            "CREATE TABLE regularity (uid TEXT, type TEXT, period INTEGER, daysMask INTEGER, byTwoWeeks INTEGER, isRemoved INTEGER)",
            """CREATE TABLE habit (uid TEXT, title TEXT, color INTEGER, icon TEXT, regularityUid TEXT, position INTEGER,
                startDate TEXT, pause INTEGER, completed INTEGER, isRemoved INTEGER)""",
            "CREATE TABLE implementation (habitId TEXT, notifyTime TEXT, isRemoved INTEGER)",
            "CREATE TABLE habit_record (habitId TEXT, count INTEGER, maxCount INTEGER, date TEXT, isRemoved INTEGER)"
        )
    }
}
