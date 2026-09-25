package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.searchEngines.AliasParser
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.dialogs.TriggerDialogState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import java.util.Locale

/** Helper function to build all the section parameters needed by SearchScreenContent */
@Composable
internal fun buildSectionParams(
    state: SearchUiState,
    derivedState: DerivedState,
    isOverlayPresentation: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    onOpenFolder: (DeviceFile) -> Unit,
    onPinFile: (DeviceFile) -> Unit,
    onUnpinFile: (DeviceFile) -> Unit,
    onMovePinnedFile: (DeviceFile, Boolean) -> Unit,
    onExcludeFile: (DeviceFile) -> Unit,
    onExcludeFileExtension: (DeviceFile) -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onSettingClick: (DeviceSetting) -> Unit,
    onAppSettingClick: (AppSettingResult) -> Unit,
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    onAppSettingWebSuggestionsCountChange: (Int) -> Unit,
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    appSettingWebSuggestionsCount: Int,
    appSettingPhoneAppGridColumns: Int,
    onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit,
    appSettingAppResultRowCount: Int,
    onAppSettingAppResultRowCountChange: (Int) -> Unit,
    onPinSetting: (DeviceSetting) -> Unit,
    onUnpinSetting: (DeviceSetting) -> Unit,
    onMovePinnedSetting: (DeviceSetting, Boolean) -> Unit,
    onExcludeSetting: (DeviceSetting) -> Unit,
    onAppShortcutClick: (StaticShortcut) -> Unit,
    onPinAppShortcut: (StaticShortcut) -> Unit,
    onUnpinAppShortcut: (StaticShortcut) -> Unit,
    onMovePinnedAppShortcut: (StaticShortcut, Boolean) -> Unit,
    onDisableAppShortcut: (StaticShortcut) -> Unit,
    onDisableAllAppShortcutsForApp: (StaticShortcut) -> Unit,
    onAppShortcutAppInfoClick: (StaticShortcut) -> Unit,
    onEditCustomAppShortcut: (StaticShortcut) -> Unit,
    onEditAppShortcutIcon: (StaticShortcut) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
    onPinContact: (ContactInfo) -> Unit,
    onUnpinContact: (ContactInfo) -> Unit,
    onMovePinnedContact: (ContactInfo, Boolean) -> Unit,
    onExcludeContact: (ContactInfo) -> Unit,
    onCalendarEventClick: (CalendarEventInfo) -> Unit,
    onPinCalendarEvent: (CalendarEventInfo) -> Unit,
    onUnpinCalendarEvent: (CalendarEventInfo) -> Unit,
    onMovePinnedCalendarEvent: (CalendarEventInfo, Boolean) -> Unit,
    onExcludeCalendarEvent: (CalendarEventInfo) -> Unit,
    onIncludeCalendarEvent: (CalendarEventInfo) -> Unit,
    onArchiveTodayCalendarEvent: (CalendarEventInfo) -> Unit,
    onNoteClick: (NoteInfo) -> Unit,
    onPinNote: (NoteInfo) -> Unit,
    onUnpinNote: (NoteInfo) -> Unit,
    onMovePinnedNote: (NoteInfo, Boolean) -> Unit,
    onDeleteNote: (NoteInfo) -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    onContactActionHintDismissed: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onOpenInSplitScreen: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    onReorderPinnedApps: (List<AppInfo>) -> Unit,
    onReorderPinnedAppGrid: (List<String>, List<AppInfo>, List<StaticShortcut>) -> Unit,
    appFolderActions: com.tk.quicksearch.search.folders.AppGridFolderActions?,
    onSuggestionTabSelected: (AppSuggestionTabType) -> Unit,
    onRateQuickSearchClick: () -> Unit,
    onRateQuickSearchNotNowClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onUpdateNotNowClick: () -> Unit,
    getFileNickname: (String) -> String?,
    getContactNickname: (Long) -> String?,
    getSettingNickname: (String) -> String?,
    getAppNickname: (String) -> String?,
    getAppShortcutNickname: (String) -> String?,
    getCalendarEventNickname: (Long) -> String?,
    onUpdateNicknameDialogState: (NicknameDialogState?) -> Unit,
    onUpdateTriggerDialogState: (TriggerDialogState?) -> Unit,
    getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getContactActionTrigger: (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getAppShortcutTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getSettingTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getNoteTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    onUpdateExpandedSection: (ExpandedSection) -> Unit,
    expandedSection: ExpandedSection,
    reminderActions: ReminderSectionActions = ReminderSectionActions(),
) = remember(
    state,
    reminderActions,
    derivedState,
    expandedSection,
    onFileClick,
    onOpenFolder,
    onPinFile,
    onUnpinFile,
    onMovePinnedFile,
    onExcludeFile,
    onExcludeFileExtension,
    onOpenStorageAccessSettings,
    onSettingClick,
    onAppSettingClick,
    onAppSettingToggle,
    onAppSettingWebSuggestionsCountChange,
    isAppSettingToggleChecked,
    appSettingWebSuggestionsCount,
    appSettingPhoneAppGridColumns,
    onAppSettingPhoneAppGridColumnsChange,
    onPinSetting,
    onUnpinSetting,
    onMovePinnedSetting,
    onExcludeSetting,
    onAppShortcutClick,
    onPinAppShortcut,
    onUnpinAppShortcut,
    onMovePinnedAppShortcut,
    onDisableAppShortcut,
    onDisableAllAppShortcutsForApp,
    onAppShortcutAppInfoClick,
    onEditCustomAppShortcut,
    onEditAppShortcutIcon,
    onContactClick,
    onShowContactMethods,
    onCallContact,
    onSmsContact,
    onContactMethodClick,
    onPinContact,
    onUnpinContact,
    onMovePinnedContact,
    onExcludeContact,
    onCalendarEventClick,
    onPinCalendarEvent,
    onUnpinCalendarEvent,
    onMovePinnedCalendarEvent,
    onExcludeCalendarEvent,
    onIncludeCalendarEvent,
    onArchiveTodayCalendarEvent,
    onOpenCalendarPermissionSettings,
    onMovePinnedNote,
    onOpenAppSettings,
    getPrimaryContactCardAction,
    getSecondaryContactCardAction,
    onPrimaryActionLongPress,
    onSecondaryActionLongPress,
    onCustomAction,
    onContactActionHintDismissed,
    onAppClick,
    onOpenInSplitScreen,
    onAppInfoClick,
    onUninstallClick,
    onHideApp,
    onPinApp,
    onUnpinApp,
    onSuggestionTabSelected,
    getFileNickname,
    getContactNickname,
    getSettingNickname,
    getAppNickname,
    getAppShortcutNickname,
    getCalendarEventNickname,
    onUpdateNicknameDialogState,
    onUpdateTriggerDialogState,
    getAppTrigger,
    getContactTrigger,
    getContactActionTrigger,
    getFileTrigger,
    getAppShortcutTrigger,
    getSettingTrigger,
    getNoteTrigger,
    onUpdateExpandedSection,
) {
    val filesParams =
        FilesSectionParams(
            files = state.fileResults,
            hasPermission = state.hasFilePermission,
            isExpanded = expandedSection == ExpandedSection.FILES,
            pinnedFileUris = derivedState.pinnedFileUris,
            onFileClick = onFileClick,
            onOpenFolder = onOpenFolder,
            onRequestPermission = onOpenStorageAccessSettings,
            onTogglePin = { file ->
                if (derivedState.pinnedFileUris.contains(
                        file.uri.toString(),
                    )
                ) {
                    onUnpinFile(file)
                } else {
                    onPinFile(file)
                }
            },
            onMovePinned = onMovePinnedFile,
            onExclude = onExcludeFile,
            onExcludeExtension = onExcludeFileExtension,
            onNicknameClick = { file ->
                onUpdateNicknameDialogState(
                    NicknameDialogState.File(
                        file = file,
                        currentNickname =
                            getFileNickname(
                                file.uri.toString(),
                            ),
                        itemName = file.displayName,
                    ),
                )
            },
            onTriggerClick = { file ->
                onUpdateTriggerDialogState(
                    TriggerDialogState.File(
                        file = file,
                        currentTrigger = getFileTrigger(file.uri.toString()),
                        itemName = file.displayName,
                    ),
                )
            },
            getFileNickname = getFileNickname,
            getFileTrigger = getFileTrigger,
            showAllResults = false,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.FILES) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.FILES
                    },
                )
            },
            permissionDisabledCard = {
                title,
                message,
                actionLabel,
                onActionClick,
                ->
                PermissionDisabledCard(
                    title = title,
                    message = message,
                    actionLabel = actionLabel,
                    onActionClick = onActionClick,
                )
            },
            showWallpaperBackground = state.showWallpaperBackground,
        )

    val appShortcutParams =
        AppShortcutsSectionParams(
            shortcuts = state.appShortcutResults,
            isExpanded = expandedSection == ExpandedSection.APP_SHORTCUTS,
            pinnedShortcutIds = derivedState.pinnedAppShortcutIds,
            excludedShortcutIds =
                state.excludedAppShortcuts.map { shortcutKey(it) }.toSet(),
            onShortcutClick = onAppShortcutClick,
            onTogglePin = { shortcut ->
                if (derivedState.pinnedAppShortcutIds.contains(
                        shortcutKey(shortcut),
                    )
                ) {
                    onUnpinAppShortcut(shortcut)
                } else {
                    onPinAppShortcut(shortcut)
                }
            },
            onMovePinned = onMovePinnedAppShortcut,
            onDisable = onDisableAppShortcut,
            onDisableAllForApp = onDisableAllAppShortcutsForApp,
            onAppInfoClick = onAppShortcutAppInfoClick,
            onNicknameClick = { shortcut ->
                onUpdateNicknameDialogState(
                    NicknameDialogState.AppShortcut(
                        shortcut = shortcut,
                        currentNickname =
                            getAppShortcutNickname(
                                shortcutKey(shortcut),
                            ),
                        itemName =
                            shortcutDisplayName(shortcut),
                    ),
                )
            },
            onTriggerClick = { shortcut ->
                onUpdateTriggerDialogState(
                    TriggerDialogState.AppShortcut(
                        shortcut = shortcut,
                        currentTrigger = getAppShortcutTrigger(shortcutKey(shortcut)),
                        itemName = shortcutDisplayName(shortcut),
                    ),
                )
            },
            onEditCustomShortcut = onEditCustomAppShortcut,
            onEditShortcutIcon = onEditAppShortcutIcon,
            getShortcutNickname = getAppShortcutNickname,
            getShortcutTrigger = getAppShortcutTrigger,
            showAllResults = false,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.APP_SHORTCUTS) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.APP_SHORTCUTS
                    },
                )
            },
            iconPackPackage = state.selectedIconPackPackage,
            showWallpaperBackground = state.showWallpaperBackground,
        )

    val settingsParams =
        SettingsSectionParams(
            settings = state.settingResults,
            appSettings = state.appSettingResults,
            isExpanded = expandedSection == ExpandedSection.SETTINGS,
            pinnedSettingIds = derivedState.pinnedSettingIds,
            onSettingClick = onSettingClick,
            onAppSettingClick = onAppSettingClick,
            onAppSettingToggle = onAppSettingToggle,
            onAppSettingWebSuggestionsCountChange = onAppSettingWebSuggestionsCountChange,
            isAppSettingToggleChecked = isAppSettingToggleChecked,
            appSettingWebSuggestionsCount = appSettingWebSuggestionsCount,
            appSettingPhoneAppGridColumns = appSettingPhoneAppGridColumns,
            onAppSettingPhoneAppGridColumnsChange = onAppSettingPhoneAppGridColumnsChange,
            appSettingAppResultRowCount = appSettingAppResultRowCount,
            onAppSettingAppResultRowCountChange = onAppSettingAppResultRowCountChange,
            onTogglePin = { setting ->
                if (derivedState.pinnedSettingIds.contains(setting.id)) {
                    onUnpinSetting(setting)
                } else {
                    onPinSetting(setting)
                }
            },
            onMovePinned = onMovePinnedSetting,
            onExclude = onExcludeSetting,
            onNicknameClick = { setting ->
                onUpdateNicknameDialogState(
                    NicknameDialogState.Setting(
                        setting = setting,
                        currentNickname =
                            getSettingNickname(setting.id),
                        itemName = setting.title,
                    ),
                )
            },
            onTriggerClick = { setting ->
                onUpdateTriggerDialogState(
                    TriggerDialogState.Setting(
                        setting = setting,
                        currentTrigger = getSettingTrigger(setting.id),
                        itemName = setting.title,
                    ),
                )
            },
            getSettingNickname = getSettingNickname,
            getSettingTrigger = getSettingTrigger,
            showAllResults = false,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.SETTINGS) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.SETTINGS
                    },
                )
            },
            onAppSettingExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.APP_SETTINGS) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.APP_SETTINGS
                    },
                )
            },
            showWallpaperBackground = state.showWallpaperBackground,
        )

    val contactsParams =
        ContactsSectionParams(
            contacts = state.contactResults,
            hasPermission = state.hasContactPermission,
            isExpanded = expandedSection == ExpandedSection.CONTACTS,
            callingApp = state.callingApp,
            messagingApp = state.messagingApp,
            pinnedContactIds = derivedState.pinnedContactIds,
            onContactClick = onContactClick,
            onShowContactMethods = onShowContactMethods,
            onCallContact = onCallContact,
            onSmsContact = onSmsContact,
            onContactMethodClick = onContactMethodClick,
            onTogglePin = { contact ->
                if (derivedState.pinnedContactIds.contains(
                        contact.contactId,
                    )
                ) {
                    onUnpinContact(contact)
                } else {
                    onPinContact(contact)
                }
            },
            onMovePinned = onMovePinnedContact,
            onExclude = onExcludeContact,
            onNicknameClick = { contact ->
                onUpdateNicknameDialogState(
                    NicknameDialogState.Contact(
                        contact = contact,
                        currentNickname =
                            getContactNickname(
                                contact.contactId,
                            ),
                        itemName = contact.displayName,
                    ),
                )
            },
            onTriggerClick = { contact ->
                onUpdateTriggerDialogState(
                    TriggerDialogState.Contact(
                        contact = contact,
                        currentTrigger = getContactTrigger(contact.contactId),
                        itemName = contact.displayName,
                    ),
                )
            },
            getContactNickname = getContactNickname,
            getContactTrigger = getContactTrigger,
            getContactActionTrigger = getContactActionTrigger,
            getPrimaryContactCardAction = getPrimaryContactCardAction,
            getSecondaryContactCardAction = getSecondaryContactCardAction,
            onPrimaryActionLongPress = onPrimaryActionLongPress,
            onSecondaryActionLongPress = onSecondaryActionLongPress,
            onCustomAction = onCustomAction,
            showContactActionHint = state.showContactActionHint,
            onContactActionHintDismissed = onContactActionHintDismissed,
            onOpenAppSettings = onOpenAppSettings,
            showAllResults = false,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.CONTACTS) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.CONTACTS
                    },
                )
            },
            permissionDisabledCard = {
                title,
                message,
                actionLabel,
                onActionClick,
                ->
                PermissionDisabledCard(
                    title = title,
                    message = message,
                    actionLabel = actionLabel,
                    onActionClick = onActionClick,
                )
            },
            showWallpaperBackground = state.showWallpaperBackground,
        )
    val suggestionExcludedKeys = state.suggestionExcludedApps.map { it.launchCountKey() }.toSet()
    val allSuggestionApps =
        state.allApps.filter { app ->
            app.hasLaunchIntent && !suggestionExcludedKeys.contains(app.launchCountKey())
        }
    val appsParams =
        AppsSectionParams(
            apps = derivedState.displayApps,
            allApps = allSuggestionApps,
            pinnedAndRecentApps = derivedState.pinnedAndRecentApps,
            pinnedApps = state.pinnedApps,
            newOrUpdatedApps = derivedState.newOrUpdatedApps,
            mostUsedApps = derivedState.mostUsedApps,
            appShortcuts = state.allAppShortcuts,
            isSearching = derivedState.isSearching,
            hasUsagePermission = state.hasUsagePermission,
            selectedSuggestionTab = state.selectedAppSuggestionTab,
            enabledSuggestionTabs =
                if (state.appSuggestionsEnabled) {
                    state.enabledAppSuggestionTabs
                } else {
                    setOf(AppSuggestionTabType.PINNED)
                },
            onSuggestionTabSelected = onSuggestionTabSelected,
            hasAppResults = derivedState.hasAppResults,
            showAllAppsButton = state.showAllAppsButton,
            pinnedPackageNames = derivedState.pinnedPackageNames,
            disabledAppShortcutIds = state.disabledAppShortcutIds,
            onAppClick = onAppClick,
            onAppShortcutClick = onAppShortcutClick,
            onOpenInSplitScreen = onOpenInSplitScreen,
            onAppInfoClick = onAppInfoClick,
            onUninstallClick = onUninstallClick,
            onHideApp = onHideApp,
            onDisableAppShortcut = onDisableAppShortcut,
            onPinApp = onPinApp,
            onUnpinApp = onUnpinApp,
            onReorderPinnedApps = onReorderPinnedApps,
            onNicknameClick = { app ->
                onUpdateNicknameDialogState(
                    NicknameDialogState.App(
                        app = app,
                        currentNickname =
                            getAppNickname(app.packageName),
                        itemName = app.appName,
                    ),
                )
            },
            onTriggerClick = { app ->
                onUpdateTriggerDialogState(
                    TriggerDialogState.App(
                        app = app,
                        currentTrigger = getAppTrigger(app.packageName),
                        itemName = app.appName,
                    ),
                )
            },
            getAppNickname = getAppNickname,
            getAppTrigger = getAppTrigger,
            rowCount = derivedState.visibleRowCount,
            phoneColumnOverride = state.phoneAppGridColumns,
            appIconSizeStep = state.appIconSizeStep,
            iconPackPackage = state.selectedIconPackPackage,
            appIconShape = state.appIconShape,
            themedIconsEnabled = state.themedIconsEnabled,
            showAppLabels = state.showAppLabels,
            oneHandedMode = state.oneHandedMode,
            isInitializing = state.isInitializing,
            startupPhase = state.startupPhase,
            isOverlayPresentation = isOverlayPresentation,
            showWallpaperBackground = state.showWallpaperBackground,
            notificationDotsEnabled = state.notificationDotsEnabled,
            showRateQuickSearchCard =
                state.showRateQuickSearchCard &&
                    !derivedState.isSearching &&
                    !isOverlayPresentation,
            onRateQuickSearchClick = onRateQuickSearchClick,
            onRateQuickSearchNotNowClick = onRateQuickSearchNotNowClick,
            showUpdateCard = state.showUpdateCard && !derivedState.isSearching && !isOverlayPresentation,
            onUpdateClick = onUpdateClick,
            onUpdateNotNowClick = onUpdateNotNowClick,
            pinnedGridAppShortcuts =
                if (state.pinnedAppShortcutsInAppGrid && !derivedState.isSearching) {
                    state.pinnedAppShortcuts
                } else {
                    emptyList()
                },
            pinnedAppGridOrder = state.pinnedAppGridOrder,
            onReorderPinnedAppGrid = onReorderPinnedAppGrid,
            appFolders = state.appFolders,
            appFolderActions = appFolderActions,
            pinnedGridShortcutActions =
                com.tk.quicksearch.search.apps.AppGridShortcutActions(
                    onTogglePin = appShortcutParams.onTogglePin,
                    onDisable = appShortcutParams.onDisable,
                    onDisableAllForApp = appShortcutParams.onDisableAllForApp,
                    onAppInfoClick = appShortcutParams.onAppInfoClick,
                    onNicknameClick = appShortcutParams.onNicknameClick,
                    onTriggerClick = appShortcutParams.onTriggerClick,
                    onEditCustomShortcut = appShortcutParams.onEditCustomShortcut,
                    onEditShortcutIcon = appShortcutParams.onEditShortcutIcon,
                    getNickname = appShortcutParams.getShortcutNickname,
                    getTrigger = appShortcutParams.getShortcutTrigger,
                ),
        )

    val calendarParams =
        CalendarSectionParams(
            events = state.calendarEvents,
            hasPermission = state.hasCalendarPermission,
            isExpanded = expandedSection == ExpandedSection.CALENDAR,
            pinnedEventIds = state.pinnedCalendarEvents.map { it.eventId }.toSet(),
            excludedEventIds = state.excludedCalendarEvents.map { it.eventId }.toSet(),
            onEventClick = onCalendarEventClick,
            onRequestPermission = onOpenCalendarPermissionSettings,
            onTogglePin = { event ->
                if (state.pinnedCalendarEvents.any { it.eventId == event.eventId }) {
                    onUnpinCalendarEvent(event)
                } else {
                    onPinCalendarEvent(event)
                }
            },
            onMovePinned = onMovePinnedCalendarEvent,
            onExclude = onExcludeCalendarEvent,
            onInclude = onIncludeCalendarEvent,
            onArchiveTodayEvent = onArchiveTodayCalendarEvent,
            onNicknameClick = { event ->
                onUpdateNicknameDialogState(
                    NicknameDialogState.CalendarEvent(
                        event = event,
                        currentNickname = getCalendarEventNickname(event.eventId),
                        itemName = event.title,
                    ),
                )
            },
            getEventNickname = getCalendarEventNickname,
            showAllResults = false,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.CALENDAR) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.CALENDAR
                    },
                )
            },
            permissionDisabledCard = { title, message, actionLabel, onActionClick ->
                PermissionDisabledCard(
                    title = title,
                    message = message,
                    actionLabel = actionLabel,
                    onActionClick = onActionClick,
                )
            },
            showWallpaperBackground = state.showWallpaperBackground,
        )

    val notesParams =
        NotesSectionParams(
            pinnedNoteIds = state.pinnedNotes.map { it.noteId }.toSet(),
            onNoteClick = onNoteClick,
            onTogglePin = { note ->
                if (state.pinnedNotes.any { it.noteId == note.noteId }) {
                    onUnpinNote(note)
                } else {
                    onPinNote(note)
                }
            },
            onMovePinned = onMovePinnedNote,
            onDelete = onDeleteNote,
            onTriggerClick = { note ->
                onUpdateTriggerDialogState(
                    TriggerDialogState.Note(
                        note = note,
                        currentTrigger = getNoteTrigger(note.noteId),
                        itemName = note.title,
                    ),
                )
            },
            getNoteTrigger = getNoteTrigger,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.NOTES) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.NOTES
                    },
                )
            },
            showWallpaperBackground = state.showWallpaperBackground,
        )

    val remindersParams =
        RemindersSectionParams(
            pinnedReminderIds = state.pinnedReminders.map { it.reminderId }.toSet(),
            onReminderClick = ReminderEditorRequests::openEdit,
            onTogglePin = { reminder ->
                if (state.pinnedReminders.any { it.reminderId == reminder.reminderId }) {
                    reminderActions.onUnpin(reminder)
                } else {
                    reminderActions.onPin(reminder)
                }
            },
            onMovePinned = reminderActions.onMovePinned,
            onMarkDone = reminderActions.onMarkDone,
            onDelete = reminderActions.onDelete,
            showExpandControls = derivedState.isSearching,
            onExpandClick = {
                onUpdateExpandedSection(
                    if (expandedSection == ExpandedSection.REMINDERS) {
                        ExpandedSection.NONE
                    } else {
                        ExpandedSection.REMINDERS
                    },
                )
            },
            showWallpaperBackground = state.showWallpaperBackground,
        )

    SectionParams(
        filesParams = filesParams,
        appShortcutsParams = appShortcutParams,
        settingsParams = settingsParams,
        contactsParams = contactsParams,
        calendarParams = calendarParams,
        notesParams = notesParams,
        appsParams = appsParams,
        remindersParams = remindersParams,
    )
}

/** Data class to hold all section parameters */
data class SectionParams(
    val filesParams: FilesSectionParams,
    val appShortcutsParams: AppShortcutsSectionParams,
    val settingsParams: SettingsSectionParams,
    val contactsParams: ContactsSectionParams,
    val calendarParams: CalendarSectionParams,
    val notesParams: NotesSectionParams,
    val appsParams: AppsSectionParams,
    val remindersParams: RemindersSectionParams,
)
