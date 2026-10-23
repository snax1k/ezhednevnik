package com.snax1k.chekki.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.data.db.CategorySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Что показывает экран «Категории». */
data class CategoriesUiState(
    /** Категории не в архиве, по порядку. */
    val categories: List<CategorySummary>,
    /** Невыполненные задачи без категории. */
    val uncategorizedCount: Int,
    /** Категории в архиве, по порядку. */
    val archived: List<CategorySummary> = emptyList(),
    /** База ещё не ответила: счётчики пока не показываем. */
    val isLoading: Boolean = false
)

/** Экран «Категории»: плитки со счётчиками, порядок, архив. Счётчики «на сегодня» пересчитываются в полночь. */
class CategoriesViewModel(
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoriesUiState> = currentDate
        .flatMapLatest { today ->
            combine(
                repository.observeCategorySummaries(today),
                repository.observeUncategorizedOpenCount(),
                repository.observeCategorySummaries(today, archived = true)
            ) { categories, uncategorized, archived -> CategoriesUiState(categories, uncategorized, archived) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState(emptyList(), uncategorizedCount = 0, isLoading = true)
        )

    /** Порядок после перетаскивания: id категорий не в архиве сверху вниз. */
    fun reorder(categoryIds: List<Long>) {
        viewModelScope.launch { repository.reorderCategories(categoryIds) }
    }
}
