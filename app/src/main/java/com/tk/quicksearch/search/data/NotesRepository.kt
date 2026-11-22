package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.data.notes.NotesRoomStore
import com.tk.quicksearch.search.data.preferences.TriggerPreferences
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.noted.NotedQuickNoteMirror
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.widgets.utils.requestNoteWidgetsRefresh
import java.util.Locale

class NotesRepository(
    context: Context,
) {
    internal val appContext: Context = context.applicationContext
    private val notesPreferences = NotesPreferences(context)
    private val notesStore = NotesRoomStore(context)
    private val triggerPreferences = TriggerPreferences(context)
    private val pendingDeletesByNoteId = mutableMapOf<Long, NoteInfo>()
    private val quickNoteTitle = context.getString(R.string.notes_quick_note_title)

    fun getAllNotes(): List<NoteInfo> {
        val quickNoteId = ensureQuickNoteExists().noteId
        val pinnedIds = notesPreferences.getPinnedNoteIds()
        return readNotes().sortedWith(allNotesOrder(quickNoteId, pinnedIds))
    }

    /** Pinned notes in [getAllNotes] order, read by id rather than loading every note. */
    fun getPinnedNotes(): List<NoteInfo> {
        val pinnedIds = notesPreferences.getPinnedNoteIds()
        if (pinnedIds.isEmpty()) return emptyList()
        val quickNoteId = ensureQuickNoteExists().noteId
        return notesStore.getByIds(pinnedIds).sortedWith(allNotesOrder(quickNoteId, pinnedIds))
    }

    private fun allNotesOrder(quickNoteId: Long, pinnedIds: Set<Long>): Comparator<NoteInfo> =
        compareByDescending<NoteInfo> { it.noteId == quickNoteId }
            .thenByDescending { pinnedIds.contains(it.noteId) }
            .thenByDescending { it.updatedAtMillis }
            .thenBy { it.title.lowercase(Locale.getDefault()) }

    fun getNoteById(noteId: Long): NoteInfo? {
        val quickNoteId = ensureQuickNoteExists().noteId
        val note = notesStore.getById(noteId) ?: return null
        return if (noteId == quickNoteId) NotedQuickNoteMirror.withNotedText(appContext, note) else note
    }

    /** The notes with these ids, in no particular order; one database read for all of them. */
    fun getNotesByIds(noteIds: Collection<Long>): List<NoteInfo> {
        if (noteIds.isEmpty()) return emptyList()
        ensureQuickNoteExists()
        return notesStore.getByIds(noteIds)
    }

    fun searchNotes(
        query: String,
        recentNoteScores: Map<Long, Int> = emptyMap(),
        noteOpenCounts: Map<Long, Int> = emptyMap(),
        secondaryRankingSignal: SecondaryRankingSignal = SecondaryRankingSignal.DEFAULT,
        includeContent: Boolean = false,
        resultLimit: Int = Int.MAX_VALUE,
    ): List<NoteInfo> {
        val quickNoteId = ensureQuickNoteExists().noteId
        val normalizedQuery = NotesTextUtils.normalize(query)
        if (normalizedQuery.isBlank()) return getAllNotes()

        val pinnedIds = notesPreferences.getPinnedNoteIds()
        val triggerMatchingIds = triggerPreferences.findNotesWithMatchingTrigger(query)
        return readNotes()
            .mapNotNull { note ->
                val normalizedTitle = NotesTextUtils.normalize(note.title)
                val titleStartsWith = normalizedTitle.startsWith(normalizedQuery)
                val titleContains = titleStartsWith || normalizedTitle.contains(normalizedQuery)
                val triggerMatches = triggerMatchingIds.contains(note.noteId)
                // The body only decides the score when neither the trigger nor the title matched.
                val bodyContains =
                    includeContent &&
                        !titleContains &&
                        !triggerMatches &&
                        NotesTextUtils.normalize(
                            NotesTextUtils.toSearchablePlainText(note.markdownContent),
                        ).contains(normalizedQuery)
                if (!titleContains && !bodyContains && !triggerMatches) {
                    return@mapNotNull null
                }
                val score =
                    when {
                        triggerMatches -> 500
                        titleStartsWith -> 400
                        titleContains -> 300
                        bodyContains -> 200
                        else -> 0
                    } +
                        if (note.noteId == quickNoteId) 50 else 0 +
                        if (pinnedIds.contains(note.noteId)) 20 else 0
                note to score
            }.sortedWith(
                compareByDescending<Pair<NoteInfo, Int>> { it.second }
                    .thenByDescending {
                        when (secondaryRankingSignal) {
                            SecondaryRankingSignal.RECENCY -> recentNoteScores[it.first.noteId] ?: 0
                            SecondaryRankingSignal.MOST_OPENED -> noteOpenCounts[it.first.noteId] ?: 0
                            SecondaryRankingSignal.NONE -> 0
                        }
                    }.thenBy { it.first.title.lowercase(Locale.getDefault()) },
            ).asSequence()
            .take(resultLimit)
            .map { it.first }
            .toList()
    }

    fun createNote(
        title: String,
        markdownContent: String,
    ): NoteInfo {
        // Restored or migrated notes can carry ids past the counter; never hand one out twice.
        notesStore.maxNoteId()?.let { notesPreferences.ensureNoteIdCounterAtLeast(it + 1L) }
        val now = System.currentTimeMillis()
        val note =
            NoteInfo(
                noteId = notesPreferences.nextNoteId(),
                title = title.trim(),
                markdownContent = markdownContent,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
        notesStore.upsert(note)
        onNotesWritten()
        return note
    }

    fun updateNote(
        noteId: Long,
        title: String,
        markdownContent: String,
    ): NoteInfo? {
        val quickNoteId = ensureQuickNoteExists().noteId
        val current = notesStore.getById(noteId) ?: return null
        if (noteId == quickNoteId && NotedQuickNoteMirror.save(appContext, markdownContent)) {
            // Noted holds the text; keep the local Quick Note as it was for when the integration is off.
            val mirrored = current.copy(markdownContent = markdownContent)
            onNotesWritten()
            PinnedNotifications.updatePinnedNote(appContext, mirrored)
            return mirrored
        }
        val updated =
            current.copy(
                title =
                    if (noteId == quickNoteId) {
                        quickNoteTitle
                    } else {
                        title.trim()
                    },
                markdownContent = markdownContent,
                updatedAtMillis = System.currentTimeMillis(),
            )
        notesStore.upsert(updated)
        onNotesWritten()
        PinnedNotifications.updatePinnedNote(appContext, updated)
        return updated
    }

    fun stageDelete(noteId: Long): NoteInfo? {
        if (isQuickNote(noteId)) return null
        val note = notesStore.getById(noteId) ?: return null
        pendingDeletesByNoteId[noteId] = note
        notesStore.deleteById(noteId)
        onNotesWritten()
        return note
    }

    fun undoDelete(noteId: Long): NoteInfo? {
        val pending = pendingDeletesByNoteId.remove(noteId) ?: return null
        if (notesStore.getById(noteId) == null) {
            notesStore.upsert(pending)
            onNotesWritten()
        }
        return pending
    }

    fun finalizeDelete(noteId: Long) {
        if (isQuickNote(noteId)) return
        pendingDeletesByNoteId.remove(noteId)
        notesPreferences.unpinNote(noteId)
        PinnedNotifications.removeNote(appContext, noteId)
    }

    fun pinNote(noteId: Long): Set<Long> = notesPreferences.pinNote(noteId)

    fun unpinNote(noteId: Long): Set<Long> {
        return notesPreferences.unpinNote(noteId)
    }

    fun getPinnedNoteOrder(): List<Long> = notesPreferences.getPinnedNoteOrder()

    fun setPinnedNoteOrder(order: List<Long>): List<Long> = notesPreferences.setPinnedNoteOrder(order)

    fun isPinned(noteId: Long): Boolean = notesPreferences.getPinnedNoteIds().contains(noteId)

    fun getPinnedNoteIds(): Set<Long> {
        ensureQuickNoteExists()
        return notesPreferences.getPinnedNoteIds()
    }

    /** The Quick Note, showing Noted's Quick Note text while Noted Integration is on. Blocking. */
    fun getOrCreateQuickNote(): NoteInfo = NotedQuickNoteMirror.withNotedText(appContext, ensureQuickNoteExists())

    fun isQuickNote(noteId: Long): Boolean = noteId > 0L && notesPreferences.getQuickNoteId() == noteId

    private fun readNotes(): List<NoteInfo> {
        return notesStore.getAll().also { notes ->
            notes.maxOfOrNull(NoteInfo::noteId)?.let { notesPreferences.ensureNoteIdCounterAtLeast(it + 1L) }
        }
    }

    private fun onNotesWritten() {
        requestNoteWidgetsRefresh(appContext)
    }

    private fun ensureQuickNoteExists(): NoteInfo {
        val storedQuickNoteId = notesPreferences.getQuickNoteId()
        // Usual case: the stored Quick Note exists with its title, so one row answers it. Anything
        // else falls through to the full check below, which repairs it.
        if (storedQuickNoteId > 0L) {
            notesStore.getById(storedQuickNoteId)
                ?.takeIf { it.title == quickNoteTitle }
                ?.let { return it }
        }
        val notes = readNotes()
        var changed = false

        var quickNote =
            notes.firstOrNull { it.noteId == storedQuickNoteId && storedQuickNoteId > 0L }
                ?: notes.firstOrNull { it.title.trim() == quickNoteTitle }

        if (quickNote == null) {
            val now = System.currentTimeMillis()
            quickNote =
                NoteInfo(
                    noteId = notesPreferences.nextNoteId(),
                    title = quickNoteTitle,
                    markdownContent = "",
                    createdAtMillis = now,
                    updatedAtMillis = now,
                )
            changed = true
        } else if (quickNote.title != quickNoteTitle) {
            val updatedQuickNote =
                quickNote.copy(
                    title = quickNoteTitle,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            quickNote = updatedQuickNote
            changed = true
        }

        if (notesPreferences.getQuickNoteId() != quickNote.noteId) {
            notesPreferences.setQuickNoteId(quickNote.noteId)
        }

        if (changed) {
            notesStore.upsert(quickNote)
            onNotesWritten()
        }

        return quickNote
    }

    companion object {
        /** Most notes a regular search returns; the collapsed card shows only the first. */
        const val SEARCH_RESULT_LIMIT = 25

        /** Most notes the Notes alias search returns. */
        const val ALIAS_SEARCH_RESULT_LIMIT = 500
    }
}
