package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.util.CalculatorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalculatorHandler(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit
) {
    var isEnabled: Boolean = userPreferences.isCalculatorEnabled()
        private set

    fun setEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setCalculatorEnabled(enabled)
            isEnabled = enabled

            // Update UI state
            uiStateUpdater { it.copy(calculatorEnabled = enabled) }

            // If disabling calculator, clear any existing calculator result
            if (!enabled) {
                uiStateUpdater { it.copy(calculatorState = CalculatorState()) }
            }
        }
    }
    
    fun processQuery(query: String): CalculatorState {
        val trimmedQuery = query.trim()
        if (isEnabled && CalculatorUtils.isMathExpression(trimmedQuery)) {
            return CalculatorUtils.evaluateExpression(trimmedQuery)?.let { result ->
                CalculatorState(result = result, expression = trimmedQuery)
            } ?: CalculatorState()
        }
        return CalculatorState()
    }
}
