package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.data.db.CategoryIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.ReminderAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Категория в списке выбора. */
data class CategoryOption(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val icon: CategoryIcon = CategoryIcon.Tag,
    /** В архиве: в меню выбора не показывается, но у задачи, которая уже в ней, остаётся. */
    val isArchived: Boolean = false
)

/**
 * Панель новой задачи. Живёт, пока открыто приложение, поэтому черновик сохраняется
 * между открытиями панели и очищается только после сохранения задачи.
 */
class AddTaskViewModel(
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>,
    private val reminderAccess: ReminderAccess
) : ViewModel() {

    // Состояние Compose, а не StateFlow: поле ввода обновляется сразу, и курсор не прыгает при наборе
    var draft by mutableStateOf(TaskDraft())
        private set

    val categories: StateFlow<List<CategoryOption>> = repository.observeCategories()
        .map { list ->
            list.map { CategoryOption(id = it.id, name = it.name, colorIndex = it.colorIndex, icon = it.icon, isArchived = it.isArchived) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Сегодняшняя дата: по ней «Завтра» и «На выходных» превращаются в конкретный день. */
    val today: StateFlow<LocalDate> = currentDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    private var isSaving = false
    private var nextSubtaskId = 0L

    fun onTitleChange(title: String) {
        draft = draft.copy(title = title)
    }

    fun onNoteChange(note: String) {
        draft = draft.copy(note = note)
    }

    fun onQuickDateSelect(quickDate: QuickDate) {
        draft = draft.withQuickDate(quickDate)
    }

    fun onDatePick(date: LocalDate) {
        draft = draft.withDate(date, today = LocalDate.now())
    }

    /** Время из циферблата; null — на весь день. */
    fun onTimePick(time: LocalTime?) {
        draft = draft.withTime(time)
    }

    fun onCategorySelect(categoryId: Long?) {
        draft = draft.copy(categoryId = categoryId)
    }

    fun onPrioritySelect(priority: TaskPriority) {
        draft = draft.copy(priority = priority)
    }

    fun onRepeatSelect(repeat: RepeatRule) {
        draft = draft.withRepeat(repeat)
    }

    /** Напоминание за сколько минут; null — без напоминания. */
    fun onReminderSelect(minutesBefore: Int?) {
        draft = draft.withReminder(minutesBefore)
    }

    /** Добавляет пустую подзадачу в конец и возвращает её id, чтобы панель поставила в неё курсор. */
    fun addSubtask(): Long {
        val id = nextSubtaskId++
        draft = draft.copy(subtasks = draft.subtasks + SubtaskDraft(id))
        return id
    }

    fun onSubtaskChange(id: Long, title: String) {
        draft = draft.copy(subtasks = draft.subtasks.map { if (it.id == id) it.copy(title = title) else it })
    }

    fun removeSubtask(id: Long) {
        draft = draft.copy(subtasks = draft.subtasks.filterNot { it.id == id })
    }

    /**
     * Нужно ли перед сохранением объяснить про точные будильники: у задачи есть напоминание,
     * точные будильники не разрешены, а объяснения ещё не было.
     */
    fun needsExactAlarmExplanation(): Boolean =
        draft.hasDate && draft.reminderMinutesBefore != null &&
            !reminderAccess.exactAlarmExplained && !reminderAccess.canScheduleExactAlarms()

    fun onExactAlarmExplained() {
        reminderAccess.exactAlarmExplained = true
    }

    /** Сохраняет задачу, очищает черновик и вызывает [onSaved]. Повторное нажатие во время сохранения игнорируется. */
    fun save(onSaved: () -> Unit) {
        val current = draft
        if (!current.canSave || isSaving) return
        isSaving = true
        viewModelScope.launch {
            try {
                val date = current.dateFor(LocalDate.now())
                // Правила черновика и так не дают времени, повтора и напоминания без даты; здесь — для надёжности
                repository.addTask(
                    title = current.title.trim(),
                    note = current.note.trim(),
                    date = date,
                    time = if (date != null) current.time else null,
                    categoryId = current.categoryId,
                    priority = current.priority,
                    repeat = if (date != null) current.repeat else RepeatRule.None,
                    reminderMinutesBefore = if (date != null) current.reminderMinutesBefore else null,
                    subtasks = current.filledSubtasks
                )
                draft = TaskDraft()
                onSaved()
            } finally {
                isSaving = false
            }
        }
    }
}
