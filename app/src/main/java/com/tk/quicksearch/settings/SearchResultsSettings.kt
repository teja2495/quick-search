package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.settings.shared.SectionSettingsSection
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.AppColors
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

    SettingsCard(modifier = modifier.fillMaxWidth()) {
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

            SettingsNavigationToggleRow(
                title = stringResource(R.string.recent_queries_toggle_title),
                subtitle = stringResource(R.string.recent_queries_toggle_desc),
                checked = recentQueriesEnabled,
                onCheckedChange = onRecentQueriesToggle,
                leadingIcon = Icons.Rounded.History,
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
    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                horizontal = DesignTokens.SpacingXXLarge,
                vertical = DesignTokens.SpacingXLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Text(
                text = stringResource(R.string.settings_refresh_data_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                OutlinedButton(
                    onClick = { onRefreshApps(true) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = DesignTokens.ShapeMedium,
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.SpacingSmall,
                        vertical = DesignTokens.SpacingMedium,
                    ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                        Text(
                            text = stringResource(R.string.settings_refresh_apps_button),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (hasContactPermission) {
                    OutlinedButton(
                        onClick = { onRefreshContacts(true) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = DesignTokens.ShapeMedium,
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.SpacingSmall,
                            vertical = DesignTokens.SpacingMedium,
                        ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Contacts,
                                contentDescription = null,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                            Text(
                                text = stringResource(R.string.settings_refresh_contacts_button),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                if (hasFilePermission) {
                    OutlinedButton(
                        onClick = { onRefreshFiles(true) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = DesignTokens.ShapeMedium,
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.SpacingSmall,
                            vertical = DesignTokens.SpacingMedium,
                        ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                            Text(
                                text = stringResource(R.string.settings_refresh_files_button),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
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
    hasCalendarPermission: Boolean,
    onNavigateToExcludedItems: () -> Unit,
    onNavigateToAppManagement: () -> Unit,
    onNavigateToAppShortcuts: () -> Unit,
    onNavigateToCallsTexts: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToDeviceSettings: () -> Unit,
    onNavigateToCalendarEvents: () -> Unit,
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
            contactsSubtitle =
                if (hasContactPermission) {
                    stringResource(R.string.settings_manage_calls_texts_contacts_desc)
                } else {
                    stringResource(R.string.settings_overlay_source_permission_required)
                },
            onContactsClick = if (hasContactPermission) onNavigateToCallsTexts else null,
            onContactsClickNoRipple = hasContactPermission,
            filesSubtitle =
                if (hasFilePermission) {
                    stringResource(R.string.settings_manage_files_desc)
                } else {
                    stringResource(R.string.settings_overlay_source_permission_required)
                },
            onFilesClick = if (hasFilePermission) onNavigateToFiles else null,
            onFilesClickNoRipple = hasFilePermission,
            deviceSettingsSubtitle = stringResource(R.string.settings_view_all_desc),
            onDeviceSettingsClick = onNavigateToDeviceSettings,
            onDeviceSettingsClickNoRipple = true,
            appSettingsSubtitle = stringResource(R.string.settings_app_settings_desc),
            calendarSubtitle =
                if (hasCalendarPermission) {
                    stringResource(R.string.settings_calendar_view_all_events_desc)
                } else {
                    stringResource(R.string.settings_overlay_source_permission_required)
                },
            onCalendarClick =
                if (hasCalendarPermission) {
                    onNavigateToCalendarEvents
                } else {
                    null
                },
            onCalendarClickNoRipple = hasCalendarPermission,
            sectionsWithHiddenAlias = buildSet {
                if (!hasContactPermission) add(SearchSection.CONTACTS)
                if (!hasFilePermission) add(SearchSection.FILES)
                if (!hasCalendarPermission) add(SearchSection.CALENDAR)
            },
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
