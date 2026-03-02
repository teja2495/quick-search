package com.tk.quicksearch.tools.calculator

import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.data.UserAppPreferences

class CalculatorHandler(
    private val userPreferences: UserAppPreferences,
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
