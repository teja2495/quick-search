package com.tk.quicksearch.search.startup

import com.tk.quicksearch.search.appSettings.AppSettingsAction
import com.tk.quicksearch.search.appSettings.AppSettingsRouteAction
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.settings.settingsDetailScreen.GestureSettingTarget
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

internal fun AppSettingsTarget.snapshotValue(): String =
    when (this) {
        is AppSettingsTarget.Page -> "page:${detailType.name}"
        is AppSettingsTarget.Gesture -> "gesture"
        is AppSettingsAction -> name
        is AppSettingsRouteAction -> name
    }

internal fun readStartupSettingsTarget(value: String, rowId: String): AppSettingsTarget? {
    if (value == "gesture" || value.startsWith("GESTURE_")) {
        return GestureSettingTarget.searchableRows(notchSupported = true)
            .firstOrNull { it.searchId == rowId }
            ?.let(AppSettingsTarget::Gesture)
    }
    if (!value.startsWith("page:")) {
        AppSettingsAction.entries.firstOrNull { it.name == value }?.let { return it }
        AppSettingsRouteAction.entries.firstOrNull { it.name == value }?.let { return it }
    }
    val pageName = when (value) {
        "AI_SEARCH_CONFIGURE", "GEMINI_API" -> "GEMINI_API_CONFIG"
        "NOTES_LIST" -> "NOTES"
        "REMINDERS_LIST" -> "REMINDERS"
        "BOOKMARKS_LIST" -> "BOOKMARKS"
        else -> value.removePrefix("page:")
    }
    return SettingsDetailType.entries.firstOrNull { it.name == pageName }?.let(AppSettingsTarget::Page)
}
