package com.snax1k.chekki.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DeletedHabit
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.ui.addtask.DragHandle
import com.snax1k.chekki.ui.addtask.PlainTextField
import com.snax1k.chekki.ui.addtask.SaveButton
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.TrackerColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Нижняя панель привычки: новая или изменение. Макета нет, сделана в стиле панели новой задачи.
 * После сохранения и удаления панель закрывается; вернуть удалённую привычку можно с плашки «Отменить».
 */
// ModalBottomSheet в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitSheet(viewModel: HabitEditorViewModel, onDismiss: () -> Unit, onDeleted: (DeletedHabit) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Сначала доигрываем анимацию закрытия, потом убираем панель
    val hideAndDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { DragHandle() }
    ) {
        if (!viewModel.isLoading) {
            HabitContent(
                draft = viewModel.draft,
                isNew = viewModel.editingId == null,
                onTitleChange = viewModel::onTitleChange,
                onIconSelect = viewModel::onIconSelect,
                onColorSelect = viewModel::onColorSelect,
                onTargetChange = viewModel::onTargetChange,
                onDayToggle = viewModel::onDayToggle,
                onClose = hideAndDismiss,
                onSave = { viewModel.save(onSaved = hideAndDismiss) },
                onDelete = {
                    viewModel.delete { deleted ->
                        hideAndDismiss()
                        onDeleted(deleted)
                    }
                }
            )
        }
    }
}

@Composable
private fun HabitContent(
    draft: HabitDraft,
    isNew: Boolean,
    onTitleChange: (String) -> Unit,
    onIconSelect: (HabitIcon) -> Unit,
    onColorSelect: (Int) -> Unit,
    onTargetChange: (Int) -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    autoFocusTitle: Boolean = true
) {
    val palette = ChekkiTheme.colors.habits
    val colors = palette[draft.colorIndex.mod(palette.size)]
    val titleFocus = remember { FocusRequester() }
    if (isNew && autoFocusTitle) {
        // У новой привычки клавиатура появляется сразу, можно печатать название
        LaunchedEffect(Unit) { titleFocus.requestFocus() }
    }
    Column {
        // Форма прокручивается, если вместе с клавиатурой не помещается; кнопка всегда видна внизу
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SheetHeader(stringResource(if (isNew) R.string.habit_new else R.string.habit_edit), onClose)
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Так круг будет выглядеть наверху главного экрана
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.fill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(draft.icon.vector, contentDescription = null, Modifier.size(24.dp), tint = colors.content)
                }
                PlainTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    label = stringResource(R.string.habit_title_label),
                    hint = stringResource(R.string.habit_title_hint),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = null,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    imeAction = ImeAction.Done,
                    maxLines = 2,
                    modifier = Modifier.focusRequester(titleFocus)
                )
            }
            SectionLabel(stringResource(R.string.habit_icon))
            IconPicker(selected = draft.icon, colors = colors, onSelect = onIconSelect)
            SectionLabel(stringResource(R.string.habit_color))
            ColorPicker(selected = draft.colorIndex, palette = palette, onSelect = onColorSelect)
            SectionLabel(stringResource(R.string.habit_target))
            TargetStepper(target = draft.dailyTarget, onChange = onTargetChange)
            SectionLabel(stringResource(R.string.habit_days))
            DaysPicker(draft = draft, colors = colors, onToggle = onDayToggle)
            if (!isNew) {
                TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.habit_delete),
                        style = MaterialTheme.typography.titleSmall,
                        color = ChekkiTheme.colors.priorityHigh
                    )
                }
            }
            Box(Modifier.height(12.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SaveButton(
            enabled = draft.canSave,
            onClick = onSave,
            text = stringResource(if (isNew) R.string.habit_add else R.string.habit_save),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Сетка иконок: выбранная залита цветом привычки. */
@Composable
private fun IconPicker(selected: HabitIcon, colors: TrackerColors, onSelect: (HabitIcon) -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 6
    ) {
        HabitIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            val description = stringResource(icon.label)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.fill else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(icon) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon.vector,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSelected) colors.content else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Цвета кружками в одну строку: выбранный обведён и с галочкой. Ячейки делят ширину поровну —
 * на телефоне шириной 412 dp это около 46 dp, соседние ячейки примыкают, мёртвых зон между ними нет.
 */
@Composable
private fun ColorPicker(selected: Int, palette: List<TrackerColors>, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        palette.forEachIndexed { index, colors ->
            val isSelected = index == selected
            val description = stringResource(HabitColorLabels[index % HabitColorLabels.size])
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .padding(if (isSelected) 4.dp else 0.dp)
                        .clip(CircleShape)
                        .background(colors.fill),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(14.dp), tint = colors.content)
                    }
                }
            }
        }
    }
}

/** Цель на день: «−», число и «+», ниже — «7 раз в день». */
@Composable
private fun TargetStepper(target: Int, onChange: (Int) -> Unit) {
    val perDay = pluralStringResource(R.plurals.habit_times_per_day, target, target)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            icon = ChekkiIcons.Minus,
            description = stringResource(R.string.habit_target_decrease),
            enabled = target > HabitDraft.MIN_TARGET,
            onClick = { onChange(target - 1) }
        )
        Text(
            text = target.toString(),
            modifier = Modifier
                .width(56.dp)
                .semantics { stateDescription = perDay },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        StepperButton(
            icon = ChekkiIcons.Add,
            description = stringResource(R.string.habit_target_increase),
            enabled = target < HabitDraft.MAX_TARGET,
            onClick = { onChange(target + 1) }
        )
        Text(
            text = perDay,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

/** Дни недели кружками Пн…Вс, включённые залиты цветом привычки. Под ними — «Каждый день» и т. п. */
@Composable
private fun DaysPicker(draft: HabitDraft, colors: TrackerColors, onToggle: (DayOfWeek) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { day ->
                val isOn = draft.isOn(day)
                val fullName = day.getDisplayName(TextStyle.FULL_STANDALONE, RussianLocale)
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isOn) colors.fill else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggle(day) })
                        .semantics { contentDescription = fullName },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT_STANDALONE, RussianLocale)
                            .replaceFirstChar { it.titlecase(RussianLocale) },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isOn) colors.content else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val preset = HabitDaysPreset.of(draft.weekdays)
        if (preset != null) {
            Text(
                text = stringResource(
                    when (preset) {
                        HabitDaysPreset.EveryDay -> R.string.habit_days_every_day
                        HabitDaysPreset.Weekdays -> R.string.habit_days_weekdays
                        HabitDaysPreset.Weekend -> R.string.habit_days_weekend
                    }
                ),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val RussianLocale: Locale = Locale.forLanguageTag("ru")

@Preview(widthDp = 412, heightDp = 860)
@Composable
private fun HabitContentPreview() {
    ChekkiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            HabitContent(
                draft = HabitDraft(title = "Пить воду", icon = HabitIcon.Water, colorIndex = 1, dailyTarget = 7),
                isNew = false,
                onTitleChange = {},
                onIconSelect = {},
                onColorSelect = {},
                onTargetChange = {},
                onDayToggle = {},
                onClose = {},
                onSave = {},
                onDelete = {},
                autoFocusTitle = false
            )
        }
    }
}
