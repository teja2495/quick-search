package com.tk.quicksearch.tools.aiSearch

import android.content.Context

/** Generic model option for UI selection and request routing across LLM providers. */
data class LlmTextModel(
    val id: String,
    val displayName: String,
    val supportsSystemInstructions: Boolean = true,
    val supportsGrounding: Boolean = true,
)

/** Shared request contract so callers don't depend on provider-specific payload formats. */
data class LlmRequest(
    val query: String,
    val personalContext: String? = null,
    val modelId: String,
    val useGroundingWithGoogleSearch: Boolean,
    val thinkingEnabled: Boolean = false,
    val useSystemInstruction: Boolean,
    val systemInstruction: String? = null,
    val responseMimeType: String = "text/plain",
)

/** Current AI search LLM providers. */
enum class AiSearchLlmProviderId(
    val storageValue: String,
) {
    GEMINI("gemini"),
    OPENAI("openai"),
    ANTHROPIC("anthropic"),
    GROQ("groq"),
    ;

    companion object {
        fun fromStorageValue(value: String?): AiSearchLlmProviderId =
            entries.firstOrNull { it.storageValue == value } ?: GEMINI

        /**
         * Detect the provider from an API key prefix.
         * - `sk-ant-*` → ANTHROPIC
         * - `gsk_*` → GROQ
         * - `sk-*` (not `sk-ant-`) → OPENAI
         * - everything else → GEMINI
         */
        fun detectFromApiKey(apiKey: String?): AiSearchLlmProviderId {
            if (apiKey.isNullOrBlank()) return GEMINI
            val trimmed = apiKey.trim()
            return when {
                trimmed.startsWith("sk-ant-") -> ANTHROPIC
                trimmed.startsWith("gsk_") -> GROQ
                trimmed.startsWith("sk-") -> OPENAI
                else -> GEMINI
            }
        }
    }
}

/** Provider abstraction for AI search style LLM requests. */
interface AiSearchLlmProvider {
    val id: AiSearchLlmProviderId
    val displayName: String
    val defaultModelId: String
    val defaultGroundingEnabled: Boolean
    val fallbackTextModels: List<LlmTextModel>

    suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>>

    suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<String>
}

/** Central lookup to keep provider wiring in one place. */
object AiSearchLlmProviderRegistry {
    fun get(
        id: AiSearchLlmProviderId,
        context: Context,
    ): AiSearchLlmProvider =
        when (id) {
            AiSearchLlmProviderId.GEMINI -> GeminiAiSearchLlmProvider
            AiSearchLlmProviderId.OPENAI -> OpenAiAiSearchLlmProvider
            AiSearchLlmProviderId.ANTHROPIC -> AnthropicAiSearchLlmProvider
            AiSearchLlmProviderId.GROQ -> GroqAiSearchLlmProvider
        }
}
