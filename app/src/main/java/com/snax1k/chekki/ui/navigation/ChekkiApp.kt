package com.snax1k.chekki.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.snax1k.chekki.ChekkiApplication
import com.snax1k.chekki.ui.addtask.AddTaskSheet
import com.snax1k.chekki.ui.addtask.AddTaskViewModel
import com.snax1k.chekki.ui.group.GroupScreen
import com.snax1k.chekki.ui.group.GroupViewModel
import com.snax1k.chekki.ui.main.MainScreen
import com.snax1k.chekki.ui.main.MainViewModel
import com.snax1k.chekki.ui.main.TaskGroup
import kotlinx.serialization.Serializable

// Экраны приложения. @Serializable нужен, чтобы стек экранов сохранялся при повороте и после
// выгрузки приложения из памяти.

@Serializable
data object MainRoute : NavKey

/** Список задач группы. */
@Serializable
data class GroupRoute(val group: TaskGroup) : NavKey

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
                    onShowMore = viewModel::showMore,
                    onAddClick = { isAddTaskOpen = true }
                )
            }
        }
    )

    if (isAddTaskOpen) {
        AddTaskSheet(viewModel = addTaskViewModel, onDismiss = { isAddTaskOpen = false })
    }
}
