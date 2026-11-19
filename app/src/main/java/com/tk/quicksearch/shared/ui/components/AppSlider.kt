package com.tk.quicksearch.shared.ui.components

import androidx.annotation.IntRange
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * Material 3 slider driven by [value]: the caller owns the value and saves it from
 * [onValueChange]. Built on Material's [SliderState] overload. Colors default to
 * [AppColors.sliderColors] so the inactive track stays visible in Monochrome.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = AppColors.sliderColors(),
) {
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    state.value = value
    Slider(
        state = state,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
    )
}
