package com.snax1k.chekki.ui.task

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.AttachmentEntity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Вложения карточки задачи вместе с окнами Android: галерея (без разрешений), камера для фото и видео
 * (снимок сразу пишется в папку вложений), «Файлы» и диктофон. Путь к файлу, который отдали камере,
 * переживает выгрузку приложения, пока камера открыта.
 */
@Composable
internal fun TaskAttachments(viewModel: TaskViewModel, onDeleted: (AttachmentEntity) -> Unit, modifier: Modifier = Modifier) {
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showError: (Int) -> Unit = { message -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    // Файл, который сейчас снимает камера, и как назвать вложение
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingIsVideo by rememberSaveable { mutableStateOf(false) }
    var isRecorderOpen by rememberSaveable { mutableStateOf(false) }

    val onCaptured: (Boolean) -> Unit = { success ->
        val path = pendingCapture
        pendingCapture = null
        if (path != null) {
            val file = File(path)
            if (success) {
                val (mime, name) = if (pendingIsVideo) "video/mp4" to R.string.attachments_name_video else "image/jpeg" to R.string.attachments_name_photo
                viewModel.addRecordedAttachment(file, mime, context.getString(name, stamp()))
            } else {
                file.delete()
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        if (uris.isNotEmpty()) viewModel.addAttachments(uris, showError)
    }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addAttachments(uris, showError)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), onCaptured)
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo(), onCaptured)

    fun capture(video: Boolean) {
        val file = viewModel.newAttachmentFile(if (video) "mp4" else "jpg")
        pendingCapture = file.path
        pendingIsVideo = video
        try {
            val uri = viewModel.uriForFile(file)
            if (video) takeVideo.launch(uri) else takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingCapture = null
            file.delete()
            showError(R.string.attachments_no_camera)
        }
    }

    AttachmentsCard(
        attachments = attachments,
        fileOf = viewModel::attachmentFile,
        onOpen = { attachment -> open(context, viewModel.attachmentUri(attachment), attachment.mimeType, showError) },
        onDelete = { attachment -> viewModel.deleteAttachment(attachment.id, onDeleted) },
        onAdd = { source ->
            when (source) {
                AttachmentSource.Gallery -> gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                AttachmentSource.Photo -> capture(video = false)
                AttachmentSource.Video -> capture(video = true)
                AttachmentSource.Voice -> isRecorderOpen = true
                AttachmentSource.File -> documents.launch(arrayOf("*/*"))
            }
        },
        modifier = modifier
    )

    if (isRecorderOpen) {
        VoiceRecorderDialog(
            newFile = { viewModel.newAttachmentFile("m4a") },
            onRecorded = { file ->
                isRecorderOpen = false
                viewModel.addRecordedAttachment(file, "audio/mp4", context.getString(R.string.attachments_name_voice, stamp()))
            },
            onDismiss = { isRecorderOpen = false }
        )
    }
}

/** Открыть вложение в подходящем приложении; права на чтение даются только на этот файл. */
private fun open(context: Context, uri: Uri, mimeType: String, onError: (Int) -> Unit) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        onError(R.string.attachments_no_viewer)
    }
}

/** «29.10 14:05» — в название снимка и записи. */
private fun stamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))

/** Сколько фото и видео можно выбрать в галерее за раз. */
private const val MAX_PICK = 20
