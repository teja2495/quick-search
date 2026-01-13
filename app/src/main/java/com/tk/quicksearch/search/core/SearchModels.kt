package com.tk.quicksearch.search.core

// IconPackInfo moved here to avoid circular imports
data class IconPackInfo(
    val packageName: String,
    val label: String
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

enum class SearchSection {
    APPS,
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

data class CalculatorState(
    val result: String? = null,
    val expression: String? = null
)

data class PhoneNumberSelection(
    val contactInfo: com.tk.quicksearch.model.ContactInfo,
    val isCall: Boolean // true for call, false for SMS
)

data class DirectDialChoice(
    val contactName: String,
    val phoneNumber: String
)

enum class DirectDialOption {
    DIRECT_CALL,
    DIALER
}

data class SearchUiState(
    val isInitializing: Boolean = true,
    val query: String = "",
    val hasUsagePermission: Boolean = false,
    val hasContactPermission: Boolean = false,
    val hasFilePermission: Boolean = false,
    val hasCallPermission: Boolean = false,
    val isLoading: Boolean = true,
    val recentApps: List<com.tk.quicksearch.model.AppInfo> = emptyList(),
    val searchResults: List<com.tk.quicksearch.model.AppInfo> = emptyList(),
    val pinnedApps: List<com.tk.quicksearch.model.AppInfo> = emptyList(),
    val suggestionExcludedApps: List<com.tk.quicksearch.model.AppInfo> = emptyList(),
    val resultExcludedApps: List<com.tk.quicksearch.model.AppInfo> = emptyList(),
    val contactResults: List<com.tk.quicksearch.model.ContactInfo> = emptyList(),
    val fileResults: List<com.tk.quicksearch.model.DeviceFile> = emptyList(),
    val settingResults: List<com.tk.quicksearch.model.SettingShortcut> = emptyList(),
    val pinnedContacts: List<com.tk.quicksearch.model.ContactInfo> = emptyList(),
    val pinnedFiles: List<com.tk.quicksearch.model.DeviceFile> = emptyList(),
    val pinnedSettings: List<com.tk.quicksearch.model.SettingShortcut> = emptyList(),
    val excludedContacts: List<com.tk.quicksearch.model.ContactInfo> = emptyList(),
    val excludedFiles: List<com.tk.quicksearch.model.DeviceFile> = emptyList(),
    val excludedSettings: List<com.tk.quicksearch.model.SettingShortcut> = emptyList(),
    val indexedAppCount: Int = 0,
    val cacheLastUpdatedMillis: Long = 0L,
    val errorMessage: String? = null,
    val searchEngineOrder: List<SearchEngine> = emptyList(),
    val disabledSearchEngines: Set<SearchEngine> = emptySet(),
    val phoneNumberSelection: PhoneNumberSelection? = null,
    val directDialChoice: DirectDialChoice? = null,
    val contactMethodsBottomSheet: com.tk.quicksearch.model.ContactInfo? = null,
    val pendingDirectCallNumber: String? = null,
    val pendingWhatsAppCallDataId: String? = null,
    val directDialEnabled: Boolean = false,
    val enabledFileTypes: Set<com.tk.quicksearch.model.FileType> = com.tk.quicksearch.model.FileType.values().toSet(),
    val excludedFileExtensions: Set<String> = emptySet(),
    val keyboardAlignedLayout: Boolean = false,
    val shortcutsEnabled: Boolean = true,
    val shortcutCodes: Map<SearchEngine, String> = emptyMap(),
    val shortcutEnabled: Map<SearchEngine, Boolean> = emptyMap(),
    val messagingApp: MessagingApp = MessagingApp.MESSAGES,
    val isWhatsAppInstalled: Boolean = false,
    val isTelegramInstalled: Boolean = false,
    val showWallpaperBackground: Boolean = true,
    val clearQueryAfterSearchEngine: Boolean = false,
    val showAllResults: Boolean = false,
    val selectedIconPackPackage: String? = null,
    val availableIconPacks: List<IconPackInfo> = emptyList(),
    val sortAppsByUsageEnabled: Boolean = false,
    val sectionOrder: List<SearchSection> = emptyList(),
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
    val detectedShortcutEngine: SearchEngine? = null,
    val webSuggestionWasSelected: Boolean = false,
    val recentQueries: List<String> = emptyList(),
    val recentQueriesEnabled: Boolean = true,
    val recentQueriesCount: Int = 3,
    val shouldShowUsagePermissionBanner: Boolean = false
)
