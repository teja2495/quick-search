package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import com.tk.quicksearch.search.data.UserAppPreferences

data class CustomLlmProviderConfig(
    val id: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val advancedPayload: String? = null,
    val advancedPayloadEnabled: Boolean = false,
)

class CustomOpenAiCompatibleLlmProvider(
    override val id: AiSearchLlmProviderId,
    context: Context,
) : AiSearchLlmProvider {
    private val userPreferences = UserAppPreferences(context.applicationContext)
    private val config: CustomLlmProviderConfig? = userPreferences.getCustomLlmProvider(id)

    override val displayName: String = "Custom"
    override val defaultModelId: String = config?.modelId ?: OpenAiModelCatalog.DEFAULT_MODEL_ID
    override val defaultGroundingEnabled: Boolean = false
    override val fallbackTextModels: List<LlmTextModel> =
        listOf(
            LlmTextModel(
                id = defaultModelId,
                displayName = defaultModelId,
                supportsSystemInstructions = true,
                supportsGrounding = false,
            ),
        )

    override suspend fun fetchAvailableTextModels(
        apiKey: String,
        context: Context,
    ): Result<List<LlmTextModel>> {
        val providerConfig =
            config
                ?: return Result.failure(IllegalStateException("Custom provider is no longer available"))
        return OpenAiClient
            .fetchAvailableTextModels(
                apiKey = apiKey,
                context = context,
                baseUrl = providerConfig.baseUrl,
                filterForOpenAiPicker = false,
            )
            .map { models ->
                val allModels =
                    models.map {
                        it.copy(
                            supportsSystemInstructions = true,
                            supportsGrounding = false,
                        )
                    } + fallbackTextModels
                allModels.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
            }
    }

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<String> {
        val providerConfig =
            config
                ?: return Result.failure(IllegalStateException("Custom provider is no longer available"))
        val client =
            OpenAiClient(
                apiKey = apiKey,
                context = context,
                baseUrl = providerConfig.baseUrl,
            )
        return client.fetchAnswer(
            query = request.query,
            personalContext = request.personalContext,
            modelId = request.modelId,
            useGroundingWithGoogleSearch = false,
            useSystemInstruction = request.useSystemInstruction,
            systemInstruction = request.systemInstruction,
            advancedPayloadJson =
                request.advancedPayloadJson
                    ?: providerConfig
                        .advancedPayload
                        ?.takeIf { providerConfig.advancedPayloadEnabled },
        )
    }
}
