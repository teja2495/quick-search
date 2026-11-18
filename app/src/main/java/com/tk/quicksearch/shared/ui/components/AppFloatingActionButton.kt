package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.floatingActionShape

/** Color role of a floating action button. */
enum class AppFabTone {
    /** The page's main action, filled with the accent. */
    Primary,

    /** A secondary action, in the accent's tonal tint flattened onto the surface. */
    Tonal,

    /** A destructive action, such as deleting a selection. */
    Error,
}

@Composable
private fun AppFabTone.containerColor(): Color =
    when (this) {
        AppFabTone.Primary -> AppColors.Accent
        AppFabTone.Tonal -> AppColors.FloatingTonalContainer
        AppFabTone.Error -> MaterialTheme.colorScheme.error
    }

@Composable
private fun AppFabTone.contentColor(): Color =
    when (this) {
        AppFabTone.Primary -> AppColors.OnAccent
        AppFabTone.Tonal -> AppColors.Accent
        AppFabTone.Error -> MaterialTheme.colorScheme.onError
    }

/**
 * Material 3 FAB: 56dp, fully round and squaring on press, with a 24dp icon. Every icon-only
 * floating action uses this rather than Material's `FloatingActionButton`.
 */
@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tone: AppFabTone = AppFabTone.Primary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(DesignTokens.FloatingActionHeight),
        shape = floatingActionShape(interactionSource),
        containerColor = tone.containerColor(),
        contentColor = tone.contentColor(),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}

/**
 * Material 3 extended FAB: 56dp tall with a leading 24dp icon and a `labelLarge` label, fully
 * round and squaring on press. The caller sets the width (for example `fillMaxWidth()` or
 * `weight(1f)`); the label describes the action, so the icon is decorative. [enabled] false fades
 * the button and ignores taps.
 */
@Composable
fun AppExtendedFloatingActionButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: AppFabTone = AppFabTone.Primary,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Material FABs have no disabled state, so a disabled one fades and ignores taps.
    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
        expanded = true,
        modifier =
            modifier
                .height(DesignTokens.FloatingActionHeight)
                .alpha(if (enabled) 1f else AppColors.DisabledContentAlpha),
        shape = floatingActionShape(interactionSource),
        containerColor = tone.containerColor(),
        contentColor = tone.contentColor(),
        interactionSource = interactionSource,
        text = { Text(text = text, style = MaterialTheme.typography.labelLarge) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
        },
    )
}
