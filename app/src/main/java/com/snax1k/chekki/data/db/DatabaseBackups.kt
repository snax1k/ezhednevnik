package com.snax1k.chekki.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Копии базы на случай, если обновление приложения пойдёт не так. Лежат во внутренней папке приложения
 * (`files/backups`), хранятся последние [KEEP]:
 * - перед установкой обновления из приложения ([backupOpen]);
 * - перед миграцией, когда новая версия впервые открывает базу старой схемы ([backupBeforeMigration]) —
 *   так копия есть, даже если обновление поставили вручную из APK.
 *
 * Копия — целая база одним файлом: с Android 11 через `VACUUM INTO` (согласованный снимок),
 * раньше — после переноса журнала WAL в основной файл.
 */
object DatabaseBackups {

    /** Сколько последних копий хранить. */
    const val KEEP = 3

    private const val TAG = "DatabaseBackups"

    fun directory(context: Context): File = File(context.filesDir, "backups")

    /** Копия открытой базы. [label] попадает в имя файла: «before-0.4». */
    fun backupOpen(context: Context, database: ChekkiDatabase, label: String): File {
        val target = newBackupFile(context, label)
        val db = database.openHelper.writableDatabase
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            db.execSQL("VACUUM INTO ${sqlString(target.path)}")
        } else {
            db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            context.getDatabasePath(ChekkiDatabase.NAME).copyTo(target)
        }
        prune(context)
        return target
    }

    /**
     * Копия перед миграцией: база [ChekkiDatabase.NAME] ещё не открыта Room, а её версия — от [ChekkiDatabase.FIRST_KEPT_VERSION]
     * до текущей не включительно. Ошибка копирования не мешает открыть базу: копия — страховка, а не условие.
     */
    fun backupBeforeMigration(context: Context) {
        val path = context.getDatabasePath(ChekkiDatabase.NAME)
        if (!path.exists()) return
        try {
            SQLiteDatabase.openDatabase(
                path.path,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
            ).use { db ->
                val version = db.version
                if (version < ChekkiDatabase.FIRST_KEPT_VERSION || version >= ChekkiDatabase.VERSION) return
                val target = newBackupFile(context, "v$version-before-migration")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    db.execSQL("VACUUM INTO ${sqlString(target.path)}")
                } else {
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                    path.copyTo(target)
                }
            }
            prune(context)
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось сохранить копию базы перед миграцией", e)
        }
    }

    private fun newBackupFile(context: Context, label: String): File {
        val dir = directory(context).apply { mkdirs() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        // VACUUM INTO не пишет поверх существующего файла
        return File(dir, "chekki-$label-$stamp.db").apply { delete() }
    }

    /** Оставить [KEEP] самых свежих копий. */
    private fun prune(context: Context) {
        directory(context).listFiles { file -> file.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEEP)
            ?.forEach { it.delete() }
    }

    /** Строка для SQL в одинарных кавычках. */
    private fun sqlString(value: String): String = "'" + value.replace("'", "''") + "'"
}
