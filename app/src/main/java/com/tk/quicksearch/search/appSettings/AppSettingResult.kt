package com.tk.quicksearch.search.appSettings

enum class AppSettingResultSource {
    APP,
}

enum class AppSettingResultAction {
    NAVIGATE,
    TOGGLE,
}

enum class AppSettingsDestination {
    APPEARANCE,
    SEARCH_RESULTS,
    SEARCH_ENGINES,
    TOOLS,
    LAUNCH_OPTIONS,
    MORE_OPTIONS,
    PERMISSIONS,
    APP_MANAGEMENT,
    APP_SHORTCUTS,
    CALLS_TEXTS,
    FILES,
    DEVICE_SETTINGS,
    EXCLUDED_ITEMS,
    DIRECT_SEARCH_CONFIGURE,
    CALENDAR_EVENTS,
    RELOAD_APPS,
    RELOAD_CONTACTS,
    RELOAD_FILES,
    SEND_FEEDBACK,
    RATE_QUICK_SEARCH,
    DEVELOPMENT,
    FEATURES_LIST,
    OPEN_SOURCE_LICENSES,
}

enum class AppSettingsToggleKey {
    OVERLAY_MODE,
    ONE_HANDED_MODE,
    BOTTOM_SEARCHBAR,
    APP_LABELS,
    SEARCH_ENGINE_COMPACT_MODE,
    SEARCH_ENGINE_ALIAS_SUFFIX,
    CALCULATOR,
    UNIT_CONVERTER,
    APP_SUGGESTIONS,
    WEB_SUGGESTIONS,
    RECENT_QUERIES,
    TOP_RESULT_INDICATOR,
    OPEN_KEYBOARD,
    CLEAR_QUERY,
    AUTO_CLOSE_OVERLAY,
    CIRCULAR_APP_ICONS,
    SHOW_FOLDERS,
    SHOW_SYSTEM_FILES,
    SHOW_HIDDEN_FILES,
    DIRECT_DIAL,
}

data class AppSettingResult(
    val id: String,
    val title: String,
    val description: String? = null,
    val keywords: List<String> = emptyList(),
    val source: AppSettingResultSource = AppSettingResultSource.APP,
    val action: AppSettingResultAction,
    val destination: AppSettingsDestination? = null,
    val toggleKey: AppSettingsToggleKey? = null,
) {
    init {
        require(
            (action == AppSettingResultAction.NAVIGATE && destination != null && toggleKey == null) ||
                (action == AppSettingResultAction.TOGGLE && toggleKey != null && destination == null),
        ) {
            "AppSettingResult action metadata is invalid for id=$id"
        }
    }

    val isToggleAction: Boolean
        get() = action == AppSettingResultAction.TOGGLE

    val isNavigateAction: Boolean
        get() = action == AppSettingResultAction.NAVIGATE
}
