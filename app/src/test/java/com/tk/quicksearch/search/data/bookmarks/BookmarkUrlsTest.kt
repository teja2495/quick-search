package com.tk.quicksearch.search.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarkUrlsTest {
    @Test
    fun normalize_addsHttpsToBareHosts() {
        assertEquals("https://example.com", BookmarkUrls.normalize("example.com"))
        assertEquals("https://example.com/path?q=1", BookmarkUrls.normalize("  example.com/path?q=1 "))
        assertEquals("https://localhost.dev:8080", BookmarkUrls.normalize("localhost.dev:8080"))
    }

    @Test
    fun normalize_keepsHttpAndHttps() {
        assertEquals("http://example.com", BookmarkUrls.normalize("http://example.com"))
        assertEquals("HTTPS://Example.com", BookmarkUrls.normalize("HTTPS://Example.com"))
    }

    @Test
    fun normalize_rejectsNonWebInput() {
        assertNull(BookmarkUrls.normalize(""))
        assertNull(BookmarkUrls.normalize("not a url"))
        assertNull(BookmarkUrls.normalize("mailto:me@example.com"))
        assertNull(BookmarkUrls.normalize("javascript:alert(1)"))
        assertNull(BookmarkUrls.normalize("ftp://example.com"))
        assertNull(BookmarkUrls.normalize("hello"))
    }

    @Test
    fun displayHost_dropsWww() {
        assertEquals("example.com", BookmarkUrls.displayHost("https://www.Example.com/a"))
    }

    @Test
    fun dedupeKey_ignoresSchemeWwwAndTrailingSlash() {
        val key = BookmarkUrls.dedupeKey("https://www.example.com/docs/")
        assertEquals(key, BookmarkUrls.dedupeKey("http://example.com/docs"))
        assertEquals("example.com/docs?x=1", BookmarkUrls.dedupeKey("https://example.com/docs/?x=1"))
    }

    @Test
    fun originOf_returnsSchemeHostAndPort() {
        assertEquals("https://example.com:8443", BookmarkUrls.originOf("https://example.com:8443/a/b"))
        assertNull(BookmarkUrls.originOf("example"))
    }
}
