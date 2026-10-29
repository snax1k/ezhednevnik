package com.snax1k.chekki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.theme.ChekkiTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Выбор своего цвета (решено 29.10.2026): круг цветов (по кругу — оттенок, к краю — насыщенность),
 * ползунок яркости, поля HEX и R, G, B. Все способы связаны: крутите круг — меняются цифры, вписали цифры —
 * точка на круге переезжает. Цвет всегда непрозрачный.
 */
@Composable
fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val start = remember(initial) { colorToHsv(initial) }
    var hue by rememberSaveable { mutableFloatStateOf(start[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(start[1]) }
    var value by rememberSaveable { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, saturation, value)
    val setColor: (Color) -> Unit = { picked ->
        val hsv = colorToHsv(picked)
        // У серых оттенков нет своего «оттенка» — оставляем прежний, чтобы точка не прыгала
        if (hsv[1] > 0f && hsv[2] > 0f) hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    stringResource(R.string.color_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HueSaturationWheel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { h, s ->
                        hue = h
                        saturation = s
                        // Тянут по кругу совсем тёмный цвет — делаем его видимым, иначе ничего не меняется
                        if (value < 0.2f) value = 1f
                    },
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    stringResource(R.string.color_picker_brightness),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.hsv(hue, saturation, 1f),
                        activeTrackColor = Color.hsv(hue, saturation, 1f)
                    )
                )
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    NumberField(
                        label = "HEX",
                        text = colorToHex(color),
                        keyboard = KeyboardType.Ascii,
                        width = 104,
                        parse = ::parseHex,
                        onParsed = setColor
                    )
                }
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rgb = colorToRgb(color)
                    listOf("R", "G", "B").forEachIndexed { channel, label ->
                        NumberField(
                            label = label,
                            text = rgb[channel].toString(),
                            keyboard = KeyboardType.Number,
                            width = 64,
                            parse = { text ->
                                text.toIntOrNull()?.takeIf { it in 0..255 }?.let { number ->
                                    val channels = rgb.copyOf().also { it[channel] = number }
                                    Color(channels[0], channels[1], channels[2])
                                }
                            },
                            onParsed = setColor
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { onPick(color) }) { Text(stringResource(R.string.action_done)) }
                }
            }
        }
    }
}

/** Круг: по кругу — оттенок, от центра к краю — насыщенность; яркость затемняет весь круг. */
@Composable
private fun HueSaturationWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateText = "${hue.roundToInt()}°, ${(saturation * 100).roundToInt()}%"
    val label = stringResource(R.string.color_picker_wheel)
    val markerRadius = with(LocalDensity.current) { 11.dp.toPx() }
    fun pick(position: Offset, width: Int, height: Int) {
        val center = Offset(width / 2f, height / 2f)
        val radius = minOf(width, height) / 2f
        val dx = position.x - center.x
        val dy = position.y - center.y
        val angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        onChange((angle + 360f) % 360f, (hypot(dx, dy) / radius).coerceIn(0f, 1f))
    }
    Canvas(
        modifier
            .semantics {
                contentDescription = label
                stateDescription = stateText
            }
            .pointerInput(Unit) { detectTapGestures { pick(it, size.width, size.height) } }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pick(change.position, size.width, size.height)
                }
            }
    ) {
        val radius = size.minDimension / 2f
        // Оттенки по кругу по часовой стрелке от «трёх часов», как и считается угол в pick
        drawCircle(
            Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                center
            ),
            radius
        )
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, radius), radius)
        drawCircle(Color.Black.copy(alpha = 1f - value), radius)
        val angle = Math.toRadians(hue.toDouble())
        val marker = Offset(
            center.x + (cos(angle) * saturation * radius).toFloat(),
            center.y + (sin(angle) * saturation * radius).toFloat()
        )
        drawCircle(Color.hsv(hue, saturation, value), markerRadius, marker)
        drawCircle(Color.White, markerRadius, marker, style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.35f), markerRadius + 2.dp.toPx(), marker, style = Stroke(width = 1.dp.toPx()))
    }
}

/**
 * Поле для HEX или одного канала. Пока его правят, показывается то, что вписано; как только вписанное
 * становится цветом ([parse]), цвет меняется. Ушёл курсор — поле снова показывает [text].
 */
@Composable
private fun NumberField(
    label: String,
    text: String,
    keyboard: KeyboardType,
    width: Int,
    parse: (String) -> Color?,
    onParsed: (Color) -> Unit
) {
    var draft by remember { mutableStateOf<String?>(null) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = draft ?: text,
            onValueChange = { typed ->
                draft = typed
                parse(typed.trim())?.let(onParsed)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier
                .padding(top = 4.dp)
                .width(width.dp)
                .height(44.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 11.dp)
                .onFocusChanged { if (!it.isFocused) draft = null }
                .semantics { contentDescription = label }
        )
    }
}

private fun colorToHsv(color: Color): FloatArray =
    FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }

private fun colorToRgb(color: Color): IntArray {
    val argb = color.toArgb()
    return intArrayOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}

/** «#7CC2FF». */
fun colorToHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

/** «#7CC2FF», «7cc2ff» или короткое «#7CF» — в цвет; иначе null. */
fun parseHex(text: String): Color? {
    val digits = text.removePrefix("#")
    val full = when (digits.length) {
        3 -> digits.map { "$it$it" }.joinToString("")
        6 -> digits
        else -> return null
    }
    val rgb = full.toIntOrNull(16) ?: return null
    return Color(0xFF000000.toInt() or rgb)
}

@Preview
@Composable
private fun ColorPickerDialogPreview() {
    ChekkiTheme {
        ColorPickerDialog(initial = Color(0xFF8AB4FF), onDismiss = {}, onPick = {})
    }
}
