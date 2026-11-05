package com.tk.quicksearch.search.data.bookmarks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = true)
abstract class BookmarksDatabase : RoomDatabase() {
    abstract fun bookmarksDao(): BookmarksDao

    companion object {
        @Volatile private var instance: BookmarksDatabase? = null

        fun get(context: Context): BookmarksDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookmarksDatabase::class.java,
                    "bookmarks.db",
                ).build().also { instance = it }
            }
    }
}
