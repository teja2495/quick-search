package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.border
import com.tk.quicksearch.settings.shared.SectionSettingsSection
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/** Card for web search suggestions settings. */
@Composable
private fun WebSearchSuggestionsCard(
    webSuggestionsEnabled: Boolean,
    onWebSuggestionsToggle: (Boolean) -> Unit,
    webSuggestionsCount: Int,
    onWebSuggestionsCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier =
                Modifier.padding(
                    bottom = if (webSuggestionsEnabled) DesignTokens.SpacingSmall else 0.dp,
                ),
        ) {
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
                                start = DesignTokens.SpacingXXLarge,
                                end = DesignTokens.SpacingXXLarge,
                                top = 0.dp,
                                bottom = DesignTokens.SpacingXXLarge,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
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
                        modifier = Modifier.width(DesignTokens.SpacingXXLarge),
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
private fun SearchOptionsCard(
    appSuggestionsEnabled: Boolean,
    hasUsagePermission: Boolean,
    onAppSuggestionsToggle: (Boolean) -> Unit,
    onRequestUsagePermission: () -> Unit,
    recentQueriesEnabled: Boolean,
    onRecentQueriesToggle: (Boolean) -> Unit,
    hasExcludedItems: Boolean,
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    onNavigateToExcludedItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var appSuggestionsTipDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(appSuggestionsEnabled) {
        if (!appSuggestionsEnabled) {
            appSuggestionsTipDismissed = false
        }
    }

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
                showTipBanner = appSuggestionsEnabled && !hasUsagePermission && !appSuggestionsTipDismissed,
                tipBannerText = stringResource(R.string.app_suggestions_usage_access_tip),
                tipBannerLinkText = stringResource(R.string.app_suggestions_usage_access_link),
                onTipBannerLinkClick = onRequestUsagePermission,
                onTipBannerDismiss = { appSuggestionsTipDismissed = true },
            )

            SettingsToggleRow(
                title = stringResource(R.string.recent_queries_toggle_title),
                subtitle = stringResource(R.string.recent_queries_toggle_desc),
                checked = recentQueriesEnabled,
                onCheckedChange = onRecentQueriesToggle,
                leadingIcon = Icons.Rounded.History,
                isFirstItem = false,
                isLastItem = !hasExcludedItems,
            )

            if (hasExcludedItems) {
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = excludedItemsTitle,
                            description = excludedItemsDescription,
                            icon = Icons.Rounded.VisibilityOff,
                            actionOnPress = onNavigateToExcludedItems,
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )
            }
        }
    }
}

@Composable
private fun RefreshDataCard(
    onRefreshApps: (Boolean) -> Unit,
    onRefreshContacts: (Boolean) -> Unit,
    onRefreshFiles: (Boolean) -> Unit,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Text(
                text = stringResource(R.string.settings_refresh_data_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        start = DesignTokens.SpacingXXLarge,
                        top = DesignTokens.SpacingSmall,
                        end = DesignTokens.SpacingLarge,
                        bottom = DesignTokens.SpacingMedium,
                    ),
            )
            val itemShape = DesignTokens.ShapeMedium
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                        bottom = DesignTokens.SpacingSmall,
                    ),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .clickable(onClick = { onRefreshApps(true) })
                        .border(DesignTokens.BorderWidth, borderColor, itemShape)
                        .padding(DesignTokens.SpacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = stringResource(R.string.settings_refresh_apps_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_refresh_apps_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (hasContactPermission) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(itemShape)
                            .clickable(onClick = { onRefreshContacts(true) })
                            .border(DesignTokens.BorderWidth, borderColor, itemShape)
                            .padding(DesignTokens.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contacts,
                            contentDescription = stringResource(R.string.settings_refresh_contacts_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.settings_refresh_contacts_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (hasFilePermission) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(itemShape)
                            .clickable(onClick = { onRefreshFiles(true) })
                            .border(DesignTokens.BorderWidth, borderColor, itemShape)
                            .padding(DesignTokens.SpacingMedium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
                            contentDescription = stringResource(R.string.settings_refresh_files_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
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
}

/** Search Results settings section that combines all search results related settings. */
@Composable
fun SearchResultsSettingsSection(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    hasUsagePermission: Boolean,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    onNavigateToExcludedItems: () -> Unit,
    onNavigateToAppManagement: () -> Unit,
    onNavigateToAppShortcuts: () -> Unit,
    onNavigateToCallsTexts: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToDeviceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty()

    Column(modifier = modifier) {
        // Search Sections Section
        SectionSettingsSection(
            sectionOrder = ItemPriorityConfig.getSearchResultsPriority(),
            disabledSections = state.disabledSections,
            onToggleSection = callbacks.onToggleSection,
            appsSubtitle = stringResource(R.string.settings_manage_apps_desc),
            onAppsClick = onNavigateToAppManagement,
            onAppsClickNoRipple = true,
            appShortcutsSubtitle = stringResource(R.string.settings_manage_shortcuts_desc),
            onAppShortcutsClick = onNavigateToAppShortcuts,
            onAppShortcutsClickNoRipple = true,
            contactsSubtitle = stringResource(R.string.settings_manage_calls_texts_contacts_desc),
            onContactsClick = onNavigateToCallsTexts,
            onContactsClickNoRipple = true,
            filesSubtitle = stringResource(R.string.settings_manage_files_desc),
            onFilesClick = onNavigateToFiles,
            onFilesClickNoRipple = true,
            deviceSettingsSubtitle = stringResource(R.string.settings_view_all_desc),
            onDeviceSettingsClick = onNavigateToDeviceSettings,
            onDeviceSettingsClickNoRipple = true,
            showTitle = false,
        )

        SearchOptionsCard(
            appSuggestionsEnabled = state.appSuggestionsEnabled,
            hasUsagePermission = hasUsagePermission,
            onAppSuggestionsToggle = callbacks.onToggleAppSuggestions,
            onRequestUsagePermission = callbacks.onRequestUsagePermission,
            recentQueriesEnabled = state.recentQueriesEnabled,
            onRecentQueriesToggle = callbacks.onToggleRecentQueries,
            hasExcludedItems = hasExcludedItems,
            excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
            excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
            onNavigateToExcludedItems = onNavigateToExcludedItems,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        WebSearchSuggestionsCard(
            webSuggestionsEnabled = state.webSuggestionsEnabled,
            onWebSuggestionsToggle = callbacks.onToggleWebSuggestions,
            webSuggestionsCount = state.webSuggestionsCount,
            onWebSuggestionsCountChange = callbacks.onWebSuggestionsCountChange,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        RefreshDataCard(
            onRefreshApps = callbacks.onRefreshApps,
            onRefreshContacts = callbacks.onRefreshContacts,
            onRefreshFiles = callbacks.onRefreshFiles,
            hasContactPermission = hasContactPermission,
            hasFilePermission = hasFilePermission,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )
    }
}
