package com.tk.quicksearch.shared.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun AppDrawerHeader(
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        leadingContent?.invoke()
        Box(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                title()
            }
        }
        if (onClose != null) {
            AppIconButton(onClick = onClose, modifier = Modifier.size(DesignTokens.Spacing48), tooltip = stringResource(R.string.common_close)) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
