package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.appSettings.AppSettingsResultsSection
import com.tk.quicksearch.search.appShortcuts.AppShortcutResultsSection
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.calendar.CalendarEventsSection
import com.tk.quicksearch.search.contacts.ContactResultsSection
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SectionRenderContext
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsResultsSection
import com.tk.quicksearch.search.files.FileResultsSection
import com.tk.quicksearch.search.notes.NotesResultsSection

// ============================================================================
// Section Rendering Functions
// ============================================================================

/** Renders a single section based on its type and current state. */
@Composable
fun renderSection(
    section: SearchSection,
    params: SectionRenderParams,
    sectionContext: SectionRenderContext,
) {
    when (section) {
        SearchSection.FILES -> renderFilesSection(params, sectionContext)
        SearchSection.CONTACTS -> renderContactsSection(params, sectionContext)
        SearchSection.APPS -> renderAppsSection(params, sectionContext)
        SearchSection.APP_SHORTCUTS -> renderAppShortcutsSection(params, sectionContext)
        SearchSection.SETTINGS -> renderSettingsSection(params, sectionContext)
        SearchSection.CALENDAR -> renderCalendarSection(params, sectionContext)
        SearchSection.NOTES -> renderNotesSection(params, sectionContext)
        SearchSection.APP_SETTINGS -> renderAppSettingsSection(params, sectionContext)
    }
}

/** Renders the files section if it should be displayed. */
@Composable
private fun renderFilesSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    if (context.shouldRenderFiles) {
        val filesParams =
            params.filesParams.copy(
                files = context.filesList,
                isExpanded = context.isFilesExpanded,
                showAllResults = context.showAllFilesResults,
                showExpandControls = context.showFilesExpandControls,
                onExpandClick = context.filesExpandClick,
            )
        FileResultsSection(
            hasPermission = filesParams.hasPermission,
            files = filesParams.files,
            isExpanded = filesParams.isExpanded,
            onFileClick = filesParams.onFileClick,
            onOpenFolder = filesParams.onOpenFolder,
            onRequestPermission = filesParams.onRequestPermission,
            pinnedFileUris = filesParams.pinnedFileUris,
            onTogglePin = filesParams.onTogglePin,
            onExclude = filesParams.onExclude,
            onExcludeExtension = filesParams.onExcludeExtension,
            onNicknameClick = filesParams.onNicknameClick,
            getFileNickname = filesParams.getFileNickname,
            showAllResults = filesParams.showAllResults,
            showExpandControls = filesParams.showExpandControls,
            onExpandClick = filesParams.onExpandClick,
            expandedCardMaxHeight = filesParams.expandedCardMaxHeight,
            permissionDisabledCard = filesParams.permissionDisabledCard,
            showWallpaperBackground = filesParams.showWallpaperBackground,
            predictedTarget = filesParams.predictedTarget,
            fillExpandedHeight = context.isSectionAliasMode,
        )
    }
}

/** Renders the contacts section if it should be displayed. */
@Composable
private fun renderContactsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    if (context.shouldRenderContacts) {
        val contactsParams =
            params.contactsParams.copy(
                contacts = context.contactsList,
                isExpanded = context.isContactsExpanded,
                showAllResults = context.showAllContactsResults,
                showExpandControls = context.showContactsExpandControls,
                onExpandClick = context.contactsExpandClick,
            )
        ContactResultsSection(
            hasPermission = contactsParams.hasPermission,
            contacts = contactsParams.contacts,
            isExpanded = contactsParams.isExpanded,
            callingApp = contactsParams.callingApp ?: CallingApp.CALL,
            messagingApp = contactsParams.messagingApp ?: MessagingApp.MESSAGES,
            onContactClick = contactsParams.onContactClick,
            onShowContactMethods = contactsParams.onShowContactMethods,
            onCallContact = contactsParams.onCallContact,
            onSmsContact = contactsParams.onSmsContact,
            onContactMethodClick = contactsParams.onContactMethodClick,
            pinnedContactIds = contactsParams.pinnedContactIds,
            onTogglePin = contactsParams.onTogglePin,
            onExclude = contactsParams.onExclude,
            onNicknameClick = contactsParams.onNicknameClick,
            getContactNickname = contactsParams.getContactNickname,
            getPrimaryContactCardAction = contactsParams.getPrimaryContactCardAction,
            getSecondaryContactCardAction = contactsParams.getSecondaryContactCardAction,
            onPrimaryActionLongPress = contactsParams.onPrimaryActionLongPress,
            onSecondaryActionLongPress = contactsParams.onSecondaryActionLongPress,
            onCustomAction = contactsParams.onCustomAction,
            onOpenAppSettings = contactsParams.onOpenAppSettings,
            showAllResults = contactsParams.showAllResults,
            showExpandControls = contactsParams.showExpandControls,
            onExpandClick = contactsParams.onExpandClick,
            expandedCardMaxHeight = contactsParams.expandedCardMaxHeight,
            showContactActionHint = contactsParams.showContactActionHint,
            onContactActionHintDismissed = contactsParams.onContactActionHintDismissed,
            permissionDisabledCard = contactsParams.permissionDisabledCard,
            showWallpaperBackground = contactsParams.showWallpaperBackground,
            predictedTarget = contactsParams.predictedTarget,
            fillExpandedHeight = context.isSectionAliasMode,
        )
    }
}

/** Renders the apps section if it should be displayed. */
@Composable
private fun renderAppsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val appsParams = params.appsParams ?: return

    if (context.shouldRenderApps && appsParams.hasAppResults && appsParams.apps.isNotEmpty()) {
        AppGridView(
            apps = appsParams.apps,
            appShortcuts = appsParams.appShortcuts,
            isSearching = appsParams.isSearching,
            hasAppResults = appsParams.hasAppResults,
            onAppClick = appsParams.onAppClick,
            onAppInfoClick = appsParams.onAppInfoClick,
            onUninstallClick = appsParams.onUninstallClick,
            onHideApp = appsParams.onHideApp,
            onPinApp = appsParams.onPinApp,
            onUnpinApp = appsParams.onUnpinApp,
            onNicknameClick = appsParams.onNicknameClick,
            getAppNickname = appsParams.getAppNickname,
            pinnedPackageNames = appsParams.pinnedPackageNames,
            disabledShortcutIds = appsParams.disabledAppShortcutIds,
            rowCount = appsParams.rowCount,
            phoneColumnOverride = appsParams.phoneColumnOverride,
            iconPackPackage = appsParams.iconPackPackage,
            appIconShape = appsParams.appIconShape,
            themedIconsEnabled = appsParams.themedIconsEnabled,
            showAppLabels = appsParams.showAppLabels,
            oneHandedMode = appsParams.oneHandedMode,
            isInitializing = appsParams.isInitializing,
            startupPhase = appsParams.startupPhase,
            isOverlayPresentation = appsParams.isOverlayPresentation,
            predictedTarget = appsParams.predictedTarget,
            showWallpaperBackground = appsParams.showWallpaperBackground,
        )
    }
}

/** Renders the app shortcuts section if it should be displayed. */
@Composable
private fun renderAppShortcutsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val appShortcutsParams = params.appShortcutsParams ?: return
    if (context.shouldRenderAppShortcuts) {
        AppShortcutResultsSection(
            shortcuts = context.appShortcutsList,
            isExpanded = context.isAppShortcutsExpanded,
            pinnedShortcutIds = appShortcutsParams.pinnedShortcutIds,
            excludedShortcutIds = appShortcutsParams.excludedShortcutIds,
            onShortcutClick = appShortcutsParams.onShortcutClick,
            onTogglePin = appShortcutsParams.onTogglePin,
            onExclude = appShortcutsParams.onExclude,
            onInclude = appShortcutsParams.onInclude,
            onAppInfoClick = appShortcutsParams.onAppInfoClick,
            onNicknameClick = appShortcutsParams.onNicknameClick,
            onEditCustomShortcut = appShortcutsParams.onEditCustomShortcut,
            onEditShortcutIcon = appShortcutsParams.onEditShortcutIcon,
            getShortcutNickname = appShortcutsParams.getShortcutNickname,
            showAllResults = context.showAllAppShortcutsResults,
            showExpandControls = context.showAppShortcutsExpandControls,
            onExpandClick = context.appShortcutsExpandClick,
            expandedCardMaxHeight = appShortcutsParams.expandedCardMaxHeight,
            iconPackPackage = appShortcutsParams.iconPackPackage,
            showWallpaperBackground = appShortcutsParams.showWallpaperBackground,
            predictedTarget = appShortcutsParams.predictedTarget,
            fillExpandedHeight = context.isSectionAliasMode,
        )
    }
}

/** Renders the device settings section if it should be displayed. */
@Composable
private fun renderSettingsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val shouldShowSettings =
        context.shouldRenderSettings &&
            params.settingsParams != null &&
            context.settingsList.isNotEmpty() &&
            !context.isAppSettingsExpanded
    if (shouldShowSettings) {
        if (params.settingsParams != null) {
            DeviceSettingsResultsSection(
                settings = context.settingsList,
                isExpanded = context.isDeviceSettingsExpanded,
                pinnedSettingIds = params.settingsParams.pinnedSettingIds,
                onSettingClick = params.settingsParams.onSettingClick,
                onTogglePin = params.settingsParams.onTogglePin,
                onExclude = params.settingsParams.onExclude,
                onNicknameClick = params.settingsParams.onNicknameClick,
                getSettingNickname = params.settingsParams.getSettingNickname,
                showAllResults = context.showAllSettingsResults,
                showExpandControls = context.showDeviceSettingsExpandControls,
                onExpandClick = context.deviceSettingsExpandClick,
                expandedCardMaxHeight = params.settingsParams.expandedCardMaxHeight,
                showWallpaperBackground = params.settingsParams.showWallpaperBackground,
                predictedTarget = params.settingsParams.predictedTarget,
                fillExpandedHeight = context.isSectionAliasMode,
            )
        }
    }
}

/** Renders the app settings section if it should be displayed. */
@Composable
private fun renderAppSettingsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val shouldShowAppSettings =
        context.shouldRenderSettings &&
            params.settingsParams != null &&
            context.appSettingsList.isNotEmpty() &&
            !context.isDeviceSettingsExpanded
    if (shouldShowAppSettings) {
        if (params.settingsParams != null) {
            AppSettingsResultsSection(
                appSettings = context.appSettingsList,
                isExpanded = context.isAppSettingsExpanded,
                onAppSettingClick = params.settingsParams.onAppSettingClick,
                onAppSettingToggle = params.settingsParams.onAppSettingToggle,
                onWebSuggestionsCountChange =
                    params.settingsParams.onAppSettingWebSuggestionsCountChange,
                isAppSettingToggleChecked = params.settingsParams.isAppSettingToggleChecked,
                webSuggestionsCount = params.settingsParams.appSettingWebSuggestionsCount,
                appSettingPhoneAppGridColumns = params.settingsParams.appSettingPhoneAppGridColumns,
                onAppSettingPhoneAppGridColumnsChange = params.settingsParams.onAppSettingPhoneAppGridColumnsChange,
                showAllResults = context.showAllSettingsResults,
                showExpandControls = context.showAppSettingsExpandControls,
                onExpandClick = context.appSettingsExpandClick,
                expandedCardMaxHeight = params.settingsParams.expandedCardMaxHeight,
                showWallpaperBackground = params.settingsParams.showWallpaperBackground,
                predictedTarget = params.settingsParams.predictedTarget,
                fillExpandedHeight = context.isSectionAliasMode,
            )
        }
    }
}

@Composable
private fun renderCalendarSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val calendarParams = params.calendarParams ?: return
    if (context.shouldRenderCalendar) {
        CalendarEventsSection(
            events = context.calendarEventsList,
            hasPermission = calendarParams.hasPermission,
            isExpanded = context.isCalendarExpanded,
            pinnedEventIds = calendarParams.pinnedEventIds,
            excludedEventIds = calendarParams.excludedEventIds,
            onEventClick = calendarParams.onEventClick,
            onRequestPermission = calendarParams.onRequestPermission,
            onTogglePin = calendarParams.onTogglePin,
            onExclude = calendarParams.onExclude,
            onInclude = calendarParams.onInclude,
            onNicknameClick = calendarParams.onNicknameClick,
            onArchiveTodayEvent = calendarParams.onArchiveTodayEvent,
            getEventNickname = calendarParams.getEventNickname,
            showAllResults = context.showAllCalendarResults,
            showExpandControls = context.showCalendarExpandControls,
            onExpandClick = context.calendarExpandClick,
            expandedCardMaxHeight = calendarParams.expandedCardMaxHeight,
            permissionDisabledCard = calendarParams.permissionDisabledCard,
            showWallpaperBackground = calendarParams.showWallpaperBackground,
            predictedTarget = calendarParams.predictedTarget,
            fillExpandedHeight = context.isSectionAliasMode,
            isHomeScreenMode = context.isHomeScreenCalendarMode,
        )
    }
}

@Composable
private fun renderNotesSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val notesParams = params.notesParams ?: return
    if (context.shouldRenderNotes) {
        NotesResultsSection(
            notes = context.notesList,
            pinnedNoteIds = notesParams.pinnedNoteIds,
            onNoteClick = notesParams.onNoteClick,
            onTogglePin = notesParams.onTogglePin,
            onDelete = notesParams.onDelete,
            isExpanded = context.isNotesExpanded,
            showAllResults = context.showAllNotesResults,
            showExpandControls = context.showNotesExpandControls,
            onExpandClick = context.notesExpandClick,
            expandedCardMaxHeight = notesParams.expandedCardMaxHeight,
            showWallpaperBackground = notesParams.showWallpaperBackground,
            predictedTarget = notesParams.predictedTarget,
            fillExpandedHeight = context.isSectionAliasMode,
        )
    }
}
