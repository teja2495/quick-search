package com.tk.quicksearch.search.noted

import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.models.NoteInfo

/**
 * Drops Quick Search's Quick Note from note results while Noted Integration is on, since Noted's own
 * quick note is used instead. The note itself is kept, and still opens from swipe navigation and widgets.
 */
internal fun List<NoteInfo>.withoutQuickNoteWhileNoted(notesRepository: NotesRepository): List<NoteInfo> =
    if (NotedIntegration.isEnabled(notesRepository.appContext)) {
        filterNot { notesRepository.isQuickNote(it.noteId) }
    } else {
        this
    }
