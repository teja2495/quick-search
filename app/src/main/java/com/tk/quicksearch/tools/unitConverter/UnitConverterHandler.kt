package com.tk.quicksearch.tools.unitConverter

import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.data.UserAppPreferences

class UnitConverterHandler(
    private val userPreferences: UserAppPreferences,
) {
    fun processQuery(
        query: String,
        forceUnitConverterMode: Boolean = false,
    ): CalculatorState {
        if (!userPreferences.isUnitConverterEnabled()) return CalculatorState()

        val trimmedQuery = query.trim()
        if (forceUnitConverterMode) {
            if (trimmedQuery.isEmpty()) {
                return CalculatorState(
                    isUnitConverterMode = true,
                    toolType = SearchToolType.UNIT_CONVERTER,
                )
            }
            return UnitConverterUtils.convertQuery(trimmedQuery)?.let { result ->
                CalculatorState(
                    result = result,
                    expression = trimmedQuery,
                    isUnitConverterMode = true,
                    toolType = SearchToolType.UNIT_CONVERTER,
                )
            }
                ?: CalculatorState(
                    expression = trimmedQuery,
                    isUnitConverterMode = true,
                    toolType = SearchToolType.UNIT_CONVERTER,
                    showInvalidExpression = true,
                )
        }

        return UnitConverterUtils.convertQuery(trimmedQuery)?.let { result ->
            CalculatorState(
                result = result,
                expression = trimmedQuery,
                toolType = SearchToolType.UNIT_CONVERTER,
            )
        } ?: CalculatorState()
    }
}
