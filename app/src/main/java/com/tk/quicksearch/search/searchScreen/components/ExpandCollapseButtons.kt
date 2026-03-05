package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor

private const val EXPAND_ICON_SIZE = 18

@Composable
internal fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textResId: Int = R.string.action_expand_more,
) {
    val overlayActionColor = LocalOverlayActionColor.current
    val moreActionColor =
        if (overlayActionColor != null) {
            Color.White
        } else {
            MaterialTheme.colorScheme.primary
        }

    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Text(
            text = stringResource(textResId),
            style = MaterialTheme.typography.bodySmall,
            color = moreActionColor,
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = moreActionColor,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp),
        )
    }
}

@Composable
internal fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlayActionColor = LocalOverlayActionColor.current
    val collapseContainerColor =
        overlayActionColor ?: MaterialTheme.colorScheme.secondaryContainer
    val collapseContentColor =
        if (overlayActionColor != null) {
            if (overlayActionColor.luminance() > 0.5f) Color.Black else Color.White
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    val collapseBorderColor = collapseContentColor.copy(alpha = 0.24f)
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        border =
            BorderStroke(
                1.dp,
                collapseBorderColor,
            ),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = collapseContainerColor,
                contentColor = collapseContentColor,
            ),
    ) {
        Icon(
            imageVector = Icons.Rounded.ExpandLess,
            contentDescription = stringResource(R.string.desc_collapse),
            tint = collapseContentColor,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_collapse),
            style = MaterialTheme.typography.bodySmall,
            color = collapseContentColor,
        )
    }
}
