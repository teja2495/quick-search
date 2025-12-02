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
    
    private val WHITESPACE_REGEX = "\\s+".toRegex()
    
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

        // Priority 1: Exact match
        if (normalizedText == normalizedQuery) {
            return PRIORITY_EXACT_MATCH
        }

        // Parse query tokens once for reuse
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val primaryToken = queryTokens.lastOrNull() ?: normalizedQuery

        // Priority 2: Starts with query or primary token
        if (normalizedText.startsWith(normalizedQuery) || normalizedText.startsWith(primaryToken)) {
            return PRIORITY_STARTS_WITH
        }

        // Priority 3: Any word starts with query/token
        val textWords = normalizedText.split(WHITESPACE_REGEX)
        if (hasWordStartingWithQuery(textWords, normalizedQuery, primaryToken, queryTokens)) {
            return PRIORITY_WORD_STARTS_WITH
        }

        // Priority 4: Other matches
        return PRIORITY_OTHER
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
        queryTokens: List<String>
    ): Boolean {
        // Check if any word starts with the full query or primary token
        if (textWords.any { word -> word.startsWith(normalizedQuery) || word.startsWith(primaryToken) }) {
            return true
        }
        
        // For multi-word queries, check if any word starts with any query token
        if (queryTokens.size > 1) {
            return textWords.any { word ->
                queryTokens.any { token -> word.startsWith(token) }
            }
        }
        
        return false
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

    /**
     * Checks if the given priority represents an "other" match (lowest priority).
     * 
     * @param priority The priority to check
     * @return true if priority is PRIORITY_OTHER
     */
    fun isOtherMatch(priority: Int): Boolean = priority == PRIORITY_OTHER
}

