package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.util.Log
import com.tk.quicksearch.tools.aiSearch.AnthropicModelCatalog

/** Preferences for Anthropic (Claude) API-related settings. Uses encrypted storage for the API key. */
class AnthropicPreferences(
    context: Context,
) : BasePreferences(context) {

    fun getApiKey(): String? {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e("AnthropicPreferences", "EncryptedSharedPreferences unavailable; Anthropic API key not loaded")
                return null
            }
        return securePrefs.getString(BasePreferences.KEY_ANTHROPIC_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setApiKey(key: String?) {
        val securePrefs =
            encryptedPrefs ?: run {
                Log.e("AnthropicPreferences", "EncryptedSharedPreferences unavailable; Anthropic API key not persisted")
                return
            }
        if (key.isNullOrBlank()) {
            securePrefs.edit().remove(BasePreferences.KEY_ANTHROPIC_API_KEY).apply()
            return
        }
        securePrefs.edit().putString(BasePreferences.KEY_ANTHROPIC_API_KEY, key.trim()).apply()
    }

    fun getPersonalContext(): String? =
        encryptedPrefs?.getString(BasePreferences.KEY_ANTHROPIC_PERSONAL_CONTEXT, null)?.takeIf { it.isNotBlank() }
            ?: prefs.getString(BasePreferences.KEY_ANTHROPIC_PERSONAL_CONTEXT, null)?.takeIf { it.isNotBlank() }

    fun setPersonalContext(value: String?) {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty()) {
            encryptedPrefs?.edit()?.remove(BasePreferences.KEY_ANTHROPIC_PERSONAL_CONTEXT)?.apply()
            prefs.edit().remove(BasePreferences.KEY_ANTHROPIC_PERSONAL_CONTEXT).apply()
            return
        }
        encryptedPrefs?.edit()?.putString(BasePreferences.KEY_ANTHROPIC_PERSONAL_CONTEXT, trimmed)?.apply()
            ?: prefs.edit().putString(BasePreferences.KEY_ANTHROPIC_PERSONAL_CONTEXT, trimmed).apply()
    }

    fun getModel(): String {
        val model = prefs.getString(BasePreferences.KEY_ANTHROPIC_MODEL, null)?.trim()
        return model.takeUnless { it.isNullOrEmpty() } ?: AnthropicModelCatalog.DEFAULT_MODEL_ID
    }

    fun setModel(modelId: String?) {
        val normalized = modelId?.trim()
        if (normalized.isNullOrEmpty()) {
            prefs.edit().remove(BasePreferences.KEY_ANTHROPIC_MODEL).apply()
            return
        }
        prefs.edit().putString(BasePreferences.KEY_ANTHROPIC_MODEL, normalized).apply()
    }

    fun isGroundingEnabled(): Boolean =
        prefs.getBoolean(BasePreferences.KEY_ANTHROPIC_GROUNDING_ENABLED, AnthropicModelCatalog.DEFAULT_GROUNDING_ENABLED)

    fun setGroundingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BasePreferences.KEY_ANTHROPIC_GROUNDING_ENABLED, enabled).apply()
    }

    fun isThinkingEnabled(): Boolean =
        prefs.getBoolean(BasePreferences.KEY_ANTHROPIC_THINKING_ENABLED, false)

    fun setThinkingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BasePreferences.KEY_ANTHROPIC_THINKING_ENABLED, enabled).apply()
    }
}
