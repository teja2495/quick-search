package com.tk.quicksearch.search.data.bookmarks

import com.tk.quicksearch.search.models.BookmarkInfo

/**
 * Writes bookmarks in the standard bookmarks HTML format (the Netscape bookmark file that Chrome,
 * Firefox, Edge and Safari import), which [BookmarkHtmlParser] reads back. Every link is written
 * at the top level, with its saved icon inlined as a data URI when [iconBase64] returns one.
 */
object BookmarkHtmlWriter {
    fun write(
        bookmarks: List<BookmarkInfo>,
        iconBase64: (BookmarkInfo) -> String? = { null },
    ): String =
        buildString {
            appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
            appendLine("<!-- This is an automatically generated file.")
            appendLine("     It will be read and overwritten.")
            appendLine("     DO NOT EDIT! -->")
            appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
            appendLine("<TITLE>Bookmarks</TITLE>")
            appendLine("<H1>Bookmarks</H1>")
            appendLine("<DL><p>")
            bookmarks.forEach { bookmark ->
                append("    <DT><A HREF=\"").append(escape(bookmark.url)).append('"')
                append(" ADD_DATE=\"").append(bookmark.createdAtMillis / 1000L).append('"')
                iconBase64(bookmark)?.let { icon ->
                    append(" ICON=\"data:image/png;base64,").append(icon).append('"')
                }
                append('>').append(escape(bookmark.name)).appendLine("</A>")
            }
            appendLine("</DL><p>")
        }

    private fun escape(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
