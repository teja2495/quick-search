package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun WidgetSlidersSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SLIDER_ROW_SPACING),
    ) {
        SliderRow(
            label = stringResource(R.string.widget_slider_radius),
            value = state.borderRadiusDp,
            valueRange = 0f..30f,
            steps = 30,
            valueFormatter = { "${it.roundToInt()} dp" },
            onValueChange = { onStateChange(state.copy(borderRadiusDp = it)) },
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_border),
            value = state.borderWidthDp,
            valueRange = 0f..4f,
            steps = 8,
            valueFormatter = { formatBorderWidth(it) },
            onValueChange = { onStateChange(state.copy(borderWidthDp = it)) },
        )
        Text(
            text = stringResource(R.string.settings_wallpaper_transparency_label),
            style = MaterialTheme.typography.titleSmall,
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_transparency_background),
            value = state.backgroundAlpha,
            valueRange = 0f..1f,
            steps = 10,
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            labelTextStyle = MaterialTheme.typography.bodySmall,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onValueChange = { onStateChange(state.copy(backgroundAlpha = it)) },
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_transparency_border),
            value = state.borderAlpha,
            valueRange = 0f..1f,
            steps = 10,
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            labelTextStyle = MaterialTheme.typography.bodySmall,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onValueChange = { onStateChange(state.copy(borderAlpha = it)) },
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
    labelTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = labelTextStyle,
                color = labelColor,
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = { v ->
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
        )
    }
}
