package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.directSearch.GeminiTextModel

// =============================================================================
// The four focused sub-state data classes that replace the monolithic
// SearchUiState for internal ViewModel state management.
//
// WHY FOUR SEPARATE FLOWS?
//   Before: every keystroke → full SearchUiState.copy() → 70+ fields copied.
//   After:  every keystroke → only SearchResultsState.copy() → ~30 fields.
//   SearchPermissionState, SearchFeatureState, SearchUiConfigState are only
//   copied when the user visits settings pages — not during typing.
//
// BACKWARD COMPATIBILITY:
//   SearchViewModel.uiState remains a single StateFlow<SearchUiState>, assembled
//   by combine()-ing these four flows. All consumer files are unchanged.
// =============================================================================

// ---------------------------------------------------------------------------
// 1. SearchResultsState — the HOT PATH. Updated on every keystroke.
//    ~30 fields vs the original 70+.
// ---------------------------------------------------------------------------

data class SearchResultsState(
        // Active query
        val query: String = "",
        // App results (updated by refreshDerivedState on each query change)
        val recentApps: List<AppInfo> = emptyList(),
        val searchResults: List<AppInfo> = emptyList(),
        val pinnedApps: List<AppInfo> = emptyList(),
        val allApps: List<AppInfo> = emptyList(),
        val suggestionExcludedApps: List<AppInfo> = emptyList(),
        val resultExcludedApps: List<AppInfo> = emptyList(),
        val indexedAppCount: Int = 0,
        val cacheLastUpdatedMillis: Long = 0L,
        // App shortcut results
        val appShortcutResults: List<StaticShortcut> = emptyList(),
        val allAppShortcuts: List<StaticShortcut> = emptyList(),
        val pinnedAppShortcuts: List<StaticShortcut> = emptyList(),
        val excludedAppShortcuts: List<StaticShortcut> = emptyList(),
        // Contact results (debounced secondary search)
        val contactResults: List<ContactInfo> = emptyList(),
        val pinnedContacts: List<ContactInfo> = emptyList(),
        val excludedContacts: List<ContactInfo> = emptyList(),
        // File results (debounced secondary search)
        val fileResults: List<DeviceFile> = emptyList(),
        val pinnedFiles: List<DeviceFile> = emptyList(),
        val excludedFiles: List<DeviceFile> = emptyList(),
        // Settings results (debounced secondary search)
        val settingResults: List<DeviceSetting> = emptyList(),
        val allDeviceSettings: List<DeviceSetting> = emptyList(),
        val pinnedSettings: List<DeviceSetting> = emptyList(),
        val excludedSettings: List<DeviceSetting> = emptyList(),
        // Computed visibility states (derived from results above)
        val screenState: ScreenVisibilityState = ScreenVisibilityState.Initializing,
        val appsSectionState: AppsSectionVisibility = AppsSectionVisibility.Hidden,
        val appShortcutsSectionState: AppShortcutsSectionVisibility =
                AppShortcutsSectionVisibility.Hidden,
        val contactsSectionState: ContactsSectionVisibility = ContactsSectionVisibility.Hidden,
        val filesSectionState: FilesSectionVisibility = FilesSectionVisibility.Hidden,
        val settingsSectionState: SettingsSectionVisibility = SettingsSectionVisibility.Hidden,
        val searchEnginesState: SearchEnginesVisibility = SearchEnginesVisibility.Hidden,
        // Transient search state (calculator answer, direct search, web suggestions)
        val calculatorState: CalculatorState = CalculatorState(),
        val DirectSearchState: DirectSearchState = DirectSearchState(),
        val webSuggestions: List<String> = emptyList(),
        val webSuggestionWasSelected: Boolean = false,
        val detectedShortcutTarget: SearchTarget? = null,
        val detectedAliasSearchSection: SearchSection? = null,
        // Recent items (shown when query is blank)
        val recentItems: List<RecentSearchItem> = emptyList(),
        // Cache invalidation counters
        val nicknameUpdateVersion: Int = 0,
        val contactActionsVersion: Int = 0,
)

// ---------------------------------------------------------------------------
// 2. SearchPermissionState — updated only when the OS grants/revokes a perm.
// ---------------------------------------------------------------------------

data class SearchPermissionState(
        val hasUsagePermission: Boolean = false,
        val hasContactPermission: Boolean = false,
        val hasFilePermission: Boolean = false,
        val hasCallPermission: Boolean = false,
        val hasWallpaperPermission: Boolean = false,
        val wallpaperAvailable: Boolean = false,
        // Messaging / calling app selection (depends on installed apps)
        val messagingApp: MessagingApp = MessagingApp.MESSAGES,
        val callingApp: CallingApp = CallingApp.CALL,
        val isWhatsAppInstalled: Boolean = false,
        val isTelegramInstalled: Boolean = false,
        val isSignalInstalled: Boolean = false,
        val isGoogleMeetInstalled: Boolean = false,
)

// ---------------------------------------------------------------------------
// 3. SearchFeatureState — updated only when the user changes settings pages.
// ---------------------------------------------------------------------------

data class SearchFeatureState(
        // Search engine targets & configuration
        val searchTargetsOrder: List<SearchTarget> = emptyList(),
        val disabledSearchTargetIds: Set<String> = emptySet(),
        val isSearchEngineCompactMode: Boolean = false,
        val searchEngineCompactRowCount: Int = 1,
        val amazonDomain: String? = null,
        // App shortcuts
        val shortcutsEnabled: Boolean = true,
        val shortcutCodes: Map<String, String> = emptyMap(),
        val shortcutEnabled: Map<String, Boolean> = emptyMap(),
        val disabledAppShortcutIds: Set<String> = emptySet(),
        // Section visibility preferences (which sections are enabled/disabled)
        val disabledSections: Set<SearchSection> = emptySet(),
        // Gemini / Direct Search
        val hasGeminiApiKey: Boolean = false,
        val geminiApiKeyLast4: String? = null,
        val personalContext: String = "",
        val geminiModel: String = GeminiModelCatalog.DEFAULT_MODEL_ID,
        val geminiGroundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED,
        val availableGeminiModels: List<GeminiTextModel> = GeminiModelCatalog.FALLBACK_TEXT_MODELS,
        // Web suggestions
        val webSuggestionsEnabled: Boolean = true,
        val webSuggestionsCount: Int = 3,
        // Calculator
        val calculatorEnabled: Boolean = true,
        // Search history
        val recentQueriesEnabled: Boolean = true,
        val hasDismissedSearchHistoryTip: Boolean = false,
        // Direct dial
        val directDialEnabled: Boolean = false,
        // Usage permission banner
        val shouldShowUsagePermissionBanner: Boolean = false,
)

// ---------------------------------------------------------------------------
// 4. SearchUiConfigState — updated only on display preference changes.
// ---------------------------------------------------------------------------

data class SearchUiConfigState(
        // Lifecycle / loading coarse state
        val startupPhase: StartupPhase = StartupPhase.PHASE_1_CACHE_PREFS,
        val isInitializing: Boolean = true,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isStartupCoreSurfaceReady: Boolean = false,
        // Wallpaper / background appearance
        val showWallpaperBackground: Boolean = false,
        val wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
        val wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS,
        val overlayGradientTheme: OverlayGradientTheme = OverlayGradientTheme.MONOCHROME,
        val overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
        val backgroundSource: BackgroundSource = BackgroundSource.THEME,
        val customImageUri: String? = null,
        val startupBackgroundPreviewPath: String? = null,
        // Layout preferences
        val overlayModeEnabled: Boolean = false,
        val oneHandedMode: Boolean = false,
        val bottomSearchBarEnabled: Boolean = false,
        val openKeyboardOnLaunch: Boolean = true,
        val clearQueryOnLaunch: Boolean = true,
        val fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER,
        // App display preferences
        val showAppLabels: Boolean = true,
        val appSuggestionsEnabled: Boolean = true,
        val selectedIconPackPackage: String? = null,
        val availableIconPacks: List<IconPackInfo> = emptyList(),
        // File display preferences
        val enabledFileTypes: Set<FileType> =
                FileType.values().filter { it != FileType.OTHER }.toSet(),
        val showFolders: Boolean = false,
        val showSystemFiles: Boolean = false,
        val showHiddenFiles: Boolean = false,
        val folderWhitelistPatterns: Set<String> = emptySet(),
        val folderBlacklistPatterns: Set<String> = emptySet(),
        val excludedFileExtensions: Set<String> = emptySet(),
        // Onboarding, hints, and dialog visibility
        val showSearchEngineOnboarding: Boolean = false,
        val showStartSearchingOnOnboarding: Boolean = false,
        val showSearchBarWelcomeAnimation: Boolean = false,
        val showContactActionHint: Boolean = false,
        val showPersonalContextHint: Boolean = false,
        val hasSeenOverlayAssistantTip: Boolean = true,
        val showReleaseNotesDialog: Boolean = false,
        val releaseNotesVersionName: String? = null,
        // Transient dialog state (ephemeral UI overlays unrelated to search query)
        val phoneNumberSelection: PhoneNumberSelection? = null,
        val directDialChoice: DirectDialChoice? = null,
        val contactMethodsBottomSheet: ContactInfo? = null,
        val contactActionPickerRequest: ContactActionPickerRequest? = null,
        val pendingDirectCallNumber: String? = null,
        val pendingThirdPartyCall: PendingThirdPartyCall? = null,
)
