package com.tk.quicksearch.search.data.preferences

import android.content.SharedPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import java.util.concurrent.atomic.AtomicLong

/**
 * Stores a bounded list of recently opened non-query search results for ranking boosts.
 */
class RecentResultOpensPreferences(
    context: android.content.Context,
) : BasePreferences(context) {
    private val openCountsPrefs =
        context.applicationContext.getSharedPreferences(
            OPEN_COUNTS_PREFS_NAME,
            android.content.Context.MODE_PRIVATE,
        )
    private val lastOpenedPrefs =
        context.applicationContext.getSharedPreferences(
            LAST_OPENED_PREFS_NAME,
            android.content.Context.MODE_PRIVATE,
        )

    init {
        registerInvalidationListeners(sessionPrefs, openCountsPrefs, lastOpenedPrefs)
    }

    fun getRecentResultOpens(): List<RecentSearchEntry> {
        val rawItems =
            PreferenceUtils.getStringListPref(
                sessionPrefs,
                BasePreferences.KEY_RECENT_RESULT_OPENS,
            )
        return rawItems
            .mapNotNull { RecentSearchEntry.fromRaw(it) }
            .filter(::isRankableEntry)
    }

    /**
     * Ranking inputs for every search section, rebuilt only after an open, delete, or clear
     * instead of re-parsing the stored list and copying both stat maps on every query.
     */
    fun getRecencyIndex(): RecentResultRankingUtils.RecencyIndex {
        // Read the version before the data so a concurrent write always forces a rebuild later.
        val version = recencyVersion.get()
        cachedRecency?.let { (cachedVersion, index) ->
            if (cachedVersion == version) return index
        }
        val index =
            RecentResultRankingUtils.buildRecencyIndex(
                getRecentResultOpens(),
                getRecentResultOpenCounts(),
                getRecentResultLastOpenedTimes(),
            )
        cachedRecency = version to index
        return index
    }

    fun addRecentResultOpen(entry: RecentSearchEntry) {
        if (!isRankableEntry(entry)) return

        recordResultOpen(entry.stableKey)

        val currentItems = getRecentResultOpens().toMutableList()
        currentItems.removeAll { it.stableKey == entry.stableKey }
        currentItems.add(0, entry)

        val limited = currentItems.take(MAX_RECENT_RESULT_OPENS)
        PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_RESULT_OPENS,
            limited.map { it.toJsonString() },
        )
        invalidateRecencyIndex()
    }

    fun deleteRecentResultOpen(entry: RecentSearchEntry) {
        val currentItems = getRecentResultOpens().toMutableList()
        currentItems.removeAll { it.stableKey == entry.stableKey }
        PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_RESULT_OPENS,
            currentItems.map { it.toJsonString() },
        )
        openCountsPrefs.edit().remove(entry.stableKey).apply()
        lastOpenedPrefs.edit().remove(entry.stableKey).apply()
        invalidateRecencyIndex()
    }

    fun getRecentResultOpenCounts(): Map<String, Int> =
        openCountsPrefs.all.mapValues { (_, value) -> value as? Int ?: 0 }

    fun getRecentResultLastOpenedTimes(): Map<String, Long> =
        lastOpenedPrefs.all.mapValues { (_, value) -> value as? Long ?: 0L }

    fun recordCalendarEventOpen(eventId: Long) {
        recordResultOpen("calendar:$eventId")
        invalidateRecencyIndex()
    }

    fun clearRecentResultOpens() {
        PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_RESULT_OPENS,
            emptyList<String>(),
        )
        openCountsPrefs.edit().clear().apply()
        lastOpenedPrefs.edit().clear().apply()
        invalidateRecencyIndex()
    }

    private fun recordResultOpen(stableKey: String) {
        val currentCount = openCountsPrefs.getInt(stableKey, 0)
        openCountsPrefs.edit().putInt(stableKey, currentCount + 1).apply()
        lastOpenedPrefs.edit().putLong(stableKey, System.currentTimeMillis()).apply()
        pruneOpenStatsIfNeeded()
    }

    /**
     * Both stat files gain a key for every distinct item ever opened, and SharedPreferences keeps
     * each file in memory and rewrites it whole on every open. Past the cap, drop the items opened
     * longest ago; the slack keeps this from running on every open.
     */
    private fun pruneOpenStatsIfNeeded() {
        val countKeys = openCountsPrefs.all.keys
        val lastOpenedTimes = getRecentResultLastOpenedTimes()
        val allKeys = countKeys + lastOpenedTimes.keys
        if (allKeys.size <= MAX_OPEN_STATS) return
        val keysToRemove =
            allKeys
                .sortedBy { lastOpenedTimes[it] ?: 0L }
                .take(allKeys.size - OPEN_STATS_SIZE_AFTER_PRUNE)
        val countsEditor = openCountsPrefs.edit()
        val lastOpenedEditor = lastOpenedPrefs.edit()
        keysToRemove.forEach { key ->
            countsEditor.remove(key)
            lastOpenedEditor.remove(key)
        }
        countsEditor.apply()
        lastOpenedEditor.apply()
    }

    private fun isRankableEntry(entry: RecentSearchEntry): Boolean =
        entry is RecentSearchEntry.Contact ||
            entry is RecentSearchEntry.File ||
            entry is RecentSearchEntry.Setting ||
            entry is RecentSearchEntry.AppSetting ||
            entry is RecentSearchEntry.Note ||
            entry is RecentSearchEntry.AppShortcut

    companion object {
        private const val MAX_RECENT_RESULT_OPENS = 100
        private const val MAX_OPEN_STATS = 1000
        private const val OPEN_STATS_SIZE_AFTER_PRUNE = 800
        private const val OPEN_COUNTS_PREFS_NAME = "recent_result_open_counts"
        private const val LAST_OPENED_PREFS_NAME = "recent_result_last_opened"

        // Process-wide: every instance reads the same SharedPreferences files.
        private val recencyVersion = AtomicLong()

        @Volatile
        private var cachedRecency: Pair<Long, RecentResultRankingUtils.RecencyIndex>? = null

        private fun invalidateRecencyIndex() {
            recencyVersion.incrementAndGet()
        }

        // SharedPreferences holds listeners weakly, so keep strong references here. They catch
        // writes that bypass this class, such as a settings import.
        private val sessionListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == null || key == BasePreferences.KEY_RECENT_RESULT_OPENS) {
                    invalidateRecencyIndex()
                }
            }
        private val statsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> invalidateRecencyIndex() }

        @Volatile
        private var listenersRegistered = false

        private fun registerInvalidationListeners(
            sessionPrefs: SharedPreferences,
            openCountsPrefs: SharedPreferences,
            lastOpenedPrefs: SharedPreferences,
        ) {
            if (listenersRegistered) return
            synchronized(this) {
                if (listenersRegistered) return
                sessionPrefs.registerOnSharedPreferenceChangeListener(sessionListener)
                openCountsPrefs.registerOnSharedPreferenceChangeListener(statsListener)
                lastOpenedPrefs.registerOnSharedPreferenceChangeListener(statsListener)
                listenersRegistered = true
            }
        }
    }
}
