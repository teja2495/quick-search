package com.tk.quicksearch.tools.aiSearch

/**
 * OpenCode serves two catalogs from one API key: Zen (pay as you go) and Go (subscription).
 * Model refs use OpenCode's own `<provider>/<model>` ids, e.g. `opencode-go/kimi-k3`, so a saved
 * selection keeps the catalog it came from.
 */
enum class OpenCodeTier(
    val refPrefix: String,
    val baseUrl: String,
    val label: String,
) {
    ZEN("opencode/", "https://opencode.ai/zen/v1", "Zen"),
    GO("opencode-go/", "https://opencode.ai/zen/go/v1", "Go"),
}

/** Wire format a model is served on; OpenCode rejects requests sent to another format's endpoint. */
enum class OpenCodeApiFormat(
    val path: String,
) {
    CHAT_COMPLETIONS("/chat/completions"),
    MESSAGES("/messages"),
    RESPONSES("/responses"),
}

/** Shared OpenCode Zen / Go model configuration defaults. */
object OpenCodeModelCatalog {
    // Kept on even though OpenCode has no native search so adding Tavily immediately enables the
    // already-selected Web Search preference.
    const val DEFAULT_GROUNDING_ENABLED = true

    /**
     * Non-Claude models served on the Anthropic Messages format. They differ per catalog, e.g.
     * qwen3.8-max is chat/completions on Zen but messages on Go (opencode.ai/docs/zen, /docs/go).
     */
    private val MESSAGES_MODELS: Map<OpenCodeTier, Set<String>> =
        mapOf(
            OpenCodeTier.ZEN to
                setOf(
                    "minimax-m2.1-free",
                    "minimax-m2.5-free",
                    "minimax-m3-free",
                    "qwen3.5-plus",
                    "qwen3.6-plus",
                    "qwen3.6-plus-free",
                    "qwen3.8-flash",
                ),
            OpenCodeTier.GO to
                setOf("minimax-m2.7", "minimax-m3", "qwen3.7-plus", "qwen3.8-flash", "qwen3.8-max"),
        )

    val FALLBACK_TEXT_MODELS: List<LlmTextModel> =
        listOf(
            textModel(OpenCodeTier.GO, "kimi-k3"),
            textModel(OpenCodeTier.GO, "glm-5.3"),
            textModel(OpenCodeTier.ZEN, "big-pickle"),
        )

    fun modelRef(
        tier: OpenCodeTier,
        modelId: String,
    ): String = tier.refPrefix + modelId.trim()

    /** Splits a stored ref into its catalog and bare model id; unprefixed ids are treated as Zen. */
    fun parseModelRef(modelRef: String): Pair<OpenCodeTier, String> {
        val trimmed = modelRef.trim()
        val tier =
            OpenCodeTier.entries.firstOrNull { trimmed.startsWith(it.refPrefix) }
                ?: return OpenCodeTier.ZEN to trimmed
        return tier to trimmed.removePrefix(tier.refPrefix)
    }

    fun textModel(
        tier: OpenCodeTier,
        modelId: String,
    ): LlmTextModel =
        LlmTextModel(
            id = modelRef(tier, modelId),
            displayName = "${modelId.trim()} (${tier.label})",
            supportsSystemInstructions = true,
            supportsGrounding = false,
        )

    /**
     * Endpoint format per model, following the endpoint tables in opencode.ai/docs/zen and
     * opencode.ai/docs/go. Null for formats this client does not speak (Gemini's Google format and
     * Jev's systemone format), which are left out of the picker.
     */
    fun apiFormatFor(
        tier: OpenCodeTier,
        modelId: String,
    ): OpenCodeApiFormat? {
        val id = modelId.trim().lowercase()
        return when {
            id.startsWith("gemini-") || id.startsWith("jev-") -> null
            id.startsWith("claude-") || id in MESSAGES_MODELS.getValue(tier) -> OpenCodeApiFormat.MESSAGES
            id == "grok-code" -> OpenCodeApiFormat.CHAT_COMPLETIONS
            id.startsWith("gpt-") || id.startsWith("grok-") || id.startsWith("muse-spark") ->
                OpenCodeApiFormat.RESPONSES
            else -> OpenCodeApiFormat.CHAT_COMPLETIONS
        }
    }
}
