package com.tk.quicksearch.search.fuzzy

import com.frosch2010.fuzzywuzzy_kotlin.FuzzySearch
import java.util.Locale

/**
 * Core fuzzy search engine providing fuzzy matching algorithms.
 * Handles the low-level fuzzy search operations.
 */
class FuzzySearchEngine {

    companion object {
        private const val ABBREVIATION_MAX_QUERY_LENGTH = 3
        private const val SUBSEQUENCE_MAX_QUERY_LENGTH = 2
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private val NON_ALPHANUMERIC_REGEX = "[^A-Za-z0-9]+".toRegex()
        private val CAMEL_CASE_REGEX = "([a-z])([A-Z])".toRegex()
    }

    /**
     * Computes a fuzzy match score between a query and target text.
     *
     * @param query The search query
     * @param targetText The text to match against (e.g., app name)
     * @param targetNickname Optional nickname for the target
     * @param minQueryLength Minimum query length required for fuzzy matching
     * @return Score from 0-100, where higher scores indicate better matches
     */
    fun computeScore(
        query: String,
        targetText: String,
        targetNickname: String? = null,
        minQueryLength: Int = 3
    ): Int {
        var bestScore = 0
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())

        // Abbreviation matching for short queries
        val compactQuery = normalizedQuery.replace(WHITESPACE_REGEX, "")
        if (compactQuery.length in 2..ABBREVIATION_MAX_QUERY_LENGTH) {
            bestScore = maxOf(bestScore, computeAbbreviationScore(compactQuery, targetText, targetNickname))
        }

        // Token set ratio matching for longer queries
        if (normalizedQuery.length >= minQueryLength) {
            val normalizedTarget = targetText.lowercase(Locale.getDefault())
            val targetScore = FuzzySearch.tokenSetRatio(normalizedQuery, normalizedTarget)
            bestScore = maxOf(bestScore, targetScore)

            // Check nickname if available
            targetNickname?.let { nickname ->
                val nicknameScore = FuzzySearch.tokenSetRatio(normalizedQuery, nickname.lowercase(Locale.getDefault()))
                bestScore = maxOf(bestScore, nicknameScore)
            }
        }

        return bestScore
    }

    /**
     * Computes abbreviation/initialism matching score.
     */
    private fun computeAbbreviationScore(query: String, targetText: String, nickname: String?): Int {
        if (query.length < 2) return 0

        val targetInitialism = buildInitialism(targetText)
        val nicknameInitialism = nickname?.let { buildInitialism(it) } ?: ""

        return when {
            targetInitialism.startsWith(query) -> 100
            nicknameInitialism.startsWith(query) -> 100
            query.length <= SUBSEQUENCE_MAX_QUERY_LENGTH &&
            (isSubsequence(query, targetText.lowercase(Locale.getDefault())) ||
             nickname?.let { isSubsequence(query, it.lowercase(Locale.getDefault())) } == true) -> 80
            else -> 0
        }
    }

    /**
     * Builds initialism from text (e.g., "WhatsApp" -> "wa", "Google Maps" -> "gm").
     */
    private fun buildInitialism(text: String): String {
        if (text.isBlank()) return ""

        val separated = text.replace(CAMEL_CASE_REGEX, "$1 $2")
        val tokens = separated.split(NON_ALPHANUMERIC_REGEX).filter { it.isNotBlank() }

        if (tokens.isEmpty()) return ""

        val builder = StringBuilder()
        for (token in tokens) {
            if (token.length > 1 && token.none { it.isLowerCase() }) {
                // All caps token (like "API") - use as-is but lowercase
                builder.append(token.lowercase(Locale.getDefault()))
            } else {
                // Regular token - take first char
                builder.append(token[0].lowercaseChar())
            }
        }

        return builder.toString()
    }

    /**
     * Checks if query is a subsequence of text (e.g., "wa" in "WhatsApp").
     */
    private fun isSubsequence(query: String, text: String): Boolean {
        var index = 0
        for (ch in text) {
            if (ch == query[index]) {
                index++
                if (index == query.length) {
                    return true
                }
            }
        }
        return false
    }
}