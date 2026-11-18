package com.tk.quicksearch.tools.aiSearch

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchState
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AiSearchHandler(
    private val context: Context,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val showToastCallback: (Int) -> Unit,
) {
    private val _aiSearchState = MutableStateFlow(AiSearchState())
    val aiSearchState: StateFlow<AiSearchState> = _aiSearchState.asStateFlow()

    // Written on the startup/IO threads and read on Main, so every field is @Volatile and
    // initialization runs under [initLock].
    @Volatile private var activeProviderId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI
    @Volatile private var activeProvider: AiSearchLlmProvider =
        AiSearchLlmProviderRegistry.get(AiSearchLlmProviderId.GEMINI, context)
    @Volatile private var llmApiKey: String? = null
    @Volatile private var personalContext: String = ""
    @Volatile private var selectedModelId: String = ""
    @Volatile private var groundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED
    @Volatile private var thinkingEnabled: Boolean = false
    @Volatile private var availableModels: List<LlmTextModel> = emptyList()
    private val initLock = Any()

    @Volatile private var hasAnyLlmApiKey: Boolean = false
    @Volatile private var hasLoadedApiKeyCache: Boolean = false
    @Volatile private var isInitialized = false
    private var aiSearchJob: Job? = null
    /** A request waiting for first-time initialization to finish off the main thread. */
    private var pendingInitRequest: Job? = null
    private val conversationTurns = mutableListOf<AiConversationTurn>()
    /** Shared by every request of the current conversation; replaced when a new one starts. */
    private var conversationSessionId: String? = null
    private var isQuickSearchHelpConversation = false

    private fun ensureInitialized(configuredApiKey: Boolean? = null) {
        if (isInitialized) return
        synchronized(initLock) {
            if (isInitialized) return
            activeProviderId = userPreferences.getAiSearchProviderId()
            activeProvider = AiSearchLlmProviderRegistry.get(activeProviderId, context)
            llmApiKey = userPreferences.getLlmApiKey(activeProviderId)
            personalContext = userPreferences.getLlmPersonalContext(activeProviderId).orEmpty()
            selectedModelId = userPreferences.getLlmModel(activeProviderId)
            groundingEnabled = userPreferences.isLlmGroundingEnabled(activeProviderId)
            thinkingEnabled = userPreferences.isLlmThinkingEnabled(activeProviderId)
            availableModels = emptyList()
            hasAnyLlmApiKey = configuredApiKey ?: userPreferences.hasAnyLlmApiKey()
            hasLoadedApiKeyCache = true
            isInitialized = true
        }
    }

    /** Reuses the authoritative key check performed by deferred startup. Call off Main. */
    fun initializeForStartup(hasApiKey: Boolean) {
        LlmModelCatalogCache.restore(context)
        ensureInitialized(hasApiKey)
        synchronized(initLock) {
            if (availableModels.isEmpty()) {
                availableModels = LlmModelCatalogCache.get(activeProviderId).orEmpty()
            }
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
        availableModels = emptyList()
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
        userPreferences.setLlmApiKey(activeProviderId, normalized)

        hasAnyLlmApiKey = userPreferences.hasAnyLlmApiKey()
        if (llmApiKey == null) {
            availableModels = emptyList()
            clearAiSearchState()
        }
    }

    fun setLlmApiKey(
        providerId: AiSearchLlmProviderId,
        apiKey: String?,
    ) {
        ensureInitialized()
        val normalized = apiKey?.trim().takeUnless { it.isNullOrBlank() }
        val isNewKey = normalized != null && userPreferences.getLlmApiKey(providerId).isNullOrBlank()
        userPreferences.setLlmApiKey(providerId, normalized)

        when {
            normalized != null &&
                (providerId == activeProviderId || llmApiKey.isNullOrBlank() ||
                    (isNewKey && outranksActiveProvider(providerId))) -> {
                setAiSearchProviderId(providerId)
                llmApiKey = normalized
            }
            providerId == activeProviderId && normalized == null -> {
                val nextProvider =
                    AiSearchLlmProviderId.preferredOf(
                        userPreferences.getConfiguredLlmProviderIds().filter {
                            !userPreferences.getLlmApiKey(it).isNullOrBlank()
                        },
                    )
                if (nextProvider != null) {
                    setAiSearchProviderId(nextProvider)
                } else {
                    llmApiKey = null
                    availableModels = emptyList()
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
        availableModels = ensureModelExists(availableModels)
    }

    /** Makes [providerId] active when no provider is usable or it outranks the active one. */
    fun activateIfPreferred(providerId: AiSearchLlmProviderId) {
        ensureInitialized()
        if (llmApiKey.isNullOrBlank() || outranksActiveProvider(providerId)) {
            setAiSearchProviderId(providerId)
        }
    }

    private fun outranksActiveProvider(providerId: AiSearchLlmProviderId): Boolean =
        AiSearchLlmProviderId.defaultPriority(providerId) <
            AiSearchLlmProviderId.defaultPriority(activeProviderId)

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
        if (!isThinkingRequestSupported(activeProviderId)) return false
        return thinkingEnabled
    }

    fun setThinkingEnabled(enabled: Boolean) {
        ensureInitialized()
        if (!isThinkingRequestSupported(activeProviderId)) return
        if (enabled == thinkingEnabled) return

        thinkingEnabled = enabled
        userPreferences.setLlmThinkingEnabled(activeProviderId, enabled)
    }

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

    fun getAvailableModels(): List<LlmTextModel> {
        ensureInitialized()
        return availableModels
    }

    fun reloadFromPreferences() {
        synchronized(initLock) {
            isInitialized = false
            hasLoadedApiKeyCache = false
            ensureInitialized()
        }
        clearAiSearchState()
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

    suspend fun refreshAvailableModels(forceRefresh: Boolean = false): List<LlmTextModel> {
        ensureInitialized()

        val providerId = activeProviderId
        val provider = activeProvider
        val apiKey = llmApiKey ?: return availableModels
        val result = LlmModelCatalogCache.fetchIfNeeded(
            context, providerId, if (forceRefresh) 0L else LlmModelCatalogCache.STARTUP_MAX_AGE_MS,
        ) { provider.fetchAvailableTextModels(apiKey, context) }
        synchronized(initLock) {
            // Settings may have switched providers while this request was in flight.
            if (activeProviderId != providerId || llmApiKey != apiKey) return availableModels
            val fetched = result.getOrNull() ?: LlmModelCatalogCache.get(providerId) ?: return availableModels
            if (result.isSuccess) {
                val resolvedModelId = resolveModelSelectionOrDefault(providerId, selectedModelId, fetched)
                if (resolvedModelId != selectedModelId) setSelectedModelId(resolvedModelId)
            }
            availableModels = ensureModelExists(fetched)
            return availableModels
        }
    }

    /** Reuses a catalog already fetched by settings instead of issuing a second network request. */
    fun updateAvailableModels(models: List<LlmTextModel>) {
        ensureInitialized()
        availableModels = ensureModelExists(models)
    }

    fun requestAiSearch(query: String) {
        isQuickSearchHelpConversation = false
        requestAiSearchInternal(query = query, isFollowUp = false)
    }

    /** Answers a `@help` question from the bundled feature docs, without web search. */
    fun requestQuickSearchHelp(query: String) {
        if (QuickSearchHelp.stripAlias(query).isEmpty()) {
            // Bare `@help`: show the intro card (Success with no answer) without calling the model.
            clearAiSearchState()
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Success,
                    activeQuery = query.trim(),
                    isQuickSearchHelp = true,
                )
            }
            return
        }
        isQuickSearchHelpConversation = true
        requestAiSearchInternal(query = query, isFollowUp = false)
    }

    fun requestAiFollowUp(
        query: String,
        previousQuestion: String,
        previousAnswer: String,
        isQuickSearchHelp: Boolean = false,
    ) {
        isQuickSearchHelpConversation = isQuickSearchHelp
        if (conversationTurns.isEmpty()) {
            val seedQuestion =
                if (isQuickSearchHelp) {
                    QuickSearchHelp.stripAlias(previousQuestion)
                } else {
                    previousQuestion.trim()
                }
            val seedAnswer = previousAnswer.trim()
            if (seedQuestion.isNotEmpty() && seedAnswer.isNotEmpty()) {
                conversationTurns.add(AiConversationTurn(seedQuestion, seedAnswer))
            }
        }
        requestAiSearchInternal(query = query, isFollowUp = true)
    }

    /**
     * Runs [block] on Main once the handler is initialized. The first request after a cold start
     * reads (and decrypts) preferences on IO instead of blocking the main thread.
     */
    private fun runWhenInitialized(block: () -> Unit) {
        pendingInitRequest?.cancel()
        pendingInitRequest = null
        if (isInitialized) {
            block()
            return
        }
        pendingInitRequest =
            scope.launch {
                withContext(Dispatchers.IO) { ensureInitialized() }
                pendingInitRequest = null
                block()
            }
    }

    private fun requestAiSearchInternal(
        query: String,
        isFollowUp: Boolean,
    ) = runWhenInitialized { startAiSearch(query, isFollowUp) }

    private fun startAiSearch(
        query: String,
        isFollowUp: Boolean,
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            showToastCallback(R.string.direct_search_enter_query)
            clearAiSearchState()
            return
        }

        if (!isFollowUp) {
            conversationTurns.clear()
        }
        if (!isFollowUp || conversationSessionId == null) {
            conversationSessionId = UUID.randomUUID().toString()
        }
        val sessionId = conversationSessionId
        val isHelp = isQuickSearchHelpConversation
        // activeQuery keeps the typed `@help` text so query-change clearing and history still match.
        val questionText = if (isHelp) QuickSearchHelp.stripAlias(trimmedQuery) else trimmedQuery

        val apiKey = llmApiKey
        if (apiKey.isNullOrBlank()) {
            _aiSearchState.update {
                AiSearchState(
                    status = AiSearchStatus.Error,
                    isFollowUp = isFollowUp,
                    errorMessage =
                        context.getString(
                            if (isHelp) {
                                R.string.quick_search_help_no_api_key
                            } else {
                                R.string.direct_search_error_no_key
                            },
                        ),
                    activeQuery = trimmedQuery,
                    canRetry = false,
                    llmProviderId = activeProviderId,
                    isQuickSearchHelp = isHelp,
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
                    canRetry = false,
                    llmProviderId = activeProviderId,
                    isQuickSearchHelp = isHelp,
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
                        isQuickSearchHelp = isHelp,
                    )
                }

                val selectedModel = availableModels.find { it.id == selectedModelId }
                val webSearch =
                    if (isHelp) {
                        PreparedWebSearch(prompt = questionText, useNativeSearch = false)
                    } else {
                        prepareWebSearch(
                            userPreferences = userPreferences,
                            searchQuery = trimmedQuery,
                            prompt = questionText,
                            nativeSearchSupported =
                                providerSupportsNativeSearch(activeProviderId) &&
                                    selectedModel?.supportsGrounding != false,
                            nativeSearchRequested = groundingEnabled,
                            onTavilyFailure = { showToastCallback(R.string.tavily_search_failed_toast) },
                        )
                    }
                val result =
                    activeProvider.fetchAnswer(
                        apiKey = apiKey,
                        context = context,
                        request =
                            LlmRequest(
                                query = webSearch.prompt,
                                personalContext =
                                    if (isHelp || selectedModel?.supportsSystemInstructions == false) {
                                        null
                                    } else {
                                        personalContext.takeIf { it.isNotBlank() }
                                    },
                                modelId = selectedModelId,
                                useGroundingWithGoogleSearch = webSearch.useNativeSearch,
                                thinkingEnabled =
                                    thinkingEnabled && isThinkingRequestSupported(activeProviderId),
                                useSystemInstruction =
                                    selectedModel?.supportsSystemInstructions != false,
                                systemInstruction =
                                    if (isHelp) QuickSearchHelp.systemInstruction(context) else null,
                                // Sent as real messages (not folded into the prompt) so web search
                                // results wrap only the new question and earlier turns stay a
                                // stable prefix that providers can serve from their prompt cache.
                                history = if (isFollowUp) previousTurns else emptyList(),
                                sessionId = sessionId,
                            ),
                    )

                result
                    .onSuccess { response ->
                        if (isFollowUp) {
                            conversationTurns.add(AiConversationTurn(questionText, response.text))
                        } else {
                            conversationTurns.clear()
                            conversationTurns.add(AiConversationTurn(questionText, response.text))
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
                                isQuickSearchHelp = isHelp,
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
                                isQuickSearchHelp = isHelp,
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
    ) = runWhenInitialized {
        startCustomToolSearch(
            query, systemInstruction, providerId, modelId, groundingEnabled, thinkingEnabled, advancedPayloadJson,
        )
    }

    private fun startCustomToolSearch(
        query: String,
        systemInstruction: String,
        providerId: AiSearchLlmProviderId,
        modelId: String,
        groundingEnabled: Boolean,
        thinkingEnabled: Boolean,
        advancedPayloadJson: String?,
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return
        val resolvedSystemInstruction = expandCustomToolPrompt(systemInstruction)

        aiSearchJob?.cancel()
        aiSearchJob =
            scope.launch {
                // Provider lookup and key decryption read encrypted preferences; keep them off Main.
                val (provider, apiKey) =
                    withContext(Dispatchers.IO) {
                        AiSearchLlmProviderRegistry.get(providerId, context) to
                            userPreferences.getLlmApiKey(providerId)?.trim()
                    }
                if (apiKey.isNullOrBlank()) {
                    _aiSearchState.update {
                        AiSearchState(
                            status = AiSearchStatus.Error,
                            errorMessage = context.getString(R.string.direct_search_error_no_key),
                            activeQuery = trimmedQuery,
                            canRetry = false,
                            llmProviderId = providerId,
                        )
                    }
                    return@launch
                }
                if (modelId.isBlank()) {
                    _aiSearchState.update {
                        AiSearchState(
                            status = AiSearchStatus.Error,
                            errorMessage = context.getString(R.string.ai_error_selected_model_unavailable),
                            activeQuery = trimmedQuery,
                            canRetry = false,
                            llmProviderId = providerId,
                        )
                    }
                    return@launch
                }
                _aiSearchState.update {
                    AiSearchState(
                        status = AiSearchStatus.Loading,
                        activeQuery = trimmedQuery,
                        llmProviderId = providerId,
                    )
                }

                val providerModels =
                    if (providerId == activeProviderId) availableModels else provider.fallbackTextModels
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
                                    thinkingEnabled && isThinkingRequestSupported(providerId),
                                useSystemInstruction = useSystemInstruction,
                                systemInstruction = resolvedSystemInstruction,
                                advancedPayloadJson = advancedPayloadJson,
                                // Custom tool runs are single questions, so each is its own session.
                                sessionId = UUID.randomUUID().toString(),
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
        pendingInitRequest?.cancel()
        pendingInitRequest = null
        aiSearchJob?.cancel()
        aiSearchJob = null
        conversationTurns.clear()
        conversationSessionId = null
        isQuickSearchHelpConversation = false
        _aiSearchState.update { AiSearchState() }
    }

    private fun ensureModelExists(models: List<LlmTextModel>): List<LlmTextModel> =
        models.distinctBy { it.id }

}
