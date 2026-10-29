package com.snax1k.chekki.ui.task

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlinx.coroutines.delay
import java.io.File

/**
 * Диктофон для вложения (решено 29.10.2026): большая кнопка «Записать» / «Стоп», время записи, «Отмена».
 * Разрешение на микрофон спрашивается, когда открыли окно. Запись — AAC в `.m4a` ([newFile] даёт файл
 * в папке вложений); после «Стоп» файл уходит в [onRecorded], при «Отмене» удаляется.
 */
@Composable
internal fun VoiceRecorderDialog(newFile: () -> File, onRecorded: (File) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        denied = !granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    val recorder = remember { VoiceRecorder(context) }
    var elapsed by remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) { onDispose { recorder.cancel() } }
    LaunchedEffect(recorder.isRecording) {
        while (recorder.isRecording) {
            elapsed = recorder.elapsedMs()
            delay(200)
        }
    }

    Dialog(onDismissRequest = {
        recorder.cancel()
        onDismiss()
    }) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.attachments_record_audio),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        denied -> stringResource(R.string.recorder_no_permission)
                        recorder.isRecording -> formatDuration(elapsed)
                        else -> stringResource(R.string.recorder_hint)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    style = if (recorder.isRecording) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                val buttonLabel = stringResource(if (recorder.isRecording) R.string.recorder_stop else R.string.recorder_start)
                val colors = ChekkiTheme.colors
                Box(
                    Modifier
                        .padding(vertical = 20.dp)
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(if (recorder.isRecording) colors.priorityHigh else MaterialTheme.colorScheme.primary)
                        .clickable(enabled = hasPermission, role = Role.Button, onClickLabel = buttonLabel) {
                            if (recorder.isRecording) {
                                recorder.stop()?.let(onRecorded)
                            } else {
                                recorder.start(newFile())
                            }
                        }
                        .semantics { contentDescription = buttonLabel },
                    contentAlignment = Alignment.Center
                ) {
                    if (recorder.isRecording) {
                        // Квадрат «Стоп»
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(colors.onFab)
                        )
                    } else {
                        Icon(ChekkiIcons.Microphone, contentDescription = null, Modifier.size(36.dp), tint = colors.onFab)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        recorder.cancel()
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

/** Запись голоса в файл. [cancel] останавливает и удаляет то, что успели записать. */
private class VoiceRecorder(private val context: Context) {
    var isRecording by mutableStateOf(false)
        private set
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    fun start(target: File) {
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(target.path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            newRecorder.release()
            target.delete()
            return
        }
        recorder = newRecorder
        file = target
        startedAt = System.currentTimeMillis()
        isRecording = true
    }

    fun elapsedMs(): Long = System.currentTimeMillis() - startedAt

    /** Записанный файл; null — записать не вышло (например, остановили сразу же). */
    fun stop(): File? {
        val current = recorder ?: return null
        val result = try {
            current.stop()
            file
        } catch (e: Exception) {
            file?.delete()
            null
        } finally {
            current.release()
            recorder = null
            isRecording = false
        }
        file = null
        return result
    }

    fun cancel() {
        val current = recorder ?: return
        try {
            current.stop()
        } catch (_: Exception) {
        }
        current.release()
        recorder = null
        isRecording = false
        file?.delete()
        file = null
    }
}
