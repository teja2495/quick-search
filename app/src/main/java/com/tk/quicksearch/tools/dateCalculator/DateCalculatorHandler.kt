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

            // Time queries — checked before date queries to avoid "to" ambiguity
            DateCalculatorUtils.parseTimeDiffQuery(trimmedQuery)?.let { r ->
                return CalculatorState(
                    timeResultLabel = r.label,
                    timeContextLabel = r.contextLabel,
                    isTimeAbsoluteResult = r.isAbsolute,
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseTimeOffsetQuery(trimmedQuery)?.let { r ->
                return CalculatorState(
                    timeResultLabel = r.label,
                    timeContextLabel = r.contextLabel,
                    isTimeAbsoluteResult = r.isAbsolute,
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseTimeArithmeticQuery(trimmedQuery)?.let { r ->
                return CalculatorState(
                    timeResultLabel = r.label,
                    timeContextLabel = r.contextLabel,
                    isTimeAbsoluteResult = r.isAbsolute,
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseAbsoluteTimeQuery(trimmedQuery)?.let { (past, future) ->
                return CalculatorState(
                    timeResultLabel = past.label,
                    timeContextLabel = past.contextLabel,
                    timeResultLabel2 = future.label,
                    timeContextLabel2 = future.contextLabel,
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseDateDiffQuery(trimmedQuery)?.let { (date1, date2) ->
                return CalculatorState(
                    dateDiffLabel = DateCalculatorUtils.diffLabel(date1, date2),
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseOffsetFromDateQuery(trimmedQuery)?.let { parsedDate ->
                return CalculatorState(
                    parsedDateMillis = parsedDate.toEpochMillis(),
                    isReverseDateMode = true,
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseDateQuery(trimmedQuery)?.let { parsedDate ->
                return CalculatorState(
                    parsedDateMillis = parsedDate.toEpochMillis(),
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            DateCalculatorUtils.parseRelativeDateQuery(trimmedQuery)?.let { parsedDate ->
                return CalculatorState(
                    parsedDateMillis = parsedDate.toEpochMillis(),
                    isReverseDateMode = true,
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            }

            return CalculatorState(
                isDateCalculatorMode = true,
                toolType = SearchToolType.DATE_CALCULATOR,
                showInvalidExpression = true,
            )
        }

        // Time queries — checked before date queries to avoid "to" ambiguity
        DateCalculatorUtils.parseTimeDiffQuery(trimmedQuery)?.let { r ->
            return CalculatorState(
                timeResultLabel = r.label,
                timeContextLabel = r.contextLabel,
                isTimeAbsoluteResult = r.isAbsolute,
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseTimeOffsetQuery(trimmedQuery)?.let { r ->
            return CalculatorState(
                timeResultLabel = r.label,
                timeContextLabel = r.contextLabel,
                isTimeAbsoluteResult = r.isAbsolute,
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseTimeArithmeticQuery(trimmedQuery)?.let { r ->
            return CalculatorState(
                timeResultLabel = r.label,
                timeContextLabel = r.contextLabel,
                isTimeAbsoluteResult = r.isAbsolute,
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseAbsoluteTimeQuery(trimmedQuery)?.let { (past, future) ->
            return CalculatorState(
                timeResultLabel = past.label,
                timeContextLabel = past.contextLabel,
                timeResultLabel2 = future.label,
                timeContextLabel2 = future.contextLabel,
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseDateDiffQuery(trimmedQuery)?.let { (date1, date2) ->
            return CalculatorState(
                dateDiffLabel = DateCalculatorUtils.diffLabel(date1, date2),
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseOffsetFromDateQuery(trimmedQuery)?.let { parsedDate ->
            return CalculatorState(
                parsedDateMillis = parsedDate.toEpochMillis(),
                isReverseDateMode = true,
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseDateQuery(trimmedQuery)?.let { parsedDate ->
            return CalculatorState(
                parsedDateMillis = parsedDate.toEpochMillis(),
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        DateCalculatorUtils.parseRelativeDateQuery(trimmedQuery)?.let { parsedDate ->
            return CalculatorState(
                parsedDateMillis = parsedDate.toEpochMillis(),
                isReverseDateMode = true,
                toolType = SearchToolType.DATE_CALCULATOR,
            )
        }

        return CalculatorState()
    }

    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
