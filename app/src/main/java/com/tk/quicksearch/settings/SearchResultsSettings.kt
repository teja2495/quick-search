package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import androidx.compose.material.icons.rounded.EventBusy
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import com.tk.quicksearch.settings.shared.SectionSettingsSection
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.NicknamePreferences
import com.tk.quicksearch.search.data.preferences.TriggerPreferences
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val RecentQueriesDisplayCountOptions = listOf(1, 3, 5, 7, 10)

private fun recentQueriesDisplayCountOptionIndex(count: Int): Int =
    RecentQueriesDisplayCountOptions.indexOf(count).coerceAtLeast(0)

private fun recentQueriesDisplayCountForOptionIndex(index: Int): Int =
    RecentQueriesDisplayCountOptions[index.coerceIn(RecentQueriesDisplayCountOptions.indices)]

/** Card for app suggestions, web suggestions, recent queries and excluded items. */
@Composable
private fun SearchOptionsCard(
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
    val reminderPreferences = remember(context) { ReminderPreferences(context) }
    var includePastReminders by remember { mutableStateOf(reminderPreferences.getIncludePastReminders()) }

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
                title = stringResource(R.string.recent_queries_toggle_title),
                subtitle = stringResource(R.string.recent_queries_toggle_desc),
                checked = recentQueriesEnabled,
                onCheckedChange = onRecentQueriesToggle,
                leadingIcon = Icons.Rounded.History,
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
                isFirstItem = false,
                isLastItem = false,
            )

            HorizontalDivider(color = AppColors.SettingsDivider)
            SettingsToggleRow(
                title = stringResource(R.string.settings_include_past_reminders_title),
                subtitle = stringResource(R.string.settings_include_past_reminders_desc),
                checked = includePastReminders,
                onCheckedChange = { enabled ->
                    includePastReminders = enabled
                    reminderPreferences.setIncludePastReminders(enabled)
                    ReminderRepository.notifyChanged()
                },
                leadingIcon = Icons.Rounded.EventBusy,
                isFirstItem = false,
                isLastItem = false,
                showDivider = false,
            )

            if (fuzzySearchAvailable) {
                HorizontalDivider(color = AppColors.SettingsDivider)
                SettingsToggleRow(
                    title = stringResource(R.string.fuzzy_search_toggle_title),
                    subtitle = stringResource(R.string.fuzzy_search_toggle_desc),
                    checked = fuzzySearchEnabled,
                    onCheckedChange = onFuzzySearchToggle,
                    leadingIcon = Icons.Rounded.AutoAwesome,
                    isFirstItem = false,
                    isLastItem = false,
                    showDivider = false,
                )
            }
            HorizontalDivider(color = AppColors.SettingsDivider)

            SettingsNavigationRow(
                item =
                    SettingsCardItem(
                        title = stringResource(R.string.secondary_ranking_title),
                        description = secondaryRankingDescription(secondaryRankingSignal),
                        icon = Icons.Rounded.Reorder,
                        actionOnPress = { showSecondaryRankingDialog = true },
                    ),
                contentPadding =
                    PaddingValues(
                        horizontal = DesignTokens.SpacingXXLarge,
                        vertical = DesignTokens.SpacingLarge,
                    ),
            )
            HorizontalDivider(color = AppColors.SettingsDivider)

            if (appResultsEnabled) {
                AppResultRowsSelector(
                    selectedRowCount = appResultRowCount,
                    onSelectRowCount = onAppResultRowCountChange,
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
            }

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
                showDivider = false,
            )
            if (appSuggestionsEnabled) {
                AppSuggestionTabsPickerRow(
                    enabledTabs = enabledAppSuggestionTabs,
                    onClick = { showAppSuggestionTabsDialog = true },
                )
            }

            if (pinnedNotificationItems.isNotEmpty()) {
                HorizontalDivider(color = AppColors.SettingsDivider)
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.notification_pinned_items_title),
                            description = stringResource(R.string.notification_pinned_items_description),
                            icon = Icons.Rounded.Notifications,
                            actionOnPress = { showPinnedNotificationItemsDialog = true },
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )
            }

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
    val view = LocalView.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingMedium,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_app_result_rows_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
            listOf(1, 2).forEach { rowCount ->
                val selected = selectedRowCount == rowCount
                AssistChip(
                    onClick = {
                        hapticToggle(view)()
                        onSelectRowCount(rowCount)
                    },
                    label = { Text(rowCount.toString()) },
                    shape = DesignTokens.ShapeFull,
                    border = if (selected) null else BorderStroke(1.dp, AppColors.SettingsDivider),
                    colors =
                        AssistChipDefaults.assistChipColors(
                            containerColor =
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            labelColor =
                                if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        }
    }

}

@Composable
private fun PinnedNotificationItemsDialog(
    items: List<CustomWidgetButtonAction>,
    onRemove: (CustomWidgetButtonAction) -> Unit,
    onReorder: (List<CustomWidgetButtonAction>) -> Unit,
    onDismiss: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val view = LocalView.current
    val reorderableItems = remember(items) { items.toMutableStateList() }
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(screenHeight * 0.65f),
            shape = MaterialTheme.shapes.extraLarge,
            color = AppColors.DialogBackground,
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
                            if (index > 0) HorizontalDivider(color = AppColors.SettingsDivider)
                            val category =
                                when (action.type) {
                                    CustomWidgetButtonType.APP -> stringResource(R.string.notification_pinned_item_type_app)
                                    CustomWidgetButtonType.APP_SHORTCUT -> stringResource(R.string.notification_pinned_item_type_app_shortcut)
                                    CustomWidgetButtonType.CONTACT -> stringResource(R.string.notification_pinned_item_type_contact)
                                    CustomWidgetButtonType.FILE -> stringResource(R.string.notification_pinned_item_type_file)
                                    CustomWidgetButtonType.SETTING -> stringResource(R.string.settings_gesture_settings)
                                    CustomWidgetButtonType.NOTE -> stringResource(R.string.notification_pinned_item_type_note)
                                }
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
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = stringResource(R.string.settings_action_reorder),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier =
                                            Modifier
                                                .size(DesignTokens.LargeIconSize)
                                                .padding(end = DesignTokens.SpacingMedium)
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
                                                ),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
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
                                    IconButton(onClick = { onRemove(action) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
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
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_close))
                    }
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start =
                        DesignTokens.SpacingXXLarge +
                            DesignTokens.IconSizeSmall +
                            DesignTokens.ItemRowSpacing,
                    top = DesignTokens.SpacingXXSmall,
                    end = DesignTokens.SpacingXXLarge,
                    bottom = DesignTokens.SpacingMedium,
                )
                .clip(MaterialTheme.shapes.extraLarge)
                .border(
                    width = 1.dp,
                    color = AppColors.SettingsDivider,
                    shape = MaterialTheme.shapes.extraLarge,
                )
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingMedium,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.app_suggestions_enabled_tabs_picker_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_app_language_desc, enabledTabsSummary),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppSuggestionTabsDialog(
    enabledTabs: Set<AppSuggestionTabType>,
    onTabEnabledChange: (AppSuggestionTabType, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val tabLabels = rememberAppSuggestionTabLabels()
    val pinnedAppsFallbackTab =
        if (AppSuggestionTabType.RECENTS in enabledTabs) {
            requireNotNull(tabLabels[AppSuggestionTabType.RECENTS])
        } else {
            requireNotNull(tabLabels[AppSuggestionTabType.MOST_USED])
        }
    val pinnedTabDisabledDescription =
        stringResource(
            R.string.app_suggestions_tab_pinned_disabled_description,
            pinnedAppsFallbackTab,
        )
    val configurableTabs =
        remember(tabLabels) {
            listOf(
                AppSuggestionTabType.PINNED to requireNotNull(tabLabels[AppSuggestionTabType.PINNED]),
                AppSuggestionTabType.RECENTS to requireNotNull(tabLabels[AppSuggestionTabType.RECENTS]),
                AppSuggestionTabType.NEW_UPDATED to requireNotNull(tabLabels[AppSuggestionTabType.NEW_UPDATED]),
                AppSuggestionTabType.MOST_USED to requireNotNull(tabLabels[AppSuggestionTabType.MOST_USED]),
            )
        }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.app_suggestions_tabs_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                Text(
                    text = stringResource(R.string.app_suggestions_tabs_dialog_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (AppSuggestionTabType.PINNED !in enabledTabs) {
                    TipBanner(
                        text = pinnedTabDisabledDescription,
                        showDismissButton = false,
                        modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                    )
                }
                configurableTabs.forEachIndexed { index, (tab, title) ->
                    if (index > 0) {
                        HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                    val isEnabled = tab in enabledTabs
                    AppSuggestionTabCheckboxItem(
                        title = title,
                        subtitle = null,
                        checked = isEnabled,
                        onCheckedChange = { enabled -> onTabEnabledChange(tab, enabled) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_done))
            }
        },
    )
}

@Composable
private fun rememberAppSuggestionTabLabels(): Map<AppSuggestionTabType, String> =
    mapOf(
        AppSuggestionTabType.PINNED to stringResource(R.string.app_suggestions_tab_pinned),
        AppSuggestionTabType.RECENTS to stringResource(R.string.app_suggestions_tab_recent),
        AppSuggestionTabType.NEW_UPDATED to stringResource(R.string.app_suggestions_tab_new_updated),
        AppSuggestionTabType.MOST_USED to stringResource(R.string.common_most_used),
    )

private fun buildAppSuggestionTabsSummary(
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
private fun AppSuggestionTabCheckboxItem(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clickable { onCheckedChange(!checked) }
                .padding(
                    vertical = DesignTokens.SpacingXSmall,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.82f),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    isLocked: Boolean = false,
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
                title = stringResource(R.string.top_matches_title),
                subtitle = stringResource(R.string.top_matches_toggle_desc),
                checked = topMatchesEnabled,
                onCheckedChange = onTopMatchesToggle,
                leadingIcon = Icons.Rounded.AutoAwesome,
                enabled = !isLocked,
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

/**
 * Drag-to-reorder dialog for search sections. Per-section enable switches are shown only when
 * [onItemEnabledChange] is provided.
 */
@Composable
internal fun PriorityReorderDialog(
    items: List<SearchSection>,
    onItemsChange: (List<SearchSection>) -> Unit,
    onDismiss: () -> Unit,
    disabledSections: Set<SearchSection> = emptySet(),
    onItemEnabledChange: ((SearchSection, Boolean) -> Unit)? = null,
    titleRes: Int = R.string.top_matches_priority_title,
    infoRes: Int = R.string.top_matches_priority_dialog_info,
) {
    val view = LocalView.current
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                Text(
                    text = stringResource(infoRes),
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
                                        vertical =
                                            if (onItemEnabledChange != null) {
                                                DesignTokens.SpacingXSmall
                                            } else {
                                                DesignTokens.SpacingMedium
                                            },
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
                            if (onItemEnabledChange != null) Switch(
                                modifier = Modifier.scale(0.7f),
                                checked = item !in disabledSections,
                                onCheckedChange = { enabled ->
                                    if (!enabled && items.count { it !in disabledSections } <= 1 && item !in disabledSections) {
                                        Toast.makeText(
                                            view.context,
                                            view.context.getString(R.string.settings_sections_at_least_one_required),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        onItemEnabledChange(item, enabled)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
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
    onNavigateToReminders: () -> Unit = {},
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
                getAllContactActionTriggers().isNotEmpty() ||
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

    val sectionOrder =
        ItemPriorityConfig
            .getSearchResultsPriority()
            .filter { section -> FeatureFlags.isSearchSectionEnabled(section) }
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
            remindersSubtitle = stringResource(R.string.settings_reminders_view_all_desc),
            onRemindersClick = onNavigateToReminders,
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
