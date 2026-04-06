package com.tk.quicksearch.search.data.preferences

import android.content.Context

class NotesPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getPinnedNoteIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_PINNED_NOTE_IDS)

    fun pinNote(noteId: Long): Set<Long> = pinLongItem(BasePreferences.KEY_PINNED_NOTE_IDS, noteId)

    fun unpinNote(noteId: Long): Set<Long> = unpinLongItem(BasePreferences.KEY_PINNED_NOTE_IDS, noteId)

    fun getNotesJson(): String = prefs.getString(BasePreferences.KEY_NOTES_DATA, null).orEmpty()

    fun setNotesJson(json: String) {
        prefs.edit().putString(BasePreferences.KEY_NOTES_DATA, json).apply()
    }

    fun nextNoteId(): Long {
        val next = prefs.getLong(BasePreferences.KEY_NOTE_ID_COUNTER, 1L)
        prefs.edit().putLong(BasePreferences.KEY_NOTE_ID_COUNTER, next + 1L).apply()
        return next
    }

    fun ensureNoteIdCounterAtLeast(nextCandidate: Long) {
        val current = prefs.getLong(BasePreferences.KEY_NOTE_ID_COUNTER, 1L)
        if (nextCandidate > current) {
            prefs.edit().putLong(BasePreferences.KEY_NOTE_ID_COUNTER, nextCandidate).apply()
        }
    }
}
