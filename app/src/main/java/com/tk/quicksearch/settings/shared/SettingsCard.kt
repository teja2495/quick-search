package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Settings surface card. When [title] is set, a header (title plus optional [description])
 * is drawn above [content] and separated from it by a divider.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = AppColors.getSettingsCardContainerColor(),
            ),
        elevation = AppColors.getCardElevation(false),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        if (title != null) {
            SettingsCardHeader(title = title, description = description)
            HorizontalDivider(color = AppColors.SettingsDivider)
        }
        content()
    }
}

@Composable
private fun SettingsCardHeader(
    title: String,
    description: String?,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = DesignTokens.CardHorizontalPadding,
            vertical = DesignTokens.CardVerticalPadding,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignTokens.SpacingXSmall),
            )
        }
    }
}
