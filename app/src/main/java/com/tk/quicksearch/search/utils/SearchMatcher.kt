package com.tk.quicksearch.search.utils

data class SearchQueryContext(
    val normalizedQuery: String,
    val tokens: List<String>,
    val compactQuery: String,
    val preparedQuery: PreparedSearchText,
) {
    companion object {
        fun fromRawQuery(query: String): SearchQueryContext {
            val normalized = SearchTextNormalizer.normalizeForSearch(query.trim())
            return fromNormalizedQuery(normalized)
        }

        fun fromNormalizedQuery(normalizedQuery: String): SearchQueryContext {
            val preparedQuery = SearchTextNormalizer.prepareNormalizedForSearch(normalizedQuery)
            return SearchQueryContext(
                normalizedQuery = normalizedQuery,
                tokens = preparedQuery.words.filter { it.isNotBlank() },
                compactQuery = preparedQuery.compact,
                preparedQuery = preparedQuery,
            )
        }
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

class CachedSearchMatcher(
    private val textCache: SearchTextCache,
) : SearchMatcher {
    override fun match(
        primaryText: String,
        query: SearchQueryContext,
        nickname: String?,
    ): Int {
        val prepared = textCache.prepare(primaryText)
        // A nickname may hold several comma-separated aliases; score each and keep the best.
        if (nickname == null || !NicknameUtils.hasMultiple(nickname)) {
            return SearchRankingUtils.calculateMatchPriorityWithNickname(
                primaryText = prepared,
                nickname = nickname?.let(textCache::prepare),
                query = query,
            )
        }
        return NicknameUtils
            .split(nickname)
            .minOfOrNull { alias ->
                SearchRankingUtils.calculateMatchPriorityWithNickname(
                    primaryText = prepared,
                    nickname = textCache.prepare(alias),
                    query = query,
                )
            }
            ?: SearchRankingUtils.calculateMatchPriority(prepared, query)
    }

    override fun matchAny(
        query: SearchQueryContext,
        vararg textFields: String,
    ): Int =
        textFields.minOfOrNull { field ->
            SearchRankingUtils.calculateMatchPriority(
                text = textCache.prepare(field),
                query = query,
            )
        } ?: SearchRankingUtils.calculateMatchPriority(
            textCache.prepare(""),
            query,
        )

    override fun isMatch(priority: Int): Boolean = !SearchRankingUtils.isOtherMatch(priority)
}
