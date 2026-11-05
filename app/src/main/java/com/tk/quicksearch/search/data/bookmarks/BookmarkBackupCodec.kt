package com.tk.quicksearch.search.data.bookmarks

import com.tk.quicksearch.search.models.BookmarkInfo
import org.json.JSONArray
import org.json.JSONObject

/** Bookmarks in settings backups. Favicons are left out and fetched again after a restore. */
internal object BookmarkBackupCodec {
    private const val KEY_ID = "bookmarkId"
    private const val KEY_NAME = "name"
    private const val KEY_URL = "url"
    private const val KEY_BROWSER = "browserPackageName"
    private const val KEY_CREATED_AT = "createdAtMillis"

    fun serialize(bookmarks: List<BookmarkInfo>): JSONArray =
        JSONArray().apply {
            bookmarks.forEach { bookmark ->
                put(
                    JSONObject()
                        .put(KEY_ID, bookmark.bookmarkId)
                        .put(KEY_NAME, bookmark.name)
                        .put(KEY_URL, bookmark.url)
                        // A null browser (system default) is written as an absent key.
                        .putOpt(KEY_BROWSER, bookmark.browserPackageName)
                        .put(KEY_CREATED_AT, bookmark.createdAtMillis),
                )
            }
        }

    fun parse(array: JSONArray): List<BookmarkInfo> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optLong(KEY_ID, -1L)
                val url = item.optString(KEY_URL).trim()
                if (id <= 0L || url.isEmpty()) continue
                add(
                    BookmarkInfo(
                        bookmarkId = id,
                        name = item.optString(KEY_NAME).ifBlank { BookmarkUrls.displayHost(url) },
                        url = url,
                        browserPackageName =
                            if (item.has(KEY_BROWSER) && !item.isNull(KEY_BROWSER)) item.optString(KEY_BROWSER) else null,
                        createdAtMillis = item.optLong(KEY_CREATED_AT, 0L),
                    ),
                )
            }
        }
}
