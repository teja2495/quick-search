package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.SearchQueryContext
import java.util.Locale

object AppSearchAlgorithm {
    fun findMatches(
        query: String,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        sortAppsByUsageEnabled: Boolean,
    ): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        return findMatches(
            queryContext = SearchQueryContext.fromRawQuery(query),
            source = source,
            limit = limit,
            fuzzySearchStrategy = fuzzySearchStrategy,
            appNicknames = appNicknames,
            sortAppsByUsageEnabled = sortAppsByUsageEnabled,
        )
    }

    fun findMatches(
        queryContext: SearchQueryContext,
        source: List<AppInfo>,
        limit: Int,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
        sortAppsByUsageEnabled: Boolean,
    ): List<AppInfo> {
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        return source
            .asSequence()
            .mapNotNull { app ->
                calculateAppMatch(
                    app = app,
                    queryContext = queryContext,
                    fuzzySearchStrategy = fuzzySearchStrategy,
                    appNicknames = appNicknames,
                )
            }.sortedWith(createAppComparator(sortAppsByUsageEnabled))
            .map { it.app }
            .take(limit)
            .toList()
    }

    private data class AppMatch(
        val app: AppInfo,
        val priority: Int,
        val fuzzyScore: Int,
        val isFuzzy: Boolean,
    )

    private fun calculateAppMatch(
        app: AppInfo,
        queryContext: SearchQueryContext,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
        appNicknames: Map<String, String>,
    ): AppMatch? {
        val nickname = appNicknames[app.packageName]
        val initials = AppSearchInitials.initialsFor(app)
        val priority = AppSearchPolicy.matchPriority(app.appName, nickname, queryContext, initials)
        if (AppSearchPolicy.hasMatch(priority)) {
            if (
                !AppSearchPolicy.areAllQueryTokensCovered(
                    queryContext,
                    app.appName,
                    nickname,
                    initials,
                    fuzzySearchStrategy,
                )
            ) {
                return null
            }
            return AppMatch(app, priority, 0, false)
        }

        val match =
            fuzzySearchStrategy.computeMatch(
                query = queryContext.normalizedQuery,
                app = app,
                nickname = appNicknames[app.packageName],
                initials = initials,
            )

        return match?.let {
            if (
                !AppSearchPolicy.areAllQueryTokensCovered(
                    queryContext,
                    app.appName,
                    nickname,
                    initials,
                    fuzzySearchStrategy,
                )
            ) {
                return null
            }
            AppMatch(app, it.priority, it.score, true)
        }
    }

    private fun createAppComparator(sortAppsByUsageEnabled: Boolean): Comparator<AppMatch> {
        return Comparator { first, second ->
            if (first.isFuzzy != second.isFuzzy) {
                return@Comparator if (first.isFuzzy) 1 else -1
            }

            if (!first.isFuzzy) {
                val priorityCompare = first.priority.compareTo(second.priority)
                if (priorityCompare != 0) {
                    return@Comparator priorityCompare
                }
                return@Comparator compareByUsageOrName(first.app, second.app, sortAppsByUsageEnabled)
            }

            val fuzzyCompare = second.fuzzyScore.compareTo(first.fuzzyScore)
            if (fuzzyCompare != 0) {
                return@Comparator fuzzyCompare
            }
            compareByUsageOrName(first.app, second.app, sortAppsByUsageEnabled)
        }
    }

    private fun compareByUsageOrName(
        first: AppInfo,
        second: AppInfo,
        sortAppsByUsageEnabled: Boolean,
    ): Int =
        if (sortAppsByUsageEnabled) {
            second.launchCount.compareTo(first.launchCount)
        } else {
            first.appName
                .lowercase(Locale.getDefault())
                .compareTo(second.appName.lowercase(Locale.getDefault()))
        }
}
