package com.tk.quicksearch.search.noted

import android.content.Context
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.models.NoteInfo

/**
 * While Noted Integration is on, Quick Search's Quick Note shows and edits Noted's Quick Note. Its own
 * text stays stored underneath, and comes back when the toggle is off or Noted can't share its note.
 */
internal object NotedQuickNoteMirror {
    /** [note] with Noted's Quick Note text, or [note] unchanged when Noted's text isn't available. Blocking. */
    fun withNotedText(context: Context, note: NoteInfo): NoteInfo {
        if (!NotedIntegration.isEnabled(context)) return note
        val text = NotedRepository(context).quickNoteTextBlocking() ?: return note
        return if (text == note.markdownContent) note else note.copy(markdownContent = text)
    }

    /**
     * Saves [text] to Noted's Quick Note while the integration is on. Returns false when it is off or
     * Noted didn't take it, so the caller saves the text locally and nothing typed is lost. Blocking.
     */
    fun save(context: Context, text: String): Boolean =
        NotedIntegration.isEnabled(context) && NotedRepository(context).replaceQuickNoteBlocking(text)
}

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
