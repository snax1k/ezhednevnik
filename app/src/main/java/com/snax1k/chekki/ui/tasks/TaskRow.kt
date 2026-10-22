package com.snax1k.chekki.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.components.formatTime
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme

/**
 * Задача в списке: отметка, название, строка свойств и флажок приоритета.
 * Нажатие на строку ([onClick]) открывает карточку, на кружок — только ставит или снимает отметку.
 */
@Composable
fun TaskRow(task: TaskUi, onToggleDone: () -> Unit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val priorityColor = task.priority?.let { priorityColor(it) }
    val titleColor by animateColorAsState(
        if (task.done) ChekkiTheme.colors.taskDoneTitle else MaterialTheme.colorScheme.onSurface,
        label = "taskTitleColor"
    )
    val openLabel = stringResource(R.string.task_open)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (onClick != null) Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        TaskCheckbox(task.done, task.title, priorityColor, onToggleDone)
        // Отступ сверху ставит первую строку названия по центру отметки (48 / 2 − 20 / 2)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 4.dp, top = 14.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                textDecoration = if (task.done) TextDecoration.LineThrough else null
            )
            TaskDetails(task, Modifier.padding(top = 3.dp))
        }
        if (task.priority != null && priorityColor != null && !task.done) {
            Icon(
                ChekkiIcons.PriorityFlag,
                contentDescription = stringResource(priorityLabel(task.priority)),
                modifier = Modifier
                    .padding(start = 8.dp, top = 15.dp)
                    .size(18.dp),
                tint = priorityColor
            )
        }
    }
}

/**
 * Отметка выполнения: кольцо цвета приоритета ([priorityColor] null — без приоритета),
 * после нажатия — заливка с галочкой. Зона нажатия 48 dp.
 */
@Composable
internal fun TaskCheckbox(done: Boolean, title: String, priorityColor: Color?, onToggleDone: () -> Unit) {
    val ringColor = priorityColor ?: MaterialTheme.colorScheme.outline
    val description = stringResource(
        if (done) R.string.task_mark_undone else R.string.task_mark_done,
        title
    )
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggleDone() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (!done) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (priorityColor != null) priorityColor.copy(alpha = 0.10f) else Color.Transparent)
                    .border(2.dp, ringColor, CircleShape)
            )
        }
        AnimatedVisibility(
            visible = done,
            enter = scaleIn(
                initialScale = 0.55f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ChekkiTheme.colors.taskDone),
                contentAlignment = Alignment.Center
            ) {
                Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(14.dp), tint = ChekkiTheme.colors.onTaskDone)
            }
        }
    }
}

/** Строка свойств: время, категория, подзадачи, повтор, напоминание. */
@Composable
private fun TaskDetails(task: TaskUi, modifier: Modifier = Modifier) {
    val categoryColors = ChekkiTheme.colors.categories
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailWithIcon(
            icon = ChekkiIcons.Clock,
            iconDescription = null,
            text = task.time?.let { formatTime(it) } ?: stringResource(R.string.task_all_day)
        )
        if (task.category != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(categoryColors[task.category.colorIndex % categoryColors.size])
                )
                DetailText(task.category.name)
            }
        }
        if (task.subtasksTotal > 0) {
            DetailWithIcon(
                icon = ChekkiIcons.Subtasks,
                iconDescription = stringResource(R.string.task_subtasks),
                text = "${task.subtasksDone}/${task.subtasksTotal}"
            )
        }
        if (task.repeats) {
            DetailIcon(ChekkiIcons.Repeat, stringResource(R.string.task_repeats))
        }
        if (task.hasReminder) {
            DetailIcon(ChekkiIcons.Reminder, stringResource(R.string.task_reminder))
        }
    }
}

@Composable
private fun DetailWithIcon(icon: ImageVector, iconDescription: String?, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailIcon(icon, iconDescription)
        DetailText(text)
    }
}

@Composable
private fun DetailIcon(icon: ImageVector, contentDescription: String?) {
    Icon(icon, contentDescription, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.High -> ChekkiTheme.colors.priorityHigh
    Priority.Medium -> ChekkiTheme.colors.priorityMedium
    Priority.Low -> ChekkiTheme.colors.priorityLow
}

private fun priorityLabel(priority: Priority): Int = when (priority) {
    Priority.High -> R.string.priority_high
    Priority.Medium -> R.string.priority_medium
    Priority.Low -> R.string.priority_low
}
