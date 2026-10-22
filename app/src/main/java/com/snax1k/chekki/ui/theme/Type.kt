package com.snax1k.chekki.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.snax1k.chekki.R

// Manrope лежит одним переменным файлом (res/font/manrope.ttf, ось начертаний 200–800).
// Каждое нужное начертание объявлено отдельно: вес нужен и для подбора шрифта, и для оси.
// Font с variationSettings для ресурса пока экспериментальный (Compose 1.10). Без него переменный
// шрифт рисуется начертанием по умолчанию (200), а жирность система достраивает искусственно.
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Manrope = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold)
)

private fun manropeStyle(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit = 0.sp,
    // Цифры одинаковой ширины, чтобы счётчики и время не прыгали при смене значения
    tabularNumbers: Boolean = false
) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabularNumbers) "tnum" else null
)

// Размеры по макету варианта Б. В комментариях — где стиль используется на главном экране.
val Typography = Typography(
    // В макете нет, размеры Material
    displayLarge = manropeStyle(57.sp, 64.sp, FontWeight.ExtraBold),
    displayMedium = manropeStyle(45.sp, 52.sp, FontWeight.ExtraBold),
    displaySmall = manropeStyle(36.sp, 44.sp, FontWeight.ExtraBold),
    // Заголовок экрана
    headlineLarge = manropeStyle(30.sp, 36.sp, FontWeight.ExtraBold, letterSpacing = (-0.6).sp),
    // Счётчик в карточках «Просроченные» и «Сегодня»
    headlineMedium = manropeStyle(26.sp, 32.sp, FontWeight.ExtraBold, tabularNumbers = true),
    // Счётчик на плитке группы
    headlineSmall = manropeStyle(22.sp, 28.sp, FontWeight.ExtraBold, tabularNumbers = true),
    // Название карточки группы, счётчик на полосе группы
    titleLarge = manropeStyle(18.sp, 24.sp, FontWeight.ExtraBold, tabularNumbers = true),
    // Название полосы группы
    titleMedium = manropeStyle(16.sp, 22.sp, FontWeight.Bold),
    // Название плитки группы
    titleSmall = manropeStyle(15.sp, 20.sp, FontWeight.Bold),
    // Название задачи
    bodyLarge = manropeStyle(15.sp, 20.sp, FontWeight.SemiBold),
    // Второстепенный текст: первые задачи в карточке, дата над заголовком (там SemiBold)
    bodyMedium = manropeStyle(14.sp, 20.sp, FontWeight.Medium),
    // В макете нет
    bodySmall = manropeStyle(13.sp, 18.sp, FontWeight.Medium),
    // Кнопки, быстрые входы
    labelLarge = manropeStyle(14.sp, 20.sp, FontWeight.Bold),
    // Время задачи
    labelMedium = manropeStyle(13.sp, 16.sp, FontWeight.Bold, tabularNumbers = true),
    // Значение в круглом трекере
    labelSmall = manropeStyle(11.sp, 12.sp, FontWeight.ExtraBold, tabularNumbers = true)
)
