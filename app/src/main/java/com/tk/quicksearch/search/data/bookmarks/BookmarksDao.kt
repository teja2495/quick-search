package com.tk.quicksearch.search.data.bookmarks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface BookmarksDao {
    @Query("SELECT * FROM bookmarks")
    fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(bookmarks: List<BookmarkEntity>): List<Long>

    @Update
    fun update(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET faviconVersion = :faviconVersion WHERE bookmarkId = :bookmarkId")
    fun setFaviconVersion(bookmarkId: Long, faviconVersion: Long)

    @Query("DELETE FROM bookmarks WHERE bookmarkId = :bookmarkId")
    fun delete(bookmarkId: Long)

    @Query("DELETE FROM bookmarks")
    fun deleteAll()

    @Transaction
    fun replaceAll(bookmarks: List<BookmarkEntity>) {
        deleteAll()
        insertAll(bookmarks)
    }
}
