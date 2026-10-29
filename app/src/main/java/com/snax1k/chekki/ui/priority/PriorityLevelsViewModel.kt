package com.snax1k.chekki.ui.priority

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedPriorityLevel
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.PriorityLevels
import com.snax1k.chekki.ui.tasks.Priority
import com.snax1k.chekki.ui.tasks.toPriority
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PriorityLevelsUiState(
    /** Уровни, самый важный первым. */
    val levels: List<Priority>,
    val isLoading: Boolean = false
) {
    val canAdd: Boolean get() = levels.size < PriorityLevels.MAX
}

/**
 * Что правится в панели уровня. [id] null — новый уровень. [colorCode] — как у категорий: 0–11 — номер
 * в палитре, отрицательное — свой цвет ARGB (`categoryColor`); в базу уходит уже сам цвет.
 */
data class PriorityDraft(val id: Long?, val name: String, val colorCode: Int) {
    val canSave: Boolean get() = name.isNotBlank()
}

/** Экран «Уровни важности» и панель уровня (решено 29.10.2026). */
class PriorityLevelsViewModel(private val repository: TaskRepository) : ViewModel() {

    val uiState: StateFlow<PriorityLevelsUiState> = repository.observePriorityLevels()
        .map { list -> PriorityLevelsUiState(list.map { it.toPriority() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PriorityLevelsUiState(emptyList(), isLoading = true))

    /** Открытая панель уровня; null — закрыта. */
    var draft by mutableStateOf<PriorityDraft?>(null)
        private set

    private var isSaving = false

    fun startNew(colorCode: Int) {
        draft = PriorityDraft(id = null, name = "", colorCode = colorCode)
    }

    fun startEdit(level: Priority, colorCode: Int) {
        draft = PriorityDraft(id = level.id, name = level.name, colorCode = colorCode)
    }

    fun close() {
        draft = null
    }

    fun onNameChange(name: String) {
        draft = draft?.copy(name = name)
    }

    fun onColorSelect(code: Int) {
        draft = draft?.copy(colorCode = code)
    }

    /** [color] — выбранный цвет ARGB (его считает экран: палитра — в теме). */
    fun save(color: Int, onSaved: () -> Unit) {
        val current = draft ?: return
        if (!current.canSave || isSaving) return
        isSaving = true
        viewModelScope.launch {
            val name = current.name.trim()
            if (current.id == null) repository.addPriorityLevel(name, color) else repository.updatePriorityLevel(current.id, name, color)
            isSaving = false
            onSaved()
        }
    }

    fun reorder(ids: List<Long>) {
        viewModelScope.launch { repository.reorderPriorityLevels(ids) }
    }

    /** Удаляет уровень из панели; задачи с ним становятся без приоритета. */
    fun delete(onDeleted: (DeletedPriorityLevel) -> Unit) {
        val id = draft?.id ?: return
        viewModelScope.launch { repository.deletePriorityLevel(id)?.let(onDeleted) }
    }
}
