package com.snax1k.chekki.ui.tasks

import java.time.LocalDate
import java.time.LocalTime

enum class Priority { High, Medium, Low }

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
    val repeats: Boolean = false,
    val hasReminder: Boolean = false,
    val done: Boolean = false,
    /** Дата задачи; null — без даты. Нужна меню «Отложить», чтобы не предлагать ту же дату. */
    val date: LocalDate? = null
)
