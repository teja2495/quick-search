package com.tk.quicksearch.search.searchScreen

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
// import com.tk.quicksearch.search.searchScreen.SearchEngineOnboardingOverlay
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.search.searchScreen.SearchScreenContent
import com.tk.quicksearch.search.searchScreen.SectionParams
import com.tk.quicksearch.search.searchScreen.DerivedState

// Import the extracted components
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.search.searchScreen.SearchScreenStateManagement
import com.tk.quicksearch.search.searchScreen.SearchScreenDialogLogic
import com.tk.quicksearch.shared.ui.theme.ThemeModeFallbackBackgroundAlpha
import com.tk.quicksearch.shared.util.ImageAppearanceUtils

private const val STARTUP_BACKGROUND_TRANSITION_DURATION_MS = 90

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSelectRetainedQueryHandled: () -> Unit,
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
    onSearchTargetClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    onDirectSearchEmailClick: (String) -> Unit,
    onSetPersonalContext: (String?) -> Unit = {},
    onSetGeminiModel: (String?) -> Unit = {},
    onSetGeminiGroundingEnabled: (Boolean) -> Unit = {},
    onRefreshAvailableGeminiModels: () -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onWallpaperLoaded: (() -> Unit)? = null,
    isOverlayPresentation: Boolean = false,
    onOpenAppSettings: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    onOpenStorageAccessSettings: () -> Unit,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismissPhoneNumberSelection: () -> Unit,
    onAppNicknameClick: (AppInfo) -> Unit,
    onContactNicknameClick: (ContactInfo) -> Unit,
    onFileNicknameClick: (DeviceFile) -> Unit,
    getAppNickname: (String) -> String?,
    getContactNickname: (Long) -> String?,
    getFileNickname: (String) -> String?,
    getAppShortcutNickname: (String) -> String?,
    getCalendarEventNickname: (Long) -> String?,
    onSaveAppNickname: (AppInfo, String?) -> Unit,
    onSaveAppShortcutNickname: (StaticShortcut, String?) -> Unit,
    onSaveContactNickname: (ContactInfo, String?) -> Unit,
    onSaveFileNickname: (DeviceFile, String?) -> Unit,
    onSaveCalendarEventNickname: (CalendarEventInfo, String?) -> Unit,
    getSettingNickname: (String) -> String?,
    onSaveSettingNickname: (DeviceSetting, String?) -> Unit,
    getAppShortcutIconOverride: (String) -> String?,
    onUpdateCustomAppShortcut: (StaticShortcut, String, String?, String?) -> Unit,
    onDeleteCustomAppShortcut: (StaticShortcut) -> Unit,
    onSetAppShortcutIconOverride: (StaticShortcut, String?) -> Unit,
    getLastShownPhoneNumber: (Long) -> String?,
    setLastShownPhoneNumber: (Long, String) -> Unit,
    onDirectDialChoiceSelected: (DirectDialOption, Boolean) -> Unit,
    onDismissDirectDialChoice: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onReleaseNotesViewAllFeatures: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onSearchEngineOnboardingDismissed: () -> Unit = {},
    onContactActionHintDismissed: () -> Unit = {},
    onClearDetectedShortcut: () -> Unit = {},
    onSectionSelected: (com.tk.quicksearch.search.core.SearchSection) -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onConsumeContactActionRequest: () -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onOpenSearchHistorySettings: () -> Unit = {},
    onDismissSearchHistoryTip: () -> Unit = {},
    onCurrencyConversionClick: () -> Unit = {},
    onDictionarySearchClick: () -> Unit = {},
    onWordClockSearchClick: () -> Unit = {},
    onCustomAction: (ContactInfo, ContactCardAction) -> Unit,
    getPrimaryContactCardAction: (Long) -> ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> ContactCardAction?,
    onSavePrimaryContactCardAction: (Long, ContactCardAction) -> Unit,
    onSaveSecondaryContactCardAction: (Long, ContactCardAction) -> Unit,
    onOverlayExpandRequest: (() -> Unit)? = null,
    isOverlayExpanded: Boolean = false,
    onOverlayNumberKeyboardUiChanged: ((Boolean, Boolean) -> Unit)? = null,
    onOverlayScrollableContentChanged: ((Boolean) -> Unit)? = null,
    onOpenPermissionsSettings: () -> Unit = {},
) {
    val directAnswerContactName = stringResource(R.string.direct_answer_contact_name)

    val stateResult = SearchScreenStateManagement(
        state = state,
        onQueryChanged = onQueryChanged,
        onClearQuery = onClearQuery,
        onSettingsClick = onSettingsClick,
        onRequestUsagePermission = onRequestUsagePermission,
        onAppClick = onAppClick,
        onAppInfoClick = onAppInfoClick,
        onUninstallClick = onUninstallClick,
        onHideApp = onHideApp,
        onPinApp = onPinApp,
        onUnpinApp = onUnpinApp,
        onContactClick = onContactClick,
        onShowContactMethods = onShowContactMethods,
        onDismissContactMethods = onDismissContactMethods,
        onCallContact = onCallContact,
        onSmsContact = onSmsContact,
        onContactMethodClick = onContactMethodClick,
        onFileClick = onFileClick,
        onOpenFolder = onOpenFolder,
        onPinContact = onPinContact,
        onUnpinContact = onUnpinContact,
        onExcludeContact = onExcludeContact,
        onCalendarEventClick = onCalendarEventClick,
        onPinCalendarEvent = onPinCalendarEvent,
        onUnpinCalendarEvent = onUnpinCalendarEvent,
        onExcludeCalendarEvent = onExcludeCalendarEvent,
        onIncludeCalendarEvent = onIncludeCalendarEvent,
        onPinFile = onPinFile,
        onUnpinFile = onUnpinFile,
        onExcludeFile = onExcludeFile,
        onExcludeFileExtension = onExcludeFileExtension,
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
        onSearchTargetClick = onSearchTargetClick,
        onSearchEngineLongPress = onSearchEngineLongPress,
        onDirectSearchEmailClick = onDirectSearchEmailClick,
        onSetPersonalContext = onSetPersonalContext,
        onSetGeminiModel = onSetGeminiModel,
        onSetGeminiGroundingEnabled = onSetGeminiGroundingEnabled,
        onRefreshAvailableGeminiModels = onRefreshAvailableGeminiModels,
        onOpenAppSettings = onOpenAppSettings,
        onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
        onOpenStorageAccessSettings = onOpenStorageAccessSettings,
        onAppNicknameClick = onAppNicknameClick,
        onClearDetectedShortcut = onClearDetectedShortcut,
        onContactNicknameClick = onContactNicknameClick,
        onFileNicknameClick = onFileNicknameClick,
        getAppNickname = getAppNickname,
        getContactNickname = getContactNickname,
        getFileNickname = getFileNickname,
        getAppShortcutNickname = getAppShortcutNickname,
        getCalendarEventNickname = getCalendarEventNickname,
        onSaveAppNickname = onSaveAppNickname,
        onSaveAppShortcutNickname = onSaveAppShortcutNickname,
        onSaveContactNickname = onSaveContactNickname,
        onSaveFileNickname = onSaveFileNickname,
        getSettingNickname = getSettingNickname,
        onSaveSettingNickname = onSaveSettingNickname,
        getLastShownPhoneNumber = getLastShownPhoneNumber,
        setLastShownPhoneNumber = setLastShownPhoneNumber,
        onDirectDialChoiceSelected = onDirectDialChoiceSelected,
        onDismissDirectDialChoice = onDismissDirectDialChoice,
        onReleaseNotesAcknowledged = onReleaseNotesAcknowledged,
        onWebSuggestionClick = onWebSuggestionClick,
        onSearchEngineOnboardingDismissed = onSearchEngineOnboardingDismissed,
        onContactActionHintDismissed = onContactActionHintDismissed,
        onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
        onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
        onDeleteRecentItem = onDeleteRecentItem,
        onOpenSearchHistorySettings = onOpenSearchHistorySettings,
        onDismissSearchHistoryTip = onDismissSearchHistoryTip,
        onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
        onWallpaperLoaded = onWallpaperLoaded,
        onCustomAction = onCustomAction,
        getPrimaryContactCardAction = getPrimaryContactCardAction,
        getSecondaryContactCardAction = getSecondaryContactCardAction,
        onSavePrimaryContactCardAction = onSavePrimaryContactCardAction,
        onSaveSecondaryContactCardAction = onSaveSecondaryContactCardAction,
        isOverlayPresentation = isOverlayPresentation,
        onOverlayExpandRequest = onOverlayExpandRequest,
        isOverlayExpanded = isOverlayExpanded,
        onOverlayNumberKeyboardUiChanged = onOverlayNumberKeyboardUiChanged,
        onOverlayScrollableContentChanged = onOverlayScrollableContentChanged,
        onConsumeContactActionRequest = onConsumeContactActionRequest,
    )

    val screenModifier =
        if (isOverlayPresentation) {
            modifier.fillMaxWidth()
        } else {
            modifier.fillMaxSize()
        }

    val imageBackgroundIsDark =
        remember(stateResult.imageBitmap, stateResult.useImageBackground) {
            if (stateResult.useImageBackground && stateResult.imageBitmap != null) {
                ImageAppearanceUtils.fromImageBitmap(stateResult.imageBitmap)?.isDark ?: true
            } else {
                null
            }
        }

    val context = LocalContext.current
    val isSystemDarkTheme = isSystemInDarkTheme()
    val effectiveBackgroundSource =
        if (stateResult.useMonoThemeFallback) {
            BackgroundSource.THEME
        } else {
            state.backgroundSource
        }
    val effectiveAppTheme =
        if (stateResult.useMonoThemeFallback) {
            AppTheme.MONOCHROME
        } else {
            state.appTheme
        }
    val useDarkSystemBarsFromTheme =
            when (state.appThemeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemDarkTheme
            }
    val useDarkSystemBars = imageBackgroundIsDark ?: useDarkSystemBarsFromTheme
    val themeSystemBarsDarkState = rememberUpdatedState(useDarkSystemBarsFromTheme)
    SideEffect {
        val activity = context as? ComponentActivity ?: return@SideEffect
        val systemBarStyle =
                if (useDarkSystemBars) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                    )
                }
        activity.enableEdgeToEdge(
                statusBarStyle = systemBarStyle,
                navigationBarStyle = systemBarStyle,
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? ComponentActivity ?: return@onDispose
            val useDark = themeSystemBarsDarkState.value
            val systemBarStyle =
                    if (useDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                        )
                    }
            activity.enableEdgeToEdge(
                    statusBarStyle = systemBarStyle,
                    navigationBarStyle = systemBarStyle,
            )
        }
    }

    CompositionLocalProvider(LocalImageBackgroundIsDark provides imageBackgroundIsDark) {
    Box(modifier = screenModifier) {
        if (!isOverlayPresentation) {
            SearchScreenBackground(
                showWallpaperBackground = stateResult.useImageBackground,
                wallpaperBitmap = stateResult.imageBitmap,
                wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = state.wallpaperBlurRadius,
                backgroundTransitionDurationMillis =
                    if (state.isInitializing &&
                        effectiveBackgroundSource != com.tk.quicksearch.search.core.BackgroundSource.THEME
                    ) {
                        STARTUP_BACKGROUND_TRANSITION_DURATION_MS
                    } else {
                        com.tk.quicksearch.shared.ui.theme.DesignTokens.WallpaperFadeInDuration + 120
                    },
                fallbackBackgroundAlpha =
                    if (effectiveBackgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME) {
                        ThemeModeFallbackBackgroundAlpha
                    } else {
                        1f
                    },
                useGradientFallback =
                    effectiveBackgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME || stateResult.useMonoThemeFallback,
                appTheme = effectiveAppTheme,
                overlayThemeIntensity = state.overlayThemeIntensity,
            )
        }

        // Main content
        SearchScreenContent(
            modifier =
                if (isOverlayPresentation) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.fillMaxSize()
                },
            state = stateResult.effectiveStateForCards,
            renderingState = stateResult.renderingState,
            contactsParams = stateResult.sectionParams.contactsParams,
            filesParams = stateResult.sectionParams.filesParams,
            appShortcutsParams = stateResult.sectionParams.appShortcutsParams,
            settingsParams = stateResult.sectionParams.settingsParams,
            calendarParams = stateResult.sectionParams.calendarParams,
            appsParams = stateResult.sectionParams.appsParams,
            onQueryChanged = onQueryChanged,
            onSelectRetainedQueryHandled = onSelectRetainedQueryHandled,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            onAppClick = onAppClick,
            onRequestUsagePermission = onRequestUsagePermission,
            onSearchTargetClick = onSearchTargetClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            onDirectSearchEmailClick = onDirectSearchEmailClick,
            onOpenPersonalContextDialog = stateResult.openPersonalContextDialog,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onPhoneNumberClick = { phoneNumber ->
                // Create a temporary ContactInfo to use the call functionality
                val tempContact =
                    ContactInfo(
                        contactId = -1L,
                        lookupKey = "",
                        displayName = directAnswerContactName,
                        phoneNumbers = listOf(phoneNumber),
                    )
                onCallContact(tempContact)
            },
            onWebSuggestionClick = onWebSuggestionClick,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
            onDeleteRecentItem = onDeleteRecentItem,
            onOpenSearchHistorySettings = onOpenSearchHistorySettings,
            onDismissSearchHistoryTip = onDismissSearchHistoryTip,
            onGeminiModelInfoClick = { stateResult.setShowGeminiModelDialog(true) },
            onCurrencyConversionClick = onCurrencyConversionClick,
            onDictionarySearchClick = onDictionarySearchClick,
            onWordClockSearchClick = onWordClockSearchClick,
            onKeyboardSwitchToggle = {
                stateResult.setManuallySwitchedToNumberKeyboard(!stateResult.manuallySwitchedToNumberKeyboard)
            },
            onOverlayNumberKeyboardUiChanged = onOverlayNumberKeyboardUiChanged,
            onOverlayExpandRequest = { onOverlayExpandRequest?.invoke() },
            isOverlayExpanded = isOverlayExpanded,
            expandedSection = stateResult.expandedSection,
            manuallySwitchedToNumberKeyboard = stateResult.manuallySwitchedToNumberKeyboard,
            scrollState = stateResult.scrollState,
            onClearDetectedShortcut = onClearDetectedShortcut,
            onSectionSelected = onSectionSelected,
            isOverlayPresentation = isOverlayPresentation,
            showSearchField = true,
            onOpenPermissionsSettings = onOpenPermissionsSettings,
        )

    }
    } // CompositionLocalProvider(LocalImageBackgroundIsDark)

    // Dialogs
    SearchScreenDialogLogic(
        state = state,
        nicknameDialogState = stateResult.nicknameDialogState,
        contactActionPickerDialogState = stateResult.contactActionPickerDialogState,
        onPhoneNumberSelected = onPhoneNumberSelected,
        onDismissPhoneNumberSelection = onDismissPhoneNumberSelection,
        onDirectDialChoiceSelected = onDirectDialChoiceSelected,
        onDismissDirectDialChoice = onDismissDirectDialChoice,
        onContactMethodClick = onContactMethodClick,
        onDismissContactMethods = onDismissContactMethods,
        onReleaseNotesAcknowledged = onReleaseNotesAcknowledged,
        onReleaseNotesViewAllFeatures = onReleaseNotesViewAllFeatures,
        onDismissNicknameDialog = { stateResult.setNicknameDialogState(null) },
        onSaveAppNickname = { app, nickname ->
            onSaveAppNickname(app, nickname)
            stateResult.setNicknameDialogState(null)
        },
        onSaveAppShortcutNickname = { shortcut, nickname ->
            onSaveAppShortcutNickname(shortcut, nickname)
            stateResult.setNicknameDialogState(null)
        },
        onSaveContactNickname = { contact, nickname ->
            onSaveContactNickname(contact, nickname)
            stateResult.setNicknameDialogState(null)
        },
        onSaveFileNickname = { file, nickname ->
            onSaveFileNickname(file, nickname)
            stateResult.setNicknameDialogState(null)
        },
        onSaveCalendarEventNickname = { event, nickname ->
            onSaveCalendarEventNickname(event, nickname)
            stateResult.setNicknameDialogState(null)
        },
        onSaveSettingNickname = { setting, nickname ->
            onSaveSettingNickname(setting, nickname)
            stateResult.setNicknameDialogState(null)
        },
        getLastShownPhoneNumber = getLastShownPhoneNumber,
        setLastShownPhoneNumber = setLastShownPhoneNumber,
        onSetPersonalContext = onSetPersonalContext,
        onSetGeminiModel = onSetGeminiModel,
        onSetGeminiGroundingEnabled = onSetGeminiGroundingEnabled,
        onRefreshAvailableGeminiModels = onRefreshAvailableGeminiModels,
        showPersonalContextDialog = stateResult.showPersonalContextDialog,
        setShowPersonalContextDialog = stateResult.setShowPersonalContextDialog,
        showGeminiModelDialog = stateResult.showGeminiModelDialog,
        setShowGeminiModelDialog = stateResult.setShowGeminiModelDialog,
        personalContextInput = stateResult.personalContextInput,
        setPersonalContextInput = stateResult.setPersonalContextInput,
        getPrimaryContactCardAction = getPrimaryContactCardAction,
        getSecondaryContactCardAction = getSecondaryContactCardAction,
        onSavePrimaryContactCardAction = onSavePrimaryContactCardAction,
        onSaveSecondaryContactCardAction = onSaveSecondaryContactCardAction,
        onDismissContactActionPicker = { stateResult.setContactActionPickerDialogState(null) },
        shortcutToEdit = stateResult.shortcutToEdit,
        onDismissShortcutToEdit = { stateResult.setShortcutToEdit(null) },
        onUpdateCustomAppShortcut = onUpdateCustomAppShortcut,
        onDeleteCustomAppShortcut = onDeleteCustomAppShortcut,
        shortcutIconEdit = stateResult.shortcutIconEdit,
        onDismissShortcutIconEdit = { stateResult.setShortcutIconEdit(null) },
        onSetAppShortcutIconOverride = onSetAppShortcutIconOverride,
        getAppShortcutIconOverride = getAppShortcutIconOverride,
    )
}
