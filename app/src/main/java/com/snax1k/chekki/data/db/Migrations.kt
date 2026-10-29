package com.snax1k.chekki.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграции базы с версии 5. Как добавить изменение схемы:
 * 1. Поменять сущности и поднять [ChekkiDatabase.VERSION] — Room сохранит новую схему в `app/schemas`.
 * 2. Добавить сюда `Migration(n, n + 1)` с SQL, который переводит старую схему в новую,
 *    не теряя данных (ALTER TABLE, новая таблица и перенос строк и т. п.).
 * 3. Дописать в `MigrationTest` проверку, что данные версии n дошли до новой версии.
 *
 * Схемы в `app/schemas` нужны тестам миграций — их нельзя удалять и менять вручную.
 */
object Migrations {
    /**
     * 5 → 6: свои уровни важности (29.10.2026). Новая таблица `priority_levels` с тремя прежними приоритетами
     * под теми же id (1 — низкий, 2 — средний, 3 — высокий), поэтому колонка `tasks.priority` не меняется:
     * её код и есть id уровня, 0 — без приоритета.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `priority_levels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `color` INTEGER NOT NULL, `position` INTEGER NOT NULL)"
            )
            insertDefaultPriorityLevels(db)
        }
    }

    /** Все миграции по порядку. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6)
}

/** Три уровня по умолчанию — при миграции и при создании новой базы. */
internal fun insertDefaultPriorityLevels(db: SupportSQLiteDatabase) {
    PriorityLevels.defaults.forEach { level ->
        db.execSQL(
            "INSERT INTO priority_levels (id, name, color, position) VALUES (?, ?, ?, ?)",
            arrayOf<Any>(level.id, level.name, level.color, level.position)
        )
    }
}
