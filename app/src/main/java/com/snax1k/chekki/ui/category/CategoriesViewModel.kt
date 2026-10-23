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
import java.time.LocalDate

/** Что показывает экран «Категории». */
data class CategoriesUiState(
    /** Категории не в архиве, по порядку. */
    val categories: List<CategorySummary>,
    /** Невыполненные задачи без категории. */
    val uncategorizedCount: Int,
    /** База ещё не ответила: счётчики пока не показываем. */
    val isLoading: Boolean = false
)

/** Экран «Категории»: плитки со счётчиками. Счётчики «на сегодня» пересчитываются в полночь. */
class CategoriesViewModel(
    repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CategoriesUiState> = currentDate
        .flatMapLatest { today ->
            combine(
                repository.observeCategorySummaries(today),
                repository.observeUncategorizedOpenCount()
            ) { categories, uncategorized -> CategoriesUiState(categories, uncategorized) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState(emptyList(), uncategorizedCount = 0, isLoading = true)
        )
}
