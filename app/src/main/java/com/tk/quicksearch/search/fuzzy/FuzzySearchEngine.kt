package com.tk.quicksearch.search.fuzzy

import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.SearchTextNormalizer

/**
 * Core fuzzy search engine providing fuzzy matching algorithms.
 * Handles the low-level fuzzy search operations.
 */
class FuzzySearchEngine {
    companion object {
        private const val ACRONYM_MAX_LENGTH = 4
        private const val SHORT_QUERY_TYPO_MIN_LENGTH = 3
        private const val SHORT_QUERY_TYPO_MAX_LENGTH = 5
        private const val SHORT_QUERY_PREFIX_MAX_EDIT_DISTANCE = 1
        private const val SHORT_QUERY_PREFIX_TYPO_SCORE = 82
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private val NON_ALPHANUMERIC_REGEX = "[^\\p{L}\\p{N}]+".toRegex()
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
        minQueryLength: Int = 3,
    ): Int {
        val trimmedQuery = query.trim()
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(trimmedQuery)

        // Acronym matching for short queries (2-4 characters)
        if (trimmedQuery.length in 2..ACRONYM_MAX_LENGTH) {
            val acronymScore = computeAcronymScore(normalizedQuery, targetText, targetNickname)
            if (acronymScore > 0) return acronymScore
        }

        if (trimmedQuery.length < minQueryLength) return 0

        val normalizedTarget = SearchTextNormalizer.normalizeForSearch(targetText)

        var bestScore = FuzzyMatcher.score(normalizedQuery, normalizedTarget)
        bestScore = maxOf(bestScore, computeShortQueryPrefixTypoScore(normalizedQuery, normalizedTarget))

        // Check nickname if available
        targetNickname?.let { nickname ->
            val normalizedNickname = SearchTextNormalizer.normalizeForSearch(nickname)
            val nicknameScore =
                FuzzyMatcher.score(
                    normalizedQuery,
                    normalizedNickname,
                )
            val nicknamePrefixTypoScore =
                computeShortQueryPrefixTypoScore(normalizedQuery, normalizedNickname)
            bestScore = maxOf(bestScore, nicknameScore, nicknamePrefixTypoScore)
        }

        return bestScore
    }

    private fun computeShortQueryPrefixTypoScore(
        normalizedQuery: String,
        normalizedTarget: String,
    ): Int {
        val queryLength = normalizedQuery.length
        if (queryLength !in SHORT_QUERY_TYPO_MIN_LENGTH..SHORT_QUERY_TYPO_MAX_LENGTH) return 0

        val tokens =
            normalizedTarget
                .split(WHITESPACE_REGEX)
                .flatMap { it.split(NON_ALPHANUMERIC_REGEX) }
                .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return 0

        for (token in tokens) {
            if (token.length < queryLength) continue
            val tokenPrefix = token.substring(0, queryLength)
            if (
                levenshteinDistance(normalizedQuery, tokenPrefix) <=
                    SHORT_QUERY_PREFIX_MAX_EDIT_DISTANCE
            ) {
                return SHORT_QUERY_PREFIX_TYPO_SCORE
            }
        }

        return 0
    }

    /**
     * Computes acronym matching score for short queries.
     */
    private fun computeAcronymScore(
        query: String,
        targetText: String,
        nickname: String?,
    ): Int {
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

        val separated = SearchTextNormalizer.normalizeForSearch(text).replace(CAMEL_CASE_REGEX, "$1 $2")
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

    private fun levenshteinDistance(
        first: String,
        second: String,
    ): Int {
        if (first == second) return 0
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length

        var shorter = first
        var longer = second
        if (shorter.length > longer.length) {
            shorter = second
            longer = first
        }

        val shorterLength = shorter.length
        var previous = IntArray(shorterLength + 1) { it }
        var current = IntArray(shorterLength + 1)

        for (longIndex in 1..longer.length) {
            current[0] = longIndex
            val longChar = longer[longIndex - 1]

            for (shortIndex in 1..shorterLength) {
                val substitutionCost = if (shorter[shortIndex - 1] == longChar) 0 else 1
                val insertion = current[shortIndex - 1] + 1
                val deletion = previous[shortIndex] + 1
                val substitution = previous[shortIndex - 1] + substitutionCost

                current[shortIndex] = minOf(insertion, deletion, substitution)
            }

            val swap = previous
            previous = current
            current = swap
        }

        return previous[shorterLength]
    }
}
