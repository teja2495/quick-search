package com.tk.quicksearch.search.notificationHistory

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** A single notification as it was posted, captured for the Notification History screen. */
data class NotificationHistoryEntry(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    /** Whether At a Glance's App notifications could show it, whatever its apps and keywords. */
    val appNotificationCandidate: Boolean = true,
    /** The database row's id; 0 for postings not read back from the database. */
    val id: Long = 0,
) {
    /** Identifies this exact posting, used to find its live tap action. */
    internal fun contentIntentKey(): String = "$key|$postTime"
}

/**
 * Bounded, persisted log of the notifications seen by the shared notification listener service.
 *
 * Rows live in Room so each posting is a small insert on a background thread instead of rewriting
 * the whole log. History can only accumulate while notification listener access is granted; the
 * platform exposes no way to backfill notifications posted before that.
 */
object NotificationHistoryStore {
    private const val PREFS_NAME = "notification_history"
    private const val KEY_HIDDEN_PACKAGES = "hidden_packages"
    private const val MAX_ENTRIES = 500

    // An identical re-post within this window (a silent update) does not refresh the row's time.
    private const val REPEAT_POSTING_WINDOW_MILLIS = 60_000L

    // Hidden until the user opts in; System UI mostly posts transient status notifications.
    // Always listed in the app filter so it can be enabled before it has any history.
    val DEFAULT_HIDDEN_PACKAGES = setOf("com.android.systemui")

    private val hiddenPackagesState = MutableStateFlow<Set<String>>(emptySet())

    // A single thread keeps writes in posting order without blocking the listener's main thread.
    // It ends after a short idle spell instead of living for the whole process.
    private val databaseExecutor =
        ThreadPoolExecutor(1, 1, DATABASE_THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, LinkedBlockingQueue())
            .apply { allowCoreThreadTimeOut(true) }

    // PendingIntents can't be persisted, so tap actions only survive while this process is alive.
    // Bounded like the log itself; the oldest postings drop out first.
    private val contentIntents =
        object : LinkedHashMap<String, Pair<String, PendingIntent>>() {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, Pair<String, PendingIntent>>?,
            ): Boolean = size > MAX_ENTRIES
        }

    // Last posting recorded per notification key in this process, bounded like the log. Ongoing and
    // progress notifications (downloads, navigation, media) re-post constantly; only their first
    // posting is kept, so their updates neither flood the log nor each cost a database write.
    // Only a fingerprint is kept, not the text itself, since this lives as long as the process.
    private val lastRecorded =
        object : LinkedHashMap<String, RecordedPosting>() {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, RecordedPosting>?,
            ): Boolean = size > MAX_ENTRIES
        }

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var dao: NotificationHistoryDao? = null

    /** Apps whose notifications are hidden from history and no longer recorded. */
    val hiddenPackages: StateFlow<Set<String>> = hiddenPackagesState.asStateFlow()

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        val loaded = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dao = NotificationHistoryDatabase.get(appContext).notificationHistoryDao()
        val storedHidden = loaded.getStringSet(KEY_HIDDEN_PACKAGES, null)?.toSet()
        hiddenPackagesState.value = storedHidden ?: DEFAULT_HIDDEN_PACKAGES
        if (storedHidden == null) {
            // Persist the defaults once so re-enabling a default app sticks.
            loaded.edit().putStringSet(KEY_HIDDEN_PACKAGES, DEFAULT_HIDDEN_PACKAGES).apply()
        }
        prefs = loaded
    }

    /**
     * Newest-first history, re-queried only while collected and only when the log changes. Loading,
     * mapping and comparing rows all happen off the main thread.
     */
    fun entries(context: Context): Flow<List<NotificationHistoryEntry>> {
        val appContext = context.applicationContext
        return flow {
            withContext(Dispatchers.IO) { ensureLoaded(appContext) }
            emitAll(requireDao().observeRecent(MAX_ENTRIES))
        }.map { rows -> rows.map(NotificationHistoryEntity::toModel) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    fun setPackageHidden(
        context: Context,
        packageName: String,
        hidden: Boolean,
    ) {
        ensureLoaded(context)
        synchronized(this) {
            val updated =
                if (hidden) {
                    hiddenPackagesState.value + packageName
                } else {
                    hiddenPackagesState.value - packageName
                }
            if (updated == hiddenPackagesState.value) return
            hiddenPackagesState.value = updated
            prefs?.edit()?.putStringSet(KEY_HIDDEN_PACKAGES, updated)?.apply()
        }
        if (hidden) {
            // Hiding an app discards what was already captured from it, not just future posts.
            synchronized(contentIntents) {
                contentIntents.values.removeAll { (owner, _) -> owner == packageName }
            }
            onDatabaseThread { deleteByPackage(packageName) }
        }
    }

    fun contentIntentFor(entry: NotificationHistoryEntry): PendingIntent? =
        synchronized(contentIntents) { contentIntents[entry.contentIntentKey()]?.second }

    /**
     * Records a single newly posted notification. [isAppNotificationCandidate] tells whether At a
     * Glance's App notifications could show it, for its Recent matches.
     */
    fun record(
        context: Context,
        notification: StatusBarNotification?,
        isAppNotificationCandidate: (StatusBarNotification) -> Boolean,
    ) {
        if (notification == null) return
        ensureLoaded(context)
        // Cheap checks first: most postings are re-posts of ongoing notifications or hidden apps.
        if (notification.packageName.orEmpty() in hiddenPackagesState.value) return
        if (notification.isOngoingOrProgress()) {
            val key = runCatching { notification.key }.getOrNull().orEmpty()
            if (synchronized(lastRecorded) { key in lastRecorded }) return
        }
        val parsed = notification.toHistoryEntry() ?: return
        if (isRedundantPosting(parsed, notification)) return
        val entry = parsed.withCandidate(notification, isAppNotificationCandidate)
        rememberContentIntent(entry, notification)
        append(listOf(entry))
    }

    /**
     * Seeds the log from the currently active notifications so a freshly granted screen is not
     * blank on first open.
     */
    fun seed(
        context: Context,
        notifications: Array<StatusBarNotification>?,
        isAppNotificationCandidate: (StatusBarNotification) -> Boolean,
    ) {
        ensureLoaded(context)
        val hidden = hiddenPackagesState.value
        val seeded =
            notifications.orEmpty().mapNotNull { notification ->
                notification.toHistoryEntry()
                    ?.takeUnless { it.packageName in hidden }
                    ?.withCandidate(notification, isAppNotificationCandidate)
                    ?.also { entry ->
                        synchronized(lastRecorded) { lastRecorded[entry.key] = entry.toRecordedPosting() }
                        rememberContentIntent(entry, notification)
                    }
            }
        if (seeded.isEmpty()) return
        append(seeded)
    }

    /** Deletes a single history row. */
    fun remove(
        context: Context,
        entry: NotificationHistoryEntry,
    ) {
        ensureLoaded(context)
        synchronized(contentIntents) { contentIntents.remove(entry.contentIntentKey()) }
        onDatabaseThread { delete(entry.key, entry.title, entry.text) }
    }

    fun clear(context: Context) {
        ensureLoaded(context)
        synchronized(lastRecorded) { lastRecorded.clear() }
        synchronized(contentIntents) { contentIntents.clear() }
        onDatabaseThread { deleteAll() }
    }

    private fun append(additions: List<NotificationHistoryEntry>) {
        val rows = additions.map(NotificationHistoryEntry::toEntity)
        onDatabaseThread { record(rows, MAX_ENTRIES) }
    }

    /** Whether [entry] repeats the last recorded posting of its notification; otherwise remembers it. */
    private fun isRedundantPosting(
        entry: NotificationHistoryEntry,
        notification: StatusBarNotification,
    ): Boolean =
        synchronized(lastRecorded) {
            val previous = lastRecorded[entry.key]
            val redundant =
                previous != null &&
                    (
                        notification.isOngoingOrProgress() ||
                            (
                                previous.hasSameContent(entry) &&
                                    entry.postTime - previous.postTime < REPEAT_POSTING_WINDOW_MILLIS
                            )
                    )
            if (!redundant) lastRecorded[entry.key] = entry.toRecordedPosting()
            redundant
        }

    private fun rememberContentIntent(
        entry: NotificationHistoryEntry,
        notification: StatusBarNotification,
    ) {
        val intent = runCatching { notification.notification?.contentIntent }.getOrNull() ?: return
        synchronized(contentIntents) {
            contentIntents[entry.contentIntentKey()] = entry.packageName to intent
        }
    }

    private fun requireDao(): NotificationHistoryDao =
        checkNotNull(dao) { "NotificationHistoryStore.ensureLoaded must be called first" }

    private fun onDatabaseThread(block: NotificationHistoryDao.() -> Unit) {
        val target = requireDao()
        databaseExecutor.execute {
            runCatching { target.block() }
                .onFailure { Log.w(TAG, "Notification history write failed", it) }
        }
    }

    private const val TAG = "NotificationHistory"
    private const val DATABASE_THREAD_KEEP_ALIVE_SECONDS = 30L
}

/** What the repeat check needs of a recorded posting, without holding on to its text. */
private class RecordedPosting(
    val titleHash: Int,
    val titleLength: Int,
    val textHash: Int,
    val textLength: Int,
    val postTime: Long,
) {
    fun hasSameContent(entry: NotificationHistoryEntry): Boolean =
        titleLength == entry.title.length &&
            textLength == entry.text.length &&
            titleHash == entry.title.hashCode() &&
            textHash == entry.text.hashCode()
}

private fun NotificationHistoryEntry.toRecordedPosting() =
    RecordedPosting(title.hashCode(), title.length, text.hashCode(), text.length, postTime)

private fun NotificationHistoryEntry.withCandidate(
    notification: StatusBarNotification,
    isAppNotificationCandidate: (StatusBarNotification) -> Boolean,
): NotificationHistoryEntry =
    copy(appNotificationCandidate = runCatching { isAppNotificationCandidate(notification) }.getOrDefault(false))

private fun StatusBarNotification.isOngoingOrProgress(): Boolean {
    val posted = runCatching { notification }.getOrNull() ?: return false
    if (posted.flags and Notification.FLAG_ONGOING_EVENT != 0) return true
    val extras = runCatching { posted.extras }.getOrNull() ?: return false
    return extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
        extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
}

/** The posting's content; [NotificationHistoryEntry.appNotificationCandidate] is set by [withCandidate]. */
private fun StatusBarNotification.toHistoryEntry(): NotificationHistoryEntry? {
    val posted = runCatching { notification }.getOrNull() ?: return null
    if (posted.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

    val extras = runCatching { posted.extras }.getOrNull()
    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
    // Prefer the expanded content (big text / inbox lines) so history shows the full notification.
    val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
    val textLines =
        extras
            ?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { line -> line?.toString()?.trim()?.takeIf { it.isNotEmpty() } }
            ?.joinToString("\n")
    val plainText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
    val text =
        listOf(bigText, textLines, plainText)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
    if (title.isBlank() && text.isBlank()) return null

    return NotificationHistoryEntry(
        key = runCatching { key }.getOrNull().orEmpty(),
        packageName = packageName.orEmpty(),
        title = title,
        text = text,
        postTime = postTime,
    )
}
