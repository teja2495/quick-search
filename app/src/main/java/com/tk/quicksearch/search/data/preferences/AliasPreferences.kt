package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.searchEngines.getDefaultShortcutCode

open class AliasPreferences(
    context: Context,
) : BasePreferences(context) {
    private val customizationStore = SearchCustomizationStore(context)

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
        val aliasValue = customizationStore.getString(aliasKey)
        if (aliasValue != null) {
            val normalizedAlias = normalizeShortcutCodeInput(aliasValue)
            return if (normalizedAlias.isEmpty()) {
                ""
            } else if (isValidGeneralAliasCode(normalizedAlias)) {
                if (aliasValue != normalizedAlias) {
                    customizationStore.putString(aliasKey, normalizedAlias)
                    customizationStore.putString(legacyKey, normalizedAlias)
                }
                normalizedAlias
            } else {
                customizationStore.putString(aliasKey, null)
                customizationStore.putString(legacyKey, null)
                ""
            }
        }

        val migratedCode =
            if (engine == SearchEngine.DIRECT_SEARCH) {
                null
            } else {
                AliasPreferenceMigration.resolveAliasValue(
                    aliasValue = null,
                    legacyShortcutValue = customizationStore.getString(legacyKey),
                )
            }
        if (migratedCode.isNullOrEmpty()) return defaultCode

        val normalizedMigrated = normalizeShortcutCodeInput(migratedCode)
        return if (isValidGeneralAliasCode(normalizedMigrated)) {
            customizationStore.putString(aliasKey, normalizedMigrated)
            customizationStore.putString(legacyKey, normalizedMigrated)
            normalizedMigrated
        } else {
            customizationStore.putString(aliasKey, null)
            customizationStore.putString(legacyKey, null)
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
            customizationStore.putString(aliasKey, "")
            customizationStore.putString(legacyKey, "")
            return
        }
        if (!isValidGeneralAliasCode(normalizedCode)) {
            return
        }
        customizationStore.putString(aliasKey, normalizedCode)
        customizationStore.putString(legacyKey, normalizedCode)
    }

    fun getAliasCode(targetId: String): String? {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val storedCode =
            AliasPreferenceMigration.resolveAliasValue(
                aliasValue = customizationStore.getString(aliasKey),
                legacyShortcutValue = customizationStore.getString(legacyKey),
            ) ?: return null
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        if (customizationStore.getString(aliasKey).isNullOrEmpty() && isValidGeneralAliasCode(normalizedCode)) {
            customizationStore.putString(aliasKey, normalizedCode)
        }
        return if (isValidGeneralAliasCode(normalizedCode)) {
            normalizedCode
        } else {
            customizationStore.putString(aliasKey, null)
            customizationStore.putString(legacyKey, null)
            null
        }
    }

    fun getAliasCodeAllowSingleChar(targetId: String): String? {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val storedCode =
            AliasPreferenceMigration.resolveAliasValue(
                aliasValue = customizationStore.getString(aliasKey),
                legacyShortcutValue = customizationStore.getString(legacyKey),
            ) ?: return null
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        if (customizationStore.getString(aliasKey).isNullOrEmpty() && normalizedCode.isNotEmpty()) {
            customizationStore.putString(aliasKey, normalizedCode)
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
        customizationStore.putString(aliasKey, normalizedCode)
        customizationStore.putString(legacyKey, normalizedCode)
    }

    fun clearAliasCode(targetId: String) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        customizationStore.putString(aliasKey, null)
        customizationStore.putString(legacyKey, null)
    }

    fun setAliasCodeAllowSingleChar(
        targetId: String,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val legacyKey = "${BasePreferences.KEY_SHORTCUT_CODE_PREFIX_LEGACY}$targetId"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (normalizedCode.isEmpty()) {
            customizationStore.putString(aliasKey, null)
            customizationStore.putString(legacyKey, null)
            return
        }
        customizationStore.putString(aliasKey, normalizedCode)
        customizationStore.putString(legacyKey, normalizedCode)
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

    fun getAllAliasWordsById(): Map<String, String> =
        customizationStore.snapshot().mapNotNull { (key, value) ->
            if (!key.startsWith(BasePreferences.KEY_ALIAS_CODE_PREFIX)) return@mapNotNull null
            val normalized = normalizeShortcutCodeInput(value as? String ?: return@mapNotNull null)
            if (isValidGeneralAliasCode(normalized)) {
                key.removePrefix(BasePreferences.KEY_ALIAS_CODE_PREFIX) to normalized
            } else {
                null
            }
        }.toMap()
}
