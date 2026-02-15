package com.tk.quicksearch.search.directSearch

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

    private var geminiApiKey: String? = null
    private var geminiClient: DirectSearchClient? = null
    private var personalContext: String = ""
    private var geminiModel: String = GeminiModelCatalog.DEFAULT_MODEL_ID
    private var geminiGroundingEnabled: Boolean = GeminiModelCatalog.DEFAULT_GROUNDING_ENABLED
    private var availableGeminiModels: List<GeminiTextModel> =
            GeminiModelCatalog.FALLBACK_TEXT_MODELS
    private var hasLoadedGeminiModelsFromApi: Boolean = false

    private var isInitialized = false

    private fun ensureInitialized() {
        if (!isInitialized) {
            geminiApiKey = userPreferences.getGeminiApiKey()
            geminiClient = geminiApiKey?.let { DirectSearchClient(it) }
            personalContext = userPreferences.getPersonalContext().orEmpty()
            geminiModel = userPreferences.getGeminiModel()
            geminiGroundingEnabled = userPreferences.isGeminiGroundingEnabled()
            availableGeminiModels = ensureModelExists(GeminiModelCatalog.FALLBACK_TEXT_MODELS)
            hasLoadedGeminiModelsFromApi = false
            isInitialized = true
        }
    }

    private var directSearchJob: Job? = null

    fun getGeminiApiKey(): String? {
        ensureInitialized()
        return geminiApiKey
    }

    fun getPersonalContext(): String {
        ensureInitialized()
        return personalContext
    }

    fun getGeminiModel(): String {
        ensureInitialized()
        return geminiModel
    }

    fun isGeminiGroundingEnabled(): Boolean {
        ensureInitialized()
        return geminiGroundingEnabled
    }

    fun getAvailableGeminiModels(): List<GeminiTextModel> {
        ensureInitialized()
        return availableGeminiModels
    }

    fun setGeminiApiKey(apiKey: String?) {
        ensureInitialized()
        val normalized = apiKey?.trim().takeUnless { it.isNullOrBlank() }
        // We can't check against previous value easily without storing it locally in VM or here.
        // But here we have it.
        if (normalized == geminiApiKey) return

        geminiApiKey = normalized
        geminiClient = normalized?.let { DirectSearchClient(it) }
        hasLoadedGeminiModelsFromApi = false
        userPreferences.setGeminiApiKey(normalized)

        if (geminiApiKey == null) {
            availableGeminiModels = ensureModelExists(GeminiModelCatalog.FALLBACK_TEXT_MODELS)
            clearDirectSearchState()
        }
        // SearchEngineManager update is done in VM, we can expose a flow or callback if needed
        // But simple getter in VM is enough for init.
    }

    fun setPersonalContext(context: String?) {
        ensureInitialized()
        val normalized = context?.trim().orEmpty()
        if (normalized == personalContext) return

        personalContext = normalized
        userPreferences.setPersonalContext(normalized.takeUnless { it.isBlank() })
    }

    fun setGeminiModel(modelId: String?) {
        ensureInitialized()
        val normalized =
                modelId?.trim().takeUnless { it.isNullOrBlank() }
                        ?: GeminiModelCatalog.DEFAULT_MODEL_ID
        if (normalized == geminiModel) return

        geminiModel = normalized
        userPreferences.setGeminiModel(normalized)
        availableGeminiModels = ensureModelExists(availableGeminiModels)
    }

    fun setGeminiGroundingEnabled(enabled: Boolean) {
        ensureInitialized()
        if (enabled == geminiGroundingEnabled) return

        geminiGroundingEnabled = enabled
        userPreferences.setGeminiGroundingEnabled(enabled)
    }

    suspend fun refreshAvailableGeminiModels(forceRefresh: Boolean = false): List<GeminiTextModel> {
        ensureInitialized()

        val apiKey = geminiApiKey ?: return availableGeminiModels
        if (!forceRefresh && hasLoadedGeminiModelsFromApi) {
            return availableGeminiModels
        }

        val fetched =
                DirectSearchClient.fetchAvailableTextModels(apiKey)
                        .getOrDefault(GeminiModelCatalog.FALLBACK_TEXT_MODELS)
        availableGeminiModels = ensureModelExists(fetched)
        hasLoadedGeminiModelsFromApi = true
        return availableGeminiModels
    }

    fun requestDirectSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            showToastCallback(R.string.direct_search_enter_query)
            clearDirectSearchState()
            return
        }

        val client = geminiClient
        if (client == null || geminiApiKey.isNullOrBlank()) {
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

                    val selectedModel =
                            availableGeminiModels.find { it.id == geminiModel }
                    val result =
                            client.fetchAnswer(
                                    query = trimmedQuery,
                                    personalContext =
                                            if (selectedModel?.supportsSystemInstructions == false) null
                                            else personalContext.takeIf { it.isNotBlank() },
                                    modelId = geminiModel,
                                    useGroundingWithGoogleSearch =
                                            geminiGroundingEnabled &&
                                                    (selectedModel?.supportsGrounding != false),
                                    useSystemInstruction = selectedModel?.supportsSystemInstructions != false,
                            )
                    result
                            .onSuccess { answer ->
                                _directSearchState.update {
                                    DirectSearchState(
                                            status = DirectSearchStatus.Success,
                                            answer = answer,
                                            activeQuery = trimmedQuery,
                                            usedModelId = geminiModel,
                                    )
                                }
                            }
                            .onFailure { error ->
                                if (error is CancellationException) return@onFailure
                                val message =
                                        error.message
                                                ?: context.getString(
                                                        R.string.direct_search_error_generic,
                                                )
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
        val normalized = if (models.isEmpty()) GeminiModelCatalog.FALLBACK_TEXT_MODELS else models
        return if (normalized.any { it.id == geminiModel }) {
            normalized
        } else {
            listOf(
                    GeminiTextModel(
                            id = geminiModel,
                            displayName = geminiModel,
                    ),
            ) + normalized
        }
    }
}
