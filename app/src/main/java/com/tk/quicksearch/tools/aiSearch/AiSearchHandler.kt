package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchState
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.data.UserAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiSearchHandler(
    private val context: Context,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val showToastCallback: (Int) -> Unit,
) {
    private val _aiSearchState = MutableStateFlow(AiSearchState())
    val aiSearchState: StateFlow<AiSearchState> = _aiSearchState.asStateFlow()

    private var activeProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI
    private var activeProvider: AiSearchLlmProvider =
        AiSearchLlmProviderRegistry.get(AiSearchLlmProviderId.GEMINI, context)
    private var llmApiKey: String? = null
    private var personalContext: String = ""
    private var selectedModelId: String = GeminiModelCatalog.DEFAULT_MODEL_ID
    private var groundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED
    private var thinkingEnabled: Boolean = false
    private var availableGeminiModels: List<GeminiTextModel> = GeminiModelCatalog.FALLBACK_TEXT_MODELS
    private var hasLoadedGeminiModelsFromApi: Boolean = false

    private var isInitialized = false
    private var aiSearchJob: Job? = null

    private fun ensureInitialized() {
        if (!isInitialized) {
            activeProviderId = userPreferences.getAiSearchProviderId()
            activeProvider = AiSearchLlmProviderRegistry.get(activeProviderId, context)
            llmApiKey = userPreferences.getLlmApiKey(activeProviderId)
            personalContext = userPreferences.getLlmPersonalContext(activeProviderId).orEmpty()
            selectedModelId = userPreferences.getLlmModel(activeProviderId)
            groundingEnabled = userPreferences.isLlmGroundingEnabled(activeProviderId)
            thinkingEnabled = userPreferences.isLlmThinkingEnabled(activeProviderId)
            availableGeminiModels = ensureModelExists(activeProvider.fallbackTextModels)
            hasLoadedGeminiModelsFromApi = false
            isInitialized = true
        }
    }

    fun getAiSearchProviderId(): AiSearchLlmProviderId {
        ensureInitialized()
        return activeProviderId
    }

    fun setAiSearchProviderId(providerId: AiSearchLlmProviderId) {
        ensureInitialized()
        if (providerId == activeProviderId) return

        activeProviderId = providerId
        activeProvider = AiSearchLlmProviderRegistry.get(providerId, context)
        userPreferences.setAiSearchProviderId(providerId)

        llmApiKey = userPreferences.getLlmApiKey(providerId)
        personalContext = userPreferences.getLlmPersonalContext(providerId).orEmpty()
        selectedModelId = userPreferences.getLlmModel(providerId)
        groundingEnabled = userPreferences.isLlmGroundingEnabled(providerId)
        thinkingEnabled = userPreferences.isLlmThinkingEnabled(providerId)
        availableGeminiModels = ensureModelExists(activeProvider.fallbackTextModels)
        hasLoadedGeminiModelsFromApi = false
        clearAiSearchState()
    }

    fun getLlmApiKey(): String? {
        ensureInitialized()
        return llmApiKey
    }

    fun setLlmApiKey(apiKey: String?) {
        ensureInitialized()
        val normalized = apiKey?.trim().takeUnless { it.isNullOrBlank() }
        if (normalized == llmApiKey) return

        llmApiKey = normalized
        hasLoadedGeminiModelsFromApi = false
        userPreferences.setLlmApiKey(activeProviderId, normalized)

        if (llmApiKey == null) {
            availableGeminiModels = ensureModelExists(activeProvider.fallbackTextModels)
            clearAiSearchState()
        }
    }

    fun getSelectedModelId(): String {
        ensureInitialized()
        return selectedModelId
    }

    fun setSelectedModelId(modelId: String?) {
        ensureInitialized()
        val normalized = modelId?.trim().takeUnless { it.isNullOrBlank() } ?: activeProvider.defaultModelId
        if (normalized == selectedModelId) return

        selectedModelId = normalized
        userPreferences.setLlmModel(activeProviderId, normalized)
        availableGeminiModels = ensureModelExists(availableGeminiModels)
    }

    fun isGroundingEnabled(): Boolean {
        ensureInitialized()
        return groundingEnabled
    }

    fun setGroundingEnabled(enabled: Boolean) {
        ensureInitialized()
        if (enabled == groundingEnabled) return

        groundingEnabled = enabled
        userPreferences.setLlmGroundingEnabled(activeProviderId, enabled)
    }

    fun isThinkingEnabled(): Boolean {
        ensureInitialized()
        if (activeProviderId == AiSearchLlmProviderId.OPENAI) return false
        return thinkingEnabled
    }

    fun setThinkingEnabled(enabled: Boolean) {
        ensureInitialized()
        if (activeProviderId == AiSearchLlmProviderId.OPENAI) return
        if (enabled == thinkingEnabled) return

        thinkingEnabled = enabled
        userPreferences.setLlmThinkingEnabled(activeProviderId, enabled)
    }

    // Backward-compatible Gemini facade methods for existing call sites.
    fun getGeminiApiKey(): String? = getLlmApiKey()

    fun getPersonalContext(): String {
        ensureInitialized()
        return personalContext
    }

    fun getGeminiModel(): String = getSelectedModelId()

    fun isGeminiGroundingEnabled(): Boolean = isGroundingEnabled()

    fun isGeminiThinkingEnabled(): Boolean = isThinkingEnabled()

    fun getAvailableGeminiModels(): List<GeminiTextModel> {
        ensureInitialized()
        return availableGeminiModels
    }

    fun reloadFromPreferences() {
        isInitialized = false
        ensureInitialized()
        clearAiSearchState()
    }

    fun setGeminiApiKey(apiKey: String?) {
        setLlmApiKey(apiKey)
    }

    fun setPersonalContext(context: String?) {
        ensureInitialized()
        val normalized = context?.trim().orEmpty()
        if (normalized == personalContext) return

        personalContext = normalized
        userPreferences.setLlmPersonalContext(
            providerId = activeProviderId,
            context = normalized.takeUnless { it.isBlank() },
        )
    }

    fun setGeminiModel(modelId: String?) {
        setSelectedModelId(modelId)
    }

    fun setGeminiGroundingEnabled(enabled: Boolean) {
        setGroundingEnabled(enabled)
    }

    fun setGeminiThinkingEnabled(enabled: Boolean) {
        setThinkingEnabled(enabled)
    }

    suspend fun refreshAvailableGeminiModels(forceRefresh: Boolean = false): List<GeminiTextModel> {
        ensureInitialized()

        val apiKey = llmApiKey ?: return availableGeminiModels
        if (!forceRefresh && hasLoadedGeminiModelsFromApi) {
            return availableGeminiModels
        }

        val fetched =
            activeProvider
                .fetchAvailableTextModels(apiKey, context)
                .getOrDefault(activeProvider.fallbackTextModels)
        availableGeminiModels = ensureModelExists(fetched)
        hasLoadedGeminiModelsFromApi = true
        return availableGeminiModels
    }

    fun requestAiSearch(query: String) {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            showToastCallback(R.string.direct_search_enter_query)
            clearAiSearchState()
            return
        }

        val apiKey = llmApiKey
        if (apiKey.isNullOrBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery,
                )
            }
            return
        }

        aiSearchJob?.cancel()
        aiSearchJob =
            scope.launch {
                _aiSearchState.update {
                    AiSearchState(
                        status = AiSearchStatus.Loading,
                        activeQuery = trimmedQuery,
                    )
                }

                val selectedModel = availableGeminiModels.find { it.id == selectedModelId }
                val result =
                    activeProvider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                            LlmRequest(
                                query = trimmedQuery,
                                personalContext =
                                    if (selectedModel?.supportsSystemInstructions == false) {
                                        null
                                    } else {
                                        personalContext.takeIf { it.isNotBlank() }
                                    },
                                modelId = selectedModelId,
                                useGroundingWithGoogleSearch =
                                    activeProviderId != AiSearchLlmProviderId.OPENAI &&
                                        groundingEnabled &&
                                        (selectedModel?.supportsGrounding != false),
                                thinkingEnabled =
                                    thinkingEnabled &&
                                        activeProviderId != AiSearchLlmProviderId.OPENAI,
                                useSystemInstruction =
                                    selectedModel?.supportsSystemInstructions != false,
                            ),
                    )

                result
                    .onSuccess { answer ->
                        _aiSearchState.update {
                            AiSearchState(
                                status = AiSearchStatus.Success,
                                answer = answer,
                                activeQuery = trimmedQuery,
                                usedModelId = selectedModelId,
                            )
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        val message =
                            when {
                                error.message?.startsWith("Request failed") == true -> {
                                    val code =
                                        error.message
                                            ?.substringAfter("Request failed (")
                                            ?.substringBefore(")")
                                            ?.toIntOrNull()
                                    if (code != null) {
                                        context.getString(R.string.error_gemini_request_failed, code)
                                    } else {
                                        error.message
                                    }
                                }

                                error.message == "Unable to load Gemini models" ->
                                    context.getString(R.string.error_gemini_load_models_failed)
                                error.message == "Empty response from Gemini" ->
                                    context.getString(R.string.error_gemini_empty_response)
                                else -> error.message ?: context.getString(R.string.direct_search_error_generic)
                            }
                        _aiSearchState.update {
                            AiSearchState(
                                status = AiSearchStatus.Error,
                                errorMessage = message,
                                activeQuery = trimmedQuery,
                            )
                        }
                    }
            }
    }

    fun requestCustomToolSearch(
        query: String,
        systemInstruction: String,
        modelId: String,
        groundingEnabled: Boolean,
        thinkingEnabled: Boolean = false,
    ) {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        val apiKey = llmApiKey
        if (apiKey.isNullOrBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery,
                )
            }
            return
        }

        aiSearchJob?.cancel()
        aiSearchJob =
            scope.launch {
                _aiSearchState.update {
                    AiSearchState(
                        status = AiSearchStatus.Loading,
                        activeQuery = trimmedQuery,
                    )
                }

                val useSystemInstruction = modelSupportsSystemInstructions(modelId)
                val result =
                    activeProvider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                            LlmRequest(
                                query = trimmedQuery,
                                personalContext = null,
                                modelId = modelId,
                                useGroundingWithGoogleSearch =
                                    activeProviderId != AiSearchLlmProviderId.OPENAI &&
                                        groundingEnabled &&
                                        modelSupportsGrounding(modelId),
                                thinkingEnabled =
                                    (this@AiSearchHandler.thinkingEnabled || thinkingEnabled) &&
                                        activeProviderId != AiSearchLlmProviderId.OPENAI,
                                useSystemInstruction = useSystemInstruction,
                                systemInstruction = systemInstruction,
                            ),
                    )

                result
                    .onSuccess { answer ->
                        _aiSearchState.update {
                            AiSearchState(
                                status = AiSearchStatus.Success,
                                answer = answer,
                                activeQuery = trimmedQuery,
                                usedModelId = modelId,
                            )
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) return@onFailure
                        val message =
                            when {
                                error.message?.startsWith("Request failed") == true -> {
                                    val code =
                                        error.message
                                            ?.substringAfter("Request failed (")
                                            ?.substringBefore(")")
                                            ?.toIntOrNull()
                                    if (code != null) {
                                        context.getString(R.string.error_gemini_request_failed, code)
                                    } else {
                                        error.message
                                    }
                                }

                                error.message == "Unable to load Gemini models" ->
                                    context.getString(R.string.error_gemini_load_models_failed)
                                error.message == "Empty response from Gemini" ->
                                    context.getString(R.string.error_gemini_empty_response)
                                else -> error.message ?: context.getString(R.string.direct_search_error_generic)
                            }
                        _aiSearchState.update {
                            AiSearchState(
                                status = AiSearchStatus.Error,
                                errorMessage = message,
                                activeQuery = trimmedQuery,
                            )
                        }
                    }
            }
    }

    fun clearAiSearchState() {
        aiSearchJob?.cancel()
        aiSearchJob = null
        _aiSearchState.update { AiSearchState() }
    }

    private fun ensureModelExists(models: List<GeminiTextModel>): List<GeminiTextModel> {
        val normalized = if (models.isEmpty()) activeProvider.fallbackTextModels else models
        return if (normalized.any { it.id == selectedModelId }) {
            normalized
        } else {
            listOf(
                GeminiTextModel(
                    id = selectedModelId,
                    displayName = selectedModelId,
                ),
            ) + normalized
        }
    }

    /**
     * When the catalog entry is missing (e.g. stale cache), match [AiSearchClient] Gemma
     * heuristics so we do not send `systemInstruction` JSON for models that reject it.
     */
    private fun modelSupportsSystemInstructions(modelId: String): Boolean {
        val model = availableGeminiModels.find { it.id == modelId }
        return model?.supportsSystemInstructions ?: !modelId.lowercase().startsWith("gemma-")
    }

    private fun modelSupportsGrounding(modelId: String): Boolean {
        val model = availableGeminiModels.find { it.id == modelId }
        return model?.supportsGrounding ?: !modelId.lowercase().startsWith("gemma-")
    }
}
