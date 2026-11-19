package com.tk.quicksearch.search.appSettings

enum class AppSettingResultSource {
    APP,
}

enum class AppSettingResultAction {
    NAVIGATE,
    TOGGLE,
}

enum class AppSettingsToggleKey {
    OVERLAY_MODE,
    ONE_HANDED_MODE,
    BOTTOM_SEARCHBAR,
    COMPACT_LAYOUT,
    UNIFIED_PINNED_ITEMS,
    PINNED_APP_SHORTCUTS_IN_APP_GRID,
    SHOW_STATUS_BAR,
    SEARCH_HINTS,
    SETTINGS_ICON,
    MIC_ICON,
    APP_LABELS,
    SEARCH_ENGINE_COMPACT_MODE,
    SEARCH_ENGINE_ALIAS_SUFFIX,
    ALIAS_TRIGGER_AFTER_SPACE,
    APP_SUGGESTIONS,
    SHOW_ALL_APPS_BUTTON,
    INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH,
    INCLUDE_ARCHIVED_APPS_IN_SEARCH,
    SHOW_IN_RECENTS,
    NOTIFICATION_DOTS,
    APP_LOCK_OUTSIDE_QUICK_SEARCH,
    SPEED_BUMP_OUTSIDE_QUICK_SEARCH,
    WEB_SUGGESTIONS,
    RECENT_QUERIES,
    FUZZY_SEARCH,
    TOP_RESULT_INDICATOR,
    OPEN_TOP_RESULT_USING_KEYBOARD,
    TOP_MATCHES,
    OPEN_KEYBOARD,
    CLEAR_QUERY,
    AUTO_CLOSE_OVERLAY,
    CIRCULAR_APP_ICONS,
    SHOW_FOLDERS,
    SHOW_SYSTEM_FILES,
    DIRECT_DIAL,
    NUMBER_SEARCH,
    SEARCH_APPS,
    SEARCH_APP_SHORTCUTS,
    SEARCH_CONTACTS,
    SEARCH_FILES,
    SEARCH_DEVICE_SETTINGS,
    SEARCH_CALENDAR,
    SEARCH_REMINDERS,
    SEARCH_NOTES,
    SEARCH_BOOKMARKS,
    SEARCH_APP_SETTINGS,
    ASSISTANT_LAUNCH_VOICE_MODE,
    THEMED_ICONS,
    DEVICE_THEME,
    AMOLED_THEME,
    SEARCH_CHROME_BLUR,
    USE_SYSTEM_FONT,
    FONT_SIZE,
    APP_ICON_SIZE,
    HOME_TEXT_COLOR,
    ACCENT_COLOR,
    APPS_PER_ROW,
    APP_RESULT_ROWS,
    CURRENCY_CONVERTER,
    WORLD_CLOCK,
    DICTIONARY,
    WEATHER,
    APP_NOTIFICATIONS,
}

data class AppSettingResult(
    val id: String,
    val title: String,
    val description: String? = null,
    val keywords: List<String> = emptyList(),
    val source: AppSettingResultSource = AppSettingResultSource.APP,
    val action: AppSettingResultAction,
    val destination: AppSettingsTarget? = null,
    val toggleKey: AppSettingsToggleKey? = null,
    val booleanSetting: BooleanSetting? = null,
) {
    // TOGGLE rows may also carry a destination; see isNavigationToggle.
    init {
        require(
            when (action) {
                AppSettingResultAction.NAVIGATE ->
                    destination != null && toggleKey == null && booleanSetting == null
                AppSettingResultAction.TOGGLE -> (toggleKey != null) xor (booleanSetting != null)
            },
        ) {
            "AppSettingResult action metadata is invalid for id=$id"
        }
    }

    val isToggleAction: Boolean
        get() = action == AppSettingResultAction.TOGGLE

    val isNavigateAction: Boolean
        get() = action == AppSettingResultAction.NAVIGATE

    /** A toggle row whose body opens [destination], like navigation+toggle rows in Settings. */
    val isNavigationToggle: Boolean
        get() = isToggleAction && destination != null
}
