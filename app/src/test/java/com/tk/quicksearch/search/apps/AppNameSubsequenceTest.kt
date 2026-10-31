package com.tk.quicksearch.search.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNameSubsequenceTest {
    @Test
    fun matchesLettersInOrderFromWordStart() {
        assertTrue(score("ffx", "Firefox") > 0)
        assertTrue(score("kdx", "KeePassDX") > 0)
        assertTrue(score("kpdx", "KeePassDX") > 0)
        assertTrue(score("gmps", "Google Maps") > 0)
    }

    @Test
    fun rejectsOutOfOrderMidWordAndShortQueries() {
        assertEquals(0, score("fxf", "Firefox"))
        assertEquals(0, score("rfx", "Firefox"))
        assertEquals(0, score("fx", "Firefox"))
        assertEquals(0, score("firefox", "Firefox"))
    }

    @Test
    fun wordStartHitsScoreHigherThanMidWordHits() {
        assertTrue(score("kpd", "KeePassDX") > score("kes", "KeePassDX"))
    }

    @Test
    fun ignoresDiacriticsAndSeparators() {
        assertTrue(score("cfe", "Café-Finder") > 0)
    }

    private fun score(
        query: String,
        name: String,
    ): Int = AppNameSubsequence.score(query, AppNameSubsequence.prepare(name))
}
