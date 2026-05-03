package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel

sealed interface SettingsCommand {
    data class Toggle(
        val key: AppSettingsToggleKey,
        val enabled: Boolean,
    ) : SettingsCommand

    data class WebSuggestionsCount(val count: Int) : SettingsCommand

    data class TopMatchesLimit(val limit: Int) : SettingsCommand

    data class TopMatchesSectionOrder(val order: List<SearchSection>) : SettingsCommand

    data class TopMatchesSectionEnabled(
        val section: SearchSection,
        val enabled: Boolean,
    ) : SettingsCommand

    data class PhoneAppGridColumns(val columns: Int) : SettingsCommand

    data class SearchEngineCompactRowCount(val rowCount: Int) : SettingsCommand

    data class FontScaleMultiplier(val multiplier: Float) : SettingsCommand

    data class UseSystemFont(val enabled: Boolean) : SettingsCommand

    data class WallpaperBackgroundAlpha(val alpha: Float) : SettingsCommand

    data class WallpaperBlurRadius(val radius: Float) : SettingsCommand

    data class OverlayThemeIntensity(val intensity: Float) : SettingsCommand

    data class AppThemeSetting(val theme: AppTheme) : SettingsCommand

    data class AppThemeModeSetting(val mode: AppThemeMode) : SettingsCommand

    data class BackgroundSourceSetting(val source: BackgroundSource) : SettingsCommand

    data class CustomImageUriSetting(val uri: String?) : SettingsCommand

    data class IconPackPackageSetting(val packageName: String?) : SettingsCommand

    data class AppIconShapeSetting(val shape: AppIconShape) : SettingsCommand

    data class LauncherAppIconSetting(val icon: LauncherAppIcon) : SettingsCommand
}

internal fun SearchViewModel.applySettingsCommand(command: SettingsCommand) {
    when (command) {
        is SettingsCommand.Toggle -> {
            val sectionToggle = SearchSectionRegistry.sectionForToggle(command.key)
            if (sectionToggle != null) {
                setSectionEnabled(sectionToggle, command.enabled)
                return
            }
            when (command.key) {
                AppSettingsToggleKey.OVERLAY_MODE -> setOverlayModeEnabled(command.enabled)
                AppSettingsToggleKey.ONE_HANDED_MODE -> setOneHandedMode(command.enabled)
                AppSettingsToggleKey.BOTTOM_SEARCHBAR -> setBottomSearchBarEnabled(command.enabled)
                AppSettingsToggleKey.APP_LABELS -> setShowAppLabels(command.enabled)
                AppSettingsToggleKey.SHOW_TODAY_EVENTS -> setShowTodayEvents(command.enabled)
                AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE ->
                    setSearchEngineCompactMode(command.enabled)
                AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX ->
                    setSearchEngineAliasSuffixEnabled(command.enabled)
                AppSettingsToggleKey.CALCULATOR -> setCalculatorEnabled(command.enabled)
                AppSettingsToggleKey.UNIT_CONVERTER -> setUnitConverterEnabled(command.enabled)
                AppSettingsToggleKey.DATE_CALCULATOR -> setDateCalculatorEnabled(command.enabled)
                AppSettingsToggleKey.APP_SUGGESTIONS -> setAppSuggestionsEnabled(command.enabled)
                AppSettingsToggleKey.WEB_SUGGESTIONS -> setWebSuggestionsEnabled(command.enabled)
                AppSettingsToggleKey.RECENT_QUERIES -> setRecentQueriesEnabled(command.enabled)
                AppSettingsToggleKey.TOP_MATCHES -> setTopMatchesEnabled(command.enabled)
                AppSettingsToggleKey.TOP_RESULT_INDICATOR ->
                    setTopResultIndicatorEnabled(command.enabled)
                AppSettingsToggleKey.OPEN_KEYBOARD -> setOpenKeyboardOnLaunchEnabled(command.enabled)
                AppSettingsToggleKey.CLEAR_QUERY -> setClearQueryOnLaunchEnabled(command.enabled)
                AppSettingsToggleKey.AUTO_CLOSE_OVERLAY ->
                    setAutoCloseOverlayEnabled(command.enabled)
                AppSettingsToggleKey.CIRCULAR_APP_ICONS ->
                    setAppIconShape(
                        if (command.enabled) {
                            AppIconShape.CIRCLE
                        } else {
                            AppIconShape.DEFAULT
                        },
                    )
                AppSettingsToggleKey.SHOW_FOLDERS -> setShowFolders(command.enabled)
                AppSettingsToggleKey.SHOW_SYSTEM_FILES -> setShowSystemFiles(command.enabled)
                AppSettingsToggleKey.DIRECT_DIAL -> setDirectDialEnabled(command.enabled)
                AppSettingsToggleKey.ASSISTANT_LAUNCH_VOICE_MODE ->
                    setAssistantLaunchVoiceModeEnabled(command.enabled)
                AppSettingsToggleKey.WALLPAPER_ACCENT ->
                    setWallpaperAccentEnabled(command.enabled)
                AppSettingsToggleKey.THEMED_ICONS -> setThemedIconsEnabled(command.enabled)
                AppSettingsToggleKey.DEVICE_THEME -> setDeviceThemeEnabled(command.enabled)
                AppSettingsToggleKey.USE_SYSTEM_FONT -> setUseSystemFont(command.enabled)
                AppSettingsToggleKey.DICTIONARY -> setDictionaryEnabled(command.enabled)
                AppSettingsToggleKey.APPS_PER_ROW -> Unit
                AppSettingsToggleKey.SEARCH_APPS,
                AppSettingsToggleKey.SEARCH_APP_SHORTCUTS,
                AppSettingsToggleKey.SEARCH_CONTACTS,
                AppSettingsToggleKey.SEARCH_FILES,
                AppSettingsToggleKey.SEARCH_DEVICE_SETTINGS,
                AppSettingsToggleKey.SEARCH_CALENDAR,
                AppSettingsToggleKey.SEARCH_NOTES,
                AppSettingsToggleKey.SEARCH_APP_SETTINGS,
                -> Unit
            }
        }

        is SettingsCommand.WebSuggestionsCount -> setWebSuggestionsCount(command.count)
        is SettingsCommand.TopMatchesLimit -> setTopMatchesLimit(command.limit)
        is SettingsCommand.TopMatchesSectionOrder -> setTopMatchesSectionOrder(command.order)
        is SettingsCommand.TopMatchesSectionEnabled ->
            setTopMatchesSectionEnabled(command.section, command.enabled)
        is SettingsCommand.PhoneAppGridColumns -> setPhoneAppGridColumns(command.columns)
        is SettingsCommand.SearchEngineCompactRowCount ->
            setSearchEngineCompactRowCount(command.rowCount)
        is SettingsCommand.FontScaleMultiplier -> setFontScaleMultiplier(command.multiplier)
        is SettingsCommand.UseSystemFont -> setUseSystemFont(command.enabled)
        is SettingsCommand.WallpaperBackgroundAlpha -> setWallpaperBackgroundAlpha(command.alpha)
        is SettingsCommand.WallpaperBlurRadius -> setWallpaperBlurRadius(command.radius)
        is SettingsCommand.OverlayThemeIntensity -> setOverlayThemeIntensity(command.intensity)
        is SettingsCommand.AppThemeSetting -> setAppTheme(command.theme)
        is SettingsCommand.AppThemeModeSetting -> setAppThemeMode(command.mode)
        is SettingsCommand.BackgroundSourceSetting -> setBackgroundSource(command.source)
        is SettingsCommand.CustomImageUriSetting -> setCustomImageUri(command.uri)
        is SettingsCommand.IconPackPackageSetting -> setIconPackPackage(command.packageName)
        is SettingsCommand.AppIconShapeSetting -> setAppIconShape(command.shape)
        is SettingsCommand.LauncherAppIconSetting -> setLauncherAppIcon(command.icon)
    }
}

internal fun SearchUiState.isAppSettingToggleEnabled(toggleKey: AppSettingsToggleKey): Boolean {
    SearchSectionRegistry.sectionForToggle(toggleKey)?.let { section ->
        return !disabledSections.contains(section)
    }

    return when (toggleKey) {
        AppSettingsToggleKey.OVERLAY_MODE -> overlayModeEnabled
        AppSettingsToggleKey.ONE_HANDED_MODE -> oneHandedMode
        AppSettingsToggleKey.BOTTOM_SEARCHBAR -> bottomSearchBarEnabled
        AppSettingsToggleKey.APP_LABELS -> showAppLabels
        AppSettingsToggleKey.SHOW_TODAY_EVENTS -> showTodayEvents
        AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE -> isSearchEngineCompactMode
        AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX -> isSearchEngineAliasSuffixEnabled
        AppSettingsToggleKey.CALCULATOR -> calculatorEnabled
        AppSettingsToggleKey.UNIT_CONVERTER -> unitConverterEnabled
        AppSettingsToggleKey.DATE_CALCULATOR -> dateCalculatorEnabled
        AppSettingsToggleKey.APP_SUGGESTIONS -> appSuggestionsEnabled
        AppSettingsToggleKey.WEB_SUGGESTIONS -> webSuggestionsEnabled
        AppSettingsToggleKey.RECENT_QUERIES -> recentQueriesEnabled
        AppSettingsToggleKey.TOP_MATCHES -> topMatchesEnabled
        AppSettingsToggleKey.TOP_RESULT_INDICATOR -> topResultIndicatorEnabled
        AppSettingsToggleKey.OPEN_KEYBOARD -> openKeyboardOnLaunch
        AppSettingsToggleKey.CLEAR_QUERY -> clearQueryOnLaunch
        AppSettingsToggleKey.AUTO_CLOSE_OVERLAY -> autoCloseOverlay
        AppSettingsToggleKey.CIRCULAR_APP_ICONS -> appIconShape == AppIconShape.CIRCLE
        AppSettingsToggleKey.SHOW_FOLDERS -> showFolders
        AppSettingsToggleKey.SHOW_SYSTEM_FILES -> showSystemFiles
        AppSettingsToggleKey.DIRECT_DIAL -> directDialEnabled
        AppSettingsToggleKey.ASSISTANT_LAUNCH_VOICE_MODE -> assistantLaunchVoiceModeEnabled
        AppSettingsToggleKey.WALLPAPER_ACCENT -> wallpaperAccentEnabled
        AppSettingsToggleKey.THEMED_ICONS -> themedIconsEnabled
        AppSettingsToggleKey.DEVICE_THEME -> deviceThemeEnabled
        AppSettingsToggleKey.USE_SYSTEM_FONT -> useSystemFont
        AppSettingsToggleKey.DICTIONARY -> dictionaryEnabled
        AppSettingsToggleKey.APPS_PER_ROW -> false
        AppSettingsToggleKey.SEARCH_APPS,
        AppSettingsToggleKey.SEARCH_APP_SHORTCUTS,
        AppSettingsToggleKey.SEARCH_CONTACTS,
        AppSettingsToggleKey.SEARCH_FILES,
        AppSettingsToggleKey.SEARCH_DEVICE_SETTINGS,
        AppSettingsToggleKey.SEARCH_CALENDAR,
        AppSettingsToggleKey.SEARCH_NOTES,
        AppSettingsToggleKey.SEARCH_APP_SETTINGS,
        -> false
    }
}
