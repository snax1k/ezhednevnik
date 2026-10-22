package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Категория задач. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Номер цвета в палитре категорий темы, а не сам цвет: так цвет подстроится под светлую тему. */
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    /** Порядок в списке категорий. */
    val position: Int
)
