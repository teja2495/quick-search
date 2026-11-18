package com.tk.quicksearch.search.data.bookmarks

import android.content.Context
import com.tk.quicksearch.search.data.preferences.BookmarkPreferences
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.utils.PreparedSearchText
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** A deleted bookmark with its icon, kept so the deletion can be undone. */
data class DeletedBookmark(
    val bookmark: BookmarkInfo,
    val iconBase64: String?,
)

data class BookmarkImportResult(
    val added: Int,
    val skippedDuplicates: Int,
)

/**
 * Room-backed bookmarks with a process-wide in-memory copy, so search never hits the database per
 * keystroke. Every mutation goes through this class to keep that copy, the database, and the
 * favicon files consistent. Blocking calls run on a dedicated database thread.
 */
class BookmarkRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao by lazy { BookmarksDatabase.get(appContext).bookmarksDao() }
    private val faviconStore = BookmarkFaviconStore(appContext)
    private val preferences by lazy { BookmarkPreferences(appContext) }

    /** All bookmarks, sorted by name. Empty until something first reads them. */
    val bookmarks: StateFlow<List<BookmarkInfo>> = bookmarksState.asStateFlow()

    /** IDs of bookmarks whose favicon is downloading; a custom icon picked meanwhile would be overwritten. */
    val faviconFetchesInProgress: StateFlow<Set<Long>> = faviconFetchesState.asStateFlow()

    fun getAll(): List<BookmarkInfo> = ensureLoaded()

    /** Browser that new and imported bookmarks open in; null until [loadDefaultBrowserPackage] runs. */
    val defaultBrowserPackage: StateFlow<String?> = defaultBrowserState.asStateFlow()

    /** Reads the default browser into [defaultBrowserPackage]. Call off the main thread. */
    fun loadDefaultBrowserPackage(): String =
        preferences.getDefaultBrowserPackage().also { defaultBrowserState.value = it }

    fun setDefaultBrowserPackage(packageName: String) {
        defaultBrowserState.value = packageName
        preferences.setDefaultBrowserPackage(packageName)
    }

    /** Every bookmark in the standard bookmarks HTML format, icons included. Call off the main thread. */
    fun exportHtml(): String =
        BookmarkHtmlWriter.write(ensureLoaded()) { bookmark ->
            if (bookmark.hasFavicon) faviconStore.readBase64(bookmark.bookmarkId) else null
        }

    fun getById(bookmarkId: Long): BookmarkInfo? = indexOf(ensureLoaded()).byId[bookmarkId]

    fun hasBookmarks(): Boolean = ensureLoaded().isNotEmpty()

    /**
     * Bookmarks whose name or host matches [query], best match first, at most [resultLimit]. Names
     * and hosts are normalized once per bookmark list rather than on every keystroke.
     */
    fun search(
        query: String,
        resultLimit: Int = Int.MAX_VALUE,
    ): List<BookmarkInfo> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        if (normalizedQuery.isBlank() || resultLimit <= 0) return emptyList()
        val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }
        val compactQuery = SearchTextNormalizer.compactForSearch(normalizedQuery)
        return indexOf(ensureLoaded())
            .searchEntries
            .mapNotNull { entry ->
                val namePriority =
                    SearchRankingUtils.calculateMatchPriority(entry.name, normalizedQuery, tokens, compactQuery)
                val hostPriority =
                    SearchRankingUtils.calculateMatchPriority(entry.host, normalizedQuery, tokens, compactQuery)
                val priority = minOf(namePriority, hostPriority)
                if (SearchRankingUtils.isOtherMatch(priority)) null else entry to priority
            }
            .sortedWith(
                compareBy<Pair<BookmarkSearchEntry, Int>> { it.second }
                    .thenBy { it.first.sortName },
            )
            .take(resultLimit)
            .map { it.first.bookmark }
    }

    /**
     * Saves a new bookmark with [customIconBase64] as its icon, or fetches its favicon in the
     * background when there is none. Returns null for an invalid URL.
     */
    fun add(
        name: String,
        url: String,
        browserPackageName: String? = IN_APP_BROWSER_PACKAGE,
        customIconBase64: String? = null,
    ): BookmarkInfo? {
        val normalizedUrl = BookmarkUrls.normalize(url) ?: return null
        val draft =
            BookmarkInfo(
                bookmarkId = 0L,
                name = name.trim().ifBlank { BookmarkUrls.displayHost(normalizedUrl) },
                url = normalizedUrl,
                browserPackageName = browserPackageName,
                createdAtMillis = System.currentTimeMillis(),
            )
        val saved =
            onDatabaseThread {
                val inserted = draft.copy(bookmarkId = dao.insert(draft.toEntity()))
                saveCustomIcon(inserted.bookmarkId, customIconBase64)
                    ?.let { version -> inserted.copy(faviconVersion = version) }
                    ?: inserted
            }
        mutate { it + saved }
        if (!saved.hasFavicon) fetchFaviconsInBackground(listOf(saved))
        return saved
    }

    /**
     * Updates name, URL and browser, and the icon when [customIconBase64] is given. Otherwise a
     * changed URL refetches the favicon. Returns false for an invalid URL.
     */
    fun update(
        bookmarkId: Long,
        name: String,
        url: String,
        browserPackageName: String?,
        customIconBase64: String? = null,
    ): Boolean {
        val existing = getById(bookmarkId) ?: return false
        val normalizedUrl = BookmarkUrls.normalize(url) ?: return false
        val urlChanged = BookmarkUrls.dedupeKey(normalizedUrl) != BookmarkUrls.dedupeKey(existing.url)
        val refetchFavicon = urlChanged && customIconBase64 == null
        if (refetchFavicon) faviconStore.delete(bookmarkId)
        val customIconVersion = onDatabaseThread { saveCustomIcon(bookmarkId, customIconBase64) }
        val updated =
            existing.copy(
                name = name.trim().ifBlank { BookmarkUrls.displayHost(normalizedUrl) },
                url = normalizedUrl,
                browserPackageName = browserPackageName,
                faviconVersion = customIconVersion ?: if (refetchFavicon) 0L else existing.faviconVersion,
            )
        onDatabaseThread { dao.update(updated.toEntity()) }
        mutate { list -> list.map { if (it.bookmarkId == bookmarkId) updated else it } }
        if (refetchFavicon) fetchFaviconsInBackground(listOf(updated))
        return true
    }

    /** Stores a user-picked icon and returns its new favicon version, or null when there is none to store. */
    private fun saveCustomIcon(
        bookmarkId: Long,
        iconBase64: String?,
    ): Long? {
        if (iconBase64 == null || !faviconStore.saveBase64(bookmarkId, iconBase64)) return null
        val version = System.currentTimeMillis()
        dao.setFaviconVersion(bookmarkId, version)
        return version
    }

    fun delete(bookmarkId: Long) {
        pendingFaviconVersions.remove(bookmarkId)
        onDatabaseThread { dao.delete(bookmarkId) }
        faviconStore.delete(bookmarkId)
        mutate { list -> list.filterNot { it.bookmarkId == bookmarkId } }
    }

    /** Deletes a bookmark and returns what [restore] needs to bring it back, icon included. */
    fun deleteForUndo(bookmarkId: Long): DeletedBookmark? {
        val bookmark = getById(bookmarkId) ?: return null
        val icon = if (bookmark.hasFavicon) faviconStore.readBase64(bookmarkId) else null
        delete(bookmarkId)
        return DeletedBookmark(bookmark, icon)
    }

    /** Puts back a bookmark removed by [deleteForUndo], under its old ID. */
    fun restore(deleted: DeletedBookmark) {
        val original = deleted.bookmark
        if (getById(original.bookmarkId) != null) return
        val restored =
            onDatabaseThread {
                dao.insert(original.copy(faviconVersion = 0L).toEntity())
                original.copy(faviconVersion = saveCustomIcon(original.bookmarkId, deleted.iconBase64) ?: 0L)
            }
        mutate { it + restored }
        if (!restored.hasFavicon) fetchFaviconsInBackground(listOf(restored))
    }

    /** Removes every bookmark and its saved icon. */
    fun deleteAll() {
        pendingFaviconVersions.clear()
        onDatabaseThread { dao.deleteAll() }
        faviconStore.deleteAll()
        mutate { emptyList() }
    }

    /**
     * Adds websites that are not already saved (matched by URL), keeping icons that come with them
     * and fetching the rest in the background.
     */
    fun import(
        items: List<ImportedBookmark>,
        browserPackageName: String? = IN_APP_BROWSER_PACKAGE,
    ): BookmarkImportResult {
        val existingKeys = ensureLoaded().mapTo(HashSet()) { BookmarkUrls.dedupeKey(it.url) }
        val now = System.currentTimeMillis()
        val newItems = items.filter { existingKeys.add(BookmarkUrls.dedupeKey(it.url)) }
        if (newItems.isEmpty()) return BookmarkImportResult(added = 0, skippedDuplicates = items.size)

        val inserted =
            onDatabaseThread {
                val ids =
                    dao.insertAll(
                        newItems.map { item ->
                            BookmarkEntity(
                                name = item.name.trim().ifBlank { BookmarkUrls.displayHost(item.url) },
                                url = item.url.trim(),
                                browserPackageName = browserPackageName,
                                faviconVersion = 0L,
                                createdAtMillis = now,
                            )
                        },
                    )
                newItems.mapIndexed { index, item ->
                    val bookmarkId = ids[index]
                    val faviconVersion =
                        if (item.iconBase64 != null && faviconStore.saveBase64(bookmarkId, item.iconBase64)) {
                            dao.setFaviconVersion(bookmarkId, now)
                            now
                        } else {
                            0L
                        }
                    BookmarkInfo(
                        bookmarkId = bookmarkId,
                        name = item.name.trim().ifBlank { BookmarkUrls.displayHost(item.url) },
                        url = item.url.trim(),
                        browserPackageName = browserPackageName,
                        faviconVersion = faviconVersion,
                        createdAtMillis = now,
                    )
                }
            }
        mutate { it + inserted }
        fetchFaviconsInBackground(inserted)
        return BookmarkImportResult(added = inserted.size, skippedDuplicates = items.size - inserted.size)
    }

    /** Replaces every bookmark with a backup's, then fetches favicons, which backups don't hold. */
    fun replaceFromBackup(bookmarks: List<BookmarkInfo>) {
        val restored =
            bookmarks.filter { BookmarkUrls.isWebUrl(it.url) }.map { it.copy(faviconVersion = 0L) }
        pendingFaviconVersions.clear()
        onDatabaseThread { dao.replaceAll(restored.map(BookmarkInfo::toEntity)) }
        faviconStore.deleteAll()
        synchronized(stateLock) {
            isLoaded = true
            publish(restored)
        }
        fetchFaviconsInBackground(restored)
    }

    private fun fetchFaviconsInBackground(bookmarks: List<BookmarkInfo>) {
        val toFetch = bookmarks.filter { !it.hasFavicon && faviconFetchesInFlight.add(it.bookmarkId) }
        if (toFetch.isEmpty()) return
        val ids = toFetch.mapTo(HashSet()) { it.bookmarkId }
        faviconFetchesState.update { it + ids }
        toFetch.forEach { bookmark ->
            faviconScope.launch {
                try {
                    faviconFetchPermits.withPermit {
                        if (faviconStore.fetchAndSave(bookmark.bookmarkId, bookmark.url)) {
                            onFaviconSaved(bookmark.bookmarkId, bookmark.url)
                        }
                    }
                } finally {
                    faviconFetchesInFlight.remove(bookmark.bookmarkId)
                    finishedFaviconFetches.add(bookmark.bookmarkId)
                    scheduleFaviconPublish()
                }
            }
        }
    }

    private fun onFaviconSaved(bookmarkId: Long, fetchedUrl: String) {
        // The bookmark may have been deleted or pointed elsewhere while its icon downloaded.
        val current = getById(bookmarkId)
        if (current == null || current.url != fetchedUrl) {
            if (current == null) faviconStore.delete(bookmarkId)
            return
        }
        val version = System.currentTimeMillis()
        onDatabaseThread { dao.setFaviconVersion(bookmarkId, version) }
        pendingFaviconVersions[bookmarkId] = PendingFavicon(url = fetchedUrl, version = version)
    }

    /** Publishes finished favicon fetches together, so a large import re-sorts and emits a few times, not once per icon. */
    private fun scheduleFaviconPublish() {
        if (!faviconPublishScheduled.compareAndSet(false, true)) return
        faviconScope.launch {
            delay(FAVICON_PUBLISH_DELAY_MILLIS)
            publishFaviconUpdates()
        }
    }

    private fun publishFaviconUpdates() {
        faviconPublishScheduled.set(false)
        val updates = HashMap<Long, PendingFavicon>()
        pendingFaviconVersions.keys.toList().forEach { id ->
            pendingFaviconVersions.remove(id)?.let { updates[id] = it }
        }
        if (updates.isNotEmpty()) {
            mutate { list ->
                list.map { bookmark ->
                    // Skip bookmarks pointed elsewhere since their icon was saved.
                    val update = updates[bookmark.bookmarkId]?.takeIf { it.url == bookmark.url }
                    if (update == null) bookmark else bookmark.copy(faviconVersion = update.version)
                }
            }
        }
        val finished = HashSet<Long>()
        finishedFaviconFetches.toList().forEach { id -> if (finishedFaviconFetches.remove(id)) finished.add(id) }
        // A bookmark fetched again meanwhile stays marked until that fetch finishes.
        finished.removeAll(faviconFetchesInFlight)
        if (finished.isNotEmpty()) faviconFetchesState.update { it - finished }
    }

    private fun indexOf(bookmarks: List<BookmarkInfo>): BookmarkIndex =
        bookmarkIndex?.takeIf { it.source === bookmarks }
            ?: BookmarkIndex(bookmarks).also { bookmarkIndex = it }

    private fun ensureLoaded(): List<BookmarkInfo> {
        if (isLoaded) return bookmarksState.value
        synchronized(stateLock) {
            if (!isLoaded) {
                publish(onDatabaseThread { dao.getAll().map(BookmarkEntity::toModel) })
                isLoaded = true
            }
        }
        return bookmarksState.value
    }

    private fun mutate(transform: (List<BookmarkInfo>) -> List<BookmarkInfo>) {
        synchronized(stateLock) { publish(transform(ensureLoaded())) }
    }

    private fun publish(bookmarks: List<BookmarkInfo>) {
        bookmarksState.value =
            bookmarks.sortedWith(compareBy({ it.name.lowercase(Locale.getDefault()) }, { it.bookmarkId }))
    }

    private fun <T> onDatabaseThread(block: () -> T): T =
        if (Thread.currentThread().name.startsWith(DB_THREAD_PREFIX)) {
            block()
        } else {
            databaseExecutor.submit<T>(block).get()
        }

    private class PendingFavicon(
        val url: String,
        val version: Long,
    )

    private class BookmarkSearchEntry(
        val bookmark: BookmarkInfo,
        val name: PreparedSearchText,
        val host: PreparedSearchText,
        val sortName: String,
    )

    /** Lookups derived from one published bookmark list, built on first use. */
    private class BookmarkIndex(
        val source: List<BookmarkInfo>,
    ) {
        val byId: Map<Long, BookmarkInfo> by lazy { source.associateBy { it.bookmarkId } }
        val searchEntries: List<BookmarkSearchEntry> by lazy {
            source.map { bookmark ->
                BookmarkSearchEntry(
                    bookmark = bookmark,
                    name = SearchTextNormalizer.prepareForSearch(bookmark.name),
                    host = SearchTextNormalizer.prepareForSearch(BookmarkUrls.displayHost(bookmark.url)),
                    sortName = bookmark.name.lowercase(Locale.getDefault()),
                )
            }
        }
    }

    companion object {
        /** Bookmarks shown in regular search; alias searches pass a higher limit. */
        const val SEARCH_RESULT_LIMIT = 25

        private const val DB_THREAD_PREFIX = "bookmarks-store"
        private const val MAX_PARALLEL_FAVICON_FETCHES = 4
        private const val FAVICON_PUBLISH_DELAY_MILLIS = 250L

        private val stateLock = Any()
        private val bookmarksState = MutableStateFlow<List<BookmarkInfo>>(emptyList())
        private val defaultBrowserState = MutableStateFlow<String?>(null)

        @Volatile private var isLoaded = false

        @Volatile private var bookmarkIndex: BookmarkIndex? = null

        private val databaseExecutor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "$DB_THREAD_PREFIX-1").apply { isDaemon = true }
            }
        private val faviconScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val faviconFetchPermits = Semaphore(MAX_PARALLEL_FAVICON_FETCHES)
        private val faviconFetchesInFlight: MutableSet<Long> = ConcurrentHashMap.newKeySet()
        private val faviconFetchesState = MutableStateFlow<Set<Long>>(emptySet())
        private val pendingFaviconVersions = ConcurrentHashMap<Long, PendingFavicon>()
        private val finishedFaviconFetches: MutableSet<Long> = ConcurrentHashMap.newKeySet()
        private val faviconPublishScheduled = AtomicBoolean(false)
    }
}
