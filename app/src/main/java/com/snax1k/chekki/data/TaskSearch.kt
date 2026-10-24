package com.snax1k.chekki.data

import java.util.Locale

/**
 * Поиск задач по тексту. Сравнение без учёта регистра и без разницы между «ё» и «е»: «отчет» находит «Отчёт».
 * Встроенный LIKE в SQLite не различает регистр только у латиницы, поэтому совпадения ищутся здесь.
 */

/** Текст для сравнения: строчными буквами, «ё» → «е». Длина не меняется — позиции совпадают с исходным текстом. */
fun searchKey(text: String): String = text.lowercase(RussianLocale).replace('ё', 'е')

/** Слова запроса: «Отчёт  квартал» → «отчет», «квартал». */
fun searchWords(query: String): List<String> = searchKey(query).split(Whitespace).filter { it.isNotBlank() }

/** Где нашлось: часть названия для подсветки, или кусок заметки, или подзадача. */
data class SearchMatch(
    /** Первое слово запроса в названии; null — в названии слов запроса нет. */
    val titleRange: IntRange? = null,
    /** Если в названии ничего нет — строка заметки или название подзадачи, где нашлось первое слово. */
    val snippet: String? = null,
    val snippetIsSubtask: Boolean = false
)

/**
 * Подходит ли задача: каждое слово запроса есть в названии, заметке или в какой-нибудь подзадаче.
 * null — не подходит. Пустой запрос подходит всем.
 */
fun matchTask(words: List<String>, title: String, note: String, subtasks: List<String>): SearchMatch? {
    if (words.isEmpty()) return SearchMatch()
    val titleKey = searchKey(title)
    val noteKey = searchKey(note)
    val subtaskKeys = subtasks.map(::searchKey)
    val allFound = words.all { word -> word in titleKey || word in noteKey || subtaskKeys.any { word in it } }
    if (!allFound) return null
    words.firstNotNullOfOrNull { word ->
        titleKey.indexOf(word).takeIf { it >= 0 }?.let { start -> start until start + word.length }
    }?.let { return SearchMatch(titleRange = it) }
    val first = words.first()
    val noteIndex = noteKey.indexOf(first)
    if (noteIndex >= 0) return SearchMatch(snippet = noteSnippet(note, noteIndex))
    val subtask = subtasks.indices.firstOrNull { first in subtaskKeys[it] }?.let { subtasks[it] }
    return SearchMatch(snippet = subtask, snippetIsSubtask = subtask != null)
}

/** Строка заметки вокруг совпадения, не длиннее [MAX_SNIPPET] знаков, обрезанное — с «…». */
private fun noteSnippet(note: String, index: Int): String {
    val lineStart = note.lastIndexOf('\n', index - 1) + 1
    val lineEnd = note.indexOf('\n', index).let { if (it < 0) note.length else it }
    val line = note.substring(lineStart, lineEnd).trim()
    if (line.length <= MAX_SNIPPET) return line
    val inLine = index - lineStart
    val from = (inLine - MAX_SNIPPET / 3).coerceIn(0, line.length - MAX_SNIPPET)
    val prefix = if (from > 0) "…" else ""
    val suffix = if (from + MAX_SNIPPET < line.length) "…" else ""
    return prefix + line.substring(from, from + MAX_SNIPPET).trim() + suffix
}

private const val MAX_SNIPPET = 70
private val Whitespace = Regex("\\s+")
private val RussianLocale: Locale = Locale.forLanguageTag("ru")
