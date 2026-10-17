package com.tk.quicksearch.search.appSettings

import android.content.Context
import android.os.Build
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.data.preferences.RATE_QUICK_SEARCH_ENABLED
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.shared.util.isTablet

internal const val PINNED_SECTIONS_ORDER_SETTING_ID = "app_settings_pinned_sections_order"

class AppSettingsRepository(
    private val context: Context,
) {
    fun loadSettings(): List<AppSettingResult> {
        return buildList {
            addNavigation(
                id = "app_settings_appearance",
                titleRes = R.string.settings_appearance_title,
                descriptionRes = R.string.settings_appearance_desc,
                destination = AppSettingsDestination.APPEARANCE,
                keywords =
                    listOf(
                        "wallpaper",
                        "style",
                        "search engines style",
                        "inline",
                        "compact"
                    ),
            )
            addNavigation(
                id = THEME_MODE_SETTING_ID,
                titleRes = R.string.settings_app_theme_title,
                destination = AppSettingsDestination.APPEARANCE,
                keywords = listOf("theme", "dark", "light", "system", "mode", "night"),
            )
            addToggle(
                id = "app_setting_font_size",
                titleRes = R.string.settings_font_size_title,
                toggleKey = AppSettingsToggleKey.FONT_SIZE,
                keywords = listOf("text size", "font", "bigger", "smaller"),
            )
            addToggle(
                id = "app_setting_home_text_color",
                titleRes = R.string.settings_home_text_color_title,
                descriptionRes = R.string.settings_home_text_color_desc,
                toggleKey = AppSettingsToggleKey.HOME_TEXT_COLOR,
                keywords = listOf("text color", "colour", "color", "white", "black", "labels"),
            )
            addToggle(
                id = "app_setting_app_icon_size",
                titleRes = R.string.settings_app_icon_size_title,
                toggleKey = AppSettingsToggleKey.APP_ICON_SIZE,
                keywords = listOf("icons", "size", "bigger", "smaller"),
            )
            addNavigation(
                id = "app_settings_launcher_icon",
                titleRes = R.string.settings_launcher_icon_title,
                destination = AppSettingsDestination.APPEARANCE,
                keywords = listOf("app icon", "launcher icon", "change icon", "home screen icon"),
            )
            addNavigation(
                id = "app_settings_icon_packs",
                titleRes = R.string.settings_icon_pack_title,
                descriptionRes = R.string.settings_search_description_change_icon_pack,
                destination = AppSettingsDestination.ICON_PACKS,
                keywords = listOf("icons", "icon pack", "themed icons"),
            )
            addNavigation(
                id = "app_settings_search_results",
                titleRes = R.string.settings_search_results_title,
                descriptionRes = R.string.settings_search_results_desc,
                destination = AppSettingsDestination.SEARCH_RESULTS,
                keywords = listOf("history", "suggestions"),
            )
            addNavigation(
                id = "app_settings_search_engines",
                titleRes = R.string.settings_app_shortcuts_filter_search_engines,
                descriptionRes = R.string.settings_search_engines_desc,
                destination = AppSettingsDestination.SEARCH_ENGINES,
                keywords = listOf("ai", "direct", "alias"),
            )
            addNavigation(
                id = "app_settings_gemini_api",
                titleRes = R.string.common_ai_provider,
                descriptionRes = R.string.settings_gemini_api_desc,
                destination = AppSettingsDestination.GEMINI_API,
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
            )
            addNavigation(
                id = AI_MODEL_SETTING_ID,
                titleRes = R.string.settings_direct_search_model_label,
                destination = AppSettingsDestination.AI_MODEL,
                keywords = listOf("ai model", "llm", "thinking", "web search", "grounding"),
            )
            addNavigation(
                id = "app_settings_api_key_setup",
                titleRes = R.string.common_api_key_setup,
                descriptionRes = R.string.settings_api_key_setup_nav_desc,
                destination = AppSettingsDestination.API_KEY_SETUP,
                keywords = listOf("api key", "token", "gemini", "openai", "groq", "claude", "meta ai", "tavily", "custom provider"),
            )
            addNavigation(
                id = "app_settings_tools",
                titleRes = R.string.settings_tools_title,
                descriptionRes = R.string.settings_tools_desc,
                destination = AppSettingsDestination.TOOLS,
                keywords = listOf("calculator", "date", "time", "unit converter", "currency converter", "conversion", "alias"),
            )
            addNavigation(
                id = "app_settings_at_a_glance",
                titleRes = R.string.settings_at_a_glance_title,
                descriptionRes = R.string.settings_at_a_glance_desc,
                destination = AppSettingsDestination.AT_A_GLANCE,
                keywords = listOf("today", "calendar", "events", "alarm", "reminders", "home", "media", "music", "playback", "birthday", "charging", "battery", "low battery", "timer", "stopwatch", "storage", "low storage", "disk space", "progress", "ongoing notifications", "live updates", "ride", "delivery", "download", "missed", "calls", "ongoing call", "otp", "one-time password", "verification code", "2fa", "sms code", "do not disturb", "dnd", "airplane", "flight", "hotspot", "tethering", "wifi", "wi-fi", "sign in", "captive portal", "hotel wifi", "flashlight", "torch", "weather", "temperature", "forecast", "weather alerts", "workout", "exercise", "fitness", "google health", "fitbit", "samsung health", "tomorrow", "custom info", "ai", "prompt", "scheduled", "daily", "briefing", "recurring"),
            )
            addToggle(
                id = "app_toggle_app_notifications",
                titleRes = R.string.settings_at_a_glance_app_notifications_title,
                descriptionRes = R.string.settings_at_a_glance_app_notifications_desc,
                toggleKey = AppSettingsToggleKey.APP_NOTIFICATIONS,
                destination = AppSettingsDestination.APP_NOTIFICATIONS,
                keywords = listOf("at a glance", "notification rules", "notification filter", "keywords"),
            )
            if (context.isTaskerInstalled()) {
                addNavigation(
                    id = "app_settings_tasker_integration",
                    titleRes = R.string.tasker_integration_title,
                    descriptionRes = R.string.tasker_integration_description,
                    destination = AppSettingsDestination.TASKER_INTEGRATION,
                    keywords = listOf("tasker", "broadcast", "intent", "automation", "action", "alias"),
                )
            }
            addNavigation(
                id = "app_settings_export_settings",
                titleRes = R.string.settings_backup_export_title,
                descriptionRes = R.string.settings_backup_export_desc,
                destination = AppSettingsDestination.EXPORT_SETTINGS,
                keywords = listOf("backup", "save"),
            )
            addNavigation(
                id = "app_settings_import_settings",
                titleRes = R.string.setup_import_button,
                descriptionRes = R.string.settings_backup_import_warning_message,
                destination = AppSettingsDestination.IMPORT_SETTINGS,
                keywords = listOf("backup", "restore"),
            )
            addNavigation(
                id = "app_settings_launch_options",
                titleRes = R.string.settings_launch_options_title,
                descriptionRes = R.string.settings_launch_options_desc,
                destination = AppSettingsDestination.LAUNCH_OPTIONS,
            )
            addNavigation(
                id = "app_settings_edge_gesture",
                titleRes = R.string.settings_edge_gesture_title,
                descriptionRes = R.string.settings_edge_gesture_desc,
                destination = AppSettingsDestination.EDGE_GESTURE,
                keywords = listOf("edge", "swipe", "side", "handle", "gesture"),
            )
            addNavigation(
                id = "app_settings_floating_button",
                titleRes = R.string.settings_floating_button_title,
                descriptionRes = R.string.settings_floating_button_desc,
                destination = AppSettingsDestination.FLOATING_BUTTON,
                keywords = listOf("floating", "bubble", "button", "overlay", "shortcut"),
            )
            addNavigation(
                id = "app_settings_default_assistant",
                titleRes = R.string.settings_default_assistant_title,
                descriptionRes = R.string.settings_default_assistant_desc,
                destination = AppSettingsDestination.SET_DEFAULT_ASSISTANT,
                keywords = listOf("digital assistant"),
            )
            addNavigation(
                id = "app_settings_default_launcher",
                titleRes = R.string.settings_default_launcher_title,
                descriptionRes = R.string.settings_default_launcher_desc,
                destination = AppSettingsDestination.SET_DEFAULT_LAUNCHER,
                keywords = listOf("home app", "default home", "launcher"),
            )
            addToggle(
                id = "app_settings_assistant_voice_mode",
                titleRes = R.string.settings_assistant_voice_mode_title,
                descriptionRes = R.string.settings_assistant_voice_mode_desc,
                toggleKey = AppSettingsToggleKey.ASSISTANT_LAUNCH_VOICE_MODE,
            )
            addNavigation(
                id = "app_settings_home_screen_widget",
                titleRes = R.string.settings_home_screen_widget_title,
                descriptionRes = R.string.settings_home_screen_widget_desc,
                destination = AppSettingsDestination.ADD_HOME_SCREEN_WIDGET,
            )
            addNavigation(
                id = "app_settings_quick_settings_tile",
                titleRes = R.string.settings_quick_settings_tile_title,
                descriptionRes = R.string.settings_quick_settings_tile_desc,
                destination = AppSettingsDestination.ADD_QUICK_SETTINGS_TILE,
            )
            addNavigation(
                id = "app_settings_more_options",
                titleRes = R.string.settings_more_options_title,
                descriptionRes = R.string.settings_more_options_desc,
                destination = AppSettingsDestination.MORE_OPTIONS,
            )
            addNavigation(
                id = "app_settings_permissions",
                titleRes = R.string.settings_permissions_title,
                descriptionRes = R.string.settings_permissions_desc,
                destination = AppSettingsDestination.PERMISSIONS,
                keywords = listOf("access"),
            )
            addNavigation(
                id = "app_settings_app_management",
                titleRes = R.string.section_apps,
                descriptionRes = R.string.settings_manage_apps_desc,
                destination = AppSettingsDestination.APP_MANAGEMENT,
                keywords = listOf("uninstall", "app info", "apps"),
            )
            addNavigation(
                id = "app_settings_app_shortcuts",
                titleRes = R.string.section_app_shortcuts,
                descriptionRes = R.string.settings_manage_shortcuts_desc,
                destination = AppSettingsDestination.APP_SHORTCUTS,
                keywords = listOf("deep link", "custom", "activity"),
            )
            addNavigation(
                id = "app_settings_calls_texts",
                titleRes = R.string.settings_calls_texts_title,
                descriptionRes = R.string.settings_manage_calls_texts_contacts_desc,
                destination = AppSettingsDestination.CALLS_TEXTS,
                keywords =
                    listOf(
                        "contacts", "calling", "messaging", "whatsapp", "telegram", "signal", "contact button",
                        "calling app", "default calling app", "messaging app", "default messaging app", "texting", "sms",
                    ),
            )
            addToggle(
                id = "app_toggle_number_search",
                titleRes = R.string.settings_number_search_title,
                descriptionRes = R.string.settings_number_search_desc,
                toggleKey = AppSettingsToggleKey.NUMBER_SEARCH,
                keywords = listOf("phone", "phone number", "contacts"),
            )
            addNavigation(
                id = "app_settings_files",
                titleRes = R.string.settings_file_types_title,
                descriptionRes = R.string.settings_manage_files_desc,
                destination = AppSettingsDestination.FILES,
                keywords = listOf("filters", "whitelist", "blacklist"),
            )
            addToggle(
                id = "app_toggle_file_previews",
                titleRes = R.string.settings_file_previews_toggle,
                descriptionRes = R.string.settings_file_previews_description,
                toggleKey = AppSettingsToggleKey.FILE_PREVIEWS,
                keywords = listOf("files", "images", "pdf", "open"),
            )
            addNavigation(
                id = "app_settings_reload_apps",
                titleRes = R.string.settings_refresh_apps_title,
                destination = AppSettingsDestination.RELOAD_APPS,
                keywords = listOf("refresh apps"),
            )
            addNavigation(
                id = "app_settings_reload_contacts",
                titleRes = R.string.settings_refresh_contacts_title,
                destination = AppSettingsDestination.RELOAD_CONTACTS,
                keywords = listOf("refresh contacts", "sync contacts"),
            )
            addNavigation(
                id = "app_settings_reload_files",
                titleRes = R.string.settings_refresh_files_title,
                destination = AppSettingsDestination.RELOAD_FILES,
                keywords = listOf("refresh files"),
            )
            addNavigation(
                id = "app_settings_device_settings",
                titleRes = R.string.section_settings,
                descriptionRes = R.string.settings_view_all_desc,
                destination = AppSettingsDestination.DEVICE_SETTINGS,
                keywords = listOf("android settings", "system settings"),
            )
            addNavigation(
                id = "app_settings_excluded_items",
                titleRes = R.string.settings_excluded_items_title,
                descriptionRes = R.string.settings_excluded_items_desc,
                destination = AppSettingsDestination.EXCLUDED_ITEMS,
            )
            addNavigation(
                id = "app_settings_pinned_notification_items",
                titleRes = R.string.notification_pinned_items_title,
                descriptionRes = R.string.notification_pinned_items_description,
                destination = AppSettingsDestination.PINNED_NOTIFICATION_ITEMS,
                keywords = listOf("pinned", "notification", "notification panel", "unpin", "reorder"),
            )
            addNavigation(
                id = "app_settings_nicknames",
                titleRes = R.string.settings_nicknames_title,
                descriptionRes = R.string.settings_nicknames_desc,
                destination = AppSettingsDestination.NICKNAMES,
                keywords = listOf("alias", "rename"),
            )
            addNavigation(
                id = "app_settings_triggers",
                titleRes = R.string.settings_triggers_title,
                descriptionRes = R.string.settings_triggers_desc,
                destination = AppSettingsDestination.TRIGGERS,
                keywords = listOf("shortcut word", "keyword"),
            )
            addNavigation(
                id = "app_settings_gestures",
                titleRes = R.string.settings_gestures_title,
                descriptionRes = R.string.settings_gestures_desc,
                destination = AppSettingsDestination.GESTURES,
                keywords = listOf("swipe", "quick note", "keyboard", "notifications"),
            )
            GESTURE_APP_SETTINGS.forEach { (id, titleRes, destination) ->
                addNavigation(
                    id = id,
                    titleRes = titleRes,
                    descriptionRes = R.string.settings_gestures_title,
                    destination = destination,
                )
            }
            addNavigation(
                id = "app_settings_calendar_events",
                titleRes = R.string.section_calendar,
                descriptionRes = R.string.settings_calendar_view_all_events_desc,
                destination = AppSettingsDestination.CALENDAR_EVENTS,
            )
            addNavigation(
                id = "app_settings_open_events_in",
                titleRes = R.string.settings_calendar_default_title,
                destination = AppSettingsDestination.OPEN_EVENTS_IN,
                keywords = listOf("calendar app", "open calendar", "event app"),
            )
            addNavigation(
                id = "app_settings_send_feedback",
                titleRes = R.string.settings_feedback_send_title,
                descriptionRes = R.string.settings_feedback_send_desc,
                destination = AppSettingsDestination.SEND_FEEDBACK,
                keywords = listOf("support", "bug", "request"),
            )
            if (RATE_QUICK_SEARCH_ENABLED) {
                addNavigation(
                    id = "app_settings_rate_quick_search",
                    titleRes = R.string.settings_feedback_rate_title,
                    descriptionRes = R.string.settings_feedback_rate_desc,
                    destination = AppSettingsDestination.RATE_QUICK_SEARCH,
                    keywords = listOf("review"),
                )
            }
            addNavigation(
                id = "app_settings_development",
                titleRes = R.string.settings_feedback_github_title,
                descriptionRes = R.string.settings_feedback_github_desc,
                destination = AppSettingsDestination.DEVELOPMENT,
                keywords = listOf("github", "code"),
            )
            addNavigation(
                id = "app_settings_features_list",
                titleRes = R.string.settings_all_quick_search_features,
                descriptionRes = R.string.settings_all_quick_search_features_desc,
                destination = AppSettingsDestination.FEATURES_LIST,
            )
            addNavigation(
                id = "app_settings_release_notes",
                titleRes = R.string.settings_release_notes_title,
                descriptionRes = R.string.settings_release_notes_desc,
                destination = AppSettingsDestination.RELEASE_NOTES,
                keywords = listOf("what's new", "changelog", "update", "version"),
            )
            addNavigation(
                id = "app_settings_app_language",
                titleRes = R.string.settings_app_language_title,
                destination = AppSettingsDestination.APP_LANGUAGE,
                keywords = listOf("locale", "translation", "translate"),
            )
            addNavigation(
                id = "app_settings_create_note",
                titleRes = R.string.app_settings_search_create_note_title,
                descriptionRes = R.string.app_settings_search_create_note_desc,
                destination = AppSettingsDestination.CREATE_NOTE,
                keywords = listOf("new note", "memo", "write"),
            )
            addNavigation(
                id = "app_settings_notes_list",
                titleRes = R.string.section_notes,
                descriptionRes = R.string.settings_notes_view_all_desc,
                destination = AppSettingsDestination.NOTES_LIST,
                keywords = listOf("quick note"),
            )
            addNavigation(
                id = "app_settings_create_reminder",
                titleRes = R.string.app_setting_create_reminder,
                descriptionRes = R.string.app_setting_create_reminder_desc,
                destination = AppSettingsDestination.CREATE_REMINDER,
                keywords = listOf("new reminder", "add reminder", "remind me", "alarm"),
            )
            addNavigation(
                id = "app_settings_reminders_list",
                titleRes = R.string.section_reminders,
                descriptionRes = R.string.settings_reminders_view_all_desc,
                destination = AppSettingsDestination.REMINDERS_LIST,
                keywords = listOf("reminder", "todo"),
            )

            addToggle(
                id = "app_toggle_overlay_mode",
                titleRes = R.string.settings_overlay_mode_title,
                descriptionRes = R.string.settings_overlay_mode_desc,
                toggleKey = AppSettingsToggleKey.OVERLAY_MODE,
            )
            addToggle(
                id = "app_toggle_one_handed_mode",
                titleRes = R.string.settings_layout_option_bottom_title,
                descriptionRes = R.string.settings_layout_option_bottom_desc,
                toggleKey = AppSettingsToggleKey.ONE_HANDED_MODE,
            )
            addToggle(
                id = "app_toggle_bottom_searchbar",
                titleRes = R.string.settings_bottom_searchbar_title,
                descriptionRes = R.string.settings_bottom_searchbar_desc,
                toggleKey = AppSettingsToggleKey.BOTTOM_SEARCHBAR,
            )
            addToggle(
                id = "app_toggle_compact_layout",
                titleRes = R.string.settings_compact_layout_title,
                descriptionRes = R.string.settings_compact_layout_desc,
                toggleKey = AppSettingsToggleKey.COMPACT_LAYOUT,
                keywords = listOf("cards", "card layout", "background", "full width", "dense", "flat"),
            )
            addToggle(
                id = "app_toggle_unified_pinned_items",
                titleRes = R.string.settings_unified_pinned_items_title,
                descriptionRes = R.string.settings_unified_pinned_items_desc,
                toggleKey = AppSettingsToggleKey.UNIFIED_PINNED_ITEMS,
                keywords = listOf("pinned", "home", "sections", "list"),
            )
            addNavigation(
                id = PINNED_SECTIONS_ORDER_SETTING_ID,
                titleRes = R.string.settings_pinned_sections_order_title,
                descriptionRes = R.string.settings_pinned_sections_order_desc,
                destination = AppSettingsDestination.APPEARANCE,
                keywords = listOf("pinned", "sections", "order", "reorder", "home"),
            )
            addToggle(
                id = "app_toggle_pinned_app_shortcuts_in_app_grid",
                titleRes = R.string.settings_pinned_app_shortcuts_in_app_grid_title,
                descriptionRes = R.string.settings_pinned_app_shortcuts_in_app_grid_desc,
                toggleKey = AppSettingsToggleKey.PINNED_APP_SHORTCUTS_IN_APP_GRID,
                keywords = listOf("pinned", "shortcuts", "apps", "grid"),
            )
            addToggle(
                id = "app_toggle_search_hints",
                titleRes = R.string.settings_search_hints_title,
                descriptionRes = R.string.settings_search_hints_desc,
                toggleKey = AppSettingsToggleKey.SEARCH_HINTS,
            )
            addToggle(
                id = "app_toggle_settings_icon",
                titleRes = R.string.settings_icon_title,
                descriptionRes = R.string.settings_icon_desc,
                toggleKey = AppSettingsToggleKey.SETTINGS_ICON,
            )
            addToggle(
                id = "app_toggle_app_labels",
                titleRes = R.string.settings_show_app_labels_title,
                descriptionRes = R.string.settings_show_app_labels_desc,
                toggleKey = AppSettingsToggleKey.APP_LABELS,
                keywords = listOf("app names"),
            )
            addToggle(
                id = "app_toggle_use_system_font",
                titleRes = R.string.settings_use_system_font_title,
                descriptionRes = R.string.settings_use_system_font_desc,
                toggleKey = AppSettingsToggleKey.USE_SYSTEM_FONT,
                keywords = listOf("font", "typeface"),
            )
            addToggle(
                id = "app_toggle_alias_after_query",
                titleRes = R.string.settings_search_engine_alias_suffix_title,
                descriptionRes = R.string.settings_search_engine_alias_suffix_desc,
                toggleKey = AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX,
                keywords = listOf("suffix", "search engine"),
            )
            addToggle(
                id = "app_toggle_calculator",
                titleRes = R.string.calculator_toggle_title,
                descriptionRes = R.string.calculator_toggle_desc,
                toggleKey = AppSettingsToggleKey.CALCULATOR,
            )
            addToggle(
                id = "app_toggle_unit_converter",
                titleRes = R.string.unit_converter_info_title,
                descriptionRes = R.string.date_calculator_toggle_desc,
                toggleKey = AppSettingsToggleKey.UNIT_CONVERTER,
                destination = AppSettingsDestination.UNIT_CONVERTER_INFO,
                keywords = listOf("conversion", "examples"),
            )
            addToggle(
                id = "app_toggle_date_calculator",
                titleRes = R.string.date_calculator_info_title,
                descriptionRes = R.string.date_calculator_toggle_desc,
                toggleKey = AppSettingsToggleKey.DATE_CALCULATOR,
                destination = AppSettingsDestination.DATE_CALCULATOR_INFO,
                keywords = listOf("examples"),
            )
            addToggle(
                id = "app_toggle_currency_converter",
                titleRes = R.string.currency_converter_toggle_title,
                descriptionRes = R.string.currency_converter_toggle_desc,
                toggleKey = AppSettingsToggleKey.CURRENCY_CONVERTER,
                keywords = listOf("currency", "exchange rate", "money"),
            )
            addToggle(
                id = "app_toggle_color_visualizer",
                titleRes = R.string.color_visualizer_toggle_title,
                descriptionRes = R.string.color_visualizer_toggle_desc,
                toggleKey = AppSettingsToggleKey.COLOR_VISUALIZER,
                keywords = listOf("color", "colour", "hex", "rgb"),
            )
            addToggle(
                id = "app_toggle_world_clock",
                titleRes = R.string.world_clock_toggle_title,
                descriptionRes = R.string.world_clock_toggle_desc,
                toggleKey = AppSettingsToggleKey.WORLD_CLOCK,
                keywords = listOf("time zone", "timezone"),
            )
            addToggle(
                id = "app_toggle_dictionary",
                titleRes = R.string.dictionary_toggle_title,
                descriptionRes = R.string.dictionary_toggle_desc,
                toggleKey = AppSettingsToggleKey.DICTIONARY,
            )
            addToggle(
                id = "app_toggle_weather",
                titleRes = R.string.weather_toggle_title,
                descriptionRes = R.string.weather_toggle_desc,
                toggleKey = AppSettingsToggleKey.WEATHER,
                keywords = listOf("forecast", "temperature", "location"),
            )
            addToggle(
                id = "app_toggle_app_suggestions",
                titleRes = R.string.app_suggestions_toggle_title,
                descriptionRes = R.string.app_suggestions_toggle_desc,
                toggleKey = AppSettingsToggleKey.APP_SUGGESTIONS,
            )
            addNavigation(
                id = "app_settings_app_suggestion_tabs",
                titleRes = R.string.app_suggestions_tabs_dialog_title,
                descriptionRes = R.string.app_suggestions_tabs_dialog_message,
                destination = AppSettingsDestination.APP_SUGGESTION_TABS,
                keywords = listOf("app suggestions", "tabs", "recents", "most used", "new", "updated"),
            )
            addToggle(
                id = "app_toggle_notification_dots",
                titleRes = R.string.notification_dots_toggle_title,
                descriptionRes = R.string.notification_dots_toggle_desc,
                toggleKey = AppSettingsToggleKey.NOTIFICATION_DOTS,
                keywords = listOf("badge", "notification", "dot", "unread"),
            )
            addToggle(
                id = "app_toggle_show_all_apps_button",
                titleRes = R.string.settings_app_shortcuts_filter_all_apps,
                descriptionRes = R.string.show_all_apps_button_toggle_desc,
                toggleKey = AppSettingsToggleKey.SHOW_ALL_APPS_BUTTON,
                keywords = listOf("all apps", "app drawer"),
            )
            addToggle(
                id = "app_toggle_include_non_launchable_apps",
                titleRes = R.string.include_non_launchable_apps_toggle_title,
                descriptionRes = R.string.include_non_launchable_apps_toggle_desc,
                toggleKey = AppSettingsToggleKey.INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH,
                keywords = listOf("app info", "launch activity", "hidden apps"),
            )
            addToggle(
                id = "app_toggle_include_archived_apps",
                titleRes = R.string.include_archived_apps_toggle_title,
                descriptionRes = R.string.include_archived_apps_toggle_desc,
                toggleKey = AppSettingsToggleKey.INCLUDE_ARCHIVED_APPS_IN_SEARCH,
                keywords = listOf("archive", "archived", "offload", "restore"),
            )
            addToggle(
                id = "app_toggle_show_in_recents",
                titleRes = R.string.show_in_recents_toggle_title,
                descriptionRes = R.string.show_in_recents_toggle_desc,
                toggleKey = AppSettingsToggleKey.SHOW_IN_RECENTS,
                keywords = listOf("recent apps", "overview", "task"),
            )
            addToggle(
                id = "app_toggle_web_suggestions",
                titleRes = R.string.web_search_suggestions_title,
                toggleKey = AppSettingsToggleKey.WEB_SUGGESTIONS,
                keywords = listOf("autocomplete"),
            )
            addToggle(
                id = "app_settings_app_result_rows",
                titleRes = R.string.settings_app_result_rows_title,
                toggleKey = AppSettingsToggleKey.APP_RESULT_ROWS,
                keywords = listOf("apps", "rows", "results"),
            )
            addToggle(
                id = "app_settings_top_matches",
                titleRes = R.string.top_matches_title,
                descriptionRes = R.string.top_matches_toggle_desc,
                toggleKey = AppSettingsToggleKey.TOP_MATCHES,
                keywords = listOf("top matches", "best results", "searches"),
            )
            addNavigation(
                id = "app_settings_top_matches_priority",
                titleRes = R.string.top_matches_priority_title,
                descriptionRes = R.string.top_matches_priority_desc,
                destination = AppSettingsDestination.TOP_MATCHES_PRIORITY,
                keywords = listOf("top matches", "priority", "order", "reorder"),
            )
            addNavigation(
                id = TOP_MATCHES_COUNT_SETTING_ID,
                titleRes = R.string.top_matches_count_label,
                destination = AppSettingsDestination.SEARCH_RESULTS,
                keywords = listOf("top matches", "count", "number", "limit"),
            )
            addToggle(
                id = "app_toggle_recent_queries",
                titleRes = R.string.recent_queries_toggle_title,
                descriptionRes = R.string.recent_queries_toggle_desc,
                toggleKey = AppSettingsToggleKey.RECENT_QUERIES,
                keywords = listOf("recent"),
            )
            addToggle(
                id = "app_toggle_fuzzy_search",
                titleRes = R.string.fuzzy_search_toggle_title,
                descriptionRes = R.string.fuzzy_search_toggle_desc,
                toggleKey = AppSettingsToggleKey.FUZZY_SEARCH,
                keywords = listOf("typo", "approximate", "matching"),
            )
            addNavigation(
                id = "app_settings_search_result_ranking",
                titleRes = R.string.secondary_ranking_title,
                descriptionRes = R.string.secondary_ranking_dialog_desc,
                destination = AppSettingsDestination.SEARCH_RESULT_RANKING,
                keywords = listOf("ranking", "recency", "most opened", "sort", "order"),
            )
            addToggle(
                id = "app_toggle_open_top_result_using_keyboard",
                titleRes = R.string.open_top_result_using_keyboard_toggle_title,
                descriptionRes = R.string.open_top_result_using_keyboard_toggle_desc,
                toggleKey = AppSettingsToggleKey.OPEN_TOP_RESULT_USING_KEYBOARD,
                keywords = listOf("keyboard", "enter", "done", "top result"),
            )
            addToggle(
                id = "app_toggle_top_result_indicator",
                titleRes = R.string.top_result_indicator_toggle_title,
                descriptionRes = R.string.top_result_indicator_toggle_desc,
                toggleKey = AppSettingsToggleKey.TOP_RESULT_INDICATOR,
            )
            addToggle(
                id = "app_toggle_open_keyboard",
                titleRes = R.string.open_keyboard_toggle_title,
                descriptionRes = R.string.open_keyboard_toggle_desc,
                toggleKey = AppSettingsToggleKey.OPEN_KEYBOARD,
            )
            addToggle(
                id = "app_toggle_clear_query",
                titleRes = R.string.clear_query_toggle_title,
                descriptionRes = R.string.clear_query_toggle_desc,
                toggleKey = AppSettingsToggleKey.CLEAR_QUERY,
            )
            addToggle(
                id = "app_toggle_auto_close_overlay",
                titleRes = R.string.auto_close_overlay_toggle_title,
                descriptionRes = R.string.auto_close_overlay_toggle_desc,
                toggleKey = AppSettingsToggleKey.AUTO_CLOSE_OVERLAY,
            )
            addToggle(
                id = "app_toggle_circular_app_icons",
                titleRes = R.string.settings_circular_app_icons_title,
                descriptionRes = R.string.settings_circular_app_icons_desc,
                toggleKey = AppSettingsToggleKey.CIRCULAR_APP_ICONS,
                keywords = listOf("icon shape", "circle"),
            )
            addToggle(
                id = "app_toggle_direct_dial",
                titleRes = R.string.settings_direct_dial_title,
                descriptionRes = R.string.settings_direct_dial_desc,
                toggleKey = AppSettingsToggleKey.DIRECT_DIAL,
                keywords = listOf("call"),
            )
            addAll(context.searchSectionToggleRows())
            addNavigation(
                id = "app_toggle_wallpaper_accent",
                titleRes = R.string.settings_wallpaper_accent_title,
                descriptionRes = R.string.settings_wallpaper_accent_desc,
                destination = AppSettingsDestination.APPEARANCE,
                keywords = listOf("wallpaper", "background", "accent", "custom color"),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addToggle(
                    id = "app_toggle_device_theme",
                    titleRes = R.string.settings_device_theme_title,
                    descriptionRes = R.string.settings_device_theme_desc,
                    toggleKey = AppSettingsToggleKey.DEVICE_THEME,
                    keywords = listOf("material you", "dynamic color", "palette", "theme"),
                )
            }
            addToggle(
                id = "app_toggle_amoled_theme",
                titleRes = R.string.settings_amoled_theme_title,
                descriptionRes = R.string.settings_amoled_theme_desc,
                toggleKey = AppSettingsToggleKey.AMOLED_THEME,
                keywords = listOf("amoled", "true black", "oled", "mono", "dark"),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addToggle(
                    id = "app_toggle_themed_icons",
                    titleRes = R.string.settings_themed_icons_title,
                    descriptionRes = R.string.settings_themed_icons_desc,
                    toggleKey = AppSettingsToggleKey.THEMED_ICONS,
                )
            }
            if (!isTablet(context)) {
                addToggle(
                    id = "app_setting_apps_per_row",
                    titleRes = R.string.settings_app_columns_title,
                    toggleKey = AppSettingsToggleKey.APPS_PER_ROW,
                    keywords = listOf("grid", "layout"),
                )
            }
        }.also(::validateAppSettingsCatalog)
    }

    private fun MutableList<AppSettingResult>.addNavigation(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        destination: AppSettingsDestination,
        keywords: List<String> = emptyList(),
    ) {
        add(
            AppSettingResult(
                id = id,
                title = context.getString(titleRes),
                description = descriptionRes?.let(context::getString),
                keywords = keywords,
                action = AppSettingResultAction.NAVIGATE,
                destination = destination,
            ),
        )
    }

    fun hasPinnedNotificationItems(): Boolean = PinnedNotifications.pinnedItems(context).isNotEmpty()

    private fun MutableList<AppSettingResult>.addToggle(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        toggleKey: AppSettingsToggleKey,
        destination: AppSettingsDestination? = null,
        keywords: List<String> = emptyList(),
    ) {
        add(
            AppSettingResult(
                id = id,
                title = context.getString(titleRes),
                description = descriptionRes?.let(context::getString),
                keywords = keywords,
                action = AppSettingResultAction.TOGGLE,
                destination = destination,
                toggleKey = toggleKey,
            ),
        )
    }

    private val descriptionResolver = AppSettingDescriptionResolver(context)

    fun resolveSearchDescription(
        setting: AppSettingResult,
        queryContext: SearchQueryContext,
    ): String? = descriptionResolver.resolveSearchDescription(setting, queryContext)

}
