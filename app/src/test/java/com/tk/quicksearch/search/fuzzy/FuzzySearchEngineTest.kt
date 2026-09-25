package com.tk.quicksearch.search.fuzzy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchEngineTest {
    private val engine = FuzzySearchEngine()

    @Test
    fun shortQueryMatchingInitialsIsAPerfectMatch() {
        assertEquals(100, engine.computeScore("gm", "Google Maps"))
        assertEquals(100, engine.computeScore("gm", "Maps", targetNickname = "Google Maps"))
    }

    @Test
    fun queriesBelowMinimumLengthOnlyMatchByInitials() {
        assertEquals(0, engine.computeScore("go", "Google Maps"))
        assertEquals(0, engine.computeScore("mapz", "Google Maps", minQueryLength = 5))
    }

    @Test
    fun oneTypoInAShortWordPrefixStillMatches() {
        assertTrue(engine.computeScore("yiu", "YouTube") >= 82)
        assertTrue(engine.computeScore("mapz", "Google Maps") >= 82)
    }

    @Test
    fun nicknameCanOutscoreTheTargetName() {
        val withoutNickname = engine.computeScore("mom", "Jane Doe")
        val withNickname = engine.computeScore("mom", "Jane Doe", targetNickname = "Mom")

        assertTrue(withNickname > withoutNickname)
        assertTrue(withNickname >= 82)
    }
}
