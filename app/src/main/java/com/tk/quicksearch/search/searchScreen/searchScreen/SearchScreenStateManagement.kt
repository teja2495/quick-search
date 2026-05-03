package com.tk.quicksearch.search.searchScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.dialogs.TriggerDialogState
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.searchScreen.buildSectionParams
import com.tk.quicksearch.search.searchScreen.rememberDerivedState
import com.tk.quicksearch.search.searchScreen.DerivedState
import com.tk.quicksearch.search.searchScreen.SectionParams
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.OneHandedModeScrollBehavior
import com.tk.quicksearch.search.searchScreen.ScrollBasedKeyboardBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private fun SearchSection.toExpandedSectionOrNone(): ExpandedSection =
    when (this) {
        SearchSection.APP_SHORTCUTS -> ExpandedSection.APP_SHORTCUTS
        SearchSection.CONTACTS -> ExpandedSection.CONTACTS
        SearchSection.FILES -> ExpandedSection.FILES
        SearchSection.SETTINGS -> ExpandedSection.SETTINGS
        SearchSection.NOTES -> ExpandedSection.NOTES
        SearchSection.APP_SETTINGS -> ExpandedSection.APP_SETTINGS
        SearchSection.CALENDAR -> ExpandedSection.CALENDAR
        SearchSection.APPS -> ExpandedSection.NONE
    }

@Composable
internal fun SearchScreenStateManagement(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onDismissContactMethods: () -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    onFileClick: (DeviceFile) -> Unit,
    onOpenFolder: (DeviceFile) -> Unit,
    onPinContact: (ContactInfo) -> Unit,
    onUnpinContact: (ContactInfo) -> Unit,
    onExcludeContact: (ContactInfo) -> Unit,
    onCalendarEventClick: (CalendarEventInfo) -> Unit,
    onPinCalendarEvent: (CalendarEventInfo) -> Unit,
    onUnpinCalendarEvent: (CalendarEventInfo) -> Unit,
    onExcludeCalendarEvent: (CalendarEventInfo) -> Unit,
    onIncludeCalendarEvent: (CalendarEventInfo) -> Unit,
    onArchiveTodayCalendarEvent: (CalendarEventInfo) -> Unit,
    onNoteClick: (NoteInfo) -> Unit,
    onPinNote: (NoteInfo) -> Unit,
    onUnpinNote: (NoteInfo) -> Unit,
    onDeleteNote: (NoteInfo) -> Unit,
    onPinFile: (DeviceFile) -> Unit,
    onUnpinFile: (DeviceFile) -> Unit,
    onExcludeFile: (DeviceFile) -> Unit,
    onExcludeFileExtension: (DeviceFile) -> Unit,
    onSettingClick: (DeviceSetting) -> Unit,
    onAppSettingClick: (AppSettingResult) -> Unit,
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    onAppSettingWebSuggestionsCountChange: (Int) -> Unit,
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    appSettingWebSuggestionsCount: Int,
    appSettingPhoneAppGridColumns: Int,
    onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit,
    onPinSetting: (DeviceSetting) -> Unit,
    onUnpinSetting: (DeviceSetting) -> Unit,
    onExcludeSetting: (DeviceSetting) -> Unit,
    onAppShortcutClick: (StaticShortcut) -> Unit,
    onPinAppShortcut: (StaticShortcut) -> Unit,
    onUnpinAppShortcut: (StaticShortcut) -> Unit,
    onExcludeAppShortcut: (StaticShortcut) -> Unit,
    onIncludeAppShortcut: (StaticShortcut) -> Unit,
    onAppShortcutAppInfoClick: (StaticShortcut) -> Unit,
    onSearchTargetClick: (String, com.tk.quicksearch.search.core.SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    onAiSearchEmailClick: (String) -> Unit,
    onSetPersonalContext: (String?) -> Unit,
    onSetGeminiModel: (String?) -> Unit,
    onSetGeminiGroundingEnabled: (Boolean) -> Unit,
    onRefreshAvailableGeminiModels: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onAppNicknameClick: (AppInfo) -> Unit,
    onClearDetectedShortcut: () -> Unit,
    onContactNicknameClick: (ContactInfo) -> Unit,
    onFileNicknameClick: (DeviceFile) -> Unit,
    getAppNickname: (String) -> String?,
    getContactNickname: (Long) -> String?,
    getFileNickname: (String) -> String?,
    getAppShortcutNickname: (String) -> String?,
    getCalendarEventNickname: (Long) -> String?,
    getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getAppShortcutTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getSettingTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    getNoteTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    onSaveAppNickname: (AppInfo, String?) -> Unit,
    onSaveAppShortcutNickname: (StaticShortcut, String?) -> Unit,
    onSaveContactNickname: (ContactInfo, String?) -> Unit,
    onSaveFileNickname: (DeviceFile, String?) -> Unit,
    getSettingNickname: (String) -> String?,
    onSaveSettingNickname: (DeviceSetting, String?) -> Unit,
    getLastShownPhoneNumber: (Long) -> String?,
    setLastShownPhoneNumber: (Long, String) -> Unit,
    onDirectDialChoiceSelected: (com.tk.quicksearch.search.core.DirectDialOption, Boolean) -> Unit,
    onDismissDirectDialChoice: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onWebSuggestionClick: (String) -> Unit,
    onSearchEngineOnboardingDismissed: () -> Unit,
    onContactActionHintDismissed: () -> Unit,
    onCustomizeSearchEnginesClick: () -> Unit,
    onOpenAiSearchConfigure: () -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    onOpenSearchHistorySettings: () -> Unit,
    onDismissSearchHistoryTip: () -> Unit,
    onWelcomeAnimationCompleted: (() -> Unit)?,
    onWallpaperLoaded: (() -> Unit)?,
    onCustomAction: (ContactInfo, ContactCardAction) -> Unit,
    getPrimaryContactCardAction: (Long) -> ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> ContactCardAction?,
    onSavePrimaryContactCardAction: (Long, ContactCardAction) -> Unit,
    onSaveSecondaryContactCardAction: (Long, ContactCardAction) -> Unit,
    isOverlayPresentation: Boolean,
    onOverlayExpandRequest: (() -> Unit)?,
    isOverlayExpanded: Boolean,
    onOverlayNumberKeyboardUiChanged: ((Boolean, Boolean) -> Unit)?,
    onOverlayScrollableContentChanged: ((Boolean) -> Unit)?,
    onConsumeContactActionRequest: () -> Unit,
): SearchScreenStateResult {
    val keyboardController = LocalSoftwareKeyboardController.current

    val derivedState = rememberDerivedState(state)

    fun getDefaultContactAction(
        contact: ContactInfo,
        isPrimary: Boolean,
    ): ContactCardAction? {
        val currentAction =
            if (isPrimary) {
                getPrimaryContactCardAction(contact.contactId)
            } else {
                getSecondaryContactCardAction(contact.contactId)
            }
        if (currentAction != null) return currentAction

        val phoneNumber = contact.phoneNumbers.firstOrNull() ?: return null
        return if (isPrimary) {
            when (
                ContactCallingAppResolver.resolveCallingAppForContact(
                    contactInfo = contact,
                    defaultApp = state.callingApp,
                )
            ) {
                CallingApp.CALL -> ContactCardAction.Phone(phoneNumber)
                CallingApp.WHATSAPP -> ContactCardAction.WhatsAppCall(phoneNumber)
                CallingApp.TELEGRAM -> ContactCardAction.TelegramCall(phoneNumber)
                CallingApp.SIGNAL -> ContactCardAction.SignalCall(phoneNumber)
                CallingApp.GOOGLE_MEET -> ContactCardAction.GoogleMeet(phoneNumber)
            }
        } else {
            when (
                ContactMessagingAppResolver.resolveMessagingAppForContact(
                    contactInfo = contact,
                    defaultApp = state.messagingApp,
                )
            ) {
                MessagingApp.MESSAGES -> ContactCardAction.Sms(phoneNumber)
                MessagingApp.WHATSAPP -> ContactCardAction.WhatsAppMessage(phoneNumber)
                MessagingApp.TELEGRAM -> ContactCardAction.TelegramMessage(phoneNumber)
                MessagingApp.SIGNAL -> ContactCardAction.SignalMessage(phoneNumber)
            }
        }
    }

    // Section expansion state
    var expandedSection by remember { mutableStateOf(ExpandedSection.NONE) }
    val scrollState = rememberScrollState()
    val showAiSearch = state.AiSearchState.status != AiSearchStatus.Idle
    val alignResultsToBottom =
        state.oneHandedMode && expandedSection == ExpandedSection.NONE && !showAiSearch

    // Nickname dialog state
    var nicknameDialogState by remember { mutableStateOf<NicknameDialogState?>(null) }
    var triggerDialogState by remember { mutableStateOf<TriggerDialogState?>(null) }

    var shortcutToEdit by remember { mutableStateOf<StaticShortcut?>(null) }
    var shortcutIconEdit by remember { mutableStateOf<StaticShortcut?>(null) }

    // Contact Action Picker state
    var contactActionPickerDialogState by remember {
        mutableStateOf<ContactActionPickerDialogState?>(null)
    }
    val contactActionRequest = state.contactActionPickerRequest
    LaunchedEffect(contactActionRequest) {
        contactActionRequest?.let { request ->
            contactActionPickerDialogState =
                ContactActionPickerDialogState(
                    contact = request.contactInfo,
                    isPrimary = request.isPrimary,
                    currentAction = request.currentAction,
                )
            onConsumeContactActionRequest()
        }
    }

    // Keyboard switching state
    var manuallySwitchedToNumberKeyboard by remember { mutableStateOf(false) }

    var showPersonalContextDialog by remember { mutableStateOf(false) }
    var showGeminiModelDialog by remember { mutableStateOf(false) }
    var personalContextInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = state.personalContext,
                selection = TextRange(state.personalContext.length),
            ),
        )
    }
    val personalContextFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showPersonalContextDialog) {
        if (showPersonalContextDialog) {
            delay(100)
            personalContextInput =
                personalContextInput.copy(
                    selection = TextRange(personalContextInput.text.length),
                )
            personalContextFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Keep alias-triggered searches expanded by default.
    LaunchedEffect(state.query, state.detectedAliasSearchSection) {
        val aliasSection = state.detectedAliasSearchSection
        expandedSection =
            if (aliasSection != null) {
                aliasSection.toExpandedSectionOrNone()
            } else {
                ExpandedSection.NONE
            }
    }

    LaunchedEffect(isOverlayPresentation, onOverlayScrollableContentChanged, scrollState) {
        if (!isOverlayPresentation) return@LaunchedEffect

        onOverlayScrollableContentChanged?.invoke(scrollState.maxValue > 0)
        snapshotFlow { scrollState.maxValue > 0 }
            .distinctUntilChanged()
            .collect { isScrollable ->
                onOverlayScrollableContentChanged?.invoke(isScrollable)
            }
    }

    val openPersonalContextDialog = {
        personalContextInput =
            TextFieldValue(
                text = state.personalContext,
                selection = TextRange(state.personalContext.length),
            )
        showPersonalContextDialog = true
    }

    // Handle back button when section is expanded
    BackHandler(
        enabled =
            expandedSection != ExpandedSection.NONE && state.detectedAliasSearchSection == null,
    ) {
        keyboardController?.show()
        expandedSection = ExpandedSection.NONE
    }

    // Handle scroll behavior for one-handed mode
    com.tk.quicksearch.search.searchScreen.OneHandedModeScrollBehavior(
        scrollState = scrollState,
        expandedSection = expandedSection,
        oneHandedMode = state.oneHandedMode,
        query = state.query,
        displayAppsSize = derivedState.displayApps.size,
        contactResultsSize = state.contactResults.size,
        appShortcutResultsSize = state.appShortcutResults.size,
        fileResultsSize = state.fileResults.size,
        pinnedContactsSize = state.pinnedContacts.size,
        pinnedAppShortcutsSize = state.pinnedAppShortcuts.size,
        pinnedFilesSize = state.pinnedFiles.size,
        settingResultsSize = state.settingResults.size + state.appSettingResults.size,
        pinnedSettingsSize = state.pinnedSettings.size,
        hasUsagePermission = state.hasUsagePermission,
        errorMessage = state.errorMessage,
        reverseScrolling = alignResultsToBottom,
    )

    // Handle keyboard visibility based on scroll position when overlay mode is off
    com.tk.quicksearch.search.searchScreen.ScrollBasedKeyboardBehavior(
        scrollState = scrollState,
        overlayModeEnabled = state.overlayModeEnabled,
        oneHandedMode = state.oneHandedMode,
        reverseScrolling = alignResultsToBottom,
    )

    val (imageBitmap, useImageBackground, useMonoThemeFallback) = SearchScreenWallpaperLogic(
        state = state,
        onWallpaperLoaded = onWallpaperLoaded,
        isOverlayPresentation = isOverlayPresentation,
    )

    val effectiveStateForCards =
        state.copy(
            showWallpaperBackground = useImageBackground,
            backgroundSource =
                if (useMonoThemeFallback) {
                    BackgroundSource.THEME
                } else {
                    state.backgroundSource
                },
            appTheme =
                if (useMonoThemeFallback) {
                    AppTheme.MONOCHROME
                } else {
                    state.appTheme
                },
        )

    val sectionParams =
        buildSectionParams(
            state = effectiveStateForCards,
            derivedState = derivedState,
            isOverlayPresentation = isOverlayPresentation,
            onFileClick = onFileClick,
            onOpenFolder = onOpenFolder,
            onPinFile = onPinFile,
            onUnpinFile = onUnpinFile,
            onExcludeFile = onExcludeFile,
            onExcludeFileExtension = onExcludeFileExtension,
            onOpenStorageAccessSettings = onOpenStorageAccessSettings,
            onSettingClick = onSettingClick,
            onAppSettingClick = onAppSettingClick,
            onAppSettingToggle = onAppSettingToggle,
            onAppSettingWebSuggestionsCountChange = onAppSettingWebSuggestionsCountChange,
            isAppSettingToggleChecked = isAppSettingToggleChecked,
            appSettingWebSuggestionsCount = appSettingWebSuggestionsCount,
            appSettingPhoneAppGridColumns = appSettingPhoneAppGridColumns,
            onAppSettingPhoneAppGridColumnsChange = onAppSettingPhoneAppGridColumnsChange,
            onPinSetting = onPinSetting,
            onUnpinSetting = onUnpinSetting,
            onExcludeSetting = onExcludeSetting,
            onAppShortcutClick = onAppShortcutClick,
            onPinAppShortcut = onPinAppShortcut,
            onUnpinAppShortcut = onUnpinAppShortcut,
            onExcludeAppShortcut = onExcludeAppShortcut,
            onIncludeAppShortcut = onIncludeAppShortcut,
            onAppShortcutAppInfoClick = onAppShortcutAppInfoClick,
            onEditCustomAppShortcut = { shortcutToEdit = it },
            onEditAppShortcutIcon = { shortcutIconEdit = it },
            onContactClick = onContactClick,
            onShowContactMethods = onShowContactMethods,
            onCallContact = onCallContact,
            onSmsContact = onSmsContact,
            onContactMethodClick = onContactMethodClick,
            onPinContact = onPinContact,
            onUnpinContact = onUnpinContact,
            onExcludeContact = onExcludeContact,
            onCalendarEventClick = onCalendarEventClick,
            onPinCalendarEvent = onPinCalendarEvent,
            onUnpinCalendarEvent = onUnpinCalendarEvent,
            onExcludeCalendarEvent = onExcludeCalendarEvent,
            onIncludeCalendarEvent = onIncludeCalendarEvent,
            onArchiveTodayCalendarEvent = onArchiveTodayCalendarEvent,
            onNoteClick = onNoteClick,
            onPinNote = onPinNote,
            onUnpinNote = onUnpinNote,
            onDeleteNote = onDeleteNote,
            onOpenAppSettings = onOpenAppSettings,
            onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
            onAppClick = onAppClick,
            onAppInfoClick = onAppInfoClick,
            onUninstallClick = onUninstallClick,
            onHideApp = onHideApp,
            onPinApp = onPinApp,
            onUnpinApp = onUnpinApp,
            getFileNickname = getFileNickname,
            getContactNickname = getContactNickname,
            getSettingNickname = getSettingNickname,
            getAppNickname = getAppNickname,
            getAppShortcutNickname = getAppShortcutNickname,
            getCalendarEventNickname = getCalendarEventNickname,
            onPrimaryActionLongPress = { contact ->
                contactActionPickerDialogState =
                    ContactActionPickerDialogState(
                        contact,
                        true,
                        getDefaultContactAction(contact, true),
                    )
            },
            onSecondaryActionLongPress = { contact ->
                contactActionPickerDialogState =
                    ContactActionPickerDialogState(
                        contact,
                        false,
                        getDefaultContactAction(contact, false),
                    )
            },
            onCustomAction = onCustomAction,
            getPrimaryContactCardAction = getPrimaryContactCardAction,
            getSecondaryContactCardAction = getSecondaryContactCardAction,
            onContactActionHintDismissed = onContactActionHintDismissed,
            onUpdateNicknameDialogState = { newState -> nicknameDialogState = newState },
            onUpdateTriggerDialogState = { newState -> triggerDialogState = newState },
            getAppTrigger = getAppTrigger,
            getContactTrigger = getContactTrigger,
            getFileTrigger = getFileTrigger,
            getAppShortcutTrigger = getAppShortcutTrigger,
            getSettingTrigger = getSettingTrigger,
            getNoteTrigger = getNoteTrigger,
            onUpdateExpandedSection = { newSection: ExpandedSection ->
                expandedSection = newSection
                if (newSection == ExpandedSection.NONE) {
                    keyboardController?.show()
                } else {
                    keyboardController?.hide()
                }
            },
            expandedSection = expandedSection,
        )

    val renderingState =
        SectionRenderingState(
            isSearching = derivedState.isSearching,
            expandedSection = expandedSection,
            hasAppResults = derivedState.hasAppResults,
            hasAppShortcutResults = derivedState.hasAppShortcutResults,
            hasContactResults = derivedState.hasContactResults,
            hasFileResults = derivedState.hasFileResults,
            hasSettingResults = derivedState.hasSettingResults,
            hasAppSettingResults = derivedState.hasAppSettingResults,
            hasCalendarResults = derivedState.hasCalendarResults,
            hasNoteResults = derivedState.hasNoteResults,
            hasPinnedAppShortcuts = derivedState.hasPinnedAppShortcuts,
            hasPinnedContacts = derivedState.hasPinnedContacts,
            hasPinnedFiles = derivedState.hasPinnedFiles,
            hasPinnedSettings = derivedState.hasPinnedSettings,
            hasPinnedCalendarEvents = derivedState.hasPinnedCalendarEvents,
            hasPinnedNotes = derivedState.hasPinnedNotes,
            shouldShowApps = derivedState.shouldShowApps,
            shouldShowAppShortcuts = derivedState.shouldShowAppShortcuts,
            shouldShowContacts = derivedState.shouldShowContacts,
            shouldShowFiles = derivedState.shouldShowFiles,
            shouldShowSettings = derivedState.shouldShowSettings,
            shouldShowCalendar = derivedState.shouldShowCalendar,
            shouldShowNotes = derivedState.shouldShowNotes,
            hasMultipleExpandableSections = derivedState.hasMultipleExpandableSections,
            displayApps = derivedState.displayApps,
            appShortcutResults = state.appShortcutResults,
            contactResults = state.contactResults,
            fileResults = state.fileResults,
            settingResults = state.settingResults,
            appSettingResults = state.appSettingResults,
            calendarEvents = state.calendarEvents,
            noteResults = state.noteResults,
            pinnedAppShortcuts = state.pinnedAppShortcuts,
            pinnedContacts = state.pinnedContacts,
            pinnedFiles = state.pinnedFiles,
            pinnedSettings = state.pinnedSettings,
            pinnedCalendarEvents = state.pinnedCalendarEvents,
            pinnedNotes = state.pinnedNotes,
            orderedSections = derivedState.orderedSections,
            shortcutDetected =
                state.detectedShortcutTarget != null ||
                    state.detectedAliasSearchSection != null ||
                    state.isCurrencyConverterAliasMode ||
                    state.isWordClockAliasMode ||
                    state.isDictionaryAliasMode,
        )

    return SearchScreenStateResult(
        derivedState = derivedState,
        expandedSection = expandedSection,
        scrollState = scrollState,
        showAiSearch = showAiSearch,
        alignResultsToBottom = alignResultsToBottom,
        shortcutToEdit = shortcutToEdit,
        shortcutIconEdit = shortcutIconEdit,
        setShortcutToEdit = { shortcutToEdit = it },
        setShortcutIconEdit = { shortcutIconEdit = it },
        nicknameDialogState = nicknameDialogState,
        triggerDialogState = triggerDialogState,
        contactActionPickerDialogState = contactActionPickerDialogState,
        manuallySwitchedToNumberKeyboard = manuallySwitchedToNumberKeyboard,
        showPersonalContextDialog = showPersonalContextDialog,
        showGeminiModelDialog = showGeminiModelDialog,
        personalContextInput = personalContextInput,
        openPersonalContextDialog = openPersonalContextDialog,
        imageBitmap = imageBitmap,
        useImageBackground = useImageBackground,
        useMonoThemeFallback = useMonoThemeFallback,
        effectiveStateForCards = effectiveStateForCards,
        sectionParams = sectionParams,
        renderingState = renderingState,
        setExpandedSection = { expandedSection = it },
        setNicknameDialogState = { nicknameDialogState = it },
        setTriggerDialogState = { triggerDialogState = it },
        setContactActionPickerDialogState = { contactActionPickerDialogState = it },
        setManuallySwitchedToNumberKeyboard = { manuallySwitchedToNumberKeyboard = it },
        setShowPersonalContextDialog = { showPersonalContextDialog = it },
        setShowGeminiModelDialog = { showGeminiModelDialog = it },
        setPersonalContextInput = { personalContextInput = it },
    )
}

internal data class SearchScreenStateResult(
    val derivedState: DerivedState,
    val expandedSection: ExpandedSection,
    val scrollState: androidx.compose.foundation.ScrollState,
    val showAiSearch: Boolean,
    val alignResultsToBottom: Boolean,
    val shortcutToEdit: StaticShortcut?,
    val shortcutIconEdit: StaticShortcut?,
    val setShortcutToEdit: (StaticShortcut?) -> Unit,
    val setShortcutIconEdit: (StaticShortcut?) -> Unit,
    val nicknameDialogState: NicknameDialogState?,
    val triggerDialogState: TriggerDialogState?,
    val contactActionPickerDialogState: ContactActionPickerDialogState?,
    val manuallySwitchedToNumberKeyboard: Boolean,
    val showPersonalContextDialog: Boolean,
    val showGeminiModelDialog: Boolean,
    val personalContextInput: TextFieldValue,
    val openPersonalContextDialog: () -> Unit,
    val imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    val useImageBackground: Boolean,
    val useMonoThemeFallback: Boolean,
    val effectiveStateForCards: SearchUiState,
    val sectionParams: SectionParams,
    val renderingState: SectionRenderingState,
    val setExpandedSection: (ExpandedSection) -> Unit,
    val setNicknameDialogState: (NicknameDialogState?) -> Unit,
    val setTriggerDialogState: (TriggerDialogState?) -> Unit,
    val setContactActionPickerDialogState: (ContactActionPickerDialogState?) -> Unit,
    val setManuallySwitchedToNumberKeyboard: (Boolean) -> Unit,
    val setShowPersonalContextDialog: (Boolean) -> Unit,
    val setShowGeminiModelDialog: (Boolean) -> Unit,
    val setPersonalContextInput: (TextFieldValue) -> Unit,
)
