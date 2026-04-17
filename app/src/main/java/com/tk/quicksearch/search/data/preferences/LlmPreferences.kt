package com.tk.quicksearch.search.data.preferences

import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId

/** Provider selection preferences for AI search LLM routing. */
class LlmPreferences(
    context: android.content.Context,
) : BasePreferences(context) {
    fun getAiSearchProviderId(): AiSearchLlmProviderId {
        val raw = prefs.getString(BasePreferences.KEY_AI_SEARCH_LLM_PROVIDER, null)
        return AiSearchLlmProviderId.fromStorageValue(raw)
    }

    fun setAiSearchProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit().putString(BasePreferences.KEY_AI_SEARCH_LLM_PROVIDER, providerId.storageValue).apply()
    }
}
