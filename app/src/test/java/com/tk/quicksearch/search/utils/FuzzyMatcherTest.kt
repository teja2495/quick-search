package com.tk.quicksearch.search.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

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

    @Test
    fun boundedEditDistanceAgreesWithFullLevenshtein() {
        val random = kotlin.random.Random(1)
        repeat(5_000) {
            val first = (0 until random.nextInt(0, 9)).map { 'a' + random.nextInt(4) }.joinToString("")
            val second = (0 until random.nextInt(0, 9)).map { 'a' + random.nextInt(4) }.joinToString("")
            val prefixLength = random.nextInt(0, second.length + 1)
            val maxDistance = random.nextInt(0, 3)
            assertEquals(
                "$first|$second|$prefixLength|$maxDistance",
                levenshtein(first, second.substring(0, prefixLength)) <= maxDistance,
                FuzzyMatcher.isWithinEditDistance(first, second, prefixLength, maxDistance),
            )
        }
    }

    @Test
    fun twoTargetScoreIsBestOfEachTarget() {
        val query = "setings"
        val maxDistance = FuzzyMatcher.maxEditDistanceForQuery(query)
        val primary = "display settings"
        val secondary = "brightness sleep"
        assertEquals(
            maxOf(
                FuzzyMatcher.score(query, primary, maxDistance),
                FuzzyMatcher.score(query, secondary, maxDistance),
            ),
            FuzzyMatcher.score(query, primary, secondary, maxDistance),
        )
        assertEquals(
            FuzzyMatcher.score(query, primary, maxDistance),
            FuzzyMatcher.score(query, primary, "  ", maxDistance),
        )
    }

    private fun levenshtein(
        first: String,
        second: String,
    ): Int {
        val distances = Array(first.length + 1) { IntArray(second.length + 1) }
        for (i in 0..first.length) distances[i][0] = i
        for (j in 0..second.length) distances[0][j] = j
        for (i in 1..first.length) {
            for (j in 1..second.length) {
                val cost = if (first[i - 1] == second[j - 1]) 0 else 1
                distances[i][j] =
                    minOf(distances[i - 1][j] + 1, distances[i][j - 1] + 1, distances[i - 1][j - 1] + cost)
            }
        }
        return distances[first.length][second.length]
    }
}
