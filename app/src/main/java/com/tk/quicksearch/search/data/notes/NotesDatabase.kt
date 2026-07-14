package com.tk.quicksearch.search.data.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        @Volatile private var instance: NotesDatabase? = null

        fun get(context: Context): NotesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "user_notes.db",
                ).build().also { instance = it }
            }
    }
}
