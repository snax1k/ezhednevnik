package com.snax1k.chekki.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.snax1k.chekki.data.db.AttachmentKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Файл вложения уже лежит у приложения; осталось записать строку в базу. */
data class StoredFile(
    val fileName: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val durationMs: Long?,
    val kind: AttachmentKind
)

/** Файл слишком большой, чтобы хранить его в приложении. */
class AttachmentTooLargeException : Exception()

/**
 * Файлы вложений (решено 29.10.2026): во внутренней папке приложения `files/attachments`, под случайными именами.
 * Выбранное в галерее или «Файлах» копируется сюда — так вложение не пропадёт, даже если исходник удалят.
 * Другим приложениям (просмотр, камера) файлы отдаются через FileProvider (`res/xml/file_paths.xml`).
 */
class AttachmentStore(context: Context) {

    private val appContext = context.applicationContext
    private val authority = "${appContext.packageName}.files"

    val directory: File get() = File(appContext.filesDir, DIRECTORY).apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    /** Ссылка на файл для других приложений: открыть, отдать камере. */
    fun uriFor(fileName: String): Uri = FileProvider.getUriForFile(appContext, authority, file(fileName))

    /** Пустой файл под снимок камеры или запись голоса. */
    fun newFile(extension: String): File = File(directory, "${UUID.randomUUID()}.$extension")

    /**
     * Папка переноса из старого приложения: фото и аудио копии лежат здесь от чтения копии до «Перенести».
     * Она на том же диске, что и вложения, поэтому [adopt] переносит файл без копирования.
     */
    val importDirectory: File get() = File(appContext.filesDir, IMPORT_DIRECTORY)

    /** Убирает папку переноса: перенос закончен или брошен. */
    fun clearImportDirectory() {
        importDirectory.deleteRecursively()
    }

    /** Файл из папки переноса становится вложением: переезжает к остальным под случайным именем. Нет файла — null. */
    fun adopt(file: File, displayName: String, mimeType: String): StoredFile? {
        if (!file.exists()) return null
        val extension = file.name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 5 } ?: "bin"
        val target = newFile(extension)
        if (!file.renameTo(target)) {
            file.copyTo(target)
            file.delete()
        }
        return describe(target, displayName, mimeType)
    }

    /** Копирует выбранное в галерее или «Файлах» к себе. Больше [MAX_SIZE] — [AttachmentTooLargeException]. */
    suspend fun copyFrom(uri: Uri): StoredFile = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        var displayName: String? = null
        var declaredSize = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(0)
                if (!cursor.isNull(1)) declaredSize = cursor.getLong(1)
            }
        }
        if (declaredSize > MAX_SIZE) throw AttachmentTooLargeException()
        val mimeType = resolver.getType(uri) ?: mimeTypeOf(displayName) ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: displayName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it.length <= 5 }
            ?: "bin"
        val target = newFile(extension)
        try {
            val input = resolver.openInputStream(uri) ?: throw IllegalStateException("Файл не открылся")
            input.use { source ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > MAX_SIZE) throw AttachmentTooLargeException()
                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        describe(target, displayName ?: target.name, mimeType)
    }

    /** Описание файла, который уже лежит в папке (снимок камеры, запись голоса, скопированное). */
    fun describe(file: File, displayName: String, mimeType: String): StoredFile {
        val kind = AttachmentKind.ofMimeType(mimeType)
        return StoredFile(
            fileName = file.name,
            displayName = displayName,
            mimeType = mimeType,
            size = file.length(),
            durationMs = if (kind == AttachmentKind.Audio || kind == AttachmentKind.Video) durationOf(file) else null,
            kind = kind
        )
    }

    /**
     * Удаляет файлы, на которые нет строк в базе ([referenced]): от удалённых задач и вложений, от брошенных
     * снимков. Свежие (младше [minAgeMillis]) не трогаем — их, может быть, только что сняли или вернут «Отменить».
     */
    suspend fun cleanupOrphans(referenced: Set<String>, minAgeMillis: Long = 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        directory.listFiles()?.forEach { file ->
            if (file.name !in referenced && now - file.lastModified() > minAgeMillis) {
                if (!file.delete()) Log.w(TAG, "Не удалился лишний файл ${file.name}")
            }
        }
    }

    private fun durationOf(file: File): Long? = try {
        MediaMetadataRetriever().run {
            try {
                setDataSource(file.path)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                release()
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun mimeTypeOf(name: String?): String? =
        name?.substringAfterLast('.', "")?.lowercase()?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }

    companion object {
        private const val TAG = "AttachmentStore"
        private const val DIRECTORY = "attachments"
        private const val IMPORT_DIRECTORY = "old-app-import"

        /** Больше — не храним: телефон быстро заполнится, а копия базы и перенос станут тяжёлыми. */
        const val MAX_SIZE = 300L * 1024 * 1024
    }
}
