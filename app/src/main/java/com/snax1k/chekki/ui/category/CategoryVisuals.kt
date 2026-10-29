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
        CategoryIcon.Coffee -> ChekkiIcons.CategoryCoffee
        CategoryIcon.Food -> ChekkiIcons.CategoryFood
        CategoryIcon.Fitness -> ChekkiIcons.CategoryFitness
        CategoryIcon.Run -> ChekkiIcons.Run
        CategoryIcon.Bike -> ChekkiIcons.CategoryBike
        CategoryIcon.Water -> ChekkiIcons.Water
        CategoryIcon.Pill -> ChekkiIcons.Pill
        CategoryIcon.Medical -> ChekkiIcons.CategoryMedical
        CategoryIcon.Tooth -> ChekkiIcons.Tooth
        CategoryIcon.Sleep -> ChekkiIcons.Sleep
        CategoryIcon.Umbrella -> ChekkiIcons.CategoryUmbrella
        CategoryIcon.Sun -> ChekkiIcons.Sun
        CategoryIcon.Cloud -> ChekkiIcons.CategoryCloud
        CategoryIcon.Leaf -> ChekkiIcons.CategoryLeaf
        CategoryIcon.Plant -> ChekkiIcons.CategoryPlant
        CategoryIcon.Pet -> ChekkiIcons.CategoryPet
        CategoryIcon.Family -> ChekkiIcons.CategoryFamily
        CategoryIcon.Smile -> ChekkiIcons.CategorySmile
        CategoryIcon.Cake -> ChekkiIcons.CategoryCake
        CategoryIcon.Book -> ChekkiIcons.Book
        CategoryIcon.Idea -> ChekkiIcons.CategoryIdea
        CategoryIcon.Target -> ChekkiIcons.CategoryTarget
        CategoryIcon.Flag -> ChekkiIcons.Goals
        CategoryIcon.Trophy -> ChekkiIcons.CategoryTrophy
        CategoryIcon.Calendar -> ChekkiIcons.Calendar
        CategoryIcon.Clock -> ChekkiIcons.Clock
        CategoryIcon.Bell -> ChekkiIcons.Reminder
        CategoryIcon.Mail -> ChekkiIcons.CategoryMail
        CategoryIcon.Chat -> ChekkiIcons.CategoryChat
        CategoryIcon.Laptop -> ChekkiIcons.CategoryLaptop
        CategoryIcon.Pen -> ChekkiIcons.Edit
        CategoryIcon.Building -> ChekkiIcons.CategoryBuilding
        CategoryIcon.Tools -> ChekkiIcons.CategoryTools
        CategoryIcon.Key -> ChekkiIcons.CategoryKey
        CategoryIcon.Lock -> ChekkiIcons.CategoryLock
        CategoryIcon.Bus -> ChekkiIcons.CategoryBus
        CategoryIcon.Place -> ChekkiIcons.CategoryPlace
        CategoryIcon.Globe -> ChekkiIcons.CategoryGlobe
        CategoryIcon.Camera -> ChekkiIcons.CategoryCamera
        CategoryIcon.Art -> ChekkiIcons.CategoryArt
        CategoryIcon.Game -> ChekkiIcons.CategoryGame
        CategoryIcon.Movie -> ChekkiIcons.CategoryMovie
        CategoryIcon.Clothes -> ChekkiIcons.CategoryClothes
        CategoryIcon.Money -> ChekkiIcons.CategoryMoney
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
        CategoryIcon.Coffee -> R.string.category_icon_coffee
        CategoryIcon.Food -> R.string.category_icon_food
        CategoryIcon.Fitness -> R.string.category_icon_fitness
        CategoryIcon.Run -> R.string.category_icon_run
        CategoryIcon.Bike -> R.string.category_icon_bike
        CategoryIcon.Water -> R.string.category_icon_water
        CategoryIcon.Pill -> R.string.category_icon_pill
        CategoryIcon.Medical -> R.string.category_icon_medical
        CategoryIcon.Tooth -> R.string.category_icon_tooth
        CategoryIcon.Sleep -> R.string.category_icon_sleep
        CategoryIcon.Umbrella -> R.string.category_icon_umbrella
        CategoryIcon.Sun -> R.string.category_icon_sun
        CategoryIcon.Cloud -> R.string.category_icon_cloud
        CategoryIcon.Leaf -> R.string.category_icon_leaf
        CategoryIcon.Plant -> R.string.category_icon_plant
        CategoryIcon.Pet -> R.string.category_icon_pet
        CategoryIcon.Family -> R.string.category_icon_family
        CategoryIcon.Smile -> R.string.category_icon_smile
        CategoryIcon.Cake -> R.string.category_icon_cake
        CategoryIcon.Book -> R.string.category_icon_book
        CategoryIcon.Idea -> R.string.category_icon_idea
        CategoryIcon.Target -> R.string.category_icon_target
        CategoryIcon.Flag -> R.string.category_icon_flag
        CategoryIcon.Trophy -> R.string.category_icon_trophy
        CategoryIcon.Calendar -> R.string.category_icon_calendar
        CategoryIcon.Clock -> R.string.category_icon_clock
        CategoryIcon.Bell -> R.string.category_icon_bell
        CategoryIcon.Mail -> R.string.category_icon_mail
        CategoryIcon.Chat -> R.string.category_icon_chat
        CategoryIcon.Laptop -> R.string.category_icon_laptop
        CategoryIcon.Pen -> R.string.category_icon_pen
        CategoryIcon.Building -> R.string.category_icon_building
        CategoryIcon.Tools -> R.string.category_icon_tools
        CategoryIcon.Key -> R.string.category_icon_key
        CategoryIcon.Lock -> R.string.category_icon_lock
        CategoryIcon.Bus -> R.string.category_icon_bus
        CategoryIcon.Place -> R.string.category_icon_place
        CategoryIcon.Globe -> R.string.category_icon_globe
        CategoryIcon.Camera -> R.string.category_icon_camera
        CategoryIcon.Art -> R.string.category_icon_art
        CategoryIcon.Game -> R.string.category_icon_game
        CategoryIcon.Movie -> R.string.category_icon_movie
        CategoryIcon.Clothes -> R.string.category_icon_clothes
        CategoryIcon.Money -> R.string.category_icon_money
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
