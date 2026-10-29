package com.snax1k.chekki.ui.category

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.CategoryIcon
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme

/** Картинка иконки категории. */
val CategoryIcon.vector: ImageVector
    get() = when (this) {
        CategoryIcon.Work -> ChekkiIcons.Work
        CategoryIcon.Personal -> ChekkiIcons.Personal
        CategoryIcon.Home -> ChekkiIcons.Home
        CategoryIcon.Health -> ChekkiIcons.Health
        CategoryIcon.Study -> ChekkiIcons.Study
        CategoryIcon.Finance -> ChekkiIcons.Finance
        CategoryIcon.Shopping -> ChekkiIcons.Shopping
        CategoryIcon.Car -> ChekkiIcons.Car
        CategoryIcon.Travel -> ChekkiIcons.Travel
        CategoryIcon.Gift -> ChekkiIcons.Gift
        CategoryIcon.Heart -> ChekkiIcons.Heart
        CategoryIcon.Star -> ChekkiIcons.Star
        CategoryIcon.Phone -> ChekkiIcons.Phone
        CategoryIcon.Code -> ChekkiIcons.Code
        CategoryIcon.Music -> ChekkiIcons.Music
        CategoryIcon.Tag -> ChekkiIcons.Tag
    }

/** Название иконки для TalkBack. */
@get:StringRes
val CategoryIcon.label: Int
    get() = when (this) {
        CategoryIcon.Work -> R.string.category_icon_work
        CategoryIcon.Personal -> R.string.category_icon_personal
        CategoryIcon.Home -> R.string.category_icon_home
        CategoryIcon.Health -> R.string.category_icon_health
        CategoryIcon.Study -> R.string.category_icon_study
        CategoryIcon.Finance -> R.string.category_icon_finance
        CategoryIcon.Shopping -> R.string.category_icon_shopping
        CategoryIcon.Car -> R.string.category_icon_car
        CategoryIcon.Travel -> R.string.category_icon_travel
        CategoryIcon.Gift -> R.string.category_icon_gift
        CategoryIcon.Heart -> R.string.category_icon_heart
        CategoryIcon.Star -> R.string.category_icon_star
        CategoryIcon.Phone -> R.string.category_icon_phone
        CategoryIcon.Code -> R.string.category_icon_code
        CategoryIcon.Music -> R.string.category_icon_music
        CategoryIcon.Tag -> R.string.category_icon_tag
    }

/** Названия цветов палитры категорий по порядку (`ChekkiTheme.colors.categories`) — для TalkBack. */
val CategoryColorLabels: List<Int> = listOf(
    R.string.category_color_blue,
    R.string.category_color_pink,
    R.string.category_color_amber,
    R.string.category_color_green,
    R.string.category_color_violet,
    R.string.category_color_teal,
    R.string.category_color_coral,
    R.string.category_color_lime,
    R.string.category_color_sky,
    R.string.category_color_lilac,
    R.string.category_color_sand,
    R.string.category_color_grey
)

/**
 * Цвет категории по коду из базы (`color_index`): 0–11 — номер в палитре темы, отрицательное число —
 * свой цвет ARGB (непрозрачный цвет как Int всегда отрицательный). Решено 29.10.2026 вместо новой колонки.
 */
@Composable
fun categoryColor(code: Int): Color {
    if (isCustomCategoryColor(code)) return Color(code)
    val palette = ChekkiTheme.colors.categories
    return palette[code.mod(palette.size)]
}

/** Код своего цвета для `color_index`: непрозрачный ARGB. */
fun customCategoryColorCode(color: Color): Int = color.toArgb() or 0xFF000000.toInt()

fun isCustomCategoryColor(code: Int): Boolean = code < 0
