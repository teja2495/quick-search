package com.tk.quicksearch.search.appSettings

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.tools.tasker.TaskerIntegration

internal const val AI_MODEL_SETTING_ID = "app_settings_ai_model"

/** One searchable row per gesture; titles match the rows on the Gestures settings page. */
internal val GESTURE_APP_SETTINGS: List<Triple<String, Int, AppSettingsDestination>> =
    listOf(
        Triple("app_settings_gesture_swipe_left", R.string.settings_gesture_swipe_left, AppSettingsDestination.GESTURE_SWIPE_LEFT),
        Triple("app_settings_gesture_swipe_right", R.string.settings_gesture_swipe_right, AppSettingsDestination.GESTURE_SWIPE_RIGHT),
        Triple("app_settings_gesture_swipe_up", R.string.settings_gesture_swipe_up_home, AppSettingsDestination.GESTURE_SWIPE_UP),
        Triple("app_settings_gesture_swipe_down", R.string.settings_gesture_swipe_down_home, AppSettingsDestination.GESTURE_SWIPE_DOWN),
        Triple("app_settings_gesture_double_tap", R.string.settings_gesture_double_tap_home, AppSettingsDestination.GESTURE_DOUBLE_TAP),
        Triple("app_settings_gesture_open_keyboard", R.string.action_open_keyboard, AppSettingsDestination.GESTURE_OPEN_KEYBOARD),
        Triple("app_settings_gesture_close_keyboard", R.string.settings_gesture_close_keyboard, AppSettingsDestination.GESTURE_CLOSE_KEYBOARD),
    )

internal fun Context.isTaskerInstalled(): Boolean =
    runCatching { packageManager.getPackageInfo(TaskerIntegration.PACKAGE_NAME, 0) }.isSuccess

internal val AppSettingsDestination.isGestureDestination: Boolean
    get() = GESTURE_APP_SETTINGS.any { it.third == this }

internal fun searchSectionToggleId(section: SearchSection): String =
    when (section) {
        SearchSection.APPS -> "app_toggle_search_apps"
        SearchSection.APP_SHORTCUTS -> "app_toggle_search_app_shortcuts"
        SearchSection.CONTACTS -> "app_toggle_search_contacts"
        SearchSection.FILES -> "app_toggle_search_files"
        SearchSection.SETTINGS -> "app_toggle_search_device_settings"
        SearchSection.CALENDAR -> "app_toggle_search_calendar"
        SearchSection.REMINDERS -> "app_toggle_search_reminders"
        SearchSection.NOTES -> "app_toggle_search_notes"
        SearchSection.APP_SETTINGS -> "app_toggle_search_app_settings"
    }

internal fun searchSectionToggleTitleRes(section: SearchSection): Int =
    when (section) {
        SearchSection.APPS -> R.string.search_section_apps_toggle_title
        SearchSection.APP_SHORTCUTS -> R.string.search_section_app_shortcuts_toggle_title
        SearchSection.CONTACTS -> R.string.search_section_contacts_toggle_title
        SearchSection.FILES -> R.string.search_section_files_toggle_title
        SearchSection.SETTINGS -> R.string.search_section_device_settings_toggle_title
        SearchSection.CALENDAR -> R.string.search_section_calendar_toggle_title
        SearchSection.REMINDERS -> R.string.search_section_reminders_toggle_title
        SearchSection.NOTES -> R.string.search_section_notes_toggle_title
        SearchSection.APP_SETTINGS -> R.string.search_section_app_settings_toggle_title
    }

internal fun searchSectionToggleDescriptionRes(section: SearchSection): Int =
    when (section) {
        SearchSection.APPS -> R.string.search_section_apps_toggle_desc
        SearchSection.APP_SHORTCUTS -> R.string.search_section_app_shortcuts_toggle_desc
        SearchSection.CONTACTS -> R.string.search_section_contacts_toggle_desc
        SearchSection.FILES -> R.string.search_section_files_toggle_desc
        SearchSection.SETTINGS -> R.string.search_section_device_settings_toggle_desc
        SearchSection.CALENDAR -> R.string.search_section_calendar_toggle_desc
        SearchSection.REMINDERS -> R.string.search_section_reminders_toggle_desc
        SearchSection.NOTES -> R.string.search_section_notes_toggle_desc
        SearchSection.APP_SETTINGS -> R.string.search_section_app_settings_toggle_desc
    }

/** One toggle row per search section, in registry order. */
internal fun Context.searchSectionToggleRows(): List<AppSettingResult> =
    SearchSectionRegistry.orderedDefinitions.map { definition ->
        AppSettingResult(
            id = searchSectionToggleId(definition.section),
            title = getString(searchSectionToggleTitleRes(definition.section)),
            description = getString(searchSectionToggleDescriptionRes(definition.section)),
            action = AppSettingResultAction.TOGGLE,
            toggleKey = definition.appSettingsToggleKey,
        )
    }
