package com.tk.quicksearch.tools.calculator

import com.tk.quicksearch.search.core.CalculatorState

internal fun evaluateCalculator(query: String, explicit: Boolean): CalculatorState? {
    if (!explicit && !CalculatorUtils.isMathExpression(query)) return null
    val result = CalculatorUtils.evaluateExpression(query) ?: return null
    return CalculatorState(result = result)
}
