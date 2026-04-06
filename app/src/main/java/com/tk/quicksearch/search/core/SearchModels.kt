package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.directSearch.GeminiTextModel

// IconPackInfo moved here to avoid circular imports
data class IconPackInfo(
        val packageName: String,
        val label: String,
)

enum class SearchEngine {
        DIRECT_SEARCH,
        GOOGLE,
        CHATGPT,
        GEMINI,
        PERPLEXITY,
        GOOGLE_PLAY,
        YOUTUBE,
        GOOGLE_MAPS,
        WAZE,
        GROK,
        REDDIT,
        AMAZON,
        X,
        YOUTUBE_MUSIC,
        SPOTIFY,
        CLAUDE,
        GOOGLE_DRIVE,
        GOOGLE_PHOTOS,
        AI_MODE,
        DUCKDUCKGO,
        BRAVE,
        FACEBOOK_MARKETPLACE,
        YOU_COM,
        WIKIPEDIA,
        BING,
        STARTPAGE,
}

data class BrowserApp(
        val packageName: String,
        val label: String,
)

data class CustomSearchEngine(
        val id: String,
        val name: String,
        val urlTemplate: String,
        val faviconBase64: String? = null,
        val browserPackage: String? = null,
)

sealed class SearchTarget {
        data class Engine(
                val engine: SearchEngine,
        ) : SearchTarget()

        data class Browser(
                val app: BrowserApp,
        ) : SearchTarget()

        data class Custom(
                val custom: CustomSearchEngine,
        ) : SearchTarget()
}

enum class SearchSection {
        APPS,
        APP_SHORTCUTS,
        CONTACTS,
        FILES,
        SETTINGS,
        CALENDAR,
        NOTES,
        APP_SETTINGS,
}

enum class MessagingApp {
        MESSAGES,
        WHATSAPP,
        TELEGRAM,
        SIGNAL,
}

enum class CallingApp {
        CALL,
        GOOGLE_MEET,
        WHATSAPP,
        TELEGRAM,
        SIGNAL,
}

enum class AppTheme {
        FOREST,
        AURORA,
        SUNSET,
        MONOCHROME,
}

enum class BackgroundSource {
        THEME,
        SYSTEM_WALLPAPER,
        CUSTOM_IMAGE,
}

enum class AppThemeMode {
        LIGHT,
        DARK,
        SYSTEM,
}

enum class StartupPhase {
        PHASE_0_SHELL,
        PHASE_1_CACHE_PREFS,
        PHASE_2_HEAVY_FEATURES,
        COMPLETE,
}

enum class DirectSearchStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class CurrencyConverterStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class WordClockStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class DictionaryStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class SearchToolType {
        CALCULATOR,
        UNIT_CONVERTER,
        DATE_CALCULATOR,
}

data class DirectSearchState(
        val status: DirectSearchStatus = DirectSearchStatus.Idle,
        val answer: String? = null,
        val errorMessage: String? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
)

data class CurrencyConverterState(
        val status: CurrencyConverterStatus = CurrencyConverterStatus.Idle,
        val convertedAmount: String? = null,
        val targetCurrencyCode: String? = null,
        val targetCurrencyName: String? = null,
        val sourceAmount: String? = null,
        val sourceCurrencyCode: String? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val errorMessage: String? = null,
)

data class WordClockState(
        val status: WordClockStatus = WordClockStatus.Idle,
        val wordClockText: String? = null,
        val sourceTimeText: String? = null,
        val placeText: String? = null,
        val timeZoneText: String? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val errorMessage: String? = null,
)

data class DictionaryState(
        val status: DictionaryStatus = DictionaryStatus.Idle,
        val word: String? = null,
        val partOfSpeech: String? = null,
        val meaning: String? = null,
        val example: String? = null,
        val synonyms: List<String> = emptyList(),
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val errorMessage: String? = null,
)

data class CalculatorState(
        val result: String? = null,
        val expression: String? = null,
        val isCalculatorMode: Boolean = false,
        val isUnitConverterMode: Boolean = false,
        val isDateCalculatorMode: Boolean = false,
        val toolType: SearchToolType = SearchToolType.CALCULATOR,
        val showInvalidExpression: Boolean = false,
        /** Epoch millis for the date parsed by the date calculator tool. */
        val parsedDateMillis: Long? = null,
        /** True when the user entered a relative expression (e.g. "2 years ago") and the result is an absolute date. */
        val isReverseDateMode: Boolean = false,
        /** Human-readable difference label when two dates are compared, e.g. "1 year 2 months 3 days". */
        val dateDiffLabel: String? = null,
        /** Pre-formatted time result, e.g. "3:45 PM", "in 4 hours 20 minutes", "8 hours 30 minutes". */
        val timeResultLabel: String? = null,
        /** Optional day context shown below the time, e.g. "tomorrow" or "yesterday". Null for today. */
        val timeContextLabel: String? = null,
        /** True when timeResultLabel is an absolute clock time (e.g. "3:45 PM"). */
        val isTimeAbsoluteResult: Boolean = false,
        /** Secondary time result shown alongside the primary (used for absolute time queries: past + future). */
        val timeResultLabel2: String? = null,
        val timeContextLabel2: String? = null,
) {
        val isToolMode: Boolean
                get() = isCalculatorMode || isUnitConverterMode || isDateCalculatorMode
}

data class PhoneNumberSelection(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        val isCall: Boolean, // true for call, false for SMS
)

data class DirectDialChoice(
        val contactName: String,
        val phoneNumber: String,
)

data class PendingThirdPartyCall(
        val app: CallingApp,
        val dataId: Long? = null,
        val phoneNumber: String? = null,
        val isVideoCall: Boolean = false,
)

enum class DirectDialOption {
        DIRECT_CALL,
        DIALER,
}

data class ContactActionPickerRequest(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        val isPrimary: Boolean,
        val currentAction: ContactCardAction?,
)

// Sealed classes for visibility states
sealed class ScreenVisibilityState {
        object Initializing : ScreenVisibilityState()

        object Loading : ScreenVisibilityState()

        data class Error(
                val message: String,
                val canRetry: Boolean = false,
        ) : ScreenVisibilityState()

        object Empty : ScreenVisibilityState()

        object Content : ScreenVisibilityState()

        object NoPermissions : ScreenVisibilityState()
}

sealed class AppsSectionVisibility {
        object Hidden : AppsSectionVisibility()

        object Loading : AppsSectionVisibility()

        object NoResults : AppsSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : AppsSectionVisibility()
}

sealed class AppShortcutsSectionVisibility {
        object Hidden : AppShortcutsSectionVisibility()

        object NoResults : AppShortcutsSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : AppShortcutsSectionVisibility()
}

sealed class ContactsSectionVisibility {
        object Hidden : ContactsSectionVisibility()

        object NoPermission : ContactsSectionVisibility()

        object NoResults : ContactsSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : ContactsSectionVisibility()
}

sealed class FilesSectionVisibility {
        object Hidden : FilesSectionVisibility()

        object NoPermission : FilesSectionVisibility()

        object NoResults : FilesSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : FilesSectionVisibility()
}

sealed class SettingsSectionVisibility {
        object Hidden : SettingsSectionVisibility()

        object NoResults : SettingsSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : SettingsSectionVisibility()
}

sealed class CalendarSectionVisibility {
        object Hidden : CalendarSectionVisibility()

        object NoPermission : CalendarSectionVisibility()

        object NoResults : CalendarSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : CalendarSectionVisibility()
}

sealed class NotesSectionVisibility {
        object Hidden : NotesSectionVisibility()

        object NoResults : NotesSectionVisibility()

        data class ShowingResults(
                val hasPinned: Boolean = false,
        ) : NotesSectionVisibility()
}

sealed class SearchEnginesVisibility {
        object Hidden : SearchEnginesVisibility()

        object Compact : SearchEnginesVisibility()

        object Full : SearchEnginesVisibility()

        data class ShortcutDetected(
                val target: SearchTarget,
        ) : SearchEnginesVisibility()
}

// ---------------------------------------------------------------------------
// SearchUiState remains a flat data class for full backward compatibility.
// All existing consumer files (35+) continue to compile with zero changes.
//
// Internally, SearchViewModel manages four focused MutableStateFlow sub-states
// (SearchResultsState, SearchPermissionState, SearchFeatureState,
// SearchUiConfigState) and combines them into this aggregate via combine().
//
// The per-keystroke hot path ONLY updates SearchResultsState (~30 fields)
// instead of copying all 70+ fields here — dramatically reducing GC pressure.
// ---------------------------------------------------------------------------

data class SearchUiState(
        // Core state
        val query: String = "",
        val hasUsagePermission: Boolean = false,
        val hasContactPermission: Boolean = false,
        val hasFilePermission: Boolean = false,
        val hasCalendarPermission: Boolean = false,
        val hasCallPermission: Boolean = false,
        val hasWallpaperPermission: Boolean = false,
        val wallpaperAvailable: Boolean = false,
        // Visibility states (computed from sub-state data)
        val screenState: ScreenVisibilityState = ScreenVisibilityState.Initializing,
        val appsSectionState: AppsSectionVisibility = AppsSectionVisibility.Hidden,
        val appShortcutsSectionState: AppShortcutsSectionVisibility =
                AppShortcutsSectionVisibility.Hidden,
        val contactsSectionState: ContactsSectionVisibility = ContactsSectionVisibility.Hidden,
        val filesSectionState: FilesSectionVisibility = FilesSectionVisibility.Hidden,
        val settingsSectionState: SettingsSectionVisibility = SettingsSectionVisibility.Hidden,
        val calendarSectionState: CalendarSectionVisibility = CalendarSectionVisibility.Hidden,
        val notesSectionState: NotesSectionVisibility = NotesSectionVisibility.Hidden,
        val searchEnginesState: SearchEnginesVisibility = SearchEnginesVisibility.Hidden,
        // App results
        val recentApps: List<AppInfo> = emptyList(),
        val searchResults: List<AppInfo> = emptyList(),
        val allApps: List<AppInfo> = emptyList(),
        val pinnedApps: List<AppInfo> = emptyList(),
        val suggestionExcludedApps: List<AppInfo> = emptyList(),
        val resultExcludedApps: List<AppInfo> = emptyList(),
        val indexedAppCount: Int = 0,
        val cacheLastUpdatedMillis: Long = 0L,
        // App shortcut results
        val appShortcutResults: List<StaticShortcut> = emptyList(),
        val allAppShortcuts: List<StaticShortcut> = emptyList(),
        val pinnedAppShortcuts: List<StaticShortcut> = emptyList(),
        val excludedAppShortcuts: List<StaticShortcut> = emptyList(),
        // Contact results
        val contactResults: List<ContactInfo> = emptyList(),
        val pinnedContacts: List<ContactInfo> = emptyList(),
        val excludedContacts: List<ContactInfo> = emptyList(),
        // File results
        val fileResults: List<DeviceFile> = emptyList(),
        val pinnedFiles: List<DeviceFile> = emptyList(),
        val excludedFiles: List<DeviceFile> = emptyList(),
        // Settings results
        val settingResults: List<DeviceSetting> = emptyList(),
        val appSettingResults: List<AppSettingResult> = emptyList(),
        val allDeviceSettings: List<DeviceSetting> = emptyList(),
        val pinnedSettings: List<DeviceSetting> = emptyList(),
        val excludedSettings: List<DeviceSetting> = emptyList(),
        // Calendar results
        val calendarEvents: List<CalendarEventInfo> = emptyList(),
        val pinnedCalendarEvents: List<CalendarEventInfo> = emptyList(),
        val excludedCalendarEvents: List<CalendarEventInfo> = emptyList(),
        // Notes results
        val noteResults: List<NoteInfo> = emptyList(),
        val pinnedNotes: List<NoteInfo> = emptyList(),
        // Lifecycle / loading
        val startupPhase: StartupPhase = StartupPhase.PHASE_1_CACHE_PREFS,
        val isInitializing: Boolean = true,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isStartupCoreSurfaceReady: Boolean = false,
        // Search engine configuration
        val searchTargetsOrder: List<SearchTarget> = emptyList(),
        val disabledSearchTargetIds: Set<String> = emptySet(),
        val isSearchEngineCompactMode: Boolean = false,
        val searchEngineCompactRowCount: Int = 1,
        val isSearchEngineAliasSuffixEnabled: Boolean = true,
        val isAliasTriggerAfterSpaceEnabled: Boolean = true,
        val amazonDomain: String? = null,
        // Dialog state (transient, not part of query hot-path)
        val phoneNumberSelection: PhoneNumberSelection? = null,
        val directDialChoice: DirectDialChoice? = null,
        val contactMethodsBottomSheet: ContactInfo? = null,
        val contactActionPickerRequest: ContactActionPickerRequest? = null,
        val pendingDirectCallNumber: String? = null,
        val pendingThirdPartyCall: PendingThirdPartyCall? = null,
        val directDialEnabled: Boolean = false,
        val assistantLaunchVoiceModeEnabled: Boolean = false,
        // File display preferences
        val enabledFileTypes: Set<com.tk.quicksearch.search.models.FileType> =
                com.tk.quicksearch.search.models.FileType.values().toSet(),
        val showFolders: Boolean = false,
        val showSystemFiles: Boolean = false,
        val folderWhitelistPatterns: Set<String> = emptySet(),
        val folderBlacklistPatterns: Set<String> = emptySet(),
        val excludedFileExtensions: Set<String> = emptySet(),
        // Layout preferences
        val oneHandedMode: Boolean = false,
        val bottomSearchBarEnabled: Boolean = false,
        val topResultIndicatorEnabled: Boolean = true,
        val openKeyboardOnLaunch: Boolean = true,
        val clearQueryOnLaunch: Boolean = true,
        val autoCloseOverlay: Boolean = true,
        val selectRetainedQuery: Boolean = false,
        val overlayModeEnabled: Boolean = false,
        // Shortcuts configuration
        val shortcutsEnabled: Boolean = true,
        val shortcutCodes: Map<String, String> = emptyMap(),
        val shortcutEnabled: Map<String, Boolean> = emptyMap(),
        val disabledAppShortcutIds: Set<String> = emptySet(),
        // Messaging / calling
        val messagingApp: MessagingApp = MessagingApp.MESSAGES,
        val callingApp: CallingApp = CallingApp.CALL,
        val isWhatsAppInstalled: Boolean = false,
        val isTelegramInstalled: Boolean = false,
        val isSignalInstalled: Boolean = false,
        val isGoogleMeetInstalled: Boolean = false,
        // Wallpaper / appearance
        val showWallpaperBackground: Boolean = true,
        val wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
        val wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS,
        val appTheme: AppTheme = AppTheme.MONOCHROME,
        val overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
        val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
        val fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER,
        val backgroundSource: BackgroundSource = BackgroundSource.THEME,
        val wallpaperAccentEnabled: Boolean = true,
        val customImageUri: String? = null,
        val startupBackgroundPreviewPath: String? = null,
        // Icon pack
        val selectedIconPackPackage: String? = null,
        val availableIconPacks: List<IconPackInfo> = emptyList(),
        val maskUnsupportedIconPackIcons: Boolean = false,
        // App display
        val showAppLabels: Boolean = true,
        val phoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
        val appIconShape: AppIconShape = AppIconShape.DEFAULT,
        val launcherAppIcon: LauncherAppIcon = LauncherAppIcon.DEFAULT,
        val themedIconsEnabled: Boolean = true,
        val deviceThemeEnabled: Boolean = false,
        val appSuggestionsEnabled: Boolean = true,
        // Section visibility preferences
        val disabledSections: Set<SearchSection> = emptySet(),
        // Web suggestions
        val webSuggestionsEnabled: Boolean = true,
        val webSuggestionsCount: Int = 3,
        // Calculator / Direct Search
        val calculatorEnabled: Boolean = true,
        val unitConverterEnabled: Boolean = true,
        val dateCalculatorEnabled: Boolean = true,
        val currencyConverterEnabled: Boolean = true,
        val wordClockEnabled: Boolean = true,
        val dictionaryEnabled: Boolean = true,
        val DirectSearchState: DirectSearchState = DirectSearchState(),
        // Gemini
        val hasGeminiApiKey: Boolean = false,
        val geminiApiKeyLast4: String? = null,
        val isSavingGeminiApiKey: Boolean = false,
        val personalContext: String = "",
        val geminiModel: String = GeminiModelCatalog.DEFAULT_MODEL_ID,
        val geminiGroundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED,
        val availableGeminiModels: List<GeminiTextModel> = GeminiModelCatalog.FALLBACK_TEXT_MODELS,
        // Release notes dialog
        val showReleaseNotesDialog: Boolean = false,
        val releaseNotesVersionName: String? = null,
        // Transient search state
        val calculatorState: CalculatorState = CalculatorState(),
        val currencyConverterState: CurrencyConverterState = CurrencyConverterState(),
        val wordClockState: WordClockState = WordClockState(),
        val dictionaryState: DictionaryState = DictionaryState(),
        val webSuggestions: List<String> = emptyList(),
        val isSecondarySearchInProgress: Boolean = false,
        val detectedShortcutTarget: SearchTarget? = null,
        val detectedAliasSearchSection: SearchSection? = null,
        val isCurrencyConverterAliasMode: Boolean = false,
        val isWordClockAliasMode: Boolean = false,
        val isDictionaryAliasMode: Boolean = false,
        val webSuggestionWasSelected: Boolean = false,
        // Onboarding / hints
        val showSearchEngineOnboarding: Boolean = false,
        val showStartSearchingOnOnboarding: Boolean = false,
        val showSearchBarWelcomeAnimation: Boolean = false,
        val showContactActionHint: Boolean = false,
        val hasSeenOverlayAssistantTip: Boolean = true,
        val hasDismissedSearchHistoryTip: Boolean = false,
        // Recent items
        val recentItems: List<RecentSearchItem> = emptyList(),
        val aliasRecentItems: List<RecentSearchItem> = emptyList(),
        val recentQueriesEnabled: Boolean = true,
        // Usage permission banner
        val shouldShowUsagePermissionBanner: Boolean = false,
        // Versioning for cache invalidation
        val contactActionsVersion: Int = 0,
        val nicknameUpdateVersion: Int = 0,
)

// ---------------------------------------------------------------------------
// Factory function: assembles a SearchUiState from the 4 focused sub-states.
// SearchViewModel.combine() calls this whenever any sub-state changes.
// ---------------------------------------------------------------------------

fun SearchUiState(
        results: SearchResultsState,
        permissions: SearchPermissionState,
        features: SearchFeatureState,
        config: SearchUiConfigState,
): SearchUiState =
        SearchUiState(
                // ── SearchResultsState ────────────────────────────────────────────
                query = results.query,
                recentApps = results.recentApps,
                searchResults = results.searchResults,
                pinnedApps = results.pinnedApps,
                allApps = results.allApps,
                suggestionExcludedApps = results.suggestionExcludedApps,
                resultExcludedApps = results.resultExcludedApps,
                indexedAppCount = results.indexedAppCount,
                cacheLastUpdatedMillis = results.cacheLastUpdatedMillis,
                appShortcutResults = results.appShortcutResults,
                allAppShortcuts = results.allAppShortcuts,
                pinnedAppShortcuts = results.pinnedAppShortcuts,
                excludedAppShortcuts = results.excludedAppShortcuts,
                contactResults = results.contactResults,
                pinnedContacts = results.pinnedContacts,
                excludedContacts = results.excludedContacts,
                fileResults = results.fileResults,
                pinnedFiles = results.pinnedFiles,
                excludedFiles = results.excludedFiles,
                settingResults = results.settingResults,
                appSettingResults = results.appSettingResults,
                allDeviceSettings = results.allDeviceSettings,
                pinnedSettings = results.pinnedSettings,
                excludedSettings = results.excludedSettings,
                calendarEvents = results.calendarEvents,
                pinnedCalendarEvents = results.pinnedCalendarEvents,
                excludedCalendarEvents = results.excludedCalendarEvents,
                noteResults = results.noteResults,
                pinnedNotes = results.pinnedNotes,
                screenState = results.screenState,
                appsSectionState = results.appsSectionState,
                appShortcutsSectionState = results.appShortcutsSectionState,
                contactsSectionState = results.contactsSectionState,
                filesSectionState = results.filesSectionState,
                settingsSectionState = results.settingsSectionState,
                calendarSectionState = results.calendarSectionState,
                notesSectionState = results.notesSectionState,
                searchEnginesState = results.searchEnginesState,
                calculatorState = results.calculatorState,
                currencyConverterState = results.currencyConverterState,
                wordClockState = results.wordClockState,
                dictionaryState = results.dictionaryState,
                DirectSearchState = results.DirectSearchState,
                webSuggestions = results.webSuggestions,
                isSecondarySearchInProgress = results.isSecondarySearchInProgress,
                webSuggestionWasSelected = results.webSuggestionWasSelected,
                detectedShortcutTarget = results.detectedShortcutTarget,
                detectedAliasSearchSection = results.detectedAliasSearchSection,
                isCurrencyConverterAliasMode = results.isCurrencyConverterAliasMode,
                isWordClockAliasMode = results.isWordClockAliasMode,
                isDictionaryAliasMode = results.isDictionaryAliasMode,
                recentItems = results.recentItems,
                aliasRecentItems = results.aliasRecentItems,
                nicknameUpdateVersion = results.nicknameUpdateVersion,
                contactActionsVersion = results.contactActionsVersion,
                // ── SearchPermissionState ─────────────────────────────────────────
                hasUsagePermission = permissions.hasUsagePermission,
                hasContactPermission = permissions.hasContactPermission,
                hasFilePermission = permissions.hasFilePermission,
                hasCalendarPermission = permissions.hasCalendarPermission,
                hasCallPermission = permissions.hasCallPermission,
                hasWallpaperPermission = permissions.hasWallpaperPermission,
                wallpaperAvailable = permissions.wallpaperAvailable,
                messagingApp = permissions.messagingApp,
                callingApp = permissions.callingApp,
                isWhatsAppInstalled = permissions.isWhatsAppInstalled,
                isTelegramInstalled = permissions.isTelegramInstalled,
                isSignalInstalled = permissions.isSignalInstalled,
                isGoogleMeetInstalled = permissions.isGoogleMeetInstalled,
                // ── SearchFeatureState ────────────────────────────────────────────
                searchTargetsOrder = features.searchTargetsOrder,
                disabledSearchTargetIds = features.disabledSearchTargetIds,
                isSearchEngineCompactMode = features.isSearchEngineCompactMode,
                searchEngineCompactRowCount = features.searchEngineCompactRowCount,
                isSearchEngineAliasSuffixEnabled = features.isSearchEngineAliasSuffixEnabled,
                isAliasTriggerAfterSpaceEnabled = features.isAliasTriggerAfterSpaceEnabled,
                amazonDomain = features.amazonDomain,
                shortcutsEnabled = features.shortcutsEnabled,
                shortcutCodes = features.shortcutCodes,
                shortcutEnabled = features.shortcutEnabled,
                disabledAppShortcutIds = features.disabledAppShortcutIds,
                disabledSections = features.disabledSections,
                hasGeminiApiKey = features.hasGeminiApiKey,
                geminiApiKeyLast4 = features.geminiApiKeyLast4,
                isSavingGeminiApiKey = features.isSavingGeminiApiKey,
                personalContext = features.personalContext,
                geminiModel = features.geminiModel,
                geminiGroundingEnabled = features.geminiGroundingEnabled,
                availableGeminiModels = features.availableGeminiModels,
                webSuggestionsEnabled = features.webSuggestionsEnabled,
                webSuggestionsCount = features.webSuggestionsCount,
                calculatorEnabled = features.calculatorEnabled,
                unitConverterEnabled = features.unitConverterEnabled,
                dateCalculatorEnabled = features.dateCalculatorEnabled,
                currencyConverterEnabled = features.currencyConverterEnabled,
                wordClockEnabled = features.wordClockEnabled,
                dictionaryEnabled = features.dictionaryEnabled,
                recentQueriesEnabled = features.recentQueriesEnabled,
                hasDismissedSearchHistoryTip = features.hasDismissedSearchHistoryTip,
                directDialEnabled = features.directDialEnabled,
                assistantLaunchVoiceModeEnabled = features.assistantLaunchVoiceModeEnabled,
                shouldShowUsagePermissionBanner = features.shouldShowUsagePermissionBanner,
                // ── SearchUiConfigState ───────────────────────────────────────────
                startupPhase = config.startupPhase,
                isInitializing = config.isInitializing,
                isLoading = config.isLoading,
                errorMessage = config.errorMessage,
                isStartupCoreSurfaceReady = config.isStartupCoreSurfaceReady,
                showWallpaperBackground = config.showWallpaperBackground,
                wallpaperBackgroundAlpha = config.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = config.wallpaperBlurRadius,
                appTheme = config.appTheme,
                overlayThemeIntensity = config.overlayThemeIntensity,
                appThemeMode = config.appThemeMode,
                backgroundSource = config.backgroundSource,
                customImageUri = config.customImageUri,
                startupBackgroundPreviewPath = config.startupBackgroundPreviewPath,
                overlayModeEnabled = config.overlayModeEnabled,
                oneHandedMode = config.oneHandedMode,
                bottomSearchBarEnabled = config.bottomSearchBarEnabled,
                topResultIndicatorEnabled = config.topResultIndicatorEnabled,
                wallpaperAccentEnabled = config.wallpaperAccentEnabled,
                openKeyboardOnLaunch = config.openKeyboardOnLaunch,
                clearQueryOnLaunch = config.clearQueryOnLaunch,
                autoCloseOverlay = config.autoCloseOverlay,
                selectRetainedQuery = config.selectRetainedQuery,
                fontScaleMultiplier = config.fontScaleMultiplier,
                showAppLabels = config.showAppLabels,
                phoneAppGridColumns = config.phoneAppGridColumns,
                appIconShape = config.appIconShape,
                launcherAppIcon = config.launcherAppIcon,
                themedIconsEnabled = config.themedIconsEnabled,
                deviceThemeEnabled = config.deviceThemeEnabled,
                appSuggestionsEnabled = config.appSuggestionsEnabled,
                selectedIconPackPackage = config.selectedIconPackPackage,
                availableIconPacks = config.availableIconPacks,
                maskUnsupportedIconPackIcons = config.maskUnsupportedIconPackIcons,
                enabledFileTypes = config.enabledFileTypes,
                showFolders = config.showFolders,
                showSystemFiles = config.showSystemFiles,
                folderWhitelistPatterns = config.folderWhitelistPatterns,
                folderBlacklistPatterns = config.folderBlacklistPatterns,
                excludedFileExtensions = config.excludedFileExtensions,
                showSearchEngineOnboarding = config.showSearchEngineOnboarding,
                showStartSearchingOnOnboarding = config.showStartSearchingOnOnboarding,
                showSearchBarWelcomeAnimation = config.showSearchBarWelcomeAnimation,
                showContactActionHint = config.showContactActionHint,
                hasSeenOverlayAssistantTip = config.hasSeenOverlayAssistantTip,
                showReleaseNotesDialog = config.showReleaseNotesDialog,
                releaseNotesVersionName = config.releaseNotesVersionName,
                phoneNumberSelection = config.phoneNumberSelection,
                directDialChoice = config.directDialChoice,
                contactMethodsBottomSheet = config.contactMethodsBottomSheet,
                contactActionPickerRequest = config.contactActionPickerRequest,
                pendingDirectCallNumber = config.pendingDirectCallNumber,
                pendingThirdPartyCall = config.pendingThirdPartyCall,
        )
