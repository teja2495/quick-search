package com.tk.quicksearch.search.fuzzy

import com.frosch2010.fuzzywuzzy_kotlin.FuzzySearch
import java.util.Locale

/**
 * Core fuzzy search engine providing fuzzy matching algorithms.
 * Handles the low-level fuzzy search operations.
 */
class FuzzySearchEngine {

    companion object {
        private const val ACRONYM_MAX_LENGTH = 4
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
        val trimmedQuery = query.trim()
        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())

        // Acronym matching for short queries (2-4 characters)
        if (trimmedQuery.length in 2..ACRONYM_MAX_LENGTH) {
            val acronymScore = computeAcronymScore(normalizedQuery, targetText, targetNickname)
            if (acronymScore > 0) return acronymScore
        }

        if (trimmedQuery.length < minQueryLength) return 0

        val normalizedTarget = targetText.lowercase(Locale.getDefault())

        var bestScore = FuzzySearch.tokenSetRatio(normalizedQuery, normalizedTarget)

        // Check nickname if available
        targetNickname?.let { nickname ->
            val nicknameScore = FuzzySearch.tokenSetRatio(normalizedQuery, nickname.lowercase(Locale.getDefault()))
            bestScore = maxOf(bestScore, nicknameScore)
        }

        return bestScore
    }

    /**
     * Computes acronym matching score for short queries.
     */
    private fun computeAcronymScore(query: String, targetText: String, nickname: String?): Int {
        val targetAcronym = buildAcronym(targetText)
        if (targetAcronym == query) return 100

        nickname?.let { nick ->
            val nicknameAcronym = buildAcronym(nick)
            if (nicknameAcronym == query) return 100
        }

        return 0
    }

    /**
     * Builds acronym from text (e.g., "YouTube" -> "yt", "YouTube Music" -> "ytm").
     */
    private fun buildAcronym(text: String): String {
        if (text.isBlank()) return ""

        val separated = text.replace(CAMEL_CASE_REGEX, "$1 $2")
        val tokens = separated.split(WHITESPACE_REGEX).flatMap { it.split(NON_ALPHANUMERIC_REGEX) }.filter { it.isNotBlank() }

        if (tokens.isEmpty()) return ""

        val builder = StringBuilder()
        for (token in tokens) {
            if (token.isNotEmpty()) {
                builder.append(token[0].lowercaseChar())
            }
        }

        return builder.toString()
    }
}