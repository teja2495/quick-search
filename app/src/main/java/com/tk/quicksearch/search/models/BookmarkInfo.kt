package com.tk.quicksearch.search.models

/**
 * A website saved in Quick Search.
 *
 * [browserPackageName] is the browser the link opens in: the in-app browser package by default,
 * a browser app's package, or null for the system default browser. [faviconVersion] is 0 when no
 * favicon is stored and changes whenever a new one is saved, so cached images refresh.
 */
data class BookmarkInfo(
    val bookmarkId: Long,
    val name: String,
    val url: String,
    val browserPackageName: String?,
    val faviconVersion: Long = 0L,
    val createdAtMillis: Long,
) {
    val hasFavicon: Boolean get() = faviconVersion > 0L
}
