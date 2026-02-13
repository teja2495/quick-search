package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle

/** Card for web search suggestions settings. */
@Composable
fun WebSearchSuggestionsCard(
    webSuggestionsEnabled: Boolean,
    onWebSuggestionsToggle: (Boolean) -> Unit,
    webSuggestionsCount: Int,
    onWebSuggestionsCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            SettingsToggleRow(
                title = stringResource(R.string.web_search_suggestions_title),
                checked = webSuggestionsEnabled,
                onCheckedChange = onWebSuggestionsToggle,
                isFirstItem = true,
                isLastItem = !webSuggestionsEnabled,
                showDivider = false,
            )

            if (webSuggestionsEnabled) {
                var lastWebStep by remember { mutableStateOf(webSuggestionsCount) }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 0.dp,
                                bottom = 24.dp,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Slider(
                        value = webSuggestionsCount.toFloat(),
                        onValueChange = { value ->
                            val step = value.toInt()
                            if (step != lastWebStep) {
                                hapticToggle(view)()
                                lastWebStep = step
                            }
                            onWebSuggestionsCountChange(value.toInt())
                        },
                        valueRange = 1f..5f,
                        steps = 3, // 1, 2, 3, 4, 5
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = webSuggestionsCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                    )
                }
            }

        }
    }
}

/**
 * Card for app suggestions, recent queries, calculator and excluded items.
 */
@Composable
fun SearchOptionsCard(
    appSuggestionsEnabled: Boolean,
    onAppSuggestionsToggle: (Boolean) -> Unit,
    recentQueriesEnabled: Boolean,
    onRecentQueriesToggle: (Boolean) -> Unit,
    calculatorEnabled: Boolean,
    onToggleCalculator: (Boolean) -> Unit,
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    onNavigateToExcludedItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.app_suggestions_toggle_title),
                subtitle = stringResource(R.string.app_suggestions_toggle_desc),
                checked = appSuggestionsEnabled,
                onCheckedChange = onAppSuggestionsToggle,
                leadingIcon = Icons.Rounded.Apps,
                isFirstItem = true,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.recent_queries_toggle_title),
                subtitle = stringResource(R.string.recent_queries_toggle_desc),
                checked = recentQueriesEnabled,
                onCheckedChange = onRecentQueriesToggle,
                leadingIcon = Icons.Rounded.History,
                isFirstItem = false,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.calculator_toggle_title),
                subtitle = stringResource(R.string.calculator_toggle_desc),
                checked = calculatorEnabled,
                onCheckedChange = onToggleCalculator,
                leadingIcon = Icons.Rounded.Calculate,
                isFirstItem = false,
                isLastItem = false,
                extraVerticalPadding = 4.dp,
            )

            SettingsNavigationRow(
                item =
                    SettingsCardItem(
                        title = excludedItemsTitle,
                        description = excludedItemsDescription,
                        icon = Icons.Rounded.VisibilityOff,
                        actionOnPress = onNavigateToExcludedItems,
                    ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
fun RefreshDataCard(
    onRefreshApps: (Boolean) -> Unit,
    onRefreshContacts: (Boolean) -> Unit,
    onRefreshFiles: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_refresh_data_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
            )
            val itemShape = RoundedCornerShape(12.dp)
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .clickable(onClick = { onRefreshApps(true) })
                        .border(1.dp, borderColor, itemShape)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = stringResource(R.string.settings_refresh_apps_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_refresh_apps_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .clickable(onClick = { onRefreshContacts(true) })
                        .border(1.dp, borderColor, itemShape)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Contacts,
                        contentDescription = stringResource(R.string.settings_refresh_contacts_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_refresh_contacts_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .clickable(onClick = { onRefreshFiles(true) })
                        .border(1.dp, borderColor, itemShape)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.InsertDriveFile,
                        contentDescription = stringResource(R.string.settings_refresh_files_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_refresh_files_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Search Results settings section that combines all search results related settings. */
@Composable
fun SearchResultsSettingsSection(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    onNavigateToExcludedItems: () -> Unit,
    onNavigateToAppManagement: () -> Unit,
    onNavigateToAppShortcuts: () -> Unit,
    onNavigateToDeviceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Search Sections Section
        SectionSettingsSection(
            sectionOrder = ItemPriorityConfig.getSearchResultsPriority(),
            disabledSections = state.disabledSections,
            onToggleSection = callbacks.onToggleSection,
            appsSubtitle = stringResource(R.string.settings_manage_apps),
            onAppsClick = onNavigateToAppManagement,
            onAppsClickNoRipple = true,
            appShortcutsSubtitle = stringResource(R.string.settings_manage_shortcuts),
            onAppShortcutsClick = onNavigateToAppShortcuts,
            onAppShortcutsClickNoRipple = true,
            deviceSettingsSubtitle = stringResource(R.string.settings_view_all),
            onDeviceSettingsClick = onNavigateToDeviceSettings,
            onDeviceSettingsClickNoRipple = true,
            showTitle = false,
        )

        WebSearchSuggestionsCard(
            webSuggestionsEnabled = state.webSuggestionsEnabled,
            onWebSuggestionsToggle = callbacks.onToggleWebSuggestions,
            webSuggestionsCount = state.webSuggestionsCount,
            onWebSuggestionsCountChange = callbacks.onWebSuggestionsCountChange,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        SearchOptionsCard(
            appSuggestionsEnabled = state.appSuggestionsEnabled,
            onAppSuggestionsToggle = callbacks.onToggleAppSuggestions,
            recentQueriesEnabled = state.recentQueriesEnabled,
            onRecentQueriesToggle = callbacks.onToggleRecentQueries,
            calculatorEnabled = state.calculatorEnabled,
            onToggleCalculator = callbacks.onToggleCalculator,
            excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
            excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
            onNavigateToExcludedItems = onNavigateToExcludedItems,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        RefreshDataCard(
            onRefreshApps = callbacks.onRefreshApps,
            onRefreshContacts = callbacks.onRefreshContacts,
            onRefreshFiles = callbacks.onRefreshFiles,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )
    }
}
