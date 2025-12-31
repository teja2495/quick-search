package com.tk.quicksearch.search.searchengines

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.core.DirectSearchState
import com.tk.quicksearch.search.core.DirectSearchStatus
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
    private val scope: CoroutineScope
) {
    private val _directSearchState = MutableStateFlow(DirectSearchState())
    val directSearchState: StateFlow<DirectSearchState> = _directSearchState.asStateFlow()

    private var geminiApiKey: String? = userPreferences.getGeminiApiKey()
    private var geminiClient: DirectSearchClient? = geminiApiKey?.let { DirectSearchClient(it) }
    private var personalContext: String = userPreferences.getPersonalContext().orEmpty()
    
    private var DirectSearchJob: Job? = null

    fun getGeminiApiKey() = geminiApiKey
    fun getPersonalContext() = personalContext

    fun setGeminiApiKey(apiKey: String?) {
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
        val normalized = context?.trim().orEmpty()
        if (normalized == personalContext) return

        personalContext = normalized
        userPreferences.setPersonalContext(normalized.takeUnless { it.isBlank() })
    }

    fun requestDirectSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            clearDirectSearchState()
            return
        }

        val client = geminiClient
        if (client == null || geminiApiKey.isNullOrBlank()) {
            _directSearchState.update {
                DirectSearchState(
                    status = DirectSearchStatus.Error,
                    errorMessage = context.getString(R.string.direct_search_error_no_key),
                    activeQuery = trimmedQuery
                )
            }
            return
        }

        DirectSearchJob?.cancel()
        DirectSearchJob = scope.launch {
            _directSearchState.update {
                DirectSearchState(
                    status = DirectSearchStatus.Loading,
                    activeQuery = trimmedQuery
                )
            }

            val result = client.fetchAnswer(
                trimmedQuery,
                personalContext.takeIf { it.isNotBlank() }
            )
            result.onSuccess { answer ->
                _directSearchState.update {
                    DirectSearchState(
                        status = DirectSearchStatus.Success,
                        answer = answer,
                        activeQuery = trimmedQuery
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                val message = error.message
                    ?: context.getString(R.string.direct_search_error_generic)
                _directSearchState.update {
                    DirectSearchState(
                        status = DirectSearchStatus.Error,
                        errorMessage = message,
                        activeQuery = trimmedQuery
                    )
                }
            }
        }
    }

    fun retryDirectSearch(currentQuery: String) {
        val lastQuery = _directSearchState.value.activeQuery ?: currentQuery
        if (lastQuery.isBlank()) return
        requestDirectSearch(lastQuery)
    }

    fun clearDirectSearchState() {
        DirectSearchJob?.cancel()
        DirectSearchJob = null
        _directSearchState.update { DirectSearchState() }
    }
}
