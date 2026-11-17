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
    val abbreviationTargets: List<String> = emptyList(),
) {
    companion object {
        fun from(app: AppInfo): PreparedAppSearchData =
            PreparedAppSearchData(
                initials = AppSearchInitials.initialsFor(app),
                abbreviationTargets = AppNameAbbreviation.targetsFor(app),
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
        ensureActive: () -> Unit = {},
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
        val searchBlock = {
            val matches = ArrayList<AppMatch>()
            // Apps with no exact match, in source order, kept for the fuzzy pass.
            val fuzzyPending = if (canUseFuzzySearch) ArrayList<FuzzyCandidate>() else null
            for (app in source) {
                // A newer keystroke cancels this search; stop instead of finishing the scan.
                ensureActive()
                val appKey = app.launchCountKey()
                // Archived apps keep their nickname, but it only matches again once they're restored.
                val nickname = if (app.isArchived) null else appNicknames[appKey]
                val appData = preparedAppData[appKey] ?: PreparedAppSearchData.from(app)
                val priority =
                    AppSearchPolicy.matchPriority(
                        appName = app.appName,
                        searchAliases = app.searchAliases,
                        nickname = nickname,
                        query = queryContext,
                        initials = appData.initials,
                        abbreviationTargets = appData.abbreviationTargets,
                        matcher = matcher,
                    )
                if (AppSearchPolicy.hasMatch(priority)) {
                    if (
                        AppSearchPolicy.areAllQueryTokensCovered(
                            queryContext,
                            preparedTokenQueries,
                            app.appName,
                            app.searchAliases,
                            nickname,
                            appData.initials,
                            fuzzySearchStrategy,
                        )
                    ) {
                        matches.add(AppMatch(app, priority, 0, false, app.appName.lowercase(Locale.getDefault())))
                    }
                } else {
                    fuzzyPending?.add(FuzzyCandidate(app, appKey, nickname, appData.initials))
                }
            }

            // Exact matches always sort ahead of fuzzy ones, so once they fill the limit no fuzzy
            // match can be shown and the typo checks below would be wasted work.
            if (fuzzyPending != null && matches.size < limit) {
                var fuzzyCandidatesScored = 0
                for (candidate in fuzzyPending) {
                    ensureActive()
                    // Once the fuzzy budget is spent no further candidate can score.
                    if (fuzzyCandidatesScored >= fuzzyCandidateLimit) break
                    val match =
                        fuzzySearchStrategy.scoreTypoCandidate(
                            preparedQuery = preparedFuzzyQuery,
                            app = candidate.app,
                            appKey = candidate.appKey,
                            nickname = candidate.nickname,
                            initials = candidate.initials,
                            canScore = {
                                if (fuzzyCandidatesScored >= fuzzyCandidateLimit) {
                                    false
                                } else {
                                    fuzzyCandidatesScored += 1
                                    true
                                }
                            },
                        ) ?: continue
                    if (
                        AppSearchPolicy.areAllQueryTokensCovered(
                            queryContext,
                            preparedTokenQueries,
                            candidate.app.appName,
                            candidate.app.searchAliases,
                            candidate.nickname,
                            candidate.initials,
                            fuzzySearchStrategy,
                        )
                    ) {
                        matches.add(
                            AppMatch(
                                candidate.app,
                                match.priority,
                                match.score,
                                true,
                                candidate.app.appName.lowercase(Locale.getDefault()),
                            ),
                        )
                    }
                }
            }

            // Stable sort: exact and fuzzy matches never compare equal, so collecting them in two
            // passes yields the same order as the former single interleaved pass.
            matches.sortWith(createAppComparator(secondaryRankingSignal))
            matches.asSequence().map { it.app }.take(limit).toList()
        }

        if (!canUseFuzzySearch) return searchBlock()

        return FuzzySearchPerformanceLogger.measure(
            section = SearchSection.APPS,
            query = queryContext.normalizedQuery,
            candidateCount = minOf(source.size, fuzzyCandidateLimit),
            block = searchBlock,
        )
    }

    private class FuzzyCandidate(
        val app: AppInfo,
        val appKey: String,
        val nickname: String?,
        val initials: List<String>,
    )

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean,
        val alphabeticalKey: String,
    )

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
