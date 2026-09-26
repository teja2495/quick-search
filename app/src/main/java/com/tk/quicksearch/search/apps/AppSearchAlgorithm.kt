package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.fuzzy.FuzzySearchPerformanceLogger
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import java.util.Locale

data class PreparedAppSearchData(
    val initials: List<String>,
) {
    companion object {
        fun from(app: AppInfo): PreparedAppSearchData =
            PreparedAppSearchData(
                initials = AppSearchInitials.initialsFor(app),
            )
    }
}

object AppSearchAlgorithm {
    fun findMatches(
        query: String,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        secondaryRankingSignal: SecondaryRankingSignal,
        matcher: SearchMatcher = DefaultSearchMatcher,
        preparedAppData: Map<String, PreparedAppSearchData> = emptyMap(),
    ): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return findMatches(
            queryContext = SearchQueryContext.fromRawQuery(query),
            source = source,
            limit = limit,
            fuzzySearchStrategy = fuzzySearchStrategy,
            appNicknames = appNicknames,
            secondaryRankingSignal = secondaryRankingSignal,
            matcher = matcher,
            preparedAppData = preparedAppData,
        )
    }

    fun findMatches(
        queryContext: SearchQueryContext,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        secondaryRankingSignal: SecondaryRankingSignal,
        matcher: SearchMatcher = DefaultSearchMatcher,
        preparedAppData: Map<String, PreparedAppSearchData> = emptyMap(),
    ): List<AppInfo> {
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        val preparedFuzzyQuery = fuzzySearchStrategy.prepareQuery(queryContext)
        val preparedTokenQueries =
            if (queryContext.tokens.size > 1) {
                queryContext.tokens.associateWith(fuzzySearchStrategy::prepareNormalizedToken)
            } else {
                emptyMap()
            }
        val canUseFuzzySearch = preparedFuzzyQuery.policy.enabled
        val fuzzyCandidateLimit =
            if (canUseFuzzySearch) {
                preparedFuzzyQuery.policy.candidateLimit
            } else {
                0
            }
        var fuzzyCandidatesScored = 0

        val searchBlock = {
            source
                .asSequence()
                .mapNotNull { app ->
                    calculateAppMatch(
                        app = app,
                        queryContext = queryContext,
                        fuzzySearchStrategy = fuzzySearchStrategy,
                        preparedFuzzyQuery = preparedFuzzyQuery,
                        preparedTokenQueries = preparedTokenQueries,
                        appNicknames = appNicknames,
                        matcher = matcher,
                        preparedAppData =
                            preparedAppData[app.launchCountKey()] ?: PreparedAppSearchData.from(app),
                        canScoreFuzzyCandidate = {
                            if (fuzzyCandidatesScored >= fuzzyCandidateLimit) {
                                false
                            } else {
                                fuzzyCandidatesScored += 1
                                true
                            }
                        },
                    )
                }.sortedWith(createAppComparator(secondaryRankingSignal))
                .map { it.app }
                .take(limit)
                .toList()
        }

        if (!canUseFuzzySearch) return searchBlock()

        return FuzzySearchPerformanceLogger.measure(
            section = SearchSection.APPS,
            query = queryContext.normalizedQuery,
            candidateCount = minOf(source.size, fuzzyCandidateLimit),
            block = searchBlock,
        )
    }

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean,
        val alphabeticalKey: String,
    )

    private fun calculateAppMatch(
        app: AppInfo,
        queryContext: SearchQueryContext,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        preparedFuzzyQuery: PreparedAppFuzzyQuery,
        preparedTokenQueries: Map<String, PreparedAppFuzzyQuery>,
        appNicknames: Map<String, String>,
        matcher: SearchMatcher,
        preparedAppData: PreparedAppSearchData,
        canScoreFuzzyCandidate: () -> Boolean,
    ): AppMatch? {
        // Archived apps keep their nickname, but it only matches again once they're restored.
        val nickname = if (app.isArchived) null else appNicknames[app.packageName]
        val initials = preparedAppData.initials
        val priority =
            AppSearchPolicy.matchPriority(
                appName = app.appName,
                searchAliases = app.searchAliases,
                nickname = nickname,
                query = queryContext,
                initials = initials,
                matcher = matcher,
            )
        if (AppSearchPolicy.hasMatch(priority)) {
            if (
                !AppSearchPolicy.areAllQueryTokensCovered(
                    queryContext,
                    preparedTokenQueries,
                    app.appName,
                    app.searchAliases,
                    nickname,
                    initials,
                    fuzzySearchStrategy,
                )
            ) {
                return null
            }
            return AppMatch(app, priority, 0, false, app.appName.lowercase(Locale.getDefault()))
        }

        if (
            !fuzzySearchStrategy.isTypoEligibleCandidate(
                preparedQuery = preparedFuzzyQuery,
                appName = app.appName,
                searchAliases = app.searchAliases,
                nickname = nickname,
                initials = initials,
            )
        ) {
            return null
        }

        if (!canScoreFuzzyCandidate()) return null

        val match =
            fuzzySearchStrategy.scoreEligibleCandidate(
                preparedQuery = preparedFuzzyQuery,
                app = app,
                searchAliases = app.searchAliases,
                nickname = nickname,
                initials = initials,
            )

        return match?.let {
            if (
                !AppSearchPolicy.areAllQueryTokensCovered(
                    queryContext,
                    preparedTokenQueries,
                    app.appName,
                    app.searchAliases,
                    nickname,
                    initials,
                    fuzzySearchStrategy,
                )
            ) {
                return null
            }
            AppMatch(app, it.priority, it.score, true, app.appName.lowercase(Locale.getDefault()))
        }
    }

    private fun createAppComparator(secondaryRankingSignal: SecondaryRankingSignal): Comparator<AppMatch> {
        return Comparator { first, second ->
            if (first.isFuzzy != second.isFuzzy) {
                return@Comparator if (first.isFuzzy) 1 else -1
            }

            if (!first.isFuzzy) {
                val priorityCompare = first.priority.compareTo(second.priority)
                if (priorityCompare != 0) {
                    return@Comparator priorityCompare
                }
                return@Comparator compareBySecondarySignalOrName(first, second, secondaryRankingSignal)
            }

            val fuzzyCompare = second.fuzzyScore.compareTo(first.fuzzyScore)
            if (fuzzyCompare != 0) {
                return@Comparator fuzzyCompare
            }
            compareBySecondarySignalOrName(first, second, secondaryRankingSignal)
        }
    }

    private fun compareBySecondarySignalOrName(
        first: AppMatch,
        second: AppMatch,
        secondaryRankingSignal: SecondaryRankingSignal,
    ): Int {
        val secondaryCompare =
            when (secondaryRankingSignal) {
                SecondaryRankingSignal.RECENCY -> second.app.lastUsedTime.compareTo(first.app.lastUsedTime)
                SecondaryRankingSignal.MOST_OPENED -> second.app.launchCount.compareTo(first.app.launchCount)
                SecondaryRankingSignal.NONE -> 0
            }
        return secondaryCompare.takeIf { it != 0 }
            ?: first.alphabeticalKey.compareTo(second.alphabeticalKey)
    }
}
