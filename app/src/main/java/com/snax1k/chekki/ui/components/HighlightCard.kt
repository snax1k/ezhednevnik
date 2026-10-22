package com.snax1k.chekki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snax1k.chekki.ui.theme.HighlightCardColors
import com.snax1k.chekki.ui.theme.cssLinearGradient

/** Яркая карточка с градиентом и цветной тенью («Просроченные», «Сегодня»). */
@Composable
fun HighlightCard(
    colors: HighlightCardColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(shape, Shadow(radius = 30.dp, color = colors.shadow, offset = DpOffset(0.dp, 10.dp)))
            .clip(shape)
            .background(cssLinearGradient(125f, colors.gradientStart, colors.gradientEnd))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

/** Иконка яркой карточки в полупрозрачном скруглённом квадрате. */
@Composable
fun CardIconBox(icon: ImageVector, colors: HighlightCardColors) {
    Box(
        Modifier
            .size(36.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(colors.iconContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = colors.content)
    }
}

/** Счётчик «выполнено/всего»: первое число крупно, «/всего» мельче и светлее. */
@Composable
fun ProgressCountText(done: Int, total: Int, colors: HighlightCardColors) {
    Text(
        text = buildAnnotatedString {
            append(done.toString())
            withStyle(SpanStyle(fontSize = 16.sp, color = colors.contentSecondary)) {
                append("/$total")
            }
        },
        style = MaterialTheme.typography.headlineMedium,
        color = colors.content
    )
}

/** Полоса прогресса на яркой карточке. */
@Composable
fun CardProgressBar(done: Int, total: Int, trackColor: Color, color: Color, modifier: Modifier = Modifier) {
    val progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}
