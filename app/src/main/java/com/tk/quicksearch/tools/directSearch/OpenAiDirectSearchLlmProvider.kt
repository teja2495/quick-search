package com.tk.quicksearch.tools.directSearch

import android.content.Context

object OpenAiDirectSearchLlmProvider : DirectSearchLlmProvider {
    override val id: DirectSearchLlmProviderId = DirectSearchLlmProviderId.OPENAI
    override val displayName: String = "OpenAI"
    override val defaultModelId: String = OpenAiModelCatalog.DEFAULT_MODEL_ID
    override val defaultGroundingEnabled: Boolean = OpenAiModelCatalog.DEFAULT_GROUNDING_ENABLED
    override val fallbackTextModels: List<LlmTextModel> = OpenAiModelCatalog.FALLBACK_TEXT_MODELS

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> = OpenAiClient.fetchAvailableTextModels(apiKey, context)

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<String> {
        val client = OpenAiClient(apiKey = apiKey, context = context)
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId,
            useGroundingWithGoogleSearch = request.useGroundingWithGoogleSearch,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
        )
    }
}
