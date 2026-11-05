package com.tk.quicksearch.search.data.bookmarks

import com.tk.quicksearch.search.models.BookmarkInfo
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkBackupCodecTest {
    @Test
    fun roundTrip_keepsFieldsAndNullBrowser_dropsFavicon() {
        val bookmarks =
            listOf(
                BookmarkInfo(1L, "Example", "https://example.com", "com.android.chrome", faviconVersion = 5L, createdAtMillis = 10L),
                BookmarkInfo(2L, "Docs", "https://docs.example.com", null, createdAtMillis = 20L),
            )

        val restored = BookmarkBackupCodec.parse(JSONArray(BookmarkBackupCodec.serialize(bookmarks).toString()))

        assertEquals(bookmarks.map { it.copy(faviconVersion = 0L) }, restored)
    }

    @Test
    fun parse_skipsInvalidEntriesAndFillsBlankNames() {
        val array =
            JSONArray()
                .put(JSONObject().put("bookmarkId", 0L).put("url", "https://skipped.example"))
                .put(JSONObject().put("bookmarkId", 3L).put("url", ""))
                .put(JSONObject().put("bookmarkId", 4L).put("url", "https://www.kept.example/").put("name", ""))

        val restored = BookmarkBackupCodec.parse(array)

        assertEquals(listOf(4L), restored.map { it.bookmarkId })
        assertEquals("kept.example", restored.single().name)
    }
}
