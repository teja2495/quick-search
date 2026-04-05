package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.StartupPreferencesFacade
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType

internal data class SearchPreferenceCache(
    val enabledFileTypes: Set<FileType> = emptySet(),
    val showFolders: Boolean = false,
    val showSystemFiles: Boolean = false,
    val folderWhitelistPatterns: Set<String> = emptySet(),
    val folderBlacklistPatterns: Set<String> = emptySet(),
    val excludedFileExtensions: Set<String> = emptySet(),
    val oneHandedMode: Boolean = false,
    val bottomSearchBarEnabled: Boolean = false,
    val topResultIndicatorEnabled: Boolean = true,
    val wallpaperAccentEnabled: Boolean = true,
    val openKeyboardOnLaunch: Boolean = true,
    val overlayModeEnabled: Boolean = false,
    val autoCloseOverlay: Boolean = true,
    val directDialEnabled: Boolean = false,
    val assistantLaunchVoiceModeEnabled: Boolean = false,
    val hasSeenDirectDialChoice: Boolean = false,
    val appSuggestionsEnabled: Boolean = true,
    val showAppLabels: Boolean = true,
    val phoneAppGridColumns: Int = UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
    val appIconShape: AppIconShape = AppIconShape.DEFAULT,
    val launcherAppIcon: LauncherAppIcon = LauncherAppIcon.DEFAULT,
    val themedIconsEnabled: Boolean = false,
    val maskUnsupportedIconPackIcons: Boolean = false,
    val wallpaperBackgroundAlpha: Float = UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
    val wallpaperBlurRadius: Float = UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS,
    val appTheme: AppTheme = AppTheme.MONOCHROME,
    val overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
    val fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER,
    val backgroundSource: BackgroundSource = BackgroundSource.THEME,
    val customImageUri: String? = null,
    val lockedShortcutTarget: SearchTarget? = null,
    val lockedAliasSearchSection: SearchSection? = null,
    val lockedToolMode: SearchToolType? = null,
    val lockedCurrencyConverterAlias: Boolean = false,
    val lockedWordClockAlias: Boolean = false,
    val lockedDictionaryAlias: Boolean = false,
    val clearQueryOnLaunch: Boolean = false,
    val amazonDomain: String? = null,
) {
    companion object {
        fun from(
            config: StartupPreferencesFacade.StartupConfig,
            wallpaperAccentEnabled: Boolean,
            assistantLaunchVoiceModeEnabled: Boolean,
        ): SearchPreferenceCache {
            return from(
                prefs = config.startupPreferences,
                wallpaperAccentEnabled = wallpaperAccentEnabled,
                assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
            ).copy(oneHandedMode = config.oneHandedMode)
        }

        fun from(
            prefs: StartupPreferencesFacade.StartupPreferences,
            wallpaperAccentEnabled: Boolean,
            assistantLaunchVoiceModeEnabled: Boolean,
        ): SearchPreferenceCache {
            return SearchPreferenceCache(
                enabledFileTypes = prefs.enabledFileTypes,
                showFolders = prefs.showFolders,
                showSystemFiles = prefs.showSystemFiles,
                folderWhitelistPatterns = prefs.folderWhitelistPatterns,
                folderBlacklistPatterns = prefs.folderBlacklistPatterns,
                excludedFileExtensions = prefs.excludedFileExtensions,
                oneHandedMode = prefs.oneHandedMode,
                bottomSearchBarEnabled = prefs.bottomSearchBarEnabled,
                topResultIndicatorEnabled = prefs.topResultIndicatorEnabled,
                wallpaperAccentEnabled = wallpaperAccentEnabled,
                openKeyboardOnLaunch = prefs.openKeyboardOnLaunch,
                overlayModeEnabled = prefs.overlayModeEnabled,
                autoCloseOverlay = prefs.autoCloseOverlay,
                directDialEnabled = prefs.directDialEnabled,
                assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                hasSeenDirectDialChoice = prefs.hasSeenDirectDialChoice,
                appSuggestionsEnabled = prefs.appSuggestionsEnabled,
                showAppLabels = prefs.showAppLabels,
                phoneAppGridColumns = prefs.phoneAppGridColumns,
                appIconShape = prefs.appIconShape,
                launcherAppIcon = prefs.launcherAppIcon,
                themedIconsEnabled = prefs.themedIconsEnabled,
                maskUnsupportedIconPackIcons = prefs.maskUnsupportedIconPackIcons,
                wallpaperBackgroundAlpha = prefs.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = prefs.wallpaperBlurRadius,
                appTheme = prefs.appTheme,
                overlayThemeIntensity = prefs.overlayThemeIntensity.coerceIn(
                    UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                    UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
                ),
                fontScaleMultiplier = prefs.fontScaleMultiplier.coerceIn(
                    UiPreferences.MIN_FONT_SCALE_MULTIPLIER,
                    UiPreferences.MAX_FONT_SCALE_MULTIPLIER,
                ),
                backgroundSource = prefs.backgroundSource,
                customImageUri = prefs.customImageUri,
                clearQueryOnLaunch = prefs.clearQueryOnLaunch,
                amazonDomain = prefs.amazonDomain,
            )
        }
    }
}
