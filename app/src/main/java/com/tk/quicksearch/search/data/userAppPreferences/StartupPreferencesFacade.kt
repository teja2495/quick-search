package com.tk.quicksearch.search.data

import android.content.Context
import android.content.res.Configuration
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.data.UserAppPreferences

/**
 * Facade for startup preference operations
 */
class StartupPreferencesFacade(
    private val parent: UserAppPreferences,
    private val context: Context
) {

    /**
     * Data class to hold all preferences needed during app startup for performance optimization.
     */
    data class StartupPreferences(
            val enabledFileTypes: Set<FileType>,
            val showFolders: Boolean,
            val showSystemFiles: Boolean,
            val folderWhitelistPatterns: Set<String>,
            val folderBlacklistPatterns: Set<String>,
            val excludedFileExtensions: Set<String>,
            val oneHandedMode: Boolean,
            val bottomSearchBarEnabled: Boolean,
            val topResultIndicatorEnabled: Boolean,
            val openKeyboardOnLaunch: Boolean,
            val clearQueryOnLaunch: Boolean,
            val autoCloseOverlay: Boolean,
            val overlayModeEnabled: Boolean,
            val overlayBlurEffectEnabled: Boolean = false,
            val directDialEnabled: Boolean,
            val hasSeenDirectDialChoice: Boolean,
            val hasSeenSearchEngineOnboarding: Boolean,
            val wallpaperBackgroundAlpha: Float,
            val wallpaperBlurRadius: Float,
            val appTheme: AppTheme,
            val overlayThemeIntensity: Float,
            val fontScaleMultiplier: Float,
            val backgroundSource: BackgroundSource,
            val customImageUri: String?,
            val amazonDomain: String?,
            val pinnedPackages: Set<String>,
            val suggestionHiddenPackages: Set<String>,
            val resultHiddenPackages: Set<String>,
            val searchHistoryEnabled: Boolean,
            val appSuggestionsEnabled: Boolean,
            val showAppLabels: Boolean,
            val appIconShape: AppIconShape,
            val launcherAppIcon: LauncherAppIcon = LauncherAppIcon.AUTO,
            val themedIconsEnabled: Boolean = true,
            val phoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
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

    private fun computeIsDarkMode(allPrefs: Map<String, *>): Boolean {
        val modeName =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_APP_THEME_MODE,
                ] as? String
        val mode = modeName?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM
        return when (mode) {
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
            AppThemeMode.SYSTEM -> {
                val nightModeFlags =
                        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    /**
     * Optimized: Loads all preferences needed during startup in a single batch operation. Uses
     * SharedPreferences.getAll() to minimize disk I/O operations.
     */
    fun getStartupPreferences(): StartupPreferences {
        // Batch read all preferences at once
        val prefs =
                context.getSharedPreferences(
                        com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE,
                )
        val allPrefs = prefs.all

        // Parse values from the batch read
        val enabledFileTypesNames =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_ENABLED_FILE_TYPES,
                ] as? @Suppress("UNCHECKED_CAST")
                        Set<String>
        val enabledFileTypes =
                if (enabledFileTypesNames == null) {
                    // Default: all file types enabled
                    com.tk.quicksearch.search.models.FileType.values().toSet()
                } else {
                    enabledFileTypesNames
                        .mapNotNull { name -> com.tk.quicksearch.search.models.FileType.values().find { it.name == name } }
                        .toSet()
                }

        return StartupPreferences(
                enabledFileTypes = enabledFileTypes,
                showFolders =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_SHOW_FOLDERS_IN_RESULTS,
                        ] as?
                                Boolean
                                ?: false,
                showSystemFiles =
                        (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SHOW_SYSTEM_FILES,
                                ] as?
                                        Boolean
                                ?: false
                        ) || (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SHOW_HIDDEN_FILES,
                                ] as?
                                        Boolean
                                ?: false
                        ),
                folderWhitelistPatterns =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_FOLDER_WHITELIST_PATTERNS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                folderBlacklistPatterns =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_FOLDER_BLACKLIST_PATTERNS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                excludedFileExtensions =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_EXCLUDED_FILE_EXTENSIONS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                oneHandedMode =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_ONE_HANDED_MODE,
                        ] as?
                                Boolean
                                ?: false,
                bottomSearchBarEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_BOTTOM_SEARCH_BAR_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
                topResultIndicatorEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_TOP_RESULT_INDICATOR_ENABLED,
                        ] as?
                                Boolean
                                ?: true,
                openKeyboardOnLaunch =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_OPEN_KEYBOARD_ON_LAUNCH,
                        ] as?
                                Boolean
                                ?: true,
                clearQueryOnLaunch =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_CLEAR_QUERY_ON_LAUNCH,
                        ] as?
                                Boolean
                                ?: true,
                autoCloseOverlay =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_AUTO_CLOSE_OVERLAY,
                        ] as?
                                Boolean
                                ?: true,
                overlayModeEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_OVERLAY_MODE_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
                overlayBlurEffectEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_OVERLAY_BLUR_EFFECT_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
                directDialEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_DIRECT_DIAL_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
                hasSeenDirectDialChoice =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_DIRECT_DIAL_CHOICE_SHOWN,
                        ] as?
                                Boolean
                                ?: false,
                hasSeenSearchEngineOnboarding =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_SEARCH_ENGINE_ONBOARDING_SEEN,
                        ] as?
                                Boolean
                                ?: false,
                wallpaperBackgroundAlpha =
                        run {
                            val isDark = computeIsDarkMode(allPrefs)
                            val key = if (isDark)
                                com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA
                            else
                                com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT
                            val default = if (isDark)
                                com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
                            else
                                com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA_LIGHT
                            allPrefs[key] as? Float ?: default
                        },
                wallpaperBlurRadius =
                        run {
                            val isDark = computeIsDarkMode(allPrefs)
                            val key = if (isDark)
                                com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BLUR_RADIUS
                            else
                                com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BLUR_RADIUS_LIGHT
                            val default = if (isDark)
                                com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
                            else
                                com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS_LIGHT
                            allPrefs[key] as? Float ?: default
                        },
                appTheme =
                        (
                                (allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_APP_THEME,
                                ] as? String)
                                        ?: (allPrefs[
                                                com.tk.quicksearch.search.data.preferences
                                                        .UiPreferences.KEY_OVERLAY_GRADIENT_THEME,
                                        ] as? String)
                                )
                                ?.let { value ->
                                    runCatching { AppTheme.valueOf(value) }.getOrNull()
                                }
                                ?: AppTheme.MONOCHROME,
                overlayThemeIntensity =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_OVERLAY_THEME_INTENSITY,
                        ] as?
                                Float
                                ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .DEFAULT_OVERLAY_THEME_INTENSITY,
                fontScaleMultiplier =
                        (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_FONT_SCALE_MULTIPLIER,
                                ] as?
                                        Float
                                ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .DEFAULT_FONT_SCALE_MULTIPLIER
                        )
                                .coerceIn(
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .MIN_FONT_SCALE_MULTIPLIER,
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .MAX_FONT_SCALE_MULTIPLIER,
                                ),
                backgroundSource =
                        (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_BACKGROUND_SOURCE,
                                ] as?
                                        String
                                )
                                ?.let { value ->
                                    runCatching { BackgroundSource.valueOf(value) }
                                            .getOrNull()
                                }
                                ?: if (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_OVERLAY_MODE_ENABLED,
                                        ] as?
                                                Boolean
                                                ?: false
                                ) {
                                    BackgroundSource.THEME
                                } else {
                                    BackgroundSource.SYSTEM_WALLPAPER
                                },
                customImageUri =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_CUSTOM_IMAGE_URI,
                        ] as?
                                String,
                amazonDomain =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_AMAZON_DOMAIN,
                        ] as?
                                String,
                pinnedPackages =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_PINNED,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                suggestionHiddenPackages =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_HIDDEN_SUGGESTIONS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                resultHiddenPackages =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_HIDDEN_RESULTS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                searchHistoryEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_RECENT_QUERIES_ENABLED,
                        ] as?
                                Boolean
                                ?: true,
                appSuggestionsEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_APP_SUGGESTIONS_ENABLED,
                        ] as?
                                Boolean
                                ?: true,
                showAppLabels =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_SHOW_APP_LABELS,
                        ] as?
                                Boolean
                                ?: true,
                appIconShape =
                        (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_APP_ICON_SHAPE,
                                ] as?
                                        String
                                )
                                ?.let { value ->
                                    runCatching { AppIconShape.valueOf(value) }.getOrNull()
                                }
                                ?: AppIconShape.DEFAULT,
                launcherAppIcon =
                        (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_LAUNCHER_APP_ICON,
                                ] as?
                                        String
                                )
                                ?.let { value ->
                                    runCatching { LauncherAppIcon.valueOf(value) }.getOrNull()
                                }
                                ?: LauncherAppIcon.AUTO,
                themedIconsEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_THEMED_ICONS_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
        )
    }

    /**
     * Loads all startup configuration in a single atomic operation for maximum performance. This
     * consolidates critical preferences, cached apps data, and startup preferences into one batch
     * read operation, minimizing disk I/O during app launch.
     */
    fun loadStartupConfig(): StartupConfig {
        // Get user preferences in one batch read
        val prefs =
                context.getSharedPreferences(
                        com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE,
                )
        val allPrefs = prefs.all

        // Get app cache metadata in separate read (different SharedPreferences file)
        val appCachePrefs =
                context.getSharedPreferences(
                        "app_cache",
                        android.content.Context.MODE_PRIVATE,
                )
        val cachedAppsLastUpdate = appCachePrefs.getLong("last_update", 0L)

        // Extract critical preference directly from batch read
        val oneHandedMode =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                .KEY_ONE_HANDED_MODE,
                ] as?
                        Boolean
                        ?: false

        // Build startup preferences from the same batch read
        val enabledFileTypesNames =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_ENABLED_FILE_TYPES,
                ] as? @Suppress("UNCHECKED_CAST")
                        Set<String>
        val enabledFileTypes =
                if (enabledFileTypesNames == null) {
                    // Default: all file types enabled
                    com.tk.quicksearch.search.models.FileType.values().toSet()
                } else {
                    enabledFileTypesNames
                        .mapNotNull { name -> com.tk.quicksearch.search.models.FileType.values().find { it.name == name } }
                        .toSet()
                }

        val startupPreferences =
                StartupPreferences(
                        enabledFileTypes = enabledFileTypes,
                        showFolders =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SHOW_FOLDERS_IN_RESULTS,
                                ] as?
                                        Boolean
                                        ?: false,
                        showSystemFiles =
                                (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                                        .KEY_SHOW_SYSTEM_FILES,
                                        ] as?
                                                Boolean
                                        ?: false
                                ) || (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                                        .KEY_SHOW_HIDDEN_FILES,
                                        ] as?
                                                Boolean
                                        ?: false
                                ),
                        folderWhitelistPatterns =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_FOLDER_WHITELIST_PATTERNS,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        folderBlacklistPatterns =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_FOLDER_BLACKLIST_PATTERNS,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        excludedFileExtensions =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_EXCLUDED_FILE_EXTENSIONS,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        oneHandedMode = oneHandedMode,
                        bottomSearchBarEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_BOTTOM_SEARCH_BAR_ENABLED,
                                ] as?
                                        Boolean
                                        ?: false,
                        topResultIndicatorEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_TOP_RESULT_INDICATOR_ENABLED,
                                ] as?
                                        Boolean
                                        ?: true,
                        openKeyboardOnLaunch =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OPEN_KEYBOARD_ON_LAUNCH,
                                ] as?
                                        Boolean
                                        ?: true,
                        clearQueryOnLaunch =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_CLEAR_QUERY_ON_LAUNCH,
                                ] as?
                                        Boolean
                                        ?: true,
                        autoCloseOverlay =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_AUTO_CLOSE_OVERLAY,
                                ] as?
                                        Boolean
                                        ?: true,
                        overlayModeEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OVERLAY_MODE_ENABLED,
                                ] as?
                                        Boolean
                                        ?: false,
                        overlayBlurEffectEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OVERLAY_BLUR_EFFECT_ENABLED,
                                ] as?
                                        Boolean
                                        ?: false,
                        directDialEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_DIRECT_DIAL_ENABLED,
                                ] as?
                                        Boolean
                                        ?: true,
                        hasSeenDirectDialChoice =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_DIRECT_DIAL_CHOICE_SHOWN,
                                ] as?
                                        Boolean
                                ?: false,
                        hasSeenSearchEngineOnboarding =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SEARCH_ENGINE_ONBOARDING_SEEN,
                                ] as?
                                        Boolean
                                ?: false,
                        wallpaperBackgroundAlpha =
                                run {
                                    val isDark = computeIsDarkMode(allPrefs)
                                    val key = if (isDark)
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA
                                    else
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT
                                    val default = if (isDark)
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
                                    else
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA_LIGHT
                                    allPrefs[key] as? Float ?: default
                                },
                        wallpaperBlurRadius =
                                run {
                                    val isDark = computeIsDarkMode(allPrefs)
                                    val key = if (isDark)
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BLUR_RADIUS
                                    else
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.KEY_WALLPAPER_BLUR_RADIUS_LIGHT
                                    val default = if (isDark)
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
                                    else
                                        com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS_LIGHT
                                    allPrefs[key] as? Float ?: default
                                },
                        appTheme =
                                (
                                        (allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_APP_THEME,
                                        ] as? String)
                                                ?: (allPrefs[
                                                        com.tk.quicksearch.search.data.preferences
                                                                .UiPreferences.KEY_OVERLAY_GRADIENT_THEME,
                                                ] as? String)
                                        )
                                        ?.let { value ->
                                            runCatching { AppTheme.valueOf(value) }
                                                    .getOrNull()
                                        }
                                        ?: AppTheme.MONOCHROME,
                        overlayThemeIntensity =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OVERLAY_THEME_INTENSITY,
                                ] as?
                                        Float
                                ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .DEFAULT_OVERLAY_THEME_INTENSITY,
                        fontScaleMultiplier =
                                (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_FONT_SCALE_MULTIPLIER,
                                        ] as?
                                                Float
                                        ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .DEFAULT_FONT_SCALE_MULTIPLIER
                                )
                                .coerceIn(
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .MIN_FONT_SCALE_MULTIPLIER,
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .MAX_FONT_SCALE_MULTIPLIER,
                                ),
                        backgroundSource =
                                (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_BACKGROUND_SOURCE,
                                        ] as?
                                                String
                                        )
                                        ?.let { value ->
                                            runCatching {
                                                        BackgroundSource.valueOf(value)
                                                    }
                                                    .getOrNull()
                                        }
                                        ?: if (
                                                allPrefs[
                                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                                .KEY_OVERLAY_MODE_ENABLED,
                                                ] as?
                                                        Boolean
                                                        ?: false
                                        ) {
                                            BackgroundSource.THEME
                                        } else {
                                            BackgroundSource.SYSTEM_WALLPAPER
                                        },
                        customImageUri =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_CUSTOM_IMAGE_URI,
                                ] as?
                                        String,
                        amazonDomain =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_AMAZON_DOMAIN,
                                ] as?
                                        String,
                        pinnedPackages =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_PINNED,
                                ] as?
                                        Set<String>
                                ?: emptySet(),
                        suggestionHiddenPackages =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_HIDDEN_SUGGESTIONS,
                                ] as?
                                        Set<String>
                                ?: emptySet(),
                        resultHiddenPackages =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_HIDDEN_RESULTS,
                                ] as?
                                        Set<String>
                                ?: emptySet(),
                        searchHistoryEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_RECENT_QUERIES_ENABLED,
                                ] as?
                                        Boolean
                                ?: true,
                        appSuggestionsEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_APP_SUGGESTIONS_ENABLED,
                                ] as?
                                        Boolean
                                ?: true,
                        showAppLabels =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_SHOW_APP_LABELS,
                                ] as?
                                        Boolean
                                ?: true,
                        appIconShape =
                                (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_APP_ICON_SHAPE,
                                        ] as?
                                                String
                                        )
                                        ?.let { value ->
                                            runCatching { AppIconShape.valueOf(value) }.getOrNull()
                                        }
                                        ?: AppIconShape.DEFAULT,
                        launcherAppIcon =
                                (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_LAUNCHER_APP_ICON,
                                        ] as?
                                                String
                                        )
                                        ?.let { value ->
                                            runCatching { LauncherAppIcon.valueOf(value) }.getOrNull()
                                        }
                                        ?: LauncherAppIcon.AUTO,
                        themedIconsEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_THEMED_ICONS_ENABLED,
                                ] as?
                                        Boolean
                                        ?: false,
                        phoneAppGridColumns =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_PHONE_APP_GRID_COLUMNS,
                                ] as?
                                        Int
                                        ?: com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
                )

        return StartupConfig(
                oneHandedMode = oneHandedMode,
                cachedAppsLastUpdate = cachedAppsLastUpdate,
                startupPreferences = startupPreferences,
        )
    }
}
