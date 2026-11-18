package com.tk.quicksearch.search.data.notes

import android.content.Context
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.models.NoteInfo
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

/** Stores notes and serializes database access on a dedicated executor. */
class NotesRoomStore(context: Context) {
    private val appContext = context.applicationContext
    private val dao = NotesDatabase.get(appContext).notesDao()

    fun getAll(): List<NoteInfo> = onDatabaseThread {
        dao.getAll().map(NoteEntity::toModel)
    }

    fun getById(noteId: Long): NoteInfo? = onDatabaseThread {
        dao.getById(noteId)?.toModel()
    }

    fun getByIds(noteIds: Collection<Long>): List<NoteInfo> {
        if (noteIds.isEmpty()) return emptyList()
        return onDatabaseThread {
            dao.getByIds(noteIds).map(NoteEntity::toModel)
        }
    }

    fun replaceAll(notes: List<NoteInfo>) = onDatabaseThread {
        dao.replaceAll(notes.map(NoteInfo::toEntity))
    }

    /** Inserts [note] or replaces the stored note with its id, leaving every other row alone. */
    fun upsert(note: NoteInfo) = onDatabaseThread {
        dao.upsert(note.toEntity())
    }

    fun deleteById(noteId: Long) = onDatabaseThread {
        dao.deleteById(noteId)
    }

    /** The highest stored note id, or null when there are no notes. */
    fun maxNoteId(): Long? = onDatabaseThread {
        dao.maxNoteId()
    }

    fun hasAny(): Boolean = onDatabaseThread {
        dao.hasAny()
    }

    private fun <T> onDatabaseThread(block: () -> T): T =
        if (Thread.currentThread().name.startsWith(DB_THREAD_PREFIX)) {
            clearMigratedNotesSnapshot()
            block()
        } else {
            databaseExecutor.submit<T> {
                clearMigratedNotesSnapshot()
                block()
            }.get()
        }

    private fun clearMigratedNotesSnapshot() {
        if (checkedLegacySnapshot) return
        val migrationPrefs = appContext.getSharedPreferences("notes_store_state", Context.MODE_PRIVATE)
        if (migrationPrefs.getInt("schema_version", 0) >= 1) {
            appContext.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(BasePreferences.KEY_NOTES_DATA).apply()
        }
        checkedLegacySnapshot = true
    }

    companion object {
        /** [notes] in the legacy JSON form that backups carry in the preferences section. */
        fun serializeLegacySnapshot(notes: List<NoteInfo>): String =
            JSONArray().apply {
                notes.forEach { note ->
                    put(
                        JSONObject()
                            .put("noteId", note.noteId)
                            .put("title", note.title)
                            .put("markdown", note.markdownContent)
                            .put("createdAtMillis", note.createdAtMillis)
                            .put("updatedAtMillis", note.updatedAtMillis),
                    )
                }
            }.toString()

        private var checkedLegacySnapshot = false
        private const val DB_THREAD_PREFIX = "notes-store"
        private val databaseExecutor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "$DB_THREAD_PREFIX-1").apply { isDaemon = true }
            }
    }
}
