package com.tk.quicksearch.settings.additional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.components.CombinedAssistantCard
import com.tk.quicksearch.settings.components.RefreshDataCard
import com.tk.quicksearch.settings.components.SettingsCard
import com.tk.quicksearch.settings.components.SettingsToggleRow
import com.tk.quicksearch.settings.main.SettingsNavigationCard
import com.tk.quicksearch.settings.main.SettingsSpacing
import com.tk.quicksearch.ui.theme.DesignTokens



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

    SettingsCard(modifier = modifier) {
        // Clear query after search engine toggle
        SettingsToggleRow(
            title = stringResource(R.string.settings_clear_query_after_search_engine_toggle),
            checked = clearQueryAfterSearchEngine,
            onCheckedChange = onToggleClearQueryAfterSearchEngine,
            isFirstItem = true
        )

        // Show all results toggle
        SettingsToggleRow(
            title = stringResource(R.string.settings_show_all_results_toggle),
            checked = showAllResults,
            onCheckedChange = onToggleShowAllResults
        )

        // Sort apps by usage toggle
        SettingsToggleRow(
            title = stringResource(R.string.settings_sort_apps_b_usage_toggle),
            checked = sortAppsByUsageEnabled,
            onCheckedChange = onToggleSortAppsByUsage,
            isLastItem = true
        )
    }

    // Combined Assistant Settings Card
    CombinedAssistantCard(
        isDefaultAssistant = isDefaultAssistant,
        onSetDefaultAssistant = onSetDefaultAssistant,
        onAddQuickSettingsTile = onAddQuickSettingsTile,
        modifier = Modifier.padding(top = 12.dp)
    )

    // Refresh Data Section at the bottom
    RefreshDataCard(
        onRefreshApps = onRefreshApps,
        onRefreshContacts = onRefreshContacts,
        onRefreshFiles = onRefreshFiles,
        modifier = Modifier.padding(top = 12.dp)
    )

}
