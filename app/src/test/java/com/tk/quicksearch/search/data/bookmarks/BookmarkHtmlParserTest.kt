package com.tk.quicksearch.search.data.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarkHtmlParserTest {
    private val export =
        """
        <!DOCTYPE NETSCAPE-Bookmark-file-1>
        <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
        <TITLE>Bookmarks</TITLE>
        <H1>Bookmarks</H1>
        <DL><p>
            <DT><H3 ADD_DATE="1">Bookmarks bar</H3>
            <DL><p>
                <DT><A HREF="https://example.com/" ADD_DATE="1" ICON="data:image/png;base64,iVBORw0KGgo=">Example &amp; Co</A>
                <DT><A HREF="javascript:void(0)">Bookmarklet</A>
                <DT><H3>Nested</H3>
                <DL><p>
                    <DT><a href='http://news.example.org/a?b=1&amp;c=2'>  News
                       site  </a>
                </DL><p>
            </DL><p>
            <DT><A HREF="https://untitled.example.net/"></A>
            <DT><A HREF="place:sort=8">Firefox smart folder</A>
        </DL><p>
        """.trimIndent()

    @Test
    fun parse_keepsWebLinksAcrossFolders() {
        val items = BookmarkHtmlParser.parse(export)

        assertEquals(
            listOf("https://example.com/", "http://news.example.org/a?b=1&c=2", "https://untitled.example.net/"),
            items.map { it.url },
        )
    }

    @Test
    fun parse_decodesAndCollapsesTitles() {
        val items = BookmarkHtmlParser.parse(export)

        assertEquals("Example & Co", items[0].name)
        assertEquals("News site", items[1].name)
        assertEquals("untitled.example.net", items[2].name)
    }

    @Test
    fun parse_readsOnlyBase64ImageIcons() {
        val items = BookmarkHtmlParser.parse(export)

        assertEquals("iVBORw0KGgo=", items[0].iconBase64)
        assertNull(items[1].iconBase64)
    }

    @Test
    fun parse_keepsOneCopyOfLinksRepeatedAcrossFolders() {
        val html =
            """
            <DL><p>
                <DT><H3>Work</H3>
                <DL><p><DT><A HREF="https://example.com/">Example</A></DL><p>
                <DT><H3>Personal</H3>
                <DL><p>
                    <DT><A HREF="http://www.example.com" ICON="data:image/png;base64,AAAA">Example again</A>
                    <DT><A HREF="https://other.example/">Other</A>
                </DL><p>
            </DL><p>
            """.trimIndent()

        val items = BookmarkHtmlParser.parse(html)

        assertEquals(listOf("https://example.com/", "https://other.example/"), items.map { it.url })
        assertEquals("Example", items[0].name)
        assertEquals("AAAA", items[0].iconBase64)
    }

    @Test
    fun parse_returnsEmptyForFilesWithoutLinks() {
        assertEquals(emptyList<ImportedBookmark>(), BookmarkHtmlParser.parse("<html><body>Nothing</body></html>"))
    }

    @Test
    fun decodeEntities_handlesNumericAndDoubleEscapedEntities() {
        assertEquals("é ' &lt;", BookmarkHtmlParser.decodeEntities("&#233; &#x27; &amp;lt;"))
    }
}
