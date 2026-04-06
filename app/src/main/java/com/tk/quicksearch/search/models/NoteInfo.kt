package com.tk.quicksearch.search.models

data class NoteInfo(
    val noteId: Long,
    val title: String,
    val markdownContent: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
