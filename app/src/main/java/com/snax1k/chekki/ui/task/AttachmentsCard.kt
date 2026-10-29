package com.snax1k.chekki.ui.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.AttachmentEntity
import com.snax1k.chekki.data.db.AttachmentKind
import com.snax1k.chekki.ui.icons.ChekkiIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Чем пополнить вложения: пункты меню «Добавить вложение». */
enum class AttachmentSource { Gallery, Photo, Video, Voice, File }

/**
 * Блок «Вложения» в карточке задачи (решено 29.10.2026): фото и видео — миниатюрами в ряд (видео со значком
 * и длительностью), голосовые и другие аудио — строками с «Слушать», остальные файлы — строками с размером.
 * Нажатие открывает файл в подходящем приложении (аудио играет прямо здесь), «×» удаляет с «Отменить».
 * Внизу «Добавить вложение» — меню: галерея, снять фото, снять видео, записать голос, файл.
 */
@Composable
internal fun AttachmentsCard(
    attachments: List<AttachmentEntity>,
    fileOf: (AttachmentEntity) -> File,
    onOpen: (AttachmentEntity) -> Unit,
    onDelete: (AttachmentEntity) -> Unit,
    onAdd: (AttachmentSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = attachments.filter { it.kind == AttachmentKind.Photo || it.kind == AttachmentKind.Video }
    val audio = attachments.filter { it.kind == AttachmentKind.Audio }
    val documents = attachments.filter { it.kind == AttachmentKind.Document }
    val player = rememberAudioPlayer()
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .heightIn(min = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.attachments_title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (attachments.isNotEmpty()) {
                Text(
                    attachments.size.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (visual.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visual.forEach { attachment ->
                    MediaTile(attachment, fileOf(attachment), onOpen = { onOpen(attachment) }, onDelete = { onDelete(attachment) })
                }
            }
        }
        Column(Modifier.padding(top = if (audio.isEmpty() && documents.isEmpty()) 0.dp else 8.dp)) {
            audio.forEach { attachment ->
                AudioRow(
                    attachment = attachment,
                    isPlaying = player.playingId == attachment.id,
                    positionMs = if (player.playingId == attachment.id) player.positionMs else 0L,
                    onToggle = { player.toggle(attachment.id, fileOf(attachment)) },
                    onDelete = {
                        player.stopIf(attachment.id)
                        onDelete(attachment)
                    }
                )
            }
            documents.forEach { attachment ->
                DocumentRow(attachment, onOpen = { onOpen(attachment) }, onDelete = { onDelete(attachment) })
            }
        }
        AddAttachmentButton(onAdd, Modifier.padding(start = 2.dp, end = 4.dp, bottom = 4.dp))
    }
}

/** Миниатюра фото или видео 96 dp; у видео — треугольник и длительность. */
@Composable
private fun MediaTile(attachment: AttachmentEntity, file: File, onOpen: () -> Unit, onDelete: () -> Unit) {
    val thumbnail by produceState<ImageBitmap?>(null, file) { value = loadThumbnail(file, attachment.kind) }
    val isVideo = attachment.kind == AttachmentKind.Video
    val label = stringResource(
        if (isVideo) R.string.attachments_video else R.string.attachments_photo,
        attachment.displayName
    )
    Box(
        Modifier
            .size(96.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onOpen)
            .semantics { contentDescription = label }
    ) {
        thumbnail?.let { Image(it, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (isVideo) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(ChekkiIcons.Play, contentDescription = null, Modifier.size(12.dp), tint = Color.White)
                attachment.durationMs?.let {
                    Text(formatDuration(it), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        RemoveButton(attachment, onDelete, Modifier.align(Alignment.TopEnd), onImage = true)
    }
}

/** Аудио: «Слушать»/«Пауза», название, время «0:12 / 1:03», «×». */
@Composable
private fun AudioRow(
    attachment: AttachmentEntity,
    isPlaying: Boolean,
    positionMs: Long,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val label = stringResource(R.string.attachments_play, attachment.displayName)
    val pauseLabel = stringResource(R.string.attachments_pause)
    AttachmentRow(
        icon = if (isPlaying) ChekkiIcons.Pause else ChekkiIcons.Play,
        iconDescription = if (isPlaying) pauseLabel else label,
        title = attachment.displayName,
        subtitle = attachment.durationMs?.let { total ->
            if (isPlaying || positionMs > 0) "${formatDuration(positionMs)} / ${formatDuration(total)}" else formatDuration(total)
        },
        onClick = onToggle,
        trailing = { RemoveButton(attachment, onDelete) }
    )
}

@Composable
private fun DocumentRow(attachment: AttachmentEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    AttachmentRow(
        icon = ChekkiIcons.Attachment,
        iconDescription = stringResource(R.string.attachments_open, attachment.displayName),
        title = attachment.displayName,
        subtitle = formatSize(attachment.size),
        onClick = onOpen,
        trailing = { RemoveButton(attachment, onDelete) }
    )
}

@Composable
private fun AttachmentRow(
    icon: ImageVector,
    iconDescription: String,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = iconDescription, onClick = onClick)
            .padding(start = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

/** «×» удаления. На миниатюре — в тёмном кружке, чтобы был виден на любой картинке. Зона нажатия 40 dp. */
@Composable
private fun RemoveButton(attachment: AttachmentEntity, onDelete: () -> Unit, modifier: Modifier = Modifier, onImage: Boolean = false) {
    val label = stringResource(R.string.attachments_delete, attachment.displayName)
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onDelete)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (onImage) Color.Black.copy(alpha = 0.55f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ChekkiIcons.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (onImage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** «+ Добавить вложение» и меню источников. */
@Composable
private fun AddAttachmentButton(onAdd: (AttachmentSource) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = { expanded = true })
                .padding(PaddingValues(start = 14.dp)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ChekkiIcons.Add, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.attachments_add), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            listOf(
                AttachmentSource.Gallery to (R.string.attachments_from_gallery to ChekkiIcons.Gallery),
                AttachmentSource.Photo to (R.string.attachments_take_photo to ChekkiIcons.CategoryCamera),
                AttachmentSource.Video to (R.string.attachments_take_video to ChekkiIcons.CategoryMovie),
                AttachmentSource.Voice to (R.string.attachments_record_audio to ChekkiIcons.Microphone),
                AttachmentSource.File to (R.string.attachments_pick_file to ChekkiIcons.Attachment)
            ).forEach { (source, item) ->
                DropdownMenuItem(
                    text = { Text(stringResource(item.first)) },
                    leadingIcon = { Icon(item.second, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        expanded = false
                        onAdd(source)
                    }
                )
            }
        }
    }
}

/** Проигрыватель аудио карточки: играет одно вложение за раз, позиция обновляется четыре раза в секунду. */
private class AudioPlayer {
    var playingId by mutableStateOf<Long?>(null)
        private set
    var positionMs by mutableLongStateOf(0L)
    private var player: MediaPlayer? = null

    fun toggle(id: Long, file: File) {
        if (playingId == id) {
            stop()
            return
        }
        stop()
        player = try {
            MediaPlayer().apply {
                setDataSource(file.path)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            null
        }
        if (player != null) playingId = id
    }

    fun stopIf(id: Long) {
        if (playingId == id) stop()
    }

    /** Заодно ловит конец записи: на части устройств `OnCompletionListener` не приходит. */
    fun updatePosition() {
        val current = player ?: return
        if (!current.isPlaying) stop() else positionMs = current.currentPosition.toLong()
    }

    fun stop() {
        player?.release()
        player = null
        playingId = null
        positionMs = 0L
    }
}

@Composable
private fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { AudioPlayer() }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    LaunchedEffect(player.playingId) {
        while (player.playingId != null) {
            player.updatePosition()
            delay(250)
        }
    }
    return player
}

/** Миниатюра ~256 px: с Android 10 — системная (учитывает поворот снимка), раньше — уменьшенная копия. */
private suspend fun loadThumbnail(file: File, kind: AttachmentKind): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (kind == AttachmentKind.Video) {
                ThumbnailUtils.createVideoThumbnail(file, Size(256, 256), null)
            } else {
                ThumbnailUtils.createImageThumbnail(file, Size(256, 256), null)
            }
        } else if (kind == AttachmentKind.Video) {
            MediaMetadataRetriever().run {
                try {
                    setDataSource(file.path)
                    getFrameAtTime(0)
                } finally {
                    release()
                }
            }
        } else {
            decodeSampledPhoto(file)
        }
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

private fun decodeSampledPhoto(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 256 && bounds.outHeight / (sample * 2) >= 256) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
}

/** «1:03», с часом — «1:02:03». */
internal fun formatDuration(millis: Long): String {
    val total = millis / 1000
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** «820 КБ», «2,3 МБ». */
internal fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"
    bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} КБ"
    else -> String.format(Locale.forLanguageTag("ru"), "%.1f МБ", bytes / (1024.0 * 1024.0))
}
