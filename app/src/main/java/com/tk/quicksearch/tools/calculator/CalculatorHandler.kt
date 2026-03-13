package com.tk.quicksearch.tools.calculator

import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.data.UserAppPreferences

class CalculatorHandler(
    private val userPreferences: UserAppPreferences,
) {
    fun processQuery(
        query: String,
        forceCalculatorMode: Boolean = false,
    ): CalculatorState {
        if (!userPreferences.isCalculatorEnabled()) return CalculatorState()

        val trimmedQuery = query.trim()
        if (forceCalculatorMode) {
            if (trimmedQuery.isEmpty()) {
                return CalculatorState(
                    isCalculatorMode = true,
                    toolType = SearchToolType.CALCULATOR,
                )
            }
            return CalculatorUtils.evaluateExpression(trimmedQuery)?.let { result ->
                CalculatorState(
                    result = result,
                    expression = trimmedQuery,
                    isCalculatorMode = true,
                    toolType = SearchToolType.CALCULATOR,
                )
            } ?: CalculatorState(
                expression = trimmedQuery,
                isCalculatorMode = true,
                toolType = SearchToolType.CALCULATOR,
                showInvalidExpression = true,
            )
        }

        if (CalculatorUtils.isMathExpression(trimmedQuery)) {
            return CalculatorUtils.evaluateExpression(trimmedQuery)?.let { result ->
                CalculatorState(
                    result = result,
                    expression = trimmedQuery,
                    toolType = SearchToolType.CALCULATOR,
                )
            } ?: CalculatorState()
        }
        return CalculatorState()
    }
}
