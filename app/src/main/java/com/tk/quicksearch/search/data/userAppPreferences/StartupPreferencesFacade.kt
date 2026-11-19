package com.tk.quicksearch.search.data.userAppPreferences

import android.content.Context
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.app.LauncherAppIcon
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType

/**
 * Facade for startup preference operations
 */
class StartupPreferencesFacade(
    private val context: Context
) {
    /**
     * Data class to hold all preferences needed during app startup for performance optimization.
     */
    data class StartupPreferences(
            val enabledFileTypes: Set<FileType>,
            val showFolders: Boolean,
            val booleanSettings: Map<String, Boolean>,
            val showSystemFiles: Boolean,
            val folderWhitelistPatterns: Set<String>,
            val folderBlacklistPatterns: Set<String>,
            val excludedFileExtensions: Set<String>,
            val oneHandedMode: Boolean,
            val bottomSearchBarEnabled: Boolean,
            val unifiedPinnedItemsEnabled: Boolean,
            val searchHintsEnabled: Boolean,
            val settingsIconEnabled: Boolean,
            val topResultIndicatorEnabled: Boolean,
            val openTopResultUsingKeyboardEnabled: Boolean,
            val openKeyboardOnLaunch: Boolean,
            val clearQueryOnLaunch: Boolean,
            val autoCloseOverlay: Boolean,
            val overlayModeEnabled: Boolean,
            val directDialEnabled: Boolean,
            val hasSeenDirectDialChoice: Boolean,
            val wallpaperBackgroundAlpha: Float,
            val wallpaperBlurRadius: Float,
            val appTheme: AppTheme,
            val overlayThemeIntensity: Float,
            val fontScaleMultiplier: Float,
            val useSystemFont: Boolean,
            val backgroundSource: BackgroundSource,
            val customImageUri: String?,
            val amazonDomain: String?,
            val pinnedPackages: Set<String>,
            val suggestionHiddenPackages: Set<String>,
            val resultHiddenPackages: Set<String>,
            val searchHistoryEnabled: Boolean,
            val appSuggestionsEnabled: Boolean,
            val showAllAppsButton: Boolean,
            val includeNonLaunchableAppsInSearch: Boolean,
            val selectedAppSuggestionTab: AppSuggestionTabType,
            val enabledAppSuggestionTabs: Set<AppSuggestionTabType>,
            val showAppLabels: Boolean,
            val appIconShape: AppIconShape,
            val launcherAppIcon: LauncherAppIcon = LauncherAppIcon.DEFAULT,
            val themedIconsEnabled: Boolean = true,
            val deviceThemeEnabled: Boolean = false,
            val amoledThemeEnabled: Boolean = false,
            val searchChromeBlurEnabled: Boolean = false,
            val accentColorMode: AccentColorMode = AccentColorMode.FROM_WALLPAPER,
            val customAccentColorArgb: Int =
                    UiPreferences.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
            val maskUnsupportedIconPackIcons: Boolean = true,
            val phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
            val appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
    )

    /**
     * Consolidated startup configuration that includes all data needed for app initialization. This
     * enables loading everything in a single batch operation for maximum performance.
     */
    data class StartupConfig(
            // Critical preferences (needed immediately for layout)
            val oneHandedMode: Boolean,
            // Cached apps metadata
            val cachedAppsLastUpdate: Long,
            // Full startup preferences (loaded in background)
            val startupPreferences: StartupPreferences,
    )

    /** Reads the authoritative preferences so reloads see changes made since startup. */
    fun getStartupPreferences(): StartupPreferences =
        StartupConfigReader(context).readPreferences(defaultDirectDialEnabled = false)

    fun loadStartupConfig(): StartupConfig = StartupConfigReader(context).loadStartupConfig()
}
