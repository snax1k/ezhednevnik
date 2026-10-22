package com.snax1k.chekki.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** База задач. */
@Database(
    entities = [CategoryEntity::class, TaskEntity::class, SubtaskEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChekkiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        fun create(context: Context): ChekkiDatabase =
            Room.databaseBuilder(context, ChekkiDatabase::class.java, "chekki.db")
                .addCallback(InitialDataCallback())
                // Пока приложение не выпущено, при изменении схемы база пересоздаётся.
                // Перед первым релизом это нужно убрать и писать миграции.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
