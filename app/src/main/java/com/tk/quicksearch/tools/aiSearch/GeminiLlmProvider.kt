package com.tk.quicksearch.tools.aiSearch

import android.content.Context

object GeminiAiSearchLlmProvider : AiSearchLlmProvider {
    override val id: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI
    override val displayName: String = "Gemini"
    override val defaultModelId: String = GeminiModelCatalog.DEFAULT_MODEL_ID
    override val defaultGroundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED
    override val fallbackTextModels: List<LlmTextModel> = GeminiModelCatalog.FALLBACK_TEXT_MODELS

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> = AiSearchClient.fetchAvailableTextModels(apiKey, context)

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<String> {
        val client = AiSearchClient(apiKey = apiKey, context = context)
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId,
            useGroundingWithGoogleSearch = request.useGroundingWithGoogleSearch,
            thinkingEnabled = request.thinkingEnabled,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
            responseMimeType = request.responseMimeType,
        )
    }
}
