package com.tk.quicksearch.tools.aiSearch

import android.content.Context

object GroqAiSearchLlmProvider : AiSearchLlmProvider {
    override val id: AiSearchLlmProviderId = AiSearchLlmProviderId.GROQ
    override val displayName: String = "Groq"
    override val defaultModelId: String = GroqModelCatalog.DEFAULT_MODEL_ID
    override val defaultGroundingEnabled: Boolean = GroqModelCatalog.DEFAULT_GROUNDING_ENABLED
    override val fallbackTextModels: List<LlmTextModel> = GroqModelCatalog.FALLBACK_TEXT_MODELS

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> = GroqClient.fetchAvailableTextModels(apiKey, context)

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<String> {
        val client = GroqClient(apiKey = apiKey, context = context)
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId,
            thinkingEnabled = request.thinkingEnabled,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
        )
    }
}
