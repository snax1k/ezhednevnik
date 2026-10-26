package com.snax1k.chekki.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.components.BottomFade
import com.snax1k.chekki.ui.components.CardIconBox
import com.snax1k.chekki.ui.components.CardProgressBar
import com.snax1k.chekki.ui.components.FloatingButtonsAreaHeight
import com.snax1k.chekki.ui.components.FloatingButtonsBottomPadding
import com.snax1k.chekki.ui.components.FloatingTaskButtons
import com.snax1k.chekki.ui.components.HighlightCard
import com.snax1k.chekki.ui.components.ProgressCountText
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.components.formatDayTitle
import com.snax1k.chekki.ui.components.formatTime
import com.snax1k.chekki.ui.habit.vector
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.GroupColor
import com.snax1k.chekki.ui.theme.HighlightCardColors
import com.snax1k.chekki.ui.theme.TrackerColors
import com.snax1k.chekki.ui.theme.cssLinearGradient
import java.time.LocalDate

/** Главный экран: трекеры, быстрые входы и группы задач. Вариант Б из макета. */
@Composable
fun MainScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onTrackerClick: (TrackerUi) -> Unit = {},
    onTrackerLongClick: (TrackerUi) -> Unit = {},
    onAddHabitClick: () -> Unit = {},
    onQuickEntryClick: (QuickEntry) -> Unit = {},
    onGroupClick: (TaskGroup) -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    /** Плашка над быстрыми входами — обновление приложения. */
    banner: @Composable () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    // Прокрутка начинается под строкой состояния, чтобы содержимое не заезжало под часы
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = FloatingButtonsAreaHeight)
            ) {
                MainHeader(state.trackers, onMenuClick, onStatisticsClick, onTrackerClick, onTrackerLongClick, onAddHabitClick)
                DateAndTitle(state.date)
                banner()
                QuickEntries(onQuickEntryClick)
                // Пока база не ответила, группы прозрачные, потом плавно появляются
                val groupsAlpha by animateFloatAsState(
                    targetValue = if (state.isLoading) 0f else 1f,
                    label = "groupsAlpha"
                )
                TaskGroups(
                    state,
                    onGroupClick,
                    Modifier
                        .padding(top = 14.dp)
                        .alpha(groupsAlpha)
                )
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

@Composable
private fun MainHeader(
    trackers: List<TrackerUi>,
    onMenuClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onTrackerClick: (TrackerUi) -> Unit,
    onTrackerLongClick: (TrackerUi) -> Unit,
    onAddHabitClick: () -> Unit
) {
    val habitColors = ChekkiTheme.colors.habits
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareIconButton(ChekkiIcons.Menu, stringResource(R.string.main_menu), onMenuClick)
        // Привычки на сегодня; если не помещаются, ряд прокручивается вбок, «+» всегда последний
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trackers.forEach { tracker ->
                    key(tracker.habitId) {
                        TrackerRing(
                            tracker = tracker,
                            colors = habitColors[tracker.colorIndex.mod(habitColors.size)],
                            onClick = { onTrackerClick(tracker) },
                            onLongClick = { onTrackerLongClick(tracker) }
                        )
                    }
                }
                AddHabitCircle(onAddHabitClick)
            }
        }
        SquareIconButton(ChekkiIcons.Statistics, stringResource(R.string.main_statistics), onStatisticsClick)
    }
}

/**
 * Круг привычки: заливка её цветом, белое кольцо прогресса, иконка и «сделано/всего».
 * Нажатие — ещё одна отметка (с вибрацией), долгое нажатие — изменить привычку.
 * Когда цель на сегодня достигнута, вместо иконки галочка, а нажатие только коротко вибрирует.
 */
@Composable
private fun TrackerRing(tracker: TrackerUi, colors: TrackerColors, onClick: () -> Unit, onLongClick: () -> Unit) {
    val description = stringResource(
        R.string.main_tracker_description, tracker.name, tracker.done, tracker.total
    )
    val haptics = LocalHapticFeedback.current
    val ringColor = ChekkiTheme.colors.trackerRing
    val ringTrackColor = ChekkiTheme.colors.trackerRingTrack
    val progress = if (tracker.total > 0) (tracker.done.toFloat() / tracker.total).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(progress, label = "habitProgress")
    Box(
        Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(colors.fill)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.habit_check),
                onLongClickLabel = stringResource(R.string.habit_open),
                onLongClick = onLongClick,
                onClick = {
                    haptics.performHapticFeedback(
                        when {
                            tracker.isComplete -> HapticFeedbackType.Reject
                            tracker.done + 1 >= tracker.total -> HapticFeedbackType.Confirm
                            else -> HapticFeedbackType.ContextClick
                        }
                    )
                    if (!tracker.isComplete) onClick()
                }
            )
            .semantics { contentDescription = description }
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                // Кольцо на радиусе 23 из 25, как в макете
                val radius = size.minDimension / 2 - 2.dp.toPx()
                drawCircle(ringTrackColor, radius = radius, style = Stroke(strokeWidth))
                if (animatedProgress > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                imageVector = if (tracker.isComplete) ChekkiIcons.Check else tracker.icon.vector,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = colors.content
            )
            Text(
                text = "${tracker.done}/${tracker.total}",
                // Для TalkBack хватает описания трекера, число отдельно не читаем
                modifier = Modifier.clearAndSetSemantics { },
                style = MaterialTheme.typography.labelSmall,
                color = colors.content
            )
        }
    }
}

/** Круг «+» в конце ряда привычек: новая привычка. Если привычек нет, наверху только он. */
@Composable
private fun AddHabitCircle(onClick: () -> Unit) {
    Box(
        Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            ChekkiIcons.Add,
            contentDescription = stringResource(R.string.habit_add),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DateAndTitle(date: LocalDate) {
    Column(Modifier.padding(top = 6.dp, bottom = 14.dp)) {
        Text(
            text = formatDayTitle(date),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier
                .padding(top = 2.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@Composable
private fun QuickEntries(onClick: (QuickEntry) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickEntryButton(
            icon = ChekkiIcons.Categories,
            label = stringResource(R.string.quick_categories),
            onClick = { onClick(QuickEntry.Categories) },
            modifier = Modifier.weight(1f)
        )
        QuickEntryButton(
            icon = ChekkiIcons.Calendar,
            label = stringResource(R.string.quick_calendar),
            onClick = { onClick(QuickEntry.Calendar) },
            modifier = Modifier.weight(1f)
        )
        QuickEntryButton(
            icon = ChekkiIcons.Search,
            label = stringResource(R.string.quick_search),
            onClick = { onClick(QuickEntry.Search) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickEntryButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(52.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TaskGroups(state: MainUiState, onGroupClick: (TaskGroup) -> Unit, modifier: Modifier = Modifier) {
    val colors = ChekkiTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GroupStrip(
            title = stringResource(R.string.group_no_date),
            count = state.noDateCount,
            icon = ChekkiIcons.NoDate,
            color = colors.noDate,
            onClick = { onGroupClick(TaskGroup.NoDate) }
        )
        OverdueCard(
            count = state.overdueCount,
            titles = state.overdueTitles,
            onClick = { onGroupClick(TaskGroup.Overdue) }
        )
        TodayCard(
            done = state.todayDone,
            total = state.todayTotal,
            upcoming = state.todayUpcoming,
            onClick = { onGroupClick(TaskGroup.Today) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // В макете у «Выполненного» счётчика нет
            GroupTile(
                title = stringResource(R.string.group_done),
                count = null,
                icon = ChekkiIcons.Done,
                color = colors.done,
                onClick = { onGroupClick(TaskGroup.Done) },
                modifier = Modifier.weight(1f)
            )
            GroupTile(
                title = stringResource(R.string.group_tomorrow),
                count = state.tomorrowCount,
                icon = ChekkiIcons.Tomorrow,
                color = colors.tomorrow,
                onClick = { onGroupClick(TaskGroup.Tomorrow) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GroupTile(
                title = stringResource(R.string.group_this_week),
                count = state.thisWeekCount,
                icon = ChekkiIcons.ThisWeek,
                color = colors.thisWeek,
                onClick = { onGroupClick(TaskGroup.ThisWeek) },
                modifier = Modifier.weight(1f)
            )
            GroupTile(
                title = stringResource(R.string.group_this_month),
                count = state.thisMonthCount,
                icon = ChekkiIcons.ThisMonth,
                color = colors.thisMonth,
                onClick = { onGroupClick(TaskGroup.ThisMonth) },
                modifier = Modifier.weight(1f)
            )
        }
        GroupStrip(
            title = stringResource(R.string.group_later),
            count = state.laterCount,
            icon = ChekkiIcons.Later,
            color = colors.later,
            onClick = { onGroupClick(TaskGroup.Later) }
        )
    }
}

/** Полоса группы («Без даты», «Потом»). */
@Composable
private fun GroupStrip(title: String, count: Int, icon: ImageVector, color: GroupColor, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(cssLinearGradient(100f, color.base.copy(alpha = 0.30f), color.base.copy(alpha = 0.10f)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = color.tint)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color.tint)
    }
}

/** Плитка группы в сетке 2×2. */
@Composable
private fun GroupTile(
    title: String,
    count: Int?,
    icon: ImageVector,
    color: GroupColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .height(84.dp)
            .clip(MaterialTheme.shapes.large)
            .background(cssLinearGradient(145f, color.base.copy(alpha = 0.28f), color.base.copy(alpha = 0.08f)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = color.tint)
            if (count != null) {
                Text(count.toString(), style = MaterialTheme.typography.headlineSmall, color = color.tint)
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HighlightCardHeader(
    icon: ImageVector,
    title: String,
    colors: HighlightCardColors,
    count: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CardIconBox(icon, colors)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colors.content)
        count()
    }
}

@Composable
private fun OverdueCard(count: Int, titles: List<String>, onClick: () -> Unit) {
    val colors = ChekkiTheme.colors.overdue
    HighlightCard(colors, onClick = onClick) {
        HighlightCardHeader(ChekkiIcons.Overdue, stringResource(R.string.group_overdue), colors) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = colors.content)
        }
        val summary = overdueSummary(count, titles)
        if (summary != null) {
            Text(
                text = summary,
                // Подпись выровнена по названию: квадрат иконки 36 + промежуток 12
                modifier = Modifier.padding(start = 48.dp, top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.contentSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** «Оплатить интернет, Записаться к врачу и ещё 23». */
@Composable
private fun overdueSummary(count: Int, titles: List<String>): String? {
    if (titles.isEmpty()) return null
    val shown = titles.take(2)
    val joined = shown.joinToString(", ")
    val rest = count - shown.size
    return if (rest > 0) stringResource(R.string.overdue_summary, joined, rest) else joined
}

@Composable
private fun TodayCard(done: Int, total: Int, upcoming: List<TaskPreviewUi>, onClick: () -> Unit) {
    val colors = ChekkiTheme.colors.today
    HighlightCard(colors, onClick = onClick) {
        HighlightCardHeader(ChekkiIcons.Today, stringResource(R.string.group_today), colors) {
            ProgressCountText(done, total, colors)
        }
        CardProgressBar(
            done = done,
            total = total,
            trackColor = ChekkiTheme.colors.todayProgressTrack,
            color = colors.content,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        upcoming.take(2).forEach { task -> TaskPreviewRow(task, colors) }
    }
}

/** Строка задачи в карточке «Сегодня». Отметка пока только для вида: нажатие открывает всю группу. */
@Composable
private fun TaskPreviewRow(task: TaskPreviewUi, colors: HighlightCardColors) {
    Row(
        Modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(horizontal = 9.dp)
                .size(18.dp)
                .border(2.dp, colors.content.copy(alpha = 0.8f), CircleShape)
        )
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (task.time != null) {
            Text(
                text = formatTime(task.time),
                style = MaterialTheme.typography.labelMedium,
                color = colors.contentSecondary
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MainScreenPreview() {
    ChekkiTheme {
        MainScreen(state = sampleMainUiState(date = LocalDate.of(2026, 10, 22)))
    }
}
