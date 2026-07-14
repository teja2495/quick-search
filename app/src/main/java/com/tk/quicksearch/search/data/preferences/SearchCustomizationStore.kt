package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Small preference store for search indexes that are enumerated as a group.
 *
 * Values are copied once from the legacy central preference file and dual-written for one release
 * so existing backups and downgrades remain compatible. Startup/query scans then copy only this
 * bounded index instead of notes, images, and unrelated UI settings.
 */
internal class SearchCustomizationStore(context: Context) {
    private val appContext = context.applicationContext
    private val indexedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs =
        appContext.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureMigrated()
    }

    fun snapshot(): Map<String, *> = indexedPrefs.all.filterKeys { it != KEY_SCHEMA_VERSION }

    fun getString(key: String): String? =
        indexedPrefs.getString(key, null) ?: legacyPrefs.getString(key, null)?.also { value ->
            indexedPrefs.edit().putString(key, value).apply()
        }

    fun putString(
        key: String,
        value: String?,
    ) {
        val indexedEditor = indexedPrefs.edit()
        val legacyEditor = legacyPrefs.edit()
        if (value == null) {
            indexedEditor.remove(key)
            legacyEditor.remove(key)
        } else {
            indexedEditor.putString(key, value)
            legacyEditor.putString(key, value)
        }
        indexedEditor.apply()
        legacyEditor.apply()
    }

    private fun ensureMigrated() {
        if (indexedPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION) return
        synchronized(migrationLock) {
            if (indexedPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION) return
            val editor = indexedPrefs.edit()
            legacyPrefs.all.forEach { (key, value) ->
                if (value is String && isIndexedKey(key)) editor.putString(key, value)
            }
            editor.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).commit()
        }
    }

    private fun isIndexedKey(key: String): Boolean =
        INDEXED_PREFIXES.any(key::startsWith)

    private companion object {
        const val PREFS_NAME = "search_customization_index"
        const val KEY_SCHEMA_VERSION = "schema_version"
        const val SCHEMA_VERSION = 1
        val migrationLock = Any()
        val INDEXED_PREFIXES =
            listOf(
                BasePreferences.KEY_ALIAS_CODE_PREFIX,
                BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY,
                BasePreferences.KEY_NICKNAME_APP_PREFIX,
                BasePreferences.KEY_NICKNAME_CONTACT_PREFIX,
                BasePreferences.KEY_NICKNAME_FILE_PREFIX,
                BasePreferences.KEY_NICKNAME_SETTING_PREFIX,
                BasePreferences.KEY_NICKNAME_CALENDAR_EVENT_PREFIX,
                BasePreferences.KEY_TRIGGER_APP_PREFIX,
                BasePreferences.KEY_TRIGGER_CONTACT_PREFIX,
                BasePreferences.KEY_TRIGGER_FILE_PREFIX,
                BasePreferences.KEY_TRIGGER_SETTING_PREFIX,
                BasePreferences.KEY_TRIGGER_NOTE_PREFIX,
            )
    }
}
