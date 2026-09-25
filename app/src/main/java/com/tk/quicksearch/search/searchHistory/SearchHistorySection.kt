package com.tk.quicksearch.search.searchHistory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultRow
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.CollapseButton
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
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
private const val SEARCH_HISTORY_TAB_ROW_HEIGHT = 56
private val SEARCH_HISTORY_EXPANDED_CARD_MIN_HEIGHT = 180.dp

@Composable
fun SearchHistorySection(
    modifier: Modifier = Modifier,
    items: List<RecentSearchItem>,
    callingApp: CallingApp,
    messagingApp: MessagingApp,
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
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    if (items.isEmpty()) return
    var localExpanded by remember { mutableStateOf(alwaysExpanded) }
    val expanded = alwaysExpanded || (isExpanded ?: localExpanded)
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
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

    fun updateSelectedTab(tab: SearchHistoryTab) {
        if (selectedTab == null) {
            localSelectedTab = tab
        }
        onSelectedTabChange(tab)
    }

    BackHandler(enabled = expanded) {
        updateExpanded(false)
        keyboardController?.show()
    }

    LaunchedEffect(collapseRequestKey) {
        if (collapseRequestKey != lastCollapseRequestKey && !alwaysExpanded && expanded) {
            lastCollapseRequestKey = collapseRequestKey
            updateExpanded(false)
            keyboardController?.show()
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
            val listModifier =
                if (displayAsExpanded) {
                    // Min height lives inside the card so its animateContentSize animates the
                    // growth instead of the card snapping to the min height on the first frame.
                    contentModifier
                        .heightIn(min = SEARCH_HISTORY_EXPANDED_CARD_MIN_HEIGHT)
                        .searchHistoryTabSwipe(
                            enabled = canSwitchTabs,
                            selectedTab = activeSelectedTab,
                            onTabSelected = ::updateSelectedTab,
                        ).verticalScroll(scrollState)
                } else {
                    contentModifier
                }
            Column(modifier = listModifier) {
                if (displayAsExpanded) {
                    SearchHistoryTabs(
                        selectedTab = activeSelectedTab,
                        onTabSelected = ::updateSelectedTab,
                        showWallpaperBackground = showWallpaperBackground,
                    )
                    HorizontalDivider(
                        color = dividerColor(showWallpaperBackground, overlayDividerColor),
                    )
                }

                val showClearAllHistory =
                    displayAsExpanded &&
                        onClearRecentItems != null &&
                        displayItems.isNotEmpty()
                displayItems.forEachIndexed { index, item ->
                    val baseShowDivider = index < displayItems.lastIndex || showClearAllHistory
                    RecentSearchItemRow(
                        item = item,
                        textColor = textColor,
                        iconColor = iconColor,
                        callingApp = callingApp,
                        messagingApp = messagingApp,
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
                        showDivider = baseShowDivider,
                        showWallpaperBackground = showWallpaperBackground,
                        overlayDividerColor = overlayDividerColor,
                    )
                }

                if (showClearAllHistory) {
                    ClearAllHistoryRow(
                        showWallpaperBackground = showWallpaperBackground,
                        onClick = { showClearAllConfirmation = true },
                    )
                }

                if (!displayAsExpanded && canExpand) {
                    ExpandButton(
                        onClick = {
                            updateExpanded(true)
                            keyboardController?.hide()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textResId = R.string.action_more_search_history,
                    )
                }
            }
        }

        if (expanded && !alwaysExpanded && showInlineCollapseButton) {
            CollapseButton(
                showWallpaperBackground = showWallpaperBackground,
                onClick = {
                    updateExpanded(false)
                    keyboardController?.show()
                },
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

@Composable
private fun SearchHistoryTabs(
    selectedTab: SearchHistoryTab,
    onTabSelected: (SearchHistoryTab) -> Unit,
    showWallpaperBackground: Boolean,
) {
    val contentColor =
        if (showWallpaperBackground) {
            AppColors.WallpaperTextPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val containerColor =
        if (showWallpaperBackground) {
            Color.Transparent
        } else {
            AppColors.getSettingsCardContainerColor()
        }

    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal),
                width = Dp.Unspecified,
                height = 2.dp,
                color = contentColor.copy(alpha = 0.42f),
            )
        },
        divider = {
            HorizontalDivider(color = contentColor.copy(alpha = 0.10f))
        },
    ) {
        SearchHistoryTabItem(
            text = stringResource(R.string.search_history_tab_searches),
            selected = selectedTab == SearchHistoryTab.SEARCHES,
            contentColor = contentColor,
            onClick = { onTabSelected(SearchHistoryTab.SEARCHES) },
        )
        SearchHistoryTabItem(
            text = stringResource(R.string.search_history_tab_recently_opened),
            selected = selectedTab == SearchHistoryTab.RECENTLY_OPENED,
            contentColor = contentColor,
            onClick = { onTabSelected(SearchHistoryTab.RECENTLY_OPENED) },
        )
    }
}

@Composable
private fun SearchHistoryTabItem(
    text: String,
    selected: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(SEARCH_HISTORY_TAB_ROW_HEIGHT.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = if (selected) contentColor else contentColor.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_clear_all_history),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.82f),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ClearAllHistoryRow(
    showWallpaperBackground: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clearColor =
        if (showWallpaperBackground) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.78f)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = (QUERY_ROW_ICON_SIZE + QUERY_ROW_VERTICAL_PADDING * 2).dp)
                .clip(DesignTokens.CardShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).padding(
                    horizontal = QUERY_TEXT_END_PADDING.dp,
                    vertical = QUERY_ROW_VERTICAL_PADDING.dp,
                ),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = clearColor,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )

        Text(
            text = stringResource(R.string.action_clear_all_history),
            style = MaterialTheme.typography.bodyMedium,
            color = clearColor,
            modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
        )
    }
}

@Composable
private fun dividerColor(
    showWallpaperBackground: Boolean,
    overlayDividerColor: Color?,
): Color =
    overlayDividerColor
        ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant

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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(DesignTokens.CardShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress,
                ).padding(
                    start = QUERY_ICON_START_PADDING.dp,
                    end = QUERY_TEXT_END_PADDING.dp,
                    top = QUERY_ROW_VERTICAL_PADDING.dp,
                    bottom = QUERY_ROW_VERTICAL_PADDING.dp,
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
                imageVector = Icons.Rounded.Search,
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
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
