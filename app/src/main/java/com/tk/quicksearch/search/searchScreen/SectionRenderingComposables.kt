package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.appSettings.AppSettingsResultsSection
import com.tk.quicksearch.search.appShortcuts.AppShortcutResultsSection
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.calendar.CalendarEventsSection
import com.tk.quicksearch.search.contacts.ContactResultsSection
import com.tk.quicksearch.search.core.AppSuggestionTabType
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
            onMovePinned = filesParams.onMovePinned,
            onExclude = filesParams.onExclude,
            onExcludeExtension = filesParams.onExcludeExtension,
            onNicknameClick = filesParams.onNicknameClick,
            onTriggerClick = filesParams.onTriggerClick,
            getFileNickname = filesParams.getFileNickname,
            getFileTrigger = filesParams.getFileTrigger,
            showAllResults = filesParams.showAllResults,
            showExpandControls = filesParams.showExpandControls,
            onExpandClick = filesParams.onExpandClick,
            expandedCardMaxHeight = filesParams.expandedCardMaxHeight,
            permissionDisabledCard = filesParams.permissionDisabledCard,
            showWallpaperBackground = filesParams.showWallpaperBackground,
            predictedTarget = filesParams.predictedTarget,
            fillExpandedHeight = false,
            showPinnedItemMenu = context.showAllFilesResults,
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
            onMovePinned = contactsParams.onMovePinned,
            onExclude = contactsParams.onExclude,
            onNicknameClick = contactsParams.onNicknameClick,
            onTriggerClick = contactsParams.onTriggerClick,
            getContactNickname = contactsParams.getContactNickname,
            getContactTrigger = contactsParams.getContactTrigger,
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
            fillExpandedHeight = false,
            showPinnedItemMenu = context.showAllContactsResults,
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
    val hasAllAppsSuggestionContent =
        !appsParams.isSearching &&
            AppSuggestionTabType.ALL_APPS in appsParams.enabledSuggestionTabs &&
            appsParams.allApps.isNotEmpty()

    if (context.shouldRenderApps && ((appsParams.hasAppResults && appsParams.apps.isNotEmpty()) || hasAllAppsSuggestionContent)) {
        val shouldShowRateQuickSearchCard = appsParams.showRateQuickSearchCard
        val renderRateQuickSearchCardFirst = shouldShowRateQuickSearchCard && appsParams.oneHandedMode

        if (renderRateQuickSearchCardFirst) {
            RateQuickSearchCard(
                showWallpaperBackground = appsParams.showWallpaperBackground,
                onClick = appsParams.onRateQuickSearchClick,
                onNotNowClick = appsParams.onRateQuickSearchNotNowClick,
            )
        }
        AppGridView(
            apps = appsParams.apps,
            allApps = appsParams.allApps,
            pinnedAndRecentApps = appsParams.pinnedAndRecentApps,
            pinnedApps = appsParams.pinnedApps,
            newOrUpdatedApps = appsParams.newOrUpdatedApps,
            mostUsedApps = appsParams.mostUsedApps,
            appShortcuts = appsParams.appShortcuts,
            isSearching = appsParams.isSearching,
            hasUsagePermission = appsParams.hasUsagePermission,
            selectedSuggestionTab = appsParams.selectedSuggestionTab,
            enabledSuggestionTabs = appsParams.enabledSuggestionTabs,
            onSuggestionTabSelected = appsParams.onSuggestionTabSelected,
            hasAppResults = appsParams.hasAppResults,
            onAppClick = appsParams.onAppClick,
            onAppInfoClick = appsParams.onAppInfoClick,
            onUninstallClick = appsParams.onUninstallClick,
            onHideApp = appsParams.onHideApp,
            onPinApp = appsParams.onPinApp,
            onUnpinApp = appsParams.onUnpinApp,
            onReorderPinnedApps = appsParams.onReorderPinnedApps,
            onNicknameClick = appsParams.onNicknameClick,
            onTriggerClick = appsParams.onTriggerClick,
            getAppNickname = appsParams.getAppNickname,
            getAppTrigger = appsParams.getAppTrigger,
            pinnedPackageNames = appsParams.pinnedPackageNames,
            disabledShortcutIds = appsParams.disabledAppShortcutIds,
            rowCount = appsParams.rowCount,
            phoneColumnOverride = appsParams.phoneColumnOverride,
            appIconSizeStep = appsParams.appIconSizeStep,
            iconPackPackage = appsParams.iconPackPackage,
            appIconShape = appsParams.appIconShape,
            themedIconsEnabled = appsParams.themedIconsEnabled,
            showAppLabels = appsParams.showAppLabels,
            oneHandedMode = appsParams.oneHandedMode,
            isInitializing = appsParams.isInitializing,
            startupPhase = appsParams.startupPhase,
            isOverlayPresentation = appsParams.isOverlayPresentation,
            predictedTarget = appsParams.predictedTarget,
            suppressTopResultIndicator = appsParams.suppressTopResultIndicator,
            showWallpaperBackground = appsParams.showWallpaperBackground,
            onGridAppeared = appsParams.onGridAppeared,
            suppressSuggestionsEnterAnimation = appsParams.suppressSuggestionsEnterAnimation,
        )
        if (shouldShowRateQuickSearchCard && !renderRateQuickSearchCardFirst) {
            RateQuickSearchCard(
                showWallpaperBackground = appsParams.showWallpaperBackground,
                onClick = appsParams.onRateQuickSearchClick,
                onNotNowClick = appsParams.onRateQuickSearchNotNowClick,
            )
        }
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
            onMovePinned = appShortcutsParams.onMovePinned,
            onExclude = appShortcutsParams.onExclude,
            onInclude = appShortcutsParams.onInclude,
            onAppInfoClick = appShortcutsParams.onAppInfoClick,
            onNicknameClick = appShortcutsParams.onNicknameClick,
            onTriggerClick = appShortcutsParams.onTriggerClick,
            onEditCustomShortcut = appShortcutsParams.onEditCustomShortcut,
            onEditShortcutIcon = appShortcutsParams.onEditShortcutIcon,
            getShortcutNickname = appShortcutsParams.getShortcutNickname,
            getShortcutTrigger = appShortcutsParams.getShortcutTrigger,
            showAllResults = context.showAllAppShortcutsResults,
            showExpandControls = context.showAppShortcutsExpandControls,
            onExpandClick = context.appShortcutsExpandClick,
            expandedCardMaxHeight = appShortcutsParams.expandedCardMaxHeight,
            iconPackPackage = appShortcutsParams.iconPackPackage,
            showWallpaperBackground = appShortcutsParams.showWallpaperBackground,
            predictedTarget = appShortcutsParams.predictedTarget,
            fillExpandedHeight = false,
            showPinnedItemMenu = context.showAllAppShortcutsResults,
        )
    }
}

/** Renders the device settings section if it should be displayed. */
@Composable
private fun renderSettingsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val settingsParams = params.settingsParams ?: return
    val shouldShowSettings =
        context.shouldRenderSettings &&
            !context.isAppSettingsExpanded

    if (shouldShowSettings && context.settingsList.isNotEmpty()) {
        DeviceSettingsResultsSection(
            settings = context.settingsList,
            isExpanded = context.isDeviceSettingsExpanded,
            pinnedSettingIds = settingsParams.pinnedSettingIds,
            onSettingClick = settingsParams.onSettingClick,
            onTogglePin = settingsParams.onTogglePin,
            onMovePinned = settingsParams.onMovePinned,
            onExclude = settingsParams.onExclude,
            onNicknameClick = settingsParams.onNicknameClick,
            onTriggerClick = settingsParams.onTriggerClick,
            getSettingNickname = settingsParams.getSettingNickname,
            getSettingTrigger = settingsParams.getSettingTrigger,
            showAllResults = context.showAllSettingsResults,
            showExpandControls = context.showDeviceSettingsExpandControls,
            onExpandClick = context.deviceSettingsExpandClick,
            expandedCardMaxHeight = settingsParams.expandedCardMaxHeight,
            showWallpaperBackground = settingsParams.showWallpaperBackground,
            predictedTarget = settingsParams.predictedTarget,
            fillExpandedHeight = false,
            showPinnedItemMenu = context.showAllSettingsResults,
        )
    }
}

/** Renders the app settings section if it should be displayed. */
@Composable
private fun renderAppSettingsSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val settingsParams = params.settingsParams ?: return
    val shouldShowAppSettings =
        context.shouldRenderAppSettings &&
            !context.isDeviceSettingsExpanded

    if (shouldShowAppSettings && context.appSettingsList.isNotEmpty()) {
        AppSettingsResultsSection(
            appSettings = context.appSettingsList,
            isExpanded = context.isAppSettingsExpanded,
            onAppSettingClick = settingsParams.onAppSettingClick,
            onAppSettingToggle = settingsParams.onAppSettingToggle,
            onWebSuggestionsCountChange =
                settingsParams.onAppSettingWebSuggestionsCountChange,
            isAppSettingToggleChecked = settingsParams.isAppSettingToggleChecked,
            webSuggestionsCount = settingsParams.appSettingWebSuggestionsCount,
            appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
            onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
            showAllResults = context.showAllSettingsResults,
            showExpandControls = context.showAppSettingsExpandControls,
            onExpandClick = context.appSettingsExpandClick,
            expandedCardMaxHeight = settingsParams.expandedCardMaxHeight,
            showWallpaperBackground = settingsParams.showWallpaperBackground,
            predictedTarget = settingsParams.predictedTarget,
            fillExpandedHeight = false,
        )
    }
}

@Composable
private fun renderCalendarSection(
    params: SectionRenderParams,
    context: SectionRenderContext,
) {
    val calendarParams = params.calendarParams ?: return
    if (context.shouldRenderCalendar || context.todayCalendarEventsList.isNotEmpty()) {
        if (context.isHomeScreenCalendarMode && context.todayCalendarEventsList.isNotEmpty()) {
            CalendarEventsSection(
                events = context.todayCalendarEventsList,
                hasPermission = calendarParams.hasPermission,
                isExpanded = true,
                pinnedEventIds = emptySet(),
                excludedEventIds = calendarParams.excludedEventIds,
                onEventClick = calendarParams.onEventClick,
                onRequestPermission = calendarParams.onRequestPermission,
                onTogglePin = calendarParams.onTogglePin,
                onMovePinned = calendarParams.onMovePinned,
                onExclude = calendarParams.onExclude,
                onInclude = calendarParams.onInclude,
                onNicknameClick = calendarParams.onNicknameClick,
                onArchiveTodayEvent = calendarParams.onArchiveTodayEvent,
                getEventNickname = calendarParams.getEventNickname,
                showAllResults = true,
                showExpandControls = false,
                onExpandClick = context.calendarExpandClick,
                expandedCardMaxHeight = calendarParams.expandedCardMaxHeight,
                permissionDisabledCard = calendarParams.permissionDisabledCard,
                showWallpaperBackground = calendarParams.showWallpaperBackground,
                predictedTarget = calendarParams.predictedTarget,
                fillExpandedHeight = false,
                isHomeScreenMode = true,
                showPinnedItemMenu = false,
            )
        }
        if (context.calendarEventsList.isEmpty() && context.isHomeScreenCalendarMode) return
        CalendarEventsSection(
            events = context.calendarEventsList,
            hasPermission = calendarParams.hasPermission,
            isExpanded = context.isCalendarExpanded,
            pinnedEventIds = calendarParams.pinnedEventIds,
            excludedEventIds = calendarParams.excludedEventIds,
            onEventClick = calendarParams.onEventClick,
            onRequestPermission = calendarParams.onRequestPermission,
            onTogglePin = calendarParams.onTogglePin,
            onMovePinned = calendarParams.onMovePinned,
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
            fillExpandedHeight = false,
            isHomeScreenMode = false,
            showPinnedItemMenu = context.showAllCalendarResults,
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
            onMovePinned = notesParams.onMovePinned,
            onDelete = notesParams.onDelete,
            onTriggerClick = notesParams.onTriggerClick,
            getNoteTrigger = notesParams.getNoteTrigger,
            isExpanded = context.isNotesExpanded,
            showAllResults = context.showAllNotesResults,
            showExpandControls = context.showNotesExpandControls,
            onExpandClick = context.notesExpandClick,
            expandedCardMaxHeight = notesParams.expandedCardMaxHeight,
            showWallpaperBackground = notesParams.showWallpaperBackground,
            predictedTarget = notesParams.predictedTarget,
            fillExpandedHeight = false,
            showPinnedItemMenu = context.showAllNotesResults,
        )
    }
}
