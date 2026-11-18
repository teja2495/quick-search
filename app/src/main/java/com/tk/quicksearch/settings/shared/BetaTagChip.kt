package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun BetaTagChip(
    modifier: Modifier = Modifier,
    tagText: String? = null,
) {
    val resolvedTagText = tagText ?: stringResource(R.string.settings_beta_tag)

    Row(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = AppShapes.full,
                ).padding(horizontal = DesignTokens.SpacingSmall, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = resolvedTagText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}
