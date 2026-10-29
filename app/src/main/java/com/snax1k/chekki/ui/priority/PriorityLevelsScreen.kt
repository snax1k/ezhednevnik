package com.snax1k.chekki.ui.priority

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DeletedPriorityLevel
import com.snax1k.chekki.ui.addtask.DragHandle
import com.snax1k.chekki.ui.addtask.PlainTextField
import com.snax1k.chekki.ui.addtask.SaveButton
import com.snax1k.chekki.ui.addtask.sheetContentMaxHeight
import com.snax1k.chekki.ui.category.ColorPicker
import com.snax1k.chekki.ui.category.categoryColor
import com.snax1k.chekki.ui.category.rememberCategoryOrderState
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.habit.SectionLabel
import com.snax1k.chekki.ui.habit.SheetHeader
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.tasks.Priority
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlinx.coroutines.launch

/**
 * «Уровни важности» (меню ☰, решено 29.10.2026): уровни сверху вниз — от самого важного, по этому
 * порядку сортируются задачи. Строку тащат за «≡» (как порядок категорий), нажатие открывает панель уровня,
 * внизу «Новый уровень» (не больше восьми). Макета нет, сделан в стиле «Всех привычек».
 */
@Composable
fun PriorityLevelsScreen(
    state: PriorityLevelsUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onLevelClick: (Priority) -> Unit = {},
    onAdd: () -> Unit = {},
    onReorder: (List<Long>) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val order = rememberCategoryOrderState(listState)
    val currentOnReorder by rememberUpdatedState(onReorder)
    // Пока строку не тащат, порядок — из базы
    LaunchedEffect(state.levels) {
        if (order.draggedId == null) order.order = state.levels.map { it.id }
    }
    val byId = state.levels.associateBy { it.id }
    val shown = order.order.mapNotNull { byId[it] } + state.levels.filter { it.id !in order.order }
    val ids = shown.map { it.id }
    val currentIds by rememberUpdatedState(ids)
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp + navigationBarHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "title") {
                    Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                        Text(
                            text = stringResource(R.string.priority_levels_title),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.priority_levels_hint),
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(shown, key = { it.id }) { level ->
                    val index = ids.indexOf(level.id)
                    val isDragged = level.id == order.draggedId
                    fun move(by: Int) {
                        val to = index + by
                        if (to !in ids.indices) return
                        order.order = ids.toMutableList().apply { add(to, removeAt(index)) }
                        onReorder(order.order)
                    }
                    LevelRow(
                        level = level,
                        isDragged = isDragged,
                        onClick = { onLevelClick(level) },
                        onMoveUp = if (index > 0) ({ move(-1) }) else null,
                        onMoveDown = if (index < ids.lastIndex) ({ move(1) }) else null,
                        dragHandle = Modifier.pointerInput(level.id) {
                            detectDragGestures(
                                onDragStart = { order.start(level.id, currentIds) },
                                onDragEnd = { order.finish(currentOnReorder) },
                                onDragCancel = { order.finish(currentOnReorder) },
                                onDrag = { change, amount ->
                                    change.consume()
                                    order.drag(amount.y)
                                }
                            )
                        },
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) order.dragOffset else 0f }
                            .then(if (isDragged) Modifier else Modifier.animateItem())
                    )
                }
                item(key = "add") {
                    AddLevelButton(enabled = state.canAdd, onClick = onAdd, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Строка уровня: флажок в его цвете, название, ручка «≡». Для TalkBack — «Выше» и «Ниже». */
@Composable
private fun LevelRow(
    level: Priority,
    isDragged: Boolean,
    onClick: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    dragHandle: Modifier,
    modifier: Modifier = Modifier
) {
    val color = Color(level.color)
    val scale by animateFloatAsState(if (isDragged) 1.03f else 1f, label = "priorityRowScale")
    val moveUpLabel = stringResource(R.string.habit_move_up)
    val moveDownLabel = stringResource(R.string.habit_move_down)
    val dragLabel = stringResource(R.string.habit_drag, level.name)
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isDragged) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                customActions = listOfNotNull(
                    onMoveUp?.let { action -> CustomAccessibilityAction(moveUpLabel) { action(); true } },
                    onMoveDown?.let { action -> CustomAccessibilityAction(moveDownLabel) { action(); true } }
                )
            }
            .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(ChekkiIcons.PriorityFlag, contentDescription = null, Modifier.size(20.dp), tint = color)
        }
        Text(
            text = level.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier
                .size(48.dp)
                .then(dragHandle)
                .semantics { contentDescription = dragLabel },
            contentAlignment = Alignment.Center
        ) {
            Icon(ChekkiIcons.DragHandle, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** «+ Новый уровень»; когда уровней восемь — бледная, с подписью. */
@Composable
private fun AddLevelButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(ChekkiIcons.Add, contentDescription = null, Modifier.size(20.dp), tint = tint)
            Text(stringResource(R.string.priority_level_new), style = MaterialTheme.typography.titleSmall, color = tint)
        }
        if (!enabled) {
            Text(
                text = stringResource(R.string.priority_level_max),
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Панель уровня: название, цвет (палитра и свой цвет, как у категорий), «Удалить уровень» и «Сохранить». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityLevelSheet(viewModel: PriorityLevelsViewModel, onDeleted: (DeletedPriorityLevel) -> Unit) {
    val draft = viewModel.draft ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideAndClose: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { viewModel.close() }
    }
    val color = categoryColor(draft.colorCode)
    val nameFocus = remember { FocusRequester() }
    if (draft.id == null) {
        LaunchedEffect(Unit) { nameFocus.requestFocus() }
    }
    ModalBottomSheet(
        onDismissRequest = viewModel::close,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { DragHandle() }
    ) {
        Column(Modifier.heightIn(max = sheetContentMaxHeight())) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                SheetHeader(
                    stringResource(if (draft.id == null) R.string.priority_level_new else R.string.priority_level_edit),
                    hideAndClose
                )
                Row(
                    Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ChekkiIcons.PriorityFlag, contentDescription = null, Modifier.size(26.dp), tint = color)
                    }
                    PlainTextField(
                        value = draft.name,
                        onValueChange = viewModel::onNameChange,
                        label = stringResource(R.string.priority_level_name_label),
                        hint = stringResource(R.string.priority_level_name_hint),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = null,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        imeAction = ImeAction.Done,
                        maxLines = 1,
                        modifier = Modifier.focusRequester(nameFocus)
                    )
                }
                SectionLabel(stringResource(R.string.habit_color))
                ColorPicker(selected = draft.colorCode, onSelect = viewModel::onColorSelect)
                if (draft.id != null) {
                    TextButton(
                        onClick = {
                            viewModel.delete { deleted ->
                                hideAndClose()
                                onDeleted(deleted)
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.priority_level_delete),
                            style = MaterialTheme.typography.titleSmall,
                            color = ChekkiTheme.colors.priorityHigh
                        )
                    }
                    Text(
                        text = stringResource(R.string.priority_level_delete_hint),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(Modifier.height(12.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SaveButton(
                enabled = draft.canSave,
                onClick = { viewModel.save(color.toArgb(), onSaved = hideAndClose) },
                text = stringResource(if (draft.id == null) R.string.priority_level_add else R.string.habit_save),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            )
        }
    }
}

/**
 * Код цвета для палитры категорий: номер, если цвет уровня есть в палитре, иначе сам цвет (свой).
 * Нужен, чтобы в панели уровня отметился нужный кружок.
 */
@Composable
fun priorityColorCode(color: Int): Int {
    val index = ChekkiTheme.colors.categories.indexOfFirst { it.toArgb() == color }
    return if (index >= 0) index else color
}

/** Первый цвет палитры, которого ещё нет у уровней, — для нового уровня. */
@Composable
fun suggestedPriorityColorCode(levels: List<Priority>): Int {
    val palette = ChekkiTheme.colors.categories
    val used = levels.map { it.color }.toSet()
    return palette.indices.firstOrNull { palette[it].toArgb() !in used } ?: 0
}

@Preview(widthDp = 412, heightDp = 760)
@Composable
private fun PriorityLevelsScreenPreview() {
    ChekkiTheme {
        PriorityLevelsScreen(PriorityLevelsUiState(listOf(Priority.High, Priority.Medium, Priority.Low)))
    }
}
