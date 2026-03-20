package com.tk.quicksearch.tools.dateCalculator

import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.search.data.UserAppPreferences
import java.time.LocalDate
import java.time.ZoneId

class DateCalculatorHandler(
    private val userPreferences: UserAppPreferences,
) {
    fun processQuery(
        query: String,
        forceDateCalculatorMode: Boolean = false,
    ): CalculatorState {
        if (!userPreferences.isDateCalculatorEnabled()) return CalculatorState()

        val trimmedQuery = query.trim()
        if (forceDateCalculatorMode) {
            if (trimmedQuery.isEmpty()) {
                return CalculatorState(
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }
            val parsedDate = DateCalculatorUtils.parseDateQuery(trimmedQuery)
            return if (parsedDate != null) {
                CalculatorState(
                    parsedDateMillis = parsedDate.toEpochMillis(),
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            } else {
                CalculatorState(
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                    showInvalidExpression = true,
                )
            }
        }

        val parsedDate = DateCalculatorUtils.parseDateQuery(trimmedQuery) ?: return CalculatorState()
        return CalculatorState(
            parsedDateMillis = parsedDate.toEpochMillis(),
            toolType = SearchToolType.DATE_CALCULATOR,
        )
    }

    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
