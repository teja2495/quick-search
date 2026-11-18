package com.tk.quicksearch.search.data.preferences

import android.content.Context

class NotesPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getPinnedNoteIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_PINNED_NOTE_IDS)

    fun getPinnedNoteOrder(): List<Long> =
        getStringListPref(BasePreferences.KEY_PINNED_NOTE_ORDER).mapNotNull { it.toLongOrNull() }

    fun setPinnedNoteOrder(order: List<Long>): List<Long> =
        order.distinct().also {
            setStringListPref(BasePreferences.KEY_PINNED_NOTE_ORDER, it.map(Long::toString))
        }

    fun pinNote(noteId: Long): Set<Long> =
        pinLongItem(BasePreferences.KEY_PINNED_NOTE_IDS, noteId).also {
            if (noteId !in getPinnedNoteOrder()) {
                setPinnedNoteOrder(getPinnedNoteOrder() + noteId)
            }
        }

    fun unpinNote(noteId: Long): Set<Long> =
        unpinLongItem(BasePreferences.KEY_PINNED_NOTE_IDS, noteId).also {
            setPinnedNoteOrder(getPinnedNoteOrder().filterNot { it == noteId })
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

    fun getQuickNoteId(): Long = prefs.getLong(BasePreferences.KEY_QUICK_NOTE_ID, -1L)

    fun setQuickNoteId(noteId: Long) {
        prefs.edit().putLong(BasePreferences.KEY_QUICK_NOTE_ID, noteId).apply()
    }

    fun isQuickNoteEnabled(): Boolean =
        prefs.getBoolean(BasePreferences.KEY_QUICK_NOTE_ENABLED, true)

    fun setQuickNoteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BasePreferences.KEY_QUICK_NOTE_ENABLED, enabled).apply()
    }
}
