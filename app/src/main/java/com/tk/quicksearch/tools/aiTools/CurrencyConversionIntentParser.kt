package com.tk.quicksearch.tools.aiTools

/**
 * Detects currency conversion queries. Two-stage: cheap candidate scan, then full-string confirmation.
 */
object CurrencyConversionIntentParser {
    private val candidateRegex =
            Regex(
                    """(?i)\d+(?:[.,]\d+)?\s*[a-z]{3}\s*(?:to|into|in|=)\s*[a-z]{3}""",
            )

    private val confirmedRegex =
            Regex(
                    """(?i)^(?:convert|exchange|conversion(?:\s+of)?\s+)?(\d+(?:[.,]\d+)?)\s*([a-z]{3})\s*(?:to|into|in|=)\s*([a-z]{3})\s*$""",
            )

    fun isCandidate(trimmedQuery: String): Boolean = candidateRegex.containsMatchIn(trimmedQuery)

    fun parseConfirmed(trimmedQuery: String): ConfirmedCurrencyQuery? {
        val match = confirmedRegex.matchEntire(trimmedQuery.trim()) ?: return null
        val amount = match.groupValues[1].replace(",", ".")
        val from = match.groupValues[2].uppercase()
        val to = match.groupValues[3].uppercase()
        if (from.length != 3 || to.length != 3 || from == to) return null
        if (amount.toDoubleOrNull() == null) return null
        return ConfirmedCurrencyQuery(
                amount = amount,
                fromCurrency = from,
                toCurrency = to,
                originalQuery = trimmedQuery.trim(),
        )
    }
}

data class ConfirmedCurrencyQuery(
        val amount: String,
        val fromCurrency: String,
        val toCurrency: String,
        val originalQuery: String,
)
