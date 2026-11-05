package com.tk.quicksearch.search.data.bookmarks

import com.tk.quicksearch.search.models.BookmarkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkHtmlWriterTest {
    private val bookmarks =
        listOf(
            BookmarkInfo(1L, "Tom & Jerry <\"Fans\">", "https://example.com/a?b=1&c=2", null, createdAtMillis = 5_000L),
            BookmarkInfo(2L, "News", "http://news.example.org/", "com.android.chrome", 7L, createdAtMillis = 0L),
        )

    @Test
    fun write_roundTripsThroughParser() {
        val html = BookmarkHtmlWriter.write(bookmarks) { if (it.bookmarkId == 2L) "iVBORw0KGgo=" else null }

        val parsed = BookmarkHtmlParser.parse(html)

        assertEquals(
            listOf(
                ImportedBookmark("Tom & Jerry <\"Fans\">", "https://example.com/a?b=1&c=2", null),
                ImportedBookmark("News", "http://news.example.org/", "iVBORw0KGgo="),
            ),
            parsed,
        )
    }

    @Test
    fun write_usesNetscapeHeaderAndSecondsForAddDate() {
        val html = BookmarkHtmlWriter.write(bookmarks)

        assertTrue(html.startsWith("<!DOCTYPE NETSCAPE-Bookmark-file-1>"))
        assertTrue(html.contains("ADD_DATE=\"5\""))
    }
}
