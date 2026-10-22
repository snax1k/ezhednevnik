package com.snax1k.chekki.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.data.TaskRepository
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.tasks.toTaskUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Что показывает экран «Сегодня». */
data class TodayUiState(
    val date: LocalDate,
    val tasks: List<TaskUi>,
    /** База ещё не ответила: счётчик и пустой список пока не показываем. */
    val isLoading: Boolean = false
)

/** Задачи на сегодня из базы. При смене даты в полночь список переключается на новый день. */
class TodayViewModel(
    private val repository: TaskRepository,
    currentDate: Flow<LocalDate>
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = currentDate
        .flatMapLatest { date ->
            repository.observeTasksOn(date).map { tasks -> TodayUiState(date, tasks.map { it.toTaskUi() }) }
        }
        .stateIn(
            scope = viewModelScope,
            // Пока экран свёрнут, базу не слушаем; 5 секунд запаса, чтобы не перезапускаться при повороте
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(date = LocalDate.now(), tasks = emptyList(), isLoading = true)
        )

    fun toggleDone(taskId: Long) {
        viewModelScope.launch { repository.toggleDone(taskId) }
    }
}
