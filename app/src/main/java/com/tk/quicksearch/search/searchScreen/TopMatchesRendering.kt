package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultRow
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchPolicy
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.apps.AppItemDropdownMenu
import com.tk.quicksearch.search.apps.AppSearchInitials
import com.tk.quicksearch.search.apps.AppSearchPolicy
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.hasNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotKeys
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.calendar.CalendarEventRow
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SectionRenderContext
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.reminders.ReminderRow
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.notes.NoteRow
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemAction
import com.tk.quicksearch.search.other.OtherSearchItemActionHandler
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.ScreenTimeResultCard
import com.tk.quicksearch.search.core.ScreenTimeState
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

@Composable
internal fun TopMatchesSection(
    matches: List<TopMatchItem>,
    params: SectionRenderParams,
    showWallpaperBackground: Boolean,
    showTopResultIndicator: Boolean,
    showHeader: Boolean = true,
    selectedMatchIndex: Int? = null,
    reverseOrder: Boolean = false,
    screenTimeState: ScreenTimeState,
    pinnedNonAppItemOrder: List<String>,
    iconPackPackage: String?,
    onOtherSearchItemAction: OtherSearchItemActionHandler,
    modifier: Modifier = Modifier,
) {
    val highlightedMatch = selectedMatchIndex?.let(matches::getOrNull) ?: matches.firstOrNull()
    val displayedMatches = if (reverseOrder) matches.asReversed() else matches

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        if (showHeader) {
            TopMatchesHeader()
        }

        displayedMatches.forEach { item ->
            key(item.stableKey()) {
                val isTopPredicted = showTopResultIndicator && item == highlightedMatch
                if (item is TopMatchItem.AppGrid) {
                    TopMatchAppGrid(
                        apps = item.apps,
                        params = params.appsParams,
                        isPredicted = isTopPredicted,
                    )
                } else if (item is TopMatchItem.Other) {
                    when (item.itemId) {
                        OtherSearchItemId.SCREEN_TIME ->
                            ScreenTimeResultCard(
                                state = screenTimeState,
                                isPinned =
                                    OtherSearchItemRegistry.isPinned(
                                        item.itemId,
                                        pinnedNonAppItemOrder,
                                    ),
                                showWallpaperBackground = showWallpaperBackground,
                                iconPackPackage = iconPackPackage,
                                onTogglePin = { onOtherSearchItemAction(item.itemId, OtherSearchItemAction.TOGGLE_PIN) },
                                onHide = { onOtherSearchItemAction(item.itemId, OtherSearchItemAction.HIDE) },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .predictedSubmitHighlight(
                                            isPredicted = isTopPredicted,
                                            shape = SearchResultCardDefaults.shape,
                                            opaqueCardTopResultBorder = true,
                                        ),
                            )
                    }
                } else {
                    SearchResultCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .predictedSubmitHighlight(
                                    isPredicted = isTopPredicted,
                                    shape = SearchResultCardDefaults.shape,
                                    opaqueCardTopResultBorder = true,
                                ),
                        showWallpaperBackground = showWallpaperBackground,
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal = DesignTokens.SpacingLarge,
                                    vertical = 4.dp,
                                ),
                        ) {
                            TopMatchRow(
                                item = item,
                                params = params,
                                isPredicted = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopMatchesHeader() {
    Row(
        modifier =
            Modifier.padding(
                start = DesignTokens.SpacingMedium,
                top = DesignTokens.SpacingXSmall,
                end = DesignTokens.SpacingLarge,
                bottom = DesignTokens.SpacingXSmall,
            ),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.top_matches_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal fun openTopMatch(
    item: TopMatchItem,
    params: SectionRenderParams,
): Boolean? {
    when (item) {
        is TopMatchItem.App -> params.appsParams?.onAppClick?.invoke(item.app) ?: return null
        is TopMatchItem.AppGrid -> {
            val app = item.apps.firstOrNull() ?: return null
            val onAppClick = params.appsParams?.onAppClick ?: return null
            onAppClick(app)
        }
        is TopMatchItem.AppShortcut ->
            params.appShortcutsParams?.onShortcutClick?.invoke(item.shortcut) ?: return null
        is TopMatchItem.Contact -> {
            if (item.contact.hasContactMethods) {
                params.contactsParams.onShowContactMethods(item.contact)
            } else {
                params.contactsParams.onContactClick(item.contact)
            }
        }
        is TopMatchItem.File -> params.filesParams.onFileClick(item.file)
        is TopMatchItem.Setting -> params.settingsParams?.onSettingClick?.invoke(item.setting) ?: return null
        is TopMatchItem.AppSetting -> {
            val settingsParams = params.settingsParams ?: return null
            if (item.setting.isToggleAction) {
                val currentValue = settingsParams.isAppSettingToggleChecked(item.setting)
                settingsParams.onAppSettingToggle(item.setting, !currentValue)
                return true
            } else {
                settingsParams.onAppSettingClick(item.setting)
            }
        }
        is TopMatchItem.Calendar ->
            params.calendarParams?.onEventClick?.invoke(item.event) ?: return null
        is TopMatchItem.Reminder ->
            params.remindersParams?.onReminderClick?.invoke(item.reminder) ?: return null
        is TopMatchItem.Note -> params.notesParams?.onNoteClick?.invoke(item.note) ?: return null
        is TopMatchItem.Other -> return true
    }
    return false
}

@Composable
private fun TopMatchRow(
    item: TopMatchItem,
    params: SectionRenderParams,
    isPredicted: Boolean,
) {
    when (item) {
        is TopMatchItem.App -> TopMatchAppRow(
            app = item.app,
            params = params.appsParams,
            isPredicted = isPredicted,
        )

        is TopMatchItem.AppGrid -> Unit

        is TopMatchItem.AppShortcut -> params.appShortcutsParams?.let { appShortcutsParams ->
            val id = shortcutKey(item.shortcut)
            AppShortcutRow(
                shortcut = item.shortcut,
                isPinned = appShortcutsParams.pinnedShortcutIds.contains(id),
                isExcluded = appShortcutsParams.excludedShortcutIds.contains(id),
                hasNickname = !appShortcutsParams.getShortcutNickname(id).isNullOrBlank(),
                hasTrigger = appShortcutsParams.getShortcutTrigger(id)?.word?.isNotBlank() == true,
                onShortcutClick = appShortcutsParams.onShortcutClick,
                onTogglePin = appShortcutsParams.onTogglePin,
                onDisable = appShortcutsParams.onDisable,
                onDisableAllForApp = appShortcutsParams.onDisableAllForApp,
                onAppInfoClick = appShortcutsParams.onAppInfoClick,
                onNicknameClick = appShortcutsParams.onNicknameClick,
                onTriggerClick = appShortcutsParams.onTriggerClick,
                onEditCustomShortcut = appShortcutsParams.onEditCustomShortcut,
                onEditShortcutIcon = appShortcutsParams.onEditShortcutIcon,
                iconPackPackage = appShortcutsParams.iconPackPackage,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.Contact -> ContactResultRow(
            contactInfo = item.contact,
            callingApp = params.contactsParams.callingApp ?: CallingApp.CALL,
            messagingApp = params.contactsParams.messagingApp ?: MessagingApp.MESSAGES,
            primaryAction = params.contactsParams.getPrimaryContactCardAction(item.contact.contactId),
            secondaryAction = params.contactsParams.getSecondaryContactCardAction(item.contact.contactId),
            onContactClick = params.contactsParams.onContactClick,
            onShowContactMethods = params.contactsParams.onShowContactMethods,
            onCallContact = params.contactsParams.onCallContact,
            onSmsContact = params.contactsParams.onSmsContact,
            onContactMethodClick = { method -> params.contactsParams.onContactMethodClick(item.contact, method) },
            isPinned = params.contactsParams.pinnedContactIds.contains(item.contact.contactId),
            onTogglePin = params.contactsParams.onTogglePin,
            onExclude = params.contactsParams.onExclude,
            onNicknameClick = params.contactsParams.onNicknameClick,
            hasNickname = !params.contactsParams.getContactNickname(item.contact.contactId).isNullOrBlank(),
            onTriggerClick = params.contactsParams.onTriggerClick,
            hasTrigger = params.contactsParams.getContactTrigger(item.contact.contactId)?.word?.isNotBlank() == true,
            onPrimaryActionLongPress = params.contactsParams.onPrimaryActionLongPress,
            onSecondaryActionLongPress = params.contactsParams.onSecondaryActionLongPress,
            onCustomAction = params.contactsParams.onCustomAction,
            isPredicted = isPredicted,
        )

        is TopMatchItem.File -> FileResultRow(
            deviceFile = item.file,
            onClick = params.filesParams.onFileClick,
            onOpenFolder = params.filesParams.onOpenFolder,
            isPinned = params.filesParams.pinnedFileUris.contains(item.file.uri.toString()),
            onTogglePin = params.filesParams.onTogglePin,
            onExclude = params.filesParams.onExclude,
            onExcludeExtension = params.filesParams.onExcludeExtension,
            onNicknameClick = params.filesParams.onNicknameClick,
            hasNickname = !params.filesParams.getFileNickname(item.file.uri.toString()).isNullOrBlank(),
            onTriggerClick = params.filesParams.onTriggerClick,
            hasTrigger = params.filesParams.getFileTrigger(item.file.uri.toString())?.word?.isNotBlank() == true,
            isPredicted = isPredicted,
        )

        is TopMatchItem.Setting -> params.settingsParams?.let { settingsParams ->
            SettingResultRow(
                shortcut = item.setting,
                isPinned = settingsParams.pinnedSettingIds.contains(item.setting.id),
                onClick = settingsParams.onSettingClick,
                onTogglePin = settingsParams.onTogglePin,
                onExclude = settingsParams.onExclude,
                onNicknameClick = settingsParams.onNicknameClick,
                hasNickname = !settingsParams.getSettingNickname(item.setting.id).isNullOrBlank(),
                onTriggerClick = settingsParams.onTriggerClick,
                hasTrigger = settingsParams.getSettingTrigger(item.setting.id)?.word?.isNotBlank() == true,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.AppSetting -> params.settingsParams?.let { settingsParams ->
            AppSettingResultRow(
                setting = item.setting,
                checked = settingsParams.isAppSettingToggleChecked(item.setting),
                onToggle = settingsParams.onAppSettingToggle,
                onWebSuggestionsCountChange = settingsParams.onAppSettingWebSuggestionsCountChange,
                onClick = settingsParams.onAppSettingClick,
                webSuggestionsCount = settingsParams.appSettingWebSuggestionsCount,
                appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
                onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
                appSettingAppResultRowCount = settingsParams.appSettingAppResultRowCount,
                onAppSettingAppResultRowCountChange = settingsParams.onAppSettingAppResultRowCountChange,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.Calendar -> params.calendarParams?.let { calendarParams ->
            CalendarEventRow(
                event = item.event,
                isPinned = calendarParams.pinnedEventIds.contains(item.event.eventId),
                isExcluded = calendarParams.excludedEventIds.contains(item.event.eventId),
                hasNickname = !calendarParams.getEventNickname(item.event.eventId).isNullOrBlank(),
                onClick = calendarParams.onEventClick,
                onTogglePin = calendarParams.onTogglePin,
                onExclude = calendarParams.onExclude,
                onInclude = calendarParams.onInclude,
                onNicknameClick = calendarParams.onNicknameClick,
                isPredicted = isPredicted,
                onArchive = calendarParams.onArchiveTodayEvent,
            )
        }

        is TopMatchItem.Reminder -> params.remindersParams?.let { remindersParams ->
            ReminderRow(
                reminder = item.reminder,
                isPinned = remindersParams.pinnedReminderIds.contains(item.reminder.reminderId),
                onClick = remindersParams.onReminderClick,
                onTogglePin = remindersParams.onTogglePin,
                onMarkDone = remindersParams.onMarkDone,
                onDelete = remindersParams.onDelete,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.Note -> params.notesParams?.let { notesParams ->
            NoteRow(
                note = item.note,
                isPinned = notesParams.pinnedNoteIds.contains(item.note.noteId),
                onClick = notesParams.onNoteClick,
                onTogglePin = notesParams.onTogglePin,
                onDelete = notesParams.onDelete,
                onTriggerClick = notesParams.onTriggerClick,
                hasTrigger = notesParams.getNoteTrigger(item.note.noteId)?.word?.isNotBlank() == true,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.Other -> Unit
    }
}

@Composable
private fun TopMatchAppGrid(
    apps: List<AppInfo>,
    params: AppsSectionParams?,
    isPredicted: Boolean,
) {
    if (params == null || apps.isEmpty()) return
    AppGridView(
        apps = apps,
        allApps = apps,
        pinnedAndRecentApps = emptyList(),
        pinnedApps = emptyList(),
        newOrUpdatedApps = emptyList(),
        mostUsedApps = emptyList(),
        appShortcuts = params.appShortcuts,
        isSearching = true,
        hasUsagePermission = params.hasUsagePermission,
        selectedSuggestionTab = params.selectedSuggestionTab,
        enabledSuggestionTabs = params.enabledSuggestionTabs,
        onSuggestionTabSelected = params.onSuggestionTabSelected,
        hasAppResults = true,
        showAllAppsButton = false,
        onAppClick = params.onAppClick,
        onAppShortcutClick = params.onAppShortcutClick,
        onAppInfoClick = params.onAppInfoClick,
        onUninstallClick = params.onUninstallClick,
        onHideApp = params.onHideApp,
        onDisableAppShortcut = params.onDisableAppShortcut,
        onPinApp = params.onPinApp,
        onUnpinApp = params.onUnpinApp,
        onReorderPinnedApps = params.onReorderPinnedApps,
        onNicknameClick = params.onNicknameClick,
        onTriggerClick = params.onTriggerClick,
        onOpenInSplitScreen = params.onOpenInSplitScreen,
        getAppNickname = params.getAppNickname,
        getAppTrigger = params.getAppTrigger,
        pinnedPackageNames = params.pinnedPackageNames,
        disabledShortcutIds = params.disabledAppShortcutIds,
        rowCount = params.rowCount,
        phoneColumnOverride = params.phoneColumnOverride,
        appIconSizeStep = params.appIconSizeStep,
        iconPackPackage = params.iconPackPackage,
        appIconShape = params.appIconShape,
        themedIconsEnabled = params.themedIconsEnabled,
        showAppLabels = params.showAppLabels,
        oneHandedMode = params.oneHandedMode,
        isInitializing = false,
        startupPhase = params.startupPhase,
        isOverlayPresentation = params.isOverlayPresentation,
        predictedTarget = if (isPredicted) params.predictedTarget else null,
        suppressTopResultIndicator = !isPredicted,
        showWallpaperBackground = params.showWallpaperBackground,
        notificationDotsEnabled = params.notificationDotsEnabled,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopMatchAppRow(
    app: AppInfo,
    params: AppsSectionParams?,
    isPredicted: Boolean,
) {
    if (params == null) return
    val notificationDotKeys = rememberNotificationDotKeys(params.notificationDotsEnabled)
    val view = LocalView.current
    val context = LocalContext.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
    var showOptions by remember { mutableStateOf(false) }
    val shortcuts =
        remember(params.appShortcuts, params.disabledAppShortcutIds, app.packageName) {
            params.appShortcuts.filter { shortcut ->
                shortcut.packageName == app.packageName &&
                    !isShortcutDisabled(shortcut, params.disabledAppShortcutIds)
            }
        }
    val iconResult =
        rememberAppIcon(
            packageName = app.packageName,
            iconPackPackage = params.iconPackPackage,
            userHandleId = app.userHandleId,
            forceCircularMask = params.appIconShape == com.tk.quicksearch.search.core.AppIconShape.CIRCLE,
        )

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = APP_ROW_MIN_HEIGHT.dp)
                    .topPredictedRowContainer(isTopPredicted = isPredicted)
                    .combinedClickable(
                        onClick = {
                            if (!showOptions) {
                                hapticConfirm(view)()
                                params.onAppClick(app)
                            }
                        },
                        onLongClick = if (app.isArchived) null else ({ showOptions = true }),
                    )
                    .topPredictedRowContentPadding()
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.padding(start = DesignTokens.SpacingXSmall).size(APP_ICON_SIZE.dp),
                contentAlignment = Alignment.Center,
            ) {
                iconResult.bitmap?.let { icon ->
                    androidx.compose.foundation.Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(APP_ICON_SIZE.dp),
                    )
                } ?: Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                AppNotificationDot(visible = app.hasNotificationDot(notificationDotKeys))
            }

            Text(
                text = rememberQueryHighlightedText(app.appName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        AppItemDropdownMenu(
            expanded = showOptions,
            onDismiss = { showOptions = false },
            isPinned = params.pinnedPackageNames.contains(app.launchCountKey()),
            showUninstall =
                !app.isSystemApp &&
                    app.userHandleId == null &&
                    app.packageName != context.packageName,
            hasNickname = !params.getAppNickname(app.packageName).isNullOrBlank(),
            hasTrigger = params.getAppTrigger(app.packageName)?.word?.isNotBlank() == true,
            shortcuts = shortcuts,
            appInfo = app,
            iconPackPackage = params.iconPackPackage,
            appIconShape = params.appIconShape,
            onShortcutClick = params.onAppShortcutClick,
            onAppInfoClick = { params.onAppInfoClick(app) },
            onHideApp = { params.onHideApp(app) },
            onDisableShortcut = params.onDisableAppShortcut,
            onPinApp = { params.onPinApp(app) },
            onUnpinApp = { params.onUnpinApp(app) },
            onUninstallClick = { params.onUninstallClick(app) },
            onNicknameClick = { params.onNicknameClick(app) },
            onTriggerClick = { params.onTriggerClick(app) },
            onAddToHome = { addToHomeHandler.addAppToHome(app) },
            onOpenInSplitScreen = { params.onOpenInSplitScreen(app) },
            showSwipeGestures = false,
        )
    }
}
