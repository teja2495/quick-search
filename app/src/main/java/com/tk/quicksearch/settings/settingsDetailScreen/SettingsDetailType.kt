package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.annotation.StringRes
import com.tk.quicksearch.R

internal data class SettingsPageSearchShortcut(
    val id: String,
    @StringRes val titleRes: Int? = null,
    @StringRes val descriptionRes: Int? = null,
    val keywords: List<String> = emptyList(),
)

enum class SettingsDetailType(
    @StringRes private val titleResId: Int,
    private val level: Int,
    private val fallbackBackDestination: (() -> SettingsDetailType)? = null,
    private val preferSourceBackDestination: Boolean = false,
    internal val searchShortcut: SettingsPageSearchShortcut? = null,
) {
    SEARCH_ENGINES(
        titleResId = R.string.settings_app_shortcuts_filter_search_engines,
        level = 1,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_search_engines",
            descriptionRes = R.string.settings_search_engines_desc,
            keywords = listOf("ai", "direct", "alias"),
        ),
    ),
    EXCLUDED_ITEMS(
        titleResId = R.string.settings_excluded_items_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_excluded_items",
            descriptionRes = R.string.settings_excluded_items_desc,
        ),
    ),
    SEARCH_RESULTS(
        titleResId = R.string.settings_search_results_title,
        level = 1,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_search_results",
            descriptionRes = R.string.settings_search_results_desc,
            keywords = listOf("history", "suggestions"),
        ),
    ),
    APP_MANAGEMENT(
        titleResId = R.string.settings_manage_apps_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_app_management",
            titleRes = R.string.section_apps,
            descriptionRes = R.string.settings_manage_apps_desc,
            keywords = listOf("uninstall", "app info", "apps"),
        ),
    ),
    APP_SHORTCUTS(
        titleResId = R.string.section_app_shortcuts,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_app_shortcuts",
            descriptionRes = R.string.settings_manage_shortcuts_desc,
            keywords = listOf("deep link", "custom", "activity"),
        ),
    ),
    DEVICE_SETTINGS(
        titleResId = R.string.section_settings,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_device_settings",
            descriptionRes = R.string.settings_view_all_desc,
            keywords = listOf("android settings", "system settings"),
        ),
    ),
    CALENDAR_EVENTS(
        titleResId = R.string.section_calendar,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_calendar_events",
            descriptionRes = R.string.settings_calendar_view_all_events_desc,
        ),
    ),
    REMINDERS(
        titleResId = R.string.section_reminders,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_reminders_list",
            descriptionRes = R.string.settings_reminders_view_all_desc,
            keywords = listOf("reminder", "todo"),
        ),
    ),
    BOOKMARKS(
        titleResId = R.string.section_bookmarks,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_bookmarks_list",
            descriptionRes = R.string.settings_bookmarks_view_all_desc,
            keywords = listOf("bookmark", "website", "websites", "link", "links", "url", "favorites", "import bookmarks", "export bookmarks", "bookmark browser"),
        ),
    ),
    NOTES(
        titleResId = R.string.section_notes,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_notes_list",
            descriptionRes = R.string.settings_notes_view_all_desc,
            keywords = listOf("quick note"),
        ),
    ),
    NOTE_EDITOR(
        titleResId = R.string.notes_editor_title,
        level = 3,
        preferSourceBackDestination = true,
    ),
    APPEARANCE(
        titleResId = R.string.settings_appearance_title,
        level = 1,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_appearance",
            descriptionRes = R.string.settings_appearance_desc,
            keywords = listOf("wallpaper", "style", "search engines style", "inline", "compact"),
        ),
    ),
    CALLS_TEXTS(
        titleResId = R.string.settings_calls_texts_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_calls_texts",
            descriptionRes = R.string.settings_manage_calls_texts_contacts_desc,
            keywords =
                listOf(
                    "contacts", "calling", "messaging", "whatsapp", "telegram", "signal", "contact button",
                    "calling app", "default calling app", "messaging app", "default messaging app", "texting", "sms",
                ),
        ),
    ),
    FILES(
        titleResId = R.string.settings_file_types_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_files",
            descriptionRes = R.string.settings_manage_files_desc,
            keywords = listOf("filters", "whitelist", "blacklist"),
        ),
    ),
    LAUNCH_OPTIONS(
        titleResId = R.string.settings_launch_options_title,
        level = 1,
    ),
    MORE_OPTIONS(
        titleResId = R.string.settings_more_options_title,
        level = 1,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_more_options",
            descriptionRes = R.string.settings_more_options_desc,
        ),
    ),
    PERMISSIONS(
        titleResId = R.string.settings_permissions_title,
        level = 1,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_permissions",
            descriptionRes = R.string.settings_permissions_desc,
            keywords = listOf("access"),
        ),
    ),
    TOOLS(
        titleResId = R.string.settings_tools_title,
        level = 2,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_tools",
            descriptionRes = R.string.settings_tools_desc,
            keywords = listOf("calculator", "date", "time", "unit converter", "currency converter", "conversion", "alias"),
        ),
    ),
    AT_A_GLANCE(
        titleResId = R.string.settings_at_a_glance_title,
        level = 2,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_at_a_glance",
            descriptionRes = R.string.settings_at_a_glance_desc,
            keywords = listOf("today", "calendar", "events", "alarm", "reminders", "home", "media", "music", "playback", "birthday", "charging", "battery", "low battery", "timer", "stopwatch", "storage", "low storage", "disk space", "progress", "ongoing notifications", "live updates", "ride", "delivery", "download", "missed", "calls", "ongoing call", "otp", "one-time password", "verification code", "2fa", "sms code", "do not disturb", "dnd", "airplane", "flight", "hotspot", "tethering", "wifi", "wi-fi", "sign in", "captive portal", "hotel wifi", "flashlight", "torch", "weather", "temperature", "forecast", "weather alerts", "workout", "exercise", "fitness", "google health", "fitbit", "samsung health", "tomorrow", "custom info", "ai", "prompt", "scheduled", "daily", "briefing", "recurring"),
        ),
    ),
    // This persisted name also appears in intents.
    GEMINI_API_CONFIG(
        titleResId = R.string.common_ai_provider,
        level = 3,
        preferSourceBackDestination = true,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_gemini_api",
            descriptionRes = R.string.settings_gemini_api_desc,
            keywords =
                listOf(
                    "ai provider",
                    "personal context",
                    "gemini",
                    "openai",
                    "groq",
                    "claude",
                    "meta ai",
                    "tavily",
                    "custom provider",
                ),
        ),
    ),
    API_KEY_SETUP(
        titleResId = R.string.common_api_key_setup,
        level = 3,
        preferSourceBackDestination = true,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_api_key_setup",
            descriptionRes = R.string.settings_api_key_setup_nav_desc,
            keywords = listOf("api key", "token", "gemini", "openai", "groq", "claude", "meta ai", "tavily", "custom provider"),
        ),
    ),
    FEATURES_LIST(
        titleResId = R.string.settings_all_quick_search_features,
        level = 1,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_features_list",
            descriptionRes = R.string.settings_all_quick_search_features_desc,
        ),
    ),
    OPEN_SOURCE_LICENSES(
        titleResId = R.string.settings_open_source_licenses_title,
        level = 1,
    ),
    UNIT_CONVERTER_INFO(
        titleResId = R.string.unit_converter_info_title,
        level = 3,
        fallbackBackDestination = { SettingsDetailType.TOOLS },
    ),
    DATE_CALCULATOR_INFO(
        titleResId = R.string.date_calculator_info_title,
        level = 3,
        fallbackBackDestination = { SettingsDetailType.TOOLS },
    ),
    CUSTOM_TOOL_EDITOR(
        titleResId = R.string.settings_custom_tool_editor_title,
        level = 3,
        fallbackBackDestination = { SettingsDetailType.TOOLS },
        preferSourceBackDestination = true,
    ),
    TASKER_INTEGRATION(
        titleResId = R.string.tasker_integration_title,
        level = 3,
        fallbackBackDestination = { SettingsDetailType.TOOLS },
    ),
    NICKNAMES(
        titleResId = R.string.settings_nicknames_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_nicknames",
            descriptionRes = R.string.settings_nicknames_desc,
            keywords = listOf("alias", "rename"),
        ),
    ),
    TRIGGERS(
        titleResId = R.string.settings_triggers_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.SEARCH_RESULTS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_triggers",
            descriptionRes = R.string.settings_triggers_desc,
            keywords = listOf("shortcut word", "keyword"),
        ),
    ),
    GESTURES(
        titleResId = R.string.settings_gestures_title,
        level = 2,
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_gestures",
            descriptionRes = R.string.settings_gestures_desc,
            keywords = listOf("swipe", "quick note", "keyboard", "notifications"),
        ),
    ),
    NOTIFICATION_HISTORY(
        titleResId = R.string.settings_shortcut_notification_history,
        level = 2,
    ),
    EDGE_GESTURE(
        titleResId = R.string.settings_edge_gesture_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.LAUNCH_OPTIONS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_edge_gesture",
            descriptionRes = R.string.settings_edge_gesture_desc,
            keywords = listOf("edge", "swipe", "side", "handle", "gesture"),
        ),
    ),
    FLOATING_BUTTON(
        titleResId = R.string.settings_floating_button_title,
        level = 2,
        fallbackBackDestination = { SettingsDetailType.LAUNCH_OPTIONS },
        searchShortcut = SettingsPageSearchShortcut(
            id = "app_settings_floating_button",
            descriptionRes = R.string.settings_floating_button_desc,
            keywords = listOf("floating", "bubble", "button", "overlay", "shortcut"),
        ),
    ),
    ;

    internal fun titleResId(): Int = titleResId

    internal fun isNestedDetail(): Boolean = level >= 2

    internal fun level(): Int = level

    internal fun resolveBackDestination(sourceDetailType: SettingsDetailType?): SettingsDetailType? {
        if (!isNestedDetail()) return null
        if (
            preferSourceBackDestination &&
                sourceDetailType != null &&
                sourceDetailType != this &&
                !(this == GEMINI_API_CONFIG && sourceDetailType == API_KEY_SETUP)
        ) {
            return sourceDetailType
        }
        return fallbackBackDestination?.invoke()
    }
}
