package com.tk.quicksearch.search.searchHistory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
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
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.CollapseButton
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private val EXPANDED_HISTORY_MAX_HEIGHT_OVERLAY = 300.dp

private const val QUERY_ROW_ICON_SIZE = 40
private const val QUERY_ICON_START_PADDING = 16
private const val QUERY_TEXT_START_PADDING = 12
private const val QUERY_TEXT_END_PADDING = 16
private const val QUERY_ROW_VERTICAL_PADDING = 10
private const val SETTINGS_HORIZONTAL_PADDING = 16
private const val SETTINGS_VERTICAL_PADDING = 4
private const val SHORTCUT_VERTICAL_PADDING = 4

@Composable
fun SearchHistorySection(
    modifier: Modifier = Modifier,
    items: List<RecentSearchItem>,
    callingApp: CallingApp,
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
    onAppSettingClick: (AppSettingResult) -> Unit = {},
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = { _, _ -> },
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean = { false },
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    showSearchHistoryTip: Boolean = false,
    onOpenSearchHistorySettings: () -> Unit = {},
    onDismissSearchHistoryTip: () -> Unit = {},
    onExpandedChange: (Boolean) -> Unit = {},
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    showWallpaperBackground: Boolean = false,
    isOverlayPresentation: Boolean = false,
    alwaysExpanded: Boolean = false,
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    if (items.isEmpty()) return
    var isExpanded by remember { mutableStateOf(alwaysExpanded) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val canExpand = !alwaysExpanded && items.size > 1
    val scrollState = rememberScrollState()
    val expandedHistoryMaxHeight =
        if (isOverlayPresentation) {
            minOf(expandedCardMaxHeight, EXPANDED_HISTORY_MAX_HEIGHT_OVERLAY)
        } else {
            expandedCardMaxHeight
        }

    BackHandler(enabled = isExpanded) {
        isExpanded = false
        onExpandedChange(false)
        keyboardController?.show()
    }

    val textColor =
        if (showWallpaperBackground) AppColors.WallpaperTextPrimary else MaterialTheme.colorScheme.onSurface

    val iconColor =
        if (showWallpaperBackground) AppColors.WallpaperTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        ExpandableResultsCard(
            resultCount = items.size,
            isExpanded = isExpanded,
            showAllResults = false,
            showExpandControls = canExpand,
            expandedCardMaxHeight = expandedHistoryMaxHeight,
            hasScrollableContent = scrollState.maxValue > 0,
            fillExpandedHeight = false,
            showWallpaperBackground = showWallpaperBackground,
            overlayCardColor = overlayCardColor,
        ) { contentModifier, cardState ->
            val displayItems = if (cardState.displayAsExpanded) items else items.take(1)
            if (cardState.displayAsExpanded) {
                Column(
                    modifier = contentModifier.verticalScroll(scrollState),
                ) {
                    displayItems.forEachIndexed { index, item ->
                        val showTipBelowFirstItem = showSearchHistoryTip && index == 0
                        val baseShowDivider = index < displayItems.lastIndex
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
                            onAppSettingClick = onAppSettingClick,
                            onAppSettingToggle = onAppSettingToggle,
                            isAppSettingToggleChecked = isAppSettingToggleChecked,
                            onDeleteRecentItem = onDeleteRecentItem,
                            showDivider = if (showTipBelowFirstItem) false else baseShowDivider,
                            showWallpaperBackground = showWallpaperBackground,
                            overlayDividerColor = overlayDividerColor,
                        )
                        if (showTipBelowFirstItem) {
                            InlineSearchHistoryTip(
                                onOpenSearchHistorySettings = onOpenSearchHistorySettings,
                                onDismiss = onDismissSearchHistoryTip,
                            )
                            if (baseShowDivider) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = dividerPadding(item)),
                                    color = dividerColor(showWallpaperBackground, overlayDividerColor),
                                )
                            }
                        }
                    }
                }
            } else {
                Column(modifier = contentModifier) {
                    displayItems.forEachIndexed { index, item ->
                        val showTipBelowFirstItem = showSearchHistoryTip && index == 0
                        val baseShowDivider = index < displayItems.lastIndex
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
                            onAppSettingClick = onAppSettingClick,
                            onAppSettingToggle = onAppSettingToggle,
                            isAppSettingToggleChecked = isAppSettingToggleChecked,
                            onDeleteRecentItem = onDeleteRecentItem,
                            showDivider = if (showTipBelowFirstItem) false else baseShowDivider,
                            showWallpaperBackground = showWallpaperBackground,
                            overlayDividerColor = overlayDividerColor,
                        )
                        if (showTipBelowFirstItem) {
                            InlineSearchHistoryTip(
                                onOpenSearchHistorySettings = onOpenSearchHistorySettings,
                                onDismiss = onDismissSearchHistoryTip,
                            )
                            if (baseShowDivider) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = dividerPadding(item)),
                                    color = dividerColor(showWallpaperBackground, overlayDividerColor),
                                )
                            }
                        }
                    }

                    if (canExpand) {
                        ExpandButton(
                            onClick = {
                                isExpanded = true
                                onExpandedChange(true)
                                keyboardController?.hide()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textResId = R.string.action_more_search_history,
                        )
                    }
                }
            }
        }

        if (isExpanded && !alwaysExpanded) {
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentSearchItemRow(
    item: RecentSearchItem,
    textColor: Color,
    iconColor: Color,
    callingApp: CallingApp,
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
    onAppSettingClick: (AppSettingResult) -> Unit,
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
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
                        onExclude = {},
                        onInclude = {},
                        onAppInfoClick = {},
                        onNicknameClick = {},
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
                        isPredicted = false,
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
        is RecentSearchItem.AppSetting -> SETTINGS_HORIZONTAL_PADDING.dp
        else -> DesignTokens.SpacingMedium
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

@Composable
private fun InlineSearchHistoryTip(
    onOpenSearchHistorySettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val linkTag = "search_history_settings"
    val tipMessage = stringResource(R.string.search_history_tip_message)
    val linkText = stringResource(R.string.search_history_tip_link)
    val fullText = "$tipMessage $linkText"
    val annotatedText =
        buildAnnotatedString {
            append(fullText)
            val startIndex = fullText.indexOf(linkText)
            if (startIndex >= 0) {
                val endIndex = startIndex + linkText.length
                addStyle(
                    style =
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    start = startIndex,
                    end = endIndex,
                )
                addStringAnnotation(
                    tag = linkTag,
                    annotation = linkText,
                    start = startIndex,
                    end = endIndex,
                )
            }
        }

    TipBanner(
        annotatedText = annotatedText,
        onTextClick = { offset ->
            val annotations =
                annotatedText.getStringAnnotations(
                    tag = linkTag,
                    start = offset,
                    end = offset,
                )
            if (annotations.isNotEmpty()) {
                onOpenSearchHistorySettings()
            }
        },
        onDismiss = onDismiss,
        modifier =
            Modifier.padding(
                start = QUERY_ICON_START_PADDING.dp,
                end = QUERY_TEXT_END_PADDING.dp,
                bottom = DesignTokens.SpacingSmall,
            ),
    )
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
private fun RecentQueryRow(
    query: String,
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
