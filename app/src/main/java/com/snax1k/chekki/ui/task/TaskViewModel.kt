package com.snax1k.chekki.ui.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import com.snax1k.chekki.data.db.AttachmentEntity
import com.snax1k.chekki.data.AttachmentTooLargeException
import com.snax1k.chekki.data.AttachmentStore
import com.snax1k.chekki.R
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.GroupRanges
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.reminders.ReminderAccess
import com.snax1k.chekki.ui.addtask.CategoryOption
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.addtask.TaskDraft
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.tasks.Priority
import com.snax1k.chekki.ui.tasks.toPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Карточка задачи. Поля и подзадачи правятся на месте, но в базу попадают только по «Сохранить»
 * (замечание клиента 23.11.2026: испорченное название можно не сохранять); «Отменить» возвращает сохранённое.
 * «Выполнено», «Отложить», «Удалить» и вложения действуют сразу — это действия, а не правка.
 * Как одно поле влияет на другие, решает [TaskDraft] — так же, как в панели новой задачи.
 */
class TaskViewModel(
    private val taskId: Long,
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>,
    private val reminderAccess: ReminderAccess,
    private val attachmentStore: AttachmentStore? = null
) : ViewModel() {

    /** Вложения задачи по порядку. */
    val attachments: StateFlow<List<AttachmentEntity>> = repository.observeAttachments(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun attachmentFile(attachment: AttachmentEntity): File = attachmentStore!!.file(attachment.fileName)

    fun attachmentUri(attachment: AttachmentEntity): Uri = attachmentStore!!.uriFor(attachment.fileName)

    fun uriForFile(file: File): Uri = attachmentStore!!.uriFor(file.name)

    /** Пустой файл под снимок камеры или запись голоса. */
    fun newAttachmentFile(extension: String): File = attachmentStore!!.newFile(extension)

    /** Выбранное в галерее или «Файлах»: копируется к приложению. [onError] — строка с причиной. */
    fun addAttachments(uris: List<Uri>, onError: (Int) -> Unit) {
        val store = attachmentStore ?: return
        viewModelScope.launch {
            uris.forEach { uri ->
                try {
                    repository.addAttachment(taskId, store.copyFrom(uri))
                } catch (e: AttachmentTooLargeException) {
                    onError(R.string.attachments_too_large)
                } catch (e: Exception) {
                    onError(R.string.attachments_failed)
                }
            }
        }
    }

    /** Снимок камеры или запись голоса уже лежат в папке вложений: осталось записать строку. */
    fun addRecordedAttachment(file: File, mimeType: String, displayName: String) {
        val store = attachmentStore ?: return
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            return
        }
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { store.describe(file, displayName, mimeType) }
            repository.addAttachment(taskId, stored)
        }
    }

    fun deleteAttachment(id: Long, onDeleted: (AttachmentEntity) -> Unit) {
        viewModelScope.launch { repository.deleteAttachment(id)?.let(onDeleted) }
    }

    /**
     * Поля, которые правятся в карточке; null — задача ещё не загрузилась.
     * Состояние Compose, а не StateFlow: поле ввода обновляется сразу, и курсор не прыгает при наборе.
     */
    var draft by mutableStateOf<TaskDraft?>(null)
        private set

    /** Поля, как они лежат в базе: с ними сравнивается [draft], к ним возвращает «Отменить». */
    private var savedDraft by mutableStateOf<TaskDraft?>(null)

    /** Подзадачи в карточке, с несохранёнными правками. */
    var subtasks by mutableStateOf<List<CardSubtask>>(emptyList())
        private set

    private var savedSubtasks by mutableStateOf<List<CardSubtask>>(emptyList())

    /** Ключ подзадачи, которую сейчас правят; null — никакую. */
    var editingSubtaskKey by mutableStateOf<Long?>(null)
        private set

    /** Ключи новых строк отрицательные, чтобы не совпасть с id из базы. */
    private var nextNewSubtaskKey = -1L

    /** Задачи нет (удалили раньше, чем открылась карточка): карточку нужно закрыть. */
    var isMissing by mutableStateOf(false)
        private set

    /** Есть несохранённые правки: внизу «Отменить» и «Сохранить», «Назад» спрашивает, закрыть ли без сохранения. */
    val hasChanges: Boolean
        get() {
            val current = draft ?: return false
            val saved = savedDraft ?: return false
            return hasUnsavedChanges(current, saved, subtasks, savedSubtasks)
        }

    /** Идёт сохранение: вторая «Сохранить» подряд ничего не делает. */
    var isSaving by mutableStateOf(false)
        private set

    /** Выполнена ли задача; отметку меняют кнопка и кружок, а не правка полей. */
    val isDone: StateFlow<Boolean> = repository.observeTask(taskId)
        .map { it?.task?.isDone == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val categories: StateFlow<List<CategoryOption>> = repository.observeCategories()
        .map { list ->
            list.map { CategoryOption(id = it.id, name = it.name, colorIndex = it.colorIndex, icon = it.icon, isArchived = it.isArchived) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Уровни важности пользователя, самый важный первым. */
    val priorityLevels: StateFlow<List<Priority>> = repository.observePriorityLevels()
        .map { list -> list.map { it.toPriority() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val today: StateFlow<LocalDate> = currentDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    /** Задачу удаляют или откладывают, карточка закрывается. */
    private var isClosing = false

    init {
        viewModelScope.launch {
            if (!loadSaved()) isMissing = true
        }
    }

    /** Читает задачу и подзадачи из базы и ставит их и в сохранённое, и в правку. false — задачи нет. */
    private suspend fun loadSaved(): Boolean {
        val task = repository.getTaskWithDetails(taskId)?.task ?: return false
        val loaded = repository.getSubtasks(taskId).map { CardSubtask(key = it.id, id = it.id, title = it.title, done = it.isDone) }
        val loadedDraft = task.toDraft()
        savedDraft = loadedDraft
        draft = loadedDraft
        savedSubtasks = loaded
        subtasks = loaded
        editingSubtaskKey = null
        return true
    }

    fun onTitleChange(title: String) = edit { it.copy(title = title) }

    /** Из поля названия ушли: пустое название возвращается к сохранённому. */
    fun onTitleEditEnd() {
        val current = draft ?: return
        val saved = savedDraft ?: return
        if (current.title.isBlank()) draft = current.copy(title = saved.title)
    }

    fun onNoteChange(note: String) = edit { it.copy(note = note) }

    fun onQuickDateSelect(quickDate: QuickDate) = edit { it.withQuickDate(quickDate) }

    fun onDatePick(date: LocalDate) = edit { it.withDate(date, today = LocalDate.now()) }

    /** Время из циферблата; null — на весь день. */
    fun onTimePick(time: LocalTime?) = edit { it.withTime(time) }

    fun onCategorySelect(categoryId: Long?) = edit { it.copy(categoryId = categoryId) }

    fun onPrioritySelect(priority: Long) = edit { it.copy(priority = priority) }

    /** Повтор из окна «Повтор»; [nextDate] — «Следующая задача», станет датой задачи. */
    fun onRepeatSelect(repeat: RepeatRule, nextDate: LocalDate?) =
        edit { it.withRepeat(repeat, nextDate, today = LocalDate.now()) }

    /** Напоминание за сколько минут; null — без напоминания. */
    fun onReminderSelect(minutesBefore: Int?) = edit { it.withReminder(minutesBefore) }

    fun toggleDone() {
        viewModelScope.launch { repository.toggleDone(taskId) }
    }

    fun toggleSubtask(row: SubtaskRowUi) = updateSubtask(row.key) { it.copy(done = !it.done) }

    /** Добавляет пустую строку в конец и начинает её правку. Вторую пустую строку не добавляет. */
    fun addSubtask() {
        val editing = subtasks.firstOrNull { it.key == editingSubtaskKey }
        if (editing != null && editing.id == null && editing.title.isBlank()) return
        finishSubtaskEdit()
        val key = nextNewSubtaskKey--
        subtasks = subtasks + CardSubtask(key = key, id = null, title = "", done = false)
        editingSubtaskKey = key
    }

    fun editSubtask(row: SubtaskRowUi) {
        if (editingSubtaskKey == row.key) return
        finishSubtaskEdit()
        editingSubtaskKey = row.key
    }

    /** Текст правящейся подзадачи — только в карточке, в базу уйдёт по «Сохранить». */
    fun onSubtaskTextChange(text: String) {
        val key = editingSubtaskKey ?: return
        updateSubtask(key) { it.copy(title = text) }
    }

    /** Строку [key] больше не правят: курсор ушёл. Если уже правят другую строку, ничего не делает. */
    fun onSubtaskEditEnd(key: Long) {
        if (editingSubtaskKey == key) finishSubtaskEdit()
    }

    fun deleteSubtask(key: Long) {
        if (editingSubtaskKey == key) editingSubtaskKey = null
        subtasks = subtasks.filterNot { it.key == key }
    }

    /** Новая строка, которую оставили пустой, исчезает; у прежней подзадачи стёртое название возвращается. */
    private fun finishSubtaskEdit() {
        val key = editingSubtaskKey ?: return
        editingSubtaskKey = null
        val row = subtasks.firstOrNull { it.key == key } ?: return
        if (row.title.isNotBlank()) return
        subtasks = if (row.id == null) {
            subtasks.filterNot { it.key == key }
        } else {
            val savedTitle = savedSubtasks.firstOrNull { it.id == row.id }?.title.orEmpty()
            subtasks.map { if (it.key == key) it.copy(title = savedTitle) else it }
        }
    }

    private fun updateSubtask(key: Long, change: (CardSubtask) -> CardSubtask) {
        subtasks = subtasks.map { if (it.key == key) change(it) else it }
    }

    /** «Сохранить»: поля и подзадачи — в базу, будильник переставляется. Потом карточка показывает то, что в базе. */
    fun save() {
        val current = draft ?: return
        val saved = savedDraft ?: return
        if (isSaving || isClosing) return
        finishSubtaskEdit()
        isSaving = true
        val rows = subtasksForSave(subtasks, savedSubtasks)
        viewModelScope.launch {
            try {
                writeTask(current.forSave(saved))
                repository.saveSubtasks(taskId, rows)
                if (!loadSaved()) isMissing = true
            } finally {
                isSaving = false
            }
        }
    }

    /** «Отменить» и «Закрыть без сохранения»: всё как в базе. */
    fun discardChanges() {
        draft = savedDraft
        subtasks = savedSubtasks
        editingSubtaskKey = null
    }

    /** Нужно ли объяснить про точные будильники: напоминание есть, а точные будильники не разрешены. */
    fun needsExactAlarmExplanation(): Boolean =
        draft?.reminderMinutesBefore != null &&
            !reminderAccess.exactAlarmExplained && !reminderAccess.canScheduleExactAlarms()

    fun onExactAlarmExplained() {
        reminderAccess.exactAlarmExplained = true
    }

    /**
     * Удаляет задачу и передаёт в [onDeleted] всё, что нужно для отмены. Несохранённые правки пропадают:
     * «Отменить» вернёт задачу такой, какая она в базе.
     */
    fun delete(onDeleted: (DeletedTask) -> Unit) {
        if (isClosing) return
        isClosing = true
        viewModelScope.launch {
            repository.deleteTask(taskId)?.let(onDeleted)
        }
    }

    /**
     * «Отложить»: переносит задачу на [date] и передаёт в [onPostponed] прежнюю дату для отмены.
     * Кнопка видна, только когда несохранённых правок нет.
     */
    fun postpone(date: LocalDate, onPostponed: (MovedTask) -> Unit) {
        if (isClosing) return
        isClosing = true
        viewModelScope.launch {
            repository.moveTask(taskId, date)?.let(onPostponed)
        }
    }

    private fun edit(change: (TaskDraft) -> TaskDraft) {
        val current = draft ?: return
        // «Сегодня» и «Завтра» сразу превращаются в конкретный день: задача не должна переехать в полночь
        draft = change(current).fixedTo(LocalDate.now())
    }

    private suspend fun writeTask(draft: TaskDraft) {
        val date = draft.dateFor(LocalDate.now())
        // Правила черновика и так не дают времени, повтора и напоминания без даты; здесь — для надёжности
        repository.updateTask(
            id = taskId,
            title = draft.title,
            note = draft.note,
            date = date,
            time = if (date != null) draft.time else null,
            categoryId = draft.categoryId,
            priority = draft.priority,
            repeat = if (date != null) draft.repeat else RepeatRule.None,
            reminderMinutesBefore = if (date != null) draft.reminderMinutesBefore else null
        )
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
