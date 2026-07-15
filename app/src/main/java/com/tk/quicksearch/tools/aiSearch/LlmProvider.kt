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
    val advancedPayloadJson: String? = null,
)

/** Current AI search LLM providers. */
data class AiSearchLlmProviderId(
    val storageValue: String,
) {
    val isCustom: Boolean
        get() = storageValue.startsWith(CUSTOM_PREFIX)

    val customId: String?
        get() = storageValue.removePrefix(CUSTOM_PREFIX).takeIf { isCustom && it.isNotBlank() }

    companion object {
        private const val CUSTOM_PREFIX = "custom:"

        val GEMINI = AiSearchLlmProviderId("gemini")
        val OPENAI = AiSearchLlmProviderId("openai")
        val ANTHROPIC = AiSearchLlmProviderId("anthropic")
        val GROQ = AiSearchLlmProviderId("groq")
        val entries = listOf(GEMINI, OPENAI, ANTHROPIC, GROQ)

        fun custom(id: String): AiSearchLlmProviderId =
            AiSearchLlmProviderId("$CUSTOM_PREFIX${id.trim()}")

        fun fromStorageValue(value: String?): AiSearchLlmProviderId =
            when {
                value.isNullOrBlank() -> GEMINI
                value.startsWith(CUSTOM_PREFIX) -> AiSearchLlmProviderId(value)
                else -> entries.firstOrNull { it.storageValue == value } ?: GEMINI
            }

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
    ): AiSearchLlmProvider {
        if (id.isCustom) {
            return CustomOpenAiCompatibleLlmProvider(id, context)
        }
        return when (id) {
            AiSearchLlmProviderId.GEMINI -> GeminiAiSearchLlmProvider
            AiSearchLlmProviderId.OPENAI -> OpenAiAiSearchLlmProvider
            AiSearchLlmProviderId.ANTHROPIC -> AnthropicAiSearchLlmProvider
            AiSearchLlmProviderId.GROQ -> GroqAiSearchLlmProvider
            else -> GeminiAiSearchLlmProvider
        }
    }
}
