package com.snax1k.chekki.ui.addtask

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.weekParity
import com.snax1k.chekki.ui.components.formatDayMonthShort
import com.snax1k.chekki.ui.components.formatShortDate
import com.snax1k.chekki.ui.habit.SectionLabel
import com.snax1k.chekki.ui.habit.SheetHeader
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Строка «Повтор» в панели и в карточке: подпись правила, по нажатию — окно «Повтор». */
@Composable
internal fun RepeatRow(
    rule: RepeatRule,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placement: FieldRowPlacement = FieldRowPlacement.Sheet
) {
    FieldRow(
        icon = ChekkiIcons.Repeat,
        label = stringResource(R.string.add_task_repeat),
        onClick = onClick,
        modifier = modifier,
        placement = placement
    ) {
        Text(
            text = repeatSummary(rule, today),
            // Длинная подпись («Две недели: Вт, Чт / Пн») не должна вытеснять название строки
            modifier = Modifier.weight(1.4f, fill = false),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Окно «Повтор» (вариант 1, решено 23.11.2026): вкладки «Неделя», «Месяц», «Свой период», настройки вкладки
 * сразу под ней, ниже «Следующая задача» и «Повторять до». [onDone] получает правило и дату следующей задачи,
 * [onClear] — «Не повторять». После любого из них и после закрытия вызывается [onDismiss].
 */
// ModalBottomSheet в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatSheet(
    rule: RepeatRule,
    taskDate: LocalDate?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onDone: (RepeatRule, LocalDate) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Сначала доигрываем анимацию закрытия, потом убираем панель
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }
    var editor by rememberSaveable(stateSaver = RepeatEditorSaver) { mutableStateOf(RepeatEditor.of(rule, taskDate, today)) }
    var picker by rememberSaveable { mutableStateOf<RepeatDatePicker?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { DragHandle() }
    ) {
        RepeatContent(
            editor = editor,
            canClear = rule != RepeatRule.None,
            onChange = { editor = it },
            onPickNextDate = { picker = RepeatDatePicker.Next },
            onPickUntil = { picker = RepeatDatePicker.Until },
            onClose = { hideThen {} },
            onClear = { hideThen(onClear) },
            onDone = {
                val chosen = editor.rule
                if (chosen != null && editor.canSave) hideThen { onDone(chosen, editor.nextDate) }
            }
        )
    }

    when (picker) {
        RepeatDatePicker.Next -> TaskDatePickerDialog(
            initialDate = editor.nextDate,
            onDismiss = { picker = null },
            onNext = { date ->
                editor = editor.withNextDate(date)
                picker = null
            },
            confirmLabel = R.string.action_done,
            firstSelectableDate = today
        )
        RepeatDatePicker.Until -> TaskDatePickerDialog(
            initialDate = editor.until ?: editor.nextDate.plusMonths(1),
            onDismiss = { picker = null },
            onNext = { date ->
                editor = editor.withUntil(date)
                picker = null
            },
            confirmLabel = R.string.action_done,
            firstSelectableDate = editor.nextDate
        )
        null -> Unit
    }
}

/** Какой календарь открыт поверх окна «Повтор». */
private enum class RepeatDatePicker { Next, Until }

@Composable
private fun RepeatContent(
    editor: RepeatEditor,
    canClear: Boolean,
    onChange: (RepeatEditor) -> Unit,
    onPickNextDate: () -> Unit,
    onPickUntil: () -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    // Не выше 85% окна, как у других панелей: иначе панель, упёршись в верх, сама прячется
    Column(Modifier.heightIn(max = sheetContentMaxHeight())) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SheetHeader(title = stringResource(R.string.add_task_repeat), onClose = onClose)
            RepeatTabs(selected = editor.tab, onSelect = { onChange(editor.withTab(it)) })
            // Высота — по самой высокой вкладке («Месяц»): иначе при переключении панель меняет высоту,
            // вкладки уезжают из-под пальца и следующее нажатие попадает не туда
            Box {
                Box(
                    Modifier
                        .alpha(0f)
                        .clearAndSetSemantics {}
                ) {
                    MonthTab(editor, onChange = {}, enabled = false)
                }
                when (editor.tab) {
                    RepeatTab.Week -> WeekTab(editor, onChange)
                    RepeatTab.Month -> MonthTab(editor, onChange)
                    RepeatTab.Custom -> CustomTab(editor, onChange)
                }
            }
            // Место под подсказку есть всегда, по той же причине
            Text(
                text = stringResource(R.string.repeat_nothing_selected),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .alpha(if (editor.rule == null) 1f else 0f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            HorizontalDivider(Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            FieldRow(
                icon = ChekkiIcons.Calendar,
                label = stringResource(R.string.repeat_next_date),
                onClick = onPickNextDate
            ) {
                Text(
                    text = formatShortDate(editor.nextDate, editor.today),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            UntilRow(editor, onPickUntil = onPickUntil, onClear = { onChange(editor.withUntil(null)) })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canClear) {
                TextButton(onClick = onClear, modifier = Modifier.height(56.dp)) {
                    Text(stringResource(R.string.repeat_none), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(8.dp))
            }
            SaveButton(
                enabled = editor.canSave,
                onClick = onDone,
                text = stringResource(R.string.action_done),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** «Неделя / Месяц / Свой период» — как вкладки, в стиле переключателя статистики. */
@Composable
private fun RepeatTabs(selected: RepeatTab, onSelect: (RepeatTab) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(52.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        RepeatTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        when (tab) {
                            RepeatTab.Week -> R.string.repeat_tab_week
                            RepeatTab.Month -> R.string.repeat_tab_month
                            RepeatTab.Custom -> R.string.repeat_tab_custom
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/** Дни недели; в двухнедельном режиме — два ряда: эта неделя и следующая. */
@Composable
private fun WeekTab(editor: RepeatEditor, onChange: (RepeatEditor) -> Unit) {
    Column {
        SectionLabel(stringResource(if (editor.twoWeeks) R.string.repeat_this_week else R.string.repeat_pick_weekdays))
        WeekdayCircles(
            isOn = { editor.isWeekdayOn(it) },
            onToggle = { onChange(editor.toggleWeekday(it)) }
        )
        if (editor.twoWeeks) {
            SectionLabel(stringResource(R.string.repeat_next_week))
            WeekdayCircles(
                isOn = { editor.isWeekdayOn(it, nextWeek = true) },
                onToggle = { onChange(editor.toggleWeekday(it, nextWeek = true)) }
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(MaterialTheme.shapes.small)
                .toggleable(
                    value = editor.twoWeeks,
                    role = Role.Switch,
                    onValueChange = { onChange(editor.withTwoWeeks(it)) }
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.repeat_two_weeks),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.repeat_two_weeks_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = editor.twoWeeks,
                // Нажатие обрабатывает вся строка
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun WeekdayCircles(isOn: (DayOfWeek) -> Boolean, onToggle: (DayOfWeek) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val on = isOn(day)
            val fullName = day.getDisplayName(TextStyle.FULL_STANDALONE, RussianLocale)
            SelectableCircle(
                text = day.getDisplayName(TextStyle.SHORT_STANDALONE, RussianLocale)
                    .replaceFirstChar { it.titlecase(RussianLocale) },
                description = fullName,
                isOn = on,
                onToggle = { onToggle(day) },
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

/** Числа 1–31 сеткой по семь; 31-е — последний день месяца. */
@Composable
private fun MonthTab(editor: RepeatEditor, onChange: (RepeatEditor) -> Unit, enabled: Boolean = true) {
    Column {
        SectionLabel(stringResource(R.string.repeat_pick_month_days))
        (1..31).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    SelectableCircle(
                        text = day.toString(),
                        description = day.toString(),
                        isOn = editor.isMonthDayOn(day),
                        onToggle = { onChange(editor.toggleMonthDay(day)) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                // Пустые места в последнем ряду, чтобы кружки остались того же размера
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = stringResource(R.string.repeat_last_day_hint),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** «Каждые − 3 +» и единица: день, неделя, месяц, год. */
@Composable
private fun CustomTab(editor: RepeatEditor, onChange: (RepeatEditor) -> Unit) {
    Column {
        SectionLabel(stringResource(R.string.repeat_every_label))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(
                text = "−",
                description = stringResource(R.string.repeat_every_decrease),
                enabled = editor.every > RepeatEditor.MIN_EVERY,
                onClick = { onChange(editor.withEvery(editor.every - 1)) }
            )
            Text(
                text = editor.every.toString(),
                modifier = Modifier.width(64.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            RoundButton(
                text = "+",
                description = stringResource(R.string.repeat_every_increase),
                enabled = editor.every < RepeatEditor.MAX_EVERY,
                onClick = { onChange(editor.withEvery(editor.every + 1)) }
            )
            Text(
                text = editor.rule?.let { repeatEveryText(it.unit, it.every) }.orEmpty(),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomRepeatUnits.forEach { unit ->
                val isSelected = unit == editor.unit
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onChange(editor.withUnit(unit)) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pluralStringResource(unitPlurals(unit), editor.every),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** «Повторять до»: «Бессрочно» или дата; у даты — крестик «Повторять бессрочно». */
@Composable
private fun UntilRow(editor: RepeatEditor, onPickUntil: () -> Unit, onClear: () -> Unit) {
    val until = editor.until
    FieldRow(
        icon = ChekkiIcons.Done,
        label = stringResource(R.string.repeat_until),
        onClick = onPickUntil
    ) {
        if (until == null) {
            Text(
                text = stringResource(R.string.repeat_until_forever),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatShortDate(until, editor.today),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (editor.untilTooEarly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                if (editor.untilTooEarly) {
                    Text(
                        text = stringResource(R.string.repeat_until_too_early),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ChekkiIcons.Close,
                    contentDescription = stringResource(R.string.repeat_until_clear),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Кружок-переключатель: день недели или число месяца. Включённый залит акцентом. */
@Composable
private fun SelectableCircle(
    text: String,
    description: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .toggleable(value = isOn, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoundButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Подпись правила: «Каждый день», «Пн, Ср, Пт», «Две недели: Вт, Чт / Пн», «Каждый месяц: 2, 15, последний день»,
 * «Каждые 3 дня», с концом — «… · до 31 дек».
 */
@Composable
internal fun repeatSummary(rule: RepeatRule, today: LocalDate): String {
    val text = when (rule.unit) {
        RepeatUnit.None -> return stringResource(R.string.repeat_none)
        RepeatUnit.Week ->
            if (rule.weekdays == 0) {
                repeatEveryText(RepeatUnit.Week, rule.every)
            } else if (rule.every == 1) {
                when (rule.weekdays) {
                    RepeatRule.ALL_WEEKDAYS -> stringResource(R.string.repeat_daily)
                    MONDAY_TO_FRIDAY -> stringResource(R.string.repeat_weekdays)
                    WEEKEND -> stringResource(R.string.repeat_weekend)
                    else -> weekdaysText(rule.weekdays)
                }
            } else {
                stringResource(R.string.repeat_with_days, repeatEveryText(RepeatUnit.Week, rule.every), weekdaysText(rule.weekdays))
            }
        RepeatUnit.TwoWeeks -> {
            val thisWeekIsEven = weekParity(today) == 0
            val thisWeek = if (thisWeekIsEven) rule.weekdays else rule.weekdays2
            val nextWeek = if (thisWeekIsEven) rule.weekdays2 else rule.weekdays
            stringResource(R.string.repeat_two_weeks_summary, weekdaysOrDash(thisWeek), weekdaysOrDash(nextWeek))
        }
        RepeatUnit.Month ->
            if (rule.monthDays == 0) repeatEveryText(RepeatUnit.Month, rule.every)
            else stringResource(R.string.repeat_with_days, repeatEveryText(RepeatUnit.Month, rule.every), monthDaysText(rule.monthDays))
        RepeatUnit.Day, RepeatUnit.Year -> repeatEveryText(rule.unit, rule.every)
    }
    val until = rule.until ?: return text
    val untilText = if (until.year == today.year) formatDayMonthShort(until) else "${formatDayMonthShort(until)} ${until.year}"
    return stringResource(R.string.repeat_until_summary, text, untilText)
}

/** «Каждый день», «Каждые 3 недели», «Каждый 21 день». */
@Composable
internal fun repeatEveryText(unit: RepeatUnit, every: Int): String {
    val (@StringRes single, @PluralsRes many) = when (unit) {
        RepeatUnit.Week, RepeatUnit.TwoWeeks -> R.string.repeat_weekly to R.plurals.repeat_every_weeks
        RepeatUnit.Month -> R.string.repeat_monthly to R.plurals.repeat_every_months
        RepeatUnit.Year -> R.string.repeat_yearly to R.plurals.repeat_every_years
        RepeatUnit.Day, RepeatUnit.None -> R.string.repeat_daily to R.plurals.repeat_every_days
    }
    return if (every <= 1) stringResource(single) else pluralStringResource(many, every, every)
}

@PluralsRes
private fun unitPlurals(unit: RepeatUnit): Int = when (unit) {
    RepeatUnit.Week -> R.plurals.repeat_unit_week
    RepeatUnit.Month -> R.plurals.repeat_unit_month
    RepeatUnit.Year -> R.plurals.repeat_unit_year
    else -> R.plurals.repeat_unit_day
}

/** «Пн, Ср, Пт». */
private fun weekdaysText(mask: Int): String =
    DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.joinToString(", ") { day ->
        day.getDisplayName(TextStyle.SHORT_STANDALONE, RussianLocale).replaceFirstChar { it.titlecase(RussianLocale) }
    }

@Composable
private fun weekdaysOrDash(mask: Int): String = if (mask == 0) stringResource(R.string.repeat_no_days) else weekdaysText(mask)

/** «2, 15, последний день». */
@Composable
private fun monthDaysText(mask: Int): String {
    val lastDay = stringResource(R.string.repeat_last_day)
    return (1..31).filter { mask and (1 shl (it - 1)) != 0 }.joinToString(", ") { if (it == 31) lastDay else it.toString() }
}

/** Сохраняет окно при повороте экрана и выгрузке процесса: все поля — числами. */
private val RepeatEditorSaver = listSaver<RepeatEditor, Long>(
    save = {
        listOf(
            it.tab.ordinal.toLong(), it.weekdays.toLong(), if (it.twoWeeks) 1L else 0L, it.nextWeekdays.toLong(),
            it.weekEvery.toLong(), it.monthDays.toLong(), it.monthEvery.toLong(), it.every.toLong(), it.unit.code.toLong(),
            it.until?.toEpochDay() ?: Long.MIN_VALUE, it.nextDate.toEpochDay(), it.base.toEpochDay(), it.today.toEpochDay()
        )
    },
    restore = {
        RepeatEditor(
            tab = RepeatTab.entries[it[0].toInt()],
            weekdays = it[1].toInt(),
            twoWeeks = it[2] == 1L,
            nextWeekdays = it[3].toInt(),
            weekEvery = it[4].toInt(),
            monthDays = it[5].toInt(),
            monthEvery = it[6].toInt(),
            every = it[7].toInt(),
            unit = RepeatUnit.fromCode(it[8].toInt()),
            until = it[9].takeIf { day -> day != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay),
            nextDate = LocalDate.ofEpochDay(it[10]),
            base = LocalDate.ofEpochDay(it[11]),
            today = LocalDate.ofEpochDay(it[12])
        )
    }
)

private val RussianLocale: Locale = Locale.forLanguageTag("ru")

// Биты с понедельника по пятницу и выходные (бит 0 — понедельник)
private const val MONDAY_TO_FRIDAY = 0b0011111
private const val WEEKEND = 0b1100000

@Preview(widthDp = 412, heightDp = 760)
@Composable
private fun RepeatContentPreview() {
    ChekkiTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            val today = LocalDate.of(2026, 11, 23)
            RepeatContent(
                editor = RepeatEditor.of(
                    RepeatRule(RepeatUnit.TwoWeeks, every = 2, weekdays = 0b1001010, weekdays2 = 0b0000001),
                    taskDate = LocalDate.of(2026, 11, 24),
                    today = today
                ),
                canClear = true,
                onChange = {},
                onPickNextDate = {},
                onPickUntil = {},
                onClose = {},
                onClear = {},
                onDone = {}
            )
        }
    }
}
