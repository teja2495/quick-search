package com.tk.quicksearch.search.data.bookmarks

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tk.quicksearch.search.models.BookmarkInfo

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val bookmarkId: Long = 0L,
    val name: String,
    val url: String,
    val browserPackageName: String?,
    val faviconVersion: Long,
    val createdAtMillis: Long,
)

internal fun BookmarkEntity.toModel() =
    BookmarkInfo(bookmarkId, name, url, browserPackageName, faviconVersion, createdAtMillis)

internal fun BookmarkInfo.toEntity() =
    BookmarkEntity(bookmarkId, name, url, browserPackageName, faviconVersion, createdAtMillis)
