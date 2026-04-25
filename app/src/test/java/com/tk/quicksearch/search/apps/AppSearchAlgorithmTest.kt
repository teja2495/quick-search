package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchAlgorithmTest {

    @Test
    fun deterministicMatchesRankBeforeFuzzyOnlyMatches() {
        val exactMatch = app("Settings", "settings", launchCount = 1)
        val fuzzyOnlyMatch = app("Settlings", "settlings", launchCount = 100)

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "settings",
                source = listOf(fuzzyOnlyMatch, exactMatch),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = true,
            )

        assertEquals(listOf(exactMatch, fuzzyOnlyMatch), matches)
    }

    @Test
    fun shortTypoQueriesDoNotReturnNoisyFuzzyMatches() {
        val matches =
            AppSearchAlgorithm.findMatches(
                query = "zz",
                source = listOf(app("Gmail", "gmail")),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun typoQueriesCanFindAppsWhenDeterministicMatchingMisses() {
        val settings = app("Settings", "settings")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "setings",
                source = listOf(settings),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                sortAppsByUsageEnabled = false,
            )

        assertEquals(listOf(settings), matches)
    }

    private fun app(
        appName: String,
        packageSuffix: String,
        launchCount: Int = 0,
    ): AppInfo =
        AppInfo(
            appName = appName,
            packageName = "com.example.$packageSuffix",
            lastUsedTime = 0L,
            totalTimeInForeground = 0L,
            launchCount = launchCount,
            firstInstallTime = 0L,
            isSystemApp = false,
        )
}
