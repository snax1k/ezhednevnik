package com.snax1k.chekki.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.DayMarks
import com.snax1k.chekki.data.monthGrid
import com.snax1k.chekki.ui.category.categoryColor
import com.snax1k.chekki.ui.components.BottomFade
import com.snax1k.chekki.ui.components.FloatingButtonsAreaHeight
import com.snax1k.chekki.ui.components.FloatingButtonsBottomPadding
import com.snax1k.chekki.ui.components.FloatingTaskButtons
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.components.formatDayMonth
import com.snax1k.chekki.ui.components.formatTime
import com.snax1k.chekki.ui.habit.shortDayName
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.main.sampleTodayTasks
import com.snax1k.chekki.ui.tasks.SwipeableTaskRow
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Календарь по макету «Календарь» (стиль А, переведён на Б): шапка с месяцем и стрелками, сетка месяца
 * с точками (листается свайпом), под ней задачи выбранного дня со свайпами, «+» — задача на выбранный день.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onShowMonth: (YearMonth) -> Unit = {},
    onSelectDate: (LocalDate) -> Unit = {},
    onToday: () -> Unit = {},
    onToggleDone: (TaskUi) -> Unit = {},
    onTaskClick: (TaskUi) -> Unit = {},
    onPostpone: (TaskUi, LocalDate) -> Unit = { _, _ -> },
    onDelete: (TaskUi) -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    // Страницы листалки — месяцы вокруг месяца, открытого при первом показе экрана
    val baseMonth = remember { state.month }
    fun pageOf(month: YearMonth) = CENTER_PAGE + ChronoUnit.MONTHS.between(baseMonth, month).toInt()
    fun monthOf(page: Int): YearMonth = baseMonth.plusMonths((page - CENTER_PAGE).toLong())
    val pagerState = rememberPagerState(initialPage = pageOf(state.month)) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val currentOnShowMonth by rememberUpdatedState(onShowMonth)
    // Пролистали — открываем этот месяц
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> currentOnShowMonth(monthOf(page)) }
    }
    // Месяц сменился кнопкой («Сегодня», день соседнего месяца) — листаем к нему
    LaunchedEffect(state.month) {
        val target = pageOf(state.month)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }
    // Строка, открытая свайпом влево: открытой может быть только одна
    var openTaskId by remember { mutableStateOf<Long?>(null) }
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                CalendarTopBar(
                    month = state.month,
                    isCurrentMonth = state.month == YearMonth.from(state.today),
                    onBack = onBack,
                    onToday = onToday,
                    onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                )
                WeekdayRow(Modifier.padding(horizontal = 16.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { page ->
                    MonthGrid(
                        month = monthOf(page),
                        today = state.today,
                        selected = state.selectedDate,
                        marks = state.marks,
                        onSelect = onSelectDate,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = FloatingButtonsAreaHeight + navigationBarHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "day-header") {
                        DayHeader(
                            state.selectedDate,
                            state.today,
                            state.dayTasks.size + state.dayRepeats.size,
                            countVisible = !state.isLoading
                        )
                    }
                    items(state.dayTasks, key = { it.id }, contentType = { "task" }) { task ->
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
                    // Будущие повторы — после настоящих задач дня, бледными строками
                    items(state.dayRepeats, key = { "repeat-${it.id}" }, contentType = { "repeat" }) { task ->
                        RepeatRow(task, onClick = { onTaskClick(task) }, modifier = Modifier.animateItem())
                    }
                    if (!state.isLoading && state.dayTasks.isEmpty() && state.dayRepeats.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.calendar_empty),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            BottomFade(Modifier.align(Alignment.BottomCenter))
            FloatingTaskButtons(
                onVoiceClick = onVoiceClick,
                onAddClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = FloatingButtonsBottomPadding)
            )
        }
    }
}

/** «Назад», «Октябрь 2026» (год приглушён), «Сегодня» в другом месяце и стрелки. */
@Composable
private fun CalendarTopBar(
    month: YearMonth,
    isCurrentMonth: Boolean,
    onBack: () -> Unit,
    onToday: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
        val yearColor = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = buildAnnotatedString {
                append(monthName(month))
                withStyle(SpanStyle(color = yearColor, fontWeight = FontWeight.SemiBold)) { append(" ${month.year}") }
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!isCurrentMonth) {
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.day_today), style = MaterialTheme.typography.titleSmall)
            }
        }
        ArrowButton(stringResource(R.string.calendar_previous_month), rotation = 180f, onClick = onPrevious)
        ArrowButton(stringResource(R.string.calendar_next_month), rotation = 0f, onClick = onNext)
    }
}

@Composable
private fun ArrowButton(description: String, rotation: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            ChekkiIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Пн…Вс над сеткой, выходные — своим цветом. Для TalkBack не нужны: дни читаются полной датой. */
@Composable
private fun WeekdayRow(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = shortDayName(day),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = if (day.isWeekend) ChekkiTheme.colors.calendarWeekend else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Шесть недель месяца: всегда одной высоты, чтобы при листании ничего не прыгало. */
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    marks: Map<LocalDate, DayMarks>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        monthGrid(month).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isSelected = date == selected,
                        marks = marks[date],
                        today = today,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * День: число в круге 36 dp (сегодня — обведено акцентом, выбранный — залит), под ним до трёх точек
 * цветов категорий невыполненных задач или серая точка, если всё выполнено. Дни соседних месяцев бледные.
 */
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    marks: DayMarks?,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val numberColor = when {
        isSelected -> ChekkiTheme.colors.onFab
        isToday -> accent
        !inMonth -> MaterialTheme.colorScheme.outline
        date.dayOfWeek.isWeekend -> ChekkiTheme.colors.calendarWeekend
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = buildString {
        append(formatDayMonth(date, today))
        if (isToday) append(", ").append(stringResource(R.string.day_today).lowercase())
        when {
            marks == null -> Unit
            marks.allDone -> append(", ").append(stringResource(R.string.calendar_day_all_done))
            else -> append(", ").append(stringResource(R.string.calendar_day_has_tasks))
        }
    }
    Column(
        modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(accent)
                        isToday -> Modifier.border(1.5.dp, accent, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = numberColor
            )
        }
        Row(
            Modifier
                .padding(top = 2.dp)
                .height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (marks != null && inMonth) {
                if (marks.allDone) {
                    Dot(MaterialTheme.colorScheme.outline)
                } else {
                    marks.openColors.forEach { colorIndex ->
                        Dot(if (colorIndex != null) categoryColor(colorIndex) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Будущий повтор: бледная строка со значком повтора вместо кружка — отметить его нельзя, задачи
 * в базе ещё нет. Нажатие открывает карточку задачи, от которой посчитан повтор.
 */
@Composable
private fun RepeatRow(task: TaskUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.calendar_repeat_description, task.title)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClickLabel = stringResource(R.string.calendar_open_task), role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .alpha(0.6f)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Icon(ChekkiIcons.Repeat, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.time?.let { formatTime(it) } ?: stringResource(R.string.task_all_day),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                task.category?.let { category ->
                    Dot(categoryColor(category.colorIndex))
                    Text(category.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = stringResource(R.string.calendar_repeat_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** «Сегодня, 24 октября» / «Вторник, 27 октября» и справа «3 задачи». */
@Composable
private fun DayHeader(date: LocalDate, today: LocalDate, count: Int, countVisible: Boolean) {
    val relative = when (date) {
        today -> stringResource(R.string.day_today)
        today.plusDays(1) -> stringResource(R.string.day_tomorrow)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, RussianLocale).replaceFirstChar { it.titlecase(RussianLocale) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(R.string.calendar_day_title, relative, formatDayMonth(date, today)),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (countVisible && count > 0) {
            Text(
                text = pluralStringResource(R.plurals.calendar_tasks, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** «Октябрь» — всегда по-русски. */
private fun monthName(month: YearMonth): String =
    month.format(MonthFormatter).replaceFirstChar { it.titlecase(RussianLocale) }

private val DayOfWeek.isWeekend: Boolean get() = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY

private val RussianLocale: Locale = Locale.forLanguageTag("ru")
private val MonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL", RussianLocale)

/** Сколько месяцев можно пролистать в каждую сторону: по сто лет. */
private const val PAGE_COUNT = 2401
private const val CENTER_PAGE = PAGE_COUNT / 2

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun CalendarScreenPreview() {
    val today = LocalDate.of(2026, 10, 24)
    val marks = mapOf(
        today.minusDays(4) to DayMarks(listOf(5, 3), allDone = false),
        today.minusDays(2) to DayMarks(emptyList(), allDone = true),
        today to DayMarks(listOf(3, 0, 1), allDone = false),
        today.plusDays(3) to DayMarks(listOf(0, 4), allDone = false)
    )
    ChekkiTheme {
        CalendarScreen(
            CalendarUiState(
                today = today,
                month = YearMonth.from(today),
                selectedDate = today,
                marks = marks,
                dayTasks = sampleTodayTasks()
            )
        )
    }
}
