package com.snax1k.chekki.ui.task

import com.snax1k.chekki.data.EditedSubtask
import com.snax1k.chekki.ui.addtask.TaskDraft

/**
 * Подзадача в карточке до «Сохранить» (с 23.11.2026 карточка сохраняет только по кнопке).
 * [key] — постоянный ключ строки на экране; [id] — id в базе, null — подзадачу добавили в этой карточке.
 */
data class CardSubtask(
    val key: Long,
    val id: Long?,
    val title: String,
    val done: Boolean
)

/** Строка подзадачи на экране. */
data class SubtaskRowUi(
    val key: Long,
    val title: String,
    val done: Boolean,
    val isEditing: Boolean
)

/**
 * Поля задачи, которые уйдут в базу: без пробелов по краям; пустое название — прежнее [saved]
 * (стёртое название не сохраняется, как и раньше).
 */
internal fun TaskDraft.forSave(saved: TaskDraft): TaskDraft =
    copy(title = title.trim().ifEmpty { saved.title }, note = note.trim())

/**
 * Подзадачи, которые уйдут в базу, по порядку. Пустая новая строка пропускается, у прежней подзадачи
 * со стёртым названием остаётся название из [saved].
 */
internal fun subtasksForSave(rows: List<CardSubtask>, saved: List<CardSubtask>): List<EditedSubtask> =
    rows.mapNotNull { row ->
        val title = row.title.trim().ifEmpty {
            row.id?.let { id -> saved.firstOrNull { it.id == id }?.title } ?: return@mapNotNull null
        }
        EditedSubtask(id = row.id, title = title, isDone = row.done)
    }

/**
 * Есть ли несохранённые правки: то, что уйдёт в базу по «Сохранить», отличается от того, что там лежит.
 * Пробелы по краям, пустая новая подзадача и стёртое (а значит, не сохраняемое) название правками не считаются.
 */
internal fun hasUnsavedChanges(
    draft: TaskDraft,
    saved: TaskDraft,
    subtasks: List<CardSubtask>,
    savedSubtasks: List<CardSubtask>
): Boolean =
    draft.forSave(saved) != saved.forSave(saved) || subtasksForSave(subtasks, savedSubtasks) != subtasksForSave(savedSubtasks, savedSubtasks)

/** Строки подзадач для экрана; [editingKey] — какую сейчас правят. */
internal fun subtaskRows(subtasks: List<CardSubtask>, editingKey: Long?): List<SubtaskRowUi> =
    subtasks.map { SubtaskRowUi(key = it.key, title = it.title, done = it.done, isEditing = it.key == editingKey) }
