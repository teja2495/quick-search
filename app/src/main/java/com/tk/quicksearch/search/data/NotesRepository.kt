package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesTextUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class NotesRepository(
    context: Context,
) {
    private val notesPreferences = NotesPreferences(context)
    private val pendingDeletesByNoteId = mutableMapOf<Long, NoteInfo>()

    fun getAllNotes(): List<NoteInfo> {
        val pinnedIds = getPinnedNoteIds()
        return readNotes()
            .sortedWith(
                compareByDescending<NoteInfo> { pinnedIds.contains(it.noteId) }
                    .thenByDescending { it.updatedAtMillis }
                    .thenBy { it.title.lowercase(Locale.getDefault()) },
            )
    }

    fun getNoteById(noteId: Long): NoteInfo? = readNotes().firstOrNull { it.noteId == noteId }

    fun searchNotes(query: String): List<NoteInfo> {
        val normalizedQuery = NotesTextUtils.normalize(query)
        if (normalizedQuery.isBlank()) return getAllNotes()

        val pinnedIds = getPinnedNoteIds()
        return readNotes()
            .mapNotNull { note ->
                val normalizedTitle = NotesTextUtils.normalize(note.title)
                val normalizedBody = NotesTextUtils.normalize(
                    NotesTextUtils.toSearchablePlainText(note.markdownContent),
                )
                val titleStartsWith = normalizedTitle.startsWith(normalizedQuery)
                val titleContains = normalizedTitle.contains(normalizedQuery)
                val bodyContains = normalizedBody.contains(normalizedQuery)
                if (!titleContains && !bodyContains) return@mapNotNull null
                val score =
                    when {
                        titleStartsWith -> 400
                        titleContains -> 300
                        bodyContains -> 200
                        else -> 0
                    } +
                        if (pinnedIds.contains(note.noteId)) 20 else 0
                note to score
            }.sortedWith(
                compareByDescending<Pair<NoteInfo, Int>> { it.second }
                    .thenByDescending { it.first.updatedAtMillis },
            ).map { it.first }
    }

    fun createNote(
        title: String,
        markdownContent: String,
    ): NoteInfo {
        val now = System.currentTimeMillis()
        val note =
            NoteInfo(
                noteId = notesPreferences.nextNoteId(),
                title = title.trim(),
                markdownContent = markdownContent,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
        val notes = readNotes().toMutableList().apply { add(note) }
        writeNotes(notes)
        return note
    }

    fun updateNote(
        noteId: Long,
        title: String,
        markdownContent: String,
    ): NoteInfo? {
        val notes = readNotes().toMutableList()
        val index = notes.indexOfFirst { it.noteId == noteId }
        if (index == -1) return null
        val current = notes[index]
        val updated =
            current.copy(
                title = title.trim(),
                markdownContent = markdownContent,
                updatedAtMillis = System.currentTimeMillis(),
            )
        notes[index] = updated
        writeNotes(notes)
        return updated
    }

    fun stageDelete(noteId: Long): NoteInfo? {
        val notes = readNotes().toMutableList()
        val note = notes.firstOrNull { it.noteId == noteId } ?: return null
        pendingDeletesByNoteId[noteId] = note
        writeNotes(notes.filterNot { it.noteId == noteId })
        return note
    }

    fun undoDelete(noteId: Long): NoteInfo? {
        val pending = pendingDeletesByNoteId.remove(noteId) ?: return null
        val notes = readNotes().toMutableList()
        if (notes.none { it.noteId == noteId }) {
            notes.add(pending)
            writeNotes(notes)
        }
        return pending
    }

    fun finalizeDelete(noteId: Long) {
        pendingDeletesByNoteId.remove(noteId)
        notesPreferences.unpinNote(noteId)
    }

    fun pinNote(noteId: Long): Set<Long> = notesPreferences.pinNote(noteId)

    fun unpinNote(noteId: Long): Set<Long> = notesPreferences.unpinNote(noteId)

    fun isPinned(noteId: Long): Boolean = notesPreferences.getPinnedNoteIds().contains(noteId)

    fun getPinnedNoteIds(): Set<Long> = notesPreferences.getPinnedNoteIds()

    private fun readNotes(): List<NoteInfo> {
        val raw = notesPreferences.getNotesJson()
        if (raw.isBlank()) return emptyList()
        val jsonArray = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val notes = mutableListOf<NoteInfo>()
        for (index in 0 until jsonArray.length()) {
            val entry = jsonArray.optJSONObject(index) ?: continue
            val noteId = entry.optLong(FIELD_NOTE_ID, -1L)
            if (noteId <= 0L) continue
            val title = entry.optString(FIELD_TITLE).orEmpty()
            val markdown = entry.optString(FIELD_MARKDOWN).orEmpty()
            val createdAt = entry.optLong(FIELD_CREATED_AT, 0L)
            val updatedAt = entry.optLong(FIELD_UPDATED_AT, createdAt)
            notes.add(
                NoteInfo(
                    noteId = noteId,
                    title = title,
                    markdownContent = markdown,
                    createdAtMillis = createdAt,
                    updatedAtMillis = updatedAt,
                ),
            )
            notesPreferences.ensureNoteIdCounterAtLeast(noteId + 1L)
        }
        return notes
    }

    private fun writeNotes(notes: List<NoteInfo>) {
        val jsonArray = JSONArray()
        notes.forEach { note ->
            jsonArray.put(
                JSONObject()
                    .put(FIELD_NOTE_ID, note.noteId)
                    .put(FIELD_TITLE, note.title)
                    .put(FIELD_MARKDOWN, note.markdownContent)
                    .put(FIELD_CREATED_AT, note.createdAtMillis)
                    .put(FIELD_UPDATED_AT, note.updatedAtMillis),
            )
        }
        notesPreferences.setNotesJson(jsonArray.toString())
    }

    private companion object {
        const val FIELD_NOTE_ID = "noteId"
        const val FIELD_TITLE = "title"
        const val FIELD_MARKDOWN = "markdown"
        const val FIELD_CREATED_AT = "createdAtMillis"
        const val FIELD_UPDATED_AT = "updatedAtMillis"
    }
}
