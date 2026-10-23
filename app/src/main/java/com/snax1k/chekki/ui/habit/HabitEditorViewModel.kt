package com.snax1k.chekki.ui.habit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedHabit
import com.snax1k.chekki.data.HabitRepository
import com.snax1k.chekki.data.db.HabitIcon
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Панель привычки: новая или изменение существующей. Живёт, пока открыто приложение;
 * [startNew] и [startEdit] каждый раз начинают заново, поворот экрана черновик не теряет.
 */
class HabitEditorViewModel(private val repository: HabitRepository) : ViewModel() {

    // Состояние Compose, а не StateFlow: поле ввода обновляется сразу, и курсор не прыгает при наборе
    var draft by mutableStateOf(HabitDraft())
        private set

    /** Какую привычку меняем; null — новая. */
    var editingId by mutableStateOf<Long?>(null)
        private set

    /** Привычка для изменения ещё грузится: поля пока не показываем. */
    var isLoading by mutableStateOf(false)
        private set

    private var isBusy = false

    fun startNew() {
        editingId = null
        isLoading = false
        draft = HabitDraft()
    }

    fun startEdit(habitId: Long) {
        editingId = habitId
        isLoading = true
        viewModelScope.launch {
            repository.getHabit(habitId)?.let { draft = HabitDraft.of(it) }
            isLoading = false
        }
    }

    fun onTitleChange(title: String) {
        draft = draft.copy(title = title)
    }

    fun onIconSelect(icon: HabitIcon) {
        draft = draft.copy(icon = icon)
    }

    fun onColorSelect(colorIndex: Int) {
        draft = draft.copy(colorIndex = colorIndex)
    }

    fun onTargetChange(target: Int) {
        draft = draft.withTarget(target)
    }

    fun onDayToggle(day: DayOfWeek) {
        draft = draft.withDayToggled(day)
    }

    /** Сохраняет привычку и вызывает [onSaved]. Повторное нажатие во время сохранения игнорируется. */
    fun save(onSaved: () -> Unit) {
        val current = draft
        if (!current.canSave || isBusy) return
        isBusy = true
        viewModelScope.launch {
            try {
                val id = editingId
                if (id == null) {
                    repository.addHabit(
                        title = current.title.trim(),
                        icon = current.icon,
                        colorIndex = current.colorIndex,
                        dailyTarget = current.dailyTarget,
                        weekdays = current.weekdays
                    )
                } else {
                    repository.updateHabit(
                        id = id,
                        title = current.title.trim(),
                        icon = current.icon,
                        colorIndex = current.colorIndex,
                        dailyTarget = current.dailyTarget,
                        weekdays = current.weekdays,
                        today = LocalDate.now()
                    )
                }
                onSaved()
            } finally {
                isBusy = false
            }
        }
    }

    /** Удаляет привычку и передаёт в [onDeleted] всё, что нужно для «Отменить». */
    fun delete(onDeleted: (DeletedHabit) -> Unit) {
        val id = editingId ?: return
        if (isBusy) return
        isBusy = true
        viewModelScope.launch {
            try {
                repository.deleteHabit(id)?.let(onDeleted)
            } finally {
                isBusy = false
            }
        }
    }
}
