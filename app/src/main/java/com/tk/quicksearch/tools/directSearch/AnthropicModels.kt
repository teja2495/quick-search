package com.tk.quicksearch.tools.directSearch

/** Shared Anthropic (Claude) model configuration defaults. */
object AnthropicModelCatalog {
    const val DEFAULT_MODEL_ID = "claude-haiku-4-5-20251001"
    const val DEFAULT_GROUNDING_ENABLED = true

    /**
     * Fallback list used when the model catalog cannot be fetched from the API.
     * Web search is supported via web_search_20250305 server-side tool.
     */
    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            LlmTextModel(id = "claude-opus-4-6", displayName = "Claude Opus 4.6", supportsGrounding = true),
            LlmTextModel(id = "claude-sonnet-4-6", displayName = "Claude Sonnet 4.6", supportsGrounding = true),
            LlmTextModel(id = "claude-haiku-4-5-20251001", displayName = "Claude Haiku 4.5", supportsGrounding = true),
            LlmTextModel(id = "claude-opus-4-5", displayName = "Claude Opus 4.5", supportsGrounding = true),
            LlmTextModel(id = "claude-sonnet-4-5", displayName = "Claude Sonnet 4.5", supportsGrounding = true),
            LlmTextModel(id = "claude-3-7-sonnet-20250219", displayName = "Claude 3.7 Sonnet", supportsGrounding = true),
            LlmTextModel(id = "claude-3-5-haiku-20241022", displayName = "Claude 3.5 Haiku", supportsGrounding = true),
        )

    /** All Claude models support system instructions and web search. */
    fun isLikelyTextModel(modelId: String): Boolean =
        modelId.lowercase().startsWith("claude-")
}
