package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.focusable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppTextButton
import java.text.NumberFormat

private val ColorAreaHeight = 180.dp
private val HueTrackHeight = DesignTokens.Spacing28
private val HexColorPattern = Regex("#?[0-9A-Fa-f]{6}")
private val ColorPreviewSize = 32.dp

@Composable
fun WidgetColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    title: String = stringResource(R.string.widget_background_color_custom_dialog_title),
    /** When set, Reset replaces Cancel and a close icon dismisses the dialog. */
    onReset: (() -> Unit)? = null,
) {
    val initialHsv = remember(initialColor) { initialColor.toHsv() }
    var hue by rememberSaveable(initialColor.toArgb()) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable(initialColor.toArgb()) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable(initialColor.toArgb()) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by rememberSaveable(initialColor.toArgb()) { mutableStateOf<String?>(null) }
    val typedColor = hexInput?.parseHexRgb()
    val selectedColor = typedColor ?: Color.hsv(hue, saturation, brightness)
    val hexValue = hexInput ?: selectedColor.toHexRgb()
    val validHex = hexInput == null || typedColor != null

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (onReset == null) {
                Text(text = title)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, modifier = Modifier.weight(1f))
                    AppIconButton(onClick = onDismiss, tooltip = stringResource(R.string.common_close)) {
                        Icon(
                            imageVector = MaterialSymbols.Close,
                            contentDescription = stringResource(R.string.common_close),
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                SaturationBrightnessPicker(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onColorChange = { newSaturation, newBrightness ->
                        hexInput = null
                        saturation = newSaturation
                        brightness = newBrightness
                    },
                )
                HuePicker(
                    hue = hue,
                    onHueChange = {
                        hexInput = null
                        hue = it
                    },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(ColorPreviewSize)
                                .clip(AppShapes.full)
                                .background(selectedColor)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = AppShapes.full,
                                ),
                    )
                    AppOutlinedTextField(
                        value = hexValue,
                        onValueChange = { input ->
                            if (input.length <= 7) {
                                hexInput = input
                                input.parseHexRgb()?.toHsv()?.let { hsv ->
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                }
                            }
                        },
                        label = stringResource(R.string.color_picker_hex),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = !validHex,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = { onConfirm(selectedColor) }, enabled = validHex) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            if (onReset == null) {
                AppTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            } else {
                AppTextButton(onClick = onReset) {
                    Text(stringResource(R.string.dialog_reset))
                }
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
    val latestOnColorChange by rememberUpdatedState(onColorChange)
    val locale = LocalConfiguration.current.locales[0]
    val percent = remember(locale) { NumberFormat.getPercentInstance(locale) }
    val saturationLabel = stringResource(R.string.color_picker_saturation)
    val brightnessLabel = stringResource(R.string.color_picker_brightness)
    fun adjust(saturationDelta: Float = 0f, brightnessDelta: Float = 0f): Boolean {
        val newSaturation = (saturation + saturationDelta).coerceIn(0f, 1f)
        val newBrightness = (brightness + brightnessDelta).coerceIn(0f, 1f)
        if (newSaturation == saturation && newBrightness == brightness) return false
        onColorChange(newSaturation, newBrightness)
        return true
    }
    val increaseSaturation = stringResource(R.string.color_picker_increase_saturation)
    val decreaseSaturation = stringResource(R.string.color_picker_decrease_saturation)
    val increaseBrightness = stringResource(R.string.color_picker_increase_brightness)
    val decreaseBrightness = stringResource(R.string.color_picker_decrease_brightness)
    val updateColor: (Offset, IntSize) -> Unit = { offset, size ->
        latestOnColorChange(
            (offset.x / size.width).coerceIn(0f, 1f),
            (1f - offset.y / size.height).coerceIn(0f, 1f),
        )
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ColorAreaHeight)
                .clip(AppShapes.medium)
                .semantics {
                    contentDescription = "$saturationLabel, $brightnessLabel"
                    stateDescription = "${percent.format(saturation)}, ${percent.format(brightness)}"
                    customActions = listOf(
                        CustomAccessibilityAction(increaseSaturation) { adjust(saturationDelta = ColorChannelStep) },
                        CustomAccessibilityAction(decreaseSaturation) { adjust(saturationDelta = -ColorChannelStep) },
                        CustomAccessibilityAction(increaseBrightness) { adjust(brightnessDelta = ColorChannelStep) },
                        CustomAccessibilityAction(decreaseBrightness) { adjust(brightnessDelta = -ColorChannelStep) },
                    )
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionRight -> adjust(saturationDelta = ColorChannelStep)
                        Key.DirectionLeft -> adjust(saturationDelta = -ColorChannelStep)
                        Key.DirectionUp -> adjust(brightnessDelta = ColorChannelStep)
                        Key.DirectionDown -> adjust(brightnessDelta = -ColorChannelStep)
                        else -> false
                    }
                }
                .colorPickerFocus()
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
            color = Color.Black.copy(alpha = AppColors.ContentSecondaryAlpha),
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
    val latestOnHueChange by rememberUpdatedState(onHueChange)
    val hueLabel = stringResource(R.string.color_picker_hue)
    fun change(value: Float): Boolean {
        val newHue = value.coerceIn(0f, MaxHue)
        if (newHue == hue) return false
        onHueChange(newHue)
        return true
    }
    val updateHue: (Offset, IntSize) -> Unit = { offset, size ->
        latestOnHueChange((offset.x / size.width).coerceIn(0f, 1f) * MaxHue)
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(DesignTokens.MinTouchTarget)
                .semantics {
                    contentDescription = hueLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(hue, 0f..MaxHue)
                    setProgress { change(it) }
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionRight, Key.DirectionUp -> change(hue + HueStep)
                        Key.DirectionLeft, Key.DirectionDown -> change(hue - HueStep)
                        else -> false
                    }
                }
                .colorPickerFocus()
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
        val trackHeight = HueTrackHeight.toPx().coerceAtMost(size.height)
        val trackTop = (size.height - trackHeight) / 2
        val thumbInset = 3.dp.toPx()
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
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )
        val indicatorX =
            ((hue / 360f) * size.width)
                .coerceIn(thumbInset, size.width - thumbInset)
        drawLine(
            color = Color.Black.copy(alpha = AppColors.ContentSecondaryAlpha),
            start = Offset(indicatorX, trackTop + thumbInset),
            end = Offset(indicatorX, trackTop + trackHeight - thumbInset),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = Offset(indicatorX, trackTop + thumbInset),
            end = Offset(indicatorX, trackTop + trackHeight - thumbInset),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private const val ColorChannelStep = 0.05f
private const val MaxHue = 359.999f
private const val HueStep = 10f

@Composable
private fun Modifier.colorPickerFocus(): Modifier {
    var focused by remember { mutableStateOf(false) }
    return onFocusChanged { focused = it.isFocused }
        .then(
            if (focused) {
                Modifier.border(DesignTokens.BorderWidth, MaterialTheme.colorScheme.onSurface, AppShapes.medium)
            } else {
                Modifier
            },
        )
        .focusable()
}

private fun Color.toHsv(): FloatArray =
    FloatArray(3).also { hsv -> AndroidColor.colorToHSV(toArgb(), hsv) }

private fun Color.toHexRgb(): String =
    String.format(java.util.Locale.US, "#%06X", toArgb() and 0xFFFFFF)

private fun String.parseHexRgb(): Color? =
    if (HexColorPattern.matches(this)) Color(0xFF000000.toInt() or removePrefix("#").toInt(16))
    else null
