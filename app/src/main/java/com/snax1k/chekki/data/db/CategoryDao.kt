package com.snax1k.chekki.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CategoryDao {
    /** Все категории в порядке списка. */
    @Query("SELECT * FROM categories ORDER BY position, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** Категория для экрана её задач; null — её удалили. */
    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeCategory(id: Long): Flow<CategoryEntity?>

    /** Категории не в архиве для экрана «Категории»: невыполненные задачи и задачи на [today]. */
    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM tasks t WHERE t.category_id = c.id AND t.is_done = 0) AS open_count,
            (SELECT COUNT(*) FROM tasks t WHERE t.category_id = c.id AND t.date = :today) AS today_total,
            (SELECT COUNT(*) FROM tasks t WHERE t.category_id = c.id AND t.date = :today AND t.is_done = 1) AS today_done
        FROM categories c
        WHERE c.is_archived = 0
        ORDER BY c.position, c.id
        """
    )
    fun observeSummaries(today: LocalDate): Flow<List<CategorySummary>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): CategoryEntity?

    /** Цвета всех категорий — чтобы новой предложить ещё не занятый. */
    @Query("SELECT color_index FROM categories")
    suspend fun getColorIndexes(): List<Int>

    /** Место для новой категории — после последней. */
    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM categories")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name, icon = :icon, color_index = :colorIndex WHERE id = :id")
    suspend fun updateCategory(id: Long, name: String, icon: CategoryIcon, colorIndex: Int)

    /** Задачи категории — их нужно запомнить перед удалением, чтобы «Отменить» вернул им категорию. */
    @Query("SELECT id FROM tasks WHERE category_id = :categoryId")
    suspend fun getTaskIds(categoryId: Long): List<Long>

    /** Удаляет категорию; её задачи остаются без категории (внешний ключ SET NULL). */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("UPDATE tasks SET category_id = :categoryId WHERE id IN (:taskIds)")
    suspend fun setCategory(taskIds: List<Long>, categoryId: Long)

    /** Невыполненные задачи без категории. */
    @Query("SELECT COUNT(*) FROM tasks WHERE category_id IS NULL AND is_done = 0")
    fun observeUncategorizedOpenCount(): Flow<Int>
}
