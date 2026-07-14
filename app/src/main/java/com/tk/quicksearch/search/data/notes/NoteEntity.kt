package com.tk.quicksearch.search.data.notes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tk.quicksearch.search.models.NoteInfo

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val noteId: Long,
    val title: String,
    val markdownContent: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

internal fun NoteEntity.toModel() =
    NoteInfo(noteId, title, markdownContent, createdAtMillis, updatedAtMillis)

internal fun NoteInfo.toEntity() =
    NoteEntity(noteId, title, markdownContent, createdAtMillis, updatedAtMillis)
