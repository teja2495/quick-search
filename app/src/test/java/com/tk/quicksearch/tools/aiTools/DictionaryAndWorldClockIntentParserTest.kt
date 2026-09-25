package com.tk.quicksearch.tools.aiTools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryAndWorldClockIntentParserTest {
    @Test
    fun dictionaryAcceptsDefinePrefixOrMeaningSuffix() {
        assertEquals("serendipity", DictionaryIntentParser.parseConfirmed("Define serendipity")?.term)
        assertEquals("carpe diem", DictionaryIntentParser.parseConfirmed("carpe diem MEANING")?.term)
    }

    @Test
    fun dictionaryIgnoresQueriesThatOnlyContainTheKeyword() {
        assertFalse(DictionaryIntentParser.isCandidate("defined benefits"))
        assertFalse(DictionaryIntentParser.isCandidate("meaningful quotes"))
        assertNull(DictionaryIntentParser.parseConfirmed("define "))
        assertNull(DictionaryIntentParser.parseConfirmed("meaning"))
    }

    @Test
    fun worldClockExtractsThePlaceAfterTimeIn() {
        assertTrue(WorldClockIntentParser.isCandidate("  Time in Tokyo "))
        assertEquals("new york", WorldClockIntentParser.parseConfirmed("time in  new york")?.timeExpression)
    }

    @Test
    fun worldClockRequiresTheTimeInPrefix() {
        assertFalse(WorldClockIntentParser.isCandidate("tokyo time"))
        assertFalse(WorldClockIntentParser.isCandidate("overtime in office"))
        assertNull(WorldClockIntentParser.parseConfirmed("time in "))
    }
}
