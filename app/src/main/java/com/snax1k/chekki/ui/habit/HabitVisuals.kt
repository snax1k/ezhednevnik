package com.snax1k.chekki.ui.habit

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.bit
import com.snax1k.chekki.ui.icons.ChekkiIcons
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Картинка иконки привычки. */
val HabitIcon.vector: ImageVector
    get() = when (this) {
        HabitIcon.Sprout -> ChekkiIcons.Habits
        HabitIcon.Water -> ChekkiIcons.Water
        HabitIcon.Goal -> ChekkiIcons.Goals
        HabitIcon.Stretch -> ChekkiIcons.Stretching
        HabitIcon.Run -> ChekkiIcons.Run
        HabitIcon.Book -> ChekkiIcons.Book
        HabitIcon.Pill -> ChekkiIcons.Pill
        HabitIcon.Sleep -> ChekkiIcons.Sleep
        HabitIcon.Tooth -> ChekkiIcons.Tooth
        HabitIcon.Shower -> ChekkiIcons.Shower
        HabitIcon.Heart -> ChekkiIcons.Heart
        HabitIcon.Sun -> ChekkiIcons.Sun
    }

/** Название иконки для TalkBack. */
@get:StringRes
val HabitIcon.label: Int
    get() = when (this) {
        HabitIcon.Sprout -> R.string.habit_icon_sprout
        HabitIcon.Water -> R.string.habit_icon_water
        HabitIcon.Goal -> R.string.habit_icon_goal
        HabitIcon.Stretch -> R.string.habit_icon_stretch
        HabitIcon.Run -> R.string.habit_icon_run
        HabitIcon.Book -> R.string.habit_icon_book
        HabitIcon.Pill -> R.string.habit_icon_pill
        HabitIcon.Sleep -> R.string.habit_icon_sleep
        HabitIcon.Tooth -> R.string.habit_icon_tooth
        HabitIcon.Shower -> R.string.habit_icon_shower
        HabitIcon.Heart -> R.string.habit_icon_heart
        HabitIcon.Sun -> R.string.habit_icon_sun
    }

/** Названия цветов палитры привычек по порядку (`ChekkiTheme.colors.habits`) — для TalkBack. */
val HabitColorLabels: List<Int> = listOf(
    R.string.habit_color_light_blue,
    R.string.habit_color_blue,
    R.string.habit_color_teal,
    R.string.habit_color_orange,
    R.string.habit_color_raspberry,
    R.string.habit_color_violet,
    R.string.habit_color_green,
    R.string.habit_color_mustard
)

/** «Каждый день», «По будням», «По выходным» или дни через запятую: «Пн, Ср, Пт». */
@Composable
fun daysSummary(weekdays: Int): String = when (HabitDaysPreset.of(weekdays)) {
    HabitDaysPreset.EveryDay -> stringResource(R.string.habit_days_every_day)
    HabitDaysPreset.Weekdays -> stringResource(R.string.habit_days_weekdays)
    HabitDaysPreset.Weekend -> stringResource(R.string.habit_days_weekend)
    null -> DayOfWeek.entries
        .filter { weekdays and it.bit != 0 }
        .joinToString(", ") { shortDayName(it) }
}

/** «Пн», «Вт», … — всегда по-русски. */
fun shortDayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT_STANDALONE, RussianLocale).replaceFirstChar { it.titlecase(RussianLocale) }

private val RussianLocale: Locale = Locale.forLanguageTag("ru")
