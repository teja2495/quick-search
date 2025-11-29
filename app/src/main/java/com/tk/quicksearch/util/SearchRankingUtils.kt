package com.tk.quicksearch.util

import java.util.Locale

/**
 * Utility object for calculating search result ranking priorities.
 * 
 * Priority levels (lower is better):
 * 1. Exact match with query
 * 2. Result starts with query
 * 3. Any word in the text starts with query
 * 4. Other matches (contains query)
 */
object SearchRankingUtils {
    
    private const val PRIORITY_EXACT_MATCH = 1
    private const val PRIORITY_STARTS_WITH = 2
    private const val PRIORITY_WORD_STARTS_WITH = 3
    private const val PRIORITY_OTHER = 4
    
    /**
     * Calculates the match priority for a given text and query.
     * Returns a lower number for higher priority matches.
     * 
     * @param text The text to match against
     * @param query The search query
     * @return Priority level (1-4, where 1 is highest priority)
     */
    fun calculateMatchPriority(text: String, query: String): Int {
        if (query.isBlank()) return PRIORITY_OTHER

        val normalizedText = text.lowercase(Locale.getDefault())
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())

        // Support multi-word queries by also matching individual words.
        // Example: "Teja Atch" should still match "Sai Teja Atchala"
        // by treating "atch" as a prefix of the "Atchala" word.
        val queryTokens = normalizedQuery
            .split("\\s+".toRegex())
            .mapNotNull { it.ifBlank { null } }

        // The primary token we consider for "starts with" checks.
        // For multi-word queries, the last token is often the one being completed
        // (e.g. "teja atch" → "atch").
        val primaryToken = queryTokens.lastOrNull() ?: normalizedQuery

        // Priority 1: Exact match
        if (normalizedText == normalizedQuery) {
            return PRIORITY_EXACT_MATCH
        }

        // Priority 2: Starts with query
        if (normalizedText.startsWith(normalizedQuery) || normalizedText.startsWith(primaryToken)) {
            return PRIORITY_STARTS_WITH
        }

        // Priority 3: Any word (after splitting on whitespace) starts with query
        val textWords = normalizedText.split("\\s+".toRegex())

        val anyWordStartsWithQuery =
            textWords.any { it.startsWith(normalizedQuery) || it.startsWith(primaryToken) }

        // Additionally, for multi-word queries, allow matching if *any* query token
        // is a prefix of a word in the text.
        val anyWordStartsWithAnyToken =
            if (queryTokens.size > 1) {
                textWords.any { word ->
                    queryTokens.any { token -> word.startsWith(token) }
                }
            } else {
                false
            }

        if (anyWordStartsWithQuery || anyWordStartsWithAnyToken) {
            return PRIORITY_WORD_STARTS_WITH
        }

        // Priority 4: Other matches (contains query)
        return PRIORITY_OTHER
    }
    
    /**
     * Gets the best match priority from multiple text fields.
     * Returns the highest priority (lowest number) among all fields.
     * 
     * @param query The search query
     * @param textFields Variable number of text fields to check
     * @return The best (lowest) priority found
     */
    fun getBestMatchPriority(query: String, vararg textFields: String): Int {
        return textFields.minOfOrNull { calculateMatchPriority(it, query) } ?: PRIORITY_OTHER
    }

    fun isOtherMatch(priority: Int): Boolean = priority == PRIORITY_OTHER
}

