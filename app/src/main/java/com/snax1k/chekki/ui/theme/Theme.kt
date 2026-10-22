package com.snax1k.chekki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalChekkiColors = staticCompositionLocalOf { DarkChekkiColors }

// Светлой темы в варианте Б пока нет, поэтому приложение всегда в тёмной.
// Системные цвета (Material You) не используем: они заменили бы палитру макета.
@Composable
fun ChekkiTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalChekkiColors provides DarkChekkiColors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

object ChekkiTheme {
    /** Цвета групп, карточек, трекеров, категорий и приоритетов. */
    val colors: ChekkiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChekkiColors.current
}
