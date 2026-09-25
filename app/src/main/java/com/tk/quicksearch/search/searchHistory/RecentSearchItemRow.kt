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
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.search.data.appShortcutRepository.SearchTargetShortcutMode
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

@Composable
internal fun RecentSearchItemRow(
    item: RecentSearchItem,
    textColor: Color,
    iconColor: Color,
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
    onNoteClick: (NoteInfo) -> Unit,
    onAppSettingClick: (AppSettingResult) -> Unit,
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    appSettingPhoneAppGridColumns: Int,
    onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit,
    appSettingAppResultRowCount: Int,
    onAppSettingAppResultRowCountChange: (Int) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    showDivider: Boolean,
    showWallpaperBackground: Boolean,
    overlayDividerColor: Color?,
) {
    var showRemoveMenu by remember { mutableStateOf(false) }
    val dividerColor =
        overlayDividerColor
            ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant

    Box(modifier = Modifier.fillMaxWidth()) {
        when (item) {
            is RecentSearchItem.Query -> {
                RecentQueryRow(
                    query = item.value,
                    showAiSearchIcon = item.entry.hasAiSnapshot,
                    textColor = textColor,
                    iconColor = iconColor,
                    onClick = { onRecentQueryClick(item.entry) },
                    onLongPress = { showRemoveMenu = true },
                )
            }

            is RecentSearchItem.Contact -> {
                Box(modifier = Modifier.padding(contactRowPadding())) {
                    ContactResultRow(
                        contactInfo = item.contact,
                        callingApp =
                            ContactCallingAppResolver.resolveCallingAppForContact(
                                item.contact,
                                callingApp,
                            ),
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
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is RecentSearchItem.AppShortcut -> {
                val historySubtext = resolveAppShortcutHistorySubtext(item.shortcut)
                Box(modifier = Modifier.padding(appShortcutRowPadding())) {
                    AppShortcutRow(
                        shortcut = item.shortcut,
                        isPinned = false,
                        isExcluded = false,
                        hasNickname = false,
                        onShortcutClick = onAppShortcutClick,
                        onTogglePin = {},
                        onDisable = {},
                        onDisableAllForApp = {},
                        onAppInfoClick = {},
                        onNicknameClick = {},
                        onEditCustomShortcut = {},
                        onEditShortcutIcon = {},
                        iconPackPackage = null,
                        showAppLabel = !historySubtext.isNullOrBlank(),
                        subtitleText = historySubtext,
                        enableLongPress = false,
                        onLongPressOverride = { showRemoveMenu = true },
                        iconTint = iconColor,
                    )
                }
            }

            is RecentSearchItem.AppSetting -> {
                Box(modifier = Modifier.padding(settingsRowPadding())) {
                    AppSettingResultRow(
                        setting = item.setting,
                        checked = isAppSettingToggleChecked(item.setting),
                        onToggle = onAppSettingToggle,
                        onWebSuggestionsCountChange = {},
                        onClick = onAppSettingClick,
                        webSuggestionsCount = 0,
                        appSettingPhoneAppGridColumns = appSettingPhoneAppGridColumns,
                        onAppSettingPhoneAppGridColumnsChange = onAppSettingPhoneAppGridColumnsChange,
                        appSettingAppResultRowCount = appSettingAppResultRowCount,
                        onAppSettingAppResultRowCountChange = onAppSettingAppResultRowCountChange,
                        isPredicted = false,
                    )
                }
            }

            is RecentSearchItem.Note -> {
                RecentNoteRow(
                    note = item.note,
                    textColor = textColor,
                    iconColor = iconColor,
                    onClick = { onNoteClick(item.note) },
                    onLongPress = { showRemoveMenu = true },
                )
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
        is RecentSearchItem.AppSetting -> SETTINGS_HORIZONTAL_PADDING.dp
        else -> DesignTokens.SpacingMedium
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentNoteRow(
    note: NoteInfo,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val title = note.title.ifBlank { stringResource(R.string.notes_untitled) }
    val preview = NotesTextUtils.firstLinesPreview(note.markdownContent)
    val subtitle =
        if (preview.isNotBlank()) {
            preview
        } else {
            stringResource(R.string.notes_empty_note_subtext)
        }

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
                    horizontal = DesignTokens.SpacingMedium,
                    vertical = QUERY_ROW_VERTICAL_PADDING.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = iconColor,
            modifier =
                Modifier
                    .size(QUERY_ROW_ICON_SIZE.dp)
                    .padding(
                        start = DesignTokens.SpacingXSmall,
                        end = QUERY_TEXT_START_PADDING.dp,
                    ),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = iconColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun resolveAppShortcutHistorySubtext(shortcut: StaticShortcut): String? {
    val deepLink =
        shortcut.intents
            .asSequence()
            .mapNotNull { it.dataString?.trim() }
            .firstOrNull { it.isNotBlank() }
    if (shortcut.id.startsWith("custom_deeplink_") && !deepLink.isNullOrBlank()) {
        return deepLink
    }

    val searchIntent =
        shortcut.intents.firstOrNull {
            it.action == SearchTargetQueryShortcutActivity.ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT
        } ?: return null
    val query =
        searchIntent
            .getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_QUERY)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
    val targetType =
        searchIntent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_TARGET_TYPE).orEmpty()
    if (targetType != SearchTargetQueryShortcutActivity.TARGET_TYPE_BROWSER) {
        return null
    }
    val browserShortcutMode =
        searchIntent
            .getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_BROWSER_SHORTCUT_MODE)
            ?.let { runCatching { SearchTargetShortcutMode.valueOf(it) }.getOrNull() }
            ?: SearchTargetShortcutMode.AUTO
    return if (browserShortcutMode == SearchTargetShortcutMode.FORCE_URL) {
        query
    } else {
        null
    }
}
