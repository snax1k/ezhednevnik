package com.snax1k.chekki.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/** Категории первого запуска. Номер в списке + 1 — это id, номер в списке — цвет в палитре темы. */
internal val DefaultCategories = listOf(
    "Работа" to CategoryIcon.Work,
    "Личное" to CategoryIcon.Personal,
    "Дом" to CategoryIcon.Home,
    "Здоровье" to CategoryIcon.Health,
    "Учёба" to CategoryIcon.Study,
    "Финансы" to CategoryIcon.Finance
)

/**
 * Начальные данные при создании базы: стандартные категории всегда, тестовые задачи и привычки — только
 * в отладочной сборке. SampleTasks и SampleHabits для отладки лежат в src/debug, для выпуска — пустые в src/release.
 */
class InitialDataCallback(private val today: () -> LocalDate = LocalDate::now) : RoomDatabase.Callback() {

    /** База пересоздана при смене схемы: заполнить её нужно, когда таблицы будут созданы заново. */
    private var refillOnOpen = false

    override fun onCreate(db: SupportSQLiteDatabase) = fill(db)

    // Room 2.8 вызывает это между удалением старых таблиц и созданием новых: писать сюда ещё некуда
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        refillOnOpen = true
    }

    // После пересоздания таблицы уже созданы, но пустые — заполняем заново
    override fun onOpen(db: SupportSQLiteDatabase) {
        if (refillOnOpen) {
            refillOnOpen = false
            fill(db)
        }
    }

    private fun fill(db: SupportSQLiteDatabase) {
        insertDefaultPriorityLevels(db)
        DefaultCategories.forEachIndexed { index, (name, icon) ->
            db.insert(
                "categories",
                SQLiteDatabase.CONFLICT_ABORT,
                contentValuesOf(
                    "id" to index + 1L,
                    "name" to name,
                    "color_index" to index,
                    "icon" to icon.code,
                    "position" to index,
                    "is_archived" to 0
                )
            )
        }
        SampleTasks.insert(db, today())
        SampleHabits.insert(db, today())
    }
}
