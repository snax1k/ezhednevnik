package com.snax1k.chekki.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.cssLinearGradient
import com.snax1k.chekki.update.UpdateError
import com.snax1k.chekki.update.UpdateInfo
import com.snax1k.chekki.update.UpdateState

/**
 * Плашка обновления на главном экране, между заголовком и быстрыми входами. Макета нет:
 * карточка цвета поверхности с иконкой акцента, текстом и кнопками «Позже» / «Обновить».
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    modifier: Modifier = Modifier,
    onUpdate: () -> Unit = {},
    onLater: () -> Unit = {},
    onOpenPermission: () -> Unit = {},
    onDismissUpdated: () -> Unit = {}
) {
    // Пока плашка уезжает, на ней остаётся то, что было, а не пустота
    val lastShown = remember { LastShown() }
    if (state !is UpdateState.Idle) lastShown.state = state
    AnimatedVisibility(
        visible = state !is UpdateState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        BannerContent(lastShown.state, modifier.padding(bottom = 14.dp), onUpdate, onLater, onOpenPermission, onDismissUpdated)
    }
}

/** Что было на плашке в последний раз; не наблюдается Compose, нужно только на время исчезновения. */
private class LastShown {
    var state: UpdateState = UpdateState.Idle
}

@Composable
private fun BannerContent(
    state: UpdateState,
    modifier: Modifier,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onOpenPermission: () -> Unit,
    onDismissUpdated: () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            // Ход загрузки и итог зачитываются TalkBack сами
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(ChekkiIcons.Update, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title(state), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                text(state)?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                notes(state).forEach { note ->
                    Text(
                        "•  $note",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (state is UpdateState.Downloading) {
            DownloadProgress(state.progress, Modifier.padding(top = 14.dp))
        }
        BannerButtons(state, onUpdate, onLater, onOpenPermission, onDismissUpdated)
    }
}

@Composable
private fun title(state: UpdateState): String = when (state) {
    is UpdateState.Available -> stringResource(R.string.update_available_title, state.info.versionName)
    is UpdateState.Downloading -> stringResource(R.string.update_downloading_title, state.info.versionName)
    is UpdateState.NeedsPermission -> stringResource(R.string.update_permission_title)
    is UpdateState.Installing -> stringResource(R.string.update_installing_title)
    is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready_title, state.info.versionName)
    is UpdateState.Failed -> stringResource(R.string.update_failed_title, state.info.versionName)
    is UpdateState.Updated -> stringResource(R.string.update_updated_title, state.versionName)
    UpdateState.Idle -> ""
}

@Composable
private fun text(state: UpdateState): String? = when (state) {
    is UpdateState.Downloading -> stringResource(R.string.update_downloading_progress, (state.progress * 100).toInt())
    is UpdateState.NeedsPermission -> stringResource(R.string.update_permission_text)
    is UpdateState.Installing -> stringResource(R.string.update_installing_text)
    is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready_text)
    is UpdateState.Failed -> stringResource(
        when (state.error) {
            UpdateError.Network -> R.string.update_error_network
            UpdateError.Corrupted -> R.string.update_error_corrupted
            UpdateError.WrongApp -> R.string.update_error_wrong_app
            UpdateError.InstallFailed -> R.string.update_error_install
        }
    )
    is UpdateState.Updated -> stringResource(R.string.update_updated_text)
    is UpdateState.Available, UpdateState.Idle -> null
}

/** «Что нового» — не больше четырёх пунктов, чтобы плашка не занимала весь экран. */
private fun notes(state: UpdateState): List<String> = when (state) {
    is UpdateState.Available -> state.info.notes
    is UpdateState.Updated -> state.notes
    else -> emptyList()
}.take(4)

@Composable
private fun BannerButtons(
    state: UpdateState,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onOpenPermission: () -> Unit,
    onDismissUpdated: () -> Unit
) {
    val main: Pair<Int, () -> Unit>? = when (state) {
        is UpdateState.Available -> R.string.update_now to onUpdate
        is UpdateState.NeedsPermission -> R.string.update_permission_open to onOpenPermission
        is UpdateState.ReadyToInstall -> R.string.update_install to onUpdate
        // Файл чужой — повторять бессмысленно
        is UpdateState.Failed -> if (state.error == UpdateError.WrongApp) null else R.string.update_retry to onUpdate
        is UpdateState.Updated -> R.string.update_ok to onDismissUpdated
        else -> null
    }
    // Пока идёт загрузка или открыто окно Android, «Позже» не нужно
    val showLater = state !is UpdateState.Downloading && state !is UpdateState.Installing && state !is UpdateState.Updated
    if (main == null && !showLater) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLater) {
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClick = onLater)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.update_later), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (main != null) {
            val colors = ChekkiTheme.colors
            Box(
                Modifier
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(cssLinearGradient(135f, colors.fabGradientStart, colors.fabGradientEnd))
                    .clickable(role = Role.Button, onClick = main.second)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(main.first), style = MaterialTheme.typography.labelLarge, color = colors.onFab)
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

private val previewInfo = UpdateInfo(
    versionCode = 5,
    versionName = "0.5",
    apkUrl = "https://github.com/snax1k/ezhednevnik/releases/download/v0.5/Ezhednevnik-0.5.apk",
    sha256 = "0".repeat(64),
    size = 2_600_000,
    minSdk = 26,
    notes = listOf("Кружок задачи в цвете категории", "Задачи с приоритетом выше остальных")
)

@Preview(widthDp = 412)
@Composable
private fun UpdateBannerPreview() {
    ChekkiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                UpdateBanner(UpdateState.Available(previewInfo))
                UpdateBanner(UpdateState.Downloading(previewInfo, 0.45f))
                UpdateBanner(UpdateState.Failed(previewInfo, UpdateError.Network))
            }
        }
    }
}
