package com.tk.quicksearch.search.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTextUtilsTest {
    @Test
    fun `centers preview around matching line with two lines of context`() {
        val content = (1..7).joinToString("\n") { line -> "line $line" }

        assertEquals(
            "line 2\nline 3\nline 4\nline 5\nline 6",
            NotesTextUtils.matchCenteredPreview(content, "line 4"),
        )
    }

    @Test
    fun `uses normalized matching for preview`() {
        assertEquals(
            "before\nCafé plans\nafter",
            NotesTextUtils.matchCenteredPreview("before\nCafé plans\nafter", "cafe"),
        )
    }

    private val fakeLinkPattern = Regex("""https?://\S+""")

    private fun fakeFindLinks(text: String): List<NotesTextUtils.LinkRange> =
        fakeLinkPattern.findAll(text).map { NotesTextUtils.LinkRange(it.range.first, it.range.last + 1, it.value) }.toList()

    private fun assertIncrementalMatchesFullScan(before: String, after: String) {
        val incremental =
            NotesTextUtils.updateLinkRanges(before, fakeFindLinks(before), after, ::fakeFindLinks)
        assertEquals("$before -> $after", fakeFindLinks(after), incremental)
    }

    @Test
    fun `incremental link ranges match a full rescan`() {
        val base = "intro http://a.example\nmiddle line\nend http://b.example tail"
        listOf(
            base,
            base.replace("middle", "middle http://c.example"),
            base.replace("intro ", ""),
            base.replace("\nmiddle line", ""),
            base.replace("middle line\n", "middle line\n\n"),
            base.replace("http://b.example", "http://bb.example"),
            "x" + base,
            base + " http://d.example",
            "",
            "http://only.example",
        ).forEach { after ->
            assertIncrementalMatchesFullScan(base, after)
            assertIncrementalMatchesFullScan(after, base)
        }
    }

    @Test
    fun `typing character by character keeps link ranges exact`() {
        val target = "call http://x.example now\nsecond http://y.example\nthird"
        var text = ""
        var links = emptyList<NotesTextUtils.LinkRange>()
        for (char in target) {
            val next = text + char
            links = NotesTextUtils.updateLinkRanges(text, links, next, ::fakeFindLinks)
            text = next
            assertEquals(fakeFindLinks(text), links)
        }
        // Delete from the middle back to the start.
        while (text.isNotEmpty()) {
            val cut = text.length / 2
            val next = text.removeRange(cut, cut + 1)
            links = NotesTextUtils.updateLinkRanges(text, links, next, ::fakeFindLinks)
            text = next
            assertEquals(fakeFindLinks(text), links)
        }
    }
}
