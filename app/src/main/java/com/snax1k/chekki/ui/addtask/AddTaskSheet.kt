package com.snax1k.chekki.ui.addtask

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snax1k.chekki.R
import com.snax1k.chekki.data.AllDayReminderTime
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.TaskNotifications
import com.snax1k.chekki.reminders.exactAlarmSettingsIntent
import com.snax1k.chekki.reminders.notificationSettingsIntent
import com.snax1k.chekki.ui.components.formatDayMonthShort
import com.snax1k.chekki.ui.components.formatShortWeekday
import com.snax1k.chekki.ui.components.formatTime
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.cssLinearGradient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Нижняя панель новой задачи. Закрытие (смахивание, «Назад», крестик) черновик не стирает:
 * он вернётся при следующем открытии. После сохранения панель закрывается, черновик очищается.
 */
// ModalBottomSheet в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(viewModel: AddTaskViewModel, onDismiss: () -> Unit) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Сначала доигрываем анимацию закрытия, потом убираем панель
    val hideAndDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    val save: () -> Unit = { viewModel.save(onSaved = hideAndDismiss) }

    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(TaskNotifications.canPost(context)) }
    // Пользователь мог включить уведомления в настройках и вернуться
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = TaskNotifications.canPost(context)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = TaskNotifications.canPost(context)
    }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { DragHandle() }
    ) {
        AddTaskContent(
            draft = viewModel.draft,
            categories = categories,
            today = today,
            onTitleChange = viewModel::onTitleChange,
            onNoteChange = viewModel::onNoteChange,
            onQuickDateSelect = viewModel::onQuickDateSelect,
            onDatePick = viewModel::onDatePick,
            onTimePick = viewModel::onTimePick,
            onCategorySelect = viewModel::onCategorySelect,
            onRepeatSelect = viewModel::onRepeatSelect,
            onReminderSelect = { minutesBefore ->
                viewModel.onReminderSelect(minutesBefore)
                // Разрешение на уведомления спрашиваем, когда напоминание понадобилось
                if (minutesBefore != null && !notificationsAllowed &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            notificationsAllowed = notificationsAllowed,
            onEnableNotifications = { context.startActivity(notificationSettingsIntent(context)) },
            onPrioritySelect = viewModel::onPrioritySelect,
            onAddSubtask = viewModel::addSubtask,
            onSubtaskChange = viewModel::onSubtaskChange,
            onSubtaskRemove = viewModel::removeSubtask,
            onClose = hideAndDismiss,
            onSave = {
                if (viewModel.needsExactAlarmExplanation()) showExactAlarmDialog = true else save()
            }
        )

        if (showExactAlarmDialog) {
            ExactAlarmDialog(
                onAllow = {
                    viewModel.onExactAlarmExplained()
                    showExactAlarmDialog = false
                    save()
                    exactAlarmSettingsIntent(context)?.let(context::startActivity)
                },
                onLater = {
                    viewModel.onExactAlarmExplained()
                    showExactAlarmDialog = false
                    save()
                },
                // Нажали мимо окна — просто закрываем его, задачу не сохраняем
                onDismiss = { showExactAlarmDialog = false }
            )
        }
    }
}

/** Один раз объясняем, зачем разрешать точные будильники. Задача сохраняется при любом ответе. */
@Composable
private fun ExactAlarmDialog(onAllow: () -> Unit, onLater: () -> Unit, onDismiss: () -> Unit) {
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

/** Какое окно выбора открыто поверх панели: сначала календарь, после «Далее» — циферблат. */
private enum class PickerStep { Date, Time }

@Composable
private fun DragHandle() {
    Box(
        Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outline)
    )
}

@Composable
private fun AddTaskContent(
    draft: TaskDraft,
    categories: List<CategoryOption>,
    today: LocalDate,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onQuickDateSelect: (QuickDate) -> Unit,
    onDatePick: (LocalDate) -> Unit,
    onTimePick: (LocalTime?) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onRepeatSelect: (RepeatRule) -> Unit,
    onReminderSelect: (Int?) -> Unit,
    notificationsAllowed: Boolean,
    onEnableNotifications: () -> Unit,
    onPrioritySelect: (TaskPriority) -> Unit,
    onAddSubtask: () -> Long,
    onSubtaskChange: (Long, String) -> Unit,
    onSubtaskRemove: (Long) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    autoFocusTitle: Boolean = true
) {
    val titleFocus = remember { FocusRequester() }
    if (autoFocusTitle) {
        // Клавиатура появляется сразу, можно печатать название
        LaunchedEffect(Unit) { titleFocus.requestFocus() }
    }
    var pickerStep by rememberSaveable { mutableStateOf<PickerStep?>(null) }
    // В какую подзадачу поставить курсор: в только что добавленную
    var subtaskToFocus by remember { mutableStateOf<Long?>(null) }
    val date = draft.dateFor(today)

    Column {
        // Форма прокручивается, если вместе с клавиатурой не помещается; кнопка всегда видна внизу
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SheetHeader(onClose)
            TitleField(draft.title, onTitleChange, Modifier.focusRequester(titleFocus))
            NoteField(draft.note, onNoteChange)
            QuickDateChips(
                selected = draft.quickDate,
                onSelect = onQuickDateSelect,
                modifier = Modifier.padding(top = 12.dp)
            )
            DateTimeRow(
                date = date,
                time = draft.time,
                today = today,
                onClick = { pickerStep = PickerStep.Date },
                modifier = Modifier.padding(top = 4.dp)
            )
            CategoryRow(
                categories = categories,
                selectedId = draft.categoryId,
                onSelect = onCategorySelect
            )
            val repeatPreset = RepeatPreset.of(draft.repeat)
            OptionsRow(
                icon = ChekkiIcons.Repeat,
                label = stringResource(R.string.add_task_repeat),
                value = repeatPreset?.let { repeatLabel(it) } ?: stringResource(R.string.repeat_custom),
                options = RepeatPreset.entries,
                selected = repeatPreset,
                optionLabel = { repeatLabel(it) },
                onSelect = { onRepeatSelect(it.rule) }
            )
            OptionsRow(
                icon = ChekkiIcons.Reminder,
                label = stringResource(R.string.add_task_reminder),
                value = reminderLabel(draft.reminderMinutesBefore, allDay = draft.time == null),
                // «Без напоминания» (null) + варианты для задачи со временем или на весь день
                options = listOf<Int?>(null) +
                    if (draft.time != null) ReminderOptions.timed else ReminderOptions.allDay,
                selected = draft.reminderMinutesBefore,
                optionLabel = { reminderLabel(it, allDay = draft.time == null) },
                onSelect = onReminderSelect
            )
            if (draft.reminderMinutesBefore != null && !notificationsAllowed) {
                NotificationsOffHint(onEnable = onEnableNotifications)
            }
            PrioritySelector(
                selected = draft.priority,
                onSelect = onPrioritySelect,
                modifier = Modifier.padding(top = 8.dp)
            )
            SubtasksSection(
                subtasks = draft.subtasks,
                focusRequestId = subtaskToFocus,
                onFocusRequestHandled = { subtaskToFocus = null },
                onAdd = { subtaskToFocus = onAddSubtask() },
                onChange = onSubtaskChange,
                onRemove = onSubtaskRemove,
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SaveButton(
            enabled = draft.canSave,
            onClick = onSave,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        )
    }

    when (pickerStep) {
        PickerStep.Date -> TaskDatePickerDialog(
            initialDate = date ?: today,
            onDismiss = { pickerStep = null },
            onNext = { picked ->
                onDatePick(picked)
                pickerStep = PickerStep.Time
            }
        )
        PickerStep.Time -> TaskTimePickerDialog(
            initialTime = draft.time ?: nextFullHour(),
            onDismiss = { pickerStep = null },
            onAllDay = {
                onTimePick(null)
                pickerStep = null
            },
            onDone = { picked ->
                onTimePick(picked)
                pickerStep = null
            }
        )
        null -> Unit
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.add_task_header),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ChekkiIcons.Close,
                contentDescription = stringResource(R.string.add_task_close),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TitleField(title: String, onTitleChange: (String) -> Unit, modifier: Modifier = Modifier) {
    PlainTextField(
        value = title,
        onValueChange = onTitleChange,
        label = stringResource(R.string.add_task_title_label),
        hint = stringResource(R.string.add_task_title_hint),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = null,
            color = MaterialTheme.colorScheme.onSurface
        ),
        imeAction = ImeAction.Next,
        maxLines = 3,
        modifier = modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit) {
    PlainTextField(
        value = note,
        onValueChange = onNoteChange,
        label = stringResource(R.string.add_task_note_label),
        hint = stringResource(R.string.add_task_note_hint),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        imeAction = ImeAction.Done,
        maxLines = 5,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/** Поле без рамки, как в макете: только текст и подсказка, пока поле пустое. */
@Composable
private fun PlainTextField(
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
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(hint, style = textStyle, color = ChekkiTheme.colors.textPlaceholder)
                }
                innerTextField()
            }
        }
    )
}

/** Плашки быстрых дат. [selected] null — дата выбрана в календаре, ни одна плашка не горит. */
@Composable
private fun QuickDateChips(selected: QuickDate?, onSelect: (QuickDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = ChekkiTheme.colors
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickDate.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    // Зона нажатия 48 dp, сама плашка ниже
                    .minimumInteractiveComponentSize()
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (isSelected) cssLinearGradient(135f, colors.fabGradientStart, colors.fabGradientEnd)
                        else SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(option.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.onFab else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Строка панели: иконка, название слева, значение и шеврон справа. */
@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun DateTimeRow(
    date: LocalDate?,
    time: LocalTime?,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SheetRow(
        icon = ChekkiIcons.Calendar,
        label = stringResource(R.string.add_task_date_time),
        onClick = onClick,
        modifier = modifier
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
private fun CategoryRow(
    categories: List<CategoryOption>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    val palette = ChekkiTheme.colors.categories
    Box(modifier) {
        SheetRow(
            icon = ChekkiIcons.Tag,
            label = stringResource(R.string.add_task_category),
            onClick = { expanded = true }
        ) {
            if (selected != null) {
                CategoryDot(palette[selected.colorIndex % palette.size])
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
                dotColor = null,
                isSelected = selectedId == null,
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                CategoryMenuItem(
                    name = category.name,
                    dotColor = palette[category.colorIndex % palette.size],
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
private fun <T> OptionsRow(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        SheetRow(icon = icon, label = label, onClick = { expanded = true }) {
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

/** Подпись под напоминанием, если уведомления запрещены: само напоминание сохранится, но не придёт. */
@Composable
private fun NotificationsOffHint(onEnable: () -> Unit) {
    Row(
        // Отступ под текст строки: иконка 22 + промежуток 12
        Modifier
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

@Composable
private fun repeatLabel(preset: RepeatPreset): String = stringResource(
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
private fun reminderLabel(minutesBefore: Int?, allDay: Boolean): String {
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
private fun CategoryMenuItem(name: String, dotColor: Color?, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        leadingIcon = {
            if (dotColor != null) CategoryDot(dotColor) else Spacer(Modifier.width(10.dp))
        },
        trailingIcon = {
            if (isSelected) {
                Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun CategoryDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PrioritySelector(selected: TaskPriority, onSelect: (TaskPriority) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.add_task_priority),
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TaskPriority.entries.forEach { priority ->
                val isSelected = priority == selected
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(priority) }),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSelected && priority != TaskPriority.None) ChekkiIcons.PriorityFlag else ChekkiIcons.PriorityFlagOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = priorityColor(priority)
                    )
                    Text(
                        text = stringResource(priority.shortLabel),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Подзадачи: строки с полем и крестиком, внизу «Добавить подзадачу».
 * «Далее» на клавиатуре в последней заполненной подзадаче сразу добавляет следующую.
 */
@Composable
private fun SubtasksSection(
    subtasks: List<SubtaskDraft>,
    focusRequestId: Long?,
    onFocusRequestHandled: () -> Unit,
    onAdd: () -> Unit,
    onChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.add_task_subtasks),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtasks.isNotEmpty()) {
                Text(
                    text = subtasks.size.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        subtasks.forEachIndexed { index, subtask ->
            key(subtask.id) {
                SubtaskField(
                    subtask = subtask,
                    requestFocus = subtask.id == focusRequestId,
                    onFocusRequested = onFocusRequestHandled,
                    onChange = { onChange(subtask.id, it) },
                    onRemove = { onRemove(subtask.id) },
                    onNext = {
                        when {
                            index < subtasks.lastIndex -> focusManager.moveFocus(FocusDirection.Down)
                            subtask.title.isNotBlank() -> onAdd()
                            else -> focusManager.clearFocus()
                        }
                    }
                )
            }
        }
        AddSubtaskButton(onAdd)
    }
}

@Composable
private fun SubtaskField(
    subtask: SubtaskDraft,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    onChange: (String) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    if (requestFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Отметка только для вида: отмечать подзадачи можно будет в карточке задачи
        Box(
            Modifier
                .size(20.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline, SubtaskBoxShape)
        )
        PlainTextField(
            value = subtask.title,
            onValueChange = onChange,
            label = stringResource(R.string.add_task_subtask_label),
            hint = stringResource(R.string.add_task_subtask_hint),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            imeAction = ImeAction.Next,
            maxLines = 1,
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ChekkiIcons.Close,
                contentDescription = stringResource(R.string.add_task_remove_subtask),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// Квадрат с небольшим скруглением, как в макете, чтобы подзадачи отличались от круглых отметок задач
private val SubtaskBoxShape = RoundedCornerShape(6.dp)

@Composable
private fun AddSubtaskButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
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

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ChekkiTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            // Пока названия нет, кнопка бледная и не нажимается
            .alpha(if (enabled) 1f else 0.4f)
            .clip(MaterialTheme.shapes.medium)
            .background(cssLinearGradient(135f, colors.fabGradientStart, colors.fabGradientEnd))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.main_add_task),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onFab
        )
    }
}

@Composable
private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.None -> MaterialTheme.colorScheme.onSurfaceVariant
    TaskPriority.Low -> ChekkiTheme.colors.priorityLow
    TaskPriority.Medium -> ChekkiTheme.colors.priorityMedium
    TaskPriority.High -> ChekkiTheme.colors.priorityHigh
}

private val QuickDate.label: Int
    get() = when (this) {
        QuickDate.Today -> R.string.quick_date_today
        QuickDate.Tomorrow -> R.string.quick_date_tomorrow
        QuickDate.Weekend -> R.string.quick_date_weekend
        QuickDate.NoDate -> R.string.quick_date_none
    }

private val TaskPriority.shortLabel: Int
    get() = when (this) {
        TaskPriority.None -> R.string.priority_short_none
        TaskPriority.Low -> R.string.priority_short_low
        TaskPriority.Medium -> R.string.priority_short_medium
        TaskPriority.High -> R.string.priority_short_high
    }

@Preview(widthDp = 412, heightDp = 760)
@Composable
private fun AddTaskContentPreview() {
    ChekkiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            AddTaskContent(
                draft = TaskDraft(
                    title = "Подготовить презентацию",
                    quickDate = QuickDate.Weekend,
                    time = LocalTime.of(16, 0),
                    categoryId = 1,
                    priority = TaskPriority.High,
                    repeat = RepeatPreset.Weekly.rule,
                    reminderMinutesBefore = 15,
                    subtasks = listOf(SubtaskDraft(0, "Собрать цифры из отчёта"), SubtaskDraft(1, "Сделать 10 слайдов"))
                ),
                categories = listOf(CategoryOption(1, "Работа", 0), CategoryOption(2, "Личное", 1)),
                today = LocalDate.of(2026, 10, 22),
                onTitleChange = {},
                onNoteChange = {},
                onQuickDateSelect = {},
                onDatePick = {},
                onTimePick = {},
                onCategorySelect = {},
                onRepeatSelect = {},
                onReminderSelect = {},
                notificationsAllowed = false,
                onEnableNotifications = {},
                onPrioritySelect = {},
                onAddSubtask = { 0L },
                onSubtaskChange = { _, _ -> },
                onSubtaskRemove = {},
                onClose = {},
                onSave = {},
                autoFocusTitle = false
            )
        }
    }
}
