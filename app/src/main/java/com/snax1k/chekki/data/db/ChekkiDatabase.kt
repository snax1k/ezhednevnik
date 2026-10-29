package com.snax1k.chekki.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * База задач и привычек. Версия 2 — привычки и их отметки, версия 3 — напоминания привычек, версия 4 — паузы привычек,
 * версия 5 — иконка и архив у категорий, версия 6 — свои уровни важности, версия 7 — вложения задач.
 *
 * С версии 5 (25.10.2026) база не пересоздаётся: в ней настоящие данные, в том числе перенесённые из старого
 * приложения. Каждое изменение схемы — новая версия и миграция в [Migrations], проверенная тестом `MigrationTest`.
 */
@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        HabitReminderEntity::class,
        HabitPauseEntity::class,
        PriorityLevelEntity::class,
        AttachmentEntity::class
    ],
    version = ChekkiDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChekkiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun categoryDao(): CategoryDao

    abstract fun habitDao(): HabitDao

    abstract fun priorityLevelDao(): PriorityLevelDao

    abstract fun attachmentDao(): AttachmentDao

    companion object {
        /** Текущая версия схемы. Подняли — добавьте миграцию в [Migrations]. */
        const val VERSION = 7

        /** С этой версии данные только переносятся миграциями. */
        const val FIRST_KEPT_VERSION = 5

        /** Имя файла базы. */
        const val NAME = "chekki.db"

        fun create(context: Context): ChekkiDatabase {
            // Новая версия приложения впервые открывает базу старой схемы — сначала копия, потом миграция
            DatabaseBackups.backupBeforeMigration(context)
            return build(context)
        }

        private fun build(context: Context): ChekkiDatabase =
            Room.databaseBuilder(context, ChekkiDatabase::class.java, NAME)
                .addCallback(InitialDataCallback())
                .addMigrations(*Migrations.ALL)
                // Версии до 5 были только у тестовых сборок с тестовыми данными: такую базу можно пересоздать.
                // Для версий с 5-й пересоздания нет: без миграции приложение упадёт, но данные не сотрёт.
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, *(1 until FIRST_KEPT_VERSION).toList().toIntArray())
                .build()
    }
}
