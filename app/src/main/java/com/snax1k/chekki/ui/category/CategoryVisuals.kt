package com.snax1k.chekki.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Цвет категории по номеру в палитре темы. */
@Composable
fun categoryColor(colorIndex: Int): Color {
    val palette = ChekkiTheme.colors.categories
    return palette[colorIndex.mod(palette.size)]
}
