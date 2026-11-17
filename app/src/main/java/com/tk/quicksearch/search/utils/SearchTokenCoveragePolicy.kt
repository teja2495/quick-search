package com.tk.quicksearch.search.utils

object SearchTokenCoveragePolicy {
    fun areAllTokensCovered(
        query: SearchQueryContext,
        primaryText: String,
        supportingText: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        if (query.tokens.size <= 1) return true

        val normalizedPrimary = SearchTextNormalizer.normalizeForSearch(primaryText)
        val normalizedSupporting =
            supportingText
                ?.takeIf { it.isNotBlank() }
                ?.let(SearchTextNormalizer::normalizeForSearch)

        return areAllTokensCoveredNormalized(
            query = query,
            normalizedPrimary = normalizedPrimary,
            normalizedSupporting = normalizedSupporting,
            fuzzyMinScore = fuzzyMinScore,
            fuzzyMaxEditDistance = fuzzyMaxEditDistance,
        )
    }

    fun areAllTokensCovered(
        query: SearchQueryContext,
        primaryText: PreparedSearchText,
        supportingText: PreparedSearchText?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        if (query.tokens.size <= 1) return true

        return areAllTokensCoveredNormalized(
            query = query,
            normalizedPrimary = primaryText.normalized,
            normalizedSupporting = supportingText?.normalized,
            fuzzyMinScore = fuzzyMinScore,
            fuzzyMaxEditDistance = fuzzyMaxEditDistance,
        )
    }

    /** Same check over text already passed through [SearchTextNormalizer.normalizeForSearch]. */
    fun areAllTokensCoveredNormalized(
        query: SearchQueryContext,
        normalizedPrimary: String,
        normalizedSupporting: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        return query.tokens.all { token ->
            normalizedPrimary.contains(token) ||
                (!normalizedSupporting.isNullOrBlank() && normalizedSupporting.contains(token)) ||
                FuzzyMatcher.score(
                    query = token,
                    primaryTarget = normalizedPrimary,
                    secondaryTarget = normalizedSupporting,
                    maxEditDistance = fuzzyMaxEditDistance,
                ) >= fuzzyMinScore
        }
    }
}
