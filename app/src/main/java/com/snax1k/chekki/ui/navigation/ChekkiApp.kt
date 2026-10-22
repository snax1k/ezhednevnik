package com.snax1k.chekki.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.snax1k.chekki.ChekkiApplication
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.ui.addtask.AddTaskSheet
import com.snax1k.chekki.ui.addtask.AddTaskViewModel
import com.snax1k.chekki.ui.components.LocalFloatingButtonsLift
import com.snax1k.chekki.ui.group.GroupScreen
import com.snax1k.chekki.ui.group.GroupViewModel
import com.snax1k.chekki.ui.main.MainScreen
import com.snax1k.chekki.ui.main.MainViewModel
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.task.TaskScreen
import com.snax1k.chekki.ui.task.TaskViewModel
import com.snax1k.chekki.ui.tasks.postponedMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate

// Экраны приложения. @Serializable нужен, чтобы стек экранов сохранялся при повороте и после
// выгрузки приложения из памяти.

@Serializable
data object MainRoute : NavKey

/** Список задач группы. */
@Serializable
data class GroupRoute(val group: TaskGroup) : NavKey

/** Карточка задачи. */
@Serializable
data class TaskRoute(val taskId: Long) : NavKey

/** Корень приложения: стек экранов (Navigation 3) и переходы между ними. */
@Composable
fun ChekkiApp() {
    val container = (LocalContext.current.applicationContext as ChekkiApplication).container
    val backStack = rememberNavBackStack(MainRoute)
    // Панель новой задачи открывается поверх любого экрана. Её ViewModel живёт, пока открыто
    // приложение, поэтому черновик сохраняется между открытиями.
    var isAddTaskOpen by rememberSaveable { mutableStateOf(false) }
    val addTaskViewModel = viewModel {
        AddTaskViewModel(container.taskRepository, container.currentDate, container.reminderAccess)
    }

    // Плашка «Задача удалена · Отменить» и «Задача отложена на … · Отменить» внизу, поверх любого экрана
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val showUndo: (message: String, undo: suspend () -> Unit) -> Unit = { message, undo ->
        scope.launch {
            // Новая плашка заменяет прежнюю: прошлое действие уже не отменить
            snackbarHostState.currentSnackbarData?.dismiss()
            // Short — 4 секунды, как в макете; с TalkBack Material держит плашку дольше
            val result = snackbarHostState.showSnackbar(message, undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) {
                // Отменяем в фоне приложения: экран могут закрыть раньше, чем база ответит
                container.applicationScope.launch { undo() }
            }
        }
    }
    val showUndoDelete: (DeletedTask) -> Unit = { deleted ->
        showUndo(deletedMessage) { container.taskRepository.restoreTask(deleted) }
    }
    val showUndoPostpone: (MovedTask) -> Unit = { moved ->
        val newDate = moved.newDate ?: LocalDate.now()
        showUndo(postponedMessage(context, newDate, LocalDate.now())) {
            container.taskRepository.moveTask(moved.taskId, moved.previousDate)
        }
    }
    val density = LocalDensity.current
    var snackbarHeight by remember { mutableStateOf(0.dp) }

    Box(Modifier.fillMaxSize()) {
        // Пока видна плашка, плавающие кнопки поднимаются над ней
        CompositionLocalProvider(
            LocalFloatingButtonsLift provides if (snackbarHeight > 0.dp) snackbarHeight + 8.dp else 0.dp
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                // Сохранение состояния экранов и своя ViewModel у каждого экрана в стеке:
                // когда экран закрывают, его ViewModel удаляется
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                // Новый экран въезжает справа, назад — уезжает вправо
                transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 } },
                popTransitionSpec = { slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it } },
                predictivePopTransitionSpec = { slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it } },
                entryProvider = entryProvider {
                    entry<MainRoute> {
                        val viewModel = viewModel { MainViewModel(container.taskRepository, container.currentDate) }
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        MainScreen(
                            state = state,
                            onGroupClick = { group -> backStack.add(GroupRoute(group)) },
                            onAddClick = { isAddTaskOpen = true }
                        )
                    }
                    entry<GroupRoute> { route ->
                        val viewModel = viewModel {
                            GroupViewModel(route.group, container.taskRepository, container.currentDate)
                        }
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        GroupScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onToggleDone = { task -> viewModel.toggleDone(task.id) },
                            onTaskClick = { task -> backStack.add(TaskRoute(task.id)) },
                            onShowMore = viewModel::showMore,
                            onAddClick = { isAddTaskOpen = true }
                        )
                    }
                    entry<TaskRoute> { route ->
                        val viewModel = viewModel {
                            TaskViewModel(
                                route.taskId,
                                container.taskRepository,
                                container.currentDate,
                                container.reminderAccess
                            )
                        }
                        TaskScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onDeleted = { deleted ->
                                backStack.removeLastOrNull()
                                showUndoDelete(deleted)
                            },
                            // Отложенная задача уходит из группы, поэтому карточка закрывается, как при удалении
                            onPostponed = { moved ->
                                backStack.removeLastOrNull()
                                showUndoPostpone(moved)
                            }
                        )
                    }
                }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .onSizeChanged { snackbarHeight = with(density) { it.height.toDp() } }
        )
    }

    if (isAddTaskOpen) {
        AddTaskSheet(viewModel = addTaskViewModel, onDismiss = { isAddTaskOpen = false })
    }
}
