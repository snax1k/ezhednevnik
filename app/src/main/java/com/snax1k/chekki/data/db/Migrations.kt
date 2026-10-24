package com.snax1k.chekki.data.db

import androidx.room.migration.Migration

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
    /** Все миграции по порядку. Пока пусто: версия 5 — первая, с которой данные сохраняются. */
    val ALL: Array<Migration> = arrayOf()
}
