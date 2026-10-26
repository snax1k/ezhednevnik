package com.snax1k.chekki.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DeletedTask
import com.snax1k.chekki.data.MovedTask
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.exactAlarmSettingsIntent
import com.snax1k.chekki.reminders.notificationSettingsIntent
import com.snax1k.chekki.ui.addtask.AddSubtaskButton
import com.snax1k.chekki.ui.addtask.CategoryMark
import com.snax1k.chekki.ui.addtask.CategoryOption
import com.snax1k.chekki.ui.addtask.CategoryRow
import com.snax1k.chekki.ui.addtask.DateTimeRow
import com.snax1k.chekki.ui.addtask.ExactAlarmDialog
import com.snax1k.chekki.ui.addtask.FieldRowPlacement
import com.snax1k.chekki.ui.addtask.NotificationsOffHint
import com.snax1k.chekki.ui.addtask.OptionsRow
import com.snax1k.chekki.ui.addtask.PlainTextField
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.addtask.RepeatPreset
import com.snax1k.chekki.ui.addtask.TaskDatePickerDialog
import com.snax1k.chekki.ui.addtask.TaskDraft
import com.snax1k.chekki.ui.addtask.TaskTimePickerDialog
import com.snax1k.chekki.ui.addtask.label
import com.snax1k.chekki.ui.addtask.nextFullHour
import com.snax1k.chekki.ui.addtask.priorityColor
import com.snax1k.chekki.ui.addtask.rememberNotificationAccess
import com.snax1k.chekki.ui.addtask.reminderLabel
import com.snax1k.chekki.ui.addtask.reminderOptions
import com.snax1k.chekki.ui.addtask.repeatLabel
import com.snax1k.chekki.ui.addtask.shortLabel
import com.snax1k.chekki.ui.components.CardProgressBar
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.tasks.PostponeMenu
import com.snax1k.chekki.ui.tasks.TaskCheckbox
import com.snax1k.chekki.ui.tasks.checkboxColor
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.GroupColor
import java.time.LocalDate
import java.time.LocalTime

/**
 * Карточка задачи на весь экран. Макет есть только в стиле А, здесь он переведён на Б.
 * Удаление закрывает карточку; вернуть задачу можно с плашки «Отменить» на предыдущем экране.
 */
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit,
    onDeleted: (DeletedTask) -> Unit,
    onPostponed: (MovedTask) -> Unit
) {
    val isDone by viewModel.isDone.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    if (viewModel.isMissing) {
        LaunchedEffect(Unit) { onBack() }
    }
    val context = LocalContext.current
    val notifications = rememberNotificationAccess()
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    TaskContent(
        draft = viewModel.draft,
        isDone = isDone,
        subtasks = subtasks,
        categories = categories,
        today = today,
        onBack = onBack,
        onDelete = { viewModel.delete(onDeleted) },
        onPostpone = { date -> viewModel.postpone(date, onPostponed) },
        onToggleDone = viewModel::toggleDone,
        onTitleChange = viewModel::onTitleChange,
        onTitleEditEnd = viewModel::onTitleEditEnd,
        onNoteChange = viewModel::onNoteChange,
        onQuickDateSelect = viewModel::onQuickDateSelect,
        onDatePick = viewModel::onDatePick,
        onTimePick = viewModel::onTimePick,
        onReminderSelect = { minutesBefore ->
            viewModel.onReminderSelect(minutesBefore)
            if (minutesBefore != null) {
                // Сначала разрешение на уведомления; про точные будильники — когда уведомления уже разрешены
                if (!notifications.allowed) notifications.request()
                else if (viewModel.needsExactAlarmExplanation()) showExactAlarmDialog = true
            }
        },
        notificationsAllowed = notifications.allowed,
        onEnableNotifications = { context.startActivity(notificationSettingsIntent(context)) },
        onRepeatSelect = viewModel::onRepeatSelect,
        onPrioritySelect = viewModel::onPrioritySelect,
        onCategorySelect = viewModel::onCategorySelect,
        subtaskRows = subtaskRows(subtasks, viewModel.editingSubtask, viewModel.newSubtaskKeys),
        subtaskActions = remember(viewModel) {
            SubtaskActions(
                onToggle = viewModel::toggleSubtask,
                onEdit = viewModel::editSubtask,
                onTextChange = viewModel::onSubtaskTextChange,
                onEditEnd = viewModel::onSubtaskEditEnd,
                onDelete = viewModel::deleteSubtask,
                onAdd = viewModel::addSubtask
            )
        }
    )

    if (showExactAlarmDialog) {
        ExactAlarmDialog(
            onAllow = {
                viewModel.onExactAlarmExplained()
                showExactAlarmDialog = false
                exactAlarmSettingsIntent(context)?.let(context::startActivity)
            },
            onLater = {
                viewModel.onExactAlarmExplained()
                showExactAlarmDialog = false
            },
            onDismiss = { showExactAlarmDialog = false }
        )
    }
}

/** Какое окно выбора открыто: календарь и после «Далее» циферблат для «Дата и время» или календарь для «Отложить». */
private enum class PickerStep { Date, Time, PostponeDate }

@Composable
private fun TaskContent(
    draft: TaskDraft?,
    isDone: Boolean,
    subtasks: List<SubtaskUi>,
    categories: List<CategoryOption>,
    today: LocalDate,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onPostpone: (LocalDate) -> Unit,
    onToggleDone: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTitleEditEnd: () -> Unit,
    onNoteChange: (String) -> Unit,
    onQuickDateSelect: (QuickDate) -> Unit,
    onDatePick: (LocalDate) -> Unit,
    onTimePick: (LocalTime?) -> Unit,
    onReminderSelect: (Int?) -> Unit,
    notificationsAllowed: Boolean,
    onEnableNotifications: () -> Unit,
    onRepeatSelect: (RepeatRule) -> Unit,
    onPrioritySelect: (TaskPriority) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    subtaskRows: List<SubtaskRowUi>,
    subtaskActions: SubtaskActions
) {
    var pickerStep by rememberSaveable { mutableStateOf<PickerStep?>(null) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // Кнопка внизу поднимается над клавиатурой, пока правят название или заметку
                .imePadding()
        ) {
            TaskTopBar(onBack, onDelete, deleteEnabled = draft != null)
            // Пока задача грузится, показываем только верхнюю панель
            if (draft != null) {
                TaskBody(
                    draft = draft,
                    isDone = isDone,
                    subtasks = subtasks,
                    categories = categories,
                    today = today,
                    onToggleDone = onToggleDone,
                    onPostpone = onPostpone,
                    onPostponePickDate = { pickerStep = PickerStep.PostponeDate },
                    onTitleChange = onTitleChange,
                    onTitleEditEnd = onTitleEditEnd,
                    onNoteChange = onNoteChange,
                    onDateClick = { pickerStep = PickerStep.Date },
                    onQuickDateSelect = onQuickDateSelect,
                    onReminderSelect = onReminderSelect,
                    notificationsAllowed = notificationsAllowed,
                    onEnableNotifications = onEnableNotifications,
                    onRepeatSelect = onRepeatSelect,
                    onPrioritySelect = onPrioritySelect,
                    onCategorySelect = onCategorySelect,
                    subtaskRows = subtaskRows,
                    subtaskActions = subtaskActions
                )
            }
        }
    }

    if (draft != null) {
        val date = draft.dateFor(today)
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
            // «Отложить» → «Выбрать дату…»: только день, время остаётся прежним
            PickerStep.PostponeDate -> TaskDatePickerDialog(
                initialDate = today.plusDays(1),
                onDismiss = { pickerStep = null },
                onNext = { picked ->
                    pickerStep = null
                    onPostpone(picked)
                },
                confirmLabel = R.string.action_done
            )
            null -> Unit
        }
    }
}

/** Всё под верхней панелью: прокручиваемые поля и кнопка «Выполнено» внизу. */
@Composable
private fun ColumnScope.TaskBody(
    draft: TaskDraft,
    isDone: Boolean,
    subtasks: List<SubtaskUi>,
    categories: List<CategoryOption>,
    today: LocalDate,
    onToggleDone: () -> Unit,
    onPostpone: (LocalDate) -> Unit,
    onPostponePickDate: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTitleEditEnd: () -> Unit,
    onNoteChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onQuickDateSelect: (QuickDate) -> Unit,
    onReminderSelect: (Int?) -> Unit,
    notificationsAllowed: Boolean,
    onEnableNotifications: () -> Unit,
    onRepeatSelect: (RepeatRule) -> Unit,
    onPrioritySelect: (TaskPriority) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    subtaskRows: List<SubtaskRowUi>,
    subtaskActions: SubtaskActions
) {
    val date = draft.dateFor(today)
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        val category = categories.firstOrNull { it.id == draft.categoryId }
        TaskChips(
            group = taskGroupOf(date, isDone, today),
            category = category
        )
        TitleRow(
            title = draft.title,
            isDone = isDone,
            categoryColorIndex = category?.colorIndex,
            onToggleDone = onToggleDone,
            onTitleChange = onTitleChange,
            onTitleEditEnd = onTitleEditEnd,
            modifier = Modifier.padding(top = 8.dp)
        )
        PlainTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            label = stringResource(R.string.add_task_note_label),
            hint = stringResource(R.string.add_task_note_hint),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            imeAction = ImeAction.Done,
            maxLines = 8,
            // Под текстом названия, правее кружка отметки
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
        )
        PropertiesCard(
            draft = draft,
            date = date,
            today = today,
            categories = categories,
            onDateClick = onDateClick,
            onQuickDateSelect = onQuickDateSelect,
            onReminderSelect = onReminderSelect,
            notificationsAllowed = notificationsAllowed,
            onEnableNotifications = onEnableNotifications,
            onRepeatSelect = onRepeatSelect,
            onPrioritySelect = onPrioritySelect,
            onCategorySelect = onCategorySelect,
            modifier = Modifier.padding(top = 16.dp)
        )
        SubtasksCard(subtasks, subtaskRows, subtaskActions, Modifier.padding(top = 16.dp))
        Spacer(Modifier.height(24.dp))
    }
    BottomButtons(
        isDone = isDone,
        date = date,
        today = today,
        onToggleDone = onToggleDone,
        onPostpone = onPostpone,
        onPostponePickDate = onPostponePickDate
    )
}

/** Верхняя панель: «Назад» и «Удалить». Кнопка «Ещё» из макета появится, когда будет что в ней показать. */
@Composable
private fun TaskTopBar(onBack: () -> Unit, onDelete: () -> Unit, deleteEnabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
        Spacer(Modifier.weight(1f))
        if (deleteEnabled) {
            SquareIconButton(ChekkiIcons.Delete, stringResource(R.string.task_delete), onDelete)
        }
    }
}

/** Плашки над названием: группа, в которой задача сейчас, и категория. Только для вида, не нажимаются. */
@Composable
private fun TaskChips(group: TaskGroup, category: CategoryOption?) {
    val colors = ChekkiTheme.colors
    val groupColor = when (group) {
        // У «Сегодня» и «Просроченных» нет пары «база — оттенок», берём цвета их ярких карточек
        TaskGroup.Today -> GroupColor(colors.today.gradientStart, colors.today.contentSecondary)
        TaskGroup.Overdue -> GroupColor(colors.overdue.gradientStart, colors.overdue.contentSecondary)
        TaskGroup.NoDate -> colors.noDate
        TaskGroup.Tomorrow -> colors.tomorrow
        TaskGroup.ThisWeek -> colors.thisWeek
        TaskGroup.ThisMonth -> colors.thisMonth
        TaskGroup.Later -> colors.later
        TaskGroup.Done -> colors.done
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(background = groupColor.base.copy(alpha = 0.22f)) {
            Icon(group.icon, contentDescription = null, Modifier.size(14.dp), tint = groupColor.tint)
            Text(stringResource(group.title), style = MaterialTheme.typography.labelLarge, color = groupColor.tint)
        }
        if (category != null) {
            val palette = colors.categories
            Chip(background = MaterialTheme.colorScheme.surfaceContainer) {
                CategoryMark(category.icon, palette[category.colorIndex % palette.size], size = 14.dp)
                Text(category.name, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Chip(background: Color, content: @Composable () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(background)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/** Кружок отметки и название, которое правится прямо на месте. У выполненной задачи название зачёркнуто. */
@Composable
private fun TitleRow(
    title: String,
    isDone: Boolean,
    categoryColorIndex: Int?,
    onToggleDone: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTitleEditEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        TaskCheckbox(
            done = isDone,
            title = title,
            ringColor = checkboxColor(categoryColorIndex),
            onToggleDone = onToggleDone
        )
        PlainTextField(
            value = title,
            onValueChange = onTitleChange,
            label = stringResource(R.string.add_task_title_label),
            hint = stringResource(R.string.add_task_title_hint),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = null,
                color = if (isDone) ChekkiTheme.colors.taskDoneTitle else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isDone) TextDecoration.LineThrough else null
            ),
            imeAction = ImeAction.Done,
            maxLines = 4,
            // «Готово» заканчивает правку: стёртое целиком название сразу вернётся
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            // Первая строка по центру кружка: (48 − 28) / 2
            modifier = Modifier
                .padding(start = 4.dp, top = 10.dp)
                .onFocusChanged { state -> if (!state.isFocused) onTitleEditEnd() }
        )
    }
}

/** Свойства задачи одним блоком, как в макете: дата и время, напоминание, повтор, приоритет, категория. */
@Composable
private fun PropertiesCard(
    draft: TaskDraft,
    date: LocalDate?,
    today: LocalDate,
    categories: List<CategoryOption>,
    onDateClick: () -> Unit,
    onQuickDateSelect: (QuickDate) -> Unit,
    onReminderSelect: (Int?) -> Unit,
    notificationsAllowed: Boolean,
    onEnableNotifications: () -> Unit,
    onRepeatSelect: (RepeatRule) -> Unit,
    onPrioritySelect: (TaskPriority) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDay = draft.time == null
    CardBlock(modifier) {
        DateMenuRow(date, draft.time, today, onQuickDateSelect, onPickDate = onDateClick)
        RowDivider()
        OptionsRow(
            icon = ChekkiIcons.Reminder,
            label = stringResource(R.string.add_task_reminder),
            value = reminderLabel(draft.reminderMinutesBefore, allDay),
            options = reminderOptions(allDay),
            selected = draft.reminderMinutesBefore,
            optionLabel = { reminderLabel(it, allDay) },
            onSelect = onReminderSelect,
            placement = FieldRowPlacement.Card
        )
        if (draft.reminderMinutesBefore != null && !notificationsAllowed) {
            NotificationsOffHint(onEnable = onEnableNotifications, modifier = Modifier.padding(start = 16.dp, end = 4.dp))
        }
        RowDivider()
        val repeatPreset = RepeatPreset.of(draft.repeat)
        OptionsRow(
            icon = ChekkiIcons.Repeat,
            label = stringResource(R.string.add_task_repeat),
            value = repeatPreset?.let { repeatLabel(it) } ?: stringResource(R.string.repeat_custom),
            options = RepeatPreset.entries,
            selected = repeatPreset,
            optionLabel = { repeatLabel(it) },
            onSelect = { onRepeatSelect(it.rule) },
            placement = FieldRowPlacement.Card
        )
        RowDivider()
        OptionsRow(
            icon = if (draft.priority == TaskPriority.None) ChekkiIcons.PriorityFlagOutline else ChekkiIcons.PriorityFlag,
            iconTint = priorityColor(draft.priority),
            label = stringResource(R.string.add_task_priority),
            value = stringResource(draft.priority.shortLabel),
            options = TaskPriority.entries,
            selected = draft.priority,
            optionLabel = { stringResource(it.shortLabel) },
            onSelect = onPrioritySelect,
            placement = FieldRowPlacement.Card
        )
        RowDivider()
        CategoryRow(
            categories = categories,
            selectedId = draft.categoryId,
            onSelect = onCategorySelect,
            placement = FieldRowPlacement.Card
        )
    }
}

/**
 * «Дата и время»: в карточке нет плашек дат, как в панели, поэтому быстрые даты и «Без даты» — в меню,
 * а «Выбрать дату…» открывает календарь и затем циферблат.
 */
@Composable
private fun DateMenuRow(
    date: LocalDate?,
    time: LocalTime?,
    today: LocalDate,
    onQuickDateSelect: (QuickDate) -> Unit,
    onPickDate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = if (date == null) QuickDate.NoDate else QuickDate.matching(date, today)
    Box {
        DateTimeRow(date, time, today, onClick = { expanded = true }, placement = FieldRowPlacement.Card)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            QuickDate.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onQuickDateSelect(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option == selected) {
                            Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.task_pick_date), style = MaterialTheme.typography.bodyLarge) },
                onClick = {
                    expanded = false
                    onPickDate()
                },
                leadingIcon = {
                    Icon(ChekkiIcons.Calendar, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }
}

/** Что можно сделать с подзадачами в карточке. */
private class SubtaskActions(
    val onToggle: (SubtaskRowUi) -> Unit,
    val onEdit: (SubtaskRowUi) -> Unit,
    val onTextChange: (String) -> Unit,
    val onEditEnd: (key: Long) -> Unit,
    val onDelete: (key: Long) -> Unit,
    val onAdd: () -> Unit
)

/**
 * Подзадачи: счётчик и полоса прогресса (по сохранённым [saved]), строки с квадратными отметками
 * и «Добавить подзадачу». Блок виден всегда, даже без подзадач, — чтобы было где добавить первую.
 */
@Composable
private fun SubtasksCard(
    saved: List<SubtaskUi>,
    rows: List<SubtaskRowUi>,
    actions: SubtaskActions,
    modifier: Modifier = Modifier
) {
    val done = saved.count { it.done }
    val progressDescription = stringResource(R.string.task_subtasks_progress, done, saved.size)
    val focusManager = LocalFocusManager.current
    CardBlock(modifier) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .semantics(mergeDescendants = true) {
                        if (saved.isNotEmpty()) contentDescription = progressDescription
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.task_subtasks),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall
                )
                if (saved.isNotEmpty()) {
                    Text(
                        text = "$done/${saved.size}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (saved.isNotEmpty()) {
                CardProgressBar(
                    done = done,
                    total = saved.size,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    color = ChekkiTheme.colors.taskDone,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }
        }
        // Зона нажатия отметки 48 dp, а сам квадрат — на одной линии с заголовком (16 dp от края)
        Column(Modifier.padding(start = 2.dp, end = 4.dp, bottom = 4.dp)) {
            rows.forEachIndexed { index, row ->
                key(row.key) {
                    SubtaskRow(
                        row = row,
                        actions = actions,
                        onNext = {
                            when {
                                row.title.isBlank() -> focusManager.clearFocus()
                                index == rows.lastIndex -> actions.onAdd()
                                else -> actions.onEdit(rows[index + 1])
                            }
                        }
                    )
                }
            }
            AddSubtaskButton(onClick = actions.onAdd, contentPadding = PaddingValues(start = 14.dp))
        }
    }
}

/**
 * Строка подзадачи. Нажатие на квадрат отмечает подзадачу, на название — начинает правку:
 * название становится полем ввода, справа появляется крестик удаления.
 */
@Composable
private fun SubtaskRow(row: SubtaskRowUi, actions: SubtaskActions, onNext: () -> Unit) {
    val colors = ChekkiTheme.colors
    val toggleDescription = stringResource(
        if (row.done) R.string.task_mark_undone else R.string.task_mark_done,
        row.title
    )
    // Правящаяся строка целиком видна над клавиатурой и кнопкой внизу, а не только строка с курсором.
    // Прокручиваем, пока выезжает клавиатура: каждый новый размер отменяет прошлую прокрутку
    val bringIntoView = remember { BringIntoViewRequester() }
    if (row.isEditing) {
        val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
        LaunchedEffect(imeBottom) { bringIntoView.bringIntoView() }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .bringIntoViewRequester(bringIntoView),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .toggleable(value = row.done, role = Role.Checkbox, onValueChange = { actions.onToggle(row) })
                .semantics { contentDescription = toggleDescription },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(SubtaskBoxShape)
                    .then(
                        if (row.done) Modifier.background(colors.taskDone)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, SubtaskBoxShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (row.done) {
                    Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(13.dp), tint = colors.onTaskDone)
                }
            }
        }
        if (row.isEditing) {
            SubtaskTitleField(row, actions, onNext, Modifier.weight(1f))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = { actions.onDelete(row.key) }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ChekkiIcons.Close,
                    contentDescription = stringResource(R.string.add_task_remove_subtask),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            Text(
                text = row.title,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClickLabel = stringResource(R.string.task_edit_subtask), role = Role.Button) {
                        actions.onEdit(row)
                    }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (row.done) colors.taskDoneTitle else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (row.done) TextDecoration.LineThrough else null
            )
        }
    }
}

/** Поле названия подзадачи: курсор ставится сразу, уход курсора заканчивает правку. */
@Composable
private fun SubtaskTitleField(row: SubtaskRowUi, actions: SubtaskActions, onNext: () -> Unit, modifier: Modifier) {
    val focusRequester = remember { FocusRequester() }
    // Поле появляется уже без фокуса: заканчиваем правку, только если фокус у него был и ушёл
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Курсор в конце названия: правку чаще всего начинают, чтобы дописать
    var field by remember { mutableStateOf(TextFieldValue(row.title, TextRange(row.title.length))) }
    PlainTextField(
        value = field,
        onValueChange = { changed ->
            field = changed
            if (changed.text != row.title) actions.onTextChange(changed.text)
        },
        label = stringResource(R.string.add_task_subtask_label),
        hint = stringResource(R.string.add_task_subtask_hint),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        imeAction = ImeAction.Next,
        maxLines = 1,
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = modifier
            .padding(horizontal = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) hadFocus = true
                else if (hadFocus) actions.onEditEnd(row.key)
            }
    )
}

// Квадрат с небольшим скруглением, как в панели новой задачи
private val SubtaskBoxShape = RoundedCornerShape(6.dp)

/** Скруглённая подложка для группы строк. */
@Composable
private fun CardBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * Кнопки внизу, как в макете: «Отложить» и «Выполнено» поровну. У выполненной задачи —
 * одна «Вернуть»: откладывать выполненное незачем.
 */
@Composable
private fun BottomButtons(
    isDone: Boolean,
    date: LocalDate?,
    today: LocalDate,
    onToggleDone: () -> Unit,
    onPostpone: (LocalDate) -> Unit,
    onPostponePickDate: () -> Unit
) {
    val colors = ChekkiTheme.colors
    var postponeMenuOpen by remember { mutableStateOf(false) }
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isDone) {
                Box(Modifier.weight(1f)) {
                    BottomButton(
                        icon = ChekkiIcons.Postpone,
                        text = stringResource(R.string.task_postpone),
                        background = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { postponeMenuOpen = true }
                    )
                    PostponeMenu(
                        expanded = postponeMenuOpen,
                        onDismiss = { postponeMenuOpen = false },
                        today = today,
                        currentDate = date,
                        onSelect = onPostpone,
                        onPickDate = onPostponePickDate
                    )
                }
            }
            BottomButton(
                icon = if (isDone) null else ChekkiIcons.Check,
                text = stringResource(if (isDone) R.string.task_return else R.string.task_complete),
                background = if (isDone) MaterialTheme.colorScheme.surfaceContainerHigh else colors.taskDone,
                contentColor = if (isDone) MaterialTheme.colorScheme.onSurface else colors.onTaskDone,
                onClick = onToggleDone,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomButton(
    icon: ImageVector?,
    text: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = contentColor)
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = contentColor)
    }
}

private val TaskGroup.title: Int
    get() = when (this) {
        TaskGroup.NoDate -> R.string.group_no_date
        TaskGroup.Overdue -> R.string.group_overdue
        TaskGroup.Today -> R.string.group_today
        TaskGroup.Done -> R.string.group_done
        TaskGroup.Tomorrow -> R.string.group_tomorrow
        TaskGroup.ThisWeek -> R.string.group_this_week
        TaskGroup.ThisMonth -> R.string.group_this_month
        TaskGroup.Later -> R.string.group_later
    }

private val TaskGroup.icon: ImageVector
    get() = when (this) {
        TaskGroup.NoDate -> ChekkiIcons.NoDate
        TaskGroup.Overdue -> ChekkiIcons.Overdue
        TaskGroup.Today -> ChekkiIcons.Today
        TaskGroup.Done -> ChekkiIcons.Done
        TaskGroup.Tomorrow -> ChekkiIcons.Tomorrow
        TaskGroup.ThisWeek -> ChekkiIcons.ThisWeek
        TaskGroup.ThisMonth -> ChekkiIcons.ThisMonth
        TaskGroup.Later -> ChekkiIcons.Later
    }

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TaskContentPreview() {
    ChekkiTheme {
        TaskContent(
            draft = TaskDraft(
                title = "Отправить отчёт за квартал",
                note = "Свести продажи из таблицы, приложить графики и отправить Ольге до конца дня.",
                quickDate = null,
                exactDate = LocalDate.of(2026, 10, 22),
                time = LocalTime.of(14, 0),
                categoryId = 1,
                priority = TaskPriority.Medium,
                repeat = RepeatPreset.Quarterly.rule,
                reminderMinutesBefore = 30
            ),
            isDone = false,
            subtasks = previewSubtasks,
            subtaskRows = subtaskRows(
                previewSubtasks,
                editing = SubtaskEditing(key = 2, text = "Построить графики", isNew = false, originalTitle = "Построить графики"),
                newKeys = emptyMap()
            ),
            subtaskActions = SubtaskActions({}, {}, {}, {}, {}, {}),
            categories = listOf(CategoryOption(1, "Работа", 0)),
            today = LocalDate.of(2026, 10, 22),
            onBack = {},
            onDelete = {},
            onPostpone = {},
            onToggleDone = {},
            onTitleChange = {},
            onTitleEditEnd = {},
            onNoteChange = {},
            onQuickDateSelect = {},
            onDatePick = {},
            onTimePick = {},
            onReminderSelect = {},
            notificationsAllowed = true,
            onEnableNotifications = {},
            onRepeatSelect = {},
            onPrioritySelect = {},
            onCategorySelect = {}
        )
    }
}

private val previewSubtasks = listOf(
    SubtaskUi(1, "Выгрузить данные о продажах", done = true),
    SubtaskUi(2, "Построить графики", done = false),
    SubtaskUi(3, "Написать короткий вывод", done = false)
)
