package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.settings.shared.settingsRoute.SectionSettingsSection
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.search.data.preferences.NicknamePreferences
import com.tk.quicksearch.search.data.preferences.TriggerPreferences
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.components.AnimatedSegmentedTiles
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.shared.ui.components.AppTonalButton

@Composable
internal fun AppSuggestionTabsDialog(
    enabledTabs: Set<AppSuggestionTabType>,
    onTabEnabledChange: (AppSuggestionTabType, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val tabLabels = rememberAppSuggestionTabLabels()
    // The Pinned tab is always shown: it holds pinned apps, shortcuts, and folders.
    val configurableTabs =
        remember(tabLabels) {
            listOf(
                AppSuggestionTabType.RECENTS to requireNotNull(tabLabels[AppSuggestionTabType.RECENTS]),
                AppSuggestionTabType.NEW_UPDATED to requireNotNull(tabLabels[AppSuggestionTabType.NEW_UPDATED]),
                AppSuggestionTabType.MOST_USED to requireNotNull(tabLabels[AppSuggestionTabType.MOST_USED]),
            )
        }

    AppPickerDrawer(
        title = stringResource(R.string.app_suggestions_tabs_dialog_title),
        onDismiss = onDismiss,
        header = {
            Text(
                text = stringResource(R.string.app_suggestions_tabs_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().clip(AppShapes.extraLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
        ) {
            itemsIndexed(configurableTabs, key = { _, tab -> tab.first.name }) { index, (tab, title) ->
                SegmentedToggleTile(
                    title = title,
                    checked = tab in enabledTabs,
                    onCheckedChange = { onTabEnabledChange(tab, it) },
                    icon = when (tab) {
                        AppSuggestionTabType.RECENTS -> MaterialSymbols.History
                        AppSuggestionTabType.NEW_UPDATED -> MaterialSymbols.Refresh
                        else -> MaterialSymbols.Apps
                    },
                    index = index,
                    count = configurableTabs.size,
                )
            }
        }
    }
}

@Composable
internal fun rememberAppSuggestionTabLabels(): Map<AppSuggestionTabType, String> =
    mapOf(
        AppSuggestionTabType.PINNED to stringResource(R.string.app_suggestions_tab_pinned),
        AppSuggestionTabType.RECENTS to stringResource(R.string.app_suggestions_tab_recent),
        AppSuggestionTabType.NEW_UPDATED to stringResource(R.string.app_suggestions_tab_new_updated),
        AppSuggestionTabType.MOST_USED to stringResource(R.string.common_most_used),
    )

internal fun buildAppSuggestionTabsSummary(
    enabledTabs: Set<AppSuggestionTabType>,
    labels: Map<AppSuggestionTabType, String>,
): String {
    val orderedTabs =
        listOf(
            AppSuggestionTabType.PINNED,
            AppSuggestionTabType.RECENTS,
            AppSuggestionTabType.NEW_UPDATED,
            AppSuggestionTabType.MOST_USED,
        )

    return orderedTabs
        .filter { it in enabledTabs }
        .mapNotNull(labels::get)
        .joinToString(separator = ", ")
}

@Composable
private fun TopMatchesCard(
    topMatchesEnabled: Boolean,
    onTopMatchesToggle: (Boolean) -> Unit,
    topMatchesLimit: Int,
    onTopMatchesLimitChange: (Int) -> Unit,
    topMatchesSectionOrder: List<SearchSection>,
    disabledTopMatchesSections: Set<SearchSection>,
    onTopMatchesSectionOrderChange: (List<SearchSection>) -> Unit,
    onTopMatchesSectionEnabledChange: (SearchSection, Boolean) -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showPriorityDialog by rememberSaveable { mutableStateOf(false) }
    val priorityItems =
        remember(topMatchesSectionOrder) {
            topMatchesSectionOrder.filter { section ->
                    section in com.tk.quicksearch.search.core.SearchSectionRegistry.settingsSections
            }
        }

    SegmentedGroup(modifier = modifier) {
        AnimatedSegmentedTiles {
            item { index, count ->
                SegmentedToggleTile(
                    title = stringResource(R.string.top_matches_title),
                    description = stringResource(R.string.top_matches_toggle_desc),
                    checked = topMatchesEnabled,
                    onCheckedChange = onTopMatchesToggle,
                    icon = MaterialSymbols.AutoAwesome,
                    enabled = !isLocked,
                    index = index,
                    count = count,
                )
            }
            item(visible = topMatchesEnabled) { index, count ->
                SettingsSegmentNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(R.string.top_matches_priority_title),
                        description = stringResource(R.string.top_matches_priority_desc),
                        icon = MaterialSymbols.Reorder,
                        actionOnPress = { showPriorityDialog = true },
                    ),
                    index = index,
                    count = count,
                )
            }
            tile(visible = topMatchesEnabled) {
                TopMatchesLimitSlider(
                    selectedLimit = topMatchesLimit,
                    onLimitSelected = onTopMatchesLimitChange,
                )
            }
        }
    }

    if (showPriorityDialog) {
        PriorityReorderDialog(
            items = priorityItems,
            onItemsChange = onTopMatchesSectionOrderChange,
            disabledSections = disabledTopMatchesSections,
            onItemEnabledChange = onTopMatchesSectionEnabledChange,
            onDismiss = { showPriorityDialog = false },
            heightFraction = TOP_MATCHES_DRAWER_HEIGHT_FRACTION,
        )
    }
}

@Composable
private fun TopMatchesLimitSlider(
    selectedLimit: Int,
    onLimitSelected: (Int) -> Unit,
) {
    val view = LocalView.current
    val limitOptions = com.tk.quicksearch.search.data.preferences.UiPreferences.TOP_MATCHES_LIMIT_OPTIONS
    val currentIndex = limitOptions.indexOf(selectedLimit).coerceAtLeast(0)
    var lastIndex by remember { mutableStateOf(currentIndex) }
    LaunchedEffect(selectedLimit) {
        lastIndex = limitOptions.indexOf(selectedLimit).coerceAtLeast(0)
    }

    SettingsToggleRow(
        title = stringResource(R.string.top_matches_count_label),
        checked = true,
        onCheckedChange = {},
        sliderDetails =
            SettingsToggleSliderDetails(
                value = currentIndex.toFloat(),
                onValueChange = { value ->
                    val index = value.toInt().coerceIn(0, limitOptions.size - 1)
                    if (index != lastIndex) {
                        hapticToggle(view)()
                        lastIndex = index
                    }
                    onLimitSelected(limitOptions[index])
                },
                valueRange = 0f..(limitOptions.size - 1).toFloat(),
                steps = limitOptions.size - 2,
                valueLabel = selectedLimit.toString(),
                valueLabelWidth = 24.dp,
            ),
        isFirstItem = false,
        isLastItem = true,
        showSwitch = false,
    )
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
    SegmentedTile(index = 0, count = 1, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                horizontal = DesignTokens.SpacingXXLarge,
                vertical = DesignTokens.SpacingXLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Text(
                text = stringResource(R.string.settings_refresh_data_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_refresh_data_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                AppTonalButton(
                    onClick = { onRefreshApps(true) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.SpacingSmall,
                        vertical = DesignTokens.SpacingMedium,
                    ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                        Text(
                            text = stringResource(R.string.section_apps),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (hasContactPermission) {
                    AppTonalButton(
                        onClick = { onRefreshContacts(true) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.SpacingSmall,
                            vertical = DesignTokens.SpacingMedium,
                        ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Contacts,
                                contentDescription = null,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                            Text(
                                text = stringResource(R.string.contacts_action_button_contacts),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                if (hasFilePermission) {
                    AppTonalButton(
                        onClick = { onRefreshFiles(true) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.SpacingSmall,
                            vertical = DesignTokens.SpacingMedium,
                        ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                            Text(
                                text = stringResource(R.string.section_files),
                                style = MaterialTheme.typography.labelLarge,
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
    onNavigateToNicknames: () -> Unit,
    onNavigateToTriggers: () -> Unit,
    onNavigateToAppManagement: () -> Unit,
    onNavigateToAppShortcuts: () -> Unit,
    onNavigateToCallsTexts: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToDeviceSettings: () -> Unit,
    onNavigateToCalendarEvents: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToReminders: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalView.current.context

    // Read once when the section is shown, then off the main thread whenever the settings state
    // changes, instead of re-reading every nickname and trigger map on each recomposition.
    val initialCustomizationFlags = remember(context) { readSearchCustomizationFlags(context) }
    val customizationFlags by
        produceState(initialValue = initialCustomizationFlags, context, state) {
            value = withContext(Dispatchers.IO) { readSearchCustomizationFlags(context) }
        }
    val hasNicknames = customizationFlags.first
    val hasTriggers = customizationFlags.second

    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedOtherItems.isNotEmpty()

    val sectionOrder =
        ItemPriorityConfig
            .getSearchResultsPriority()
            .filter { section ->
                    section in com.tk.quicksearch.search.core.SearchSectionRegistry.settingsSections
            }
    val allSectionsDisabled = sectionOrder.isNotEmpty() && sectionOrder.all { it in state.disabledSections }

    Column(modifier = modifier) {
        // Search Sections Section
        SectionSettingsSection(
            sectionOrder = sectionOrder,
            disabledSections = state.disabledSections,
            onToggleSection = callbacks.onToggleSection,
            sectionAliasCodes = state.shortcutCodes,
            onSetSectionAlias = callbacks.onSetSearchSectionAlias,
            appsSubtitle = stringResource(R.string.settings_manage_apps_desc),
            onAppsClick = onNavigateToAppManagement,
            appShortcutsSubtitle = stringResource(R.string.settings_manage_shortcuts_desc),
            onAppShortcutsClick = onNavigateToAppShortcuts,
            contactsSubtitle =
                if (hasContactPermission) {
                    stringResource(R.string.settings_manage_calls_texts_contacts_desc)
                } else {
                    stringResource(R.string.permission_required_title)
                },
            onContactsClick = if (hasContactPermission) onNavigateToCallsTexts else null,
            filesSubtitle =
                if (hasFilePermission) {
                    stringResource(R.string.settings_manage_files_desc)
                } else {
                    stringResource(R.string.permission_required_title)
                },
            onFilesClick = if (hasFilePermission) onNavigateToFiles else null,
            deviceSettingsSubtitle = stringResource(R.string.settings_view_all_desc),
            onDeviceSettingsClick = onNavigateToDeviceSettings,
            appSettingsSubtitle = stringResource(R.string.settings_app_settings_desc),
            calendarSubtitle =
                if (hasCalendarPermission) {
                    stringResource(R.string.settings_calendar_view_all_events_desc)
                } else {
                    stringResource(R.string.permission_required_title)
                },
            onCalendarClick =
                if (hasCalendarPermission) {
                    onNavigateToCalendarEvents
                } else {
                    null
                },
            notesSubtitle = stringResource(R.string.settings_notes_view_all_desc),
            onNotesClick = onNavigateToNotes,
            remindersSubtitle = stringResource(R.string.settings_reminders_view_all_desc),
            onRemindersClick = onNavigateToReminders,
            bookmarksSubtitle = stringResource(R.string.settings_bookmarks_view_all_desc),
            onBookmarksClick = onNavigateToBookmarks,
            sectionsWithHiddenAlias = buildSet {
                if (!hasContactPermission) add(SearchSection.CONTACTS)
                if (!hasFilePermission) add(SearchSection.FILES)
                if (!hasCalendarPermission) add(SearchSection.CALENDAR)
            },
            showTitle = false,
        )

        TopMatchesCard(
            topMatchesEnabled = state.topMatchesEnabled,
            onTopMatchesToggle = { enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.Toggle(
                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.TOP_MATCHES,
                        enabled = enabled,
                    ),
                )
            },
            topMatchesLimit = state.topMatchesLimit,
            onTopMatchesLimitChange = { limit ->
                callbacks.onApplySettingsCommand(SettingsCommand.TopMatchesLimit(limit))
            },
            topMatchesSectionOrder = state.topMatchesSectionOrder,
            disabledTopMatchesSections = state.disabledTopMatchesSections,
            onTopMatchesSectionOrderChange = { order ->
                callbacks.onApplySettingsCommand(SettingsCommand.TopMatchesSectionOrder(order))
            },
            onTopMatchesSectionEnabledChange = { section, enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.TopMatchesSectionEnabled(section, enabled),
                )
            },
            isLocked = allSectionsDisabled,
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        SearchOptionsCard(
            appSuggestionsEnabled = state.appSuggestionsEnabled,
            enabledAppSuggestionTabs = state.enabledAppSuggestionTabs,
            hasUsagePermission = hasUsagePermission,
            onAppSuggestionsToggle = { enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.Toggle(
                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.APP_SUGGESTIONS,
                        enabled = enabled,
                    ),
                )
            },
            onAppSuggestionTabEnabledChange = { tab, enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.AppSuggestionTabEnabled(tab = tab, enabled = enabled),
                )
            },
            onRequestUsagePermission = callbacks.onRequestUsagePermission,
            webSuggestionsEnabled = state.webSuggestionsEnabled,
            onWebSuggestionsToggle = { enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.Toggle(
                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.WEB_SUGGESTIONS,
                        enabled = enabled,
                    ),
                )
            },
            webSuggestionsCount = state.webSuggestionsCount,
            onWebSuggestionsCountChange = { count ->
                callbacks.onApplySettingsCommand(SettingsCommand.WebSuggestionsCount(count))
            },
            recentQueriesEnabled = state.recentQueriesEnabled,
            onRecentQueriesToggle = { enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.Toggle(
                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.RECENT_QUERIES,
                        enabled = enabled,
                    ),
                )
            },
            recentQueriesDisplayCount = state.recentQueriesDisplayCount,
            onRecentQueriesDisplayCountChange = { count ->
                callbacks.onApplySettingsCommand(SettingsCommand.RecentQueriesDisplayCount(count))
            },
            appResultsEnabled = SearchSection.APPS !in state.disabledSections,
            appResultRowCount = state.appResultRowCount,
            onAppResultRowCountChange = { rowCount ->
                callbacks.onApplySettingsCommand(SettingsCommand.AppResultRowCount(rowCount))
            },
            fuzzySearchEnabled = state.fuzzySearchEnabled,
            fuzzySearchAvailable = state.fuzzySearchAvailable,
            onFuzzySearchToggle = { enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.Toggle(
                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.FUZZY_SEARCH,
                        enabled = enabled,
                    ),
                )
            },
            secondaryRankingSignal = state.secondaryRankingSignal,
            onSecondaryRankingSignalChange = { signal ->
                callbacks.onApplySettingsCommand(SettingsCommand.SecondaryRanking(signal))
            },
            hasExcludedItems = hasExcludedItems,
            hasNicknames = hasNicknames,
            hasTriggers = hasTriggers,
            excludedItemsTitle = stringResource(R.string.settings_excluded_items_title),
            excludedItemsDescription = stringResource(R.string.settings_excluded_items_desc),
            onNavigateToExcludedItems = onNavigateToExcludedItems,
            onNavigateToNicknames = onNavigateToNicknames,
            onNavigateToTriggers = onNavigateToTriggers,
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

/** Whether any nickname (first) and any trigger (second) is saved. Reads preferences; call sparingly. */
private fun readSearchCustomizationFlags(context: android.content.Context): Pair<Boolean, Boolean> {
    val hasNicknames =
        NicknamePreferences(context).run {
            getAllAppNicknames().isNotEmpty() ||
                getAllAppShortcutNicknames().isNotEmpty() ||
                getAllContactNicknames().isNotEmpty() ||
                getAllFileNicknames().isNotEmpty() ||
                getAllSettingNicknames().isNotEmpty() ||
                getAllCalendarEventNicknames().isNotEmpty()
        }
    val hasTriggers =
        TriggerPreferences(context).run {
            getAllAppTriggers().isNotEmpty() ||
                getAllAppShortcutTriggers().isNotEmpty() ||
                getAllContactTriggers().isNotEmpty() ||
                getAllContactActionTriggers().isNotEmpty() ||
                getAllFileTriggers().isNotEmpty() ||
                getAllSettingTriggers().isNotEmpty() ||
                getAllNoteTriggers().isNotEmpty()
        }
    return hasNicknames to hasTriggers
}
