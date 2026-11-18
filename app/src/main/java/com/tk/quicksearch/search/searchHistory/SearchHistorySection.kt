package com.tk.quicksearch.search.searchHistory

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.CollapseButton
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonSegment
import com.tk.quicksearch.search.searchScreen.shared.ResultRowDivider
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import com.tk.quicksearch.search.searchScreen.shared.segmentedRowsActive
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal const val QUERY_ROW_ICON_SIZE = 40
internal const val AI_QUERY_ROW_ICON_SIZE = 34
internal const val QUERY_ICON_START_PADDING = 16
internal const val QUERY_TEXT_START_PADDING = 12
internal const val QUERY_TEXT_END_PADDING = 16
internal const val QUERY_ROW_VERTICAL_PADDING = 10
internal const val SETTINGS_HORIZONTAL_PADDING = 16
internal const val SETTINGS_VERTICAL_PADDING = 4
internal const val SHORTCUT_VERTICAL_PADDING = 4
private const val SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX = 64f
private val SEARCH_HISTORY_EXPANDED_CARD_MIN_HEIGHT = 180.dp

@Composable
fun SearchHistorySection(
    modifier: Modifier = Modifier,
    items: List<RecentSearchItem>,
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    getPrimaryContactCardAction: (Long) -> ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, ContactCardAction) -> Unit,
    onFileClick: (DeviceFile) -> Unit,
    onSettingClick: (DeviceSetting) -> Unit,
    onAppShortcutClick: (StaticShortcut) -> Unit,
    onNoteClick: (NoteInfo) -> Unit = {},
    onAppSettingClick: (AppSettingResult) -> Unit = {},
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = { _, _ -> },
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean = { false },
    appSettingPhoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
    onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit = {},
    appSettingAppResultRowCount: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_APP_RESULT_ROW_COUNT,
    onAppSettingAppResultRowCountChange: (Int) -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    onClearRecentItems: (() -> Unit)? = null,
    isExpanded: Boolean? = null,
    collapsedItemCount: Int = SearchScreenConstants.INITIAL_RESULT_COUNT,
    reverseCollapsedItems: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    collapseRequestKey: Int = 0,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    showWallpaperBackground: Boolean = false,
    isOverlayPresentation: Boolean = false,
    alwaysExpanded: Boolean = false,
    showInlineCollapseButton: Boolean = true,
    selectedTab: SearchHistoryTab? = null,
    onSelectedTabChange: (SearchHistoryTab) -> Unit = {},
    showTabs: Boolean = true,
    // Null means every item came from history and can be removed.
    removableEntryKeys: Set<String>? = null,
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    if (items.isEmpty()) return
    var localExpanded by remember { mutableStateOf(alwaysExpanded) }
    val expanded = alwaysExpanded || (isExpanded ?: localExpanded)
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    // Collapsing restores the keyboard only if it was open when More History was tapped.
    var keyboardOpenBeforeExpand by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var lastCollapseRequestKey by remember { mutableStateOf(collapseRequestKey) }
    var localSelectedTab by rememberSaveable { mutableStateOf(SearchHistoryTab.SEARCHES) }
    val activeSelectedTab = selectedTab ?: localSelectedTab
    val queryItems = items.filterIsInstance<RecentSearchItem.Query>()
    val openedItems = items.filterNot { it is RecentSearchItem.Query }
    val selectedTabItems =
        when (activeSelectedTab) {
            SearchHistoryTab.RECENTLY_OPENED -> openedItems
            else -> queryItems
        }
    val canSwitchTabs = queryItems.isNotEmpty() && openedItems.isNotEmpty()
    val canExpand =
        !alwaysExpanded &&
            (selectedTabItems.size > collapsedItemCount || canSwitchTabs)

    fun updateExpanded(value: Boolean) {
        if (isExpanded == null) {
            localExpanded = value
        }
        onExpandedChange(value)
    }

    fun collapseAndRestoreKeyboard() {
        updateExpanded(false)
        if (keyboardOpenBeforeExpand) keyboardController?.show()
        keyboardOpenBeforeExpand = false
    }

    fun updateSelectedTab(tab: SearchHistoryTab) {
        if (selectedTab == null) {
            localSelectedTab = tab
        }
        onSelectedTabChange(tab)
    }

    BackHandler(enabled = expanded) {
        collapseAndRestoreKeyboard()
    }

    LaunchedEffect(collapseRequestKey) {
        if (collapseRequestKey != lastCollapseRequestKey && !alwaysExpanded && expanded) {
            lastCollapseRequestKey = collapseRequestKey
            collapseAndRestoreKeyboard()
        } else {
            lastCollapseRequestKey = collapseRequestKey
        }
    }

    val textColor =
        if (showWallpaperBackground) AppColors.WallpaperTextPrimary else MaterialTheme.colorScheme.onSurface

    val iconColor =
        if (showWallpaperBackground) AppColors.WallpaperTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        ExpandableResultsCard(
            resultCount = items.size,
            isExpanded = expanded,
            showAllResults = false,
            showExpandControls = canExpand,
            expandedCardMaxHeight = expandedCardMaxHeight,
            hasScrollableContent = scrollState.maxValue > 0,
            fillExpandedHeight = false,
            showWallpaperBackground = showWallpaperBackground,
            overlayCardColor = overlayCardColor,
            segmented = true,
        ) { contentModifier, cardState ->
            val displayAsExpanded = cardState.displayAsExpanded
            val activeItems =
                if (displayAsExpanded) {
                    when (activeSelectedTab) {
                        SearchHistoryTab.RECENTLY_OPENED -> openedItems
                        else -> queryItems
                    }
                } else {
                    selectedTabItems
                }
            val displayItems =
                if (displayAsExpanded) {
                    activeItems
                } else {
                    activeItems
                        .take(collapsedItemCount)
                        .let { items -> if (reverseCollapsedItems) items.reversed() else items }
                }
            val cardLayoutEnabled = LocalSearchCardLayoutEnabled.current
            val listModifier =
                if (displayAsExpanded) {
                    val minHeight = if (cardLayoutEnabled) SEARCH_HISTORY_EXPANDED_CARD_MIN_HEIGHT else 0.dp
                    contentModifier
                        .heightIn(min = minHeight)
                        .searchHistoryTabSwipe(
                            enabled = canSwitchTabs,
                            selectedTab = activeSelectedTab,
                            onTabSelected = ::updateSelectedTab,
                        ).verticalScroll(scrollState)
                } else {
                    contentModifier
                }
            Column(modifier = listModifier) {
                val segmented = segmentedRowsActive()
                val showHistoryTabs = displayAsExpanded && showTabs
                val showClearAllHistory =
                    displayAsExpanded &&
                        onClearRecentItems != null &&
                        displayItems.isNotEmpty()
                val showMoreHistory = !displayAsExpanded && canExpand
                // Segmented: tabs, rows, Clear All and More History are each a tile.
                val firstRowSegment = if (showHistoryTabs) 1 else 0
                val segmentCount =
                    firstRowSegment + displayItems.size +
                        (if (showClearAllHistory) 1 else 0) +
                        (if (showMoreHistory) 1 else 0)

                if (showHistoryTabs) {
                    ResultSegment(index = 0, count = segmentCount) {
                        SearchHistoryTabs(
                            selectedTab = activeSelectedTab,
                            onTabSelected = ::updateSelectedTab,
                            showWallpaperBackground = showWallpaperBackground,
                        )
                    }
                }

                val dividerColor = SearchResultCardDefaults.dividerColor(showWallpaperBackground, overlayDividerColor)
                // Flat rows (card layout off) also separate the last row from More History.
                val showMoreHistoryDivider = showMoreHistory && !cardLayoutEnabled
                displayItems.forEachIndexed { index, item ->
                    val baseShowDivider =
                        !segmented &&
                            (index < displayItems.lastIndex || showClearAllHistory || showMoreHistoryDivider)
                    // Keyed so deleting a row doesn't shift remembered row state onto its neighbours.
                    key(item.entry.stableKey) {
                    ResultSegment(index = firstRowSegment + index, count = segmentCount) {
                    RecentSearchItemRow(
                        item = item,
                        textColor = textColor,
                        iconColor = iconColor,
                        primaryContactButton = primaryContactButton,
                        secondaryContactButton = secondaryContactButton,
                        onRecentQueryClick = onRecentQueryClick,
                        onContactClick = onContactClick,
                        onShowContactMethods = onShowContactMethods,
                        onCallContact = onCallContact,
                        onSmsContact = onSmsContact,
                        onContactMethodClick = onContactMethodClick,
                        getPrimaryContactCardAction = getPrimaryContactCardAction,
                        getSecondaryContactCardAction = getSecondaryContactCardAction,
                        onPrimaryActionLongPress = onPrimaryActionLongPress,
                        onSecondaryActionLongPress = onSecondaryActionLongPress,
                        onCustomAction = onCustomAction,
                        onFileClick = onFileClick,
                        onSettingClick = onSettingClick,
                        onAppShortcutClick = onAppShortcutClick,
                        onNoteClick = onNoteClick,
                        onAppSettingClick = onAppSettingClick,
                        onAppSettingToggle = onAppSettingToggle,
                        isAppSettingToggleChecked = isAppSettingToggleChecked,
                        appSettingPhoneAppGridColumns = appSettingPhoneAppGridColumns,
                        onAppSettingPhoneAppGridColumnsChange = onAppSettingPhoneAppGridColumnsChange,
                        appSettingAppResultRowCount = appSettingAppResultRowCount,
                        onAppSettingAppResultRowCountChange = onAppSettingAppResultRowCountChange,
                        onDeleteRecentItem = onDeleteRecentItem,
                        canRemoveFromHistory =
                            removableEntryKeys == null || item.entry.stableKey in removableEntryKeys,
                        showDivider = baseShowDivider && cardLayoutEnabled,
                        showWallpaperBackground = showWallpaperBackground,
                        overlayDividerColor = overlayDividerColor,
                    )
                    }
                    if (baseShowDivider && !cardLayoutEnabled) {
                        ResultRowDivider(showWallpaperBackground, color = dividerColor)
                    }
                    }
                }

                if (showClearAllHistory) {
                    ResultSegment(index = segmentCount - 1, count = segmentCount, padCompactOuterEdges = true) {
                        ClearAllHistoryRow(onClick = { showClearAllConfirmation = true })
                    }
                }

                if (showMoreHistory) {
                    val onMoreHistoryClick: () -> Unit = {
                        keyboardOpenBeforeExpand = imeInsets.getBottom(density) > 0
                        updateExpanded(true)
                        keyboardController?.hide()
                    }
                    if (segmented) {
                        ExpandButtonSegment(
                            index = segmentCount - 1,
                            count = segmentCount,
                            onClick = onMoreHistoryClick,
                            textResId = R.string.action_more_search_history,
                        )
                    } else {
                        ExpandButton(
                            onClick = onMoreHistoryClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textResId = R.string.action_more_search_history,
                            compact = !cardLayoutEnabled,
                        )
                    }
                }
            }
        }

        if (expanded && !alwaysExpanded && showInlineCollapseButton) {
            CollapseButton(
                showWallpaperBackground = showWallpaperBackground,
                onClick = { collapseAndRestoreKeyboard() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = DesignTokens.SpacingXXLarge),
            )
        }

        if (showClearAllConfirmation && onClearRecentItems != null) {
            ClearAllHistoryConfirmationDialog(
                onConfirm = {
                    showClearAllConfirmation = false
                    onClearRecentItems()
                },
                onDismiss = { showClearAllConfirmation = false },
            )
        }
    }
}

/** Material 3 primary tabs: primary-colored label and a content-width rounded indicator. */
@Composable
private fun SearchHistoryTabs(
    selectedTab: SearchHistoryTab,
    onTabSelected: (SearchHistoryTab) -> Unit,
    showWallpaperBackground: Boolean,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor =
        if (showWallpaperBackground) {
            AppColors.WallpaperTextSecondary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        // The enclosing result card or segment tile supplies the background.
        containerColor = Color.Transparent,
        contentColor = selectedColor,
        modifier = Modifier.fillMaxWidth(),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal, matchContentSize = true),
                width = Dp.Unspecified,
                color = selectedColor,
            )
        },
        divider = {},
    ) {
        SearchHistoryTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { if (selectedTab != tab) onTabSelected(tab) },
                selectedContentColor = selectedColor,
                unselectedContentColor = unselectedColor,
                text = {
                    Text(
                        text =
                            stringResource(
                                when (tab) {
                                    SearchHistoryTab.RECENTLY_OPENED -> R.string.search_history_tab_recently_opened
                                    else -> R.string.search_history_tab_searches
                                },
                            ),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

private fun Modifier.searchHistoryTabSwipe(
    enabled: Boolean,
    selectedTab: SearchHistoryTab,
    onTabSelected: (SearchHistoryTab) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(selectedTab) {
        var totalHorizontalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalHorizontalDrag = 0f },
            onHorizontalDrag = { change, dragAmount ->
                totalHorizontalDrag += dragAmount
                change.consume()
            },
            onDragEnd = {
                when {
                    totalHorizontalDrag <= -SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX &&
                        selectedTab != SearchHistoryTab.RECENTLY_OPENED ->
                        onTabSelected(SearchHistoryTab.RECENTLY_OPENED)

                    totalHorizontalDrag >= SEARCH_HISTORY_TAB_SWIPE_THRESHOLD_PX &&
                        selectedTab != SearchHistoryTab.SEARCHES ->
                        onTabSelected(SearchHistoryTab.SEARCHES)
                }
                totalHorizontalDrag = 0f
            },
            onDragCancel = { totalHorizontalDrag = 0f },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClearAllHistoryConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_clear_all_history_title)) },
        text = {
            Text(
                text = stringResource(R.string.dialog_clear_all_history_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmLabel = stringResource(R.string.action_clear_all_history),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.dialog_cancel),
        destructive = true,
    )
}

@Composable
private fun ClearAllHistoryRow(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val clearColor = MaterialTheme.colorScheme.error
    val minHeight =
        if (LocalSearchCardLayoutEnabled.current) {
            (QUERY_ROW_ICON_SIZE + QUERY_ROW_VERTICAL_PADDING * 2).dp
        } else {
            DesignTokens.MinTouchTarget
        }

    Row(
        modifier =
            Modifier
                .compactEdgeToEdge()
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(AppShapes.card)
                .combinedClickable(
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick,
                ).padding(
                    horizontal = QUERY_TEXT_END_PADDING.dp,
                    vertical = QUERY_ROW_VERTICAL_PADDING.dp,
                ),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MaterialSymbols.DeleteOutline,
            contentDescription = null,
            tint = clearColor,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )

        Text(
            text = stringResource(R.string.action_clear_all_history),
            style = MaterialTheme.typography.labelLarge,
            color = clearColor,
            modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecentQueryRow(
    query: String,
    showAiSearchIcon: Boolean,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardLayoutEnabled = LocalSearchCardLayoutEnabled.current
    val verticalPadding =
        if (cardLayoutEnabled) QUERY_ROW_VERTICAL_PADDING.dp else DesignTokens.SpacingXSmall
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (cardLayoutEnabled) 0.dp else DesignTokens.MinTouchTarget)
                .clip(AppShapes.card)
                .combinedClickable(
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick,
                    onLongClickLabel = stringResource(R.string.desc_show_options),
                    onLongClick = onLongPress,
                ).padding(
                    start = historyRowHorizontalPadding(QUERY_ICON_START_PADDING.dp),
                    end = historyRowHorizontalPadding(QUERY_TEXT_END_PADDING.dp),
                    top = verticalPadding,
                    bottom = verticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAiSearchIcon) {
            Icon(
                painter = painterResource(R.drawable.direct_search),
                contentDescription = stringResource(R.string.settings_direct_search_setup_nav_title),
                tint = Color.Unspecified,
                modifier =
                    Modifier
                        .size(AI_QUERY_ROW_ICON_SIZE.dp)
                        .padding(
                            start = DesignTokens.SpacingXSmall,
                            end = QUERY_TEXT_START_PADDING.dp,
                        ),
            )
        } else {
            Icon(
                imageVector = MaterialSymbols.History,
                contentDescription = stringResource(R.string.common_search),
                tint = iconColor,
                modifier =
                    Modifier
                        .size(QUERY_ROW_ICON_SIZE.dp)
                        .padding(
                            start = DesignTokens.SpacingXSmall,
                            end = QUERY_TEXT_START_PADDING.dp,
                        ),
            )
        }

        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Row inset; segmented tiles already carry it, so rows drop theirs. */
@Composable
internal fun historyRowHorizontalPadding(base: Dp): Dp = if (segmentedRowsActive()) 0.dp else base
