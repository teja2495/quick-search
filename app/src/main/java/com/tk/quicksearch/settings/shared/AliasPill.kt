package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * shows a set alias: the code and a remove button. Without it, an action such as "Setup Alias". The
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
    val isSet = onClearClick != null
    val containerColor = AppColors.AccentTintLight
    val labelColor = MaterialTheme.colorScheme.onSurface
    val content: @Composable () -> Unit = {
        Row(
            modifier =
                Modifier
                    .height(AliasChipHeight)
                    .padding(
                        start = AliasChipStartPadding,
                        end = if (isSet) 4.dp else 8.dp,
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
            if (onClearClick != null) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.action_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onClearClick)
                            .padding(2.dp)
                            .size(AliasChipIconSize),
                )
            }
        }
    }

    // A clickable Surface pads itself to a 48dp touch height, which would add 12dp above and below
    // the 24dp chip and inflate every row that shows one. The row around it is the larger target.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        if (onClick != null) {
            Surface(
                onClick = onClick,
                modifier = modifier,
                shape = AppShapes.small,
                color = containerColor,
                content = content,
            )
        } else {
            Surface(
                modifier = modifier,
                shape = AppShapes.small,
                color = containerColor,
                content = content,
            )
        }
    }
}

private val AliasChipHeight = 24.dp
private val AliasChipStartPadding = 6.dp
private val AliasChipIconSize = 14.dp
