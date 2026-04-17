package com.tk.quicksearch.tools.aiSearch

import android.content.Context

object AnthropicAiSearchLlmProvider : AiSearchLlmProvider {
    override val id: AiSearchLlmProviderId = AiSearchLlmProviderId.ANTHROPIC
    override val displayName: String = "Claude"
    override val defaultModelId: String = AnthropicModelCatalog.DEFAULT_MODEL_ID
    override val defaultGroundingEnabled: Boolean = AnthropicModelCatalog.DEFAULT_GROUNDING_ENABLED
    override val fallbackTextModels: List<LlmTextModel> = AnthropicModelCatalog.FALLBACK_TEXT_MODELS

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> = AnthropicClient.fetchAvailableTextModels(apiKey, context)

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<String> {
        val client = AnthropicClient(apiKey = apiKey, context = context)
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId,
            useGroundingWithGoogleSearch = request.useGroundingWithGoogleSearch,
            thinkingEnabled = request.thinkingEnabled,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
        )
    }
}
