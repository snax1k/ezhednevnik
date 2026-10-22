package com.snax1k.chekki.ui.group

import com.snax1k.chekki.ui.tasks.TaskUi
import java.time.LocalDate

/** Задачи одного дня под общим подзаголовком. [day] = null — секция без подзаголовка. */
data class TaskSection(val day: LocalDate?, val tasks: List<TaskUi>)

/**
 * Раскладывает задачи по дням в том порядке, в каком они пришли: подряд идущие задачи одного дня —
 * одна секция. Список должен быть уже отсортирован по дню.
 */
fun sectionsByDay(tasks: List<Pair<LocalDate?, TaskUi>>): List<TaskSection> {
    val sections = mutableListOf<Pair<LocalDate?, MutableList<TaskUi>>>()
    for ((day, task) in tasks) {
        if (sections.isEmpty() || sections.last().first != day) sections += day to mutableListOf()
        sections.last().second += task
    }
    return sections.map { (day, dayTasks) -> TaskSection(day, dayTasks) }
}
