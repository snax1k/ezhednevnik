package com.snax1k.chekki.ui.habit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snax1k.chekki.R
import com.snax1k.chekki.data.HabitDay
import com.snax1k.chekki.data.HabitStreaks
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.ui.addtask.DragHandle
import com.snax1k.chekki.ui.components.formatShortDate
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.TrackerColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Карточка привычки: прогресс за день с «−» и «+», последние 7 дней (прошлый день можно выбрать
 * и исправить) и серии. Макета нет, сделана в стиле панели новой задачи. «Изменить» открывает панель привычки,
 * «Все привычки» внизу — экран со списком, порядком и архивом.
 */
// ModalBottomSheet в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCardSheet(
    viewModel: HabitCardViewModel,
    onDismiss: () -> Unit,
    onEdit: (Long) -> Unit,
    onShowAll: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Привычку удалили, пока карточка была открыта, — закрываем
    var wasShown by remember { mutableStateOf(false) }
    LaunchedEffect(state == null) {
        if (state != null) wasShown = true else if (wasShown) onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { DragHandle() }
    ) {
        state?.let { current ->
            HabitCardContent(
                state = current,
                onEdit = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onEdit(current.habit.id) }
                },
                onShowAll = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onShowAll() }
                },
                onResume = viewModel::resume,
                onSelectDay = viewModel::selectDay,
                onIncrement = viewModel::increment,
                onDecrement = viewModel::decrement
            )
        }
    }
}

@Composable
private fun HabitCardContent(
    state: HabitCardState,
    onEdit: () -> Unit,
    onShowAll: () -> Unit,
    onResume: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val palette = ChekkiTheme.colors.habits
    val colors = palette[state.habit.colorIndex.mod(palette.size)]
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
    ) {
        CardHeader(state.habit, colors, onEdit)
        state.pause?.let { pause -> PauseBanner(pause.resumeDate, state.today, onResume, Modifier.padding(top = 16.dp)) }
        val dayTitle = dayTitle(state.selectedDay.date, state.today)
        Text(
            text = dayTitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        DayProgress(state.selectedDay, dayTitle, colors, onIncrement, onDecrement, Modifier.padding(top = 8.dp))
        WeekStrip(
            week = state.week,
            selected = state.selectedDay.date,
            today = state.today,
            colors = colors,
            onSelect = onSelectDay,
            modifier = Modifier.padding(top = 20.dp)
        )
        StreakTiles(state.streaks, Modifier.padding(top = 20.dp))
        TextButton(
            onClick = onShowAll,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.habits_all), style = MaterialTheme.typography.titleSmall)
            Icon(
                ChekkiIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp)
            )
        }
    }
}

/** «На паузе до 30 окт» и «Продолжить» — у привычки на паузе. */
@Composable
private fun PauseBanner(resumeDate: LocalDate?, today: LocalDate, onResume: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Pause, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = if (resumeDate == null) {
                stringResource(R.string.habit_card_paused_no_end)
            } else {
                stringResource(R.string.habit_card_paused_until, pauseEndText(resumeDate, today))
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onResume) {
            Text(stringResource(R.string.habit_resume), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Круг с иконкой, название, «Каждый день · 6 раз в день» и «Изменить». */
@Composable
private fun CardHeader(habit: HabitEntity, colors: TrackerColors, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.fill),
            contentAlignment = Alignment.Center
        ) {
            Icon(habit.icon.vector, contentDescription = null, Modifier.size(22.dp), tint = colors.content)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = habit.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                text = stringResource(
                    R.string.habit_card_summary,
                    daysSummary(habit.weekdays),
                    pluralStringResource(R.plurals.habit_times_per_day, habit.dailyTarget, habit.dailyTarget)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onEdit) {
            Text(stringResource(R.string.habit_card_edit), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Крупное кольцо за выбранный день с «−» слева и «+» справа. */
@Composable
private fun DayProgress(
    day: HabitDay,
    /** «Сегодня», «Вчера», «Ср, 21 окт» — для TalkBack. */
    dayTitle: String,
    colors: TrackerColors,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundButton(
            icon = ChekkiIcons.Minus,
            description = stringResource(R.string.habit_card_uncheck),
            enabled = day.count > 0,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onDecrement()
            }
        )
        val description = stringResource(R.string.main_tracker_description, dayTitle, day.count, day.target)
        ProgressRing(
            day = day,
            colors = colors,
            size = 148.dp,
            strokeWidth = 10.dp,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description }
        ) {
            if (day.isComplete) {
                Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(22.dp), tint = colors.fill)
            }
            Text(
                text = buildAnnotatedString {
                    append(day.count.toString())
                    withStyle(SpanStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append("/${day.target}")
                    }
                },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        RoundButton(
            icon = ChekkiIcons.Add,
            description = stringResource(R.string.habit_card_check),
            enabled = !day.isComplete,
            onClick = {
                haptics.performHapticFeedback(
                    if (day.count + 1 >= day.target) HapticFeedbackType.Confirm else HapticFeedbackType.ContextClick
                )
                onIncrement()
            }
        )
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Кольцо прогресса дня цветом привычки. Выполненный день в маленьком кольце залит целиком —
 * это решает вызывающий, а здесь только дорожка и дуга.
 */
@Composable
private fun ProgressRing(
    day: HabitDay,
    colors: TrackerColors,
    size: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val progress = if (day.target > 0) (day.count.toFloat() / day.target).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(progress, label = "habitDayProgress")
    val track = colors.fill.copy(alpha = 0.22f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = this.size.minDimension / 2 - stroke / 2
            drawCircle(track, radius = radius, style = Stroke(stroke))
            if (animated > 0f) {
                drawArc(
                    color = colors.fill,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

/** Последние 7 дней: день недели, маленькое кольцо, число. Выбранный день подсвечен. */
@Composable
private fun WeekStrip(
    week: List<HabitDay>,
    selected: LocalDate,
    today: LocalDate,
    colors: TrackerColors,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth()) {
        week.forEach { day ->
            val isSelected = day.date == selected
            val label = dayTitle(day.date, today)
            val description = if (day.isScheduled) {
                stringResource(R.string.habit_week_day, label, day.count, day.target)
            } else {
                stringResource(R.string.habit_week_day_off, label)
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                    .clickable(enabled = day.isScheduled, role = Role.Tab, onClick = { onSelect(day.date) })
                    .semantics {
                        contentDescription = description
                        this.selected = isSelected
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = shortDayName(day.date.dayOfWeek),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    when {
                        !day.isScheduled -> Text(
                            text = "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        day.isComplete -> Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colors.fill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ChekkiIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = colors.content)
                        }
                        else -> ProgressRing(day, colors, size = 34.dp, strokeWidth = 3.dp)
                    }
                }
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (day.date == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** «Серия: 5 дней подряд» и «Лучшая серия: 12 дней подряд» двумя плитками. */
@Composable
private fun StreakTiles(streaks: HabitStreaks, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StreakTile(stringResource(R.string.habit_streak), streaks.current, Modifier.weight(1f))
        StreakTile(stringResource(R.string.habit_best_streak), streaks.best, Modifier.weight(1f))
    }
}

@Composable
private fun StreakTile(label: String, days: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics(mergeDescendants = true) {}
            .padding(14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = pluralStringResource(R.plurals.habit_streak_days, days, days),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun dayTitle(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    else -> formatShortDate(date, today)
}

@Preview(widthDp = 412, heightDp = 680)
@Composable
private fun HabitCardContentPreview() {
    val today = LocalDate.of(2026, 10, 23)
    val habit = HabitEntity(
        id = 1,
        title = "Пить воду",
        icon = HabitIcon.Water,
        colorIndex = 1,
        dailyTarget = 6,
        position = 0,
        createdAt = Instant.EPOCH
    )
    val week = (6 downTo 0).map { ago ->
        HabitDay(today.minusDays(ago.toLong()), isScheduled = true, count = if (ago == 0) 3 else if (ago % 3 == 0) 2 else 6, target = 6)
    }
    ChekkiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            HabitCardContent(
                state = HabitCardState(habit, today, week.last(), week, HabitStreaks(current = 2, best = 9)),
                onEdit = {},
                onShowAll = {},
                onResume = {},
                onSelectDay = {},
                onIncrement = {},
                onDecrement = {}
            )
        }
    }
}
