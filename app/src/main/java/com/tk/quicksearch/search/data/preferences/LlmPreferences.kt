package com.tk.quicksearch.search.data.preferences

import com.tk.quicksearch.tools.directSearch.DirectSearchLlmProviderId

/** Provider selection preferences for direct-search LLM routing. */
class LlmPreferences(
    context: android.content.Context,
) : BasePreferences(context) {
    fun getDirectSearchProviderId(): DirectSearchLlmProviderId {
        val raw = prefs.getString(BasePreferences.KEY_DIRECT_SEARCH_LLM_PROVIDER, null)
        return DirectSearchLlmProviderId.fromStorageValue(raw)
    }

    fun setDirectSearchProviderId(providerId: DirectSearchLlmProviderId) {
        prefs.edit().putString(BasePreferences.KEY_DIRECT_SEARCH_LLM_PROVIDER, providerId.storageValue).apply()
    }
}
