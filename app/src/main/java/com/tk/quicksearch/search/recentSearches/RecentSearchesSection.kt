package com.tk.quicksearch.search.recentSearches

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
import com.tk.quicksearch.search.contacts.CollapseButton
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens

private val EXPANDED_HISTORY_MAX_HEIGHT = 420.dp
private val EXPANDED_HISTORY_MAX_HEIGHT_OVERLAY = 300.dp

private const val QUERY_ICON_SIZE = 42
private const val QUERY_ROW_ICON_SIZE = 40
private const val QUERY_ICON_START_PADDING = 16
private const val QUERY_TEXT_START_PADDING = 12
private const val QUERY_TEXT_END_PADDING = 16
private const val QUERY_ROW_VERTICAL_PADDING = 10
private const val SETTINGS_HORIZONTAL_PADDING = 16
private const val SETTINGS_VERTICAL_PADDING = 4
private const val SHORTCUT_VERTICAL_PADDING = 4

@Composable
fun RecentSearchesSection(
    modifier: Modifier = Modifier,
    items: List<RecentSearchItem>,
    messagingApp: MessagingApp,
    onRecentQueryClick: (String) -> Unit,
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
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    onDisableSearchHistory: () -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    showWallpaperBackground: Boolean = false,
    isOverlayPresentation: Boolean = false,
) {
    if (items.isEmpty()) return
    var isExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val displayItems = if (isExpanded) items else items.take(1)
    val canExpand = items.size > 1
    val expandedHistoryMaxHeight =
        if (isOverlayPresentation) {
            EXPANDED_HISTORY_MAX_HEIGHT_OVERLAY
        } else {
            EXPANDED_HISTORY_MAX_HEIGHT
        }

    BackHandler(enabled = isExpanded) {
        isExpanded = false
        onExpandedChange(false)
        keyboardController?.show()
    }

    val textColor =
        if (showWallpaperBackground) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val iconColor =
        if (showWallpaperBackground) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = AppColors.getCardColors(showWallpaperBackground),
        elevation = AppColors.getCardElevation(showWallpaperBackground),
    ) {
        if (isExpanded) {
            Column {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = expandedHistoryMaxHeight)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                ) {
                    displayItems.forEachIndexed { index, item ->
                        RecentSearchItemRow(
                            item = item,
                            textColor = textColor,
                            iconColor = iconColor,
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
                            onDeleteRecentItem = onDeleteRecentItem,
                            onDisableSearchHistory = onDisableSearchHistory,
                            showDivider = true,
                            showWallpaperBackground = showWallpaperBackground,
                        )
                    }
                    val disableDividerColor =
                        if (showWallpaperBackground) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingMedium),
                        color = disableDividerColor,
                    )
                    DisableSearchHistoryRow(
                        textColor = textColor,
                        iconColor = iconColor,
                        onClick = onDisableSearchHistory,
                    )
                }
                CollapseButton(
                    onClick = {
                        isExpanded = false
                        onExpandedChange(false)
                        keyboardController?.show()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = DesignTokens.SpacingXXLarge),
                )
            }
        } else {
            Column {
                displayItems.forEachIndexed { index, item ->
                    RecentSearchItemRow(
                        item = item,
                        textColor = textColor,
                        iconColor = iconColor,
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
                        onDeleteRecentItem = onDeleteRecentItem,
                        onDisableSearchHistory = onDisableSearchHistory,
                        showDivider = index < displayItems.lastIndex,
                        showWallpaperBackground = showWallpaperBackground,
                    )
                }

                if (canExpand) {
                    TextButton(
                        onClick = {
                            isExpanded = true
                            onExpandedChange(true)
                            keyboardController?.hide()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(R.string.action_more_search_history))
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = stringResource(R.string.desc_expand),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentSearchItemRow(
    item: RecentSearchItem,
    textColor: Color,
    iconColor: Color,
    messagingApp: MessagingApp,
    onRecentQueryClick: (String) -> Unit,
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
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    onDisableSearchHistory: () -> Unit = {},
    showDivider: Boolean,
    showWallpaperBackground: Boolean,
) {
    var showRemoveMenu by remember { mutableStateOf(false) }
    val dividerColor =
        if (showWallpaperBackground) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        when (item) {
            is RecentSearchItem.Query -> {
                RecentQueryRow(
                    query = item.value,
                    textColor = textColor,
                    iconColor = iconColor,
                    onClick = { onRecentQueryClick(item.value) },
                    onLongPress = { showRemoveMenu = true },
                )
            }

            is RecentSearchItem.Contact -> {
                Box(modifier = Modifier.padding(contactRowPadding())) {
                    ContactResultRow(
                        contactInfo = item.contact,
                        messagingApp =
                            ContactMessagingAppResolver.resolveMessagingAppForContact(
                                item.contact,
                                messagingApp,
                            ),
                        primaryAction = getPrimaryContactCardAction(item.contact.contactId),
                        secondaryAction =
                            getSecondaryContactCardAction(item.contact.contactId),
                        onContactClick = onContactClick,
                        onShowContactMethods = onShowContactMethods,
                        onCallContact = onCallContact,
                        onSmsContact = onSmsContact,
                        onPrimaryActionLongPress = onPrimaryActionLongPress,
                        onSecondaryActionLongPress = onSecondaryActionLongPress,
                        onCustomAction = onCustomAction,
                        onContactMethodClick = { method ->
                            onContactMethodClick(item.contact, method)
                        },
                        enableLongPress = false,
                        onLongPressOverride = { showRemoveMenu = true },
                        iconTint = iconColor,
                    )
                }
            }

            is RecentSearchItem.File -> {
                Box(modifier = Modifier.padding(fileRowPadding())) {
                    FileResultRow(
                        deviceFile = item.file,
                        onClick = onFileClick,
                        enableLongPress = false,
                        onLongPressOverride = { showRemoveMenu = true },
                        iconTint = iconColor,
                    )
                }
            }

            is RecentSearchItem.Setting -> {
                Box(modifier = Modifier.padding(settingsRowPadding())) {
                    SettingResultRow(
                        shortcut = item.setting,
                        isPinned = false,
                        onClick = onSettingClick,
                        onTogglePin = {},
                        onExclude = {},
                        onNicknameClick = {},
                        hasNickname = false,
                        showDescription = false,
                        enableLongPress = false,
                        onLongPressOverride = { showRemoveMenu = true },
                        iconTint = iconColor,
                    )
                }
            }

            is RecentSearchItem.AppShortcut -> {
                Box(modifier = Modifier.padding(appShortcutRowPadding())) {
                    AppShortcutRow(
                        shortcut = item.shortcut,
                        isPinned = false,
                        isExcluded = false,
                        hasNickname = false,
                        onShortcutClick = onAppShortcutClick,
                        onTogglePin = {},
                        onExclude = {},
                        onInclude = {},
                        onAppInfoClick = {},
                        onNicknameClick = {},
                        iconPackPackage = null,
                        showAppLabel = false,
                        enableLongPress = false,
                        onLongPressOverride = { showRemoveMenu = true },
                        iconTint = iconColor,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showRemoveMenu,
            onDismissRequest = { showRemoveMenu = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = AppColors.DialogBackground,
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_remove_from_history)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                },
                onClick = {
                    showRemoveMenu = false
                    onDeleteRecentItem(item.entry)
                },
            )
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dividerPadding(item)),
            color = dividerColor,
        )
    }
}

private fun contactRowPadding(): PaddingValues =
    PaddingValues(
        horizontal = DesignTokens.SpacingMedium,
        vertical = DesignTokens.SpacingXSmall,
    )

private fun fileRowPadding(): PaddingValues = PaddingValues(horizontal = DesignTokens.SpacingMedium)

private fun appShortcutRowPadding(): PaddingValues =
    PaddingValues(
        horizontal = DesignTokens.SpacingMedium,
        vertical = SHORTCUT_VERTICAL_PADDING.dp,
    )

private fun settingsRowPadding(): PaddingValues =
    PaddingValues(
        horizontal = SETTINGS_HORIZONTAL_PADDING.dp,
        vertical = SETTINGS_VERTICAL_PADDING.dp,
    )

private fun dividerPadding(item: RecentSearchItem) =
    when (item) {
        is RecentSearchItem.Query -> DesignTokens.SpacingLarge
        is RecentSearchItem.Setting -> SETTINGS_HORIZONTAL_PADDING.dp
        else -> DesignTokens.SpacingMedium
    }

@Composable
private fun DisableSearchHistoryRow(
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(DesignTokens.CardShape)
                .clickable(onClick = onClick)
                .padding(
                    start = QUERY_ICON_START_PADDING.dp,
                    end = QUERY_TEXT_END_PADDING.dp,
                    top = DesignTokens.SpacingSmall,
                    bottom = DesignTokens.SpacingSmall,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.VisibilityOff,
            contentDescription = null,
            tint = iconColor,
            modifier =
                Modifier
                    .size(QUERY_ICON_SIZE.dp)
                    .padding(
                        start = DesignTokens.SpacingXSmall,
                        end = QUERY_TEXT_START_PADDING.dp,
                    ),
        )
        Text(
            text = stringResource(R.string.action_disable_search_history),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentQueryRow(
    query: String,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(DesignTokens.CardShape)
                .combinedClickable(
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
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = stringResource(R.string.desc_search_icon),
            tint = iconColor,
            modifier =
                Modifier
                    .size(QUERY_ROW_ICON_SIZE.dp)
                    .padding(
                        start = DesignTokens.SpacingXSmall,
                        end = QUERY_TEXT_START_PADDING.dp,
                    ),
        )

        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}
