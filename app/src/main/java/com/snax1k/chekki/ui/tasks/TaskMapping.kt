package com.snax1k.chekki.ui.tasks

import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.db.TaskWithDetails

/** Задача из базы → строка списка. */
fun TaskWithDetails.toTaskUi(): TaskUi = TaskUi(
    id = task.id,
    title = task.title,
    time = task.time,
    category = if (categoryName != null && categoryColorIndex != null) {
        CategoryUi(name = categoryName, colorIndex = categoryColorIndex)
    } else {
        null
    },
    priority = priorityName?.let { name ->
        Priority(id = task.priority, name = name, color = priorityColor ?: 0, position = priorityPosition ?: 0)
    },
    subtasksDone = subtasksDone,
    subtasksTotal = subtasksTotal,
    repeats = task.repeat.unit != RepeatUnit.None,
    hasReminder = task.reminderMinutesBefore != null,
    done = task.isDone,
    date = task.date
)
