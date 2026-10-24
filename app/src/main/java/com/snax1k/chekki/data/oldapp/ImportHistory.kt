package com.snax1k.chekki.data.oldapp

import android.content.Context
import java.time.LocalDate

/**
 * Какие копии уже переносились: подпись копии (`backup_meta.hash`) → день переноса. Нужна, чтобы предупредить
 * о повторном переносе той же копии — иначе задачи задвоятся. Хранится в SharedPreferences, как последние запросы поиска.
 */
class ImportHistory(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** Когда эту копию уже переносили; null — не переносили (или у копии нет подписи). */
    fun importedOn(hash: String?): LocalDate? {
        if (hash.isNullOrBlank()) return null
        val day = preferences.getLong(KEY_PREFIX + hash, Long.MIN_VALUE)
        return if (day == Long.MIN_VALUE) null else LocalDate.ofEpochDay(day)
    }

    fun remember(hash: String?, day: LocalDate) {
        if (hash.isNullOrBlank()) return
        preferences.edit().putLong(KEY_PREFIX + hash, day.toEpochDay()).apply()
    }

    private companion object {
        const val PREFERENCES = "old_app_import"
        const val KEY_PREFIX = "imported_"
    }
}
