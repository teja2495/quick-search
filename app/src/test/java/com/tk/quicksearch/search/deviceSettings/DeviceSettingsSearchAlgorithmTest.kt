package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSettingsSearchAlgorithmTest {

    @Test
    fun deterministicMatchesStayAheadOfFuzzyOnlyMatches() {
        val fuzzyOnly =
            DeviceSetting(
                id = "fuzzy",
                title = "Blutooth",
                description = null,
                keywords = emptyList(),
                action = "android.settings.SETTINGS",
            )
        val exact =
            DeviceSetting(
                id = "exact",
                title = "Bluetooth",
                description = null,
                keywords = emptyList(),
                action = "android.settings.SETTINGS",
            )

        val results =
            DeviceSettingsSearchAlgorithm.search(
                fullList = listOf(fuzzyOnly, exact),
                queryContext = SearchQueryContext.fromRawQuery("bluetooth"),
                excludedIds = emptySet(),
                matchingNicknameIds = emptySet(),
                nicknameCache = emptyMap(),
                recentSettingScores = emptyMap(),
                resultLimit = 10,
                enableFuzzyMatching = true,
                isLowRamDevice = false,
            )

        assertEquals(2, results.size)
        assertEquals("Bluetooth", results[0].title)
        assertEquals("Blutooth", results[1].title)
    }

    @Test
    fun excludedSettingsAreNotConsideredForFuzzyFillIn() {
        val excluded =
            DeviceSetting(
                id = "ex",
                title = "Blutooth",
                description = null,
                keywords = emptyList(),
                action = "android.settings.SETTINGS",
            )

        val results =
            DeviceSettingsSearchAlgorithm.search(
                fullList = listOf(excluded),
                queryContext = SearchQueryContext.fromRawQuery("bluetooth"),
                excludedIds = setOf("ex"),
                matchingNicknameIds = emptySet(),
                nicknameCache = emptyMap(),
                recentSettingScores = emptyMap(),
                resultLimit = 10,
                enableFuzzyMatching = true,
                isLowRamDevice = false,
            )

        assertTrue(results.isEmpty())
    }

    @Test
    fun veryShortQueriesSkipFuzzyFillIn() {
        val noDeterministicMatch =
            DeviceSetting(
                id = "1",
                title = "Quantum battery",
                description = null,
                keywords = emptyList(),
                action = "android.settings.SETTINGS",
            )

        val results =
            DeviceSettingsSearchAlgorithm.search(
                fullList = listOf(noDeterministicMatch),
                queryContext = SearchQueryContext.fromRawQuery("qq"),
                excludedIds = emptySet(),
                matchingNicknameIds = emptySet(),
                nicknameCache = emptyMap(),
                recentSettingScores = emptyMap(),
                resultLimit = 10,
                enableFuzzyMatching = true,
                isLowRamDevice = false,
            )

        assertTrue(results.isEmpty())
    }
}
