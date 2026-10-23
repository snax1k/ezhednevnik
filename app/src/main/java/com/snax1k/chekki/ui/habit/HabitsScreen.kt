package com.snax1k.chekki.ui.habit

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.HabitListItem
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import java.time.Instant

/**
 * Все привычки: действующие в порядке кругов наверху (перетаскиваются за ручку «≡»), «Новая привычка»
 * и свёрнутый архив. Макета нет, сделан в стиле экранов групп. Нажатие на строку открывает карточку привычки.
 */
@Composable
fun HabitsScreen(
    state: HabitsUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onHabitClick: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onArchive: (habitId: Long, archived: Boolean) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onReorder: (List<Long>) -> Unit = {},
    onAdd: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)
    // Порядок на экране: пока строку тащат — свой, после — снова из базы
    var order by remember { mutableStateOf(state.active) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.active) {
        if (draggedId == null) order = state.active
    }
    var archiveExpanded by rememberSaveable { mutableStateOf(false) }

    /** Середина перетаскиваемой строки зашла на соседнюю — меняем их местами. */
    fun moveIfNeeded() {
        val id = draggedId ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == id } ?: return
        val middle = (current.offset + dragOffset + current.size / 2f).toInt()
        val target = visible.firstOrNull { info ->
            info.key != id && order.any { it.habit.id == info.key } &&
                middle in info.offset..(info.offset + info.size)
        } ?: return
        val from = order.indexOfFirst { it.habit.id == id }
        val to = order.indexOfFirst { it.habit.id == target.key }
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        // Строка встанет на место соседней, а под пальцем должна остаться там же
        dragOffset += current.offset - target.offset
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    fun move(id: Long, by: Int) {
        val from = order.indexOfFirst { it.habit.id == id }
        val to = from + by
        if (from < 0 || to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        currentOnReorder(order.map { it.habit.id })
    }

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
                    Text(
                        text = stringResource(R.string.habits_title),
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 8.dp)
                            .semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!state.isLoading && order.isEmpty() && state.archived.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.habits_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(order, key = { it.habit.id }) { item ->
                    val id = item.habit.id
                    val isDragged = id == draggedId
                    val index = order.indexOf(item)
                    HabitRow(
                        item = item,
                        isDragged = isDragged,
                        onClick = { onHabitClick(id) },
                        onEdit = { onEdit(id) },
                        onArchiveToggle = { onArchive(id, true) },
                        onDelete = { onDelete(id) },
                        onMoveUp = if (index > 0) ({ move(id, -1) }) else null,
                        onMoveDown = if (index < order.lastIndex) ({ move(id, 1) }) else null,
                        dragHandle = Modifier.pointerInput(id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedId = id
                                    dragOffset = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffset = 0f
                                    currentOnReorder(order.map { it.habit.id })
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffset = 0f
                                    currentOnReorder(order.map { it.habit.id })
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    moveIfNeeded()
                                }
                            )
                        },
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                            // Остальные строки плавно уступают место; перетаскиваемая идёт за пальцем
                            .then(if (isDragged) Modifier else Modifier.animateItem())
                    )
                }
                item(key = "add") { AddHabitRow(onAdd) }
                if (state.archived.isNotEmpty()) {
                    item(key = "archive") {
                        ArchiveHeader(
                            count = state.archived.size,
                            expanded = archiveExpanded,
                            onToggle = { archiveExpanded = !archiveExpanded }
                        )
                    }
                    if (archiveExpanded) {
                        items(state.archived, key = { it.habit.id }) { item ->
                            val id = item.habit.id
                            HabitRow(
                                item = item,
                                isDragged = false,
                                onClick = { onHabitClick(id) },
                                onEdit = { onEdit(id) },
                                onArchiveToggle = { onArchive(id, false) },
                                onDelete = { onDelete(id) },
                                onMoveUp = null,
                                onMoveDown = null,
                                dragHandle = null,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Строка привычки: круг с иконкой, название, «Каждый день · 6 раз в день · 4 напоминания»,
 * ручка «≡» (у действующих) и меню «⋮». Архивная — бледнее.
 */
@Composable
private fun HabitRow(
    item: HabitListItem,
    isDragged: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    dragHandle: Modifier?,
    modifier: Modifier = Modifier
) {
    val habit = item.habit
    val palette = ChekkiTheme.colors.habits
    val colors = palette[habit.colorIndex.mod(palette.size)]
    val scale by animateFloatAsState(if (isDragged) 1.03f else 1f, label = "habitRowScale")
    val moveUpLabel = stringResource(R.string.habit_move_up)
    val moveDownLabel = stringResource(R.string.habit_move_down)
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isDragged) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClickLabel = stringResource(R.string.habit_open_card), role = Role.Button, onClick = onClick)
            .semantics {
                customActions = listOfNotNull(
                    onMoveUp?.let { action -> CustomAccessibilityAction(moveUpLabel) { action(); true } },
                    onMoveDown?.let { action -> CustomAccessibilityAction(moveDownLabel) { action(); true } }
                )
            }
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .alpha(if (habit.isArchived) 0.5f else 1f)
                .clip(CircleShape)
                .background(colors.fill),
            contentAlignment = Alignment.Center
        ) {
            Icon(habit.icon.vector, contentDescription = null, Modifier.size(20.dp), tint = colors.content)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = habit.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (habit.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = habitSummary(habit, item.reminderCount),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (dragHandle != null) {
            val dragLabel = stringResource(R.string.habit_drag, habit.title)
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
        Box {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = { menuOpen = true }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ChekkiIcons.More,
                    contentDescription = stringResource(R.string.habit_menu, habit.title),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                MenuItem(stringResource(R.string.habit_card_edit)) {
                    menuOpen = false
                    onEdit()
                }
                MenuItem(stringResource(if (habit.isArchived) R.string.habit_unarchive else R.string.habit_archive)) {
                    menuOpen = false
                    onArchiveToggle()
                }
                MenuItem(stringResource(R.string.habit_menu_delete), color = ChekkiTheme.colors.priorityHigh) {
                    menuOpen = false
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge, color = color) },
        onClick = onClick
    )
}

/** «Каждый день · 6 раз в день · 4 напоминания»; без напоминаний — без последней части. */
@Composable
private fun habitSummary(habit: HabitEntity, reminderCount: Int): String {
    val parts = buildList {
        add(daysSummary(habit.weekdays))
        add(pluralStringResource(R.plurals.habit_times_per_day, habit.dailyTarget, habit.dailyTarget))
        if (reminderCount > 0) add(pluralStringResource(R.plurals.habit_reminders_count, reminderCount, reminderCount))
    }
    return parts.joinToString(" · ")
}

/** «+ Новая привычка» в конце списка действующих. */
@Composable
private fun AddHabitRow(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Add, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.habit_new), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** «В архиве · 13» со стрелкой: раздел архива свёрнут, пока его не откроют. */
@Composable
private fun ArchiveHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "archiveChevron")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { heading() }
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.habits_archived),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = count.toString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Icon(
            ChekkiIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun HabitsScreenPreview() {
    fun habit(id: Long, title: String, icon: HabitIcon, color: Int, target: Int, archived: Boolean = false) =
        HabitEntity(
            id = id,
            title = title,
            icon = icon,
            colorIndex = color,
            dailyTarget = target,
            position = id.toInt(),
            isArchived = archived,
            createdAt = Instant.EPOCH
        )
    ChekkiTheme {
        HabitsScreen(
            state = HabitsUiState(
                active = listOf(
                    HabitListItem(habit(1, "Утренняя рутина", HabitIcon.Sprout, 0, 1), 0),
                    HabitListItem(habit(2, "Пить воду", HabitIcon.Water, 1, 6), 4),
                    HabitListItem(habit(3, "Английский", HabitIcon.Goal, 2, 2), 2)
                ),
                archived = listOf(HabitListItem(habit(4, "Коллаген", HabitIcon.Pill, 4, 1, archived = true), 0))
            )
        )
    }
}
