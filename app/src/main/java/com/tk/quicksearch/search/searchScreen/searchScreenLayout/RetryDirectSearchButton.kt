package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor

private const val RETRY_ICON_SIZE = 18

@Composable
internal fun RetryDirectSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overlayActionColor = LocalOverlayActionColor.current
    val containerColor = overlayActionColor ?: MaterialTheme.colorScheme.secondaryContainer
    val contentColor =
        if (overlayActionColor != null) {
            if (overlayActionColor.luminance() > 0.5f) Color.Black else Color.White
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    val borderColor = contentColor.copy(alpha = 0.24f)

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(1.dp, borderColor),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.action_retry),
            tint = contentColor,
            modifier = Modifier.size(RETRY_ICON_SIZE.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_retry),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}