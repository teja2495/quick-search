package com.tk.quicksearch.search.core

import com.tk.quicksearch.app.LauncherAppIcon
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.tools.inlineSearch.SearchToolType

internal class SearchViewModelLegacyPreferenceState(
    clearQueryOnLaunch: Boolean,
) {
    var enabledFileTypes: Set<FileType> = emptySet()
    var showFolders: Boolean = false
    var showSystemFiles: Boolean = false
    var folderWhitelistPatterns: Set<String> = emptySet()
    var folderBlacklistPatterns: Set<String> = emptySet()
    var excludedFileExtensions: Set<String> = emptySet()
    var oneHandedMode: Boolean = false
    var bottomSearchBarEnabled: Boolean = false
    var unifiedPinnedItemsEnabled: Boolean = false
    var searchHintsEnabled: Boolean = true
    var settingsIconEnabled: Boolean = true
    var topResultIndicatorEnabled: Boolean = false
    var openTopResultUsingKeyboardEnabled: Boolean = true
    var accentColorMode: AccentColorMode = AccentColorMode.FROM_WALLPAPER
    var customAccentColorArgb: Int = UiPreferences.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB
    var openKeyboardOnLaunch: Boolean = true
    var overlayModeEnabled: Boolean = false
    var autoCloseOverlay: Boolean = true
    var directDialEnabled: Boolean = false
    var assistantLaunchVoiceModeEnabled: Boolean = false
    var hasSeenDirectDialChoice: Boolean = false
    var appSuggestionsEnabled: Boolean = true
    var showAllAppsButton: Boolean = false
    var showAppLabels: Boolean = true
    var phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS
    var appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP
    var appIconShape: AppIconShape = AppIconShape.DEFAULT
    var launcherAppIcon: LauncherAppIcon = LauncherAppIcon.DEFAULT
    var themedIconsEnabled: Boolean = false
    var deviceThemeEnabled: Boolean = false
    var amoledThemeEnabled: Boolean = false
    var searchChromeBlurEnabled: Boolean = true
    var maskUnsupportedIconPackIcons: Boolean = true
    var wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
    var wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
    var appTheme: AppTheme = AppTheme.MONOCHROME
    var overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY
    var fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER
    var useSystemFont: Boolean = false
    var homeTextColorOverride: HomeTextColor? = null
    var backgroundSource: BackgroundSource = BackgroundSource.THEME
    var customImageUri: String? = null
    var lockedShortcutTarget: SearchTarget? = null
    var lockedAliasSearchSection: SearchSection? = null
    var lockedToolMode: SearchToolType? = null
    var lockedCurrencyConverterAlias: Boolean = false
    var lockedWorldClockAlias: Boolean = false
    var lockedDictionaryAlias: Boolean = false
    var lockedWeatherAlias: Boolean = false
    var lockedCustomToolId: String? = null
    var lockedTaskerIntentId: String? = null
    var clearQueryOnLaunch: Boolean = clearQueryOnLaunch
    var amazonDomain: String? = null

    fun applyPreferenceCacheToLegacyVars(prefCache: SearchPreferenceCache) {
        enabledFileTypes = prefCache.enabledFileTypes
        showFolders = prefCache.showFolders
        showSystemFiles = prefCache.showSystemFiles
        folderWhitelistPatterns = prefCache.folderWhitelistPatterns
        folderBlacklistPatterns = prefCache.folderBlacklistPatterns
        excludedFileExtensions = prefCache.excludedFileExtensions
        oneHandedMode = prefCache.oneHandedMode
        bottomSearchBarEnabled = prefCache.bottomSearchBarEnabled
        unifiedPinnedItemsEnabled = prefCache.unifiedPinnedItemsEnabled
        searchHintsEnabled = prefCache.searchHintsEnabled
        settingsIconEnabled = prefCache.settingsIconEnabled
        topResultIndicatorEnabled = prefCache.topResultIndicatorEnabled
        openTopResultUsingKeyboardEnabled = prefCache.openTopResultUsingKeyboardEnabled
        accentColorMode = prefCache.accentColorMode
        customAccentColorArgb = prefCache.customAccentColorArgb
        openKeyboardOnLaunch = prefCache.openKeyboardOnLaunch
        overlayModeEnabled = prefCache.overlayModeEnabled
        autoCloseOverlay = prefCache.autoCloseOverlay
        directDialEnabled = prefCache.directDialEnabled
        assistantLaunchVoiceModeEnabled = prefCache.assistantLaunchVoiceModeEnabled
        hasSeenDirectDialChoice = prefCache.hasSeenDirectDialChoice
        appSuggestionsEnabled = prefCache.appSuggestionsEnabled
        showAllAppsButton = prefCache.showAllAppsButton
        showAppLabels = prefCache.showAppLabels
        phoneAppGridColumns = prefCache.phoneAppGridColumns
        appIconSizeStep = prefCache.appIconSizeStep
        appIconShape = prefCache.appIconShape
        launcherAppIcon = prefCache.launcherAppIcon
        themedIconsEnabled = prefCache.themedIconsEnabled
        deviceThemeEnabled = prefCache.deviceThemeEnabled
        amoledThemeEnabled = prefCache.amoledThemeEnabled
        searchChromeBlurEnabled = prefCache.searchChromeBlurEnabled
        maskUnsupportedIconPackIcons = prefCache.maskUnsupportedIconPackIcons
        wallpaperBackgroundAlpha = prefCache.wallpaperBackgroundAlpha
        wallpaperBlurRadius = prefCache.wallpaperBlurRadius
        appTheme = prefCache.appTheme
        overlayThemeIntensity = prefCache.overlayThemeIntensity
        fontScaleMultiplier = prefCache.fontScaleMultiplier
        useSystemFont = prefCache.useSystemFont
        backgroundSource = prefCache.backgroundSource
        customImageUri = prefCache.customImageUri
        lockedShortcutTarget = prefCache.lockedShortcutTarget
        lockedAliasSearchSection = prefCache.lockedAliasSearchSection
        lockedToolMode = prefCache.lockedToolMode
        lockedCurrencyConverterAlias = prefCache.lockedCurrencyConverterAlias
        lockedWorldClockAlias = prefCache.lockedWorldClockAlias
        lockedDictionaryAlias = prefCache.lockedDictionaryAlias
        lockedWeatherAlias = prefCache.lockedWeatherAlias
        clearQueryOnLaunch = prefCache.clearQueryOnLaunch
        amazonDomain = prefCache.amazonDomain
    }
}
