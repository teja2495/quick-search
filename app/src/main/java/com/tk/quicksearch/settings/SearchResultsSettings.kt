package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/** Card for app suggestions, web suggestions, recent queries and excluded items. */
@Composable
private fun SearchOptionsCard(
    appSuggestionsEnabled: Boolean,
    hasUsagePermission: Boolean,
    onAppSuggestionsToggle: (Boolean) -> Unit,
    onRequestUsagePermission: () -> Unit,
    webSuggestionsEnabled: Boolean,
    onWebSuggestionsToggle: (Boolean) -> Unit,
    webSuggestionsCount: Int,
    onWebSuggestionsCountChange: (Int) -> Unit,
    recentQueriesEnabled: Boolean,
    onRecentQueriesToggle: (Boolean) -> Unit,
    hasExcludedItems: Boolean,
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    onNavigateToExcludedItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var appSuggestionsTipDismissed by rememberSaveable { mutableStateOf(false) }
    var lastWebStep by remember { mutableStateOf(webSuggestionsCount) }

    LaunchedEffect(appSuggestionsEnabled) {
        if (!appSuggestionsEnabled) {
            appSuggestionsTipDismissed = false
        }
    }
    LaunchedEffect(webSuggestionsEnabled) {
        if (!webSuggestionsEnabled) {
            lastWebStep = webSuggestionsCount
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.web_search_suggestions_title),
                checked = webSuggestionsEnabled,
                onCheckedChange = onWebSuggestionsToggle,
                leadingIcon = Icons.Rounded.Language,
                sliderDetails =
                    if (webSuggestionsEnabled) {
                        SettingsToggleSliderDetails(
                            value = webSuggestionsCount.toFloat(),
                            onValueChange = { value ->
                                val step = value.toInt()
                                if (step != lastWebStep) {
                                    hapticToggle(view)()
                                    lastWebStep = step
                                }
                                onWebSuggestionsCountChange(step)
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            valueLabel = webSuggestionsCount.toString(),
                        )
                    } else {
                        null
                    },
                isFirstItem = true,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.app_suggestions_toggle_title),
                subtitle = stringResource(R.string.app_suggestions_toggle_desc),
                checked = appSuggestionsEnabled,
                onCheckedChange = onAppSuggestionsToggle,
                leadingIcon = Icons.Rounded.Apps,
                isFirstItem = false,
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
private fun LaunchOptionsCard(
    onRefreshApps: (Boolean) -> Unit,
    onRefreshContacts: (Boolean) -> Unit,
    onRefreshFiles: (Boolean) -> Unit,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val itemShape = MaterialTheme.shapes.extraLarge
    val buttonBackgroundColor = MaterialTheme.colorScheme.surfaceContainerLow

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    bottom = DesignTokens.SpacingLarge,
                ),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(itemShape)
                .background(buttonBackgroundColor)
                .clickable(onClick = { onRefreshApps(true) })
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (hasContactPermission) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(buttonBackgroundColor)
                    .clickable(onClick = { onRefreshContacts(true) })
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (hasFilePermission) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(buttonBackgroundColor)
                    .clickable(onClick = { onRefreshFiles(true) })
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
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
            sectionOrder =
                ItemPriorityConfig
                    .getSearchResultsPriority()
                    .filter { section -> FeatureFlags.isSearchSectionEnabled(section) },
            disabledSections = state.disabledSections,
            onToggleSection = callbacks.onToggleSection,
            sectionAliasCodes = state.shortcutCodes,
            onSetSectionAlias = callbacks.onSetSearchSectionAlias,
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
            webSuggestionsEnabled = state.webSuggestionsEnabled,
            onWebSuggestionsToggle = callbacks.onToggleWebSuggestions,
            webSuggestionsCount = state.webSuggestionsCount,
            onWebSuggestionsCountChange = callbacks.onWebSuggestionsCountChange,
            recentQueriesEnabled = state.recentQueriesEnabled,
            onRecentQueriesToggle = callbacks.onToggleRecentQueries,
            hasExcludedItems = hasExcludedItems,
            excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
            excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
            onNavigateToExcludedItems = onNavigateToExcludedItems,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        LaunchOptionsCard(
            onRefreshApps = callbacks.onRefreshApps,
            onRefreshContacts = callbacks.onRefreshContacts,
            onRefreshFiles = callbacks.onRefreshFiles,
            hasContactPermission = hasContactPermission,
            hasFilePermission = hasFilePermission,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )
    }
}
