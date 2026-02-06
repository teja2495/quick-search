package com.tk.quicksearch.search.fuzzy

/**
 * Strategy interface for fuzzy search implementations.
 * Allows different search types (apps, contacts, files, etc.) to implement fuzzy search.
 */
interface FuzzySearchStrategy<T> {
    /**
     * Configuration for this fuzzy search strategy.
     */
    val config: FuzzySearchConfig

    /**
     * Finds fuzzy matches for the given query among the candidates.
     *
     * @param query The search query
     * @param candidates List of items to search through
     * @return List of matches with their scores, sorted by relevance
     */
    fun findMatches(
        query: String,
        candidates: List<T>,
    ): List<Match<T>>

    /**
     * Data class representing a fuzzy search match.
     */
    data class Match<T>(
        val item: T,
        val score: Int,
        val priority: Int,
        val isFuzzyMatch: Boolean = true,
    )
}

/**
 * Base implementation providing common fuzzy search logic.
 */
abstract class BaseFuzzySearchStrategy<T> : FuzzySearchStrategy<T> {
    protected val engine = FuzzySearchEngine()

    /**
     * Filters candidates based on fuzzy search configuration.
     */
    protected fun List<T>.filterByFuzzySearch(
        query: String,
        scoreSelector: (T) -> Int,
    ): List<FuzzySearchStrategy.Match<T>> {
        if (query.isBlank()) return emptyList()

        return this
            .mapNotNull { candidate ->
                val score = scoreSelector(candidate)
                if (score >= config.matchThreshold) {
                    FuzzySearchStrategy.Match(
                        item = candidate,
                        score = score,
                        priority = config.priority,
                        isFuzzyMatch = true,
                    )
                } else {
                    null
                }
            }.sortedByDescending { it.score }
    }
}
