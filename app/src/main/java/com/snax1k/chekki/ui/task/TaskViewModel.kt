package com.snax1k.chekki.ui.task

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.GroupRanges
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.ReminderAccess
import com.snax1k.chekki.ui.addtask.CategoryOption
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.addtask.TaskDraft
import com.snax1k.chekki.ui.main.TaskGroup
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Подзадача в карточке, как она сохранена в базе. */
data class SubtaskUi(
    val id: Long,
    val title: String,
    val done: Boolean
)

/** Подзадача, которую сейчас правят. Её текст живёт здесь, а не в базе, чтобы курсор не прыгал при наборе. */
data class SubtaskEditing(
    /** Ключ строки: id подзадачи или временный отрицательный ключ строки, которой ещё нет в базе. */
    val key: Long,
    val text: String,
    /** Строку только что добавили: если оставить её пустой, она исчезнет. */
    val isNew: Boolean,
    /** Название до начала правки: если стереть его целиком, вернётся оно. У новой строки пустое. */
    val originalTitle: String
)

/** Строка подзадачи на экране. [key] не меняется, когда новая подзадача появляется в базе, — так строка не теряет курсор. */
data class SubtaskRowUi(
    val key: Long,
    val title: String,
    val done: Boolean,
    val isEditing: Boolean
)

/**
 * Карточка задачи. Поля правятся на месте и сразу сохраняются в базу, кнопки «Сохранить» нет.
 * Как одно поле влияет на другие, решает [TaskDraft] — так же, как в панели новой задачи.
 */
class TaskViewModel(
    private val taskId: Long,
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>,
    private val reminderAccess: ReminderAccess
) : ViewModel() {

    /**
     * Поля, которые правятся в карточке; null — задача ещё не загрузилась.
     * Состояние Compose, а не StateFlow: поле ввода обновляется сразу, и курсор не прыгает при наборе.
     */
    var draft by mutableStateOf<TaskDraft?>(null)
        private set

    /** Задачи нет (удалили раньше, чем открылась карточка): карточку нужно закрыть. */
    var isMissing by mutableStateOf(false)
        private set

    /** Выполнена ли задача; отметку меняют кнопка и кружок, а не правка полей. */
    val isDone: StateFlow<Boolean> = repository.observeTask(taskId)
        .map { it?.task?.isDone == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val subtasks: StateFlow<List<SubtaskUi>> = repository.observeSubtasks(taskId)
        .map { list -> list.map { SubtaskUi(id = it.id, title = it.title, done = it.isDone) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryOption>> = repository.observeCategories()
        .map { list -> list.map { CategoryOption(id = it.id, name = it.name, colorIndex = it.colorIndex) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val today: StateFlow<LocalDate> = currentDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    /**
     * Название до начала правки. Пока поле названия пустое, в базе лежит оно (а не обрывок,
     * оставшийся от стирания по букве), и оно же возвращается в поле, когда из него уходят.
     */
    private var committedTitle = ""

    /** Последняя несохранённая правка. Сохраняются по одной и по порядку; промежуточные при быстром наборе пропускаются. */
    private val pendingSave = MutableStateFlow<TaskDraft?>(null)

    /** Задачу удаляют или откладывают, карточка закрывается: правки больше не сохраняем. */
    private var isClosing = false

    /** Подзадача, которую сейчас правят; null — никакую. */
    var editingSubtask by mutableStateOf<SubtaskEditing?>(null)
        private set

    /** Подзадачи, добавленные в этой карточке: id в базе → ключ строки (см. [SubtaskRowUi.key]). */
    var newSubtaskKeys by mutableStateOf<Map<Long, Long>>(emptyMap())
        private set

    /** Обратная связь: ключ новой строки → id в базе, когда подзадачу уже сохранили. */
    private val newSubtaskIds = mutableMapOf<Long, Long>()
    private var nextNewSubtaskKey = -1L

    /**
     * Изменения подзадач выполняются строго по очереди: переименование новой подзадачи
     * не должно обогнать её добавление в базу.
     */
    private val subtaskOps = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (op in subtaskOps) {
                if (isClosing) continue
                try {
                    op()
                } catch (_: SQLiteConstraintException) {
                    // Задачу удалили, пока изменение подзадачи ждало очереди, — сохранять некуда
                }
            }
        }
        viewModelScope.launch {
            val task = repository.getTaskWithDetails(taskId)?.task
            if (task == null) {
                isMissing = true
            } else {
                committedTitle = task.title
                draft = task.toDraft()
            }
        }
        viewModelScope.launch {
            pendingSave.filterNotNull().collect { if (!isClosing) save(it) }
        }
    }

    fun onTitleChange(title: String) = edit { it.copy(title = title) }

    /** Из поля названия ушли: пустое название возвращается к прежнему, непустое становится прежним для следующей правки. */
    fun onTitleEditEnd() {
        val current = draft ?: return
        if (current.title.isBlank()) draft = current.copy(title = committedTitle)
        else committedTitle = current.title.trim()
    }

    fun onNoteChange(note: String) = edit { it.copy(note = note) }

    fun onQuickDateSelect(quickDate: QuickDate) = edit { it.withQuickDate(quickDate) }

    fun onDatePick(date: LocalDate) = edit { it.withDate(date, today = LocalDate.now()) }

    /** Время из циферблата; null — на весь день. */
    fun onTimePick(time: LocalTime?) = edit { it.withTime(time) }

    fun onCategorySelect(categoryId: Long?) = edit { it.copy(categoryId = categoryId) }

    fun onPrioritySelect(priority: TaskPriority) = edit { it.copy(priority = priority) }

    fun onRepeatSelect(repeat: RepeatRule) = edit { it.withRepeat(repeat) }

    /** Напоминание за сколько минут; null — без напоминания. */
    fun onReminderSelect(minutesBefore: Int?) = edit { it.withReminder(minutesBefore) }

    fun toggleDone() {
        viewModelScope.launch { repository.toggleDone(taskId) }
    }

    fun toggleSubtask(row: SubtaskRowUi) = queueSubtaskOp {
        subtaskId(row.key)?.let { repository.setSubtaskDone(it, isDone = !row.done) }
    }

    /** Добавляет пустую строку в конец и начинает её правку. Вторую пустую строку не добавляет. */
    fun addSubtask() {
        val current = editingSubtask
        if (current != null && current.isNew && current.text.isBlank()) return
        finishSubtaskEdit()
        editingSubtask = SubtaskEditing(key = nextNewSubtaskKey--, text = "", isNew = true, originalTitle = "")
    }

    fun editSubtask(row: SubtaskRowUi) {
        if (editingSubtask?.key == row.key) return
        finishSubtaskEdit()
        editingSubtask = SubtaskEditing(key = row.key, text = row.title, isNew = false, originalTitle = row.title)
    }

    /**
     * Текст правящейся подзадачи. Сохраняется сразу. Новая строка попадает в базу с первой непустой буквы.
     * Пустой текст не сохраняется: при стирании по букве в базу успели бы попасть обрывки,
     * поэтому у прежней подзадачи сразу возвращается название до правки, а новая убирается из базы.
     */
    fun onSubtaskTextChange(text: String) {
        val current = editingSubtask ?: return
        editingSubtask = current.copy(text = text)
        val title = text.trim()
        val key = current.key
        if (title.isEmpty()) {
            if (current.isNew) {
                queueSubtaskOp {
                    newSubtaskIds.remove(key)?.let { id ->
                        repository.deleteSubtask(id)
                        newSubtaskKeys = newSubtaskKeys - id
                    }
                }
            } else {
                queueSubtaskOp { subtaskId(key)?.let { repository.renameSubtask(it, current.originalTitle) } }
            }
            return
        }
        queueSubtaskOp {
            val id = subtaskId(key)
            if (id != null) {
                repository.renameSubtask(id, title)
            } else {
                val newId = repository.addSubtask(taskId, title)
                newSubtaskIds[key] = newId
                newSubtaskKeys = newSubtaskKeys + (newId to key)
            }
        }
    }

    /** Строку [key] больше не правят: курсор ушёл. Если уже правят другую строку, ничего не делает. */
    fun onSubtaskEditEnd(key: Long) {
        if (editingSubtask?.key == key) finishSubtaskEdit()
    }

    fun deleteSubtask(key: Long) {
        if (editingSubtask?.key == key) editingSubtask = null
        queueSubtaskOp { subtaskId(key)?.let { repository.deleteSubtask(it) } }
    }

    private fun finishSubtaskEdit() {
        val current = editingSubtask ?: return
        editingSubtask = null
        // Новая строка, которую оставили пустой, исчезает. У прежней подзадачи при стирании
        // в базу уже вернулось название до правки — оно и покажется.
        if (current.isNew && current.text.isBlank()) {
            queueSubtaskOp { subtaskId(current.key)?.let { repository.deleteSubtask(it) } }
        }
    }

    /** id подзадачи по ключу строки; null — новую строку ещё не сохранили. */
    private fun subtaskId(key: Long): Long? = if (key > 0) key else newSubtaskIds[key]

    private fun queueSubtaskOp(op: suspend () -> Unit) {
        subtaskOps.trySend(op)
    }

    /** Нужно ли объяснить про точные будильники: напоминание есть, а точные будильники не разрешены. */
    fun needsExactAlarmExplanation(): Boolean =
        draft?.reminderMinutesBefore != null &&
            !reminderAccess.exactAlarmExplained && !reminderAccess.canScheduleExactAlarms()

    fun onExactAlarmExplained() {
        reminderAccess.exactAlarmExplained = true
    }

    /**
     * Удаляет задачу и передаёт в [onDeleted] всё, что нужно для отмены. Перед удалением
     * дописывает последнюю правку, чтобы «Отменить» вернул задачу такой, какой её видели.
     */
    fun delete(onDeleted: (DeletedTask) -> Unit) {
        if (isClosing) return
        isClosing = true
        viewModelScope.launch {
            draft?.let { save(it) }
            repository.deleteTask(taskId)?.let(onDeleted)
        }
    }

    /**
     * «Отложить»: переносит задачу на [date] и передаёт в [onPostponed] прежнюю дату для отмены.
     * Как и удаление, сначала дописывает последнюю правку: иначе запоздавшее сохранение вернуло бы старую дату.
     */
    fun postpone(date: LocalDate, onPostponed: (MovedTask) -> Unit) {
        if (isClosing) return
        isClosing = true
        viewModelScope.launch {
            draft?.let { save(it) }
            repository.moveTask(taskId, date)?.let(onPostponed)
        }
    }

    private fun edit(change: (TaskDraft) -> TaskDraft) {
        val current = draft ?: return
        // «Сегодня» и «Завтра» сразу превращаются в конкретный день: задача не должна переехать в полночь
        val changed = change(current).fixedTo(LocalDate.now())
        draft = changed
        pendingSave.value = changed
    }

    private suspend fun save(draft: TaskDraft) {
        val date = draft.dateFor(LocalDate.now())
        val title = draft.title.trim().ifEmpty { committedTitle }
        // Правила черновика и так не дают времени, повтора и напоминания без даты; здесь — для надёжности
        repository.updateTask(
            id = taskId,
            title = title,
            note = draft.note.trim(),
            date = date,
            time = if (date != null) draft.time else null,
            categoryId = draft.categoryId,
            priority = draft.priority,
            repeat = if (date != null) draft.repeat else RepeatRule.None,
            reminderMinutesBefore = if (date != null) draft.reminderMinutesBefore else null
        )
    }
}

/**
 * Строки подзадач для экрана: сохранённые в базе по порядку, у правящейся — текст из правки.
 * Новая строка, которой ещё нет в базе, идёт последней.
 */
internal fun subtaskRows(
    saved: List<SubtaskUi>,
    editing: SubtaskEditing?,
    newKeys: Map<Long, Long>
): List<SubtaskRowUi> {
    val rows = saved.map { subtask ->
        val key = newKeys[subtask.id] ?: subtask.id
        val isEditing = editing != null && editing.key == key
        SubtaskRowUi(
            key = key,
            title = if (isEditing) editing.text else subtask.title,
            done = subtask.done,
            isEditing = isEditing
        )
    }
    return if (editing != null && editing.isNew && rows.none { it.key == editing.key }) {
        rows + SubtaskRowUi(key = editing.key, title = editing.text, done = false, isEditing = true)
    } else {
        rows
    }
}

/** Поля задачи из базы → черновик карточки. Дата всегда конкретная, плашка горит только у «Без даты». */
internal fun TaskEntity.toDraft(): TaskDraft = TaskDraft(
    title = title,
    note = note,
    quickDate = if (date == null) QuickDate.NoDate else null,
    exactDate = date,
    time = time,
    categoryId = categoryId,
    priority = priority,
    repeat = repeat,
    reminderMinutesBefore = reminderMinutesBefore
)

/** Плашки «Сегодня», «Завтра», «На выходных» превращаются в конкретную дату для дня [today]. */
internal fun TaskDraft.fixedTo(today: LocalDate): TaskDraft {
    val date = dateFor(today)
    return copy(quickDate = if (date == null) QuickDate.NoDate else null, exactDate = date)
}

/**
 * В какой группе главного экрана задача сейчас. Задача на сегодня остаётся в «Сегодня»,
 * даже выполненная; другие выполненные — в «Выполненном».
 */
internal fun taskGroupOf(date: LocalDate?, isDone: Boolean, today: LocalDate): TaskGroup {
    if (date == today) return TaskGroup.Today
    if (isDone) return TaskGroup.Done
    if (date == null) return TaskGroup.NoDate
    val ranges = GroupRanges.of(today)
    return when {
        date < today -> TaskGroup.Overdue
        date == ranges.tomorrow -> TaskGroup.Tomorrow
        date in ranges.thisWeek -> TaskGroup.ThisWeek
        date in ranges.thisMonth -> TaskGroup.ThisMonth
        else -> TaskGroup.Later
    }
}
