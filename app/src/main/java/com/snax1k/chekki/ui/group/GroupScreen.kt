package com.snax1k.chekki.ui.group

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.GroupRanges
import com.snax1k.chekki.ui.components.BottomFade
import com.snax1k.chekki.ui.components.CardProgressBar
import com.snax1k.chekki.ui.components.FloatingButtonsAreaHeight
import com.snax1k.chekki.ui.components.FloatingButtonsBottomPadding
import com.snax1k.chekki.ui.components.FloatingTaskButtons
import com.snax1k.chekki.ui.components.HighlightCard
import com.snax1k.chekki.ui.components.ProgressCountText
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.components.formatDateRange
import com.snax1k.chekki.ui.components.formatDayMonth
import com.snax1k.chekki.ui.components.formatDayTitle
import com.snax1k.chekki.ui.components.formatShortDate
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.main.TaskGroup
import com.snax1k.chekki.ui.main.sampleTodayTasks
import com.snax1k.chekki.ui.tasks.SwipeableTaskRow
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.GroupColor
import com.snax1k.chekki.ui.theme.cssLinearGradient
import java.time.LocalDate

/**
 * Список задач одной группы. Макет есть только у «Сегодня» и только в стиле А, здесь он переведён на Б:
 * шапка — карточка группы как на главном экране, задачи — скруглённые карточки.
 */
@Composable
fun GroupScreen(
    state: GroupUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onToggleDone: (TaskUi) -> Unit = {},
    onTaskClick: (TaskUi) -> Unit = {},
    onPostpone: (TaskUi, LocalDate) -> Unit = { _, _ -> },
    onDelete: (TaskUi) -> Unit = {},
    onShowMore: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    TaskListScreen(
        today = state.today,
        sections = state.sections,
        isLoading = state.isLoading,
        emptyText = stringResource(state.group.emptyText),
        header = { headerModifier -> GroupHeader(state, headerModifier) },
        modifier = modifier,
        canShowMore = state.canShowMore,
        onBack = onBack,
        onToggleDone = onToggleDone,
        onTaskClick = onTaskClick,
        onPostpone = onPostpone,
        onDelete = onDelete,
        onShowMore = onShowMore,
        onVoiceClick = onVoiceClick,
        onAddClick = onAddClick
    )
}

/**
 * Общий каркас списка задач — групп и категорий: «Назад», шапка [header], задачи по дням со свайпами,
 * подпись [emptyText] у пустого списка, «+» и микрофон внизу.
 */
@Composable
fun TaskListScreen(
    today: LocalDate,
    sections: List<TaskSection>,
    isLoading: Boolean,
    emptyText: String,
    header: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    /** Кнопка справа в верхней панели (у категории — «Изменить»). */
    topBarAction: @Composable () -> Unit = {},
    canShowMore: Boolean = false,
    onBack: () -> Unit = {},
    onToggleDone: (TaskUi) -> Unit = {},
    onTaskClick: (TaskUi) -> Unit = {},
    onPostpone: (TaskUi, LocalDate) -> Unit = { _, _ -> },
    onDelete: (TaskUi) -> Unit = {},
    onShowMore: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Строка, открытая свайпом влево: открытой может быть только одна
    var openTaskId by remember { mutableStateOf<Long?>(null) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                GroupTopBar(onBack, topBarAction)
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = FloatingButtonsAreaHeight + navigationBarHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        header(Modifier.padding(bottom = 6.dp))
                    }
                    for (section in sections) {
                        if (section.day != null) {
                            item(key = "day-${section.day}", contentType = "day") {
                                DayHeader(dayTitle(section.day, today))
                            }
                        } else if (section.isNoDate) {
                            item(key = "no-date", contentType = "day") {
                                DayHeader(stringResource(R.string.group_no_date))
                            }
                        }
                        items(section.tasks, key = { it.id }, contentType = { "task" }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                today = today,
                                isOpen = openTaskId == task.id,
                                onOpen = { openTaskId = task.id },
                                onClose = { if (openTaskId == task.id) openTaskId = null },
                                onToggleDone = { onToggleDone(task) },
                                onClick = { onTaskClick(task) },
                                onPostpone = { date -> onPostpone(task, date) },
                                onDelete = { onDelete(task) },
                                // Удалённая или отложенная задача уходит из списка плавно, остальные сдвигаются
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    if (canShowMore) {
                        item(key = "show-more") {
                            ShowMoreButton(onShowMore, Modifier.padding(top = 4.dp))
                        }
                    }
                    if (!isLoading && sections.isEmpty()) {
                        item {
                            Text(
                                text = emptyText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
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

/** Верхняя панель. Кнопки «Сортировка» и «Ещё» из макета вернутся, когда появятся сами функции. */
@Composable
private fun GroupTopBar(onBack: () -> Unit, action: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
        Spacer(Modifier.weight(1f))
        action()
    }
}

/** Подзаголовок дня в группах на несколько дней, в «Выполненном» и в категориях. */
@Composable
private fun DayHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    today.plusDays(1) -> stringResource(R.string.day_tomorrow)
    else -> formatShortDate(day, today)
}

/** «Показать ещё» в конце «Выполненного». */
@Composable
private fun ShowMoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.list_show_more),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Шапка группы: у «Сегодня» и «Просроченных» яркая карточка, как на главном экране, у остальных —
 * карточка в цвете группы, как плитка. Счётчик показывает невыполненные задачи, у «Сегодня» —
 * «выполнено/всего» с полосой прогресса, у «Выполненного» счётчика нет (как на главном).
 * Пока задачи грузятся, счётчик прозрачный, но место занимает.
 */
@Composable
private fun GroupHeader(state: GroupUiState, modifier: Modifier = Modifier) {
    val title = stringResource(state.group.title)
    val subtitle = groupSubtitle(state.group, state.today)
    val countAlpha = if (state.isLoading) 0f else 1f
    val openCount = state.tasks.count { !it.done }
    when (state.group) {
        TaskGroup.Today -> {
            val colors = ChekkiTheme.colors.today
            val done = state.tasks.count { it.done }
            HighlightCard(colors, modifier) {
                HeaderTitleRow(title, subtitle, colors.content, colors.contentSecondary, countAlpha) {
                    ProgressCountText(done, state.tasks.size, colors)
                }
                CardProgressBar(
                    done = done,
                    total = state.tasks.size,
                    trackColor = ChekkiTheme.colors.todayProgressTrack,
                    color = colors.content,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .alpha(countAlpha)
                )
            }
        }
        TaskGroup.Overdue -> {
            val colors = ChekkiTheme.colors.overdue
            HighlightCard(colors, modifier) {
                HeaderTitleRow(title, subtitle, colors.content, colors.contentSecondary, countAlpha) {
                    CountText(openCount, colors.content)
                }
            }
        }
        else -> {
            val color = groupColor(state.group)
            TintedCard(color, modifier) {
                HeaderTitleRow(
                    title,
                    subtitle,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    countAlpha
                ) {
                    if (state.group != TaskGroup.Done) {
                        CountText(openCount, color.tint)
                    }
                }
            }
        }
    }
}

/** Карточка в цвете группы или категории: диагональный градиент, как у плиток главного экрана. */
@Composable
internal fun TintedCard(color: GroupColor, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cssLinearGradient(145f, color.base.copy(alpha = 0.28f), color.base.copy(alpha = 0.08f)))
            .padding(16.dp),
        content = content
    )
}

/** Название группы с подписью слева и счётчик справа, по нижнему краю. */
@Composable
internal fun HeaderTitleRow(
    title: String,
    subtitle: String?,
    titleColor: Color,
    subtitleColor: Color,
    countAlpha: Float,
    count: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = subtitleColor
                )
            }
        }
        Box(Modifier.alpha(countAlpha)) {
            count()
        }
    }
}

@Composable
internal fun CountText(count: Int, color: Color) {
    Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = color)
}

/** Подпись под названием группы: день или даты, которые в неё входят. */
@Composable
private fun groupSubtitle(group: TaskGroup, today: LocalDate): String? {
    val ranges = GroupRanges.of(today)
    // В субботу и воскресенье «На неделе» пустая, а в конце месяца бывает пустым «В этом месяце»
    fun range(dates: ClosedRange<LocalDate>) = if (dates.isEmpty()) null else formatDateRange(dates)
    return when (group) {
        TaskGroup.Today -> formatDayTitle(today)
        TaskGroup.Tomorrow -> formatDayTitle(ranges.tomorrow)
        TaskGroup.ThisWeek -> range(ranges.thisWeek)
        TaskGroup.ThisMonth -> range(ranges.thisMonth)
        TaskGroup.Later -> stringResource(R.string.group_later_from, formatDayMonth(ranges.laterFrom, today))
        TaskGroup.NoDate, TaskGroup.Overdue, TaskGroup.Done -> null
    }
}

/** Цвета группы для карточки-шапки. У «Сегодня» и «Просроченных» свои яркие карточки. */
@Composable
private fun groupColor(group: TaskGroup): GroupColor = with(ChekkiTheme.colors) {
    when (group) {
        TaskGroup.NoDate -> noDate
        TaskGroup.Tomorrow -> tomorrow
        TaskGroup.ThisWeek -> thisWeek
        TaskGroup.ThisMonth -> thisMonth
        TaskGroup.Later -> later
        TaskGroup.Done -> done
        TaskGroup.Overdue, TaskGroup.Today -> error("У группы $group яркая карточка")
    }
}

@get:StringRes
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

@get:StringRes
private val TaskGroup.emptyText: Int
    get() = when (this) {
        TaskGroup.Today -> R.string.group_empty_today
        TaskGroup.Tomorrow -> R.string.group_empty_tomorrow
        TaskGroup.NoDate -> R.string.group_empty_no_date
        TaskGroup.Overdue -> R.string.group_empty_overdue
        TaskGroup.ThisWeek -> R.string.group_empty_this_week
        TaskGroup.ThisMonth -> R.string.group_empty_this_month
        TaskGroup.Later -> R.string.group_empty_later
        TaskGroup.Done -> R.string.group_empty_done
    }

private val PreviewToday = LocalDate.of(2026, 10, 22)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TodayGroupPreview() {
    ChekkiTheme {
        var tasks by remember { mutableStateOf(sampleTodayTasks()) }
        GroupScreen(
            state = GroupUiState(TaskGroup.Today, PreviewToday, sections = listOf(TaskSection(null, tasks))),
            onToggleDone = { task ->
                tasks = tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }
            }
        )
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun ThisMonthGroupPreview() {
    ChekkiTheme {
        val tasks = sampleTodayTasks().map { it.copy(done = false) }
        GroupScreen(
            state = GroupUiState(
                TaskGroup.ThisMonth,
                PreviewToday,
                sections = listOf(
                    TaskSection(PreviewToday.plusDays(4), tasks.take(2)),
                    TaskSection(PreviewToday.plusDays(7), tasks.drop(2).take(3))
                )
            )
        )
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun DoneGroupPreview() {
    ChekkiTheme {
        val tasks = sampleTodayTasks().map { it.copy(done = true) }
        GroupScreen(
            state = GroupUiState(
                TaskGroup.Done,
                PreviewToday,
                sections = listOf(
                    TaskSection(PreviewToday, tasks.take(2)),
                    TaskSection(PreviewToday.minusDays(1), tasks.drop(2).take(2))
                ),
                canShowMore = true
            )
        )
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun EmptyNoDateGroupPreview() {
    ChekkiTheme {
        GroupScreen(state = GroupUiState(TaskGroup.NoDate, PreviewToday, sections = emptyList()))
    }
}
