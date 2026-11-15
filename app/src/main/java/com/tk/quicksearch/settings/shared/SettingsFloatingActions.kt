package com.tk.quicksearch.settings.shared

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Height shared by the floating actions at the bottom of settings pages, so they line up. */
val SettingsFloatingActionHeight: Dp = 56.dp

/**
 * Material 3 Expressive shape for a floating settings action: fully round at rest, morphing to
 * rounded corners while pressed on the expressive spring.
 */
@Composable
fun settingsFloatingActionShape(interactionSource: MutableInteractionSource): RoundedCornerShape {
    val pressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) SettingsFloatingActionPressedCorner else SettingsFloatingActionHeight / 2,
        animationSpec = DesignTokens.expressiveFastSpatialSpring(),
        label = "settingsFloatingActionCorner",
    )
    return RoundedCornerShape(corner)
}

/**
 * Container of a secondary floating action beside the accent one (e.g. Search beside Add): the
 * accent tint the settings tiles use, made opaque over a raised surface since it floats over the
 * list. Follows the accent through Material You, custom, wallpaper, and monochrome themes.
 */
val SettingsFloatingTonalContainer: Color
    @Composable
    get() =
        AppColors.Accent.copy(alpha = SettingsFloatingTonalAlpha)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)

private val SettingsFloatingActionPressedCorner: Dp = 16.dp
private const val SettingsFloatingTonalAlpha = 0.24f
