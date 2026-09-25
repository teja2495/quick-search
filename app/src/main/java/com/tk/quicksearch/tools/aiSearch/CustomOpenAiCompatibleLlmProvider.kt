package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences

data class CustomLlmProviderConfig(
    val id: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val groundingEnabled: Boolean = true,
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
    override val defaultModelId: String = config?.modelId.orEmpty()
    override val defaultGroundingEnabled: Boolean = config?.groundingEnabled ?: true
    override val fallbackTextModels: List<LlmTextModel> =
        defaultModelId.takeIf { it.isNotBlank() }?.let { modelId ->
            listOf(
                LlmTextModel(
                    id = modelId,
                    displayName = modelId,
                    supportsSystemInstructions = true,
                    supportsGrounding = false,
                ),
            )
        }.orEmpty()

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
                trustUserCertificates = true,
            )
            .map { models ->
                val allModels =
                    models.map {
                        it.copy(
                            supportsSystemInstructions = true,
                            supportsGrounding = false,
                        )
                    }
                allModels.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
            }
    }

    override suspend fun fetchAnswer(
        apiKey: String,
        context: Context,
        request: LlmRequest,
    ): Result<LlmResponse> {
        val providerConfig =
            config
                ?: return Result.failure(IllegalStateException("Custom provider is no longer available"))
        val client =
            OpenAiClient(
                apiKey = apiKey,
                context = context,
                baseUrl = providerConfig.baseUrl,
                trustUserCertificates = true,
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
        ).map(::LlmResponse)
    }
}
