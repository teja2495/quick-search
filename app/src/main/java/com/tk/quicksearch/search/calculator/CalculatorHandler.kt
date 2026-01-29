package com.tk.quicksearch.search.calculator

import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import kotlinx.coroutines.CoroutineScope

class CalculatorHandler(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    fun processQuery(query: String): CalculatorState {
        val trimmedQuery = query.trim()
        if (userPreferences.isCalculatorEnabled() && CalculatorUtils.isMathExpression(trimmedQuery)) {
            return CalculatorUtils.evaluateExpression(trimmedQuery)?.let { result ->
                CalculatorState(result = result, expression = trimmedQuery)
            } ?: CalculatorState()
        }
        return CalculatorState()
    }
}
