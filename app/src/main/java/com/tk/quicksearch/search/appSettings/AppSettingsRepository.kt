package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
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
            addPageShortcut(SettingsDetailType.APPEARANCE)
            addNavigation(
                id = THEME_MODE_SETTING_ID,
                titleRes = R.string.settings_app_theme_title,
                destination = AppSettingsTarget.Page(SettingsDetailType.APPEARANCE),
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
                    destination = AppSettingsRouteAction.LAUNCHER_ICON,
                    keywords = listOf("app icon", "launcher icon", "change icon", "home screen icon"),
                )
            }
            addNavigation(
                id = "app_settings_search_bar_style",
                titleRes = R.string.settings_search_bar_style_title,
                destination = AppSettingsTarget.Page(SettingsDetailType.APPEARANCE),
                keywords =
                    listOf("search bar", "border", "outline", "thickness", "corner", "rounded", "colour", "color", "transparency", "background"),
            )
            addNavigation(
                id = "app_settings_result_card_style",
                titleRes = R.string.settings_result_card_style_title,
                destination = AppSettingsTarget.Page(SettingsDetailType.APPEARANCE),
                keywords =
                    listOf("cards", "result cards", "corner", "rounded", "radius", "transparency", "transparent", "opacity", "blur", "blurred", "frosted", "home", "at a glance", "pinned"),
            )
            addNavigation(
                id = "app_settings_icon_packs",
                titleRes = R.string.settings_icon_pack_title,
                descriptionRes = R.string.settings_search_description_change_icon_pack,
                destination = AppSettingsRouteAction.ICON_PACKS,
                keywords = listOf("icons", "icon pack", "themed icons"),
            )
            addPageShortcut(SettingsDetailType.SEARCH_RESULTS)
            addPageShortcut(SettingsDetailType.SEARCH_ENGINES)
            addPageShortcut(SettingsDetailType.GEMINI_API_CONFIG)
            addNavigation(
                id = AI_MODEL_SETTING_ID,
                titleRes = R.string.settings_direct_search_model_label,
                destination = AppSettingsRouteAction.AI_MODEL,
                keywords = listOf("ai model", "llm", "thinking", "web search", "grounding"),
            )
            addPageShortcut(SettingsDetailType.API_KEY_SETUP)
            addPageShortcut(SettingsDetailType.TOOLS)
            addPageShortcut(SettingsDetailType.AT_A_GLANCE)
            addToggle(
                id = "app_toggle_app_notifications",
                titleRes = R.string.settings_at_a_glance_app_notifications_title,
                descriptionRes = R.string.settings_at_a_glance_app_notifications_desc,
                toggleKey = AppSettingsToggleKey.APP_NOTIFICATIONS,
                destination = AppSettingsRouteAction.APP_NOTIFICATIONS,
                keywords = listOf("at a glance", "notification rules", "notification filter", "keywords"),
            )
            if (context.isTaskerInstalled()) {
                addNavigation(
                    id = "app_settings_tasker_integration",
                    titleRes = R.string.tasker_integration_title,
                    descriptionRes = R.string.tasker_integration_description,
                    destination = AppSettingsTarget.Page(SettingsDetailType.TASKER_INTEGRATION),
                    keywords = listOf("tasker", "broadcast", "intent", "automation", "action", "alias"),
                )
            }
            addNavigation(
                id = "app_settings_export_settings",
                titleRes = R.string.settings_backup_export_title,
                descriptionRes = R.string.settings_backup_export_desc,
                destination = AppSettingsRouteAction.EXPORT_SETTINGS,
                keywords = listOf("backup", "save"),
            )
            // The schedule lives in the Export Data drawer.
            addNavigation(
                id = "app_settings_schedule_auto_backup",
                titleRes = R.string.auto_backup_schedule_title,
                descriptionRes = R.string.auto_backup_schedule_desc,
                destination = AppSettingsRouteAction.EXPORT_SETTINGS,
                keywords = listOf("auto backup", "automatic", "schedule", "folder", "backup"),
            )
            addNavigation(
                id = "app_settings_import_settings",
                titleRes = R.string.setup_import_button,
                descriptionRes = R.string.settings_backup_import_warning_message,
                destination = AppSettingsRouteAction.IMPORT_SETTINGS,
                keywords = listOf("backup", "restore"),
            )
            addNavigation(
                id = "app_settings_launch_options",
                titleRes = R.string.settings_launch_options_title,
                descriptionRes = R.string.settings_launch_options_desc,
                destination = AppSettingsTarget.Page(SettingsDetailType.LAUNCH_OPTIONS),
                keywords = if (NotchGestureSupport.isSupported(context)) listOf("notch") else emptyList(),
            )
            addPageShortcut(SettingsDetailType.EDGE_GESTURE)
            addPageShortcut(SettingsDetailType.FLOATING_BUTTON)
            addNavigation(
                id = "app_settings_default_assistant",
                titleRes = R.string.settings_default_assistant_title,
                descriptionRes = R.string.settings_default_assistant_desc,
                destination = AppSettingsAction.SET_DEFAULT_ASSISTANT,
                keywords = listOf("digital assistant"),
            )
            addNavigation(
                id = "app_settings_default_launcher",
                titleRes = R.string.settings_default_launcher_title,
                descriptionRes = R.string.settings_default_launcher_desc,
                destination = AppSettingsAction.SET_DEFAULT_LAUNCHER,
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
                destination = AppSettingsAction.ADD_HOME_SCREEN_WIDGET,
            )
            addNavigation(
                id = "app_settings_quick_settings_tile",
                titleRes = R.string.settings_quick_settings_tile_title,
                descriptionRes = R.string.settings_quick_settings_tile_desc,
                destination = AppSettingsAction.ADD_QUICK_SETTINGS_TILE,
            )
            addPageShortcut(SettingsDetailType.MORE_OPTIONS)
            addPageShortcut(SettingsDetailType.PERMISSIONS)
            addPageShortcut(SettingsDetailType.APP_MANAGEMENT)
            addPageShortcut(SettingsDetailType.APP_SHORTCUTS)
            addPageShortcut(SettingsDetailType.CALLS_TEXTS)
            addToggle(
                id = "app_toggle_number_search",
                titleRes = R.string.settings_number_search_title,
                descriptionRes = R.string.settings_number_search_desc,
                toggleKey = AppSettingsToggleKey.NUMBER_SEARCH,
                keywords = listOf("phone", "phone number", "contacts"),
            )
            addPageShortcut(SettingsDetailType.FILES)
            BooleanSettings.entries.forEach { addBooleanSetting(it.setting) }
            addNavigation(
                id = "app_settings_reload_apps",
                titleRes = R.string.settings_refresh_apps_title,
                destination = AppSettingsAction.RELOAD_APPS,
                keywords = listOf("refresh apps"),
            )
            addNavigation(
                id = "app_settings_reload_contacts",
                titleRes = R.string.settings_refresh_contacts_title,
                destination = AppSettingsAction.RELOAD_CONTACTS,
                keywords = listOf("refresh contacts", "sync contacts"),
            )
            addNavigation(
                id = "app_settings_reload_files",
                titleRes = R.string.settings_refresh_files_title,
                destination = AppSettingsAction.RELOAD_FILES,
                keywords = listOf("refresh files"),
            )
            addPageShortcut(SettingsDetailType.DEVICE_SETTINGS)
            addPageShortcut(SettingsDetailType.EXCLUDED_ITEMS)
            addNavigation(
                id = "app_settings_pinned_notification_items",
                titleRes = R.string.notification_pinned_items_title,
                descriptionRes = R.string.notification_pinned_items_description,
                destination = AppSettingsRouteAction.PINNED_NOTIFICATION_ITEMS,
                keywords = listOf("pinned", "notification", "notification panel", "unpin", "reorder"),
            )
            addPageShortcut(SettingsDetailType.NICKNAMES)
            addPageShortcut(SettingsDetailType.TRIGGERS)
            addPageShortcut(
                SettingsDetailType.GESTURES,
                extraKeywords = if (NotchGestureSupport.isSupported(context)) listOf("notch") else emptyList(),
            )
            addGestureSettings(context)
            addPageShortcut(SettingsDetailType.CALENDAR_EVENTS)
            addNavigation(
                id = "app_settings_open_events_in",
                titleRes = R.string.settings_calendar_default_title,
                destination = AppSettingsRouteAction.OPEN_EVENTS_IN,
                keywords = listOf("calendar app", "open calendar", "event app"),
            )
            addNavigation(
                id = "app_settings_send_feedback",
                titleRes = R.string.settings_feedback_send_title,
                descriptionRes = R.string.settings_feedback_send_desc,
                destination = AppSettingsAction.SEND_FEEDBACK,
                keywords = listOf("support", "bug", "request"),
            )
            if (RATE_QUICK_SEARCH_ENABLED) {
                addNavigation(
                    id = "app_settings_rate_quick_search",
                    titleRes = R.string.settings_feedback_rate_title,
                    descriptionRes = R.string.settings_feedback_rate_desc,
                    destination = AppSettingsAction.RATE_QUICK_SEARCH,
                    keywords = listOf("review"),
                )
            }
            addNavigation(
                id = "app_settings_development",
                titleRes = R.string.settings_feedback_github_title,
                descriptionRes = R.string.settings_feedback_github_desc,
                destination = AppSettingsAction.DEVELOPMENT,
                keywords = listOf("github", "code"),
            )
            addPageShortcut(SettingsDetailType.FEATURES_LIST)
            addNavigation(
                id = "app_settings_release_notes",
                titleRes = R.string.settings_release_notes_title,
                descriptionRes = R.string.settings_release_notes_desc,
                destination = AppSettingsRouteAction.RELEASE_NOTES,
                keywords = listOf("what's new", "changelog", "update", "version"),
            )
            addNavigation(
                id = "app_settings_app_language",
                titleRes = R.string.settings_app_language_title,
                destination = AppSettingsRouteAction.APP_LANGUAGE,
                keywords = listOf("locale", "translation", "translate"),
            )
            addNavigation(
                id = "app_settings_create_note",
                titleRes = R.string.app_settings_search_create_note_title,
                descriptionRes = R.string.app_settings_search_create_note_desc,
                destination = AppSettingsAction.CREATE_NOTE,
                keywords = listOf("new note", "memo", "write"),
            )
            addNavigation(
                id = "app_settings_create_notification_note",
                titleRes = R.string.app_settings_search_create_notification_note_title,
                descriptionRes = R.string.app_settings_search_create_notification_note_desc,
                destination = AppSettingsAction.CREATE_NOTIFICATION_NOTE,
                keywords = listOf("sticky note", "pin note", "notification panel", "shade"),
            )
            addPageShortcut(SettingsDetailType.NOTES)
            addNavigation(
                id = "app_settings_create_reminder",
                titleRes = R.string.app_setting_create_reminder,
                descriptionRes = R.string.app_setting_create_reminder_desc,
                destination = AppSettingsAction.CREATE_REMINDER,
                keywords = listOf("new reminder", "add reminder", "remind me", "alarm"),
            )
            addPageShortcut(SettingsDetailType.REMINDERS)
            addPageShortcut(SettingsDetailType.BOOKMARKS)
            addToggleAndInlineSettings(context)
            addRemainingPageShortcuts()
        }.build().also(::validateAppSettingsCatalog)
    }

    fun hasPinnedNotificationItems(): Boolean = PinnedNotifications.pinnedItems(context).isNotEmpty()

    private val descriptionResolver = AppSettingDescriptionResolver(context)

    fun resolveSearchDescription(
        setting: AppSettingResult,
        queryContext: SearchQueryContext,
    ): String? = descriptionResolver.resolveSearchDescription(setting, queryContext)
}
