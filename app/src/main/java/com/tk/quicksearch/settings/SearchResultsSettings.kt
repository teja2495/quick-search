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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.NicknamePreferences
import com.tk.quicksearch.search.data.preferences.TriggerPreferences
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import sh.calvin.reorderable.ReorderableColumn

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
    hasNicknames: Boolean,
    hasTriggers: Boolean,
    excludedItemsTitle: String,
    excludedItemsDescription: String,
    onNavigateToExcludedItems: () -> Unit,
    onNavigateToNicknames: () -> Unit,
    onNavigateToTriggers: () -> Unit,
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

            val hasAnyNavigationRows = hasNicknames || hasTriggers || hasExcludedItems
            var hasRenderedNavigationRow = false

            if (hasAnyNavigationRows) {
                HorizontalDivider(color = AppColors.SettingsDivider)
            }

            if (hasNicknames) {
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.settings_nicknames_title),
                            description = stringResource(R.string.settings_nicknames_desc),
                            icon = Icons.Rounded.Label,
                            actionOnPress = onNavigateToNicknames,
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )
                hasRenderedNavigationRow = true
            }

            if (hasTriggers) {
                if (hasRenderedNavigationRow) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.settings_triggers_title),
                            description = stringResource(R.string.settings_triggers_desc),
                            icon = Icons.Rounded.Bolt,
                            actionOnPress = onNavigateToTriggers,
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )
                hasRenderedNavigationRow = true
            }

            if (hasExcludedItems) {
                if (hasRenderedNavigationRow) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
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
private fun TopMatchesCard(
    topMatchesEnabled: Boolean,
    onTopMatchesToggle: (Boolean) -> Unit,
    topMatchesLimit: Int,
    onTopMatchesLimitChange: (Int) -> Unit,
    topMatchesSectionOrder: List<SearchSection>,
    disabledTopMatchesSections: Set<SearchSection>,
    onTopMatchesSectionOrderChange: (List<SearchSection>) -> Unit,
    onTopMatchesSectionEnabledChange: (SearchSection, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPriorityDialog by rememberSaveable { mutableStateOf(false) }
    val priorityItems =
        remember(topMatchesSectionOrder) {
            topMatchesSectionOrder.filter { section -> FeatureFlags.isSearchSectionEnabled(section) }
        }

    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.top_matches_toggle_title),
                subtitle = stringResource(R.string.top_matches_toggle_desc),
                checked = topMatchesEnabled,
                onCheckedChange = onTopMatchesToggle,
                leadingIcon = Icons.Rounded.AutoAwesome,
                isFirstItem = true,
                isLastItem = !topMatchesEnabled,
            )

            if (topMatchesEnabled) {
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.top_matches_priority_title),
                            description = stringResource(R.string.top_matches_priority_desc),
                            icon = Icons.Rounded.Reorder,
                            actionOnPress = { showPriorityDialog = true },
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                TopMatchesLimitChips(
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
        )
    }
}

@Composable
private fun TopMatchesLimitChips(
    selectedLimit: Int,
    onLimitSelected: (Int) -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
            Text(
                text = stringResource(R.string.top_matches_count_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                com.tk.quicksearch.search.data.preferences.UiPreferences.TOP_MATCHES_LIMIT_OPTIONS.forEach { limit ->
                    val selected = selectedLimit == limit
                    AssistChip(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hapticToggle(view)()
                            onLimitSelected(limit)
                        },
                        label = {
                            Text(
                                text = limit.toString(),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        shape = DesignTokens.ShapeFull,
                        border = if (selected) null else BorderStroke(1.dp, AppColors.SettingsDivider),
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor =
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                labelColor =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityReorderDialog(
    items: List<SearchSection>,
    onItemsChange: (List<SearchSection>) -> Unit,
    disabledSections: Set<SearchSection>,
    onItemEnabledChange: (SearchSection, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.top_matches_priority_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                Text(
                    text = stringResource(R.string.top_matches_priority_dialog_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReorderableColumn(
                    list = items,
                    onSettle = { fromIndex, toIndex ->
                        if (fromIndex != toIndex) {
                            val newOrder =
                                items.toMutableList().apply {
                                    add(toIndex, removeAt(fromIndex))
                                }
                            onItemsChange(newOrder)
                        }
                    },
                    onMove = {
                        performHapticFeedbackSafely(
                            view,
                            HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK,
                        )
                    },
                ) { index, item, isDragging ->
                    if (index > 0) {
                        HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                    Surface(
                        color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            performHapticFeedbackSafely(
                                                view,
                                                HapticFeedbackConstantsCompat.GESTURE_START,
                                            )
                                        },
                                        onDragStopped = {
                                            performHapticFeedbackSafely(
                                                view,
                                                HapticFeedbackConstantsCompat.GESTURE_END,
                                            )
                                        },
                                    )
                                    .padding(
                                        vertical = DesignTokens.SpacingXSmall,
                                        horizontal = DesignTokens.SpacingSmall,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text =
                                    stringResource(
                                        SearchSectionUiMetadataRegistry
                                            .metadataFor(item)
                                            .sectionLabelRes,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                modifier = Modifier.scale(0.7f),
                                checked = item !in disabledSections,
                                onCheckedChange = { enabled -> onItemEnabledChange(item, enabled) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        },
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
            Text(
                text = stringResource(R.string.settings_refresh_data_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            text = stringResource(R.string.section_apps),
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
                                text = stringResource(R.string.contacts_action_button_contacts),
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
                                text = stringResource(R.string.section_files),
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
    onNavigateToNicknames: () -> Unit,
    onNavigateToTriggers: () -> Unit,
    onNavigateToAppManagement: () -> Unit,
    onNavigateToAppShortcuts: () -> Unit,
    onNavigateToCallsTexts: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToDeviceSettings: () -> Unit,
    onNavigateToCalendarEvents: () -> Unit,
    onNavigateToNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalView.current.context

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
                getAllFileTriggers().isNotEmpty() ||
                getAllSettingTriggers().isNotEmpty() ||
                getAllNoteTriggers().isNotEmpty()
        }

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
                    stringResource(R.string.permission_required_title)
                },
            onContactsClick = if (hasContactPermission) onNavigateToCallsTexts else null,
            onContactsClickNoRipple = hasContactPermission,
            filesSubtitle =
                if (hasFilePermission) {
                    stringResource(R.string.settings_manage_files_desc)
                } else {
                    stringResource(R.string.permission_required_title)
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
                    stringResource(R.string.permission_required_title)
                },
            onCalendarClick =
                if (hasCalendarPermission) {
                    onNavigateToCalendarEvents
                } else {
                    null
                },
            onCalendarClickNoRipple = hasCalendarPermission,
            notesSubtitle = stringResource(R.string.settings_notes_view_all_desc),
            onNotesClick = onNavigateToNotes,
            onNotesClickNoRipple = true,
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
            modifier = Modifier.padding(top = DesignTokens.SpacingLarge),
        )

        SearchOptionsCard(
            appSuggestionsEnabled = state.appSuggestionsEnabled,
            hasUsagePermission = hasUsagePermission,
            onAppSuggestionsToggle = { enabled ->
                callbacks.onApplySettingsCommand(
                    SettingsCommand.Toggle(
                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.APP_SUGGESTIONS,
                        enabled = enabled,
                    ),
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
