package com.tk.quicksearch.search.data

import android.content.ContentResolver
import android.os.SystemClock
import android.util.Log
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.utils.SearchTextCache
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** A contact with a phone number plus the lowercased names used for in-memory matching. */
internal class ContactSearchEntry(
    val contact: ContactInfo,
    primaryName: String,
    alternativeName: String?,
) {
    val lowerPrimaryName = primaryName.lowercase(Locale.getDefault())
    val lowerAlternativeName = alternativeName?.lowercase(Locale.getDefault())
    val compactPrimaryName = lowerPrimaryName.replace(" ", "")
    val compactAlternativeName = lowerAlternativeName?.replace(" ", "")
}

/**
 * Process-wide snapshot of every contact with a phone number, sorted by display name. Typing
 * reuses it for name-token matching and fuzzy candidates instead of re-reading the provider on
 * each keystroke. A provider change ([ContactsChangeTracker]) or the TTL marks it stale; a stale
 * snapshot keeps serving while one background reload replaces it, so a keystroke never waits on a
 * full provider scan once a snapshot exists.
 */
internal object ContactSearchSnapshot {
    // The observer catches real changes; the TTL only bounds staleness if it failed to register.
    private const val MAX_AGE_MILLIS = 30L * 60L * 1000L

    private class Cached(
        val entries: List<ContactSearchEntry>,
        val loadedAtElapsedMillis: Long,
        val changeGeneration: Long,
    )

    private val loadLock = Any()
    private val isBackgroundReloadQueued = AtomicBoolean(false)
    private val backgroundExecutor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "ContactSearchSnapshot").apply { isDaemon = true }
        }
    }
    @Volatile private var cached: Cached? = null

    /** Prepared (normalized) contact names and nicknames for ranking; cleared with the snapshot. */
    val textCache = SearchTextCache()

    fun get(
        contentResolver: ContentResolver,
        load: () -> List<ContactSearchEntry>,
    ): List<ContactSearchEntry> {
        val generation = ContactsChangeTracker.generation(contentResolver)
        cached?.let { snapshot ->
            if (!snapshot.isFresh(generation)) reloadInBackground(contentResolver, load)
            return snapshot.entries
        }
        // One load at a time; concurrent keystrokes reuse it instead of each reading every contact.
        synchronized(loadLock) {
            cached?.let { return it.entries }
            return loadLocked(contentResolver, load)
        }
    }

    /** Builds the snapshot ahead of the first search, or refreshes it when stale. */
    fun prewarm(
        contentResolver: ContentResolver,
        load: () -> List<ContactSearchEntry>,
    ) {
        val generation = ContactsChangeTracker.generation(contentResolver)
        if (cached?.isFresh(generation) == true) return
        synchronized(loadLock) {
            if (cached?.isFresh(ContactsChangeTracker.generation(contentResolver)) == true) return
            loadLocked(contentResolver, load)
        }
    }

    /** Drops the snapshot so the next read reloads it (memory pressure or permission loss). */
    fun release() {
        cached = null
        textCache.clear()
    }

    private fun Cached.isFresh(generation: Long): Boolean {
        val age = SystemClock.elapsedRealtime() - loadedAtElapsedMillis
        return changeGeneration == generation && age in 0 until MAX_AGE_MILLIS
    }

    private fun loadLocked(
        contentResolver: ContentResolver,
        load: () -> List<ContactSearchEntry>,
    ): List<ContactSearchEntry> {
        val generation = ContactsChangeTracker.generation(contentResolver)
        val startElapsed = SystemClock.elapsedRealtime()
        val entries = load()
        SnapshotLoadLog.loaded("Contact search snapshot", entries.size, startElapsed)
        cached = Cached(entries, startElapsed, generation)
        // Names that left the snapshot shouldn't stay prepared forever.
        textCache.clear()
        return entries
    }

    private fun reloadInBackground(
        contentResolver: ContentResolver,
        load: () -> List<ContactSearchEntry>,
    ) {
        if (!isBackgroundReloadQueued.compareAndSet(false, true)) return
        backgroundExecutor.execute {
            try {
                synchronized(loadLock) {
                    val current = cached
                    if (current == null || !current.isFresh(ContactsChangeTracker.generation(contentResolver))) {
                        loadLocked(contentResolver, load)
                    }
                }
            } catch (_: SecurityException) {
                // Permission revoked: stop serving contacts that can no longer be read.
                release()
            } catch (exception: Exception) {
                Log.w(TAG, "Contact search snapshot reload failed", exception)
            } finally {
                isBackgroundReloadQueued.set(false)
            }
        }
    }

    /** Every whitespace token appears in one of the names, or the space-free query appears in one. */
    fun findByNameTokens(
        entries: List<ContactSearchEntry>,
        query: String,
        limit: Int,
        excludedContactIds: Set<Long>,
    ): List<ContactInfo> {
        if (limit <= 0) return emptyList()
        val lowerQuery = query.lowercase(Locale.getDefault())
        val tokens = lowerQuery.split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        val compactQuery = lowerQuery.replace(" ", "")
        val results = ArrayList<ContactInfo>(minOf(limit, 16))
        for (entry in entries) {
            if (entry.contact.contactId in excludedContactIds) continue
            val matchesTokens =
                tokens.all { token ->
                    entry.lowerPrimaryName.contains(token) ||
                        entry.lowerAlternativeName?.contains(token) == true
                }
            val matchesCompact =
                entry.compactPrimaryName.contains(compactQuery) ||
                    entry.compactAlternativeName?.contains(compactQuery) == true
            if (matchesTokens || matchesCompact) {
                results.add(entry.contact)
                if (results.size >= limit) break
            }
        }
        return results
    }

    private const val TAG = "ContactSearchSnapshot"
}
