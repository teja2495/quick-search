package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType

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
    var topResultIndicatorEnabled: Boolean = true
    var wallpaperAccentEnabled: Boolean = true
    var openKeyboardOnLaunch: Boolean = true
    var overlayModeEnabled: Boolean = false
    var autoCloseOverlay: Boolean = true
    var directDialEnabled: Boolean = false
    var assistantLaunchVoiceModeEnabled: Boolean = false
    var hasSeenDirectDialChoice: Boolean = false
    var appSuggestionsEnabled: Boolean = true
    var showAppLabels: Boolean = true
    var phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS
    var appIconShape: AppIconShape = AppIconShape.DEFAULT
    var launcherAppIcon: LauncherAppIcon = LauncherAppIcon.AUTO
    var themedIconsEnabled: Boolean = false
    var maskUnsupportedIconPackIcons: Boolean = false
    var wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
    var wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
    var appTheme: AppTheme = AppTheme.MONOCHROME
    var overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY
    var fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER
    var backgroundSource: BackgroundSource = BackgroundSource.THEME
    var customImageUri: String? = null
    var lockedShortcutTarget: SearchTarget? = null
    var lockedAliasSearchSection: SearchSection? = null
    var lockedToolMode: SearchToolType? = null
    var lockedCurrencyConverterAlias: Boolean = false
    var lockedWordClockAlias: Boolean = false
    var lockedDictionaryAlias: Boolean = false
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
        topResultIndicatorEnabled = prefCache.topResultIndicatorEnabled
        wallpaperAccentEnabled = prefCache.wallpaperAccentEnabled
        openKeyboardOnLaunch = prefCache.openKeyboardOnLaunch
        overlayModeEnabled = prefCache.overlayModeEnabled
        autoCloseOverlay = prefCache.autoCloseOverlay
        directDialEnabled = prefCache.directDialEnabled
        assistantLaunchVoiceModeEnabled = prefCache.assistantLaunchVoiceModeEnabled
        hasSeenDirectDialChoice = prefCache.hasSeenDirectDialChoice
        appSuggestionsEnabled = prefCache.appSuggestionsEnabled
        showAppLabels = prefCache.showAppLabels
        phoneAppGridColumns = prefCache.phoneAppGridColumns
        appIconShape = prefCache.appIconShape
        launcherAppIcon = prefCache.launcherAppIcon
        themedIconsEnabled = prefCache.themedIconsEnabled
        maskUnsupportedIconPackIcons = prefCache.maskUnsupportedIconPackIcons
        wallpaperBackgroundAlpha = prefCache.wallpaperBackgroundAlpha
        wallpaperBlurRadius = prefCache.wallpaperBlurRadius
        appTheme = prefCache.appTheme
        overlayThemeIntensity = prefCache.overlayThemeIntensity
        fontScaleMultiplier = prefCache.fontScaleMultiplier
        backgroundSource = prefCache.backgroundSource
        customImageUri = prefCache.customImageUri
        lockedShortcutTarget = prefCache.lockedShortcutTarget
        lockedAliasSearchSection = prefCache.lockedAliasSearchSection
        lockedToolMode = prefCache.lockedToolMode
        lockedCurrencyConverterAlias = prefCache.lockedCurrencyConverterAlias
        lockedWordClockAlias = prefCache.lockedWordClockAlias
        lockedDictionaryAlias = prefCache.lockedDictionaryAlias
        clearQueryOnLaunch = prefCache.clearQueryOnLaunch
        amazonDomain = prefCache.amazonDomain
    }
}
