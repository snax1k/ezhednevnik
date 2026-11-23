package com.snax1k.chekki.ui.main

import com.snax1k.chekki.icon.SeasonalIconState
import com.snax1k.chekki.icon.DEFAULT_MASCOT
import com.snax1k.chekki.icon.Season
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.addtask.DragHandle
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.update.ManualCheck
import kotlinx.coroutines.launch

/** Строка обновлений в меню: установленная версия, итог проверки и найденная новая версия. */
data class UpdateMenuState(
    val installedVersion: String,
    val check: ManualCheck,
    /** Найдена версия новее установленной; null — не найдена. */
    val availableVersion: String?
)

/**
 * Меню ☰ главного экрана: «Привычки», «Уровни важности», «Перенести из старого приложения» и обновления. Макета нет.
 * Нажатие на строку обновлений проверяет их, а если новая версия уже найдена — возвращает её плашку.
 */
// ModalBottomSheet в Material 3 1.4 всё ещё помечен экспериментальным
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuSheet(
    onDismiss: () -> Unit,
    onHabits: () -> Unit,
    onImport: () -> Unit,
    onPriorityLevels: () -> Unit,
    update: UpdateMenuState,
    onCheckUpdates: () -> Unit,
    onShowUpdate: () -> Unit,
    seasonal: SeasonalIconState,
    onSeasonalToggle: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Сначала закрываем панель, потом открываем экран
    val then: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { DragHandle() }
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
        ) {
            MenuHeader(seasonal.season)
            MenuRow(ChekkiIcons.Habits, stringResource(R.string.menu_habits)) { then(onHabits) }
            MenuRow(ChekkiIcons.PriorityFlag, stringResource(R.string.menu_priority_levels)) { then(onPriorityLevels) }
            MenuRow(ChekkiIcons.Archive, stringResource(R.string.menu_import)) { then(onImport) }
            SeasonalIconRow(seasonal.enabled, onSeasonalToggle)
            UpdateRow(update, onClick = {
                // Версия уже найдена — закрываем меню и показываем плашку; иначе проверяем, меню остаётся открытым
                if (update.availableVersion != null) then(onShowUpdate) else onCheckUpdates()
            })
        }
    }
}

/** Шапка меню: маскот сезона (как на иконке) и название приложения; [season] null — обычный маскот. */
@Composable
private fun MenuHeader(season: Season?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(season?.mascot ?: DEFAULT_MASCOT),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Column {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(
                    when (season) {
                        Season.Winter -> R.string.season_winter
                        Season.Spring -> R.string.season_spring
                        Season.Summer -> R.string.season_summer
                        Season.Autumn -> R.string.season_autumn
                        null -> R.string.season_none
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** «Сезонная иконка»: переключатель на случай, если рабочий стол плохо переносит смену иконки. */
@Composable
private fun SeasonalIconRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.CategorySmile, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.menu_seasonal_icon),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(if (enabled) R.string.menu_seasonal_icon_on else R.string.menu_seasonal_icon_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            // Нажатие обрабатывает вся строка
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun UpdateRow(update: UpdateMenuState, onClick: () -> Unit) {
    val subtitle = when {
        update.availableVersion != null -> stringResource(R.string.menu_update_available, update.availableVersion)
        update.check == ManualCheck.Checking -> stringResource(R.string.menu_update_checking)
        update.check == ManualCheck.UpToDate -> stringResource(R.string.menu_update_up_to_date, update.installedVersion)
        update.check == ManualCheck.Failed -> stringResource(R.string.menu_update_failed)
        else -> stringResource(R.string.menu_update_version, update.installedVersion)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = update.check != ManualCheck.Checking, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            // Итог проверки зачитывается TalkBack сам
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Update, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(
                stringResource(R.string.menu_update),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (update.availableVersion != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
