package com.snax1k.chekki.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Уровень важности (приоритета) — свой у пользователя (решено 29.10.2026, схема 6): название, цвет флажка
 * (ARGB, непрозрачный) и место в списке. Чем выше место ([position] меньше), тем важнее: по этому порядку
 * сортируются задачи. У задачи в колонке `priority` — id уровня, 0 — без приоритета.
 */
@Entity(tableName = "priority_levels")
data class PriorityLevelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val position: Int
)

/** Уровни по умолчанию: такие же, как три приоритета до схемы 6, с теми же id — старые задачи не меняются. */
object PriorityLevels {
    const val NONE = 0L
    const val LOW = 1L
    const val MEDIUM = 2L
    const val HIGH = 3L

    /** Больше уровней не нужно: флажки перестанут различаться на глаз. */
    const val MAX = 8

    /** Высокий сверху; низкий — зелёный (до 29.10.2026 был синим, как категория «Работа»). */
    val defaults = listOf(
        PriorityLevelEntity(id = HIGH, name = "Высокий", color = 0xFFFF8A7A.toInt(), position = 0),
        PriorityLevelEntity(id = MEDIUM, name = "Средний", color = 0xFFF5BE6A.toInt(), position = 1),
        PriorityLevelEntity(id = LOW, name = "Низкий", color = 0xFF7BD49A.toInt(), position = 2)
    )
}

@Dao
interface PriorityLevelDao {
    /** Все уровни, самый важный первым. */
    @Query("SELECT * FROM priority_levels ORDER BY position, id")
    fun observeAll(): Flow<List<PriorityLevelEntity>>

    @Query("SELECT * FROM priority_levels ORDER BY position, id")
    suspend fun getAll(): List<PriorityLevelEntity>

    @Query("SELECT * FROM priority_levels WHERE id = :id")
    suspend fun get(id: Long): PriorityLevelEntity?

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM priority_levels")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(level: PriorityLevelEntity): Long

    @Update
    suspend fun update(level: PriorityLevelEntity)

    @Query("UPDATE priority_levels SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM priority_levels WHERE id = :id")
    suspend fun delete(id: Long)

    /** Задачи уровня — чтобы вернуть им уровень, если удаление отменят. */
    @Query("SELECT id FROM tasks WHERE priority = :id")
    suspend fun getTaskIds(id: Long): List<Long>

    /** Удалили уровень — его задачи становятся без приоритета. */
    @Query("UPDATE tasks SET priority = 0 WHERE priority = :id")
    suspend fun clearFromTasks(id: Long)

    @Query("UPDATE tasks SET priority = :levelId WHERE id IN (:taskIds)")
    suspend fun setOnTasks(levelId: Long, taskIds: List<Long>)
}
