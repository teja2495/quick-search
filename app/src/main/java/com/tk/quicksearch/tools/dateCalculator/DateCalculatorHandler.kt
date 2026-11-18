package com.tk.quicksearch.tools.dateCalculator

import com.tk.quicksearch.search.core.CalculatorState
import java.time.LocalDate
import java.time.ZoneId

internal fun evaluateDateCalculator(query: String, explicit: Boolean): CalculatorState? {
    // Time queries — checked before date queries to avoid "to" ambiguity
    DateCalculatorUtils.parseTimeDiffQuery(query)?.let { r ->
        return CalculatorState(
            timeResultLabel = r.label,
            timeContextLabel = r.contextLabel,
            isTimeAbsoluteResult = r.isAbsolute,
        )
    }

    DateCalculatorUtils.parseTimeOffsetQuery(query)?.let { r ->
        return CalculatorState(
            timeResultLabel = r.label,
            timeContextLabel = r.contextLabel,
            isTimeAbsoluteResult = r.isAbsolute,
        )
    }

    DateCalculatorUtils.parseTimeArithmeticQuery(query)?.let { r ->
        return CalculatorState(
            timeResultLabel = r.label,
            timeContextLabel = r.contextLabel,
            isTimeAbsoluteResult = r.isAbsolute,
        )
    }

    // Only inspect the trailing "in …" expression after standalone calculator parsing
    // fails, so ordinary searches don't change calculator matching or its hot path.
    if (!explicit) {
        DateCalculatorUtils.parseTrailingTimeArithmeticQuery(query)?.let { r ->
            return CalculatorState(
                timeResultLabel = r.label,
                timeContextLabel = r.contextLabel,
                isTimeAbsoluteResult = r.isAbsolute,
            )
        }
    }

    DateCalculatorUtils.parseAbsoluteTimeQuery(query)?.let { (next, last) ->
        return CalculatorState(
            timeResultLabel = next.label,
            timeContextLabel = next.contextLabel,
            timeResultLabel2 = last.label,
        )
    }

    DateCalculatorUtils.parseDateDiffQuery(query)?.let { (date1, date2) ->
        return CalculatorState(
            dateDiffLabel = DateCalculatorUtils.diffLabel(date1, date2),
        )
    }

    DateCalculatorUtils.parseOffsetFromDateQuery(query)?.let { parsedDate ->
        return CalculatorState(
            parsedDateMillis = parsedDate.toEpochMillis(),
            isReverseDateMode = true,
        )
    }

    DateCalculatorUtils.parseDateQuery(query)?.let { parsedDate ->
        return CalculatorState(
            parsedDateMillis = parsedDate.toEpochMillis(),
        )
    }

    DateCalculatorUtils.parseRelativeDateQuery(query)?.let { parsedDate ->
        return CalculatorState(
            parsedDateMillis = parsedDate.toEpochMillis(),
            isReverseDateMode = true,
        )
    }

    return null
}

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
