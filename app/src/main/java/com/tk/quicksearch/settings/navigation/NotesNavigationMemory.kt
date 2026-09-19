package com.tk.quicksearch.settings.settingsDetailScreen

object NotesNavigationMemory {
    private var pendingNoteId: Long? = null
    private var pendingIsSnippet: Boolean = false
    private var pendingHideEditorAppBar: Boolean = false

    fun setPendingNoteId(
        noteId: Long?,
        hideEditorAppBar: Boolean = false,
        isSnippet: Boolean = false,
    ) {
        pendingNoteId = noteId
        pendingIsSnippet = isSnippet
        pendingHideEditorAppBar = hideEditorAppBar
    }

    fun consumePendingNoteId(): Long? {
        val value = pendingNoteId
        pendingNoteId = null
        return value
    }

    /** Whether the pending editor entry is a snippet; read by the header before the editor consumes it. */
    fun peekPendingIsSnippet(): Boolean = pendingIsSnippet

    fun consumePendingIsSnippet(): Boolean {
        val value = pendingIsSnippet
        pendingIsSnippet = false
        return value
    }

    fun consumeHideEditorAppBarRequest(): Boolean {
        val value = pendingHideEditorAppBar
        pendingHideEditorAppBar = false
        return value
    }
}
