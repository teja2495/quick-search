package com.tk.quicksearch.settings.settingsDetailScreen

object NotesNavigationMemory {
    private var pendingNoteId: Long? = null
    private var pendingHideEditorAppBar: Boolean = false

    fun setPendingNoteId(
        noteId: Long?,
        hideEditorAppBar: Boolean = false,
    ) {
        pendingNoteId = noteId
        pendingHideEditorAppBar = hideEditorAppBar
    }

    fun consumePendingNoteId(): Long? {
        val value = pendingNoteId
        pendingNoteId = null
        return value
    }

    fun consumeHideEditorAppBarRequest(): Boolean {
        val value = pendingHideEditorAppBar
        pendingHideEditorAppBar = false
        return value
    }
}
