package com.tk.quicksearch.search.data.notes

import android.content.Context
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.models.NoteInfo
import java.util.concurrent.Executors
import org.json.JSONArray

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

    fun replaceFromBackup(notes: List<NoteInfo>) = onDatabaseThread {
        dao.replaceAll(notes.map(NoteInfo::toEntity))
        migrationPrefs.edit().putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).commit()
    }

    fun parseLegacySnapshot(raw: String): List<NoteInfo> = parseLegacyNotes(raw)

    private fun ensureMigrated() {
        if (migrationPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION) return
        synchronized(migrationLock) {
            if (migrationPrefs.getInt(KEY_SCHEMA_VERSION, 0) >= SCHEMA_VERSION) return
            val legacyNotes = parseLegacyNotes(legacyPreferences.getNotesJson())
            // replaceAll is one Room transaction. The completion marker is written only after it
            // commits, so process death safely retries the migration.
            dao.replaceAll(legacyNotes.map(NoteInfo::toEntity))
            migrationPrefs.edit().putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).commit()
        }
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

    private companion object {
        const val MIGRATION_PREFS_NAME = "notes_store_state"
        const val KEY_SCHEMA_VERSION = "schema_version"
        const val SCHEMA_VERSION = 1
        const val DB_THREAD_PREFIX = "notes-store"
        val migrationLock = Any()
        val databaseExecutor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "$DB_THREAD_PREFIX-1").apply { isDaemon = true }
            }
    }
}
