package com.tk.quicksearch.searchEngines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AliasParserTest {
    private val aliases = mapOf("g" to "google", "yt" to "youtube")

    @Test
    fun `prefix alias needs a separating space`() {
        assertNull(AliasParser.detectPrefixAlias("g", aliases))
        assertNull(AliasParser.detectPrefixAlias("gcats", aliases))

        val match = AliasParser.detectPrefixAlias("g ", aliases)
        assertEquals("google", match?.target)
        assertEquals("", match?.queryWithoutAlias)
    }

    @Test
    fun `prefix alias strips alias and surrounding whitespace`() {
        val match = AliasParser.detectPrefixAlias("  YT   lofi beats ", aliases)
        assertEquals("youtube", match?.target)
        assertEquals("lofi beats ", match?.queryWithoutAlias)
    }

    @Test
    fun `prefix alias ignores unknown and blank queries`() {
        assertNull(AliasParser.detectPrefixAlias("x cats", aliases))
        assertNull(AliasParser.detectPrefixAlias("   ", aliases))
        assertNull(AliasParser.detectPrefixAlias("", aliases))
    }

    @Test
    fun `suffix alias waits for trailing space by default`() {
        assertNull(AliasParser.detectSuffixAlias("cats g", aliases))

        val match = AliasParser.detectSuffixAlias("funny  cats g ", aliases)
        assertEquals("google", match?.target)
        assertEquals("funny cats", match?.queryWithoutAlias)
    }

    @Test
    fun `suffix alias can trigger without trailing space`() {
        val match = AliasParser.detectSuffixAlias("cats YT", aliases, requireTrailingSpace = false)
        assertEquals("youtube", match?.target)
        assertEquals("cats", match?.queryWithoutAlias)
    }

    @Test
    fun `suffix alias needs a query before the alias`() {
        assertNull(AliasParser.detectSuffixAlias("g ", aliases))
        assertNull(AliasParser.detectSuffixAlias("  yt  ", aliases))
        assertNull(AliasParser.detectSuffixAlias("cats x ", aliases))
        assertNull(AliasParser.detectSuffixAlias(" ", aliases))
    }
}
