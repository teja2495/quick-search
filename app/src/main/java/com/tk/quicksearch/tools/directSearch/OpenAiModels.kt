package com.tk.quicksearch.tools.directSearch

/** Shared OpenAI model configuration defaults. */
object OpenAiModelCatalog {
    const val DEFAULT_MODEL_ID = "gpt-4o-mini"
    const val DEFAULT_GROUNDING_ENABLED = true

    /**
     * Fallback list used when the model catalog cannot be fetched from the API.
     *
     * Web search in Chat Completions requires dedicated search-preview models.
     * gpt-4o and gpt-4o-mini have search variants (gpt-4o-search-preview /
     * gpt-4o-mini-search-preview) used automatically when grounding is enabled.
     * gpt-4.1 series and reasoning (o-series) models have no Chat Completions
     * web search support.
     */
    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            LlmTextModel(id = "gpt-4o", displayName = "GPT-4o", supportsGrounding = true),
            LlmTextModel(id = "gpt-4o-mini", displayName = "GPT-4o Mini", supportsGrounding = true),
            LlmTextModel(id = "gpt-4.1", displayName = "GPT-4.1", supportsGrounding = false),
            LlmTextModel(id = "gpt-4.1-mini", displayName = "GPT-4.1 Mini", supportsGrounding = false),
            LlmTextModel(id = "gpt-4.1-nano", displayName = "GPT-4.1 Nano", supportsGrounding = false),
            LlmTextModel(id = "o3", displayName = "o3", supportsGrounding = false),
            LlmTextModel(id = "o4-mini", displayName = "o4-mini", supportsGrounding = false),
        )

    /**
     * Maps a model ID to its search-preview variant used when grounding is enabled.
     * Returns null if the model has no search-preview variant.
     */
    fun searchPreviewModelFor(modelId: String): String? {
        val lower = modelId.lowercase()
        return when {
            lower == "gpt-4o" || lower.startsWith("gpt-4o-202") -> "gpt-4o-search-preview"
            lower == "gpt-4o-mini" -> "gpt-4o-mini-search-preview"
            else -> null
        }
    }

    /** True when the given model supports web search in Chat Completions (via search-preview). */
    fun supportsWebSearch(modelId: String): Boolean = searchPreviewModelFor(modelId) != null

    /** True when the given model is a reasoning model (o-series). */
    fun isReasoningModel(modelId: String): Boolean {
        val lower = modelId.lowercase()
        return lower.startsWith("o1") || lower.startsWith("o2") ||
            lower.startsWith("o3") || lower.startsWith("o4")
    }

    /** Heuristic filter: keep only GPT and o-series text models. */
    fun isLikelyTextModel(modelId: String): Boolean {
        val lower = modelId.lowercase()
        if (lower.contains("whisper") || lower.contains("tts") ||
            lower.contains("dall-e") || lower.contains("embed") ||
            lower.contains("moderat") || lower.contains("babbage") ||
            lower.contains("davinci") || lower.contains("ada") ||
            lower.contains("curie")
        ) return false
        return lower.startsWith("gpt-") || lower.startsWith("o1") ||
            lower.startsWith("o2") || lower.startsWith("o3") ||
            lower.startsWith("o4") || lower.startsWith("chatgpt-")
    }
}
