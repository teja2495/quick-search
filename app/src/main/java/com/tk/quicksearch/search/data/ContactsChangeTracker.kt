package com.tk.quicksearch.search.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.ContactsContract
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide change counter for ContactsContract. Contact caches (search snapshot, hydrated
 * details, avatar bitmaps) compare against it instead of each registering their own observer.
 */
internal object ContactsChangeTracker {
    private val generation = AtomicLong()
    private val isObserverRegistered = AtomicBoolean(false)

    /** Current generation, registering the provider observer on first use. */
    fun generation(contentResolver: ContentResolver): Long {
        ensureObserverRegistered(contentResolver)
        return generation.get()
    }

    /** Current generation without registering the observer (for UI-side caches). */
    fun currentGeneration(): Long = generation.get()

    /** Treats contacts as changed, e.g. after the user asks to reload them. */
    fun markChanged() {
        generation.incrementAndGet()
    }

    private fun ensureObserverRegistered(contentResolver: ContentResolver) {
        if (!isObserverRegistered.compareAndSet(false, true)) return
        // A null handler delivers onChange on a binder thread; it only bumps a counter.
        val observer =
            object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    generation.incrementAndGet()
                }
            }
        val registered =
            runCatching {
                contentResolver.registerContentObserver(ContactsContract.AUTHORITY_URI, true, observer)
            }.isSuccess
        // Without an observer the snapshot TTL alone bounds staleness; retry registration next use.
        if (!registered) isObserverRegistered.set(false)
    }
}
