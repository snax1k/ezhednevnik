package com.snax1k.chekki.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Линейный градиент с углом, как в CSS `linear-gradient(<угол>deg, ...)`: 90° — слева направо,
 * 180° — сверху вниз. Так градиенты из макета переносятся без пересчёта.
 */
fun cssLinearGradient(angleDegrees: Float, start: Color, end: Color): Brush =
    CssLinearGradient(angleDegrees, start, end)

// data class, чтобы одинаковые градиенты были равны и не вызывали лишней перерисовки
private data class CssLinearGradient(
    val angleDegrees: Float,
    val start: Color,
    val end: Color
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        // Длина линии градиента по правилам CSS: крайние цвета попадают точно в углы фигуры
        val halfLength = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val center = size.center
        return LinearGradientShader(
            from = Offset(center.x - dx * halfLength, center.y - dy * halfLength),
            to = Offset(center.x + dx * halfLength, center.y + dy * halfLength),
            colors = listOf(start, end)
        )
    }
}
