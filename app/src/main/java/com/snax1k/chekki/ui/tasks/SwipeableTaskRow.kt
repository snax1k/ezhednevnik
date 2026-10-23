package com.snax1k.chekki.ui.tasks

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.addtask.TaskDatePickerDialog
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Ширина одной кнопки под строкой при свайпе влево, как в макете «Состояния и жесты». */
private val ActionWidth = 80.dp

/** Свайп вправо срабатывает после 30% ширины строки, длинный свайп влево — после 60%. */
private const val CompleteThreshold = 0.3f
private const val QuickPostponeThreshold = 0.6f

/**
 * Строка задачи со свайпами (по макету «Состояния и жесты»):
 * - вправо — «Выполнить» (у выполненной — «Вернуть»), срабатывает после 30% ширины, на пороге вибрация;
 * - влево коротко — кнопки «Отложить» (меню вариантов) и «Удалить», у выполненной — только «Удалить»;
 * - влево длинно — сразу отложить на первый вариант меню (обычно «Завтра»).
 *
 * Открытой может быть только одна строка: [isOpen] приходит от экрана, [onOpen] и [onClose] сообщают ему.
 * Те же действия доступны TalkBack через меню действий строки.
 */
@Composable
fun SwipeableTaskRow(
    task: TaskUi,
    today: LocalDate,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    onPostpone: (LocalDate) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var rowWidth by remember { mutableIntStateOf(0) }
    var postponeMenuOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    val actionWidthPx = with(density) { ActionWidth.toPx() }
    // У выполненной задачи под строкой только «Удалить»
    val revealWidthPx = if (task.done) actionWidthPx else actionWidthPx * 2
    // Длинный свайп влево откладывает на первый подходящий вариант: обычно «Завтра»,
    // а у задачи, которая уже на завтра, — «На выходных»
    val quickPostpone = if (task.done) null else PostponeOption.available(today, task.date).firstOrNull()
    val completeArmed = rowWidth > 0 && offsetX >= rowWidth * CompleteThreshold
    val postponeArmed = rowWidth > 0 && quickPostpone != null && -offsetX >= rowWidth * QuickPostponeThreshold

    // Лёгкая вибрация, когда свайп переходит порог
    LaunchedEffect(completeArmed) {
        if (completeArmed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }
    LaunchedEffect(postponeArmed) {
        if (postponeArmed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    suspend fun animateOffsetTo(target: Float) {
        animate(initialValue = offsetX, targetValue = target) { value, _ -> offsetX = value }
    }

    fun close() {
        scope.launch { animateOffsetTo(0f) }
        onClose()
    }

    // Открыли другую строку — эта закрывается
    LaunchedEffect(isOpen) {
        if (!isOpen && offsetX != 0f) animateOffsetTo(0f)
    }

    val completeLabel = stringResource(if (task.done) R.string.action_uncomplete else R.string.swipe_complete)
    val deleteLabel = stringResource(R.string.task_delete)
    val postponeLabel = quickPostpone?.let {
        stringResource(R.string.action_postpone_to, stringResource(it.first.label).lowercase())
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .onSizeChanged { rowWidth = it.width }
    ) {
        if (offsetX > 0f) {
            CompleteBackground(task.done, armed = completeArmed, Modifier.matchParentSize())
        }
        if (offsetX < 0f) {
            val exposed = -offsetX
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
                if (quickPostpone != null) {
                    // Пока свайп длиннее двух кнопок, «Отложить» растягивается; за порогом — занимает всё
                    val postponeWidth = if (postponeArmed) exposed else maxOf(actionWidthPx, exposed - actionWidthPx)
                    Box(Modifier.width(with(density) { postponeWidth.toDp() }).fillMaxHeight()) {
                        SwipeAction(
                            icon = ChekkiIcons.Postpone,
                            text = if (postponeArmed) stringResource(quickPostpone.first.label) else stringResource(R.string.task_postpone),
                            background = ChekkiTheme.colors.swipePostpone,
                            content = ChekkiTheme.colors.onSwipePostpone,
                            onClick = { postponeMenuOpen = true }
                        )
                        PostponeMenu(
                            expanded = postponeMenuOpen,
                            onDismiss = { postponeMenuOpen = false },
                            today = today,
                            currentDate = task.date,
                            onSelect = { date ->
                                close()
                                onPostpone(date)
                            },
                            onPickDate = { datePickerOpen = true }
                        )
                    }
                }
                if (!postponeArmed) {
                    val deleteWidth = if (quickPostpone == null) maxOf(actionWidthPx, exposed) else actionWidthPx
                    SwipeAction(
                        icon = ChekkiIcons.Delete,
                        text = stringResource(R.string.swipe_delete),
                        background = ChekkiTheme.colors.swipeDelete,
                        content = ChekkiTheme.colors.onSwipeDelete,
                        onClick = {
                            close()
                            onDelete()
                        },
                        modifier = Modifier.width(with(density) { deleteWidth.toDp() })
                    )
                }
            }
        }
        TaskRow(
            task = task,
            onToggleDone = onToggleDone,
            // Открытую строку нажатие закрывает, а не открывает карточку
            onClick = { if (offsetX != 0f) close() else onClick() },
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-rowWidth.toFloat(), rowWidth.toFloat())
                    },
                    onDragStarted = { onOpen() },
                    onDragStopped = {
                        // Пороги считаем по последнему положению строки, а не по прошлой отрисовке
                        val width = rowWidth.toFloat()
                        when {
                            width > 0f && offsetX >= width * CompleteThreshold -> {
                                onToggleDone()
                                animateOffsetTo(0f)
                                onClose()
                            }
                            width > 0f && quickPostpone != null && -offsetX >= width * QuickPostponeThreshold -> {
                                // Строка уезжает влево целиком, задача уходит из группы
                                animateOffsetTo(-rowWidth.toFloat())
                                onClose()
                                onPostpone(quickPostpone.second)
                                offsetX = 0f
                            }
                            offsetX <= -revealWidthPx / 2 -> animateOffsetTo(-revealWidthPx)
                            else -> {
                                animateOffsetTo(0f)
                                onClose()
                            }
                        }
                    }
                )
                .semantics {
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(completeLabel) { onToggleDone(); true },
                        if (quickPostpone != null && postponeLabel != null) {
                            CustomAccessibilityAction(postponeLabel) { onPostpone(quickPostpone.second); true }
                        } else {
                            null
                        },
                        CustomAccessibilityAction(deleteLabel) { onDelete(); true }
                    )
                }
        )
    }

    if (datePickerOpen) {
        TaskDatePickerDialog(
            initialDate = today.plusDays(1),
            onDismiss = { datePickerOpen = false },
            onNext = { date ->
                datePickerOpen = false
                close()
                onPostpone(date)
            },
            confirmLabel = R.string.action_done
        )
    }
}

/** Под строкой при свайпе вправо: зелёная полоса с галочкой, за порогом иконка увеличивается. */
@Composable
private fun CompleteBackground(isDone: Boolean, armed: Boolean, modifier: Modifier) {
    val colors = ChekkiTheme.colors
    Row(
        modifier
            .background(colors.taskDone)
            .padding(start = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) ChekkiIcons.Back else ChekkiIcons.Check,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .scale(if (armed) 1.25f else 1f),
            tint = colors.onTaskDone
        )
        Text(
            text = stringResource(if (isDone) R.string.task_return else R.string.swipe_complete),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onTaskDone
        )
    }
}

/** Кнопка под строкой при свайпе влево: иконка и подпись в столбик. */
@Composable
private fun SwipeAction(
    icon: ImageVector,
    text: String,
    background: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = content)
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}
