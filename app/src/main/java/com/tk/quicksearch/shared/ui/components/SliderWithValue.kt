package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Displays the value beside the slider, or alongside a label above it. */
@Composable
internal fun SliderWithValue(
    valueLabel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    headerLabel: (@Composable () -> Unit)? = null,
    headerModifier: Modifier = Modifier,
    valueSpacing: Dp = 0.dp,
    leadingLabel: (@Composable () -> Unit)? = null,
    slider: @Composable (Modifier) -> Unit,
) {
    if (headerLabel == null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(valueSpacing),
        ) {
            leadingLabel?.invoke()
            slider(Modifier.weight(1f))
            valueLabel()
        }
    } else {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth().then(headerModifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                headerLabel()
                valueLabel()
            }
            slider(Modifier)
        }
    }
}
