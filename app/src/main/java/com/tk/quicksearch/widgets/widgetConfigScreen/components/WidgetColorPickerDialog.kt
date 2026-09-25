package com.tk.quicksearch.widgets.widgetConfigScreen.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private val ColorAreaHeight = 180.dp
private val HueBarHeight = 28.dp
private val ColorPreviewSize = 32.dp

@Composable
fun WidgetColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    title: String = stringResource(R.string.widget_background_color_custom_dialog_title),
) {
    val initialHsv = remember(initialColor) { initialColor.toHsv() }
    var hue by rememberSaveable(initialColor.toArgb()) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable(initialColor.toArgb()) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable(initialColor.toArgb()) { mutableFloatStateOf(initialHsv[2]) }
    val selectedColor = Color.hsv(hue, saturation, brightness)

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge)) {
                SaturationBrightnessPicker(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onColorChange = { newSaturation, newBrightness ->
                        saturation = newSaturation
                        brightness = newBrightness
                    },
                )
                HuePicker(
                    hue = hue,
                    onHueChange = { hue = it },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(ColorPreviewSize)
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                    )
                    Text(
                        text = selectedColor.toHexRgb(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedColor) }) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun SaturationBrightnessPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChange: (saturation: Float, brightness: Float) -> Unit,
) {
    val updateColor: (Offset, IntSize) -> Unit = { offset, size ->
        onColorChange(
            (offset.x / size.width).coerceIn(0f, 1f),
            (1f - offset.y / size.height).coerceIn(0f, 1f),
        )
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ColorAreaHeight)
                .clip(MaterialTheme.shapes.medium)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateColor(down.position, size)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.filter { it.pressed }.forEach { change ->
                                updateColor(change.position, size)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
    ) {
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))),
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
        )

        val indicatorCenter =
            Offset(
                x = (saturation * size.width).coerceIn(9.dp.toPx(), size.width - 9.dp.toPx()),
                y =
                    ((1f - brightness) * size.height)
                        .coerceIn(9.dp.toPx(), size.height - 9.dp.toPx()),
            )
        drawCircle(
            color = Color.Black.copy(alpha = 0.65f),
            radius = 9.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 4.dp.toPx()),
        )
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = indicatorCenter,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun HuePicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
) {
    val updateHue: (Offset, IntSize) -> Unit = { offset, size ->
        onHueChange((offset.x / size.width).coerceIn(0f, 1f) * 359.999f)
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(HueBarHeight)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateHue(down.position, size)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.filter { it.pressed }.forEach { change ->
                                updateHue(change.position, size)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
    ) {
        drawRoundRect(
            brush =
                Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    ),
                ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
        )
        val indicatorX =
            ((hue / 360f) * size.width)
                .coerceIn(3.dp.toPx(), size.width - 3.dp.toPx())
        drawLine(
            color = Color.Black.copy(alpha = 0.65f),
            start = Offset(indicatorX, 3.dp.toPx()),
            end = Offset(indicatorX, size.height - 3.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = Offset(indicatorX, 3.dp.toPx()),
            end = Offset(indicatorX, size.height - 3.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun Color.toHsv(): FloatArray =
    FloatArray(3).also { hsv -> AndroidColor.colorToHSV(toArgb(), hsv) }

private fun Color.toHexRgb(): String =
    String.format(java.util.Locale.US, "#%06X", toArgb() and 0xFFFFFF)
