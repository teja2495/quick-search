package com.tk.quicksearch.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Constants for consistent spacing and styling in search engine settings.
 */
object SearchEngineSettingsSpacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
    val cardTopPadding = 20.dp
    val cardBottomPadding = 20.dp
    val rowHorizontalPadding = 24.dp
    val rowVerticalPadding = 12.dp
    val itemHeight = 60.dp // Approximate row height with padding
}

/**
 * Reusable toggle row component for settings cards.
 * Provides consistent styling and layout across all toggle rows.
 */
@Composable
fun SearchEngineToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false
) {
    val topPadding = if (isFirstItem) SearchEngineSettingsSpacing.cardTopPadding 
                     else SearchEngineSettingsSpacing.cardVerticalPadding
    val bottomPadding = if (isLastItem) SearchEngineSettingsSpacing.cardBottomPadding 
                        else SearchEngineSettingsSpacing.cardVerticalPadding
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SearchEngineSettingsSpacing.cardHorizontalPadding,
                top = topPadding,
                end = SearchEngineSettingsSpacing.cardHorizontalPadding,
                bottom = bottomPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Reusable divider component with consistent styling.
 */
@Composable
fun SearchEngineDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
