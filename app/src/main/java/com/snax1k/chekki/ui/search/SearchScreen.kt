package com.snax1k.chekki.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DateFilter
import com.snax1k.chekki.ui.addtask.CategoryMark
import com.snax1k.chekki.ui.addtask.CategoryOption
import com.snax1k.chekki.ui.addtask.PeriodPickerDialog
import com.snax1k.chekki.ui.components.formatDateRange
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.main.sampleTodayTasks
import com.snax1k.chekki.ui.tasks.SwipeableTaskRow
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.theme.ChekkiTheme
import java.time.LocalDate

/**
 * Поиск по макету «Поиск» (стиль А, переведён на Б): поле с «Назад» и крестиком, плашки-фильтры,
 * «Найдено: N задач» и результаты по группам с цветными подзаголовками. Строки — как в группах.
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    query: String,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onFiltersChange: (SearchFilters) -> Unit = {},
    /** «Найти» на клавиатуре: запрос запоминается в последних. */
    onSearch: () -> Unit = {},
    onRecentClick: (String) -> Unit = {},
    onRecentRemove: (String) -> Unit = {},
    onRecentClear: () -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDone: (TaskUi) -> Unit = {},
    onTaskClick: (TaskUi) -> Unit = {},
    onPostpone: (TaskUi, LocalDate) -> Unit = { _, _ -> },
    onDelete: (TaskUi) -> Unit = {},
    onShowMore: () -> Unit = {},
    autoFocus: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    // Клавиатура — сразу при открытии, но не после возврата из карточки задачи
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    if (autoFocus) {
        LaunchedEffect(Unit) {
            if (!focusedOnce) {
                focusRequester.requestFocus()
                focusedOnce = true
            }
        }
    }
    var openTaskId by remember { mutableStateOf<Long?>(null) }
    val bottomInset = maxOf(
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    )
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "field") {
                SearchField(query, onQueryChange, onSearch, onBack, Modifier.focusRequester(focusRequester))
            }
            item(key = "filters") {
                FilterChips(state.filters, state.categories, state.today, onFiltersChange, Modifier.padding(top = 8.dp))
            }
            when {
                state.isLoading -> Unit
                state.isIdle && state.recent.isNotEmpty() -> {
                    item(key = "recent-header") { RecentHeader(onRecentClear) }
                    items(state.recent, key = { "recent-$it" }) { recent ->
                        RecentRow(recent, onClick = { onRecentClick(recent) }, onRemove = { onRecentRemove(recent) })
                    }
                }
                state.isIdle -> item(key = "hint") { CenterText(stringResource(R.string.search_hint)) }
                state.total == 0 -> item(key = "empty") { CenterText(stringResource(R.string.search_nothing)) }
                else -> {
                    item(key = "found") {
                        Text(
                            text = pluralStringResource(R.plurals.search_found, state.total, state.total),
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    for (section in state.sections) {
                        item(key = "group-${section.group}", contentType = "group") {
                            Text(
                                text = stringResource(section.group.searchTitle),
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 10.dp)
                                    .semantics { heading() },
                                style = MaterialTheme.typography.titleSmall,
                                color = groupHeaderColor(section.group)
                            )
                        }
                        items(section.tasks, key = { it.id }, contentType = { "task" }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                today = state.today,
                                isOpen = openTaskId == task.id,
                                onOpen = { openTaskId = task.id },
                                onClose = { if (openTaskId == task.id) openTaskId = null },
                                onToggleDone = { onToggleDone(task) },
                                onClick = { onTaskClick(task) },
                                onPostpone = { date -> onPostpone(task, date) },
                                onDelete = { onDelete(task) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    if (state.canShowMore) {
                        item(key = "more") {
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable(role = Role.Button, onClick = onShowMore),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.list_show_more),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Поле поиска в «пилюле» 56 dp: «Назад», текст, крестик (виден, когда есть текст). */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val label = stringResource(R.string.search_placeholder)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = ChekkiTheme.colors.textPlaceholder)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch()
                    focusManager.clearFocus()
                })
            )
        }
        if (query.isNotEmpty()) {
            RoundIconButton(ChekkiIcons.Close, stringResource(R.string.search_clear), onClick = { onQueryChange("") })
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Плашки-фильтры: «Невыполненные», «Категория ▾», «Любая дата ▾», «С напоминанием», «Высокий приоритет». */
@Composable
private fun FilterChips(
    filters: SearchFilters,
    categories: List<CategoryOption>,
    today: LocalDate,
    onChange: (SearchFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(stringResource(R.string.search_filter_open), filters.openOnly) {
            onChange(filters.copy(openOnly = !filters.openOnly))
        }
        CategoryChip(filters.category, categories) { onChange(filters.copy(category = it)) }
        DateChip(filters.date, today) { onChange(filters.copy(date = it)) }
        FilterChip(stringResource(R.string.search_filter_reminder), filters.withReminder) {
            onChange(filters.copy(withReminder = !filters.withReminder))
        }
        FilterChip(stringResource(R.string.search_filter_high), filters.highPriority) {
            onChange(filters.copy(highPriority = !filters.highPriority))
        }
    }
}

/** Плашка-переключатель: включённая — на подложке акцента с галочкой, выключенная — в рамке. */
@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .height(48.dp)
            .padding(vertical = 6.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .then(
                if (selected) Modifier.background(accent.copy(alpha = 0.18f))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
            )
            .clickable(role = if (trailing == null) Role.Checkbox else Role.DropdownList, onClick = onClick)
            .semantics { if (trailing == null) contentDescription = label }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected && leading == null && trailing == null) {
            Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = accent)
        }
        leading?.invoke()
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
        )
        trailing?.invoke()
    }
}

/** «Категория ▾» — меню: любая, без категории и категории не из архива. */
@Composable
private fun CategoryChip(selected: CategoryFilter, categories: List<CategoryOption>, onSelect: (CategoryFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val chosen = (selected as? CategoryFilter.Of)?.let { filter -> categories.firstOrNull { it.id == filter.id } }
    val palette = ChekkiTheme.colors.categories
    val label = when {
        chosen != null -> chosen.name
        selected == CategoryFilter.None -> stringResource(R.string.add_task_no_category)
        else -> stringResource(R.string.search_filter_category)
    }
    Box {
        FilterChip(
            label = label,
            selected = selected != CategoryFilter.Any,
            leading = chosen?.let { { CategoryMark(it.icon, palette[it.colorIndex % palette.size], size = 16.dp) } },
            trailing = {
                Icon(
                    ChekkiIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            CategoryMenuItem(stringResource(R.string.search_filter_any_category), selected == CategoryFilter.Any) {
                expanded = false
                onSelect(CategoryFilter.Any)
            }
            CategoryMenuItem(stringResource(R.string.add_task_no_category), selected == CategoryFilter.None) {
                expanded = false
                onSelect(CategoryFilter.None)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            categories.forEach { category ->
                CategoryMenuItem(
                    name = category.name,
                    isSelected = chosen?.id == category.id,
                    leading = { CategoryMark(category.icon, palette[category.colorIndex % palette.size]) }
                ) {
                    expanded = false
                    onSelect(CategoryFilter.Of(category.id))
                }
            }
        }
    }
}

/** «Любая дата ▾» — меню готовых вариантов и «Выбрать период…» (календарь с первым и последним днём). */
@Composable
private fun DateChip(selected: DateFilter, today: LocalDate, onSelect: (DateFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var pickingPeriod by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            label = dateFilterLabel(selected),
            selected = selected != DateFilter.Any,
            trailing = {
                Icon(
                    ChekkiIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            listOf(
                DateFilter.Any,
                DateFilter.Overdue,
                DateFilter.Today,
                DateFilter.ThisWeek,
                DateFilter.ThisMonth,
                DateFilter.NoDate
            ).forEach { option ->
                CategoryMenuItem(dateFilterLabel(option), selected == option) {
                    expanded = false
                    onSelect(option)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            CategoryMenuItem(
                name = stringResource(R.string.search_date_period),
                isSelected = selected is DateFilter.Period,
                leading = {
                    Icon(ChekkiIcons.Calendar, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            ) {
                expanded = false
                pickingPeriod = true
            }
        }
    }
    if (pickingPeriod) {
        val current = selected as? DateFilter.Period
        PeriodPickerDialog(
            initialFrom = current?.from ?: today,
            initialTo = current?.to,
            onDismiss = { pickingPeriod = false },
            onDone = { from, to ->
                pickingPeriod = false
                onSelect(DateFilter.Period(from, to))
            }
        )
    }
}

@Composable
private fun dateFilterLabel(filter: DateFilter): String = when (filter) {
    DateFilter.Any -> stringResource(R.string.search_date_any)
    DateFilter.Overdue -> stringResource(R.string.group_overdue)
    DateFilter.Today -> stringResource(R.string.group_today)
    DateFilter.ThisWeek -> stringResource(R.string.search_date_week)
    DateFilter.ThisMonth -> stringResource(R.string.group_this_month)
    DateFilter.NoDate -> stringResource(R.string.group_no_date)
    is DateFilter.Period -> formatDateRange(filter.from..filter.to)
}

/** «Недавние» и «Очистить» над последними запросами. */
@Composable
private fun RecentHeader(onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.search_recent),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.search_recent_clear), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Последний запрос: нажатие ищет его снова, крестик убирает из списка. */
@Composable
private fun RecentRow(query: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Clock, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = query,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        RoundIconButton(ChekkiIcons.Close, stringResource(R.string.search_recent_remove, query), onRemove)
    }
}

@Composable
private fun CategoryMenuItem(
    name: String,
    isSelected: Boolean,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        leadingIcon = leading ?: { Spacer(Modifier.width(18.dp)) },
        trailingIcon = {
            if (isSelected) Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    )
}

@Composable
private fun CenterText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/** Цвет подзаголовка группы: оттенок цвета группы, у «Сегодня» и «Просроченных» — зелёный и красный. */
@Composable
private fun groupHeaderColor(group: TaskGroup): Color = with(ChekkiTheme.colors) {
    when (group) {
        TaskGroup.Overdue -> priorityHigh
        TaskGroup.Today -> taskDone
        TaskGroup.NoDate -> noDate.tint
        TaskGroup.Tomorrow -> tomorrow.tint
        TaskGroup.ThisWeek -> thisWeek.tint
        TaskGroup.ThisMonth -> thisMonth.tint
        TaskGroup.Later -> later.tint
        TaskGroup.Done -> done.tint
    }
}

private val TaskGroup.searchTitle: Int
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

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SearchScreenPreview() {
    val tasks = sampleTodayTasks().take(3).mapIndexed { index, task ->
        if (index == 0) task.copy(titleHighlight = 9 until 14) else task.copy(matchSnippet = "Взять отчёт с собой", showDate = true)
    }
    ChekkiTheme {
        SearchScreen(
            state = SearchUiState(
                today = LocalDate.of(2026, 10, 24),
                filters = SearchFilters(),
                sections = listOf(SearchSection(TaskGroup.Today, tasks.take(1)), SearchSection(TaskGroup.ThisWeek, tasks.drop(1))),
                total = 3,
                canShowMore = false,
                categories = emptyList(),
                isIdle = false
            ),
            query = "отчет",
            autoFocus = false
        )
    }
}
