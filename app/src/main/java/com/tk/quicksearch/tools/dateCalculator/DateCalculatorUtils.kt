package com.tk.quicksearch.tools.dateCalculator

import java.time.LocalDate
import java.util.Locale

object DateCalculatorUtils {
    private val monthNames: Map<String, Int> = buildMap {
        put("january", 1); put("jan", 1)
        put("february", 2); put("feb", 2)
        put("march", 3); put("mar", 3)
        put("april", 4); put("apr", 4)
        put("may", 5)
        put("june", 6); put("jun", 6)
        put("july", 7); put("jul", 7)
        put("august", 8); put("aug", 8)
        put("september", 9); put("sep", 9)
        put("october", 10); put("oct", 10)
        put("november", 11); put("nov", 11)
        put("december", 12); put("dec", 12)
    }

    // Matches month names (full or abbreviated, case-insensitive)
    private val monthPattern = Regex(
        "\\b(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|october|oct|november|nov|december|dec)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val yearPattern = Regex("\\b(\\d{4})\\b")
    private val dayPattern = Regex("\\b(\\d{1,2})\\b")

    /**
     * Parses a date query containing a month name, a 4-digit year, and a 1-2 digit day in any order.
     *
     * Supported formats:
     *   March 12 2025 | 12 March 2025 | 2025 March 12 | 2025 12 March | Mar 12 2025 | etc.
     */
    fun parseDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        // Must contain a recognizable month name
        val monthMatch = monthPattern.find(lower) ?: return null
        val monthNum = monthNames[monthMatch.value.lowercase(Locale.US)] ?: return null

        // Remove the month name to isolate numbers
        val withoutMonth = lower.replace(monthMatch.value, " ")

        // Find 4-digit year
        val yearMatch = yearPattern.find(withoutMonth) ?: return null
        val year = yearMatch.groupValues[1].toIntOrNull() ?: return null

        // Remove year to isolate the day
        val withoutYear = withoutMonth.replace(yearMatch.value, " ")

        // Remaining numeric token is the day
        val dayMatch = dayPattern.find(withoutYear.trim()) ?: return null
        val day = dayMatch.groupValues[1].toIntOrNull() ?: return null

        // Ensure no unexpected extra tokens remain
        val remaining = withoutYear.replace(dayMatch.value, " ").trim()
        if (remaining.any { it.isLetterOrDigit() }) return null

        return try {
            LocalDate.of(year, monthNum, day)
        } catch (_: Exception) {
            null
        }
    }
}
