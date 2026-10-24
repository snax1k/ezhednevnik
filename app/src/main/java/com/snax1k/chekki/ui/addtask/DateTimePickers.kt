package com.snax1k.chekki.ui.addtask

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.components.formatDayMonthShort
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Календарь: выбор даты, кнопки «Отмена» и «Далее» ([confirmLabel] — если после календаря
 * ничего не открывается, например «Готово» в «Отложить»). [firstSelectableDate] — раньше неё дни не выбираются.
 * Месяцы и дни недели по-русски и неделя с понедельника, даже если язык телефона другой.
 */
@Composable
fun TaskDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onNext: (LocalDate) -> Unit,
    @StringRes confirmLabel: Int = R.string.action_next,
    firstSelectableDate: LocalDate? = null
) {
    WithRussianLocale {
        val selectableDates = remember(firstSelectableDate) {
            if (firstSelectableDate == null) {
                DatePickerDefaults.AllDates
            } else {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toPickerDate() >= firstSelectableDate

                    override fun isSelectableYear(year: Int) = year >= firstSelectableDate.year
                }
            }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toPickerMillis(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { state.selectedDateMillis?.let { onNext(it.toPickerDate()) } },
                    enabled = state.selectedDateMillis != null
                ) {
                    Text(stringResource(confirmLabel))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            // Окно рисуется в отдельном окне со своей конфигурацией, поэтому русский язык задаём и здесь:
            // иначе крупная дата сверху выйдет на языке телефона
            WithRussianLocale {
                DatePicker(
                    state = state,
                    // Свой заголовок: стандартный Material берёт текст из языка телефона
                    title = {
                        Text(
                            text = stringResource(R.string.date_picker_title),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    // Только календарь, без ручного ввода даты
                    showModeToggle = false
                )
            }
        }
    }
}

/** Циферблат на 24 часа: кнопки «Весь день» (без времени) и «Готово». [onAllDay] null — вместо «Весь день» «Отмена». */
// TimePicker в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onAllDay: (() -> Unit)?,
    onDone: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    // Окно времени рисует фон слоем «поверхности», подкрашенным акцентом (коричневый оттенок).
    // Для этого окна поверхность — цвет окна календаря, без подкраски.
    val colorScheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = colorScheme.copy(surface = colorScheme.surfaceContainerHigh, surfaceTint = Color.Transparent)
    ) {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { onDone(LocalTime.of(state.hour, state.minute)) }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            title = { Text(stringResource(R.string.time_picker_title), style = MaterialTheme.typography.labelLarge) },
            dismissButton = {
                if (onAllDay != null) {
                    TextButton(onClick = onAllDay) { Text(stringResource(R.string.task_all_day)) }
                } else {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                }
            },
            containerColor = colorScheme.surfaceContainerHigh
        ) {
            TimePicker(
                state = state,
                // Циферблат темнее окна: в теме фон окна и стандартный цвет циферблата совпадают
                colors = TimePickerDefaults.colors(
                    clockDialColor = colorScheme.background,
                    timeSelectorUnselectedContainerColor = colorScheme.background
                )
            )
        }
    }
}

/** Ближайший целый час: время по умолчанию в циферблате. */
fun nextFullHour(): LocalTime = LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1)

/**
 * Выбор периода (фильтр поиска «Выбрать период…»): календарь с прокруткой месяцев, первый и последний день,
 * кнопки «Отмена» и «Готово». Как и [TaskDatePickerDialog], всегда по-русски и с понедельника.
 */
@Composable
fun PeriodPickerDialog(
    initialFrom: LocalDate?,
    initialTo: LocalDate?,
    onDismiss: () -> Unit,
    onDone: (from: LocalDate, to: LocalDate) -> Unit
) {
    WithRussianLocale {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialFrom?.toPickerMillis(),
            initialSelectedEndDateMillis = initialTo?.toPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                // Выбран один день — это период из одного дня
                val from = state.selectedStartDateMillis
                TextButton(
                    onClick = { from?.let { onDone(it.toPickerDate(), (state.selectedEndDateMillis ?: it).toPickerDate()) } },
                    enabled = from != null
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            WithRussianLocale {
                DateRangePicker(
                    state = state,
                    modifier = Modifier.weight(1f),
                    title = {
                        Text(
                            text = stringResource(R.string.period_picker_title),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    // Свой заголовок «24 окт – 31 окт»: стандартный пишет «Start date – End date» на языке телефона
                    headline = {
                        val from = state.selectedStartDateMillis?.toPickerDate()
                        val to = state.selectedEndDateMillis?.toPickerDate()
                        Text(
                            text = stringResource(
                                R.string.period_picker_headline,
                                from?.let(::formatDayMonthShort) ?: stringResource(R.string.period_picker_start),
                                to?.let(::formatDayMonthShort) ?: stringResource(R.string.period_picker_end)
                            ),
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    showModeToggle = false
                )
            }
        }
    }
}

@Composable
private fun WithRussianLocale(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val russian = remember(configuration) {
        Configuration(configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
    }
    CompositionLocalProvider(LocalConfiguration provides russian, content = content)
}

// Календарь Material хранит дату как полночь по UTC в миллисекундах
private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
