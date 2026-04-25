package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsSearchAlgorithmTest {

    @Test
    fun deterministicMatchesStayAheadOfFuzzyOnlyMatches() {
        val fuzzyOnly =
            setting(
                id = "fuzzy",
                title = "Serch results",
            )
        val exact =
            setting(
                id = "exact",
                title = "Search",
            )

        val results =
            AppSettingsSearchAlgorithm.search(
                fullList = listOf(fuzzyOnly, exact),
                queryContext = SearchQueryContext.fromRawQuery("search"),
                recentSettingScores = emptyMap(),
                resultLimit = 10,
                enableFuzzyMatching = true,
                isLowRamDevice = false,
            )

        assertEquals(2, results.size)
        assertEquals("Search", results[0].title)
        assertEquals("Serch results", results[1].title)
    }

    @Test
    fun veryShortQueriesDoNotAddFuzzyOnlyNoise() {
        val noMatch =
            setting(
                id = "1",
                title = "Quantum toggle",
            )

        val results =
            AppSettingsSearchAlgorithm.search(
                fullList = listOf(noMatch),
                queryContext = SearchQueryContext.fromRawQuery("qq"),
                recentSettingScores = emptyMap(),
                resultLimit = 10,
                enableFuzzyMatching = true,
                isLowRamDevice = false,
            )

        assertTrue(results.isEmpty())
    }

    @Test
    fun fuzzyMatchesAreCappedToResultLimit() {
        val exact =
            setting(
                id = "e",
                title = "Search",
            )
        val fuzzyRows =
            (1..30).map { i ->
                setting(
                    id = "f$i",
                    title = "Serch option $i",
                )
            }

        val results =
            AppSettingsSearchAlgorithm.search(
                fullList = listOf(exact) + fuzzyRows,
                queryContext = SearchQueryContext.fromRawQuery("search"),
                recentSettingScores = emptyMap(),
                resultLimit = 5,
                enableFuzzyMatching = true,
                isLowRamDevice = false,
            )

        assertEquals(5, results.size)
        assertEquals("Search", results[0].title)
    }

    private fun setting(
        id: String,
        title: String,
    ): AppSettingResult =
        AppSettingResult(
            id = id,
            title = title,
            description = null,
            keywords = emptyList(),
            action = AppSettingResultAction.NAVIGATE,
            destination = AppSettingsDestination.SEARCH_RESULTS,
            toggleKey = null,
        )
}
