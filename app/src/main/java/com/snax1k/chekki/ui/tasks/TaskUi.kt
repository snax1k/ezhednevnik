package com.snax1k.chekki.ui.tasks

import com.snax1k.chekki.data.db.PriorityLevelEntity
import com.snax1k.chekki.data.db.PriorityLevels
import java.time.LocalDate
import java.time.LocalTime

/**
 * Уровень важности задачи на экране: название, цвет флажка (ARGB) и место по важности (0 — самый важный).
 * [id] — уровень в `priority_levels`. Уровни у пользователя свои (решено 29.10.2026).
 */
data class Priority(val id: Long, val name: String, val color: Int, val position: Int = 0) {
    companion object {
        // Уровни по умолчанию — для превью и тестов
        val High = PriorityLevels.defaults[0].toPriority()
        val Medium = PriorityLevels.defaults[1].toPriority()
        val Low = PriorityLevels.defaults[2].toPriority()
    }
}

fun PriorityLevelEntity.toPriority() = Priority(id = id, name = name, color = color, position = position)

/** Категория задачи. Цвет — номер в палитре категорий темы (`ChekkiTheme.colors.categories`). */
data class CategoryUi(
    val name: String,
    val colorIndex: Int
)

/** Задача в списке. */
data class TaskUi(
    val id: Long,
    val title: String,
    /** Время задачи; null — на весь день. */
    val time: LocalTime?,
    val category: CategoryUi?,
    val priority: Priority?,
    val subtasksDone: Int = 0,
    val subtasksTotal: Int = 0,
    /** Сколько вложений (фото, видео, аудио, файлы). */
    val attachments: Int = 0,
    val repeats: Boolean = false,
    val hasReminder: Boolean = false,
    val done: Boolean = false,
    /** Дата задачи; null — без даты. Нужна меню «Отложить», чтобы не предлагать ту же дату. */
    val date: LocalDate? = null,
    /** Показывать дату в строке свойств («30 окт · 14:00») — в поиске, где задачи разных дней идут вместе. */
    val showDate: Boolean = false,
    /** Что подсветить в названии — найденное слово поиска. */
    val titleHighlight: IntRange? = null,
    /** Строка под названием: где нашлось, если не в названии (кусок заметки или подзадача). */
    val matchSnippet: String? = null
)
