package com.tk.quicksearch.search.appSettings

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.notchGesture.NotchGestureSupport
import com.tk.quicksearch.settings.gestureSettings.GestureSettingTarget
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.tools.tasker.TaskerIntegration

internal const val AI_MODEL_SETTING_ID = "app_settings_ai_model"

internal fun AppSettingsCatalogBuilder.addGestureSettings(context: Context) {
    for (target in GestureSettingTarget.searchableRows(NotchGestureSupport.isSupported(context))) {
        addNavigation(
            id = target.searchId,
            titleRes = target.searchTitleResId,
            descriptionRes = R.string.settings_gestures_title,
            destination = AppSettingsTarget.Gesture(target),
        )
    }
}

internal fun Context.isTaskerInstalled(): Boolean =
    runCatching { packageManager.getPackageInfo(TaskerIntegration.PACKAGE_NAME, 0) }.isSuccess

/** Null for sections without a settings toggle (Noted). */
internal fun searchSectionToggleId(section: SearchSection): String? =
    when (section) {
        SearchSection.APPS -> "app_toggle_search_apps"
        SearchSection.APP_SHORTCUTS -> "app_toggle_search_app_shortcuts"
        SearchSection.CONTACTS -> "app_toggle_search_contacts"
        SearchSection.FILES -> "app_toggle_search_files"
        SearchSection.SETTINGS -> "app_toggle_search_device_settings"
        SearchSection.CALENDAR -> "app_toggle_search_calendar"
        SearchSection.REMINDERS -> "app_toggle_search_reminders"
        SearchSection.NOTES -> "app_toggle_search_notes"
        SearchSection.BOOKMARKS -> "app_toggle_search_bookmarks"
        SearchSection.APP_SETTINGS -> "app_toggle_search_app_settings"
        SearchSection.NOTED -> null
    }

internal fun searchSectionToggleTitleRes(section: SearchSection): Int? =
    when (section) {
        SearchSection.APPS -> R.string.search_section_apps_toggle_title
        SearchSection.APP_SHORTCUTS -> R.string.search_section_app_shortcuts_toggle_title
        SearchSection.CONTACTS -> R.string.search_section_contacts_toggle_title
        SearchSection.FILES -> R.string.search_section_files_toggle_title
        SearchSection.SETTINGS -> R.string.search_section_device_settings_toggle_title
        SearchSection.CALENDAR -> R.string.search_section_calendar_toggle_title
        SearchSection.REMINDERS -> R.string.search_section_reminders_toggle_title
        SearchSection.NOTES -> R.string.search_section_notes_toggle_title
        SearchSection.BOOKMARKS -> R.string.search_section_bookmarks_toggle_title
        SearchSection.APP_SETTINGS -> R.string.search_section_app_settings_toggle_title
        SearchSection.NOTED -> null
    }

internal fun searchSectionToggleDescriptionRes(section: SearchSection): Int? =
    when (section) {
        SearchSection.APPS -> R.string.search_section_apps_toggle_desc
        SearchSection.APP_SHORTCUTS -> R.string.search_section_app_shortcuts_toggle_desc
        SearchSection.CONTACTS -> R.string.search_section_contacts_toggle_desc
        SearchSection.FILES -> R.string.search_section_files_toggle_desc
        SearchSection.SETTINGS -> R.string.search_section_device_settings_toggle_desc
        SearchSection.CALENDAR -> R.string.search_section_calendar_toggle_desc
        SearchSection.REMINDERS -> R.string.search_section_reminders_toggle_desc
        SearchSection.NOTES -> R.string.search_section_notes_toggle_desc
        SearchSection.BOOKMARKS -> R.string.search_section_bookmarks_toggle_desc
        SearchSection.APP_SETTINGS -> R.string.search_section_app_settings_toggle_desc
        SearchSection.NOTED -> null
    }

/** One toggle row per search section that has a settings toggle, in registry order. */
internal fun Context.searchSectionToggleRows(): List<AppSettingResult> =
    SearchSectionRegistry.orderedDefinitions.mapNotNull { definition ->
        val toggleKey = definition.appSettingsToggleKey ?: return@mapNotNull null
        AppSettingResult(
            id = searchSectionToggleId(definition.section) ?: return@mapNotNull null,
            title = getString(searchSectionToggleTitleRes(definition.section) ?: return@mapNotNull null),
            description = getString(searchSectionToggleDescriptionRes(definition.section) ?: return@mapNotNull null),
            action = AppSettingResultAction.TOGGLE,
            toggleKey = toggleKey,
        )
    }
