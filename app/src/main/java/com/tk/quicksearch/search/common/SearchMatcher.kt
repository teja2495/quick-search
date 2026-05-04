package com.tk.quicksearch.search.utils

private val QUERY_TOKENS_REGEX = "\\s+".toRegex()

data class SearchQueryContext(
    val normalizedQuery: String,
    val tokens: List<String>,
    val compactQuery: String,
) {
    companion object {
        fun fromRawQuery(query: String): SearchQueryContext {
            val normalized = SearchTextNormalizer.normalizeForSearch(query.trim())
            return fromNormalizedQuery(normalized)
        }

        fun fromNormalizedQuery(normalizedQuery: String): SearchQueryContext =
            SearchQueryContext(
                normalizedQuery = normalizedQuery,
                tokens = normalizedQuery.split(QUERY_TOKENS_REGEX).filter { it.isNotBlank() },
                compactQuery = SearchTextNormalizer.compactForSearch(normalizedQuery),
            )
    }
}

interface SearchMatcher {
    fun match(
        primaryText: String,
        query: SearchQueryContext,
        nickname: String? = null,
    ): Int

    fun matchAny(
        query: SearchQueryContext,
        vararg textFields: String,
    ): Int

    fun isMatch(priority: Int): Boolean
}

object DefaultSearchMatcher : SearchMatcher {
    override fun match(
        primaryText: String,
        query: SearchQueryContext,
        nickname: String?,
    ): Int =
        SearchRankingUtils.calculateMatchPriorityWithNickname(
            primaryText = primaryText,
            nickname = nickname,
            normalizedQuery = query.normalizedQuery,
            queryTokens = query.tokens,
            compactQuery = query.compactQuery,
        )

    override fun matchAny(
        query: SearchQueryContext,
        vararg textFields: String,
    ): Int =
        textFields.minOfOrNull { field ->
            SearchRankingUtils.calculateMatchPriority(
                text = field,
                normalizedQuery = query.normalizedQuery,
                queryTokens = query.tokens,
                compactQuery = query.compactQuery,
            )
        } ?: SearchRankingUtils.calculateMatchPriority(
            "",
            query.normalizedQuery,
            query.tokens,
            query.compactQuery,
        )

    override fun isMatch(priority: Int): Boolean = !SearchRankingUtils.isOtherMatch(priority)
}
