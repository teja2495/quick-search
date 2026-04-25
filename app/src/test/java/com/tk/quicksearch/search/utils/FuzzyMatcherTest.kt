package com.tk.quicksearch.search.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    @Test
    fun maxEditDistanceForQueryUsesMediumDistanceForShortAndMediumQueries() {
        assertEquals(1, FuzzyMatcher.maxEditDistanceForQuery("yt"))
        assertEquals(1, FuzzyMatcher.maxEditDistanceForQuery("sett"))
    }

    @Test
    fun maxEditDistanceForQueryAllowsLongDistanceForLongQueries() {
        assertEquals(2, FuzzyMatcher.maxEditDistanceForQuery("setting"))
    }

    @Test
    fun scoreRejectsTooDistantTypoWhenEditDistanceIsCapped() {
        val score =
            FuzzyMatcher.score(
                query = "abc",
                target = "settings",
                maxEditDistance = 1,
            )

        assertEquals(0, score)
    }

    @Test
    fun scoreAllowsLongQueryTypoWhenDistanceTwoIsSelected() {
        val maxDistance = FuzzyMatcher.maxEditDistanceForQuery("setings")
        val score =
            FuzzyMatcher.score(
                query = "setings",
                target = "settings",
                maxEditDistance = maxDistance,
            )

        assertTrue(score > 0)
    }

    @Test
    fun searchAppliesScoreThresholdAndCandidateLimitBeforeScoring() {
        val matches =
            FuzzyMatcher.search(
                query = "setings",
                candidates = listOf("settings", "settlings", "search"),
                minScore = 80,
                limit = 10,
                candidateLimit = 2,
                maxEditDistance = FuzzyMatcher.maxEditDistanceForQuery("setings"),
                primaryTextSelector = { it },
            )

        assertEquals(listOf("settings", "settlings"), matches.map { it.item })
    }
}
