package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.recentSearches.RecentSearchItem

// IconPackInfo moved here to avoid circular imports
data class IconPackInfo(val packageName: String, val label: String)

enum class SearchEngine {
    DIRECT_SEARCH,
    GOOGLE,
    CHATGPT,
    GEMINI,
    PERPLEXITY,
    GOOGLE_PLAY,
    YOUTUBE,
    GOOGLE_MAPS,
    GROK,
    REDDIT,
    AMAZON,
    X,
    YOUTUBE_MUSIC,
    SPOTIFY,
    GOOGLE_DRIVE,
    GOOGLE_PHOTOS,
    AI_MODE,
    DUCKDUCKGO,
    BRAVE,
    FACEBOOK_MARKETPLACE,
    YOU_COM,
    BING,
    STARTPAGE,
}

data class BrowserApp(val packageName: String, val label: String)

sealed class SearchTarget {
    data class Engine(val engine: SearchEngine) : SearchTarget()
    data class Browser(val app: BrowserApp) : SearchTarget()
}

enum class SearchSection {
    APPS,
    APP_SHORTCUTS,
    CONTACTS,
    FILES,
    SETTINGS
}

enum class MessagingApp {
    MESSAGES,
    WHATSAPP,
    TELEGRAM
}

enum class DirectSearchStatus {
    Idle,
    Loading,
    Success,
    Error
}

data class DirectSearchState(
        val status: DirectSearchStatus = DirectSearchStatus.Idle,
        val answer: String? = null,
        val errorMessage: String? = null,
        val activeQuery: String? = null
)

data class CalculatorState(val result: String? = null, val expression: String? = null)

data class PhoneNumberSelection(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        val isCall: Boolean // true for call, false for SMS
)

data class DirectDialChoice(val contactName: String, val phoneNumber: String)

enum class DirectDialOption {
    DIRECT_CALL,
    DIALER
}

data class ContactActionPickerRequest(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        val isPrimary: Boolean,
        val currentAction: ContactCardAction?
)

// Sealed classes for visibility states
sealed class ScreenVisibilityState {
    object Initializing : ScreenVisibilityState()
    object Loading : ScreenVisibilityState()
    data class Error(val message: String, val canRetry: Boolean = false) : ScreenVisibilityState()
    object Empty : ScreenVisibilityState()
    object Content : ScreenVisibilityState()
    object NoPermissions : ScreenVisibilityState()
}

sealed class AppsSectionVisibility {
    object Hidden : AppsSectionVisibility()
    object Loading : AppsSectionVisibility()
    object NoResults : AppsSectionVisibility()
    data class ShowingResults(val hasPinned: Boolean = false) : AppsSectionVisibility()
}

sealed class AppShortcutsSectionVisibility {
    object Hidden : AppShortcutsSectionVisibility()
    object NoResults : AppShortcutsSectionVisibility()
    data class ShowingResults(val hasPinned: Boolean = false) : AppShortcutsSectionVisibility()
}

sealed class ContactsSectionVisibility {
    object Hidden : ContactsSectionVisibility()
    object NoPermission : ContactsSectionVisibility()
    object NoResults : ContactsSectionVisibility()
    data class ShowingResults(val hasPinned: Boolean = false) : ContactsSectionVisibility()
}

sealed class FilesSectionVisibility {
    object Hidden : FilesSectionVisibility()
    object NoPermission : FilesSectionVisibility()
    object NoResults : FilesSectionVisibility()
    data class ShowingResults(val hasPinned: Boolean = false) : FilesSectionVisibility()
}

sealed class SettingsSectionVisibility {
    object Hidden : SettingsSectionVisibility()
    object NoResults : SettingsSectionVisibility()
    data class ShowingResults(val hasPinned: Boolean = false) : SettingsSectionVisibility()
}

sealed class SearchEnginesVisibility {
    object Hidden : SearchEnginesVisibility()
    object Compact : SearchEnginesVisibility()
    object Full : SearchEnginesVisibility()
    data class ShortcutDetected(val target: SearchTarget) : SearchEnginesVisibility()
}

data class SearchUiState(
        // Core state
        val query: String = "",
        val hasUsagePermission: Boolean = false,
        val hasContactPermission: Boolean = false,
        val hasFilePermission: Boolean = false,
        val hasCallPermission: Boolean = false,
        val hasWallpaperPermission: Boolean = false,
        val wallpaperAvailable: Boolean = false,

        // Visibility states (replaces scattered boolean flags)
        val screenState: ScreenVisibilityState = ScreenVisibilityState.Initializing,
        val appsSectionState: AppsSectionVisibility = AppsSectionVisibility.Hidden,
        val appShortcutsSectionState: AppShortcutsSectionVisibility =
                AppShortcutsSectionVisibility.Hidden,
        val contactsSectionState: ContactsSectionVisibility = ContactsSectionVisibility.Hidden,
        val filesSectionState: FilesSectionVisibility = FilesSectionVisibility.Hidden,
        val settingsSectionState: SettingsSectionVisibility = SettingsSectionVisibility.Hidden,
        val searchEnginesState: SearchEnginesVisibility = SearchEnginesVisibility.Hidden,

        // Data (unchanged)
        val recentApps: List<com.tk.quicksearch.search.models.AppInfo> = emptyList(),
        val searchResults: List<com.tk.quicksearch.search.models.AppInfo> = emptyList(),
        val appShortcutResults: List<StaticShortcut> = emptyList(),
        val pinnedApps: List<com.tk.quicksearch.search.models.AppInfo> = emptyList(),
        val suggestionExcludedApps: List<com.tk.quicksearch.search.models.AppInfo> = emptyList(),
        val resultExcludedApps: List<com.tk.quicksearch.search.models.AppInfo> = emptyList(),
        val contactResults: List<com.tk.quicksearch.search.models.ContactInfo> = emptyList(),
        val fileResults: List<com.tk.quicksearch.search.models.DeviceFile> = emptyList(),
        val settingResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val pinnedAppShortcuts: List<StaticShortcut> = emptyList(),
        val pinnedContacts: List<com.tk.quicksearch.search.models.ContactInfo> = emptyList(),
        val pinnedFiles: List<com.tk.quicksearch.search.models.DeviceFile> = emptyList(),
        val pinnedSettings: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val excludedAppShortcuts: List<StaticShortcut> = emptyList(),
        val excludedContacts: List<com.tk.quicksearch.search.models.ContactInfo> = emptyList(),
        val excludedFiles: List<com.tk.quicksearch.search.models.DeviceFile> = emptyList(),
        val excludedSettings: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),

        // Metadata
        val indexedAppCount: Int = 0,
        val cacheLastUpdatedMillis: Long = 0L,

        // Legacy fields (keeping for backward compatibility during migration)
        val isInitializing: Boolean = true,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,

        // UI configuration (unchanged)
        val searchTargetsOrder: List<SearchTarget> = emptyList(),
        val disabledSearchTargetIds: Set<String> = emptySet(),
        val phoneNumberSelection: PhoneNumberSelection? = null,
        val directDialChoice: DirectDialChoice? = null,
        val contactMethodsBottomSheet: com.tk.quicksearch.search.models.ContactInfo? = null,
        val contactActionPickerRequest: ContactActionPickerRequest? = null,
        val pendingDirectCallNumber: String? = null,
        val pendingWhatsAppCallDataId: String? = null,
        val directDialEnabled: Boolean = false,
        val enabledFileTypes: Set<com.tk.quicksearch.search.models.FileType> =
                com.tk.quicksearch.search.models.FileType.values()
                        .filter { it != com.tk.quicksearch.search.models.FileType.OTHER }
                        .toSet(),
        val showFolders: Boolean = false,
        val showSystemFiles: Boolean = false,
        val showHiddenFiles: Boolean = false,
        val excludedFileExtensions: Set<String> = emptySet(),
        val oneHandedMode: Boolean = false,
        val overlayModeEnabled: Boolean = false,
        val shortcutsEnabled: Boolean = true,
        val shortcutCodes: Map<String, String> = emptyMap(),
        val shortcutEnabled: Map<String, Boolean> = emptyMap(),
        val messagingApp: MessagingApp = MessagingApp.MESSAGES,
        val isWhatsAppInstalled: Boolean = false,
        val isTelegramInstalled: Boolean = false,
        val showWallpaperBackground: Boolean = true,
        val wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
        val wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS,
        val selectedIconPackPackage: String? = null,
        val availableIconPacks: List<IconPackInfo> = emptyList(),
        val disabledSections: Set<SearchSection> = emptySet(),
        val isSearchEngineCompactMode: Boolean = false,
        val amazonDomain: String? = null,
        val webSuggestionsEnabled: Boolean = true,
        val webSuggestionsCount: Int = 3,
        val calculatorEnabled: Boolean = true,
        val DirectSearchState: DirectSearchState = DirectSearchState(),
        val hasGeminiApiKey: Boolean = false,
        val geminiApiKeyLast4: String? = null,
        val personalContext: String = "",
        val showReleaseNotesDialog: Boolean = false,
        val releaseNotesVersionName: String? = null,
        val calculatorState: CalculatorState = CalculatorState(),
        val webSuggestions: List<String> = emptyList(),
        val showSearchEngineOnboarding: Boolean = false,
        val showSearchBarWelcomeAnimation: Boolean = false,
        val showContactActionHint: Boolean = false,
        val showPersonalContextHint: Boolean = false,
        val detectedShortcutTarget: SearchTarget? = null,
        val webSuggestionWasSelected: Boolean = false,
        val recentItems: List<RecentSearchItem> = emptyList(),
        val recentQueriesEnabled: Boolean = true,
        val recentQueriesCount: Int = 3,
        val shouldShowUsagePermissionBanner: Boolean = false,
        val contactActionsVersion: Int = 0,
        val nicknameUpdateVersion: Int = 0,
        val showOverlayCloseTip: Boolean = false
)
