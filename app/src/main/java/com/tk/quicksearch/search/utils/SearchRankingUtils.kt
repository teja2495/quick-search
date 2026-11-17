package com.tk.quicksearch.search.utils

/**
 * Utility object for calculating search result ranking priorities.
 *
 * Priority levels (lower is better). At each match strength the result's own name ranks
 * above a nickname match of the same strength:
 * 1. Name starts with query
 * 2. Nickname starts with (or exactly matches) query
 * 3. Any word in the name starts with query
 * 4. Any word in the nickname starts with query
 * 5. Name contains query
 * 6. Nickname contains query
 * 7. No match
 */
object SearchRankingUtils {
    private const val PRIORITY_STARTS_WITH = 1
    private const val PRIORITY_NICKNAME_STARTS_WITH = 2
    private const val PRIORITY_WORD_STARTS_WITH = 3
    const val PRIORITY_CONTAINS = 5
    const val PRIORITY_NO_MATCH = 7

    /** Ranks fuzzy matches after every direct match. */
    const val PRIORITY_FUZZY_MATCH = PRIORITY_NO_MATCH + 1

    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Calculates the match priority for a given text and query.
     * Returns a lower number for higher priority matches.
     *
     * @param text The text to match against
     * @param query The search query
     * @return Priority level (1, 3, 5 for matches, 7 for no match; lower is better)
     */
    fun calculateMatchPriority(
        text: String,
        query: String,
    ): Int {
        if (query.isBlank()) return PRIORITY_NO_MATCH

        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        // Parse query tokens once for reuse
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val compactQuery = SearchTextNormalizer.compactForSearch(normalizedQuery)

        return calculateMatchPriority(text, normalizedQuery, queryTokens, compactQuery)
    }

    /**
     * Optimized version of calculateMatchPriority that accepts pre-calculated query tokens.
     * Use this in tight loops to avoid re-normalizing the query.
     */
    fun calculateMatchPriority(
        text: String,
        normalizedQuery: String,
        queryTokens: List<String>,
        compactQuery: String = SearchTextNormalizer.compactForSearch(normalizedQuery),
    ): Int {
        if (normalizedQuery.isBlank()) return PRIORITY_NO_MATCH

        val normalizedText = SearchTextNormalizer.normalizeForSearch(text)
        return calculateMatchPriority(
            normalizedText = normalizedText,
            compactText = SearchTextNormalizer.compactForSearch(normalizedText),
            textWords = normalizedText.split(WHITESPACE_REGEX),
            normalizedQuery = normalizedQuery,
            queryTokens = queryTokens,
            compactQuery = compactQuery,
        )
    }

    fun calculateMatchPriority(
        text: PreparedSearchText,
        query: SearchQueryContext,
    ): Int =
        calculateMatchPriority(text, query.normalizedQuery, query.tokens, query.compactQuery)

    fun calculateMatchPriority(
        text: PreparedSearchText,
        normalizedQuery: String,
        queryTokens: List<String>,
        compactQuery: String,
    ): Int {
        if (normalizedQuery.isBlank()) return PRIORITY_NO_MATCH

        return calculateMatchPriority(
            normalizedText = text.normalized,
            compactText = text.compact,
            textWords = text.words,
            normalizedQuery = normalizedQuery,
            queryTokens = queryTokens,
            compactQuery = compactQuery,
        )
    }

    private fun calculateMatchPriority(
        normalizedText: String,
        compactText: String,
        textWords: List<String>,
        normalizedQuery: String,
        queryTokens: List<String>,
        compactQuery: String,
    ): Int {
        val isMultiWord = queryTokens.size > 1
        val primaryToken = queryTokens.lastOrNull() ?: normalizedQuery

        // Multi-word matching:
        // 1) exact phrase at start, then 2) all tokens match word starts (order-agnostic),
        // then 3) all tokens are present anywhere in the text.
        if (isMultiWord) {
            if (normalizedText.startsWith(normalizedQuery)) {
                return PRIORITY_STARTS_WITH
            }
            if (allTokensMatchWordStarts(textWords, queryTokens)) {
                return PRIORITY_WORD_STARTS_WITH
            }
            if (allTokensContained(normalizedText, queryTokens)) {
                return PRIORITY_CONTAINS
            }
            if (hasCompactContainingMatch(compactText, compactQuery)) {
                return PRIORITY_CONTAINS
            }
            return PRIORITY_NO_MATCH
        }

        // Starts with query or primary token (single-word only)
        if (normalizedText.startsWith(normalizedQuery) || normalizedText.startsWith(primaryToken)) {
            return PRIORITY_STARTS_WITH
        }

        // Any word starts with query/token (single-word only)
        if (hasWordStartingWithQuery(textWords, normalizedQuery, primaryToken, queryTokens)) {
            return PRIORITY_WORD_STARTS_WITH
        }

        // Contains query/token anywhere (single-word only)
        if (hasContainingMatch(normalizedText, normalizedQuery, queryTokens)) {
            return PRIORITY_CONTAINS
        }

        if (hasCompactContainingMatch(compactText, compactQuery)) {
            return PRIORITY_CONTAINS
        }

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
                    allTokensContained(normalizedText, queryTokens)
            )

    private fun allTokensMatchWordStarts(
        textWords: List<String>,
        queryTokens: List<String>,
    ): Boolean =
        queryTokens.all { token ->
            token.isNotBlank() && textWords.any { word -> word.startsWith(token) }
        }

    private fun allTokensContained(
        normalizedText: String,
        queryTokens: List<String>,
    ): Boolean =
        queryTokens.all { token ->
            token.isNotBlank() && normalizedText.contains(token)
        }

    private fun hasCompactContainingMatch(
        compactText: String,
        compactQuery: String,
    ): Boolean =
        compactQuery.isNotBlank() &&
            compactText.contains(compactQuery)

    /**
     * Calculates match priority with optional nickname support.
     * Nickname is treated as an additional searchable name using the same priority rules.
     * A nickname may hold several comma-separated aliases; the best-matching alias wins.
     */
    fun calculateMatchPriorityWithNickname(
        primaryText: String,
        nickname: String?,
        query: String,
    ): Int {
        if (query.isBlank()) return PRIORITY_NO_MATCH
        return calculateMatchPriorityWithNickname(primaryText, nickname, prepareRankingQuery(query))
    }

    /** A raw query normalized once, for scoring many candidates against it. */
    class RankingQuery internal constructor(
        val normalizedQuery: String,
        val queryTokens: List<String>,
        val compactQuery: String,
    )

    /** Same query preparation as the raw-query overloads, done once instead of per candidate. */
    fun prepareRankingQuery(query: String): RankingQuery {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        return RankingQuery(
            normalizedQuery = normalizedQuery,
            queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() },
            compactQuery = SearchTextNormalizer.compactForSearch(normalizedQuery),
        )
    }

    fun calculateMatchPriorityWithNickname(
        primaryText: String,
        nickname: String?,
        query: RankingQuery,
    ): Int =
        calculateMatchPriorityWithNickname(
            primaryText,
            nickname,
            query.normalizedQuery,
            query.queryTokens,
            query.compactQuery,
        )

    /**
     * Optimized version of calculateMatchPriorityWithNickname.
     */
    fun calculateMatchPriorityWithNickname(
        primaryText: String,
        nickname: String?,
        normalizedQuery: String,
        queryTokens: List<String>,
        compactQuery: String = SearchTextNormalizer.compactForSearch(normalizedQuery),
    ): Int {
        if (normalizedQuery.isBlank()) return PRIORITY_NO_MATCH

        val primaryPriority = calculateMatchPriority(primaryText, normalizedQuery, queryTokens, compactQuery)
        val nicknamePriority =
            bestNicknamePriority(nickname) { alias ->
                val preparedAlias = SearchTextNormalizer.prepareForSearch(alias)
                if (isExactNicknameMatch(preparedAlias, normalizedQuery, compactQuery)) {
                    PRIORITY_NICKNAME_STARTS_WITH
                } else {
                    toNicknamePriority(
                        calculateMatchPriority(preparedAlias, normalizedQuery, queryTokens, compactQuery),
                    )
                }
            }
        return minOf(primaryPriority, nicknamePriority)
    }

    fun calculateMatchPriorityWithNickname(
        primaryText: PreparedSearchText,
        nickname: PreparedSearchText?,
        query: SearchQueryContext,
    ): Int {
        if (query.normalizedQuery.isBlank()) return PRIORITY_NO_MATCH

        val primaryPriority = calculateMatchPriority(primaryText, query)
        val nicknamePriority =
            nickname?.let {
                if (isExactNicknameMatch(it, query.normalizedQuery, query.compactQuery)) {
                    PRIORITY_NICKNAME_STARTS_WITH
                } else {
                    toNicknamePriority(calculateMatchPriority(it, query))
                }
            } ?: PRIORITY_NO_MATCH
        return minOf(primaryPriority, nicknamePriority)
    }

    /** Places a nickname match one step below a name match of the same strength. */
    private fun toNicknamePriority(namePriority: Int): Int =
        if (namePriority == PRIORITY_NO_MATCH) PRIORITY_NO_MATCH else namePriority + 1

    private fun isExactNicknameMatch(
        nickname: PreparedSearchText,
        normalizedQuery: String,
        compactQuery: String,
    ): Boolean =
        nickname.normalized == normalizedQuery ||
            (compactQuery.isNotBlank() && nickname.compact == compactQuery)

    /**
     * Checks if the given priority represents a non-match (lowest priority).
     *
     * @param priority The priority to check
     * @return true if priority is PRIORITY_NO_MATCH
     */
    fun isOtherMatch(priority: Int): Boolean = priority == PRIORITY_NO_MATCH

    /**
     * Scores every comma-separated alias in [nickname] and keeps the best one. Falls back to
     * [PRIORITY_NO_MATCH] when there is no nickname or it holds nothing usable.
     */
    private inline fun bestNicknamePriority(
        nickname: String?,
        score: (String) -> Int,
    ): Int {
        if (nickname.isNullOrBlank()) return PRIORITY_NO_MATCH
        if (!NicknameUtils.hasMultiple(nickname)) return score(nickname)
        return NicknameUtils.split(nickname).minOfOrNull(score) ?: PRIORITY_NO_MATCH
    }
}
