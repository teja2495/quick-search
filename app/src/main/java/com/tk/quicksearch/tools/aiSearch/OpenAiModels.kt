package com.tk.quicksearch.tools.aiSearch

/** Shared OpenAI model configuration defaults. */
object OpenAiModelCatalog {
    private val gptMajorVersionRegex = Regex("^gpt-(\\d+)", RegexOption.IGNORE_CASE)
    private val datedSnapshotSuffixRegex = Regex("-\\d{4}-\\d{2}-\\d{2}$")

    const val DEFAULT_MODEL_ID = "gpt-5-mini"
    const val DEFAULT_GROUNDING_ENABLED = true

    /**
     * Fallback list used when the model catalog cannot be fetched from the API.
     *
     * Web search in Chat Completions requires dedicated search-preview models when available.
     * Models without a search-preview mapping have no Chat Completions web search support.
     */
    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            LlmTextModel(id = "gpt-5", displayName = "GPT-5", supportsGrounding = false),
            LlmTextModel(id = "gpt-5-mini", displayName = "GPT-5 Mini", supportsGrounding = false),
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

    /**
     * Models eligible for the OpenAI model picker: `gpt-{major}` with major version 5 or higher
     * (e.g. gpt-5, gpt-6), excluding any ID containing "codex" or "chat", or ending with
     * `-\d{4}-\d{2}-\d{2}` (dated snapshots).
     */
    fun isModelPickerModel(modelId: String): Boolean {
        val trimmed = modelId.trim()
        if (datedSnapshotSuffixRegex.containsMatchIn(trimmed)) return false
        val lower = trimmed.lowercase()
        if (lower.contains("codex") || lower.contains("chat")) return false
        val major = gptMajorVersionRegex.find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return major >= 5
    }
}
