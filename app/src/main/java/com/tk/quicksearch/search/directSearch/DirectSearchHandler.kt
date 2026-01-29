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

    private var isInitialized = false

    private fun ensureInitialized() {
        if (!isInitialized) {
            geminiApiKey = userPreferences.getGeminiApiKey()
            geminiClient = geminiApiKey?.let { DirectSearchClient(it) }
            personalContext = userPreferences.getPersonalContext().orEmpty()
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

    fun setGeminiApiKey(apiKey: String?) {
        ensureInitialized()
        val normalized = apiKey?.trim().takeUnless { it.isNullOrBlank() }
        // We can't check against previous value easily without storing it locally in VM or here.
        // But here we have it.
        if (normalized == geminiApiKey) return

        geminiApiKey = normalized
        geminiClient = normalized?.let { DirectSearchClient(it) }
        userPreferences.setGeminiApiKey(normalized)

        if (geminiApiKey == null) {
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

                val result =
                    client.fetchAnswer(
                        trimmedQuery,
                        personalContext.takeIf { it.isNotBlank() },
                    )
                result
                    .onSuccess { answer ->
                        _directSearchState.update {
                            DirectSearchState(
                                status = DirectSearchStatus.Success,
                                answer = answer,
                                activeQuery = trimmedQuery,
                            )
                        }
                    }.onFailure { error ->
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
}
