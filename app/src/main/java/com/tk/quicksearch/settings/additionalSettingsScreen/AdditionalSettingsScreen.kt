package com.tk.quicksearch.settings.additionalSettingsScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.additionalSettingsScreen.CombinedAssistantCard
import com.tk.quicksearch.settings.additionalSettingsScreen.RefreshDataCard
import com.tk.quicksearch.settings.SettingsCard
import com.tk.quicksearch.settings.SettingsToggleRow
import com.tk.quicksearch.settings.settingsScreen.SettingsNavigationCard
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.ui.theme.DesignTokens



/**
 * Additional Settings section with advanced options and configuration.
 */
@Composable
fun AdditionalSettingsScreen(
    clearQueryAfterSearchEngine: Boolean,
    onToggleClearQueryAfterSearchEngine: (Boolean) -> Unit,
    showAllResults: Boolean,
    onToggleShowAllResults: (Boolean) -> Unit,
    sortAppsByUsageEnabled: Boolean,
    onToggleSortAppsByUsage: (Boolean) -> Unit,
    fuzzyAppSearchEnabled: Boolean,
    onToggleFuzzyAppSearch: (Boolean) -> Unit,
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
        )

        // Fuzzy app search toggle
        SettingsToggleRow(
            title = stringResource(R.string.settings_fuzzy_app_search_toggle),
            checked = fuzzyAppSearchEnabled,
            onCheckedChange = onToggleFuzzyAppSearch,
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

    // All Quick Search Features link
    val context = LocalContext.current

    Text(
        text = stringResource(R.string.settings_all_quick_search_features),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .clickable {
                val url = "https://github.com/teja2495/quick-search/blob/main/FEATURES.md"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                    // Handle exception if the URL can't be opened
                }
            }
    )

}
