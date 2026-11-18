package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Material 3 standard icon button with a plain tooltip.
 *
 * Material 3 labels every icon-only action with a tooltip shown on long-press or hover. [tooltip]
 * is that label, and normally matches the content description of the icon inside. Use this instead
 * of calling Material `IconButton` directly.
 *
 * [modifier] goes on the button itself, as with `IconButton`, so sizes behave the same. Modifiers
 * that the parent layout reads, such as `align` or `weight`, go in [anchorModifier] instead,
 * because the tooltip anchor sits between the button and its parent. A null [tooltip] is only for
 * callers whose label is optional; the button then has no tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    tooltip: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    anchorModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (tooltip == null) {
        IconButton(
            onClick = onClick,
            modifier = anchorModifier.then(modifier),
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
        modifier = anchorModifier,
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            content = content,
        )
    }
}
