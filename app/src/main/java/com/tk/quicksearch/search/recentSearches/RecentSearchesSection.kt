package com.tk.quicksearch.search.recentSearches

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
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

private const val QUERY_ICON_SIZE = 42
private const val QUERY_ICON_START_PADDING = 16
private const val QUERY_TEXT_START_PADDING = 12
private const val QUERY_TEXT_END_PADDING = 16
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
        showWallpaperBackground: Boolean = false
) {
    if (items.isEmpty()) return

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
            elevation = AppColors.getCardElevation(showWallpaperBackground)
    ) {
        Column {
            items.forEachIndexed { index, item ->
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
                        showDivider = index < items.lastIndex,
                        showWallpaperBackground = showWallpaperBackground
                )
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
        showDivider: Boolean,
        showWallpaperBackground: Boolean
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
            is RecentSearchItem.Query ->
                    RecentQueryRow(
                            query = item.value,
                            textColor = textColor,
                            iconColor = iconColor,
                            onClick = { onRecentQueryClick(item.value) },
                            onLongPress = { showRemoveMenu = true }
                    )
            is RecentSearchItem.Contact ->
                    Box(modifier = Modifier.padding(contactRowPadding())) {
                        ContactResultRow(
                                contactInfo = item.contact,
                                messagingApp =
                                        ContactMessagingAppResolver.resolveMessagingAppForContact(
                                                item.contact,
                                                messagingApp
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
                                icon = Icons.Rounded.History,
                                iconTint = iconColor
                        )
                    }
            is RecentSearchItem.File ->
                    Box(modifier = Modifier.padding(fileRowPadding())) {
                        FileResultRow(
                                deviceFile = item.file,
                                onClick = onFileClick,
                                enableLongPress = false,
                                onLongPressOverride = { showRemoveMenu = true },
                                icon = Icons.Rounded.History,
                                iconTint = iconColor
                        )
                    }
            is RecentSearchItem.Setting ->
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
                                icon = Icons.Rounded.History,
                                iconTint = iconColor
                        )
                    }
            is RecentSearchItem.AppShortcut ->
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
                                icon = Icons.Rounded.History,
                                iconTint = iconColor
                        )
                    }
        }

        DropdownMenu(
                expanded = showRemoveMenu,
                onDismissRequest = { showRemoveMenu = false },
                shape = RoundedCornerShape(24.dp),
                properties = PopupProperties(focusable = false),
                containerColor = AppColors.DialogBackground
        ) {
            DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_remove)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                    },
                    onClick = {
                        showRemoveMenu = false
                        onDeleteRecentItem(item.entry)
                    }
            )
        }
    }

    if (showDivider) {
        HorizontalDivider(
                modifier = Modifier.padding(horizontal = dividerPadding(item)),
                color = dividerColor
        )
    }
}

private fun contactRowPadding(): PaddingValues =
        PaddingValues(
                horizontal = DesignTokens.SpacingMedium,
                vertical = DesignTokens.SpacingXSmall
        )

private fun fileRowPadding(): PaddingValues = PaddingValues(horizontal = DesignTokens.SpacingMedium)

private fun appShortcutRowPadding(): PaddingValues =
        PaddingValues(
                horizontal = DesignTokens.SpacingMedium,
                vertical = SHORTCUT_VERTICAL_PADDING.dp
        )

private fun settingsRowPadding(): PaddingValues =
        PaddingValues(
                horizontal = SETTINGS_HORIZONTAL_PADDING.dp,
                vertical = SETTINGS_VERTICAL_PADDING.dp
        )

private fun dividerPadding(item: RecentSearchItem) =
        when (item) {
            is RecentSearchItem.Query -> DesignTokens.SpacingLarge
            is RecentSearchItem.Setting -> SETTINGS_HORIZONTAL_PADDING.dp
            else -> DesignTokens.SpacingMedium
        }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentQueryRow(
        query: String,
        textColor: Color,
        iconColor: Color,
        onClick: () -> Unit,
        onLongPress: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(DesignTokens.CardShape)
                            .combinedClickable(
                                    onClick = onClick,
                                    onLongClick = onLongPress
                            )
                            .padding(
                                    start = QUERY_ICON_START_PADDING.dp,
                                    end = QUERY_TEXT_END_PADDING.dp,
                                    top = DesignTokens.SpacingSmall,
                                    bottom = DesignTokens.SpacingSmall
                            ),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = stringResource(R.string.desc_search_icon),
                tint = iconColor,
                modifier =
                        Modifier.size(QUERY_ICON_SIZE.dp)
                                .padding(
                                        start = DesignTokens.SpacingXSmall,
                                        end = QUERY_TEXT_START_PADDING.dp
                                )
        )

        Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
        )
    }
}
