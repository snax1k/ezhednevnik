package com.snax1k.chekki.ui.addtask

import com.snax1k.chekki.ui.category.vector
import com.snax1k.chekki.data.db.CategoryIcon
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.snax1k.chekki.R
import com.snax1k.chekki.data.AllDayReminderTime
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.TaskNotifications
import com.snax1k.chekki.ui.components.formatDayMonthShort
import com.snax1k.chekki.ui.components.formatShortWeekday
import com.snax1k.chekki.ui.components.formatTime
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import java.time.LocalDate
import java.time.LocalTime

// Поля задачи, общие для панели новой задачи и карточки задачи: выглядят и работают одинаково.

/**
 * Где стоит строка: в панели строки идут без своего фона и отступов, в карточке —
 * внутри общей скруглённой подложки, поэтому им нужны отступы по бокам и прямые края.
 */
internal enum class FieldRowPlacement { Sheet, Card }

/** Строка свойства: иконка, название слева, значение и шеврон справа. */
@Composable
internal fun FieldRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placement: FieldRowPlacement = FieldRowPlacement.Sheet,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    value: @Composable RowScope.() -> Unit
) {
    val inCard = placement == FieldRowPlacement.Card
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(if (inCard) RectangleShape else MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = if (inCard) 16.dp else 0.dp, end = if (inCard) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = iconTint)
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        value()
        Icon(ChekkiIcons.ChevronRight, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
internal fun DateTimeRow(
    date: LocalDate?,
    time: LocalTime?,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placement: FieldRowPlacement = FieldRowPlacement.Sheet
) {
    FieldRow(
        icon = ChekkiIcons.Calendar,
        label = stringResource(R.string.add_task_date_time),
        onClick = onClick,
        modifier = modifier,
        placement = placement
    ) {
        Text(
            text = dateTimeValue(date, time, today),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
    }
}

/** «Сегодня, 22 окт · 16:00», «Сб, 24 окт · весь день» или «Не задана». */
@Composable
private fun dateTimeValue(date: LocalDate?, time: LocalTime?, today: LocalDate): String {
    if (date == null) return stringResource(R.string.add_task_date_not_set)
    val day = when (date) {
        today -> stringResource(R.string.quick_date_today)
        today.plusDays(1) -> stringResource(R.string.quick_date_tomorrow)
        else -> formatShortWeekday(date)
    }
    val timeText = time?.let { formatTime(it) } ?: stringResource(R.string.add_task_all_day_value)
    return stringResource(R.string.add_task_date_time_value, day, formatDayMonthShort(date), timeText)
}

@Composable
internal fun CategoryRow(
    categories: List<CategoryOption>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    placement: FieldRowPlacement = FieldRowPlacement.Sheet
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    val palette = ChekkiTheme.colors.categories
    Box(modifier) {
        FieldRow(
            icon = ChekkiIcons.Tag,
            label = stringResource(R.string.add_task_category),
            onClick = { expanded = true },
            placement = placement
        ) {
            if (selected != null) {
                CategoryMark(selected.icon, palette[selected.colorIndex % palette.size])
            }
            Text(
                text = selected?.name ?: stringResource(R.string.add_task_no_category),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            CategoryMenuItem(
                name = stringResource(R.string.add_task_no_category),
                icon = null,
                color = null,
                isSelected = selectedId == null,
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            // Архивные не предлагаются; выбранная остаётся в меню, даже если её убрали в архив
            categories.filter { !it.isArchived || it.id == selectedId }.forEach { category ->
                CategoryMenuItem(
                    name = category.name,
                    icon = category.icon,
                    color = palette[category.colorIndex % palette.size],
                    isSelected = category.id == selectedId,
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Строка с выбором из списка: [value] справа, по нажатию — меню, выбранный вариант с галочкой.
 * Пустой вариант (null) тоже может быть выбранным — например, «Без напоминания».
 */
@Composable
internal fun <T> OptionsRow(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placement: FieldRowPlacement = FieldRowPlacement.Sheet,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        FieldRow(
            icon = icon,
            label = label,
            onClick = { expanded = true },
            placement = placement,
            iconTint = iconTint
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option == selected) {
                            Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

/** Варианты напоминания для задачи со временем или на весь день, первым — «Без напоминания» (null). */
internal fun reminderOptions(allDay: Boolean): List<Int?> =
    listOf<Int?>(null) + if (allDay) ReminderOptions.allDay else ReminderOptions.timed

/** Подпись под напоминанием, если уведомления запрещены: само напоминание сохранится, но не придёт. */
@Composable
internal fun NotificationsOffHint(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        // Отступ под текст строки: иконка 22 + промежуток 12
        modifier
            .fillMaxWidth()
            .padding(start = 34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.reminder_notifications_off),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = ChekkiTheme.colors.priorityHigh
        )
        TextButton(onClick = onEnable) { Text(stringResource(R.string.action_enable)) }
    }
}

/** Можно ли показывать уведомления и как попросить разрешение. */
internal class NotificationAccess(val allowed: Boolean, val request: () -> Unit)

/**
 * Разрешение на уведомления для напоминаний. Спрашиваем, когда напоминание понадобилось;
 * после возвращения из настроек проверяем заново — пользователь мог включить уведомления там.
 */
@Composable
internal fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(TaskNotifications.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = TaskNotifications.canPost(context)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = TaskNotifications.canPost(context)
    }
    return NotificationAccess(allowed) {
        if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** Один раз объясняем, зачем разрешать точные будильники. */
@Composable
internal fun ExactAlarmDialog(onAllow: () -> Unit, onLater: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onAllow) { Text(stringResource(R.string.action_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.action_not_now)) }
        },
        icon = { Icon(ChekkiIcons.Reminder, contentDescription = null, Modifier.size(24.dp)) },
        title = { Text(stringResource(R.string.exact_alarm_title)) },
        text = { Text(stringResource(R.string.exact_alarm_text), style = MaterialTheme.typography.bodyMedium) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.primary
    )
}

/** Поле без рамки, как в макете: только текст и подсказка, пока поле пустое. */
@Composable
internal fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    textStyle: TextStyle,
    imeAction: ImeAction,
    maxLines: Int,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        // В одну строку Enter не переносит строку, а выполняет действие клавиатуры
        singleLine = maxLines == 1,
        maxLines = maxLines,
        decorationBox = { innerTextField -> HintBox(value.isEmpty(), hint, textStyle, innerTextField) }
    )
}

/**
 * То же поле, но со своим положением курсора: например, чтобы при начале правки
 * курсор стоял в конце текста, а не в начале.
 */
@Composable
internal fun PlainTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    hint: String,
    textStyle: TextStyle,
    imeAction: ImeAction,
    maxLines: Int,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = maxLines == 1,
        maxLines = maxLines,
        decorationBox = { innerTextField -> HintBox(value.text.isEmpty(), hint, textStyle, innerTextField) }
    )
}

/** Подсказка серым, пока поле пустое. */
@Composable
private fun HintBox(isEmpty: Boolean, hint: String, textStyle: TextStyle, innerTextField: @Composable () -> Unit) {
    Box {
        if (isEmpty) {
            Text(hint, style = textStyle, color = ChekkiTheme.colors.textPlaceholder)
        }
        innerTextField()
    }
}

@Composable
internal fun repeatLabel(preset: RepeatPreset): String = stringResource(
    when (preset) {
        RepeatPreset.None -> R.string.repeat_none
        RepeatPreset.Daily -> R.string.repeat_daily
        RepeatPreset.Weekdays -> R.string.repeat_weekdays
        RepeatPreset.Weekly -> R.string.repeat_weekly
        RepeatPreset.Biweekly -> R.string.repeat_biweekly
        RepeatPreset.Monthly -> R.string.repeat_monthly
        RepeatPreset.Quarterly -> R.string.repeat_quarterly
        RepeatPreset.Yearly -> R.string.repeat_yearly
    }
)

/** «За 15 минут»; у задачи на весь день — «В день задачи, 9:00» и «Накануне, 9:00». */
@Composable
internal fun reminderLabel(minutesBefore: Int?, allDay: Boolean): String {
    val allDayTime = formatTime(AllDayReminderTime)
    return when {
        minutesBefore == null -> stringResource(R.string.reminder_none)
        allDay && minutesBefore == 0 -> stringResource(R.string.reminder_all_day_same_day, allDayTime)
        allDay && minutesBefore == 24 * 60 -> stringResource(R.string.reminder_all_day_day_before, allDayTime)
        minutesBefore == 0 -> stringResource(R.string.reminder_at_start)
        minutesBefore == 60 -> stringResource(R.string.reminder_hour_before)
        minutesBefore == 24 * 60 -> stringResource(R.string.reminder_day_before)
        else -> stringResource(R.string.reminder_minutes_before, minutesBefore)
    }
}

@Composable
private fun CategoryMenuItem(name: String, icon: CategoryIcon?, color: Color?, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        leadingIcon = {
            if (icon != null && color != null) CategoryMark(icon, color) else Spacer(Modifier.width(18.dp))
        },
        trailingIcon = {
            if (isSelected) {
                Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

/** Иконка категории в её цвете — в выборе категории и на плашке карточки задачи. */
@Composable
internal fun CategoryMark(icon: CategoryIcon, color: Color, size: Dp = 18.dp) {
    Icon(icon.vector, contentDescription = null, Modifier.size(size), tint = color)
}

@Composable
internal fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.None -> MaterialTheme.colorScheme.onSurfaceVariant
    TaskPriority.Low -> ChekkiTheme.colors.priorityLow
    TaskPriority.Medium -> ChekkiTheme.colors.priorityMedium
    TaskPriority.High -> ChekkiTheme.colors.priorityHigh
}

internal val QuickDate.label: Int
    get() = when (this) {
        QuickDate.Today -> R.string.quick_date_today
        QuickDate.Tomorrow -> R.string.quick_date_tomorrow
        QuickDate.Weekend -> R.string.quick_date_weekend
        QuickDate.NoDate -> R.string.quick_date_none
    }

internal val TaskPriority.shortLabel: Int
    get() = when (this) {
        TaskPriority.None -> R.string.priority_short_none
        TaskPriority.Low -> R.string.priority_short_low
        TaskPriority.Medium -> R.string.priority_short_medium
        TaskPriority.High -> R.string.priority_short_high
    }

/** «Добавить подзадачу» под списком подзадач. [contentPadding] — отступ иконки внутри зоны нажатия. */
@Composable
internal fun AddSubtaskButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Add, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.add_task_add_subtask),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Наибольшая высота содержимого нижней панели — 85% окна. В Material 3 1.4 панель, которая при изменении
 * содержимого дорастает до верха экрана, сама уходит вниз, а её невидимое окно остаётся и перехватывает
 * нажатия. С этим ограничением панель до верха не дорастает, лишнее прокручивается внутри.
 */
@Composable
internal fun sheetContentMaxHeight(): Dp {
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { windowHeightPx.toDp() } * 0.85f
}
