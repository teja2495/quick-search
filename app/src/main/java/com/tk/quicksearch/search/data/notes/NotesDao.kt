package com.tk.quicksearch.search.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes")
    fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE noteId = :noteId LIMIT 1")
    fun getById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE noteId IN (:noteIds)")
    fun getByIds(noteIds: Collection<Long>): List<NoteEntity>

    @Query("SELECT MAX(noteId) FROM notes")
    fun maxNoteId(): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM notes LIMIT 1)")
    fun hasAny(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE noteId = :noteId")
    fun deleteById(noteId: Long)

    @Query("DELETE FROM notes")
    fun deleteAll()

    @Transaction
    fun replaceAll(notes: List<NoteEntity>) {
        deleteAll()
        upsertAll(notes)
    }
}
