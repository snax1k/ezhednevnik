package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Вложение задачи (схема 7, решено 29.10.2026): фото, видео, аудио или любой файл. Сам файл лежит во внутренней
 * папке приложения `files/attachments` под именем [fileName]; [displayName] — как его звали у пользователя.
 * Удалили задачу — строки удаляются вместе с ней, а файлы без строк убирает AttachmentStore
 * при следующем запуске (так «Отменить» успевает вернуть задачу с вложениями).
 * У копии повторяющейся задачи свои строки на те же файлы.
 */
@Entity(
    tableName = "attachments",
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
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    val kind: AttachmentKind,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    /** Размер файла в байтах. */
    val size: Long,
    /** Длительность аудио и видео; null — нет или не удалось узнать. */
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    val position: Int
)

/** Вид вложения. Коды фиксированы, как у других перечислений в базе: менять нельзя, новые — с новыми кодами. */
enum class AttachmentKind(val code: Int) {
    Photo(1),
    Video(2),
    Audio(3),
    Document(4);

    companion object {
        fun fromCode(code: Int): AttachmentKind = entries.firstOrNull { it.code == code } ?: Document

        /** Вид по типу файла: «image/…» — фото, «video/…» — видео, «audio/…» — аудио, остальное — документ. */
        fun ofMimeType(mimeType: String?): AttachmentKind = when {
            mimeType == null -> Document
            mimeType.startsWith("image/") -> Photo
            mimeType.startsWith("video/") -> Video
            mimeType.startsWith("audio/") -> Audio
            else -> Document
        }
    }
}
