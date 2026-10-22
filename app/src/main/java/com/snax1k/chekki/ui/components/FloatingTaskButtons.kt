package com.snax1k.chekki.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.cssLinearGradient

/** Отступ снизу у прокручиваемого содержимого, чтобы оно не пряталось за плавающими кнопками. */
val FloatingButtonsAreaHeight = 120.dp

/**
 * На сколько поднять плавающие кнопки: снизу может появиться плашка «Задача удалена · Отменить»,
 * и кнопки уходят над ней, а не прячутся под неё (как в макете «Состояния и жесты»).
 */
val LocalFloatingButtonsLift = compositionLocalOf { 0.dp }

/** Плавающие кнопки внизу справа: голосовой ввод и «+». */
@Composable
fun FloatingTaskButtons(onVoiceClick: () -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ChekkiTheme.colors
    val lift by animateDpAsState(LocalFloatingButtonsLift.current, label = "floatingButtonsLift")
    Row(
        modifier.offset { IntOffset(0, -lift.roundToPx()) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(56.dp)
                .dropShadow(CircleShape, Shadow(radius = 20.dp, color = colors.floatingShadow, offset = DpOffset(0.dp, 6.dp)))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onVoiceClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ChekkiIcons.Microphone,
                stringResource(R.string.main_voice_input),
                Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box(
            Modifier
                .size(72.dp)
                .dropShadow(CircleShape, Shadow(radius = 30.dp, color = colors.fabShadow, offset = DpOffset(0.dp, 12.dp)))
                .clip(CircleShape)
                .background(cssLinearGradient(135f, colors.fabGradientStart, colors.fabGradientEnd))
                .clickable(role = Role.Button, onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(ChekkiIcons.Add, stringResource(R.string.main_add_task), Modifier.size(32.dp), tint = colors.onFab)
        }
    }
}

/** Затемнение внизу экрана, на фоне которого стоят плавающие кнопки. */
@Composable
fun BottomFade(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0f),
                    0.55f to background.copy(alpha = 0.92f),
                    1f to background.copy(alpha = 0.92f)
                )
            )
            .navigationBarsPadding()
            .height(100.dp)
    )
}
