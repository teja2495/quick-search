package com.tk.quicksearch.settings.main

import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchEngine
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
    val searchEngineOrder: List<SearchEngine>,
    val disabledSearchEngines: Set<SearchEngine>,
    val isSearchEngineCompactMode: Boolean,
    val shortcutCodes: Map<SearchEngine, String>,
    val shortcutEnabled: Map<SearchEngine, Boolean>
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
    val excludedSettings: List<SettingShortcut>
)