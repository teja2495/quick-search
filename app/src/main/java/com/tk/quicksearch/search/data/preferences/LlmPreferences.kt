package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.util.Log
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LegacyPersonalContext

/** Provider selection and built-in provider settings, keyed by stable provider ID. */
class LlmPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getAiSearchProviderId(): AiSearchLlmProviderId {
        val raw = prefs.getString(BasePreferences.KEY_AI_SEARCH_LLM_PROVIDER, null)
        return AiSearchLlmProviderId.fromStorageValue(raw)
    }

    fun setAiSearchProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit().putString(BasePreferences.KEY_AI_SEARCH_LLM_PROVIDER, providerId.storageValue).apply()
    }

    fun getApiKey(id: AiSearchLlmProviderId): String? {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return null
        return encryptedPrefs?.getString(definition.apiKeyPreference, null)?.takeIf { it.isNotBlank() }
    }

    fun setApiKey(id: AiSearchLlmProviderId, value: String?) {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return
        val securePrefs = encryptedPrefs ?: run {
            Log.e("LlmPreferences", "Encrypted storage unavailable; API key for ${id.storageValue} not saved")
            return
        }
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        securePrefs.edit().putString(definition.apiKeyPreference, normalized).apply()
    }

    fun getPersonalContext(id: AiSearchLlmProviderId): String? {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return null
        val key = definition.personalContextPreference
        return encryptedPrefs?.getString(key, null)?.takeIf { it.isNotBlank() }
            ?: when (definition.legacyPersonalContext) {
                LegacyPersonalContext.NONE -> null
                LegacyPersonalContext.KEEP_PLAINTEXT_FALLBACK ->
                    prefs.getString(key, null)?.takeIf { it.isNotBlank() }
                LegacyPersonalContext.CLEAR_PLAINTEXT_ON_ENCRYPTED_WRITE -> prefs.getString(key, null)
            }
    }

    fun setPersonalContext(id: AiSearchLlmProviderId, value: String?) {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return
        val key = definition.personalContextPreference
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        val securePrefs = encryptedPrefs
        when {
            normalized == null -> {
                securePrefs?.edit()?.remove(key)?.apply()
                prefs.edit().remove(key).apply()
            }
            securePrefs != null -> {
                securePrefs.edit().putString(key, normalized).apply()
                if (definition.legacyPersonalContext == LegacyPersonalContext.CLEAR_PLAINTEXT_ON_ENCRYPTED_WRITE) {
                    prefs.edit().remove(key).apply()
                }
            }
            definition.legacyPersonalContext != LegacyPersonalContext.NONE ->
                prefs.edit().putString(key, normalized).apply()
            else -> Log.e("LlmPreferences", "Encrypted storage unavailable; personal context for ${id.storageValue} not saved")
        }
    }

    fun getModel(id: AiSearchLlmProviderId): String {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return ""
        return prefs.getString(definition.modelPreference, null)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(definition.normalizeSavedModel)
            .orEmpty()
    }

    fun setModel(id: AiSearchLlmProviderId, value: String?) {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().putString(definition.modelPreference, normalized).apply()
    }

    fun isGroundingEnabled(id: AiSearchLlmProviderId): Boolean {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return false
        return prefs.getBoolean(definition.groundingPreference, definition.defaultGroundingEnabled)
    }

    fun setGroundingEnabled(id: AiSearchLlmProviderId, value: Boolean) {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return
        prefs.edit().putBoolean(definition.groundingPreference, value).apply()
    }

    fun isThinkingEnabled(id: AiSearchLlmProviderId): Boolean {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return false
        return definition.supportsThinkingRequest && prefs.getBoolean(definition.thinkingPreference, false)
    }

    fun setThinkingEnabled(id: AiSearchLlmProviderId, value: Boolean) {
        val definition = AiSearchLlmProviderRegistry.definition(id) ?: return
        if (definition.supportsThinkingRequest) {
            prefs.edit().putBoolean(definition.thinkingPreference, value).apply()
        }
    }
}
