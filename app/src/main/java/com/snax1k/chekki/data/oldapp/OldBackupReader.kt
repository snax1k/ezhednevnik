package com.snax1k.chekki.data.oldapp

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.ZipInputStream

/**
 * Чтение файла `.plnb`: это zip, перед которым несколько лишних байт (в изученной копии — 8). Ищем начало
 * zip по подписи `PK\u0003\u0004`, достаём `Planner.db` во временный файл в [workDir] и читаем его
 * стандартным SQLite Android только на чтение. Фото и аудио не распаковываются — только считаются.
 */
object OldBackupReader {

    /** Нет `Planner.db` или файл не похож на копию старого ежедневника. */
    class NotABackupException(message: String) : IOException(message)

    fun read(input: InputStream, workDir: File): OldBackup {
        val databaseFile = File(workDir, "old-planner.db")
        try {
            var photoCount = 0
            var audioCount = 0
            var hash: String? = null
            var hasDatabase = false
            ZipInputStream(skipToZip(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.name == DATABASE_ENTRY -> {
                            databaseFile.outputStream().use { zip.copyTo(it) }
                            hasDatabase = true
                        }
                        entry.name == META_ENTRY -> hash = readHash(zip.readBytes())
                        entry.name.startsWith("photo/") && !entry.isDirectory -> photoCount++
                        entry.name.startsWith("audio/") && !entry.isDirectory -> audioCount++
                    }
                }
            }
            if (!hasDatabase) throw NotABackupException("В архиве нет $DATABASE_ENTRY")
            return readDatabase(databaseFile).copy(photoCount = photoCount, audioCount = audioCount, hash = hash)
        } finally {
            databaseFile.delete()
            File(databaseFile.path + "-journal").delete()
        }
    }

    /** Пропускает байты до подписи zip. Подпись ищется в первых [MAX_PREFIX] байтах. */
    private fun skipToZip(input: InputStream): InputStream {
        val stream = PushbackInputStream(input.buffered(), ZIP_SIGNATURE.size)
        val window = ByteArray(ZIP_SIGNATURE.size)
        var filled = 0
        var skipped = 0
        while (skipped <= MAX_PREFIX) {
            val byte = stream.read()
            if (byte < 0) break
            if (filled < window.size) {
                window[filled++] = byte.toByte()
            } else {
                System.arraycopy(window, 1, window, 0, window.size - 1)
                window[window.size - 1] = byte.toByte()
                skipped++
            }
            if (filled == window.size && window.contentEquals(ZIP_SIGNATURE)) {
                stream.unread(window)
                return stream
            }
        }
        throw NotABackupException("Не найдено начало архива")
    }

    private fun readHash(bytes: ByteArray): String? =
        runCatching { JSONObject(String(bytes)).optString("hash").ifEmpty { null } }.getOrNull()

    /** Читает нужные таблицы; удалённые строки (`isRemoved = 1`) не берёт. */
    fun readDatabase(file: File): OldBackup {
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        return db.use {
            OldBackup(
                categories = it.rows("SELECT uid, title, icon, color, orderPosition FROM category WHERE isRemoved = 0") { c ->
                    OldCategory(c.str(0)!!, c.str(1).orEmpty(), c.str(2), c.getLong(3).toInt(), c.getInt(4))
                },
                tasks = it.rows(
                    """
                    SELECT uid, title, text, completed, importanceType, created, nextDate, time, completedDate,
                        notifyDate, deadline, regularityUid
                    FROM task WHERE isRemoved = 0
                    """
                ) { c ->
                    OldTask(
                        uid = c.str(0)!!,
                        title = c.str(1).orEmpty(),
                        text = c.str(2),
                        completed = c.getInt(3) != 0,
                        importance = c.str(4),
                        created = c.str(5),
                        nextDate = c.str(6),
                        time = c.str(7),
                        completedDate = c.str(8),
                        notifyDate = c.str(9),
                        deadline = c.str(10),
                        regularityUid = c.str(11)
                    )
                },
                subtasks = it.rows("SELECT taskId, title, position, completed FROM subTask WHERE isRemoved = 0") { c ->
                    OldSubtask(c.str(0)!!, c.str(1).orEmpty(), c.getInt(2), c.getInt(3) != 0)
                },
                taskCategories = it.rows("SELECT taskId, categoryId, orderPosition FROM categoryInTask WHERE isRemoved = 0") { c ->
                    OldTaskCategory(c.str(0)!!, c.str(1)!!, c.getInt(2))
                },
                regularities = it.rows("SELECT uid, type, period, daysMask, byTwoWeeks FROM regularity WHERE isRemoved = 0") { c ->
                    OldRegularity(c.str(0)!!, c.str(1).orEmpty(), c.getInt(2), c.getLong(3), c.getInt(4) != 0)
                },
                habits = it.rows(
                    "SELECT uid, title, icon, color, regularityUid, position, startDate, pause, completed FROM habit WHERE isRemoved = 0"
                ) { c ->
                    OldHabit(
                        uid = c.str(0)!!,
                        title = c.str(1).orEmpty(),
                        icon = c.str(2),
                        color = c.getLong(3).toInt(),
                        regularityUid = c.str(4),
                        position = c.getInt(5),
                        startDate = c.str(6),
                        paused = c.getInt(7) != 0,
                        completed = c.getInt(8) != 0
                    )
                },
                habitRecords = it.rows("SELECT habitId, count, maxCount, date FROM habit_record WHERE isRemoved = 0") { c ->
                    OldHabitRecord(c.str(0)!!, c.getInt(1), c.getInt(2), c.str(3)!!)
                },
                implementations = it.rows("SELECT habitId, notifyTime FROM implementation WHERE isRemoved = 0") { c ->
                    OldImplementation(c.str(0)!!, c.str(1))
                }
            )
        }
    }

    private fun <T> SQLiteDatabase.rows(sql: String, map: (Cursor) -> T): List<T> =
        rawQuery(sql, null).use { cursor -> buildList { while (cursor.moveToNext()) add(map(cursor)) } }

    private fun Cursor.str(index: Int): String? = if (isNull(index)) null else getString(index)

    private const val DATABASE_ENTRY = "Planner.db"
    private const val META_ENTRY = "backup_meta"
    private const val MAX_PREFIX = 1024
    private val ZIP_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
}
