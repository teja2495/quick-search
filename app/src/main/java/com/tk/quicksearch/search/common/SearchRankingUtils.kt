package com.tk.quicksearch.search.utils

import java.util.Locale

/**
 * Utility object for calculating search result ranking priorities.
 *
 * Priority levels (lower is better):
 * 1. Result starts with query
 * 2. Any word in the text starts with query
 * 3. Result contains query
 * 4. No match
 */
object SearchRankingUtils {
    private const val PRIORITY_STARTS_WITH = 1
    private const val PRIORITY_WORD_STARTS_WITH = 2
    private const val PRIORITY_CONTAINS = 3
    private const val PRIORITY_NO_MATCH = 4

    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Calculates the match priority for a given text and query.
     * Returns a lower number for higher priority matches.
     *
     * @param text The text to match against
     * @param query The search query
     * @return Priority level (1-4, where 1 is highest priority)
     */
    fun calculateMatchPriority(
        text: String,
        query: String,
    ): Int {
        if (query.isBlank()) return PRIORITY_NO_MATCH

        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        // Parse query tokens once for reuse
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        return calculateMatchPriority(text, normalizedQuery, queryTokens)
    }

    /**
     * Optimized version of calculateMatchPriority that accepts pre-calculated query tokens.
     * Use this in tight loops to avoid re-normalizing the query.
     */
    fun calculateMatchPriority(
        text: String,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Int {
        if (normalizedQuery.isBlank()) return PRIORITY_NO_MATCH

        val normalizedText = text.lowercase(Locale.getDefault())
        val isMultiWord = queryTokens.size > 1
        val primaryToken = queryTokens.lastOrNull() ?: normalizedQuery

        // For multi-word queries, only match if the text starts with the full query
        if (isMultiWord) {
            if (normalizedText.startsWith(normalizedQuery)) {
                return PRIORITY_STARTS_WITH
            }
            return PRIORITY_NO_MATCH
        }

        // Priority 1: Starts with query or primary token (single-word only)
        if (normalizedText.startsWith(normalizedQuery) || normalizedText.startsWith(primaryToken)) {
            return PRIORITY_STARTS_WITH
        }

        // Priority 2: Any word starts with query/token (single-word only)
        val textWords = normalizedText.split(WHITESPACE_REGEX)
        if (hasWordStartingWithQuery(textWords, normalizedQuery, primaryToken, queryTokens)) {
            return PRIORITY_WORD_STARTS_WITH
        }

        // Priority 3: Contains query/token anywhere (single-word only)
        if (hasContainingMatch(normalizedText, normalizedQuery, queryTokens)) {
            return PRIORITY_CONTAINS
        }

        // Priority 4: No match
        return PRIORITY_NO_MATCH
    }

    /**
     * Checks if any word in the text starts with the query, primary token, or any query token.
     *
     * @param textWords Words from the normalized text
     * @param normalizedQuery The full normalized query
     * @param primaryToken The last token of multi-word queries (or full query for single-word)
     * @param queryTokens All tokens from the query
     * @return true if any word matches
     */
    private fun hasWordStartingWithQuery(
        textWords: List<String>,
        normalizedQuery: String,
        primaryToken: String,
        queryTokens: List<String>,
    ): Boolean =
        textWords.any { word ->
            word.startsWith(normalizedQuery) ||
                word.startsWith(primaryToken) ||
                (queryTokens.size > 1 && queryTokens.any { token -> word.startsWith(token) })
        }

    private fun hasContainingMatch(
        normalizedText: String,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Boolean =
        normalizedText.contains(normalizedQuery) ||
            (
                queryTokens.size > 1 &&
                    queryTokens.all { token ->
                        token.isNotBlank() && normalizedText.contains(token)
                    }
            )

    /**
     * Calculates match priority while giving an optional nickname the highest boost.
     * Nickname matches get priority 0 (higher than any text match).
     */

    /**
     * Calculates match priority while giving an optional nickname the highest boost.
     * Nickname matches get priority 0 (higher than any text match).
     */
    fun calculateMatchPriorityWithNickname(
        primaryText: String,
        nickname: String?,
        query: String,
    ): Int {
        if (query.isBlank()) return PRIORITY_NO_MATCH

        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        // Parse query tokens once for reuse
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

        return calculateMatchPriorityWithNickname(primaryText, nickname, normalizedQuery, queryTokens)
    }

    /**
     * Optimized version of calculateMatchPriorityWithNickname.
     */
    fun calculateMatchPriorityWithNickname(
        primaryText: String,
        nickname: String?,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Int {
        if (normalizedQuery.isBlank()) return PRIORITY_NO_MATCH

        val normalizedNickname = nickname?.lowercase(Locale.getDefault())

        if (normalizedNickname?.contains(normalizedQuery) == true) {
            return 0
        }

        return calculateMatchPriority(primaryText, normalizedQuery, queryTokens)
    }

    /**
     * Gets the best match priority from multiple text fields.
     * Returns the highest priority (lowest number) among all fields.
     *
     * @param query The search query
     * @param textFields Variable number of text fields to check
     * @return The best (lowest) priority found
     */
    fun getBestMatchPriority(
        query: String,
        vararg textFields: String,
    ): Int = textFields.minOfOrNull { calculateMatchPriority(it, query) } ?: PRIORITY_NO_MATCH

    /**
     * Checks if the given priority represents a non-match (lowest priority).
     *
     * @param priority The priority to check
     * @return true if priority is PRIORITY_NO_MATCH
     */
    fun isOtherMatch(priority: Int): Boolean = priority == PRIORITY_NO_MATCH
}
