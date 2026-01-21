package com.tk.quicksearch.search.contacts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactUiConstants

// ============================================================================
// Expand/Collapse Buttons
// ============================================================================

@Composable
internal fun ExpandButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(
                text = stringResource(R.string.action_expand_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
        )
        Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.desc_expand),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp)
        )
    }
}

@Composable
internal fun CollapseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = modifier) {
        Text(
                text = stringResource(R.string.action_collapse),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Icon(
                imageVector = Icons.Rounded.ExpandLess,
                contentDescription = stringResource(R.string.desc_collapse),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp)
        )
    }
}
