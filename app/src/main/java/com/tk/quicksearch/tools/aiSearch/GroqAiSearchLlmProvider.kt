package com.tk.quicksearch.tools.aiSearch

import android.content.Context

object GroqAiSearchLlmProvider : AiSearchLlmProvider {
    override val id: AiSearchLlmProviderId = AiSearchLlmProviderId.GROQ
    override val defaultModelId: String = GroqModelCatalog.DEFAULT_MODEL_ID
    override val fallbackTextModels: List<LlmTextModel> = GroqModelCatalog.FALLBACK_TEXT_MODELS

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> = GroqClient.fetchAvailableTextModels(apiKey, context)

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<LlmResponse> {
        val client = GroqClient(apiKey = apiKey, context = context)
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId,
            thinkingEnabled = request.thinkingEnabled,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
            history = request.history,
            responseMimeType = request.responseMimeType,
            advancedPayloadJson = request.advancedPayloadJson,
        ).map(::LlmResponse)
    }
}
