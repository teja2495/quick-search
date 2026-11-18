package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SliderWithValue
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetRanges
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Corner radius, border width and transparency as one group of tiles. [leadingTile] adds a tile of
 * the caller's own sliders (a widget's font size) at the top of the group.
 */
@Composable
fun WidgetSlidersSection(
    state: WidgetPreferences,
    showBorderControls: Boolean = true,
    leadingTile: (@Composable ColumnScope.() -> Unit)? = null,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val offset = if (leadingTile != null) 1 else 0
    val count = offset + if (showBorderControls) 3 else 2
    SegmentedGroup {
        if (leadingTile != null) {
            WidgetSettingTile(index = 0, count = count, content = leadingTile)
        }
        WidgetSettingTile(index = offset, count = count) {
            SliderRow(
                label = stringResource(R.string.widget_slider_radius),
                value = state.borderRadiusDp,
                valueRange = 0f..40f,
                steps = 40,
                valueFormatter = { "${it.roundToInt()} dp" },
                onValueChange = { onStateChange(state.copy(borderRadiusDp = it)) },
            )
        }
        if (showBorderControls) {
            WidgetSettingTile(index = offset + 1, count = count) {
                SliderRow(
                    label = stringResource(R.string.widget_slider_border),
                    value = state.borderWidthDp,
                    valueRange = 0f..4f,
                    steps = 8,
                    valueFormatter = { formatBorderWidth(it) },
                    onValueChange = { onStateChange(state.copy(borderWidthDp = it)) },
                )
            }
            WidgetSettingTile(
                title = stringResource(R.string.settings_wallpaper_transparency_label),
                index = offset + 2,
                count = count,
            ) {
                SliderRow(
                    label = stringResource(R.string.widget_slider_transparency_background),
                    value = state.backgroundAlpha,
                    valueRange = 0f..1f,
                    steps = 10,
                    valueFormatter = { "${(it * 100).roundToInt()}%" },
                    labelTextStyle = MaterialTheme.typography.bodyMedium,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onValueChange = { onStateChange(state.copy(backgroundAlpha = it)) },
                )
                SliderRow(
                    label = stringResource(R.string.widget_slider_transparency_border),
                    value = state.borderAlpha,
                    valueRange = 0f..1f,
                    steps = 10,
                    valueFormatter = { "${(it * 100).roundToInt()}%" },
                    labelTextStyle = MaterialTheme.typography.bodyMedium,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onValueChange = { onStateChange(state.copy(borderAlpha = it)) },
                )
            }
        } else {
            // Background is the only transparency here, so it needs no sub-label.
            WidgetSettingTile(index = offset + 1, count = count) {
                SliderRow(
                    label = stringResource(R.string.settings_wallpaper_transparency_label),
                    value = state.backgroundAlpha,
                    valueRange = 0f..1f,
                    steps = 10,
                    valueFormatter = { "${(it * 100).roundToInt()}%" },
                    onValueChange = { onStateChange(state.copy(backgroundAlpha = it)) },
                )
            }
        }
    }
}

/** Icon size slider; a tile at [index] of [count] in the caller's group. */
@Composable
fun WidgetIconSizeSection(
    state: WidgetPreferences,
    index: Int = 0,
    count: Int = 1,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    WidgetSettingTile(index = index, count = count) {
        SliderRow(
            label = stringResource(R.string.widget_slider_icon_size),
            value = state.iconSizeScale,
            valueRange = 1f..1.4f,
            steps = 8,
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            onValueChange = { onStateChange(state.copy(iconSizeScale = it)) },
        )
    }
}

@Composable
fun WidgetTextSizeSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    WidgetSettingTile {
        SliderRow(
            label = stringResource(R.string.settings_font_size_title),
            value = state.textScale,
            valueRange = WidgetRanges.TEXT_SCALE_MIN..WidgetRanges.TEXT_SCALE_MAX,
            steps = 14,
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            showTicks = false,
            onValueChange = { onStateChange(state.copy(textScale = it)) },
        )
    }
}

private fun formatBorderWidth(value: Float): String =
    if (value == 0f) {
        "0 dp"
    } else {
        String.format(Locale.US, "%.1f dp", value)
    }

@Composable
internal fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    labelTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    showTicks: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    val view = LocalView.current
    val start = valueRange.start
    val span = valueRange.endInclusive - start
    val lastStepIndexState = remember {
        mutableStateOf(
            if (span > 0) {
                ((value - start) / span * steps).roundToInt().coerceIn(0, steps)
            } else {
                0
            },
        )
    }
    SliderWithValue(
        headerModifier = Modifier.padding(horizontal = WidgetTileTextInset),
        headerLabel = {
            Text(
                text = label,
                style = labelTextStyle,
                color = labelColor,
            )
        },
        valueLabel = {
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Accent,
            )
        },
    ) { sliderModifier ->
        Slider(
            value = value,
            onValueChange = { rawValue ->
                val v = if (!showTicks && steps > 0 && span > 0) {
                    val stepSize = span / steps
                    (((rawValue - start) / stepSize).roundToInt() * stepSize + start)
                        .coerceIn(valueRange.start, valueRange.endInclusive)
                } else {
                    rawValue
                }
                val step =
                    if (span > 0) {
                        ((v - start) / span * steps)
                            .roundToInt()
                            .coerceIn(0, steps)
                    } else {
                        0
                    }
                if (step != lastStepIndexState.value) {
                    hapticToggle(view)()
                    lastStepIndexState.value = step
                }
                onValueChange(v)
            },
            valueRange = valueRange,
            steps = if (showTicks) (steps - 1).coerceAtLeast(0) else 0,
            modifier = sliderModifier,
        )
    }
}
