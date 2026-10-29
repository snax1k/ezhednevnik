package com.snax1k.chekki.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.CompletedTask
import com.snax1k.chekki.data.StatsBar
import com.snax1k.chekki.data.StatsPeriod
import com.snax1k.chekki.data.TaskStats
import com.snax1k.chekki.data.taskStats
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.components.formatDateRange
import com.snax1k.chekki.ui.components.formatShortDate
import com.snax1k.chekki.ui.components.formatShortWeekday
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * «Статистика» (кнопка справа вверху на главном, шаг 1 — 29.10.2026; макета нет, в стиле экранов групп):
 * «Неделя / Месяц / Год», период листается стрелками назад, плитки «Выполнено», «В срок», «Просрочено»,
 * «Серия» и столбики выполненного по дням (у года — по месяцам). Нажатие на столбик показывает его число.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSelectPeriod: (StatsPeriod) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp + navigationBarHeight),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_title),
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 6.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PeriodSwitch(state.period, onSelectPeriod)
                val stats = state.stats
                if (stats != null) {
                    PeriodRow(state.period, state.offset, stats, state.today, onPrevious, onNext)
                    Tiles(state, stats)
                    BarsCard(stats, state.period, state.today)
                }
            }
        }
    }
}

/** «Неделя / Месяц / Год» — как вкладки. */
@Composable
private fun PeriodSwitch(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
    ) {
        StatsPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(period) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(period.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** «26 октября – 1 ноября» и под ним «Эта неделя»; стрелки — к предыдущему и следующему периоду. */
@Composable
private fun PeriodRow(
    period: StatsPeriod,
    offset: Int,
    stats: TaskStats,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArrowButton(stringResource(R.string.stats_previous_period), rotation = 180f, enabled = true, onClick = onPrevious)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                periodTitle(period, stats.range, today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (offset == 0) {
                Text(
                    stringResource(period.currentLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ArrowButton(stringResource(R.string.stats_next_period), rotation = 0f, enabled = offset > 0, onClick = onNext)
    }
}

@Composable
private fun ArrowButton(description: String, rotation: Float, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.3f),
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

/** Плитки 2×2: выполнено (и периодом раньше), в срок, просрочено сейчас, серия. */
@Composable
private fun Tiles(state: StatsUiState, stats: TaskStats) {
    val colors = ChekkiTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.stats_done),
                value = stats.done.toString(),
                hint = stringResource(state.period.previousLabel, stats.previousDone),
                valueColor = colors.taskDone,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_on_time),
                value = stats.onTimePercent?.let { "$it%" } ?: "—",
                hint = stringResource(if (stats.onTimePercent != null) R.string.stats_on_time_hint else R.string.stats_on_time_none),
                valueColor = colors.thisWeek.tint,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.stats_overdue),
                value = state.overdue.toString(),
                hint = stringResource(R.string.stats_overdue_hint),
                valueColor = if (state.overdue > 0) colors.priorityHigh else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_streak),
                value = pluralStringResource(R.plurals.stats_days, stats.currentStreak, stats.currentStreak),
                hint = stringResource(
                    R.string.stats_streak_best,
                    pluralStringResource(R.plurals.stats_days, stats.bestStreak, stats.bestStreak)
                ),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, hint: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .heightIn(min = 112.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        Spacer(Modifier.weight(1f, fill = false))
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Столбики выполненного: по дням (неделя, месяц) или по месяцам (год). Выбран сначала нынешний день
 * (месяц), а в прошлом периоде — самый высокий столбик; его дата и число — над столбиками.
 * Будущие дни не рисуются, дни без выполненного — низкой чёрточкой.
 */
@Composable
private fun BarsCard(stats: TaskStats, period: StatsPeriod, today: LocalDate) {
    val bars = stats.bars
    val currentIndex = bars.indexOfFirst { isCurrent(it, period, today) }
    var selected by remember(stats.range, period) {
        mutableIntStateOf(
            when {
                currentIndex >= 0 -> currentIndex
                stats.done == 0 -> -1
                else -> bars.indices.maxBy { bars[it].count }
            }
        )
    }
    val selectedBar = bars.getOrNull(selected)
    val accent = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedLabelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    val labels = bars.mapIndexed { index, bar -> barLabel(bar, index, period) }
    val description = bars.joinToString(", ") { "${barTitle(it, period, today)}: ${it.count}" }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Text(
            stringResource(if (period == StatsPeriod.Year) R.string.stats_by_month else R.string.stats_by_day),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedBar == null) {
                Text(
                    // Выбранного нет, только когда за период ничего не выполнено
                    stringResource(R.string.stats_nothing_done),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    barTitle(selectedBar, period, today),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    pluralStringResource(R.plurals.calendar_tasks, selectedBar.count, selectedBar.count),
                    style = MaterialTheme.typography.titleSmall,
                    color = accent
                )
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(bars, period, today) {
                    detectTapGestures { position ->
                        val index = (position.x / (size.width.toFloat() / bars.size)).toInt().coerceIn(bars.indices)
                        if (!isFuture(bars[index], period, today)) selected = index
                    }
                }
                .semantics { contentDescription = description }
        ) {
            val labelHeight = 22.dp.toPx()
            val bottom = size.height - labelHeight
            val slot = size.width / bars.size
            val barWidth = (slot * if (bars.size <= 12) 0.56f else 0.64f).coerceAtLeast(2.dp.toPx())
            val maxCount = (bars.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
            val corner = CornerRadius(minOf(barWidth / 2, 6.dp.toPx()))
            bars.forEachIndexed { index, bar ->
                val left = slot * index + (slot - barWidth) / 2
                if (!isFuture(bar, period, today)) {
                    val height = ((bottom - 4.dp.toPx()) * bar.count / maxCount).coerceAtLeast(3.dp.toPx())
                    val color = when {
                        index == selected -> accent
                        bar.count == 0 -> emptyColor
                        else -> accent.copy(alpha = 0.42f)
                    }
                    drawRoundRect(color, Offset(left, bottom - height), Size(barWidth, height), corner)
                }
                val label = labels[index] ?: return@forEachIndexed
                val measured = textMeasurer.measure(
                    label,
                    labelStyle.copy(color = if (index == selected) selectedLabelColor else labelColor)
                )
                val x = (slot * index + slot / 2 - measured.size.width / 2).coerceIn(0f, size.width - measured.size.width)
                drawText(measured, topLeft = Offset(x, bottom + 6.dp.toPx()))
            }
        }
    }
}

/** Подпись под столбиком: дни недели, у месяца — 1, 8, 15, 22, 29, у года — первые буквы месяцев. */
private fun barLabel(bar: StatsBar, index: Int, period: StatsPeriod): String? = when (period) {
    StatsPeriod.Week -> formatShortWeekday(bar.start)
    StatsPeriod.Month -> if (index % 7 == 0) bar.start.dayOfMonth.toString() else null
    StatsPeriod.Year -> bar.start.format(MonthFormatter).take(1).uppercase(RussianLocale)
}

/** «Чт, 29 окт» или «Октябрь 2026» — над столбиками и для TalkBack. */
private fun barTitle(bar: StatsBar, period: StatsPeriod, today: LocalDate): String =
    if (period == StatsPeriod.Year) monthTitle(YearMonth.from(bar.start)) else formatShortDate(bar.start, today)

private fun isCurrent(bar: StatsBar, period: StatsPeriod, today: LocalDate): Boolean =
    if (period == StatsPeriod.Year) YearMonth.from(bar.start) == YearMonth.from(today) else bar.start == today

private fun isFuture(bar: StatsBar, period: StatsPeriod, today: LocalDate): Boolean =
    if (period == StatsPeriod.Year) YearMonth.from(bar.start) > YearMonth.from(today) else bar.start > today

/** «26 октября – 1 ноября» (в другом году — с годом), «Октябрь 2026», «2026». */
private fun periodTitle(period: StatsPeriod, range: ClosedRange<LocalDate>, today: LocalDate): String = when (period) {
    StatsPeriod.Week -> formatDateRange(range).let { if (range.endInclusive.year == today.year) it else "$it ${range.endInclusive.year}" }
    StatsPeriod.Month -> monthTitle(YearMonth.from(range.start))
    StatsPeriod.Year -> range.start.year.toString()
}

private fun monthTitle(month: YearMonth): String =
    "${month.format(MonthFormatter).replaceFirstChar { it.titlecase(RussianLocale) }} ${month.year}"

private val StatsPeriod.label: Int
    get() = when (this) {
        StatsPeriod.Week -> R.string.stats_period_week
        StatsPeriod.Month -> R.string.stats_period_month
        StatsPeriod.Year -> R.string.stats_period_year
    }

private val StatsPeriod.currentLabel: Int
    get() = when (this) {
        StatsPeriod.Week -> R.string.stats_this_week
        StatsPeriod.Month -> R.string.stats_this_month
        StatsPeriod.Year -> R.string.stats_this_year
    }

private val StatsPeriod.previousLabel: Int
    get() = when (this) {
        StatsPeriod.Week -> R.string.stats_done_week_before
        StatsPeriod.Month -> R.string.stats_done_month_before
        StatsPeriod.Year -> R.string.stats_done_year_before
    }

private val RussianLocale: Locale = Locale.forLanguageTag("ru")
private val MonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL", RussianLocale)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun StatsScreenPreview() {
    val today = LocalDate.of(2026, 10, 29)
    val zone = ZoneId.of("Europe/Moscow")
    val completed = (0..40).map { day ->
        val date = today.minusDays((day % 12).toLong())
        CompletedTask(date.atTime(12, 0).atZone(zone).toInstant(), date)
    }
    ChekkiTheme {
        StatsScreen(StatsUiState(today = today, stats = taskStats(completed, StatsPeriod.Week, 0, today, zone), overdue = 51))
    }
}
