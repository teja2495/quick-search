package com.tk.quicksearch.settings.settingsScreen

import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchSection

// ============================================================================
// App Settings State
// ============================================================================

data class AppSettingsState(
    val suggestionExcludedApps: List<AppInfo>,
    val resultExcludedApps: List<AppInfo>
)

// ============================================================================
// Contact Settings State
// ============================================================================

data class ContactSettingsState(
    val excludedContacts: List<ContactInfo>,
    val directDialEnabled: Boolean
)

// ============================================================================
// File Settings State
// ============================================================================

data class FileSettingsState(
    val enabledFileTypes: Set<FileType>,
    val excludedFileExtensions: Set<String>,
    val excludedFiles: List<DeviceFile>
)

// ============================================================================
// UI/Appearance Settings State
// ============================================================================

data class UiSettingsState(
    val keyboardAlignedLayout: Boolean,
    val showWallpaperBackground: Boolean,
    val selectedIconPackPackage: String?,
    val availableIconPacks: List<IconPackInfo>,
    val messagingApp: MessagingApp,
    val isWhatsAppInstalled: Boolean,
    val isTelegramInstalled: Boolean
)

// ============================================================================
// Search Engine Settings State
// ============================================================================

data class SearchEngineSettingsState(
    val searchEngineOrder: List<SearchTarget>,
    val disabledSearchEngines: Set<String>,
    val isSearchEngineCompactMode: Boolean,
    val shortcutCodes: Map<String, String>,
    val shortcutEnabled: Map<String, Boolean>
)

// ============================================================================
// Web Search Settings State
// ============================================================================

data class WebSearchSettingsState(
    val webSuggestionsEnabled: Boolean,
    val webSuggestionsCount: Int,
    val recentQueriesEnabled: Boolean,
    val recentQueriesCount: Int
)

// ============================================================================
// AI/Gemini Settings State
// ============================================================================

data class AiSettingsState(
    val calculatorEnabled: Boolean,
    val hasGeminiApiKey: Boolean,
    val geminiApiKeyLast4: String?,
    val personalContext: String,
    val amazonDomain: String?
)

// ============================================================================
// Section Settings State
// ============================================================================

data class SectionSettingsState(
    val sectionOrder: List<SearchSection>,
    val disabledSections: Set<SearchSection>
)

// ============================================================================
// Excluded Items State
// ============================================================================

data class ExcludedItemsState(
    val suggestionExcludedApps: List<AppInfo>,
    val resultExcludedApps: List<AppInfo>,
    val excludedContacts: List<ContactInfo>,
    val excludedFiles: List<DeviceFile>,
    val excludedSettings: List<DeviceSetting>,
    val excludedAppShortcuts: List<StaticShortcut>
)
