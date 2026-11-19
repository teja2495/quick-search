package com.tk.quicksearch.search.data.userAppPreferences

import com.tk.quicksearch.search.appSettings.readBooleanSettings
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.app.LauncherAppIcon
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType

/** Decodes the same preference snapshot for cold startup and subsequent reloads. */
internal fun decodeStartupPreferences(
    allPrefs: Map<String, *>,
    systemDark: Boolean,
    defaultDirectDialEnabled: Boolean,
): StartupPreferencesFacade.StartupPreferences {
    fun boolean(key: String, default: Boolean = false): Boolean = allPrefs[key] as? Boolean ?: default
    fun strings(key: String): Set<String> = allPrefs[key].asPreferenceStringSet().orEmpty()

    val isDarkMode = when (allPrefs.readEnum(UiPreferences.KEY_APP_THEME_MODE, AppThemeMode.SYSTEM)) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }
    val savedFileTypes = allPrefs[BasePreferences.KEY_ENABLED_FILE_TYPES].asPreferenceStringSet()
    val enabledFileTypes = savedFileTypes
        ?.mapNotNull { name -> FileType.entries.firstOrNull { it.name == name } }
        ?.toSet()
        ?: FileType.entries.toSet()
    val overlayModeEnabled = boolean(UiPreferences.KEY_OVERLAY_MODE_ENABLED)
    val wallpaperAlpha = if (isDarkMode) {
        allPrefs[UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA] as? Float
            ?: UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
    } else {
        allPrefs[UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT] as? Float
            ?: UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA_LIGHT
    }
    val wallpaperBlur = if (isDarkMode) {
        allPrefs[UiPreferences.KEY_WALLPAPER_BLUR_RADIUS] as? Float
            ?: UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
    } else {
        allPrefs[UiPreferences.KEY_WALLPAPER_BLUR_RADIUS_LIGHT] as? Float
            ?: UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS_LIGHT
    }

    return StartupPreferencesFacade.StartupPreferences(
        enabledFileTypes = enabledFileTypes,
        showFolders = boolean(BasePreferences.KEY_SHOW_FOLDERS_IN_RESULTS),
        booleanSettings = readBooleanSettings(allPrefs),
        showSystemFiles = boolean(BasePreferences.KEY_SHOW_SYSTEM_FILES),
        folderWhitelistPatterns = strings(BasePreferences.KEY_FOLDER_WHITELIST_PATTERNS),
        folderBlacklistPatterns = strings(BasePreferences.KEY_FOLDER_BLACKLIST_PATTERNS),
        excludedFileExtensions = strings(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS),
        oneHandedMode = boolean(UiPreferences.KEY_ONE_HANDED_MODE),
        bottomSearchBarEnabled = boolean(UiPreferences.KEY_BOTTOM_SEARCH_BAR_ENABLED),
        unifiedPinnedItemsEnabled = boolean(UiPreferences.KEY_UNIFIED_PINNED_ITEMS_ENABLED),
        searchHintsEnabled = boolean(UiPreferences.KEY_SEARCH_HINTS_ENABLED, true),
        settingsIconEnabled = boolean(UiPreferences.KEY_SETTINGS_ICON_ENABLED, true),
        topResultIndicatorEnabled = boolean(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED),
        openTopResultUsingKeyboardEnabled = boolean(UiPreferences.KEY_OPEN_TOP_RESULT_USING_KEYBOARD_ENABLED, true),
        openKeyboardOnLaunch = boolean(UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH, true),
        clearQueryOnLaunch = boolean(UiPreferences.KEY_CLEAR_QUERY_ON_LAUNCH, true),
        autoCloseOverlay = boolean(UiPreferences.KEY_AUTO_CLOSE_OVERLAY, true),
        overlayModeEnabled = overlayModeEnabled,
        directDialEnabled =
            boolean(BasePreferences.KEY_DIRECT_DIAL_ENABLED, defaultDirectDialEnabled),
        hasSeenDirectDialChoice = boolean(BasePreferences.KEY_DIRECT_DIAL_CHOICE_SHOWN),
        wallpaperBackgroundAlpha = wallpaperAlpha,
        wallpaperBlurRadius = wallpaperBlur,
        appTheme = allPrefs.readEnum(UiPreferences.KEY_APP_THEME, AppTheme.MONOCHROME),
        overlayThemeIntensity = allPrefs[UiPreferences.KEY_OVERLAY_THEME_INTENSITY] as? Float
            ?: UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
        fontScaleMultiplier =
            (allPrefs[UiPreferences.KEY_FONT_SCALE_MULTIPLIER] as? Float
                ?: UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER)
                .coerceIn(UiPreferences.MIN_FONT_SCALE_MULTIPLIER, UiPreferences.MAX_FONT_SCALE_MULTIPLIER),
        useSystemFont = boolean(UiPreferences.KEY_USE_SYSTEM_FONT),
        backgroundSource = allPrefs.readEnum(
            UiPreferences.KEY_BACKGROUND_SOURCE,
            if (overlayModeEnabled) BackgroundSource.THEME else BackgroundSource.SYSTEM_WALLPAPER,
        ),
        customImageUri = allPrefs[UiPreferences.KEY_CUSTOM_IMAGE_URI] as? String,
        amazonDomain = allPrefs[BasePreferences.KEY_AMAZON_DOMAIN] as? String,
        pinnedPackages = strings(BasePreferences.KEY_PINNED),
        suggestionHiddenPackages = strings(BasePreferences.KEY_HIDDEN_SUGGESTIONS),
        resultHiddenPackages = strings(BasePreferences.KEY_HIDDEN_RESULTS),
        searchHistoryEnabled = boolean(UiPreferences.KEY_RECENT_QUERIES_ENABLED, true),
        appSuggestionsEnabled = boolean(UiPreferences.KEY_APP_SUGGESTIONS_ENABLED, true),
        showAllAppsButton = boolean(UiPreferences.KEY_SHOW_ALL_APPS_BUTTON),
        includeNonLaunchableAppsInSearch = boolean(UiPreferences.KEY_INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH),
        selectedAppSuggestionTab =
            allPrefs.readEnum(UiPreferences.KEY_SELECTED_APP_SUGGESTION_TAB, AppSuggestionTabType.RECENTS),
        enabledAppSuggestionTabs =
            AppSuggestionTabType.parseEnabledTabs(allPrefs[UiPreferences.KEY_ENABLED_APP_SUGGESTION_TABS] as? Set<*>),
        showAppLabels = boolean(UiPreferences.KEY_SHOW_APP_LABELS, true),
        appIconShape = allPrefs.readEnum(UiPreferences.KEY_APP_ICON_SHAPE, AppIconShape.DEFAULT),
        launcherAppIcon = allPrefs.readEnum(UiPreferences.KEY_LAUNCHER_APP_ICON, LauncherAppIcon.DEFAULT),
        themedIconsEnabled = boolean(UiPreferences.KEY_THEMED_ICONS_ENABLED),
        deviceThemeEnabled = boolean(UiPreferences.KEY_DEVICE_THEME_ENABLED),
        amoledThemeEnabled = boolean(UiPreferences.KEY_AMOLED_THEME_ENABLED),
        searchChromeBlurEnabled = boolean(UiPreferences.KEY_SEARCH_CHROME_BLUR_ENABLED),
        accentColorMode = UiPreferences.resolveAccentColorMode(
            savedModeName = allPrefs[UiPreferences.KEY_ACCENT_COLOR_MODE] as? String,
            wallpaperAccentEnabled = boolean(UiPreferences.KEY_WALLPAPER_ACCENT_ENABLED, true),
        ),
        customAccentColorArgb = allPrefs[UiPreferences.KEY_CUSTOM_ACCENT_COLOR_ARGB] as? Int
            ?: UiPreferences.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
        maskUnsupportedIconPackIcons = boolean(UiPreferences.KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED, true),
        phoneAppGridColumns = allPrefs[UiPreferences.KEY_PHONE_APP_GRID_COLUMNS] as? Int
            ?: UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
        appIconSizeStep = allPrefs[UiPreferences.KEY_APP_ICON_SIZE_STEP] as? Int
            ?: UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
    )
}

private inline fun <reified T : Enum<T>> Map<String, *>.readEnum(key: String, default: T): T =
    (get(key) as? String)?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: default

// SharedPreferences stores only strings in set-valued entries.
@Suppress("UNCHECKED_CAST")
private fun Any?.asPreferenceStringSet(): Set<String>? = this as? Set<String>
