package com.snax1k.chekki.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snax1k.chekki.data.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseBackupsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: ChekkiDatabase

    @Before
    fun setUp() {
        DatabaseBackups.directory(context).deleteRecursively()
        database = Room.inMemoryDatabaseBuilder(context, ChekkiDatabase::class.java)
            .addCallback(InitialDataCallback(today = { LocalDate.of(2026, 10, 27) }))
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        DatabaseBackups.directory(context).deleteRecursively()
    }

    @Test
    fun копияСодержитДанныеИЧитается() {
        runBlocking {
            TaskRepository(database).addTask("Проверить копию", "", null, null, null, PriorityLevels.HIGH)
        }
        val backup = DatabaseBackups.backupOpen(context, database, "before-0.5")
        assertTrue(backup.name.startsWith("chekki-before-0.5-"))
        SQLiteDatabase.openDatabase(backup.path, null, SQLiteDatabase.OPEN_READONLY).use { copy ->
            assertEquals(ChekkiDatabase.VERSION, copy.version)
            copy.rawQuery("SELECT priority FROM tasks WHERE title = 'Проверить копию'", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(PriorityLevels.HIGH, cursor.getLong(0))
            }
        }
    }

    @Test
    fun хранятсяТолькоПоследниеКопии() {
        repeat(DatabaseBackups.KEEP + 2) { index ->
            DatabaseBackups.backupOpen(context, database, "before-test$index")
            // Время изменения файла на части файловых систем — с точностью до секунды
            Thread.sleep(1100)
        }
        val left = DatabaseBackups.directory(context).listFiles()!!.map { it.name }
        assertEquals(DatabaseBackups.KEEP, left.size)
        assertTrue(left.none { it.contains("before-test0") || it.contains("before-test1") })
    }
}
