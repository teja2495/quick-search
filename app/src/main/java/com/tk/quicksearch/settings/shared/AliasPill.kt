package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * A tonal input chip for an alias. The label opens the editor, and the trailing close action
 * clears the alias from the same container. An unset alias uses the label action by itself.
 */
@Composable
fun AliasPill(
    text: AnnotatedString,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClearClick: (() -> Unit)? = null,
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val content: @Composable () -> Unit = {
        Row(
            modifier =
                Modifier
                    .heightIn(min = DesignTokens.ButtonHeightExtraSmall)
                    .padding(
                        start = DesignTokens.SpacingSmall,
                        end = if (onClearClick == null) DesignTokens.SpacingSmall else 0.dp,
                    ),
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

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = (DesignTokens.MinTouchTarget - DesignTokens.ButtonHeightExtraSmall) / 2)
                .background(AppColors.AccentTintLight, AppShapes.small),
        )
        Row(
            modifier = Modifier.heightIn(min = DesignTokens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClick != null) {
                Surface(
                    onClick = onClick,
                    modifier = Modifier.weight(1f, fill = false),
                    shape = AppShapes.small,
                    color = Color.Transparent,
                    content = content,
                )
            } else {
                Box(modifier = Modifier.weight(1f, fill = false)) { content() }
            }
            if (onClearClick != null) {
                val removeLabel = stringResource(R.string.action_remove)
                AppIconButton(onClick = onClearClick, tooltip = removeLabel) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = removeLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AliasChipIconSize),
                    )
                }
            }
        }
    }
}

private val AliasChipIconSize = DesignTokens.ButtonIconSizeExtraSmall
