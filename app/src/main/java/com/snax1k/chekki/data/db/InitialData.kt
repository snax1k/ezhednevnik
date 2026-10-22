package com.snax1k.chekki.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/** Категории первого запуска. Номер в списке + 1 — это id, номер в списке — цвет в палитре темы. */
internal val DefaultCategories = listOf("Работа", "Личное", "Дом", "Здоровье", "Учёба", "Финансы")

/**
 * Начальные данные при создании базы: стандартные категории всегда, тестовые задачи — только
 * в отладочной сборке. SampleTasks для отладки лежит в src/debug, для выпуска — пустой в src/release.
 */
class InitialDataCallback(private val today: () -> LocalDate = LocalDate::now) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) = fill(db)

    // После пересоздания базы при смене схемы таблицы пустые, заполняем заново
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) = fill(db)

    private fun fill(db: SupportSQLiteDatabase) {
        DefaultCategories.forEachIndexed { index, name ->
            db.insert(
                "categories",
                SQLiteDatabase.CONFLICT_ABORT,
                contentValuesOf("id" to index + 1L, "name" to name, "color_index" to index, "position" to index)
            )
        }
        SampleTasks.insert(db, today())
    }
}
