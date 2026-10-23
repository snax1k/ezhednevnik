package com.snax1k.chekki.ui.category

import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.data.db.CategoryIcon

/** Поля панели категории. */
data class CategoryDraft(
    val name: String = "",
    val icon: CategoryIcon = CategoryIcon.Tag,
    val colorIndex: Int = 0
) {
    val canSave: Boolean get() = name.isNotBlank()

    companion object {
        /** Сколько цветов в палитре категорий темы (`ChekkiTheme.colors.categories`). */
        const val COLOR_COUNT = 12

        fun of(category: CategoryEntity) = CategoryDraft(
            name = category.name,
            icon = category.icon,
            colorIndex = category.colorIndex
        )

        /** Цвет для новой категории: первый, который ещё ни у кого не занят; все заняты — по кругу. */
        fun suggestedColor(usedColors: Collection<Int>): Int =
            (0 until COLOR_COUNT).firstOrNull { it !in usedColors } ?: (usedColors.size % COLOR_COUNT)
    }
}
