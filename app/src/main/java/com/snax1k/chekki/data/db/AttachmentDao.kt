package com.snax1k.chekki.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Вложения задач. */
@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE task_id = :taskId ORDER BY position, id")
    fun observeForTask(taskId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE task_id = :taskId ORDER BY position, id")
    suspend fun getForTask(taskId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun get(id: Long): AttachmentEntity?

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM attachments WHERE task_id = :taskId")
    suspend fun nextPosition(taskId: Long): Int

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Insert
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)

    /** Имена всех файлов, на которые есть строки: остальные файлы в папке — лишние. */
    @Query("SELECT DISTINCT file_name FROM attachments")
    suspend fun allFileNames(): List<String>
}
