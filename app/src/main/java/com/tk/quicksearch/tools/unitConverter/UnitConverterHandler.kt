package com.tk.quicksearch.tools.unitConverter

import com.tk.quicksearch.search.core.CalculatorState

internal fun evaluateUnitConverter(query: String): CalculatorState? {
    val result = UnitConverterUtils.convertQuery(query) ?: return null
    return CalculatorState(result = result)
}
