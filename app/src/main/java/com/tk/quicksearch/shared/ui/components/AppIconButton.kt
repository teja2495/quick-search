package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.AppMotion
import kotlinx.coroutines.delay

/** Material 3 icon button color styles. */
enum class AppIconButtonStyle {
    /** No container; for toolbar and row actions. */
    Standard,

    /** `primary` container; for the single most important icon action in a group. */
    Filled,

    /** `secondaryContainer` container; for a prominent but secondary icon action. */
    Tonal,
}

/**
 * Material 3 icon button with a plain tooltip.
 *
 * Material 3 labels every icon-only action with a tooltip shown on long-press or hover. [tooltip]
 * is that label, and normally matches the content description of the icon inside. Use this instead
 * of calling Material `IconButton`, `FilledIconButton`, or `FilledTonalIconButton` directly; pick
 * the variant with [style]. [colors] overrides the style's default colors.
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
    style: AppIconButtonStyle = AppIconButtonStyle.Standard,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource? = null,
    anchorModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (tooltip == null) {
        StyledIconButton(onClick, anchorModifier.then(modifier), enabled, style, colors, interactionSource, content)
        return
    }
    AppPlainTooltip(text = tooltip, modifier = anchorModifier) {
        StyledIconButton(onClick, modifier, enabled, style, colors, interactionSource, content)
    }
}

/** Anchors a Material tooltip to a custom control. Hover keeps a reserved long-press action available. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppPlainTooltip(
    text: String,
    modifier: Modifier = Modifier,
    preserveLongPress: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState()
    val anchorModifier = if (preserveLongPress) {
        val hover = remember { MutableInteractionSource() }
        val hovered by hover.collectIsHoveredAsState()
        LaunchedEffect(state, hovered) {
            if (hovered) {
                delay(AppMotion.DurationGlowFade.toLong())
                state.show()
            } else {
                state.dismiss()
            }
        }
        modifier.hoverable(hover)
    } else {
        modifier
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = state,
        modifier = anchorModifier,
        enableUserInput = !preserveLongPress,
        content = content,
    )
}

@Composable
private fun StyledIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    style: AppIconButtonStyle,
    colors: IconButtonColors?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit,
) {
    // Material's expressive press morph: round at rest, squaring while pressed, on the theme's
    // motion scheme.
    // Reserve touch space outside caller-supplied visual sizes, including 28dp and 40dp buttons.
    val touchTarget = Modifier.minimumInteractiveComponentSize().then(modifier)
    when (style) {
        AppIconButtonStyle.Standard ->
            IconButton(
                onClick = onClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = touchTarget,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.iconButtonColors(),
                interactionSource = interactionSource,
                content = content,
            )
        AppIconButtonStyle.Filled ->
            FilledIconButton(
                onClick = onClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = touchTarget,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledIconButtonColors(),
                interactionSource = interactionSource,
                content = content,
            )
        AppIconButtonStyle.Tonal ->
            FilledTonalIconButton(
                onClick = onClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = touchTarget,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledTonalIconButtonColors(),
                interactionSource = interactionSource,
                content = content,
            )
    }
}
