package com.tk.quicksearch.tools.aiSearch

import android.content.Context

/** Generic model option for UI selection and request routing across LLM providers. */
data class LlmTextModel(
    val id: String,
    val displayName: String,
    val supportsSystemInstructions: Boolean = true,
    val supportsGrounding: Boolean = true,
)

/** Keeps a selection only when it is present in a successfully loaded live catalog. */
fun resolveModelSelection(
    selectedModelId: String,
    availableModels: List<LlmTextModel>,
): String =
    selectedModelId.trim().takeIf { selected ->
        selected.isNotEmpty() && availableModels.any { it.id == selected }
    }.orEmpty()

/** Keeps a still-available selection; otherwise picks the provider's default from the live catalog. */
fun resolveModelSelectionOrDefault(
    providerId: AiSearchLlmProviderId,
    selectedModelId: String,
    availableModels: List<LlmTextModel>,
): String =
    resolveModelSelection(selectedModelId, availableModels)
        .ifEmpty { LlmDefaultModels.pick(providerId, availableModels) }

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
    /** Earlier turns of the conversation, sent as real messages before [query]. */
    val history: List<AiConversationTurn> = emptyList(),
    /** Stable for every request of one conversation, for providers that route or cache by session. */
    val sessionId: String? = null,
)

/** Provider response plus request-level delivery details needed by the result UI. */
data class LlmResponse(
    val text: String,
    val webSearchDisabledForRequest: Boolean = false,
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
        val META = AiSearchLlmProviderId("meta")
        val OPENCODE = AiSearchLlmProviderId("opencode")
        val entries: List<AiSearchLlmProviderId>
            get() = AiSearchLlmProviderRegistry.ids

        fun defaultPriority(id: AiSearchLlmProviderId): Int =
            if (id.isCustom) AiSearchLlmProviderRegistry.CUSTOM_PRIORITY
            else AiSearchLlmProviderRegistry.definition(id)?.priority ?: Int.MAX_VALUE

        /** Highest-priority provider in [ids]; ties keep the original order. */
        fun preferredOf(ids: Iterable<AiSearchLlmProviderId>): AiSearchLlmProviderId? =
            ids.minByOrNull(::defaultPriority)

        fun custom(id: String): AiSearchLlmProviderId =
            AiSearchLlmProviderId("$CUSTOM_PREFIX${id.trim()}")

        fun fromStorageValue(value: String?): AiSearchLlmProviderId =
            when {
                value.isNullOrBlank() -> GEMINI
                value.startsWith(CUSTOM_PREFIX) -> AiSearchLlmProviderId(value)
                else -> entries.firstOrNull { it.storageValue == value } ?: GEMINI
            }

        fun detectFromApiKey(apiKey: String?): AiSearchLlmProviderId =
            apiKey?.trim()?.takeIf { it.isNotEmpty() }
                ?.let(AiSearchLlmProviderRegistry::detect) ?: GEMINI
    }
}

/** Provider abstraction for AI search style LLM requests. */
interface AiSearchLlmProvider {
    val id: AiSearchLlmProviderId
    val defaultModelId: String
    val fallbackTextModels: List<LlmTextModel>

    suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>>

    suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<LlmResponse>
}
