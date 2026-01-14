package com.tk.quicksearch.settings.settingsScreen

import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection

// ============================================================================
// App Settings Callbacks
// ============================================================================

data class AppSettingsCallbacks(
    val onRemoveSuggestionExcludedApp: (AppInfo) -> Unit,
    val onRemoveResultExcludedApp: (AppInfo) -> Unit
)

// ============================================================================
// Contact Settings Callbacks
// ============================================================================

data class ContactSettingsCallbacks(
    val onRemoveExcludedContact: (ContactInfo) -> Unit,
    val onToggleDirectDial: (Boolean) -> Unit
)

// ============================================================================
// File Settings Callbacks
// ============================================================================

data class FileSettingsCallbacks(
    val onToggleFileType: (FileType, Boolean) -> Unit,
    val onRemoveExcludedFileExtension: (String) -> Unit,
    val onRemoveExcludedFile: (DeviceFile) -> Unit
)

// ============================================================================
// UI/Appearance Settings Callbacks
// ============================================================================

data class UiSettingsCallbacks(
    val onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    val onToggleShowWallpaperBackground: (Boolean) -> Unit,
    val onSelectIconPack: (String?) -> Unit,
    val onSearchIconPacks: () -> Unit,
    val onRefreshIconPacks: () -> Unit,
    val onSetMessagingApp: (MessagingApp) -> Unit
)

// ============================================================================
// Search Engine Settings Callbacks
// ============================================================================

data class SearchEngineSettingsCallbacks(
    val onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    val onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    val onToggleSearchEngineCompactMode: (Boolean) -> Unit,
    val setShortcutCode: (SearchEngine, String) -> Unit,
    val setShortcutEnabled: (SearchEngine, Boolean) -> Unit
)

// ============================================================================
// Web Search Settings Callbacks
// ============================================================================

data class WebSearchSettingsCallbacks(
    val onToggleWebSuggestions: (Boolean) -> Unit,
    val onWebSuggestionsCountChange: (Int) -> Unit,
    val onToggleRecentQueries: (Boolean) -> Unit,
    val onRecentQueriesCountChange: (Int) -> Unit
)

// ============================================================================
// AI/Gemini Settings Callbacks
// ============================================================================

data class AiSettingsCallbacks(
    val onToggleCalculator: (Boolean) -> Unit,
    val onSetGeminiApiKey: (String?) -> Unit,
    val onSetPersonalContext: (String?) -> Unit,
    val onSetAmazonDomain: (String?) -> Unit
)

// ============================================================================
// Section Settings Callbacks
// ============================================================================

data class SectionSettingsCallbacks(
    val onToggleSection: (SearchSection, Boolean) -> Unit,
    val onReorderSections: (List<SearchSection>) -> Unit
)

// ============================================================================
// Excluded Items Callbacks
// ============================================================================

data class ExcludedItemsCallbacks(
    val onRemoveSuggestionExcludedApp: (AppInfo) -> Unit,
    val onRemoveResultExcludedApp: (AppInfo) -> Unit,
    val onRemoveExcludedContact: (ContactInfo) -> Unit,
    val onRemoveExcludedFile: (DeviceFile) -> Unit,
    val onRemoveExcludedSetting: (DeviceSetting) -> Unit,
    val onClearAllExclusions: () -> Unit
)