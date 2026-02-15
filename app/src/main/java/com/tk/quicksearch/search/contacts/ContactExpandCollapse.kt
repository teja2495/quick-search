package com.tk.quicksearch.search.contacts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor

// ============================================================================
// Expand/Collapse Buttons
// ============================================================================

@Composable
internal fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Text(
            text = stringResource(R.string.action_expand_more),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = Color.White,
            modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp),
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
        overlayActionColor
            ?: MaterialTheme.colorScheme.onSecondaryContainer
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = collapseContainerColor,
                contentColor = collapseContentColor,
            ),
    ) {
        Icon(
            imageVector = Icons.Rounded.ExpandLess,
            contentDescription = stringResource(R.string.desc_collapse),
            tint = Color.White,
            modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_collapse),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
        )
    }
}
