package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.AliasValidator.isValidShortcutCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.searchEngines.getDefaultShortcutCode

open class AliasPreferences(
    context: Context,
) : BasePreferences(context) {
    fun areAliasesEnabled(): Boolean =
        getBooleanPref(BasePreferences.KEY_ALIASES_ENABLED, true)

    fun setAliasesEnabled(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_ALIASES_ENABLED, enabled)
        setBooleanPref(BasePreferences.KEY_SHORTCUTS_ENABLED_LEGACY, enabled)
    }

    fun getAliasCode(engine: SearchEngine): String {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}${engine.name}"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}${engine.name}"
        val defaultCode = engine.getDefaultShortcutCode()
        val aliasValue = prefs.getString(aliasKey, null)
        val storedCode =
            if (engine == SearchEngine.DIRECT_SEARCH) {
                aliasValue ?: defaultCode
            } else {
                AliasPreferenceMigration.resolveAliasValue(
                    aliasValue = aliasValue,
                    legacyShortcutValue = prefs.getString(legacyKey, defaultCode),
                ) ?: defaultCode
            }
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        if (aliasValue.isNullOrEmpty() && isValidShortcutCode(normalizedCode)) {
            prefs.edit().putString(aliasKey, normalizedCode).apply()
        }
        return if (isValidShortcutCode(normalizedCode)) {
            normalizedCode
        } else {
            defaultCode
        }
    }

    fun setAliasCode(
        engine: SearchEngine,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}${engine.name}"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}${engine.name}"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (!isValidShortcutCode(normalizedCode)) {
            return
        }
        prefs.edit().putString(aliasKey, normalizedCode).putString(legacyKey, normalizedCode).apply()
    }

    fun getAliasCode(targetId: String): String? {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val storedCode =
            AliasPreferenceMigration.resolveAliasValue(
                aliasValue = prefs.getString(aliasKey, null),
                legacyShortcutValue = prefs.getString(legacyKey, null),
            ) ?: return null
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        if (prefs.getString(aliasKey, null).isNullOrEmpty() && isValidShortcutCode(normalizedCode)) {
            prefs.edit().putString(aliasKey, normalizedCode).apply()
        }
        return if (isValidShortcutCode(normalizedCode)) {
            normalizedCode
        } else {
            null
        }
    }

    fun getAliasCodeAllowSingleChar(targetId: String): String? {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val storedCode =
            AliasPreferenceMigration.resolveAliasValue(
                aliasValue = prefs.getString(aliasKey, null),
                legacyShortcutValue = prefs.getString(legacyKey, null),
            ) ?: return null
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        if (prefs.getString(aliasKey, null).isNullOrEmpty() && normalizedCode.isNotEmpty()) {
            prefs.edit().putString(aliasKey, normalizedCode).apply()
        }
        return if (normalizedCode.isNotEmpty()) {
            normalizedCode
        } else {
            null
        }
    }

    fun setAliasCode(
        targetId: String,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (!isValidShortcutCode(normalizedCode)) {
            return
        }
        prefs.edit().putString(aliasKey, normalizedCode).putString(legacyKey, normalizedCode).apply()
    }

    fun clearAliasCode(targetId: String) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        prefs.edit().remove(aliasKey).remove(legacyKey).apply()
    }

    fun setAliasCodeAllowSingleChar(
        targetId: String,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (normalizedCode.isEmpty()) {
            prefs.edit().remove(aliasKey).remove(legacyKey).apply()
            return
        }
        prefs.edit().putString(aliasKey, normalizedCode).putString(legacyKey, normalizedCode).apply()
    }

    fun isAliasEnabled(engine: SearchEngine): Boolean {
        val aliasKey = "${BasePreferences.KEY_ALIAS_ENABLED_PREFIX}${engine.name}"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_ENABLED_PREFIX_LEGACY}${engine.name}"
        if (prefs.contains(aliasKey)) {
            return getBooleanPref(aliasKey, true)
        }
        val legacyValue =
            if (engine == SearchEngine.DIRECT_SEARCH) {
                true
            } else {
                getBooleanPref(legacyKey, true)
            }
        setBooleanPref(aliasKey, legacyValue)
        return legacyValue
    }

    fun setAliasEnabled(
        engine: SearchEngine,
        enabled: Boolean,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_ENABLED_PREFIX}${engine.name}"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_ENABLED_PREFIX_LEGACY}${engine.name}"
        setBooleanPref(aliasKey, enabled)
        setBooleanPref(legacyKey, enabled)
    }

    fun isAliasEnabled(
        targetId: String,
        defaultValue: Boolean,
    ): Boolean {
        val aliasKey = "${BasePreferences.KEY_ALIAS_ENABLED_PREFIX}$targetId"
        return if (prefs.contains(aliasKey)) {
            getBooleanPref(aliasKey, defaultValue)
        } else {
            defaultValue
        }
    }

    fun setAliasEnabled(
        targetId: String,
        enabled: Boolean,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_ENABLED_PREFIX}$targetId"
        setBooleanPref(aliasKey, enabled)
    }

    fun getAllAliasCodes(): Map<SearchEngine, String> =
        SearchEngine.values().associateWith { getAliasCode(it) }
}
