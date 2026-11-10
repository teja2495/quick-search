package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

internal fun AppSettingsDestination.toSettingsDetailTypeOrNull(): SettingsDetailType? =
    when (this) {
        AppSettingsDestination.APPEARANCE -> SettingsDetailType.APPEARANCE
        AppSettingsDestination.SEARCH_RESULTS -> SettingsDetailType.SEARCH_RESULTS
        AppSettingsDestination.SEARCH_ENGINES -> SettingsDetailType.SEARCH_ENGINES
        AppSettingsDestination.TOOLS -> SettingsDetailType.TOOLS
        AppSettingsDestination.AT_A_GLANCE -> SettingsDetailType.AT_A_GLANCE
        AppSettingsDestination.TASKER_INTEGRATION -> SettingsDetailType.TASKER_INTEGRATION
        AppSettingsDestination.LAUNCH_OPTIONS -> SettingsDetailType.LAUNCH_OPTIONS
        AppSettingsDestination.EDGE_GESTURE -> SettingsDetailType.EDGE_GESTURE
        AppSettingsDestination.FLOATING_BUTTON -> SettingsDetailType.FLOATING_BUTTON
        AppSettingsDestination.MORE_OPTIONS -> SettingsDetailType.MORE_OPTIONS
        AppSettingsDestination.PERMISSIONS -> SettingsDetailType.PERMISSIONS
        AppSettingsDestination.APP_MANAGEMENT -> SettingsDetailType.APP_MANAGEMENT
        AppSettingsDestination.APP_SHORTCUTS -> SettingsDetailType.APP_SHORTCUTS
        AppSettingsDestination.CALLS_TEXTS -> SettingsDetailType.CALLS_TEXTS
        AppSettingsDestination.FILES -> SettingsDetailType.FILES
        AppSettingsDestination.DEVICE_SETTINGS -> SettingsDetailType.DEVICE_SETTINGS
        AppSettingsDestination.EXCLUDED_ITEMS -> SettingsDetailType.EXCLUDED_ITEMS
        AppSettingsDestination.AI_SEARCH_CONFIGURE,
        AppSettingsDestination.GEMINI_API,
        AppSettingsDestination.AI_MODEL -> SettingsDetailType.GEMINI_API_CONFIG
        AppSettingsDestination.API_KEY_SETUP -> SettingsDetailType.API_KEY_SETUP
        AppSettingsDestination.CALENDAR_EVENTS -> SettingsDetailType.CALENDAR_EVENTS
        AppSettingsDestination.REMINDERS_LIST -> SettingsDetailType.REMINDERS
        AppSettingsDestination.BOOKMARKS_LIST -> SettingsDetailType.BOOKMARKS
        AppSettingsDestination.NICKNAMES -> SettingsDetailType.NICKNAMES
        AppSettingsDestination.TRIGGERS -> SettingsDetailType.TRIGGERS
        AppSettingsDestination.GESTURES,
        AppSettingsDestination.GESTURE_SWIPE_LEFT,
        AppSettingsDestination.GESTURE_SWIPE_RIGHT,
        AppSettingsDestination.GESTURE_SWIPE_UP,
        AppSettingsDestination.GESTURE_SWIPE_DOWN,
        AppSettingsDestination.GESTURE_DOUBLE_TAP,
        AppSettingsDestination.GESTURE_TWO_FINGER_SWIPE_UP,
        AppSettingsDestination.GESTURE_TWO_FINGER_SWIPE_DOWN,
        AppSettingsDestination.GESTURE_OPEN_KEYBOARD,
        AppSettingsDestination.GESTURE_CLOSE_KEYBOARD,
        AppSettingsDestination.GESTURE_NOTCH_SINGLE_TAP,
        AppSettingsDestination.GESTURE_NOTCH_DOUBLE_TAP,
        AppSettingsDestination.GESTURE_NOTCH_LONG_PRESS -> SettingsDetailType.GESTURES
        AppSettingsDestination.FEATURES_LIST -> SettingsDetailType.FEATURES_LIST
        AppSettingsDestination.OPEN_SOURCE_LICENSES -> SettingsDetailType.OPEN_SOURCE_LICENSES
        AppSettingsDestination.UNIT_CONVERTER_INFO -> SettingsDetailType.UNIT_CONVERTER_INFO
        AppSettingsDestination.DATE_CALCULATOR_INFO -> SettingsDetailType.DATE_CALCULATOR_INFO
        AppSettingsDestination.RELOAD_APPS,
        AppSettingsDestination.ICON_PACKS,
        AppSettingsDestination.SEARCH_RESULT_RANKING,
        AppSettingsDestination.RELOAD_CONTACTS,
        AppSettingsDestination.RELOAD_FILES,
        AppSettingsDestination.SEND_FEEDBACK,
        AppSettingsDestination.RATE_QUICK_SEARCH,
        AppSettingsDestination.DEVELOPMENT,
        AppSettingsDestination.SET_DEFAULT_ASSISTANT,
        AppSettingsDestination.SET_DEFAULT_LAUNCHER,
        AppSettingsDestination.ADD_HOME_SCREEN_WIDGET,
        AppSettingsDestination.ADD_QUICK_SETTINGS_TILE,
        AppSettingsDestination.CREATE_NOTE,
        AppSettingsDestination.CREATE_NOTIFICATION_NOTE,
        AppSettingsDestination.NOTES_LIST,
        AppSettingsDestination.CREATE_REMINDER -> null
        AppSettingsDestination.OPEN_EVENTS_IN -> null
        AppSettingsDestination.TOP_MATCHES_PRIORITY -> null
        AppSettingsDestination.APP_SUGGESTION_TABS -> null
        AppSettingsDestination.APP_NOTIFICATIONS -> null
        AppSettingsDestination.PINNED_NOTIFICATION_ITEMS -> null
        AppSettingsDestination.EXPORT_SETTINGS -> null
        AppSettingsDestination.IMPORT_SETTINGS -> null
        AppSettingsDestination.APP_LANGUAGE -> null
        AppSettingsDestination.RELEASE_NOTES -> null
    }
