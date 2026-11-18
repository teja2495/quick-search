package com.tk.quicksearch.tools.aiSearch

import android.content.Context

/** Built-in adapter for endpoints that support OpenAI's model list and chat completions formats. */
internal class OpenAiCompatibleLlmProvider(
    override val id: AiSearchLlmProviderId,
    private val baseUrl: String,
    override val defaultModelId: String,
    override val fallbackTextModels: List<LlmTextModel> =
        listOf(LlmTextModel(defaultModelId, defaultModelId, supportsGrounding = false)),
) : AiSearchLlmProvider {
    init {
        require(baseUrl.startsWith("https://")) { "Built-in AI provider endpoint must use HTTPS" }
        require(defaultModelId.isNotBlank()) { "Built-in AI provider requires a default model" }
    }

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> =
        OpenAiClient.fetchAvailableTextModels(
            apiKey = apiKey,
            context = context,
            baseUrl = baseUrl,
            filterForOpenAiPicker = false,
        )

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<LlmResponse> =
        OpenAiClient(apiKey, context, baseUrl).fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId.ifBlank { defaultModelId },
            useGroundingWithGoogleSearch = false,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
            history = request.history,
            advancedPayloadJson = request.advancedPayloadJson,
        ).map(::LlmResponse)
}
