package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import kotlin.math.roundToInt

private const val HORIZONTAL_PADDING_MAX_DP = 16f
private const val VERTICAL_OFFSET_MAX_DP = 12f

@Composable
fun WidgetInternalPaddingSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SLIDER_ROW_SPACING),
    ) {
        Text(
            text = stringResource(R.string.widget_slider_internal_padding),
            style = MaterialTheme.typography.titleSmall,
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_internal_padding_horizontal),
            value = toInvertedPercent(state.internalHorizontalPaddingDp, HORIZONTAL_PADDING_MAX_DP),
            valueRange = 0f..100f,
            steps = 100,
            valueFormatter = { "${it.roundToInt()}%" },
            labelTextStyle = MaterialTheme.typography.bodySmall,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onValueChange = { value ->
                onStateChange(
                    state.copy(
                        internalHorizontalPaddingDp = fromInvertedPercent(value, HORIZONTAL_PADDING_MAX_DP),
                    ),
                )
            },
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_internal_padding_vertical),
            value = toPercent(state.internalVerticalPaddingDp, VERTICAL_OFFSET_MAX_DP),
            valueRange = -100f..100f,
            steps = 200,
            valueFormatter = { formatSignedPercent(it) },
            labelTextStyle = MaterialTheme.typography.bodySmall,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onValueChange = { value ->
                onStateChange(
                    state.copy(
                        internalVerticalPaddingDp = fromPercent(value, VERTICAL_OFFSET_MAX_DP),
                    ),
                )
            },
        )
    }
}

private fun toPercent(
    valueDp: Float,
    maxDp: Float,
): Float = ((valueDp / maxDp) * 100f).coerceIn(-100f, 100f)

private fun fromPercent(
    percent: Float,
    maxDp: Float,
): Float = ((percent / 100f) * maxDp).coerceIn(-maxDp, maxDp)

private fun toInvertedPercent(
    valueDp: Float,
    maxDp: Float,
): Float = (100f - ((valueDp / maxDp) * 100f)).coerceIn(0f, 100f)

private fun fromInvertedPercent(
    percent: Float,
    maxDp: Float,
): Float = ((100f - percent) / 100f * maxDp).coerceIn(0f, maxDp)

private fun formatSignedPercent(value: Float): String {
    val rounded = value.roundToInt()
    return when {
        rounded > 0 -> "+$rounded%"
        rounded < 0 -> "$rounded%"
        else -> "0%"
    }
}
