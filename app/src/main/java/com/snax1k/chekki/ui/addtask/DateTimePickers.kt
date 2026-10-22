package com.snax1k.chekki.ui.addtask

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Календарь: выбор даты, кнопки «Отмена» и «Далее» ([confirmLabel] — если после календаря
 * ничего не открывается, например «Готово» в «Отложить»).
 * Месяцы и дни недели по-русски и неделя с понедельника, даже если язык телефона другой.
 */
@Composable
fun TaskDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onNext: (LocalDate) -> Unit,
    @StringRes confirmLabel: Int = R.string.action_next
) {
    WithRussianLocale {
        val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.toPickerMillis())
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

/** Циферблат на 24 часа: кнопки «Весь день» (без времени) и «Готово». */
// TimePicker в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onAllDay: () -> Unit,
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
                TextButton(onClick = onAllDay) { Text(stringResource(R.string.task_all_day)) }
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
