package com.tk.quicksearch.search.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnippetExpanderTest {
    private val keywords = mapOf("addr" to "1 Main St", "sig" to "Thanks,\nTeja", "siglong" to "Long one")

    @Test
    fun `expands keyword at caret and appends a trailing space`() {
        val (replacement, undo) = SnippetExpander.computeExpansion("addr", 4, keywords)!!
        assertEquals("1 Main St ", replacement.text)
        assertEquals(10, replacement.cursor)
        assertEquals("addr", undo.keyword)
        assertEquals(0, undo.startIndex)
        assertEquals(9, undo.spaceIndex)
    }

    @Test
    fun `expands mid sentence and keeps the text after the caret`() {
        val (replacement, _) = SnippetExpander.computeExpansion("go to addr now", 10, keywords)!!
        assertEquals("go to 1 Main St  now", replacement.text)
        assertEquals(16, replacement.cursor)
    }

    @Test
    fun `does not fire inside a longer word`() {
        assertNull(SnippetExpander.computeExpansion("myaddr", 6, keywords))
    }

    @Test
    fun `prefers the longest matching keyword`() {
        val (replacement, _) = SnippetExpander.computeExpansion("siglong", 7, keywords)!!
        assertEquals("Long one ", replacement.text)
    }

    @Test
    fun `matches case insensitively but restores what was typed`() {
        val (replacement, undo) = SnippetExpander.computeExpansion("ADDR", 4, keywords)!!
        assertEquals("1 Main St ", replacement.text)
        assertEquals("ADDR", undo.keyword)
    }

    @Test
    fun `ignores an out of range caret`() {
        assertNull(SnippetExpander.computeExpansion("addr", 99, keywords))
        assertNull(SnippetExpander.computeExpansion("addr", -1, keywords))
    }

    @Test
    fun `deleting the appended space reverts the expansion`() {
        val (_, undo) = SnippetExpander.computeExpansion("go to addr", 10, keywords)!!
        val afterBackspace = "go to 1 Main St"
        val undone = SnippetExpander.computeUndo(afterBackspace, afterBackspace.length, undo)!!
        assertEquals("go to addr", undone.text)
        assertEquals(10, undone.cursor)
    }

    @Test
    fun `reverts correctly when text follows the expansion`() {
        val (_, undo) = SnippetExpander.computeExpansion("addr now", 4, keywords)!!
        // "1 Main St  now" -> delete the space we appended
        val afterBackspace = "1 Main St now"
        val undone = SnippetExpander.computeUndo(afterBackspace, 9, undo)!!
        assertEquals("addr now", undone.text)
        assertEquals(4, undone.cursor)
    }

    @Test
    fun `typing more text does not revert`() {
        val (_, undo) = SnippetExpander.computeExpansion("addr", 4, keywords)!!
        assertNull(SnippetExpander.computeUndo("1 Main St x", 11, undo))
    }

    @Test
    fun `deleting a different character does not revert`() {
        val (_, undo) = SnippetExpander.computeExpansion("addr", 4, keywords)!!
        assertNull(SnippetExpander.computeUndo("1 Main S ", 8, undo))
    }

    @Test
    fun `blank bodies and empty keyword maps are ignored`() {
        assertNull(SnippetExpander.computeExpansion("addr", 4, emptyMap()))
        assertNull(SnippetExpander.computeExpansion("addr", 4, mapOf("addr" to "")))
    }
}
