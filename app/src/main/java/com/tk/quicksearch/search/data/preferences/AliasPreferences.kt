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

    fun getAliasCode(engine: SearchEngine): String {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}${engine.name}"
        val defaultCode = engine.getDefaultShortcutCode()
        val aliasValue = customizationStore.getString(aliasKey)
        if (aliasValue != null) {
            val normalizedAlias = normalizeShortcutCodeInput(aliasValue)
            return if (normalizedAlias.isEmpty()) {
                ""
            } else if (isValidGeneralAliasCode(normalizedAlias)) {
                if (aliasValue != normalizedAlias) {
                    customizationStore.putString(aliasKey, normalizedAlias)
                }
                normalizedAlias
            } else {
                customizationStore.putString(aliasKey, null)
                ""
            }
        }

        return defaultCode
    }

    fun setAliasCode(
        engine: SearchEngine,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}${engine.name}"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (normalizedCode.isEmpty()) {
            customizationStore.putString(aliasKey, "")
            return
        }
        if (!isValidGeneralAliasCode(normalizedCode)) {
            return
        }
        customizationStore.putString(aliasKey, normalizedCode)
    }

    fun getAliasCode(targetId: String): String? {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val storedCode = customizationStore.getString(aliasKey) ?: return null
        if (storedCode.isEmpty()) return ""
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        if (customizationStore.getString(aliasKey).isNullOrEmpty() && isValidGeneralAliasCode(normalizedCode)) {
            customizationStore.putString(aliasKey, normalizedCode)
        }
        return if (isValidGeneralAliasCode(normalizedCode)) {
            normalizedCode
        } else {
            customizationStore.putString(aliasKey, null)
            null
        }
    }

    fun getAliasCodeAllowSingleChar(targetId: String): String? {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val storedCode = customizationStore.getString(aliasKey) ?: return null
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
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (normalizedCode.isEmpty()) {
            customizationStore.putString(aliasKey, "")
            return
        }
        if (!isValidGeneralAliasCode(normalizedCode)) {
            return
        }
        customizationStore.putString(aliasKey, normalizedCode)
    }

    fun clearAliasCode(targetId: String) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        customizationStore.putString(aliasKey, null)
    }

    fun setAliasCodeAllowSingleChar(
        targetId: String,
        code: String,
    ) {
        val aliasKey = "${BasePreferences.KEY_ALIAS_CODE_PREFIX}$targetId"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (normalizedCode.isEmpty()) {
            customizationStore.putString(aliasKey, null)
            return
        }
        customizationStore.putString(aliasKey, normalizedCode)
    }

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
