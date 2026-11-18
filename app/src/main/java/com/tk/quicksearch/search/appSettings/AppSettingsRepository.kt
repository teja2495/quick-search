package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.notchGesture.NotchGestureSupport
import android.content.Context
import android.os.Build
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.data.preferences.RATE_QUICK_SEARCH_ENABLED
import com.tk.quicksearch.search.utils.SearchQueryContext

class AppSettingsRepository(
    private val context: Context,
) {
    fun loadSettings(): List<AppSettingResult> {
        return AppSettingsCatalogBuilder(context).apply {
            addNavigation(
                id = "app_settings_appearance",
                titleRes = R.string.settings_appearance_title,
                descriptionRes = R.string.settings_appearance_desc,
                destination = AppSettingsDestination.APPEARANCE,
                keywords = listOf("wallpaper", "style", "search engines style", "inline", "compact"),
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                addNavigation(
                    id = "app_settings_launcher_icon",
                    titleRes = R.string.settings_launcher_icon_title,
                    destination = AppSettingsDestination.LAUNCHER_ICON,
                    keywords = listOf("app icon", "launcher icon", "change icon", "home screen icon"),
                )
            }
            addNavigation(
                id = "app_settings_search_bar_style",
                titleRes = R.string.settings_search_bar_style_title,
                destination = AppSettingsDestination.APPEARANCE,
                keywords =
                    listOf("search bar", "border", "outline", "thickness", "corner", "rounded", "colour", "color", "transparency", "background"),
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
                keywords = if (NotchGestureSupport.isSupported(context)) listOf("notch") else emptyList(),
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
            BooleanSettings.entries.forEach { addBooleanSetting(it.setting) }
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
                keywords = listOf("swipe", "quick note", "keyboard", "notifications") +
                    if (NotchGestureSupport.isSupported(context)) listOf("notch") else emptyList(),
            )
            GESTURE_APP_SETTINGS.forEach { (id, titleRes, destination) ->
                if (destination.isNotchGestureDestination && !NotchGestureSupport.isSupported(context)) return@forEach
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
                id = "app_settings_create_notification_note",
                titleRes = R.string.app_settings_search_create_notification_note_title,
                descriptionRes = R.string.app_settings_search_create_notification_note_desc,
                destination = AppSettingsDestination.CREATE_NOTIFICATION_NOTE,
                keywords = listOf("sticky note", "pin note", "notification panel", "shade"),
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
            addNavigation(
                id = "app_settings_bookmarks_list",
                titleRes = R.string.section_bookmarks,
                descriptionRes = R.string.settings_bookmarks_view_all_desc,
                destination = AppSettingsDestination.BOOKMARKS_LIST,
                keywords = listOf("bookmark", "website", "websites", "link", "links", "url", "favorites", "import bookmarks", "export bookmarks", "bookmark browser"),
            )
            addToggleAndInlineSettings(context)
        }.build().also(::validateAppSettingsCatalog)
    }

    fun hasPinnedNotificationItems(): Boolean = PinnedNotifications.pinnedItems(context).isNotEmpty()

    private val descriptionResolver = AppSettingDescriptionResolver(context)

    fun resolveSearchDescription(
        setting: AppSettingResult,
        queryContext: SearchQueryContext,
    ): String? = descriptionResolver.resolveSearchDescription(setting, queryContext)
}
