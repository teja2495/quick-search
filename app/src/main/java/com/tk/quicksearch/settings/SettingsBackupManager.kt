package com.tk.quicksearch.settings.settingsScreen

import android.content.Context
import android.net.Uri
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.GeminiPreferences
import java.io.File
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes and restores backup-eligible SharedPreferences into a portable JSON file.
 */
object SettingsBackupManager {
    private const val FORMAT_VERSION = 1

    private const val FIELD_FORMAT_VERSION = "formatVersion"
    private const val FIELD_EXPORTED_AT_EPOCH_MS = "exportedAtEpochMs"
    private const val FIELD_PREFERENCES = "preferences"
    private const val FIELD_GEMINI_API_KEY = "geminiApiKey"
    private const val FIELD_TYPE = "type"
    private const val FIELD_VALUE = "value"

    private const val TYPE_STRING = "string"
    private const val TYPE_BOOLEAN = "boolean"
    private const val TYPE_INT = "int"
    private const val TYPE_LONG = "long"
    private const val TYPE_FLOAT = "float"
    private const val TYPE_STRING_SET = "string_set"

    private val excludedPreferenceFiles =
        setOf(
            BasePreferences.FIRST_LAUNCH_PREFS_NAME,
            BasePreferences.TIMING_PREFS_NAME,
            BasePreferences.SESSION_PREFS_NAME,
            "app_cache",
            BasePreferences.ENCRYPTED_PREFS_NAME,
            "app_launch_counts",
        )

    private val excludedUserPreferenceKeys =
        setOf(
            "hidden_packages_suggestions",
            "hidden_packages_results",
            "excluded_contact_ids",
            "excluded_file_uris",
            "excluded_file_extensions",
            "excluded_app_shortcuts",
            "excluded_settings",
            "search_engine_onboarding_seen",
            "has_seen_search_bar_welcome",
            "has_seen_contact_action_hint",
            "has_seen_personal_context_hint",
            "recent_app_launches",
            "last_seen_version",
            "direct_search_setup_expanded",
            "disabled_search_engines_expanded",
            "usage_permission_banner_dismiss_count",
            "usage_permission_banner_session_dismissed",
            "shortcut_hint_banner_dismiss_count",
            "shortcut_hint_banner_session_dismissed",
            "default_engine_hint_banner_dismissed",
            "last_overlay_keyboard_open_height_dp",
            "update_check_shown_this_session",
        )

    fun exportToUri(
        context: Context,
        outputUri: Uri,
        includeGeminiApiKey: Boolean = false,
    ) {
        val geminiApiKey =
            if (includeGeminiApiKey) {
                GeminiPreferences(context).getGeminiApiKey()?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        val payload =
            JSONObject()
                .put(FIELD_FORMAT_VERSION, FORMAT_VERSION)
                .put(FIELD_EXPORTED_AT_EPOCH_MS, System.currentTimeMillis())
                .put(FIELD_PREFERENCES, buildPreferencesJson(context))
                .apply {
                    geminiApiKey?.let { put(FIELD_GEMINI_API_KEY, it) }
                }

        val outputStream =
            context.contentResolver.openOutputStream(outputUri)
                ?: throw IOException("Unable to open output stream")

        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(payload.toString(2))
        }
    }

    fun importFromUri(
        context: Context,
        inputUri: Uri,
    ) {
        val inputStream =
            context.contentResolver.openInputStream(inputUri)
                ?: throw IOException("Unable to open input stream")
        val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (text.isBlank()) {
            throw IllegalArgumentException("Backup file is empty")
        }

        val root = JSONObject(text)
        val preferencesJson = root.optJSONObject(FIELD_PREFERENCES)
            ?: throw IllegalArgumentException("Invalid backup file format")
        val geminiApiKey =
            root
                .optString(FIELD_GEMINI_API_KEY, "")
                .takeIf { it.isNotBlank() }

        val importedNames = preferencesJson.keys().asSequence().toSet()
        val preferenceNames = collectEligiblePreferenceNames(context, importedNames)

        preferenceNames.forEach { prefName ->
            val sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit().clear()

            val prefEntries = preferencesJson.optJSONObject(prefName)
            if (prefEntries != null) {
                prefEntries.keys().forEach { key ->
                    if (shouldExcludeKey(prefName, key)) return@forEach
                    val valueObject = prefEntries.optJSONObject(key) ?: return@forEach
                    when (valueObject.optString(FIELD_TYPE)) {
                        TYPE_STRING -> editor.putString(key, valueObject.optString(FIELD_VALUE))
                        TYPE_BOOLEAN -> editor.putBoolean(key, valueObject.optBoolean(FIELD_VALUE))
                        TYPE_INT -> editor.putInt(key, valueObject.optInt(FIELD_VALUE))
                        TYPE_LONG -> editor.putLong(key, valueObject.optLong(FIELD_VALUE))
                        TYPE_FLOAT -> editor.putFloat(
                            key,
                            valueObject.optDouble(FIELD_VALUE).toFloat(),
                        )
                        TYPE_STRING_SET -> {
                            val jsonArray = valueObject.optJSONArray(FIELD_VALUE) ?: JSONArray()
                            val stringSet =
                                buildSet {
                                    for (index in 0 until jsonArray.length()) {
                                        add(jsonArray.optString(index))
                                    }
                                }
                            editor.putStringSet(key, stringSet)
                        }
                    }
                }
            }

            if (!editor.commit()) {
                throw IOException("Failed to import $prefName")
            }
        }

        if (root.has(FIELD_GEMINI_API_KEY)) {
            GeminiPreferences(context).setGeminiApiKey(geminiApiKey)
        }
    }

    private fun buildPreferencesJson(context: Context): JSONObject {
        val preferencesJson = JSONObject()
        collectEligiblePreferenceNames(context)
            .sorted()
            .forEach { prefName ->
                val pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                val prefEntriesJson = JSONObject()
                pref.all
                    .asSequence()
                    .filterNot { shouldExcludeKey(prefName, it.key) }
                    .sortedBy { it.key }
                    .forEach { (key, value) ->
                        serializePreferenceValue(value)?.let { serialized ->
                            prefEntriesJson.put(key, serialized)
                        }
                    }
                preferencesJson.put(prefName, prefEntriesJson)
            }
        return preferencesJson
    }

    private fun collectEligiblePreferenceNames(
        context: Context,
        additionalNames: Set<String> = emptySet(),
    ): Set<String> {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val discoveredNames =
            sharedPrefsDir.listFiles()
                ?.asSequence()
                ?.mapNotNull { file ->
                    val name = file.name
                    if (name.endsWith(".xml")) {
                        name.removeSuffix(".xml")
                    } else {
                        null
                    }
                }?.toSet()
                ?: emptySet()

        return (discoveredNames + additionalNames + BasePreferences.PREFS_NAME)
            .filterNot { it in excludedPreferenceFiles }
            .toSet()
    }

    private fun shouldExcludeKey(
        prefName: String,
        key: String,
    ): Boolean = prefName == BasePreferences.PREFS_NAME && key in excludedUserPreferenceKeys

    private fun serializePreferenceValue(value: Any?): JSONObject? =
        when (value) {
            is String ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_STRING)
                    .put(FIELD_VALUE, value)
            is Boolean ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_BOOLEAN)
                    .put(FIELD_VALUE, value)
            is Int ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_INT)
                    .put(FIELD_VALUE, value)
            is Long ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_LONG)
                    .put(FIELD_VALUE, value)
            is Float ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_FLOAT)
                    .put(FIELD_VALUE, value.toDouble())
            is Set<*> -> {
                val stringValues = value.mapNotNull { it as? String }
                JSONObject()
                    .put(FIELD_TYPE, TYPE_STRING_SET)
                    .put(FIELD_VALUE, JSONArray(stringValues))
            }
            else -> {
                null
            }
        }
}
