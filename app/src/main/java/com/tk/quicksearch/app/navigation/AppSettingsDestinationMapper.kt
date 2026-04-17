package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

internal fun AppSettingsDestination.toSettingsDetailTypeOrNull(): SettingsDetailType? =
    when (this) {
        AppSettingsDestination.APPEARANCE -> SettingsDetailType.APPEARANCE
        AppSettingsDestination.SEARCH_RESULTS -> SettingsDetailType.SEARCH_RESULTS
        AppSettingsDestination.SEARCH_ENGINES -> SettingsDetailType.SEARCH_ENGINES
        AppSettingsDestination.TOOLS -> SettingsDetailType.TOOLS
        AppSettingsDestination.LAUNCH_OPTIONS -> SettingsDetailType.LAUNCH_OPTIONS
        AppSettingsDestination.MORE_OPTIONS -> SettingsDetailType.MORE_OPTIONS
        AppSettingsDestination.PERMISSIONS -> SettingsDetailType.PERMISSIONS
        AppSettingsDestination.APP_MANAGEMENT -> SettingsDetailType.APP_MANAGEMENT
        AppSettingsDestination.APP_SHORTCUTS -> SettingsDetailType.APP_SHORTCUTS
        AppSettingsDestination.CALLS_TEXTS -> SettingsDetailType.CALLS_TEXTS
        AppSettingsDestination.FILES -> SettingsDetailType.FILES
        AppSettingsDestination.DEVICE_SETTINGS -> SettingsDetailType.DEVICE_SETTINGS
        AppSettingsDestination.EXCLUDED_ITEMS -> SettingsDetailType.EXCLUDED_ITEMS
        AppSettingsDestination.AI_SEARCH_CONFIGURE,
        AppSettingsDestination.GEMINI_API -> SettingsDetailType.GEMINI_API_CONFIG
        AppSettingsDestination.CALENDAR_EVENTS -> SettingsDetailType.CALENDAR_EVENTS
        AppSettingsDestination.FEATURES_LIST -> SettingsDetailType.FEATURES_LIST
        AppSettingsDestination.OPEN_SOURCE_LICENSES -> SettingsDetailType.OPEN_SOURCE_LICENSES
        AppSettingsDestination.UNIT_CONVERTER_INFO -> SettingsDetailType.UNIT_CONVERTER_INFO
        AppSettingsDestination.DATE_CALCULATOR_INFO -> SettingsDetailType.DATE_CALCULATOR_INFO
        AppSettingsDestination.RELOAD_APPS,
        AppSettingsDestination.RELOAD_CONTACTS,
        AppSettingsDestination.RELOAD_FILES,
        AppSettingsDestination.SEND_FEEDBACK,
        AppSettingsDestination.RATE_QUICK_SEARCH,
        AppSettingsDestination.DEVELOPMENT,
        AppSettingsDestination.SET_DEFAULT_ASSISTANT,
        AppSettingsDestination.ADD_HOME_SCREEN_WIDGET,
        AppSettingsDestination.ADD_QUICK_SETTINGS_TILE,
        AppSettingsDestination.CREATE_NOTE,
        AppSettingsDestination.NOTES_LIST,
        AppSettingsDestination.CREATE_CALENDAR_EVENT -> null
    }
