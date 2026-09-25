package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchState
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AiConversationTurn(
    val question: String,
    val answer: String,
)

internal fun buildAiFollowUpPrompt(
    previousTurns: List<AiConversationTurn>,
    followUpQuestion: String,
): String =
    buildString {
        append("Use the complete conversation below as context for the final follow-up question.\n\n")
        previousTurns.forEach { turn ->
            append("User: ")
            append(turn.question)
            append("\nAssistant: ")
            append(turn.answer)
            append("\n\n")
        }
        append("User follow-up: ")
        append(followUpQuestion)
    }

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
    @Volatile private var llmApiKey: String? = null
    private var personalContext: String = ""
    private var selectedModelId: String = ""
    private var groundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED
    private var thinkingEnabled: Boolean = false
    private var availableGeminiModels: List<GeminiTextModel> = emptyList()
    private var hasLoadedGeminiModelsFromApi: Boolean = false

    @Volatile private var hasAnyLlmApiKey: Boolean = false
    @Volatile private var hasLoadedApiKeyCache: Boolean = false
    @Volatile private var isInitialized = false
    private var aiSearchJob: Job? = null
    private val conversationTurns = mutableListOf<AiConversationTurn>()

    private fun ensureInitialized() {
        if (!isInitialized) {
            activeProviderId = userPreferences.getAiSearchProviderId()
            activeProvider = AiSearchLlmProviderRegistry.get(activeProviderId, context)
            llmApiKey = userPreferences.getLlmApiKey(activeProviderId)
            personalContext = userPreferences.getLlmPersonalContext(activeProviderId).orEmpty()
            selectedModelId = userPreferences.getLlmModel(activeProviderId)
            groundingEnabled = userPreferences.isLlmGroundingEnabled(activeProviderId)
            thinkingEnabled = userPreferences.isLlmThinkingEnabled(activeProviderId)
            availableGeminiModels = emptyList()
            hasLoadedGeminiModelsFromApi = false
            hasAnyLlmApiKey = userPreferences.hasAnyLlmApiKey()
            hasLoadedApiKeyCache = true
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
        availableGeminiModels = emptyList()
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

        hasAnyLlmApiKey = userPreferences.hasAnyLlmApiKey()
        if (llmApiKey == null) {
            availableGeminiModels = emptyList()
            clearAiSearchState()
        }
    }

    fun setLlmApiKey(
        providerId: AiSearchLlmProviderId,
        apiKey: String?,
    ) {
        ensureInitialized()
        val normalized = apiKey?.trim().takeUnless { it.isNullOrBlank() }
        userPreferences.setLlmApiKey(providerId, normalized)

        when {
            normalized != null && (providerId == activeProviderId || llmApiKey.isNullOrBlank()) -> {
                setAiSearchProviderId(providerId)
                llmApiKey = normalized
                hasLoadedGeminiModelsFromApi = false
            }
            providerId == activeProviderId && normalized == null -> {
                val nextProvider =
                    userPreferences.getConfiguredLlmProviderIds().firstOrNull {
                        !userPreferences.getLlmApiKey(it).isNullOrBlank()
                    }
                if (nextProvider != null) {
                    setAiSearchProviderId(nextProvider)
                } else {
                    llmApiKey = null
                    availableGeminiModels = emptyList()
                    hasLoadedGeminiModelsFromApi = false
                    clearAiSearchState()
                }
            }
        }
        hasAnyLlmApiKey = userPreferences.hasAnyLlmApiKey()
    }

    fun getSelectedModelId(): String {
        ensureInitialized()
        return selectedModelId
    }

    fun setSelectedModelId(modelId: String?) {
        ensureInitialized()
        val normalized = modelId?.trim().orEmpty()
        if (normalized == selectedModelId) return

        selectedModelId = normalized
        userPreferences.setLlmModel(activeProviderId, normalized)
        availableGeminiModels = ensureModelExists(availableGeminiModels)
    }

    fun setSelectedModelId(
        providerId: AiSearchLlmProviderId,
        modelId: String?,
    ) {
        ensureInitialized()
        if (providerId != activeProviderId) {
            setAiSearchProviderId(providerId)
        }
        setSelectedModelId(modelId)
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
        if (activeProviderId == AiSearchLlmProviderId.OPENAI || activeProviderId.isCustom) return false
        return thinkingEnabled
    }

    fun setThinkingEnabled(enabled: Boolean) {
        ensureInitialized()
        if (activeProviderId == AiSearchLlmProviderId.OPENAI || activeProviderId.isCustom) return
        if (enabled == thinkingEnabled) return

        thinkingEnabled = enabled
        userPreferences.setLlmThinkingEnabled(activeProviderId, enabled)
    }

    // Backward-compatible Gemini facade methods for existing call sites.
    fun getGeminiApiKey(): String? = getLlmApiKey()

    /**
     * Returns the cached active-provider API key state without triggering [ensureInitialized].
     * Safe to call from the main thread. Treats an unknown cache as available so startup alias
     * parsing does not hide AI targets before background preference initialization completes.
     */
    fun hasLlmApiKeyCached(): Boolean = !hasLoadedApiKeyCache || !llmApiKey.isNullOrBlank()

    /**
     * Returns whether any LLM provider has a cached API key without triggering [ensureInitialized].
     * Safe to call from the main thread. Treats an unknown cache as available so startup alias
     * parsing does not hide AI targets before background preference initialization completes.
     */
    fun hasAnyLlmApiKeyCached(): Boolean = !hasLoadedApiKeyCache || hasAnyLlmApiKey

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
        hasLoadedApiKeyCache = false
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

        val result = activeProvider.fetchAvailableTextModels(apiKey, context)
        val fetched = result.getOrDefault(emptyList())
        if (result.isSuccess) {
            val resolvedModelId = resolveModelSelection(selectedModelId, fetched)
            if (resolvedModelId != selectedModelId) setSelectedModelId(resolvedModelId)
        }
        availableGeminiModels = ensureModelExists(fetched)
        hasLoadedGeminiModelsFromApi = true
        return availableGeminiModels
    }

    /** Reuses a catalog already fetched by settings instead of issuing a second network request. */
    fun updateAvailableModels(models: List<GeminiTextModel>) {
        ensureInitialized()
        availableGeminiModels = ensureModelExists(models)
        hasLoadedGeminiModelsFromApi = true
    }

    fun requestAiSearch(query: String) {
        requestAiSearchInternal(query = query, isFollowUp = false)
    }

    fun requestAiFollowUp(
        query: String,
        previousQuestion: String,
        previousAnswer: String,
    ) {
        if (conversationTurns.isEmpty()) {
            val seedQuestion = previousQuestion.trim()
            val seedAnswer = previousAnswer.trim()
            if (seedQuestion.isNotEmpty() && seedAnswer.isNotEmpty()) {
                conversationTurns.add(AiConversationTurn(seedQuestion, seedAnswer))
            }
        }
        requestAiSearchInternal(query = query, isFollowUp = true)
    }

    private fun requestAiSearchInternal(
        query: String,
        isFollowUp: Boolean,
    ) {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            showToastCallback(R.string.direct_search_enter_query)
            clearAiSearchState()
            return
        }

        if (!isFollowUp) {
            conversationTurns.clear()
        }

        val apiKey = llmApiKey
        if (apiKey.isNullOrBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    isFollowUp = isFollowUp,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery,
                    llmProviderId = activeProviderId,
                )
            }
            return
        }
        if (selectedModelId.isBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    isFollowUp = isFollowUp,
                    errorMessage = context.getString(R.string.ai_error_selected_model_unavailable),
                    activeQuery = trimmedQuery,
                    llmProviderId = activeProviderId,
                )
            }
            return
        }

        aiSearchJob?.cancel()
        val previousTurns = conversationTurns.toList()
        aiSearchJob =
            scope.launch {
                _aiSearchState.update {
                    AiSearchState(
                        status = AiSearchStatus.Loading,
                        isFollowUp = isFollowUp,
                        activeQuery = trimmedQuery,
                        llmProviderId = activeProviderId,
                    )
                }

                val selectedModel = availableGeminiModels.find { it.id == selectedModelId }
                val webSearch =
                    prepareWebSearch(
                        userPreferences = userPreferences,
                        searchQuery = trimmedQuery,
                        prompt =
                            if (isFollowUp) {
                                buildAiFollowUpPrompt(previousTurns, trimmedQuery)
                            } else {
                                trimmedQuery
                            },
                        nativeSearchSupported =
                            providerSupportsNativeSearch(activeProviderId) &&
                                selectedModel?.supportsGrounding != false,
                        nativeSearchRequested = groundingEnabled,
                        onTavilyFailure = { showToastCallback(R.string.tavily_search_failed_toast) },
                    )
                val result =
                    activeProvider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                            LlmRequest(
                                query = webSearch.prompt,
                                personalContext =
                                    if (selectedModel?.supportsSystemInstructions == false) {
                                        null
                                    } else {
                                        personalContext.takeIf { it.isNotBlank() }
                                    },
                                modelId = selectedModelId,
                                useGroundingWithGoogleSearch = webSearch.useNativeSearch,
                                thinkingEnabled =
                                    thinkingEnabled &&
                                        activeProviderId != AiSearchLlmProviderId.OPENAI &&
                                        !activeProviderId.isCustom,
                                useSystemInstruction =
                                    selectedModel?.supportsSystemInstructions != false,
                            ),
                    )

                result
                    .onSuccess { response ->
                        if (isFollowUp) {
                            conversationTurns.add(AiConversationTurn(trimmedQuery, response.text))
                        } else {
                            conversationTurns.clear()
                            conversationTurns.add(AiConversationTurn(trimmedQuery, response.text))
                        }
                        val showWebSearchFallbackTip =
                            response.webSearchDisabledForRequest &&
                                userPreferences.shouldShowWebSearchFallbackTip()
                        if (showWebSearchFallbackTip) {
                            userPreferences.recordWebSearchFallbackTipShown()
                        }
                        _aiSearchState.update {
                            AiSearchState(
                                status = AiSearchStatus.Success,
                                answer = response.text,
                                isFollowUp = isFollowUp,
                                webSearchDisabledForRequest =
                                    response.webSearchDisabledForRequest,
                                showWebSearchFallbackTip = showWebSearchFallbackTip,
                                activeQuery = trimmedQuery,
                                usedModelId = selectedModelId,
                                llmProviderId = activeProviderId,
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
                                isFollowUp = isFollowUp,
                                errorMessage = message,
                                activeQuery = trimmedQuery,
                                llmProviderId = activeProviderId,
                            )
                        }
                    }
            }
    }

    fun requestCustomToolSearch(
        query: String,
        systemInstruction: String,
        providerId: AiSearchLlmProviderId,
        modelId: String,
        groundingEnabled: Boolean,
        thinkingEnabled: Boolean = false,
        advancedPayloadJson: String? = null,
    ) {
        ensureInitialized()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return
        val resolvedSystemInstruction = expandCustomToolPrompt(systemInstruction)

        val provider = AiSearchLlmProviderRegistry.get(providerId, context)
        val apiKey = userPreferences.getLlmApiKey(providerId)?.trim()
        if (apiKey.isNullOrBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery,
                    llmProviderId = providerId,
                )
            }
            return
        }
        if (modelId.isBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    errorMessage = context.getString(R.string.ai_error_selected_model_unavailable),
                    activeQuery = trimmedQuery,
                    llmProviderId = providerId,
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
                        llmProviderId = providerId,
                    )
                }

                val providerModels =
                    if (providerId == activeProviderId) availableGeminiModels else provider.fallbackTextModels
                val useSystemInstruction =
                    providerModels.firstOrNull { it.id == modelId }?.supportsSystemInstructions
                        ?: !modelId.lowercase().startsWith("gemma-")
                val supportsGrounding =
                    modelSupportsGrounding(modelId, providerModels, providerId)
                val webSearch =
                    prepareWebSearch(
                        userPreferences = userPreferences,
                        searchQuery = trimmedQuery,
                        prompt = trimmedQuery,
                        nativeSearchSupported = providerSupportsNativeSearch(providerId) && supportsGrounding,
                        nativeSearchRequested = groundingEnabled,
                        onTavilyFailure = { showToastCallback(R.string.tavily_search_failed_toast) },
                    )
                val result =
                    provider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                            LlmRequest(
                                query = webSearch.prompt,
                                personalContext = null,
                                modelId = modelId,
                                useGroundingWithGoogleSearch = webSearch.useNativeSearch,
                                thinkingEnabled =
                                    thinkingEnabled &&
                                        providerId != AiSearchLlmProviderId.OPENAI &&
                                        !providerId.isCustom,
                                useSystemInstruction = useSystemInstruction,
                                systemInstruction = resolvedSystemInstruction,
                                advancedPayloadJson = advancedPayloadJson,
                            ),
                    )

                result
                    .onSuccess { response ->
                        val showWebSearchFallbackTip =
                            response.webSearchDisabledForRequest &&
                                userPreferences.shouldShowWebSearchFallbackTip()
                        if (showWebSearchFallbackTip) {
                            userPreferences.recordWebSearchFallbackTipShown()
                        }
                        _aiSearchState.update {
                            AiSearchState(
                                status = AiSearchStatus.Success,
                                answer = response.text,
                                webSearchDisabledForRequest =
                                    response.webSearchDisabledForRequest,
                                showWebSearchFallbackTip = showWebSearchFallbackTip,
                                activeQuery = trimmedQuery,
                                usedModelId = modelId,
                                llmProviderId = providerId,
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
                                llmProviderId = providerId,
                            )
                        }
                    }
            }
    }

    fun clearAiSearchState() {
        aiSearchJob?.cancel()
        aiSearchJob = null
        conversationTurns.clear()
        _aiSearchState.update { AiSearchState() }
    }

    private fun ensureModelExists(models: List<GeminiTextModel>): List<GeminiTextModel> =
        models.distinctBy { it.id }

    /**
     * When the catalog entry is missing (e.g. stale cache), match [AiSearchClient] Gemma
     * heuristics so we do not send `systemInstruction` JSON for models that reject it.
     */
    private fun modelSupportsSystemInstructions(modelId: String): Boolean {
        val model = availableGeminiModels.find { it.id == modelId }
        return model?.supportsSystemInstructions ?: !modelId.lowercase().startsWith("gemma-")
    }
}
