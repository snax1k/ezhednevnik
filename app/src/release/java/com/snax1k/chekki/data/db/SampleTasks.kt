package com.snax1k.chekki.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/** В выпускной сборке тестовых задач нет. Отладочная версия — в src/debug. */
internal object SampleTasks {
    @Suppress("UNUSED_PARAMETER")
    fun insert(db: SupportSQLiteDatabase, today: LocalDate) = Unit
}
