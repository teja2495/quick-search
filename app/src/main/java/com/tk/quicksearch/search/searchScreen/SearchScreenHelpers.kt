package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState

sealed interface PredictedSubmitTarget {
    data class App(val packageName: String, val userHandleId: Int?) : PredictedSubmitTarget

    data class AppShortcut(val id: String) : PredictedSubmitTarget

    data class Contact(val contactId: Long) : PredictedSubmitTarget

    data class File(val uri: String) : PredictedSubmitTarget

    data class Setting(val id: String) : PredictedSubmitTarget

    data class AppSetting(val id: String) : PredictedSubmitTarget

    data class Calendar(val eventId: Long) : PredictedSubmitTarget

    data class SearchTarget(val targetId: String) : PredictedSubmitTarget
}

internal fun resolvePredictedSubmitTarget(
    query: String,
    renderingState: SectionRenderingState,
    enabledTargets: List<SearchTarget>,
    detectedShortcutTarget: SearchTarget?,
    searchTargetsOrder: List<SearchTarget>,
    defaultBrowserPackage: String?,
): PredictedSubmitTarget? {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isNotBlank() && isLikelyWebUrl(trimmedQuery)) {
        val browserTarget = defaultBrowserTarget(searchTargetsOrder, defaultBrowserPackage)
        if (browserTarget != null) {
            return PredictedSubmitTarget.SearchTarget(browserTarget.getId())
        }
    }

    val firstApp = renderingState.displayApps.firstOrNull()
    if (firstApp != null) {
        return PredictedSubmitTarget.App(
            packageName = firstApp.packageName,
            userHandleId = firstApp.userHandleId,
        )
    }

    val firstShortcut = renderingState.appShortcutResults.firstOrNull()
    if (firstShortcut != null) {
        return PredictedSubmitTarget.AppShortcut(shortcutKey(firstShortcut))
    }

    val firstContact = renderingState.contactResults.firstOrNull()
    if (firstContact != null) {
        return PredictedSubmitTarget.Contact(firstContact.contactId)
    }

    val firstFile = renderingState.fileResults.firstOrNull()
    if (firstFile != null) {
        return PredictedSubmitTarget.File(firstFile.uri.toString())
    }

    val firstSetting = renderingState.settingResults.firstOrNull()
    if (firstSetting != null) {
        return PredictedSubmitTarget.Setting(firstSetting.id)
    }

    val firstCalendarEvent = renderingState.calendarEvents.firstOrNull()
    if (firstCalendarEvent != null) {
        return PredictedSubmitTarget.Calendar(firstCalendarEvent.eventId)
    }

    val firstAppSetting =
        renderingState.appSettingResults.firstOrNull { it.isNavigateAction }
    if (firstAppSetting != null) {
        return PredictedSubmitTarget.AppSetting(firstAppSetting.id)
    }

    if (detectedShortcutTarget != null) {
        return PredictedSubmitTarget.SearchTarget(detectedShortcutTarget.getId())
    }

    if (trimmedQuery.isBlank()) return null

    val firstEnabledTarget = enabledTargets.firstOrNull() ?: return null
    return PredictedSubmitTarget.SearchTarget(firstEnabledTarget.getId())
}

/** Data class for Files section parameters */
data class FilesSectionParams(
    val files: List<DeviceFile>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val pinnedFileUris: Set<String>,
    val onFileClick: (DeviceFile) -> Unit,
    val onOpenFolder: (DeviceFile) -> Unit,
    val onRequestPermission: () -> Unit,
    val onTogglePin: (DeviceFile) -> Unit,
    val onExclude: (DeviceFile) -> Unit,
    val onExcludeExtension: (DeviceFile) -> Unit,
    val onNicknameClick: (DeviceFile) -> Unit,
    val getFileNickname: (String) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Settings section parameters */
data class SettingsSectionParams(
    val settings: List<DeviceSetting>,
    val appSettings: List<AppSettingResult>,
    val isExpanded: Boolean,
    val pinnedSettingIds: Set<String>,
    val onSettingClick: (DeviceSetting) -> Unit,
    val onAppSettingClick: (AppSettingResult) -> Unit,
    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    val isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    val onTogglePin: (DeviceSetting) -> Unit,
    val onExclude: (DeviceSetting) -> Unit,
    val onNicknameClick: (DeviceSetting) -> Unit,
    val getSettingNickname: (String) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for App Shortcuts section parameters */
data class AppShortcutsSectionParams(
    val shortcuts: List<StaticShortcut>,
    val isExpanded: Boolean,
    val pinnedShortcutIds: Set<String>,
    val excludedShortcutIds: Set<String>,
    val onShortcutClick: (StaticShortcut) -> Unit,
    val onTogglePin: (StaticShortcut) -> Unit,
    val onExclude: (StaticShortcut) -> Unit,
    val onInclude: (StaticShortcut) -> Unit,
    val onAppInfoClick: (StaticShortcut) -> Unit,
    val onNicknameClick: (StaticShortcut) -> Unit,
    val getShortcutNickname: (String) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val iconPackPackage: String?,
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Contacts section parameters */
data class ContactsSectionParams(
    val contacts: List<ContactInfo>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val callingApp: CallingApp?,
    val messagingApp: MessagingApp?,
    val pinnedContactIds: Set<Long>,
    val onContactClick: (ContactInfo) -> Unit,
    val onShowContactMethods: (ContactInfo) -> Unit,
    val onCallContact: (ContactInfo) -> Unit,
    val onSmsContact: (ContactInfo) -> Unit,
    val onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
    val onTogglePin: (ContactInfo) -> Unit,
    val onExclude: (ContactInfo) -> Unit,
    val onNicknameClick: (ContactInfo) -> Unit,
    val getContactNickname: (Long) -> String?,
    val getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    val getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    val onPrimaryActionLongPress: (ContactInfo) -> Unit,
    val onSecondaryActionLongPress: (ContactInfo) -> Unit,
    val onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    val showContactActionHint: Boolean = false,
    val onContactActionHintDismissed: () -> Unit = {},
    val onOpenAppSettings: () -> Unit,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Apps section parameters */
data class AppsSectionParams(
    val apps: List<AppInfo>,
    val appShortcuts: List<StaticShortcut>,
    val isSearching: Boolean,
    val hasAppResults: Boolean,
    val pinnedPackageNames: Set<String>,
    val disabledAppShortcutIds: Set<String>,
    val onAppClick: (AppInfo) -> Unit,
    val onAppInfoClick: (AppInfo) -> Unit,
    val onUninstallClick: (AppInfo) -> Unit,
    val onHideApp: (AppInfo) -> Unit,
    val onPinApp: (AppInfo) -> Unit,
    val onUnpinApp: (AppInfo) -> Unit,
    val onNicknameClick: (AppInfo) -> Unit,
    val getAppNickname: (String) -> String?,
    val rowCount: Int,
    val iconPackPackage: String?,
    val showAppLabels: Boolean,
    val oneHandedMode: Boolean,
    val isInitializing: Boolean,
    val startupPhase: StartupPhase,
    val isOverlayPresentation: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Calendar section parameters */
data class CalendarSectionParams(
    val events: List<CalendarEventInfo>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val pinnedEventIds: Set<Long>,
    val excludedEventIds: Set<Long>,
    val onEventClick: (CalendarEventInfo) -> Unit,
    val onRequestPermission: () -> Unit,
    val onTogglePin: (CalendarEventInfo) -> Unit,
    val onExclude: (CalendarEventInfo) -> Unit,
    val onInclude: (CalendarEventInfo) -> Unit,
    val onNicknameClick: (CalendarEventInfo) -> Unit,
    val getEventNickname: (Long) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

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
    onExcludeFile: (DeviceFile) -> Unit,
    onExcludeFileExtension: (DeviceFile) -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onSettingClick: (DeviceSetting) -> Unit,
    onAppSettingClick: (AppSettingResult) -> Unit,
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    onPinSetting: (DeviceSetting) -> Unit,
    onUnpinSetting: (DeviceSetting) -> Unit,
    onExcludeSetting: (DeviceSetting) -> Unit,
    onAppShortcutClick: (StaticShortcut) -> Unit,
    onPinAppShortcut: (StaticShortcut) -> Unit,
    onUnpinAppShortcut: (StaticShortcut) -> Unit,
    onExcludeAppShortcut: (StaticShortcut) -> Unit,
    onIncludeAppShortcut: (StaticShortcut) -> Unit,
    onAppShortcutAppInfoClick: (StaticShortcut) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
    onPinContact: (ContactInfo) -> Unit,
    onUnpinContact: (ContactInfo) -> Unit,
    onExcludeContact: (ContactInfo) -> Unit,
    onCalendarEventClick: (CalendarEventInfo) -> Unit,
    onPinCalendarEvent: (CalendarEventInfo) -> Unit,
    onUnpinCalendarEvent: (CalendarEventInfo) -> Unit,
    onExcludeCalendarEvent: (CalendarEventInfo) -> Unit,
    onIncludeCalendarEvent: (CalendarEventInfo) -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    onPrimaryActionLongPress: (ContactInfo) -> Unit,
    onSecondaryActionLongPress: (ContactInfo) -> Unit,
    onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    onContactActionHintDismissed: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    getFileNickname: (String) -> String?,
    getContactNickname: (Long) -> String?,
    getSettingNickname: (String) -> String?,
    getAppNickname: (String) -> String?,
    getAppShortcutNickname: (String) -> String?,
    getCalendarEventNickname: (Long) -> String?,
    onUpdateNicknameDialogState: (NicknameDialogState?) -> Unit,
    onUpdateExpandedSection: (ExpandedSection) -> Unit,
    expandedSection: ExpandedSection,
) = remember(
    state,
    derivedState,
    expandedSection,
    onFileClick,
    onOpenFolder,
    onPinFile,
    onUnpinFile,
    onExcludeFile,
    onExcludeFileExtension,
    onOpenStorageAccessSettings,
    onSettingClick,
    onAppSettingClick,
    onAppSettingToggle,
    isAppSettingToggleChecked,
    onPinSetting,
    onUnpinSetting,
    onExcludeSetting,
    onAppShortcutClick,
    onPinAppShortcut,
    onUnpinAppShortcut,
    onExcludeAppShortcut,
    onIncludeAppShortcut,
    onAppShortcutAppInfoClick,
    onContactClick,
    onShowContactMethods,
    onCallContact,
    onSmsContact,
    onContactMethodClick,
    onPinContact,
    onUnpinContact,
    onExcludeContact,
    onCalendarEventClick,
    onPinCalendarEvent,
    onUnpinCalendarEvent,
    onExcludeCalendarEvent,
    onIncludeCalendarEvent,
    onOpenCalendarPermissionSettings,
    onOpenAppSettings,
    getPrimaryContactCardAction,
    getSecondaryContactCardAction,
    onPrimaryActionLongPress,
    onSecondaryActionLongPress,
    onCustomAction,
    onContactActionHintDismissed,
    onAppClick,
    onAppInfoClick,
    onUninstallClick,
    onHideApp,
    onPinApp,
    onUnpinApp,
    getFileNickname,
    getContactNickname,
    getSettingNickname,
    getAppNickname,
    getAppShortcutNickname,
    getCalendarEventNickname,
    onUpdateNicknameDialogState,
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
            getFileNickname = getFileNickname,
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
            onExclude = onExcludeAppShortcut,
            onInclude = onIncludeAppShortcut,
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
            getShortcutNickname = getAppShortcutNickname,
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
            isAppSettingToggleChecked = isAppSettingToggleChecked,
            onTogglePin = { setting ->
                if (derivedState.pinnedSettingIds.contains(setting.id)) {
                    onUnpinSetting(setting)
                } else {
                    onPinSetting(setting)
                }
            },
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
            getSettingNickname = getSettingNickname,
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
            getContactNickname = getContactNickname,
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
    val appsParams =
        AppsSectionParams(
            apps = derivedState.displayApps,
            appShortcuts = state.allAppShortcuts,
            isSearching = derivedState.isSearching,
            hasAppResults = derivedState.hasAppResults,
            pinnedPackageNames = derivedState.pinnedPackageNames,
            disabledAppShortcutIds = state.disabledAppShortcutIds,
            onAppClick = onAppClick,
            onAppInfoClick = onAppInfoClick,
            onUninstallClick = onUninstallClick,
            onHideApp = onHideApp,
            onPinApp = onPinApp,
            onUnpinApp = onUnpinApp,
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
            getAppNickname = getAppNickname,
            rowCount = derivedState.visibleRowCount,
            iconPackPackage = state.selectedIconPackPackage,
            showAppLabels = state.showAppLabels,
            oneHandedMode = state.oneHandedMode,
            isInitializing = state.isInitializing,
            startupPhase = state.startupPhase,
            isOverlayPresentation = isOverlayPresentation,
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
            onExclude = onExcludeCalendarEvent,
            onInclude = onIncludeCalendarEvent,
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

    SectionParams(
        filesParams = filesParams,
        appShortcutsParams = appShortcutParams,
        settingsParams = settingsParams,
        contactsParams = contactsParams,
        calendarParams = calendarParams,
        appsParams = appsParams,
    )
}

/** Data class to hold all section parameters */
data class SectionParams(
    val filesParams: FilesSectionParams,
    val appShortcutsParams: AppShortcutsSectionParams,
    val settingsParams: SettingsSectionParams,
    val contactsParams: ContactsSectionParams,
    val calendarParams: CalendarSectionParams,
    val appsParams: AppsSectionParams,
)
