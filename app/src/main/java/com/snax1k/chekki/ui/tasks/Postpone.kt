package com.snax1k.chekki.ui.tasks

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.addtask.QuickDate
import com.snax1k.chekki.ui.components.formatDayMonth
import com.snax1k.chekki.ui.components.formatShortDate
import com.snax1k.chekki.ui.icons.ChekkiIcons
import java.time.LocalDate

/** Готовые варианты «Отложить». Отсчёт всегда от сегодняшнего дня, а не от даты задачи. */
enum class PostponeOption(@param:StringRes val label: Int) {
    Tomorrow(R.string.quick_date_tomorrow),
    /** Ближайшая суббота, а в субботу и воскресенье — сегодня, как плашка «На выходных» в панели. */
    Weekend(R.string.quick_date_weekend),
    NextWeek(R.string.postpone_next_week);

    fun dateFor(today: LocalDate): LocalDate = when (this) {
        Tomorrow -> today.plusDays(1)
        Weekend -> QuickDate.Weekend.resolve(today) ?: today
        NextWeek -> today.plusWeeks(1)
    }

    companion object {
        /**
         * Варианты для задачи с датой [currentDate]: без того, что оставит задачу на месте,
         * и без повторов (в пятницу «Завтра» и «На выходных» — один и тот же день, остаётся «Завтра»).
         */
        fun available(today: LocalDate, currentDate: LocalDate?): List<Pair<PostponeOption, LocalDate>> =
            entries.map { it to it.dateFor(today) }
                .filter { (_, date) -> date != currentDate }
                .distinctBy { (_, date) -> date }
    }
}

/** Меню «Отложить»: варианты с датой справа и «Выбрать дату…», которое открывает календарь. */
@Composable
fun PostponeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    today: LocalDate,
    currentDate: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onPickDate: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        PostponeOption.available(today, currentDate).forEach { (option, date) ->
            DropdownMenuItem(
                text = { Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge) },
                trailingIcon = {
                    Text(
                        text = formatShortDate(date, today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    onDismiss()
                    onSelect(date)
                }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        DropdownMenuItem(
            text = { Text(stringResource(R.string.task_pick_date), style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = {
                Icon(ChekkiIcons.Calendar, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = {
                onDismiss()
                onPickDate()
            }
        )
    }
}

/**
 * «Задача отложена на завтра», «…на 24 октября» — текст плашки после переноса.
 * Не composable: плашку показывают из обработчика нажатия, а не при отрисовке.
 */
fun postponedMessage(context: Context, date: LocalDate, today: LocalDate): String {
    val whenText = when (date) {
        today -> context.getString(R.string.postponed_today)
        today.plusDays(1) -> context.getString(R.string.postponed_tomorrow)
        else -> formatDayMonth(date, today)
    }
    return context.getString(R.string.task_postponed, whenText)
}
