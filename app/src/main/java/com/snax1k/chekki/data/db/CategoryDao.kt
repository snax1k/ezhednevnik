package com.snax1k.chekki.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    /** Все категории в порядке списка. */
    @Query("SELECT * FROM categories ORDER BY position, id")
    fun observeAll(): Flow<List<CategoryEntity>>
}
