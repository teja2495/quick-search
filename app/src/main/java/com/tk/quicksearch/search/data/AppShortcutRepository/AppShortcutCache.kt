package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isValidShortcutId
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal class AppShortcutCache(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCachedShortcuts(): List<StaticShortcut>? {
        if (!prefs.contains(KEY_SHORTCUT_LIST)) return null

        return runCatching {
            val json = prefs.getString(KEY_SHORTCUT_LIST, null) ?: return null
            if (json.length < 10 || json == "[]") return null
            JSONArray(json).toShortcutList()
        }.getOrNull()
    }

    fun saveShortcuts(shortcuts: List<StaticShortcut>): Boolean =
        runCatching {
            val json = shortcuts.toShortcutJsonArray().toString()
            prefs
                .edit()
                .putString(KEY_SHORTCUT_LIST, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            true
        }.getOrDefault(false)

    fun loadCustomShortcuts(): List<StaticShortcut>? {
        if (!prefs.contains(KEY_CUSTOM_SHORTCUT_LIST)) return null
        return runCatching {
            val json = prefs.getString(KEY_CUSTOM_SHORTCUT_LIST, null) ?: return null
            if (json.isBlank() || json == "[]") return emptyList()
            JSONArray(json).toShortcutList()
        }.getOrNull()
    }

    fun saveCustomShortcuts(shortcuts: List<StaticShortcut>): Boolean =
        runCatching {
            val json = shortcuts.toShortcutJsonArray().toString()
            prefs.edit().putString(KEY_CUSTOM_SHORTCUT_LIST, json).apply()
            true
        }.getOrDefault(false)

    fun clearCache() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "app_shortcut_cache"
        private const val KEY_SHORTCUT_LIST = "shortcut_list"
        private const val KEY_CUSTOM_SHORTCUT_LIST = "custom_shortcut_list"
        private const val KEY_LAST_UPDATE = "last_update"

        private const val FIELD_PACKAGE_NAME = "packageName"
        private const val FIELD_APP_LABEL = "appLabel"
        private const val FIELD_ID = "id"
        private const val FIELD_SHORT_LABEL = "shortLabel"
        private const val FIELD_LONG_LABEL = "longLabel"
        private const val FIELD_ICON_RES_ID = "iconResId"
        private const val FIELD_ICON_BASE64 = "iconBase64"
        private const val FIELD_ENABLED = "enabled"
        private const val FIELD_INTENTS = "intents"

        private const val FIELD_ACTION = "action"
        private const val FIELD_TARGET_PACKAGE = "targetPackage"
        private const val FIELD_TARGET_CLASS = "targetClass"
        private const val FIELD_DATA = "data"
        private const val FIELD_EXTRAS = "extras"
        private const val FIELD_EXTRA_NAME = "name"
        private const val FIELD_EXTRA_TYPE = "type"
        private const val FIELD_EXTRA_VALUE = "value"

        private const val EXTRA_TYPE_STRING = "string"
        private const val EXTRA_TYPE_BOOLEAN = "boolean"
        private const val EXTRA_TYPE_INT = "int"
        private const val EXTRA_TYPE_LONG = "long"
        private const val EXTRA_TYPE_FLOAT = "float"
        private const val EXTRA_TYPE_DOUBLE = "double"
        private const val EXTRA_TYPE_URI = "uri"

        private fun JSONArray.toShortcutList(): List<StaticShortcut> {
            val shortcuts = mutableListOf<StaticShortcut>()
            for (index in 0 until length()) {
                val jsonObject = getJSONObject(index)
                val id = jsonObject.getString(FIELD_ID).trim()
                val enabled = jsonObject.optBoolean(FIELD_ENABLED, true)
                if (!enabled || !isValidShortcutId(id)) continue
                val packageName = jsonObject.getString(FIELD_PACKAGE_NAME)
                val appLabel = jsonObject.optString(FIELD_APP_LABEL, packageName)
                val shortLabel = jsonObject.getNullableString(FIELD_SHORT_LABEL)
                val longLabel = jsonObject.getNullableString(FIELD_LONG_LABEL)
                val iconResId =
                    if (jsonObject.has(FIELD_ICON_RES_ID)) {
                        jsonObject.getInt(FIELD_ICON_RES_ID)
                    } else {
                        null
                    }
                val iconBase64 =
                    if (jsonObject.has(FIELD_ICON_BASE64) && !jsonObject.isNull(FIELD_ICON_BASE64)) {
                        jsonObject.getString(FIELD_ICON_BASE64).takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                val intentsJson = jsonObject.optJSONArray(FIELD_INTENTS) ?: JSONArray()
                val intents = intentsJson.toIntentList(packageName)

                shortcuts.add(
                    StaticShortcut(
                        packageName = packageName,
                        appLabel = appLabel,
                        id = id,
                        shortLabel = shortLabel,
                        longLabel = longLabel,
                        iconResId = iconResId,
                        iconBase64 = iconBase64,
                        enabled = enabled,
                        intents = intents,
                    ),
                )
            }
            return shortcuts
        }

        private fun List<StaticShortcut>.toShortcutJsonArray(): JSONArray =
            JSONArray().apply {
                forEach { shortcut ->
                    put(
                        JSONObject().apply {
                            put(FIELD_PACKAGE_NAME, shortcut.packageName)
                            put(FIELD_APP_LABEL, shortcut.appLabel)
                            put(FIELD_ID, shortcut.id)
                            shortcut.shortLabel?.let { put(FIELD_SHORT_LABEL, it) }
                            shortcut.longLabel?.let { put(FIELD_LONG_LABEL, it) }
                            shortcut.iconResId?.let { put(FIELD_ICON_RES_ID, it) }
                            shortcut.iconBase64?.let { put(FIELD_ICON_BASE64, it) }
                            put(FIELD_ENABLED, shortcut.enabled)
                            put(FIELD_INTENTS, shortcut.intents.toIntentJsonArray())
                        },
                    )
                }
            }

        private fun List<Intent>.toIntentJsonArray(): JSONArray =
            JSONArray().apply {
                forEach { intent ->
                    val component = intent.component
                    val targetPackage = component?.packageName ?: intent.`package`
                    val targetClass = component?.className
                    val data = intent.dataString
                    val action = intent.action
                    val extras = intent.extras?.toExtrasJsonArray()
                    put(
                        JSONObject().apply {
                            if (!action.isNullOrBlank()) put(FIELD_ACTION, action)
                            if (!targetPackage.isNullOrBlank()) {
                                put(FIELD_TARGET_PACKAGE, targetPackage)
                            }
                            if (!targetClass.isNullOrBlank()) {
                                put(FIELD_TARGET_CLASS, targetClass)
                            }
                            if (!data.isNullOrBlank()) put(FIELD_DATA, data)
                            if (extras != null && extras.length() > 0) {
                                put(FIELD_EXTRAS, extras)
                            }
                        },
                    )
                }
            }

        private fun JSONArray.toIntentList(defaultPackage: String): List<Intent> {
            val intents = mutableListOf<Intent>()
            for (index in 0 until length()) {
                val jsonObject = getJSONObject(index)
                val action = jsonObject.getNullableString(FIELD_ACTION)
                val targetPackage =
                    jsonObject.getNullableString(FIELD_TARGET_PACKAGE) ?: defaultPackage
                val targetClass = jsonObject.getNullableString(FIELD_TARGET_CLASS)
                val data = jsonObject.getNullableString(FIELD_DATA)
                val extrasJson = jsonObject.optJSONArray(FIELD_EXTRAS)

                intents.add(
                    Intent().apply {
                        if (!action.isNullOrBlank()) setAction(action)
                        if (!data.isNullOrBlank()) setData(android.net.Uri.parse(data))
                        if (!targetClass.isNullOrBlank()) {
                            component = android.content.ComponentName(targetPackage, targetClass)
                        } else {
                            `package` = targetPackage
                        }
                        extrasJson?.applyExtras(this)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
            return intents
        }

        private fun JSONObject.getNullableString(name: String): String? {
            if (!has(name) || isNull(name)) return null
            return getString(name).takeIf { it.isNotBlank() }
        }

        private fun Bundle.toExtrasJsonArray(): JSONArray =
            JSONArray().apply {
                for (key in keySet()) {
                    @Suppress("DEPRECATION")
                    val value = get(key) ?: continue
                    val type =
                        when (value) {
                            is Boolean -> EXTRA_TYPE_BOOLEAN
                            is Int -> EXTRA_TYPE_INT
                            is Long -> EXTRA_TYPE_LONG
                            is Float -> EXTRA_TYPE_FLOAT
                            is Double -> EXTRA_TYPE_DOUBLE
                            is android.net.Uri -> EXTRA_TYPE_URI
                            is CharSequence -> EXTRA_TYPE_STRING
                            is String -> EXTRA_TYPE_STRING
                            else -> null
                        }
                            ?: continue
                    val jsonValue =
                        when (value) {
                            is android.net.Uri -> value.toString()
                            else -> value
                        }
                    put(
                        JSONObject().apply {
                            put(FIELD_EXTRA_NAME, key)
                            put(FIELD_EXTRA_TYPE, type)
                            put(FIELD_EXTRA_VALUE, jsonValue)
                        },
                    )
                }
            }

        private fun JSONArray.applyExtras(intent: Intent) {
            for (index in 0 until length()) {
                val jsonObject = optJSONObject(index) ?: continue
                val name =
                    jsonObject.optString(FIELD_EXTRA_NAME).takeIf { it.isNotBlank() }
                        ?: continue
                val type = jsonObject.optString(FIELD_EXTRA_TYPE)
                if (!jsonObject.has(FIELD_EXTRA_VALUE)) continue
                when (type) {
                    EXTRA_TYPE_BOOLEAN -> {
                        intent.putExtra(name, jsonObject.optBoolean(FIELD_EXTRA_VALUE))
                    }

                    EXTRA_TYPE_INT -> {
                        intent.putExtra(name, jsonObject.optInt(FIELD_EXTRA_VALUE))
                    }

                    EXTRA_TYPE_LONG -> {
                        intent.putExtra(name, jsonObject.optLong(FIELD_EXTRA_VALUE))
                    }

                    EXTRA_TYPE_FLOAT -> {
                        intent.putExtra(name, jsonObject.optDouble(FIELD_EXTRA_VALUE).toFloat())
                    }

                    EXTRA_TYPE_DOUBLE -> {
                        intent.putExtra(name, jsonObject.optDouble(FIELD_EXTRA_VALUE))
                    }

                    EXTRA_TYPE_URI -> {
                        val raw = jsonObject.optString(FIELD_EXTRA_VALUE)
                        if (raw.isNotBlank()) {
                            intent.putExtra(name, android.net.Uri.parse(raw))
                        }
                    }

                    EXTRA_TYPE_STRING -> {
                        intent.putExtra(name, jsonObject.optString(FIELD_EXTRA_VALUE))
                    }
                }
            }
        }
    }
}