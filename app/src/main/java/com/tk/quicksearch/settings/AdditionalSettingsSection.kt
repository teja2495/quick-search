package com.tk.quicksearch.settings.additional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.components.RefreshDataCard
import com.tk.quicksearch.settings.main.SettingsNavigationCard
import com.tk.quicksearch.settings.main.SettingsSpacing

// Constants for consistent spacing
private object AdditionalSettingsSpacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
    val dividerHorizontalPadding = 16.dp
    val navigationCardBottomPadding = 12.dp
    val refreshDataTopPadding = 12.dp
}

/**
 * Reusable toggle row component for additional settings.
 * Provides consistent styling and layout across all toggle rows.
 */
@Composable
private fun SettingsToggleRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AdditionalSettingsSpacing.cardHorizontalPadding,
                vertical = AdditionalSettingsSpacing.cardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Additional Settings section with advanced options and configuration.
 */
@Composable
fun AdditionalSettingsSection(
    clearQueryAfterSearchEngine: Boolean,
    onToggleClearQueryAfterSearchEngine: (Boolean) -> Unit,
    showAllResults: Boolean,
    onToggleShowAllResults: (Boolean) -> Unit,
    sortAppsByUsageEnabled: Boolean,
    onToggleSortAppsByUsage: (Boolean) -> Unit,
    onSetDefaultAssistant: () -> Unit,
    onAddQuickSettingsTile: () -> Unit = {},
    isDefaultAssistant: Boolean = false,
    onRefreshApps: () -> Unit,
    onRefreshContacts: () -> Unit,
    onRefreshFiles: () -> Unit,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_additional_settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
        )
        Text(
            text = stringResource(R.string.settings_additional_settings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
        )
    }

    // Default Assistant - Navigation Card
    SettingsNavigationCard(
        title = stringResource(R.string.settings_default_assistant_title),
        description = stringResource(
            if (isDefaultAssistant) {
                R.string.settings_default_assistant_desc_change
            } else {
                R.string.settings_default_assistant_desc
            }
        ),
        onClick = onSetDefaultAssistant,
        modifier = Modifier.padding(bottom = AdditionalSettingsSpacing.navigationCardBottomPadding),
        contentPadding = SettingsSpacing.singleCardPadding
    )

    SettingsNavigationCard(
        title = stringResource(R.string.settings_quick_settings_tile_title),
        description = stringResource(R.string.settings_quick_settings_tile_desc),
        onClick = onAddQuickSettingsTile,
        modifier = Modifier.padding(bottom = AdditionalSettingsSpacing.navigationCardBottomPadding),
        contentPadding = SettingsSpacing.singleCardPadding
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Clear query after search engine toggle
            SettingsToggleRow(
                text = stringResource(R.string.settings_clear_query_after_search_engine_toggle),
                checked = clearQueryAfterSearchEngine,
                onCheckedChange = onToggleClearQueryAfterSearchEngine,
                isFirstItem = true
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AdditionalSettingsSpacing.dividerHorizontalPadding)
            )

            // Show all results toggle
            SettingsToggleRow(
                text = stringResource(R.string.settings_show_all_results_toggle),
                checked = showAllResults,
                onCheckedChange = onToggleShowAllResults
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AdditionalSettingsSpacing.dividerHorizontalPadding)
            )

            // Sort apps by usage toggle
            SettingsToggleRow(
                text = stringResource(R.string.settings_sort_apps_by_usage_toggle),
                checked = sortAppsByUsageEnabled,
                onCheckedChange = onToggleSortAppsByUsage,
                isLastItem = true
            )

        }
    }

    // Refresh Data Section at the bottom
    RefreshDataCard(
        onRefreshApps = onRefreshApps,
        onRefreshContacts = onRefreshContacts,
        onRefreshFiles = onRefreshFiles,
        modifier = Modifier.padding(top = AdditionalSettingsSpacing.refreshDataTopPadding)
    )

}
