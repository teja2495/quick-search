package com.tk.quicksearch.tools.directSearch

/** Shared Groq model configuration defaults. */
object GroqModelCatalog {
    const val DEFAULT_MODEL_ID = "llama-3.3-70b-versatile"
    const val DEFAULT_GROUNDING_ENABLED = false

    /**
     * Fallback list used when the model catalog cannot be fetched from the API.
     * Groq does not support grounding/web search natively.
     */
    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            LlmTextModel(id = "llama-3.3-70b-versatile", displayName = "Llama 3.3 70B", supportsGrounding = false),
            LlmTextModel(id = "llama-3.1-8b-instant", displayName = "Llama 3.1 8B Instant", supportsGrounding = false),
            LlmTextModel(id = "llama3-70b-8192", displayName = "Llama 3 70B", supportsGrounding = false),
            LlmTextModel(id = "llama3-8b-8192", displayName = "Llama 3 8B", supportsGrounding = false),
            LlmTextModel(id = "mixtral-8x7b-32768", displayName = "Mixtral 8x7B", supportsGrounding = false),
            LlmTextModel(id = "gemma2-9b-it", displayName = "Gemma 2 9B", supportsGrounding = false),
        )

    /** Heuristic filter: keep only chat/text generation models hosted on Groq. */
    fun isLikelyTextModel(modelId: String): Boolean {
        val lower = modelId.lowercase()
        if (lower.contains("whisper") || lower.contains("tts") || lower.contains("embed")) return false
        return lower.startsWith("llama") || lower.startsWith("mixtral") ||
            lower.startsWith("gemma") || lower.startsWith("qwen") ||
            lower.startsWith("deepseek") || lower.startsWith("mistral")
    }
}
