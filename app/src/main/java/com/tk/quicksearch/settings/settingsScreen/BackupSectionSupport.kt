package com.tk.quicksearch.settings.settingsScreen

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import org.json.JSONArray

internal data class BackupSectionOption(
    @StringRes val titleRes: Int,
    val icon: () -> ImageVector,
    val isAvailable: (BackupSectionAvailability) -> Boolean,
)

internal class BackupSectionAvailability(val context: Context) {
    val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
    }
}

internal data class BackupSectionPayload(
    val field: String,
    val export: (Context) -> JSONArray,
    val restore: (Context, JSONArray) -> Unit,
)

internal fun isSearchHistoryKey(
    prefName: String,
    key: String,
): Boolean =
    (prefName == BasePreferences.SESSION_PREFS_NAME && key == BasePreferences.KEY_RECENT_QUERIES) ||
        (prefName == BasePreferences.PREFS_NAME && key == BasePreferences.KEY_RECENT_RESULT_OPENS)

internal fun isPinnedItemKey(
    prefName: String,
    key: String,
): Boolean {
    if (prefName != BasePreferences.PREFS_NAME) return false
    return key == BasePreferences.KEY_PINNED ||
        key == BasePreferences.KEY_PINNED_NON_APP_ITEM_ORDER ||
        key == BasePreferences.KEY_PINNED_CONTACT_IDS ||
        key == BasePreferences.KEY_PINNED_CONTACT_ORDER ||
        key == BasePreferences.KEY_PINNED_FILE_URIS ||
        key == BasePreferences.KEY_PINNED_FILE_ORDER ||
        key == BasePreferences.KEY_PINNED_SETTINGS ||
        key == BasePreferences.KEY_PINNED_SETTINGS_ORDER ||
        key == BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS ||
        key == BasePreferences.KEY_PINNED_CALENDAR_EVENT_ORDER ||
        key == BasePreferences.KEY_PINNED_NOTE_IDS ||
        key == BasePreferences.KEY_PINNED_NOTE_ORDER ||
        key == BasePreferences.KEY_PINNED_REMINDER_IDS ||
        key == BasePreferences.KEY_PINNED_REMINDER_ORDER ||
        key == BasePreferences.KEY_PINNED_APP_SHORTCUTS ||
        key == BasePreferences.KEY_PINNED_APP_SHORTCUT_ORDER ||
        key == BasePreferences.KEY_APP_FOLDERS ||
        key == UiPreferences.KEY_PINNED_APP_GRID_ORDER
}

internal fun isShortcutKey(
    prefName: String,
    key: String,
): Boolean {
    if (prefName != BasePreferences.PREFS_NAME) return false
    return key == BasePreferences.KEY_DISABLED_APP_SHORTCUTS ||
        key.startsWith(BasePreferences.KEY_APP_SHORTCUT_ICON_OVERRIDE_PREFIX) ||
        key.startsWith(BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX)
}

internal fun isSearchEngineKey(
    prefName: String,
    key: String,
): Boolean {
    if (prefName != BasePreferences.PREFS_NAME) return false
    return key == BasePreferences.KEY_DISABLED_SEARCH_ENGINES ||
        key == BasePreferences.KEY_SEARCH_ENGINE_ORDER ||
        key == BasePreferences.KEY_CUSTOM_SEARCH_ENGINES ||
        key == BasePreferences.KEY_SEARCH_ENGINE_COMPACT_MODE ||
        key == BasePreferences.KEY_SEARCH_ENGINE_COMPACT_ROW_COUNT ||
        key == BasePreferences.KEY_SEARCH_ENGINE_ALIAS_SUFFIX_ENABLED ||
        key == BasePreferences.KEY_ALIAS_TRIGGER_AFTER_SPACE ||
        key == BasePreferences.KEY_AMAZON_DOMAIN ||
        key.startsWith(BasePreferences.KEY_ALIAS_CODE_PREFIX)
}

internal fun isNotesKey(
    prefName: String,
    key: String,
): Boolean {
    if (prefName != BasePreferences.PREFS_NAME) return false
    return key == BasePreferences.KEY_NOTE_ID_COUNTER ||
        key == BasePreferences.KEY_PINNED_NOTE_IDS ||
        key == BasePreferences.KEY_PINNED_NOTE_ORDER ||
        key == BasePreferences.KEY_QUICK_NOTE_ID ||
        key == BasePreferences.KEY_QUICK_NOTE_ENABLED ||
        key.startsWith(BasePreferences.KEY_TRIGGER_NOTE_PREFIX)
}

internal fun isRemindersKey(
    prefName: String,
    key: String,
): Boolean {
    if (prefName != BasePreferences.PREFS_NAME) return false
    return key == BasePreferences.KEY_REMINDERS_DATA ||
        key == BasePreferences.KEY_REMINDER_ID_COUNTER
}
