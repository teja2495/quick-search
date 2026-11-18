package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tk.quicksearch.R

internal data class AiProviderBranding(
    @DrawableRes val icon: Int,
    @DrawableRes val iconOnDark: Int = icon,
    val tintIcon: Boolean = false,
    val fillsBadge: Boolean = false,
    val attribution: AiProviderAttribution = AiProviderAttribution.Label,
)

internal sealed interface AiProviderAttribution {
    data object Label : AiProviderAttribution

    data object Gemini : AiProviderAttribution

    data object Claude : AiProviderAttribution

    data class Wordmark(
        @DrawableRes val drawable: Int,
        val aspectRatio: Float,
        val heightDp: Int = 18,
    ) : AiProviderAttribution
}

internal enum class LegacyPersonalContext {
    NONE,
    KEEP_PLAINTEXT_FALLBACK,
    CLEAR_PLAINTEXT_ON_ENCRYPTED_WRITE,
}

internal data class AiSearchLlmProviderDefinition(
    val id: AiSearchLlmProviderId,
    val searchName: String,
    @StringRes val nameRes: Int,
    val priority: Int,
    val branding: AiProviderBranding,
    val create: (AiSearchLlmProviderId) -> AiSearchLlmProvider,
    val apiKeySignature: Regex? = null,
    val supportsNativeSearch: Boolean = false,
    val showGroundingInResultPicker: Boolean = supportsNativeSearch,
    val supportsThinkingRequest: Boolean = false,
    val supportsModelThinking: (String) -> Boolean = { true },
    val supportsGroundingWithoutCatalog: (String) -> Boolean = { supportsNativeSearch },
    val defaultGroundingEnabled: Boolean = true,
    val featuredModels: (List<LlmTextModel>) -> List<LlmTextModel> = { it },
    val preferredModel: (List<LlmTextModel>) -> String? = { null },
    val normalizeSavedModel: (String) -> String = { it },
    val legacyPersonalContext: LegacyPersonalContext = LegacyPersonalContext.NONE,
) {
    // Established names in both the regular and encrypted preference stores.
    val apiKeyPreference: String get() = "${id.storageValue}_api_key"
    val personalContextPreference: String get() = "${id.storageValue}_personal_context"
    val modelPreference: String get() = "${id.storageValue}_model"
    val groundingPreference: String get() = "${id.storageValue}_grounding_enabled"
    val thinkingPreference: String get() = "${id.storageValue}_thinking_enabled"
}

/** Built-in providers in settings and model-picker display order. */
object AiSearchLlmProviderRegistry {
    internal val definitions: List<AiSearchLlmProviderDefinition> =
        listOf(
            AiSearchLlmProviderDefinition(
                id = AiSearchLlmProviderId.GEMINI,
                searchName = "Gemini",
                nameRes = R.string.search_engine_gemini,
                priority = 0,
                branding = AiProviderBranding(
                    icon = R.drawable.ic_gemini_sparkle_search_engine,
                    attribution = AiProviderAttribution.Gemini,
                ),
                create = { GeminiAiSearchLlmProvider },
                supportsNativeSearch = true,
                supportsThinkingRequest = true,
                supportsGroundingWithoutCatalog = { !it.startsWith("gemma-", ignoreCase = true) },
                featuredModels = { GeminiModelCatalog.pickerModels(it) },
                preferredModel = { LlmDefaultModels.gemini(it) },
                legacyPersonalContext = LegacyPersonalContext.CLEAR_PLAINTEXT_ON_ENCRYPTED_WRITE,
            ),
            AiSearchLlmProviderDefinition(
                id = AiSearchLlmProviderId.OPENAI,
                searchName = "OpenAI",
                nameRes = R.string.settings_ai_provider_openai,
                priority = 5,
                branding = AiProviderBranding(
                    icon = R.drawable.chatgpt,
                    tintIcon = true,
                    attribution = AiProviderAttribution.Wordmark(R.drawable.openai_wordmark, 1564.3f / 428.4f),
                ),
                create = { OpenAiAiSearchLlmProvider },
                apiKeySignature = Regex("sk-.*", RegexOption.DOT_MATCHES_ALL),
                supportsNativeSearch = true,
                showGroundingInResultPicker = false,
                supportsGroundingWithoutCatalog = { OpenAiModelCatalog.supportsWebSearch(it) },
                featuredModels = { OpenAiModelCatalog.pickerModels(it) },
                preferredModel = { LlmDefaultModels.openAi(it) },
                legacyPersonalContext = LegacyPersonalContext.KEEP_PLAINTEXT_FALLBACK,
            ),
            AiSearchLlmProviderDefinition(
                id = AiSearchLlmProviderId.ANTHROPIC,
                searchName = "Claude",
                nameRes = R.string.search_engine_claude,
                priority = 6,
                branding = AiProviderBranding(
                    icon = R.drawable.claude,
                    attribution = AiProviderAttribution.Claude,
                ),
                create = { AnthropicAiSearchLlmProvider },
                apiKeySignature = Regex("sk-ant-.*", RegexOption.DOT_MATCHES_ALL),
                supportsNativeSearch = true,
                supportsThinkingRequest = true,
                featuredModels = { AnthropicModelCatalog.pickerModels(it) },
                preferredModel = { LlmDefaultModels.anthropic(it) },
                legacyPersonalContext = LegacyPersonalContext.KEEP_PLAINTEXT_FALLBACK,
            ),
            AiSearchLlmProviderDefinition(
                id = AiSearchLlmProviderId.GROQ,
                searchName = "Groq",
                nameRes = R.string.settings_ai_provider_groq,
                priority = 2,
                branding = AiProviderBranding(
                    icon = R.drawable.groq_logo,
                    fillsBadge = true,
                    attribution = AiProviderAttribution.Wordmark(R.drawable.groq_wordmark, 152f / 55.5f),
                ),
                create = { GroqAiSearchLlmProvider },
                apiKeySignature = Regex("gsk_.*", RegexOption.DOT_MATCHES_ALL),
                supportsThinkingRequest = true,
                supportsModelThinking = {
                    it.contains("gpt-oss", ignoreCase = true) || it.contains("qwen", ignoreCase = true)
                },
                normalizeSavedModel = { GroqModelCatalog.replaceRetiredModel(it) },
                legacyPersonalContext = LegacyPersonalContext.KEEP_PLAINTEXT_FALLBACK,
            ),
            AiSearchLlmProviderDefinition(
                id = AiSearchLlmProviderId.META,
                searchName = "Meta AI",
                nameRes = R.string.settings_ai_provider_meta,
                priority = 3,
                branding = AiProviderBranding(icon = R.drawable.meta_logo),
                create = { MetaAiSearchLlmProvider },
                apiKeySignature = Regex("LLM\\|.*", RegexOption.DOT_MATCHES_ALL),
                supportsNativeSearch = true,
                supportsThinkingRequest = true,
                featuredModels = { MetaModelCatalog.pickerModels(it) },
                preferredModel = { LlmDefaultModels.meta(it) },
                legacyPersonalContext = LegacyPersonalContext.KEEP_PLAINTEXT_FALLBACK,
            ),
            AiSearchLlmProviderDefinition(
                id = AiSearchLlmProviderId.OPENCODE,
                searchName = "OpenCode",
                nameRes = R.string.settings_ai_provider_opencode,
                priority = 4,
                branding = AiProviderBranding(
                    icon = R.drawable.opencode_logo_light,
                    iconOnDark = R.drawable.opencode_logo_dark,
                    attribution = AiProviderAttribution.Wordmark(R.drawable.opencode_wordmark, 640.714f / 115f, 14),
                ),
                create = { OpenCodeAiSearchLlmProvider },
                apiKeySignature = Regex("sk-[A-Za-z0-9]{64}"),
                legacyPersonalContext = LegacyPersonalContext.KEEP_PLAINTEXT_FALLBACK,
            ),
        )

    private val byId = definitions.associateBy { it.id }
    internal val ids = definitions.map { it.id }
    internal const val CUSTOM_PRIORITY = 1
    // OpenAI's generic sk- prefix also matches Anthropic and OpenCode keys.
    private val apiKeyCandidates = definitions.filter { it.apiKeySignature != null }
        .sortedBy { it.id == AiSearchLlmProviderId.OPENAI }

    init {
        check(byId.size == definitions.size) { "Duplicate built-in AI provider ID" }
        check(definitions.all { it.id.storageValue.matches(Regex("[a-z][a-z0-9_]*")) }) {
            "Built-in AI provider IDs must be stable preference keys"
        }
    }

    internal fun definition(id: AiSearchLlmProviderId): AiSearchLlmProviderDefinition? = byId[id]

    internal fun detect(apiKey: String): AiSearchLlmProviderId? =
        apiKeyCandidates.firstOrNull { it.apiKeySignature?.matches(apiKey) == true }?.id

    internal fun pickerOrder(id: AiSearchLlmProviderId): Int =
        ids.indexOf(id).takeIf { it >= 0 } ?: ids.size

    fun get(id: AiSearchLlmProviderId, context: Context): AiSearchLlmProvider =
        if (id.isCustom) {
            CustomOpenAiCompatibleLlmProvider(id, context)
        } else {
            builtIn(id)
        }

    internal fun builtIn(id: AiSearchLlmProviderId): AiSearchLlmProvider =
        byId.getValue(id).create(id).also { provider ->
            check(provider.id == id) {
                "AI provider adapter ID ${provider.id.storageValue} does not match ${id.storageValue}"
            }
        }
}
