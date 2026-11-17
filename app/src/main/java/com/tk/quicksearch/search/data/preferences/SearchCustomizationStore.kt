package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Small preference store for search indexes that are enumerated as a group.
 *
 * Nicknames, triggers and alias codes live only here. Older builds kept them in the central
 * preference file, so those copies are moved here once, and a restored backup's copies are moved
 * by [syncAfterImport]. Startup/query scans then copy only this bounded index instead of notes,
 * images, and unrelated UI settings.
 */
internal class SearchCustomizationStore(context: Context) {
    private val appContext = context.applicationContext
    private val indexedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs =
        appContext.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureMigrated()
    }

    /** The backing preferences instance, so process-wide caches can tell when it was replaced. */
    val preferencesIdentity: Any get() = indexedPrefs

    fun snapshot(): Map<String, *> = indexedPrefs.all.filterKeys { it != KEY_SCHEMA_VERSION }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        indexedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun getString(key: String): String? =
        indexedPrefs.getString(key, null) ?: legacyPrefs.getString(key, null)?.also { value ->
            indexedPrefs.edit().putString(key, value).apply()
            legacyPrefs.edit().remove(key).apply()
        }

    fun putString(
        key: String,
        value: String?,
    ) {
        val indexedEditor = indexedPrefs.edit()
        if (value == null) indexedEditor.remove(key) else indexedEditor.putString(key, value)
        indexedEditor.apply()
        if (legacyPrefs.contains(key)) legacyPrefs.edit().remove(key).apply()
    }

    private fun ensureMigrated() {
        if (indexedPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION) return
        synchronized(migrationLock) {
            if (indexedPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION) return
            // Schema 1 dual-wrote both files, so the index already holds the current values;
            // only copy what it lacks, then drop the legacy copies.
            val legacyKeys = legacyPrefs.all.filterKeys(::isIndexedKey)
            val indexEditor = indexedPrefs.edit()
            legacyKeys.forEach { (key, value) ->
                if (value is String && !indexedPrefs.contains(key)) indexEditor.putString(key, value)
            }
            indexEditor.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).commit()
            if (legacyKeys.isNotEmpty()) {
                legacyPrefs.edit().apply { legacyKeys.keys.forEach(::remove) }.commit()
            }
        }
    }

    companion object {
        const val PREFS_NAME = "search_customization_index"
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val SCHEMA_VERSION = 2
        private val migrationLock = Any()
        private val INDEXED_PREFIXES =
            listOf(
                BasePreferences.KEY_ALIAS_CODE_PREFIX,
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

        fun isIndexedKey(key: String): Boolean = INDEXED_PREFIXES.any(key::startsWith)

        /**
         * Backups carry these keys in the central preference file. After a restore wrote them
         * there, move them into the index so they replace the current values. A full restore
         * replaces everything, so it also drops index entries the backup doesn't have.
         */
        fun syncAfterImport(
            context: Context,
            fullRestore: Boolean,
        ) {
            val store = SearchCustomizationStore(context)
            synchronized(migrationLock) {
                val imported = store.legacyPrefs.all.filterKeys(::isIndexedKey)
                val indexEditor = store.indexedPrefs.edit()
                if (fullRestore) store.snapshot().keys.forEach(indexEditor::remove)
                imported.forEach { (key, value) ->
                    if (value is String) indexEditor.putString(key, value)
                }
                if (!indexEditor.commit()) error("Failed to import search customizations")
                if (imported.isNotEmpty()) {
                    store.legacyPrefs.edit().apply { imported.keys.forEach(::remove) }.commit()
                }
            }
        }
    }
}
