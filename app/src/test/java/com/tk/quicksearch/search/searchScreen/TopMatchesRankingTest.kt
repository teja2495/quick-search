package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchRankingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TopMatchesRankingTest {
    @Test
    fun otherResultPriorityTracksQueryRelevance() {
        val exactPriority =
            otherSearchItemMatchPriority(OtherSearchItemId.SCREEN_TIME, "screen time")
        val partialPriority =
            otherSearchItemMatchPriority(OtherSearchItemId.SCREEN_TIME, "time")

        org.junit.Assert.assertTrue(exactPriority < partialPriority)
    }

    @Test
    fun secondaryScoreOrdersTopMatchesWithinSameMatchTier() {
        val lowerScore = topMatch("lower", priority = 1, secondaryScore = 2L, index = 0)
        val higherScore = topMatch("higher", priority = 1, secondaryScore = 9L, index = 1)

        assertEquals(listOf(higherScore, lowerScore), rankTopMatches(listOf(lowerScore, higherScore), 10))
    }

    @Test
    fun primaryMatchQualityAlwaysOutranksSecondarySignal() {
        val betterMatch = topMatch("better", priority = 0, secondaryScore = 0L, index = 0)
        val weakerMatch = topMatch("weaker", priority = 1, secondaryScore = Long.MAX_VALUE, index = 1)

        assertEquals(listOf(betterMatch, weakerMatch), rankTopMatches(listOf(weakerMatch, betterMatch), 10))
    }

    @Test
    fun nonePreservesSectionAndSourceOrderingForTies() {
        val laterSection = topMatch("later-section", priority = 1, secondaryScore = 0L, sectionOrder = 2, index = 0)
        val secondInSection = topMatch("second", priority = 1, secondaryScore = 0L, sectionOrder = 1, index = 1)
        val firstInSection = topMatch("first", priority = 1, secondaryScore = 0L, sectionOrder = 1, index = 0)

        assertEquals(
            listOf(firstInSection, secondInSection, laterSection),
            rankTopMatches(listOf(laterSection, secondInSection, firstInSection), 10),
        )
    }

    @Test
    fun topMatchesDeferUpdatesUntilBothLocalSearchBatchesAreSettled() {
        assertEquals(
            true,
            shouldDeferTopMatchesForLocalSearch(
                query = "Gm",
                isAppSearchInProgress = true,
                isSecondarySearchInProgress = false,
            ),
        )
        assertEquals(
            true,
            shouldDeferTopMatchesForLocalSearch(
                query = "Gm",
                isAppSearchInProgress = false,
                isSecondarySearchInProgress = true,
            ),
        )
        assertEquals(
            false,
            shouldDeferTopMatchesForLocalSearch(
                query = "Gm",
                isAppSearchInProgress = false,
                isSecondarySearchInProgress = false,
            ),
        )
    }

    @Test
    fun topMatchesKeepLastSettledResultsUntilTheNewSearchFinishes() {
        val oldMatches = listOf(topMatch("Teams", priority = 0, secondaryScore = 0L, index = 0))
        val partialMatches = listOf(topMatch("Telegram", priority = 0, secondaryScore = 0L, index = 0))
        val newMatches = listOf(topMatch("Tesla", priority = 0, secondaryScore = 0L, index = 0))
        val buffer = SettledSearchResultsBuffer(emptyList<TopMatchItem>())

        assertEquals(oldMatches, buffer.displayedValue("T", oldMatches, isSearchRefreshing = false))
        assertEquals(oldMatches, buffer.displayedValue("Te", partialMatches, isSearchRefreshing = true))
        assertEquals(newMatches, buffer.displayedValue("Te", newMatches, isSearchRefreshing = false))
    }

    @Test
    fun firstSearchDoesNotReuseHomeOrClearedResults() {
        val buffer = SettledSearchResultsBuffer(emptyList<String>())

        assertEquals(emptyList<String>(), buffer.displayedValue("", listOf("Home"), false))
        assertEquals(emptyList<String>(), buffer.displayedValue("T", listOf("Partial"), true))
        assertEquals(listOf("Teams"), buffer.displayedValue("T", listOf("Teams"), false))
    }

    @Test
    fun regularResultsKeepTheWholePreviousSnapshotUntilAllSearchesFinish() {
        val oldResults = listOf("app:Teams", "contact:Teja")
        val appOnlyPartialResults = listOf("app:Telegram")
        val newResults = listOf("app:Telegram", "file:Template")
        val buffer = SettledSearchResultsBuffer(emptyList<String>())

        assertEquals(oldResults, buffer.displayedValue("T", oldResults, false))
        assertEquals(oldResults, buffer.displayedValue("Te", appOnlyPartialResults, true))
        assertEquals(newResults, buffer.displayedValue("Te", newResults, false))
    }

    @Test
    fun unrelatedQueryDoesNotReuseSettledResults() {
        val buffer = SettledSearchResultsBuffer(emptyList<String>())

        assertEquals(listOf("Gmail"), buffer.displayedValue("gm", listOf("Gmail"), false))
        assertEquals(emptyList<String>(), buffer.displayedValue("weather", listOf("Gmail"), true))
    }

    @Test
    fun backspaceKeepsSettledSectionResultsUntilBroaderSearchFinishes() {
        val narrowResults = listOf("Template")
        val broaderResults = listOf("Teams", "Template")
        val buffer = SettledSearchResultsBuffer(emptyList<String>())

        assertEquals(narrowResults, buffer.displayedValue("tem", narrowResults, false))
        assertEquals(narrowResults, buffer.displayedValue("te", emptyList(), true))
        assertEquals(broaderResults, buffer.displayedValue("te", broaderResults, false))
    }

    @Test
    fun topMatchesSettleAtDeadlineAndAdoptFinalRankingOnceSearchCompletes() {
        val first = topMatch("first", priority = 1, secondaryScore = 0L, index = 0)
        val betterLate = topMatch("late", priority = 0, secondaryScore = 0L, index = 0)
        val buffer = StableTopMatchesBuffer()

        assertFalse(
            buffer.displayedValue("te", listOf(first), true, deadlineReached = false, limit = 3)
                .isReady,
        )
        assertEquals(
            listOf(first),
            buffer.displayedValue("te", listOf(first), true, deadlineReached = true, limit = 3)
                .matches,
        )
        assertEquals(
            listOf(first),
            buffer.displayedValue(
                "te",
                listOf(betterLate, first),
                true,
                deadlineReached = true,
                limit = 3,
            ).matches,
        )
        assertEquals(
            listOf(betterLate, first),
            buffer.displayedValue(
                "te",
                listOf(betterLate, first),
                false,
                deadlineReached = true,
                limit = 3,
            ).matches,
        )
    }

    @Test
    fun lateMatchReplacesDeadlineResultWhenTopMatchesAreAlreadyFull() {
        val early = topMatch("early", priority = 1, secondaryScore = 0L, index = 0)
        val betterLate = topMatch("late", priority = 0, secondaryScore = 0L, index = 0)
        val buffer = StableTopMatchesBuffer()

        assertEquals(
            listOf(early),
            buffer.displayedValue("te", listOf(early), true, deadlineReached = true, limit = 1)
                .matches,
        )
        assertEquals(
            listOf(betterLate),
            buffer.displayedValue(
                "te",
                listOf(betterLate),
                false,
                deadlineReached = true,
                limit = 1,
            ).matches,
        )
    }

    @Test
    fun extendingQueryKeepsSettledTopMatchesUntilTheNextDeadline() {
        val oldMatch = topMatch("old", priority = 1, secondaryScore = 0L, index = 0)
        val newMatch = topMatch("new", priority = 0, secondaryScore = 0L, index = 0)
        val buffer = StableTopMatchesBuffer()

        buffer.displayedValue("t", listOf(oldMatch), false, deadlineReached = false, limit = 3)

        assertEquals(
            listOf(oldMatch),
            buffer.displayedValue(
                "te",
                listOf(newMatch),
                true,
                deadlineReached = false,
                limit = 3,
            ).matches,
        )
        assertEquals(
            listOf(newMatch),
            buffer.displayedValue(
                "te",
                listOf(newMatch),
                true,
                deadlineReached = true,
                limit = 3,
            ).matches,
        )
    }

    @Test
    fun backspaceKeepsSettledTopMatchesUntilTheNextDeadline() {
        val narrowMatch = topMatch("narrow", priority = 0, secondaryScore = 0L, index = 0)
        val broaderMatch = topMatch("broader", priority = 0, secondaryScore = 0L, index = 0)
        val buffer = StableTopMatchesBuffer()

        buffer.displayedValue("tem", listOf(narrowMatch), false, deadlineReached = false, limit = 3)

        assertEquals(
            listOf(narrowMatch),
            buffer.displayedValue(
                "te",
                listOf(broaderMatch),
                true,
                deadlineReached = false,
                limit = 3,
            ).matches,
        )
        assertEquals(
            listOf(broaderMatch),
            buffer.displayedValue(
                "te",
                listOf(broaderMatch),
                true,
                deadlineReached = true,
                limit = 3,
            ).matches,
        )
    }

    @Test
    fun refreshingTopMatchesKeepCurrentMatchesAndDropStaleOnes() {
        val currentMatch = topMatch("Gmail", priority = 0, secondaryScore = 0L, index = 0)
        val staleMatch = topMatch("Old shortcut", priority = SearchRankingUtils.PRIORITY_NO_MATCH, secondaryScore = 0L, index = 1)

        assertEquals(
            listOf(currentMatch),
            filterTopMatchesForActiveQuery(
                matches = listOf(currentMatch, staleMatch),
                filterStaleCandidates = true,
            ),
        )
        assertEquals(
            listOf(currentMatch, staleMatch),
            filterTopMatchesForActiveQuery(
                matches = listOf(currentMatch, staleMatch),
                filterStaleCandidates = false,
            ),
        )
    }

    @Test
    fun appInitialsRemainAValidTopMatchWhileResultsRefresh() {
        val googleMaps =
            AppInfo(
                appName = "Google Maps",
                packageName = "com.google.android.apps.maps",
                lastUsedTime = 0L,
                totalTimeInForeground = 0L,
                launchCount = 0,
                firstInstallTime = 0L,
                isSystemApp = false,
            )

        val priority =
            appTopMatchPriority(
                app = googleMaps,
                nickname = null,
                query = SearchQueryContext.fromRawQuery("Gm"),
            )

        assertFalse(SearchRankingUtils.isOtherMatch(priority))
    }

    private fun topMatch(
        name: String,
        priority: Int,
        secondaryScore: Long,
        sectionOrder: Int = 0,
        index: Int,
    ): TopMatchItem.App =
        TopMatchItem.App(
            app = AppInfo(
                appName = name,
                packageName = "com.example.$name",
                lastUsedTime = 0L,
                totalTimeInForeground = 0L,
                launchCount = 0,
                firstInstallTime = 0L,
                isSystemApp = false,
            ),
            priority = priority,
            sectionOrder = sectionOrder,
            secondaryScore = secondaryScore,
            index = index,
        )
}
