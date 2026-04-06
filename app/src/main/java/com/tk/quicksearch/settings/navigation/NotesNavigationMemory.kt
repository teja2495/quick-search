package com.tk.quicksearch.settings.settingsDetailScreen

object NotesNavigationMemory {
    private var pendingNoteId: Long? = null

    fun setPendingNoteId(noteId: Long?) {
        pendingNoteId = noteId
    }

    fun consumePendingNoteId(): Long? {
        val value = pendingNoteId
        pendingNoteId = null
        return value
    }
}
