package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
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
        if (aliasValue != null) {
            val normalizedAlias = normalizeShortcutCodeInput(aliasValue)
            return if (normalizedAlias.isEmpty()) {
                ""
            } else if (isValidShortcutCode(normalizedAlias)) {
                if (aliasValue != normalizedAlias) {
                    prefs.edit().putString(aliasKey, normalizedAlias).putString(legacyKey, normalizedAlias).apply()
                }
                normalizedAlias
            } else {
                prefs.edit().remove(aliasKey).remove(legacyKey).apply()
                ""
            }
        }

        val migratedCode =
            if (engine == SearchEngine.DIRECT_SEARCH) {
                null
            } else {
                AliasPreferenceMigration.resolveAliasValue(
                    aliasValue = null,
                    legacyShortcutValue = prefs.getString(legacyKey, null),
                )
            }
        if (migratedCode.isNullOrEmpty()) return defaultCode

        val normalizedMigrated = normalizeShortcutCodeInput(migratedCode)
        return if (isValidShortcutCode(normalizedMigrated)) {
            prefs.edit().putString(aliasKey, normalizedMigrated).putString(legacyKey, normalizedMigrated).apply()
            normalizedMigrated
        } else {
            prefs.edit().remove(aliasKey).remove(legacyKey).apply()
            ""
        }
    }

    fun setAliasCode(
        engine: SearchEngine,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}${engine.name}"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}${engine.name}"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (normalizedCode.isEmpty()) {
            prefs.edit().putString(aliasKey, "").putString(legacyKey, "").apply()
            return
        }
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
        if (prefs.getString(aliasKey, null).isNullOrEmpty() && isValidGeneralAliasCode(normalizedCode)) {
            prefs.edit().putString(aliasKey, normalizedCode).apply()
        }
        return if (isValidGeneralAliasCode(normalizedCode)) {
            normalizedCode
        } else {
            prefs.edit().remove(aliasKey).remove(legacyKey).apply()
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
        if (!isValidGeneralAliasCode(normalizedCode)) {
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
        return getAliasCode(engine).isNotEmpty()
    }

    fun setAliasEnabled(
        engine: SearchEngine,
        enabled: Boolean,
    ) = Unit

    fun isAliasEnabled(
        targetId: String,
        defaultValue: Boolean,
    ): Boolean = getAliasCodeAllowSingleChar(targetId)?.isNotEmpty() == true

    fun setAliasEnabled(
        targetId: String,
        enabled: Boolean,
    ) = Unit

    fun getAllAliasCodes(): Map<SearchEngine, String> =
        SearchEngine.values().associateWith { getAliasCode(it) }
}
