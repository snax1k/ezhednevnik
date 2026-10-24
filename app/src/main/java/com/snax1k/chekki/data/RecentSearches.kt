package com.snax1k.chekki.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Последние запросы поиска: не больше [MAX_RECENT], новые сверху. Хранятся в SharedPreferences, как отметка
 * про точные будильники в `ReminderAccess`: DataStore в проект не подключён, а для короткого списка хватает и этого.
 */
class RecentSearches(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<String>> = _items.asStateFlow()

    /** Запомнить запрос: он встаёт первым, такой же (без учёта регистра и ё/е) убирается. */
    fun add(query: String) = save(withRecent(_items.value, query))

    fun remove(query: String) = save(_items.value - query)

    fun clear() = save(emptyList())

    private fun load(): List<String> =
        preferences.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private fun save(list: List<String>) {
        _items.value = list
        // Запрос однострочный, поэтому перевод строки — надёжный разделитель
        preferences.edit().putString(KEY, list.joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val PREFERENCES = "search"
        const val KEY = "recent_queries"
        const val SEPARATOR = "\n"
    }
}

/** Сколько последних запросов помнить. */
const val MAX_RECENT = 8

/** Список последних запросов после поиска [query]: он первый, повтор убран, не больше [max]. Пустой не запоминается. */
fun withRecent(list: List<String>, query: String, max: Int = MAX_RECENT): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return list
    val key = searchKey(trimmed)
    return (listOf(trimmed) + list.filter { searchKey(it) != key }).take(max)
}
