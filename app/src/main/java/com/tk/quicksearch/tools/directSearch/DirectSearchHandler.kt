package com.tk.quicksearch.tools.directSearch

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectSearchState
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.data.UserAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DirectSearchHandler(
    private val context: Context,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val showToastCallback: (Int) -> Unit,
) {
    private val _directSearchState = MutableStateFlow(DirectSearchState())
    val directSearchState: StateFlow<DirectSearchState> = _directSearchState.asStateFlow()

    private var activeProviderId: DirectSearchLlmProviderId = DirectSearchLlmProviderId.GEMINI
    private var activeProvider: DirectSearchLlmProvider =
        DirectSearchLlmProviderRegistry.get(DirectSearchLlmProviderId.GEMINI, context)
    private var llmApiKey: String? = null
    private var personalContext: String = ""
    private var selectedModelId: String = GeminiModelCatalog.DEFAULT_MODEL_ID
    private var groundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED
    private var availableGeminiModels: List<GeminiTextModel> = GeminiModelCatalog.FALLBACK_TEXT_MODELS
    private var hasLoadedGeminiModelsFromApi: Boolean = false

    private var isInitialized = false
    private var directSearchJob: Job? = null

    private fun ensureInitialized() {
        if (!isInitialized) {
            activeProviderId = userPreferences.getDirectSearchProviderId()
            activeProvider = DirectSearchLlmProviderRegistry.get(activeProviderId, context)
            llmApiKey = userPreferences.getLlmApiKey(activeProviderId)
            personalContext = userPreferences.getLlmPersonalContext(activeProviderId).orEmpty()
            selectedModelId = userPreferences.getLlmModel(activeProviderId)
            groundingEnabled = userPreferences.isLlmGroundingEnabled(activeProviderId)
            availableGeminiModels = ensureModelExists(activeProvider.fallbackTextModels)
            hasLoadedGeminiModelsFromApi = false
            isInitialized = true
        }
    }

    fun getDirectSearchProviderId(): DirectSearchLlmProviderId {
        ensureInitialized()
        return activeProviderId
    }

    fun setDirectSearchProviderId(providerId: DirectSearchLlmProviderId) {
        ensureInitialized()
        if (providerId == activeProviderId) return

        activeProviderId = providerId
        activeProvider = DirectSearchLlmProviderRegistry.get(providerId, context)
        userPreferences.setDirectSearchProviderId(providerId)

        llmApiKey = userPreferences.getLlmApiKey(providerId)
        personalContext = userPreferences.getLlmPersonalContext(providerId).orEmpty()
        selectedModelId = userPreferences.getLlmModel(providerId)
        groundingEnabled = userPreferences.isLlmGroundingEnabled(providerId)
        availableGeminiModels = ensureModelExists(activeProvider.fallbackTextModels)
        hasLoadedGeminiModelsFromApi = false
        clearDirectSearchState()
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
            clearDirectSearchState()
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

    // Backward-compatible Gemini facade methods for existing call sites.
    fun getGeminiApiKey(): String? = getLlmApiKey()

    fun getPersonalContext(): String {
        ensureInitialized()
        return personalContext
    }

    fun getGeminiModel(): String = getSelectedModelId()

    fun isGeminiGroundingEnabled(): Boolean = isGroundingEnabled()

    fun getAvailableGeminiModels(): List<GeminiTextModel> {
        ensureInitialized()
        return availableGeminiModels
    }

    fun reloadFromPreferences() {
        isInitialized = false
        ensureInitialized()
        clearDirectSearchState()
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

    fun requestDirectSearch(query: String) {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            showToastCallback(R.string.direct_search_enter_query)
            clearDirectSearchState()
            return
        }

        val apiKey = llmApiKey
        if (apiKey.isNullOrBlank()) {
            _directSearchState.update {
                DirectSearchState(
                    status = DirectSearchStatus.Error,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery,
                )
            }
            return
        }

        directSearchJob?.cancel()
        directSearchJob =
            scope.launch {
                _directSearchState.update {
                    DirectSearchState(
                        status = DirectSearchStatus.Loading,
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
                                    groundingEnabled && (selectedModel?.supportsGrounding != false),
                                useSystemInstruction =
                                    selectedModel?.supportsSystemInstructions != false,
                            ),
                    )

                result
                    .onSuccess { answer ->
                        _directSearchState.update {
                            DirectSearchState(
                                status = DirectSearchStatus.Success,
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
                        _directSearchState.update {
                            DirectSearchState(
                                status = DirectSearchStatus.Error,
                                errorMessage = message,
                                activeQuery = trimmedQuery,
                            )
                        }
                    }
            }
    }

    fun requestCustomToolSearch(query: String, systemInstruction: String, modelId: String, groundingEnabled: Boolean) {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        val apiKey = llmApiKey
        if (apiKey.isNullOrBlank()) {
            _directSearchState.update {
                DirectSearchState(
                    status = DirectSearchStatus.Error,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery,
                )
            }
            return
        }

        directSearchJob?.cancel()
        directSearchJob =
            scope.launch {
                _directSearchState.update {
                    DirectSearchState(
                        status = DirectSearchStatus.Loading,
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
                                    groundingEnabled && modelSupportsGrounding(modelId),
                                useSystemInstruction = useSystemInstruction,
                                systemInstruction = systemInstruction,
                            ),
                    )

                result
                    .onSuccess { answer ->
                        _directSearchState.update {
                            DirectSearchState(
                                status = DirectSearchStatus.Success,
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
                        _directSearchState.update {
                            DirectSearchState(
                                status = DirectSearchStatus.Error,
                                errorMessage = message,
                                activeQuery = trimmedQuery,
                            )
                        }
                    }
            }
    }

    fun clearDirectSearchState() {
        directSearchJob?.cancel()
        directSearchJob = null
        _directSearchState.update { DirectSearchState() }
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
     * When the catalog entry is missing (e.g. stale cache), match [DirectSearchClient] Gemma
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
