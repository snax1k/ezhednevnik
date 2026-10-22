package com.snax1k.chekki.ui.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.GroupRanges
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.ReminderAccess
import com.snax1k.chekki.ui.addtask.CategoryOption
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.addtask.TaskDraft
import com.snax1k.chekki.ui.main.TaskGroup
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

/** Подзадача в карточке. */
data class SubtaskUi(
    val id: Long,
    val title: String,
    val done: Boolean
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

    /** Название, которое сейчас в базе: пустое название не сохраняется, остаётся прежнее. */
    private var savedTitle = ""

    /** Последняя несохранённая правка. Сохраняются по одной и по порядку; промежуточные при быстром наборе пропускаются. */
    private val pendingSave = MutableStateFlow<TaskDraft?>(null)

    private var isDeleting = false

    init {
        viewModelScope.launch {
            val task = repository.getTaskWithDetails(taskId)?.task
            if (task == null) {
                isMissing = true
            } else {
                savedTitle = task.title
                draft = task.toDraft()
            }
        }
        viewModelScope.launch {
            pendingSave.filterNotNull().collect { if (!isDeleting) save(it) }
        }
    }

    fun onTitleChange(title: String) = edit { it.copy(title = title) }

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

    fun toggleSubtask(subtask: SubtaskUi) {
        viewModelScope.launch { repository.setSubtaskDone(subtask.id, isDone = !subtask.done) }
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
        if (isDeleting) return
        isDeleting = true
        viewModelScope.launch {
            draft?.let { save(it) }
            repository.deleteTask(taskId)?.let(onDeleted)
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
        val title = draft.title.trim().ifEmpty { savedTitle }
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
        savedTitle = title
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
