package com.tk.quicksearch.tools.aiSearch

/** Shared Groq model configuration defaults. */
object GroqModelCatalog {
    const val DEFAULT_MODEL_ID = "openai/gpt-oss-120b"
    // Kept on even though Groq has no native search so adding Tavily immediately enables the
    // already-selected Web Search preference.
    const val DEFAULT_GROUNDING_ENABLED = true

    /**
     * Models Groq has shut down for free and developer tiers, mapped to their recommended
     * replacement. Saved selections pointing at these would otherwise fail with "model does not exist".
     */
    private val RETIRED_MODEL_REPLACEMENTS: Map<String, String> =
        mapOf(
            "llama-3.3-70b-versatile" to "openai/gpt-oss-120b",
            "llama-3.1-8b-instant" to "openai/gpt-oss-20b",
            "llama3-70b-8192" to "openai/gpt-oss-120b",
            "llama3-8b-8192" to "openai/gpt-oss-20b",
            "mixtral-8x7b-32768" to "openai/gpt-oss-120b",
        )

    /**
     * Fallback list used when the model catalog cannot be fetched from the API.
     * Groq does not support grounding/web search natively.
     */
    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            LlmTextModel(id = "openai/gpt-oss-120b", displayName = "GPT-OSS 120B", supportsGrounding = false),
            LlmTextModel(id = "openai/gpt-oss-20b", displayName = "GPT-OSS 20B", supportsGrounding = false),
            LlmTextModel(id = "qwen/qwen3.6-27b", displayName = "Qwen 3.6 27B", supportsGrounding = false),
        )

    fun replaceRetiredModel(modelId: String): String =
        RETIRED_MODEL_REPLACEMENTS[modelId.trim()] ?: modelId

    /** Heuristic filter: keep only chat/text generation models hosted on Groq. */
    fun isLikelyTextModel(modelId: String): Boolean {
        val lower = modelId.lowercase()
        if (lower.contains("whisper") || lower.contains("tts") || lower.contains("embed") ||
            lower.contains("guard") || lower.contains("orpheus")
        ) {
            return false
        }
        // Newer Groq ids are namespaced, e.g. "openai/gpt-oss-120b" or "qwen/qwen3.6-27b".
        val name = lower.substringAfterLast('/')
        return name.startsWith("llama") || name.startsWith("mixtral") ||
            name.startsWith("gemma") || name.startsWith("qwen") ||
            name.startsWith("deepseek") || name.startsWith("mistral") ||
            name.startsWith("gpt-oss") || name.startsWith("kimi")
    }
}
