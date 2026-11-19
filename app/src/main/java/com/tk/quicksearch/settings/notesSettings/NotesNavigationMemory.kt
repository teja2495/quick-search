package com.tk.quicksearch.settings.notesSettings

object NotesNavigationMemory {
    private var pendingNoteId: Long? = null
    private var pendingHideEditorAppBar: Boolean = false
    private var pendingPinToNotification: Boolean = false

    fun setPendingNoteId(
        noteId: Long?,
        hideEditorAppBar: Boolean = false,
        pinToNotification: Boolean = false,
    ) {
        pendingNoteId = noteId
        pendingHideEditorAppBar = hideEditorAppBar
        pendingPinToNotification = pinToNotification
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

    fun consumePinToNotificationRequest(): Boolean {
        val value = pendingPinToNotification
        pendingPinToNotification = false
        return value
    }
}
