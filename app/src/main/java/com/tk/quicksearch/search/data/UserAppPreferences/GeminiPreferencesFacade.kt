package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.GeminiPreferences

/**
 * Facade for Gemini preference operations
 */
class GeminiPreferencesFacade(
    private val geminiPreferences: GeminiPreferences
) {
    fun getGeminiApiKey(): String? = geminiPreferences.getGeminiApiKey()

    fun setGeminiApiKey(key: String?) = geminiPreferences.setGeminiApiKey(key)

    fun getPersonalContext(): String? = geminiPreferences.getPersonalContext()

    fun setPersonalContext(context: String?) = geminiPreferences.setPersonalContext(context)

    fun getGeminiModel(): String = geminiPreferences.getGeminiModel()

    fun setGeminiModel(modelId: String?) = geminiPreferences.setGeminiModel(modelId)

    fun isGeminiGroundingEnabled(): Boolean = geminiPreferences.isGeminiGroundingEnabled()

    fun setGeminiGroundingEnabled(enabled: Boolean) =
            geminiPreferences.setGeminiGroundingEnabled(enabled)
}