package com.tk.quicksearch.search.data.notes

import android.content.Context
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.models.NoteInfo
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

/** Room-backed notes with an idempotent, transactional migration from the legacy JSON value. */
class NotesRoomStore(context: Context) {
    private val appContext = context.applicationContext
    private val dao = NotesDatabase.get(appContext).notesDao()
    private val migrationPrefs =
        appContext.getSharedPreferences(MIGRATION_PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPreferences = NotesPreferences(appContext)

    fun getAll(): List<NoteInfo> = onDatabaseThread {
        ensureMigrated()
        dao.getAll().map(NoteEntity::toModel)
    }

    fun getById(noteId: Long): NoteInfo? = onDatabaseThread {
        ensureMigrated()
        dao.getById(noteId)?.toModel()
    }

    fun getByIds(noteIds: Collection<Long>): List<NoteInfo> {
        if (noteIds.isEmpty()) return emptyList()
        return onDatabaseThread {
            ensureMigrated()
            dao.getByIds(noteIds).map(NoteEntity::toModel)
        }
    }

    fun replaceAll(notes: List<NoteInfo>) = onDatabaseThread {
        ensureMigrated()
        dao.replaceAll(notes.map(NoteInfo::toEntity))
    }

    /** Inserts [note] or replaces the stored note with its id, leaving every other row alone. */
    fun upsert(note: NoteInfo) = onDatabaseThread {
        ensureMigrated()
        dao.upsert(note.toEntity())
    }

    fun deleteById(noteId: Long) = onDatabaseThread {
        ensureMigrated()
        dao.deleteById(noteId)
    }

    /** The highest stored note id, or null when there are no notes. */
    fun maxNoteId(): Long? = onDatabaseThread {
        ensureMigrated()
        dao.maxNoteId()
    }

    fun hasAny(): Boolean = onDatabaseThread {
        ensureMigrated()
        dao.hasAny()
    }

    fun replaceFromBackup(notes: List<NoteInfo>) = onDatabaseThread {
        dao.replaceAll(notes.map(NoteInfo::toEntity))
        migrationPrefs.edit().putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).commit()
    }

    fun parseLegacySnapshot(raw: String): List<NoteInfo> = parseLegacyNotes(raw)

    private fun ensureMigrated() {
        if (migrationPrefs.getInt(KEY_SCHEMA_VERSION, 0) < SCHEMA_VERSION) {
            synchronized(migrationLock) {
                if (migrationPrefs.getInt(KEY_SCHEMA_VERSION, 0) < SCHEMA_VERSION) {
                    val legacyNotes = parseLegacyNotes(legacyPreferences.getNotesJson())
                    // replaceAll is one Room transaction. The completion marker is written only after it
                    // commits, so process death safely retries the migration.
                    dao.replaceAll(legacyNotes.map(NoteInfo::toEntity))
                    migrationPrefs.edit().putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).commit()
                }
            }
        }
        clearLegacySnapshotOnce()
    }

    /**
     * Room is the only notes store once migrated. Older builds also kept every note body as JSON in
     * the main preferences file, which startup parses in full, so drop that stale copy once.
     */
    private fun clearLegacySnapshotOnce() {
        if (legacySnapshotCleared) return
        legacySnapshotCleared = true
        legacyPreferences.clearNotesJson()
    }

    private fun parseLegacyNotes(raw: String): List<NoteInfo> {
        if (raw.isBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val noteId = entry.optLong("noteId", -1L)
                if (noteId <= 0L) continue
                val createdAt = entry.optLong("createdAtMillis", 0L)
                add(
                    NoteInfo(
                        noteId = noteId,
                        title = entry.optString("title").orEmpty(),
                        markdownContent = entry.optString("markdown").orEmpty(),
                        createdAtMillis = createdAt,
                        updatedAtMillis = entry.optLong("updatedAtMillis", createdAt),
                    ),
                )
            }
        }
    }

    private fun <T> onDatabaseThread(block: () -> T): T =
        if (Thread.currentThread().name.startsWith(DB_THREAD_PREFIX)) {
            block()
        } else {
            databaseExecutor.submit<T>(block).get()
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

        private const val MIGRATION_PREFS_NAME = "notes_store_state"
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val SCHEMA_VERSION = 1
        private const val DB_THREAD_PREFIX = "notes-store"
        private val migrationLock = Any()
        private @Volatile var legacySnapshotCleared = false
        private val databaseExecutor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "$DB_THREAD_PREFIX-1").apply { isDaemon = true }
            }
    }
}
