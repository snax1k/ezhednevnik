package com.snax1k.chekki.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DeletedCategory
import com.snax1k.chekki.data.db.CategoryIcon
import com.snax1k.chekki.ui.addtask.DragHandle
import com.snax1k.chekki.ui.addtask.PlainTextField
import com.snax1k.chekki.ui.addtask.SaveButton
import com.snax1k.chekki.ui.addtask.sheetContentMaxHeight
import com.snax1k.chekki.ui.components.ColorPickerDialog
import com.snax1k.chekki.ui.components.colorToHex
import com.snax1k.chekki.ui.habit.SectionLabel
import com.snax1k.chekki.ui.habit.SheetHeader
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlinx.coroutines.launch

/**
 * Нижняя панель категории: новая или изменение. Макета нет, сделана как панель привычки:
 * название, 16 иконок, 12 цветов. Удалённую категорию можно вернуть с плашки «Отменить».
 */
// ModalBottomSheet в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(viewModel: CategoryEditorViewModel, onDismiss: () -> Unit, onDeleted: (DeletedCategory) -> Unit) {
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
            CategoryContent(
                draft = viewModel.draft,
                isNew = viewModel.editingId == null,
                isArchived = viewModel.isArchived,
                onNameChange = viewModel::onNameChange,
                onIconSelect = viewModel::onIconSelect,
                onColorSelect = viewModel::onColorSelect,
                onClose = hideAndDismiss,
                onSave = { viewModel.save(onSaved = hideAndDismiss) },
                onArchiveToggle = { viewModel.toggleArchived(onDone = hideAndDismiss) },
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
private fun CategoryContent(
    draft: CategoryDraft,
    isNew: Boolean,
    isArchived: Boolean,
    onNameChange: (String) -> Unit,
    onIconSelect: (CategoryIcon) -> Unit,
    onColorSelect: (Int) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    autoFocusName: Boolean = true
) {
    val color = categoryColor(draft.colorIndex)
    val nameFocus = remember { FocusRequester() }
    if (isNew && autoFocusName) {
        // У новой категории клавиатура появляется сразу, можно печатать название
        LaunchedEffect(Unit) { nameFocus.requestFocus() }
    }
    // Не выше 85% окна, как у других панелей: иначе ModalBottomSheet может сам спрятаться
    Column(Modifier.heightIn(max = sheetContentMaxHeight())) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SheetHeader(stringResource(if (isNew) R.string.category_new else R.string.category_edit), onClose)
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Так иконка будет выглядеть на плитке категории
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(draft.icon.vector, contentDescription = null, Modifier.size(26.dp), tint = color)
                }
                PlainTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.category_name_label),
                    hint = stringResource(R.string.category_name_hint),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = null,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    imeAction = ImeAction.Done,
                    maxLines = 2,
                    modifier = Modifier.focusRequester(nameFocus)
                )
            }
            SectionLabel(stringResource(R.string.habit_icon))
            IconPicker(selected = draft.icon, color = color, onSelect = onIconSelect)
            SectionLabel(stringResource(R.string.habit_color))
            ColorPicker(selected = draft.colorIndex, onSelect = onColorSelect)
            if (!isNew) {
                TextButton(onClick = onArchiveToggle, enabled = draft.canSave, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(if (isArchived) R.string.category_unarchive else R.string.category_archive),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (!isArchived) {
                    Text(
                        text = stringResource(R.string.category_archived_hint),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.category_delete),
                        style = MaterialTheme.typography.titleSmall,
                        color = ChekkiTheme.colors.priorityHigh
                    )
                }
                Text(
                    text = stringResource(R.string.category_delete_hint),
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
            onClick = onSave,
            text = stringResource(if (isNew) R.string.category_add else R.string.habit_save),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        )
    }
}

/** Сетка иконок по шесть в ряд: выбранная — на подложке цвета категории. */
@Composable
private fun IconPicker(selected: CategoryIcon, color: Color, onSelect: (CategoryIcon) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryIcon.entries.chunked(ICONS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { icon ->
                    val isSelected = icon == selected
                    val description = stringResource(icon.label)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isSelected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .then(if (isSelected) Modifier.border(2.dp, color, MaterialTheme.shapes.small) else Modifier)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(icon) })
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon.vector,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Неполный последний ряд: пустые места, чтобы иконки стояли под иконками
                repeat(ICONS_PER_ROW - row.size) { Spacer(Modifier.size(48.dp)) }
            }
        }
    }
}

/**
 * 12 цветов в два ряда по шесть: выбранный обведён и с галочкой, ячейки делят ширину поровну.
 * Под ними «Свой цвет» — круг цветов, HEX и R, G, B ([ColorPickerDialog]).
 */
@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    val palette = ChekkiTheme.colors.categories
    var isCustomOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        palette.indices.chunked(COLORS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { index ->
                    val isSelected = index == selected
                    val description = stringResource(CategoryColorLabels[index % CategoryColorLabels.size])
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .padding(if (isSelected) 4.dp else 0.dp)
                                .clip(CircleShape)
                                .background(palette[index]),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(14.dp), tint = ChekkiTheme.colors.onFab)
                            }
                        }
                    }
                }
            }
        }
        CustomColorRow(selected, onClick = { isCustomOpen = true })
    }
    if (isCustomOpen) {
        ColorPickerDialog(
            initial = categoryColor(selected),
            onDismiss = { isCustomOpen = false },
            onPick = { color ->
                isCustomOpen = false
                onSelect(customCategoryColorCode(color))
            }
        )
    }
}

/** «Свой цвет»: радужный круг, а если свой цвет уже выбран — он сам с галочкой и подписью «#7CC2FF». */
@Composable
private fun CustomColorRow(selected: Int, onClick: () -> Unit) {
    val isCustom = isCustomCategoryColor(selected)
    val title = stringResource(R.string.category_color_custom)
    val description = if (isCustom) {
        stringResource(R.string.category_color_custom_value, colorToHex(Color(selected)))
    } else {
        title
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .heightIn(min = 52.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .selectable(selected = isCustom, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .then(if (isCustom) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                .padding(if (isCustom) 4.dp else 0.dp)
                .clip(CircleShape)
                .background(
                    if (isCustom) {
                        SolidColor(Color(selected))
                    } else {
                        Brush.sweepGradient(
                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCustom) {
                Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(14.dp), tint = ChekkiTheme.colors.onFab)
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        if (isCustom) {
            Text(
                colorToHex(Color(selected)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val ICONS_PER_ROW = 6
private const val COLORS_PER_ROW = 6

@Preview(widthDp = 412, heightDp = 760)
@Composable
private fun CategoryContentPreview() {
    ChekkiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CategoryContent(
                draft = CategoryDraft(name = "Поездки", icon = CategoryIcon.Travel, colorIndex = 8),
                isNew = false,
                isArchived = false,
                onNameChange = {},
                onIconSelect = {},
                onColorSelect = {},
                onClose = {},
                onSave = {},
                onArchiveToggle = {},
                onDelete = {},
                autoFocusName = false
            )
        }
    }
}
