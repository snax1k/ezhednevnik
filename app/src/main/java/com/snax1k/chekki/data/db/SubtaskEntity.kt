package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Подзадача. Удаляется вместе со своей задачей. */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("task_id")]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    val title: String,
    @ColumnInfo(name = "is_done") val isDone: Boolean = false,
    /** Порядок внутри задачи. */
    val position: Int
)
