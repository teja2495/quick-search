package com.tk.quicksearch.tools.aiSearch

import android.content.Context

object OpenCodeAiSearchLlmProvider : AiSearchLlmProvider {
    override val id: AiSearchLlmProviderId = AiSearchLlmProviderId.OPENCODE
    override val defaultModelId: String = OpenCodeModelCatalog.FALLBACK_TEXT_MODELS.first().id
    override val fallbackTextModels: List<LlmTextModel> = OpenCodeModelCatalog.FALLBACK_TEXT_MODELS

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> = OpenCodeClient.fetchAvailableTextModels(apiKey, context)

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<LlmResponse> {
        val client = OpenCodeClient(apiKey = apiKey, context = context)
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelRef = request.modelId,
            sessionId = request.sessionId,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
            history = request.history,
            advancedPayloadJson = request.advancedPayloadJson,
        ).map(::LlmResponse)
    }
}
