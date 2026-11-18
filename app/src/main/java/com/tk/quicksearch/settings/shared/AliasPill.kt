package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.Box
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * A compact alias chip that sits under a row's title without competing with it (24dp, labelMedium,
 * 8dp corners, no border, a soft accent tonal fill so it reads as tappable). With [onClearClick] it
 * shows a set alias alongside a separate remove button. Without it, an action such as "Setup Alias". The
 * accent follows custom and wallpaper accents.
 */
@Composable
fun AliasPill(
    text: AnnotatedString,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClearClick: (() -> Unit)? = null,
) {
    val containerColor = AppColors.AccentTintLight
    val labelColor = MaterialTheme.colorScheme.onSurface
    val content: @Composable () -> Unit = {
        Row(
            modifier =
                Modifier
                    .height(AliasChipHeight)
                    .padding(horizontal = DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(AliasChipIconSize),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (onClick != null) {
            // Reserve the touch target around the compact alias surface.
            Box(modifier = Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Surface(onClick = onClick, shape = AppShapes.small, color = containerColor, content = content)
                }
            }
        } else {
            Surface(shape = AppShapes.small, color = containerColor, content = content)
        }
        if (onClearClick != null) {
            val removeLabel = stringResource(R.string.action_remove)
            AppIconButton(onClick = onClearClick, tooltip = removeLabel) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = removeLabel,
                    modifier = Modifier.size(AliasChipIconSize),
                )
            }
        }
    }
}

private val AliasChipHeight = 24.dp
private val AliasChipIconSize = DesignTokens.ButtonIconSizeExtraSmall
