package com.snax1k.chekki.ui.group

import com.snax1k.chekki.ui.tasks.Priority
import com.snax1k.chekki.ui.tasks.TaskUi
import java.time.LocalDate

/**
 * Задачи под общим подзаголовком. [day] = null — секция без подзаголовка, а с [isNoDate] — под подзаголовком
 * «Без даты» (задачи без даты в конце списка категории). С [isPriority] подзаголовок — уровень [priority]
 * («Высокий», …; null — «Без приоритета»). С [isDone] — свёрнутый раздел «Выполнено сегодня» внизу «Сегодня».
 */
data class TaskSection(
    val day: LocalDate?,
    val tasks: List<TaskUi>,
    val isNoDate: Boolean = false,
    val isPriority: Boolean = false,
    val priority: Priority? = null,
    val isDone: Boolean = false
)

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

/**
 * Как [sectionsByDay], но задачи без даты — под подзаголовком «Без даты»: в списке категории они идут
 * после датированных. Список должен быть уже отсортирован по дню, задачи без даты — в конце.
 */
fun sectionsWithNoDateLast(tasks: List<Pair<LocalDate?, TaskUi>>): List<TaskSection> =
    sectionsByDay(tasks).map { section -> if (section.day == null) section.copy(isNoDate = true) else section }

/**
 * Список на несколько дней (решено 26.10.2026): если хоть у одной задачи есть приоритет — разделы
 * «Высокий», «Средний», «Низкий», «Без приоритета» в порядке прихода, а день пишется в строке задачи.
 * Без приоритетов разделять нечего — тогда по дням, как [sectionsByDay] (с [noDateLast] —
 * как [sectionsWithNoDateLast]). Список должен быть уже отсортирован по приоритету, внутри — по дню.
 */
fun sectionsByPriorityOrDay(tasks: List<Pair<LocalDate?, TaskUi>>, noDateLast: Boolean = false): List<TaskSection> {
    if (tasks.none { (_, task) -> task.priority != null }) {
        return if (noDateLast) sectionsWithNoDateLast(tasks) else sectionsByDay(tasks)
    }
    val sections = mutableListOf<TaskSection>()
    for ((_, task) in tasks) {
        val shown = task.copy(showDate = true)
        val last = sections.lastOrNull()
        if (last != null && last.priority == task.priority) {
            sections[sections.lastIndex] = last.copy(tasks = last.tasks + shown)
        } else {
            sections += TaskSection(day = null, tasks = listOf(shown), isPriority = true, priority = task.priority)
        }
    }
    return sections
}
