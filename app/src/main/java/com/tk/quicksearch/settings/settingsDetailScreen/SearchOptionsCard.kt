package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.tk.quicksearch.shared.ui.components.AppDialogSurface
import com.tk.quicksearch.shared.ui.components.AnimatedSegmentedTiles
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.AppTextButton

private val RecentQueriesDisplayCountOptions = UiPreferences.RECENT_QUERIES_DISPLAY_COUNT_OPTIONS

private fun recentQueriesDisplayCountOptionIndex(count: Int): Int =
    RecentQueriesDisplayCountOptions.indexOf(count).coerceAtLeast(0)

private fun recentQueriesDisplayCountForOptionIndex(index: Int): Int =
    RecentQueriesDisplayCountOptions[index.coerceIn(RecentQueriesDisplayCountOptions.indices)]

/** Card for app suggestions, web suggestions, recent queries and excluded items. */
@Composable
internal fun SearchOptionsCard(
    appSuggestionsEnabled: Boolean,
    enabledAppSuggestionTabs: Set<AppSuggestionTabType>,
    hasUsagePermission: Boolean,
    onAppSuggestionsToggle: (Boolean) -> Unit,
    onAppSuggestionTabEnabledChange: (AppSuggestionTabType, Boolean) -> Unit,
    onRequestUsagePermission: () -> Unit,
    webSuggestionsEnabled: Boolean,
    onWebSuggestionsToggle: (Boolean) -> Unit,
    webSuggestionsCount: Int,
    onWebSuggestionsCountChange: (Int) -> Unit,
    recentQueriesEnabled: Boolean,
    onRecentQueriesToggle: (Boolean) -> Unit,
    recentQueriesDisplayCount: Int,
    onRecentQueriesDisplayCountChange: (Int) -> Unit,
    appResultsEnabled: Boolean,
    appResultRowCount: Int,
    onAppResultRowCountChange: (Int) -> Unit,
    fuzzySearchEnabled: Boolean,
    fuzzySearchAvailable: Boolean,
    onFuzzySearchToggle: (Boolean) -> Unit,
    secondaryRankingSignal: SecondaryRankingSignal,
    onSecondaryRankingSignalChange: (SecondaryRankingSignal) -> Unit,
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
    var lastRecentQueriesDisplayCount by remember { mutableStateOf(recentQueriesDisplayCount) }
    var showAppSuggestionTabsDialog by rememberSaveable { mutableStateOf(false) }
    var showSecondaryRankingDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var pinnedNotificationItems by remember { mutableStateOf(PinnedNotifications.pinnedItems(context)) }
    var showPinnedNotificationItemsDialog by rememberSaveable { mutableStateOf(false) }

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
    LaunchedEffect(recentQueriesEnabled) {
        if (!recentQueriesEnabled) {
            lastRecentQueriesDisplayCount = recentQueriesDisplayCount
        }
    }

    SegmentedGroup(modifier = modifier) {
        AnimatedSegmentedTiles {
            tile {
                SearchResultsSliderToggle(
                    title = stringResource(R.string.web_search_suggestions_title),
                    checked = webSuggestionsEnabled,
                    onCheckedChange = onWebSuggestionsToggle,
                    leadingIcon = MaterialSymbols.Language,
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
                )
            }
            tile {
                SearchResultsSliderToggle(
                    title = stringResource(R.string.recent_queries_toggle_title),
                    subtitle = stringResource(R.string.recent_queries_toggle_desc),
                    checked = recentQueriesEnabled,
                    onCheckedChange = onRecentQueriesToggle,
                    leadingIcon = MaterialSymbols.History,
                    sliderDetails =
                        if (recentQueriesEnabled) {
                            SettingsToggleSliderDetails(
                                value = recentQueriesDisplayCountOptionIndex(recentQueriesDisplayCount).toFloat(),
                                onValueChange = { value ->
                                    val count = recentQueriesDisplayCountForOptionIndex(value.toInt())
                                    if (count != lastRecentQueriesDisplayCount) {
                                        hapticToggle(view)()
                                        lastRecentQueriesDisplayCount = count
                                    }
                                    onRecentQueriesDisplayCountChange(count)
                                },
                                valueRange = 0f..4f,
                                steps = 3,
                                valueLabel = recentQueriesDisplayCount.toString(),
                                description = stringResource(R.string.recent_queries_toggle_desc),
                            )
                        } else {
                            null
                        },
                )
            }
            tile(visible = fuzzySearchAvailable) {
                SettingsToggleRow(
                    title = stringResource(R.string.fuzzy_search_toggle_title),
                    subtitle = stringResource(R.string.fuzzy_search_toggle_desc),
                    checked = fuzzySearchEnabled,
                    onCheckedChange = onFuzzySearchToggle,
                    leadingIcon = MaterialSymbols.AutoAwesome,
                    showDivider = false,
                )
            }
            tile {
                SearchOptionsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.secondary_ranking_title),
                            description = secondaryRankingDescription(secondaryRankingSignal),
                            icon = MaterialSymbols.Reorder,
                            actionOnPress = { showSecondaryRankingDialog = true },
                        ),
                )
            }
            tile(visible = appResultsEnabled) {
                AppResultRowsSelector(
                    selectedRowCount = appResultRowCount,
                    onSelectRowCount = onAppResultRowCountChange,
                )
            }
            tile {
                SettingsToggleRow(
                    title = stringResource(R.string.app_suggestions_toggle_title),
                    subtitle = stringResource(R.string.app_suggestions_toggle_desc),
                    checked = appSuggestionsEnabled,
                    onCheckedChange = onAppSuggestionsToggle,
                    leadingIcon = MaterialSymbols.Apps,
                    showTipBanner = appSuggestionsEnabled && !hasUsagePermission && !appSuggestionsTipDismissed,
                    tipBannerText = stringResource(R.string.app_suggestions_usage_access_tip),
                    tipBannerLinkText = stringResource(R.string.app_suggestions_usage_access_link),
                    onTipBannerLinkClick = onRequestUsagePermission,
                    onTipBannerDismiss = { appSuggestionsTipDismissed = true },
                    showDivider = false,
                )
            }
            tile(visible = appSuggestionsEnabled) {
                AppSuggestionTabsPickerRow(
                    enabledTabs = enabledAppSuggestionTabs,
                    onClick = { showAppSuggestionTabsDialog = true },
                )
            }
            tile(visible = pinnedNotificationItems.isNotEmpty()) {
                SearchOptionsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.notification_pinned_items_title),
                            description = stringResource(R.string.notification_pinned_items_description),
                            icon = MaterialSymbols.Notifications,
                            actionOnPress = { showPinnedNotificationItemsDialog = true },
                        ),
                )
            }
            tile(visible = hasNicknames) {
                SearchOptionsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.settings_nicknames_title),
                            description = stringResource(R.string.settings_nicknames_desc),
                            icon = MaterialSymbols.Label,
                            actionOnPress = onNavigateToNicknames,
                        ),
                )
            }
            tile(visible = hasTriggers) {
                SearchOptionsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.settings_triggers_title),
                            description = stringResource(R.string.settings_triggers_desc),
                            icon = MaterialSymbols.Bolt,
                            actionOnPress = onNavigateToTriggers,
                        ),
                )
            }
            tile(visible = hasExcludedItems) {
                SearchOptionsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = excludedItemsTitle,
                            description = excludedItemsDescription,
                            icon = MaterialSymbols.VisibilityOff,
                            actionOnPress = onNavigateToExcludedItems,
                        ),
                )
            }
        }
    }

    if (showAppSuggestionTabsDialog) {
        AppSuggestionTabsDialog(
            enabledTabs = enabledAppSuggestionTabs,
            onTabEnabledChange = onAppSuggestionTabEnabledChange,
            onDismiss = { showAppSuggestionTabsDialog = false },
        )
    }

    if (showPinnedNotificationItemsDialog) {
        PinnedNotificationItemsDialog(
            items = pinnedNotificationItems,
            onRemove = { action ->
                PinnedNotifications.remove(context, action)
                pinnedNotificationItems = pinnedNotificationItems.filterNot { it.toJson() == action.toJson() }
                if (pinnedNotificationItems.isEmpty()) showPinnedNotificationItemsDialog = false
            },
            onReorder = { reorderedItems ->
                PinnedNotifications.reorder(context, reorderedItems)
                pinnedNotificationItems = reorderedItems
            },
            onDismiss = { showPinnedNotificationItemsDialog = false },
        )
    }

    if (showSecondaryRankingDialog) {
        SecondaryRankingDialog(
            selectedSignal = secondaryRankingSignal,
            onSignalSelected = onSecondaryRankingSignalChange,
            onDismiss = { showSecondaryRankingDialog = false },
        )
    }
}

@Composable
private fun secondaryRankingDescription(signal: SecondaryRankingSignal): String {
    val description = stringResource(R.string.secondary_ranking_dialog_desc)
    val currentSelection =
        secondaryRankingCurrentSelectionLabel(
            signal = signal,
            recencyLabel = stringResource(R.string.secondary_ranking_recency),
            mostOpenedLabel = stringResource(R.string.secondary_ranking_most_opened),
        )
    val currentLine =
        currentSelection?.let {
            stringResource(R.string.settings_app_language_desc, it)
        }
    return buildSecondaryRankingDescription(description, currentLine)
}

private fun secondaryRankingCurrentSelectionLabel(
    signal: SecondaryRankingSignal,
    recencyLabel: String,
    mostOpenedLabel: String,
): String? =
    when (signal) {
        SecondaryRankingSignal.RECENCY -> recencyLabel
        SecondaryRankingSignal.MOST_OPENED -> mostOpenedLabel
        SecondaryRankingSignal.NONE -> null
    }

private fun buildSecondaryRankingDescription(
    description: String,
    currentSelectionLine: String?,
): String = currentSelectionLine?.let { "$description\n$it" } ?: description

@Composable
private fun AppResultRowsSelector(
    selectedRowCount: Int,
    onSelectRowCount: (Int) -> Unit,
) {
    SegmentedTileContent {
        Text(
            text = stringResource(R.string.settings_app_result_rows_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ConnectedChoiceGroup(
            options = listOf(1, 2).map { ChoiceOption(it, it.toString()) },
            selected = selectedRowCount,
            onSelect = onSelectRowCount,
        )
    }
}

@Composable
private fun SearchOptionsNavigationRow(item: SettingsCardItem) {
    SegmentedTileRow(
        title = item.title,
        description = item.description,
        leading = { SettingsIconBadge(item) },
        modifier = Modifier.clickable(role = Role.Button, onClick = item.actionOnPress),
    )
}

@Composable
internal fun PinnedNotificationItemsDialog(
    items: List<CustomWidgetButtonAction>,
    onRemove: (CustomWidgetButtonAction) -> Unit,
    onReorder: (List<CustomWidgetButtonAction>) -> Unit,
    onDismiss: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val view = LocalView.current
    val reorderableItems = remember(items) { items.toMutableStateList() }
    val moveUp = stringResource(R.string.action_move_up)
    val moveDown = stringResource(R.string.action_move_down)
    val listState = rememberLazyListState()
    var reorderedDuringDrag by remember { mutableStateOf(false) }
    val reorderableState =
        rememberReorderableLazyListState(listState) { from, to ->
            if (from.index != to.index) {
                reorderableItems.add(to.index, reorderableItems.removeAt(from.index))
                reorderedDuringDrag = true
                performHapticFeedbackSafely(
                    view,
                    HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK,
                )
            }
        }
    AppDialogSurface(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().height(screenHeight * 0.65f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(
                        horizontal = DesignTokens.SpacingXXLarge,
                        vertical = DesignTokens.SpacingXLarge,
                    ),
        ) {
            Text(
                text = stringResource(R.string.notification_pinned_items_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignTokens.SpacingMedium),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                itemsIndexed(
                    items = reorderableItems,
                    key = { _, action -> action.toJson() },
                ) { index, action ->
                    ReorderableItem(
                        state = reorderableState,
                        key = action.toJson(),
                    ) { isDragging ->
                        fun moveBy(delta: Int): Boolean {
                            val current = reorderableItems.indexOf(action)
                            val target = current + delta
                            if (current < 0 || target !in reorderableItems.indices) return false
                            reorderableItems.add(target, reorderableItems.removeAt(current))
                            onReorder(reorderableItems.toList())
                            return true
                        }
                        if (index > 0) HorizontalDivider(color = AppColors.SettingsDivider)
                        val category = stringResource(action.type.categoryRes)
                        Surface(
                            color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(vertical = DesignTokens.SpacingSmall),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.DragHandle,
                                    contentDescription = stringResource(R.string.settings_action_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .size(DesignTokens.MinTouchTarget)
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    reorderedDuringDrag = false
                                                    performHapticFeedbackSafely(
                                                        view,
                                                        HapticFeedbackConstantsCompat.GESTURE_START,
                                                    )
                                                },
                                                onDragStopped = {
                                                    if (reorderedDuringDrag) {
                                                        onReorder(reorderableItems.toList())
                                                    }
                                                    performHapticFeedbackSafely(
                                                        view,
                                                        HapticFeedbackConstantsCompat.GESTURE_END,
                                                    )
                                                },
                                            )
                                            .padding(DesignTokens.SpacingMedium),
                                )
                                Column(
                                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {
                                        customActions = buildList {
                                            if (index > 0) add(CustomAccessibilityAction(moveUp) { moveBy(-1) })
                                            if (index < reorderableItems.lastIndex) add(CustomAccessibilityAction(moveDown) { moveBy(1) })
                                        }
                                    },
                                ) {
                                    Text(
                                        text = action.displayLabel(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                AppIconButton(onClick = { onRemove(action) }, tooltip = stringResource(
                                            R.string.notification_pinned_items_remove,
                                            action.displayLabel(),
                                        )) {
                                    Icon(
                                        imageVector = MaterialSymbols.DeleteOutline,
                                        contentDescription = stringResource(
                                            R.string.notification_pinned_items_remove,
                                            action.displayLabel(),
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AppTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@Composable
private fun AppSuggestionTabsPickerRow(
    enabledTabs: Set<AppSuggestionTabType>,
    onClick: () -> Unit,
) {
    val tabLabels = rememberAppSuggestionTabLabels()
    val enabledTabsSummary =
        remember(enabledTabs, tabLabels) {
            buildAppSuggestionTabsSummary(enabledTabs, tabLabels)
        }

    SegmentedTileRow(
        title = stringResource(R.string.app_suggestions_enabled_tabs_picker_hint),
        description = stringResource(R.string.settings_app_language_desc, enabledTabsSummary),
        leading = { TonalIconBadge(icon = MaterialSymbols.Reorder) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
