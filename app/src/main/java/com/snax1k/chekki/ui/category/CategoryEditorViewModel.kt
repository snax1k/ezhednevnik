package com.snax1k.chekki.ui.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.DeletedCategory
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.CategoryIcon
import kotlinx.coroutines.launch

/**
 * Панель категории: новая или изменение существующей. Живёт, пока открыто приложение;
 * [startNew] и [startEdit] каждый раз начинают заново, поворот экрана черновик не теряет.
 */
class CategoryEditorViewModel(private val repository: TaskRepository) : ViewModel() {

    // Состояние Compose, а не StateFlow: поле ввода обновляется сразу, и курсор не прыгает при наборе
    var draft by mutableStateOf(CategoryDraft())
        private set

    /** Какую категорию меняем; null — новая. */
    var editingId by mutableStateOf<Long?>(null)
        private set

    /** Меняемая категория в архиве: в панели «Вернуть из архива» вместо «В архив». */
    var isArchived by mutableStateOf(false)
        private set

    /** Категория для изменения ещё грузится: поля пока не показываем. */
    var isLoading by mutableStateOf(false)
        private set

    private var isBusy = false
    private var isStarted = false

    /** Панель снова открыта после выгрузки приложения из памяти, а ViewModel новая: начинаем ту же правку заново. */
    fun restoreIfNeeded(categoryId: Long?) {
        if (isStarted) return
        if (categoryId == null) startNew() else startEdit(categoryId)
    }

    /** Новая категория: цвет — первый ещё не занятый. */
    fun startNew() {
        isStarted = true
        editingId = null
        isArchived = false
        draft = CategoryDraft()
        isLoading = true
        viewModelScope.launch {
            draft = draft.copy(colorIndex = CategoryDraft.suggestedColor(repository.getCategoryColorIndexes()))
            isLoading = false
        }
    }

    fun startEdit(categoryId: Long) {
        isStarted = true
        editingId = categoryId
        isLoading = true
        viewModelScope.launch {
            repository.getCategory(categoryId)?.let {
                draft = CategoryDraft.of(it)
                isArchived = it.isArchived
            }
            isLoading = false
        }
    }

    fun onNameChange(name: String) {
        draft = draft.copy(name = name)
    }

    fun onIconSelect(icon: CategoryIcon) {
        draft = draft.copy(icon = icon)
    }

    fun onColorSelect(colorIndex: Int) {
        draft = draft.copy(colorIndex = colorIndex)
    }

    /** Сохраняет категорию и вызывает [onSaved]. Повторное нажатие во время сохранения игнорируется. */
    fun save(onSaved: () -> Unit) {
        val current = draft
        if (!current.canSave || isBusy) return
        isBusy = true
        viewModelScope.launch {
            try {
                val id = editingId
                val name = current.name.trim()
                if (id == null) {
                    repository.addCategory(name, current.icon, current.colorIndex)
                } else {
                    repository.updateCategory(id, name, current.icon, current.colorIndex)
                }
                onSaved()
            } finally {
                isBusy = false
            }
        }
    }

    /** «В архив» или «Вернуть из архива»; правки названия, иконки и цвета тоже сохраняются. */
    fun toggleArchived(onDone: () -> Unit) {
        val id = editingId ?: return
        val current = draft
        if (!current.canSave || isBusy) return
        isBusy = true
        viewModelScope.launch {
            try {
                repository.updateCategory(id, current.name.trim(), current.icon, current.colorIndex)
                repository.setCategoryArchived(id, !isArchived)
                onDone()
            } finally {
                isBusy = false
            }
        }
    }

    /** Удаляет категорию (задачи остаются без категории) и передаёт в [onDeleted] всё для «Отменить». */
    fun delete(onDeleted: (DeletedCategory) -> Unit) {
        val id = editingId ?: return
        if (isBusy) return
        isBusy = true
        viewModelScope.launch {
            try {
                repository.deleteCategory(id)?.let(onDeleted)
            } finally {
                isBusy = false
            }
        }
    }
}
