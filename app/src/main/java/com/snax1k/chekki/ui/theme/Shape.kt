package com.snax1k.chekki.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Скругления по макету варианта Б. Круглые элементы (трекеры, «+», голосовой ввод) — CircleShape.
val Shapes = Shapes(
    // Квадрат под иконкой в карточке группы
    extraSmall = RoundedCornerShape(12.dp),
    // Кнопки «Меню» и «Статистика»
    small = RoundedCornerShape(16.dp),
    // Полосы групп, быстрые входы
    medium = RoundedCornerShape(18.dp),
    // Плитки групп
    large = RoundedCornerShape(22.dp),
    // Карточки «Просроченные» и «Сегодня», нижние панели
    extraLarge = RoundedCornerShape(24.dp)
)
