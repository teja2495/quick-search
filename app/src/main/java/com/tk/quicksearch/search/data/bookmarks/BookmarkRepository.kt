package com.tk.quicksearch.search.data.bookmarks

import android.content.Context
import com.tk.quicksearch.search.data.preferences.BookmarkPreferences
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    fun getById(bookmarkId: Long): BookmarkInfo? = ensureLoaded().firstOrNull { it.bookmarkId == bookmarkId }

    fun hasBookmarks(): Boolean = ensureLoaded().isNotEmpty()

    fun search(query: String): List<BookmarkInfo> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        if (normalizedQuery.isBlank()) return emptyList()
        val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }
        return ensureLoaded()
            .mapNotNull { bookmark ->
                val namePriority = SearchRankingUtils.calculateMatchPriority(bookmark.name, normalizedQuery, tokens)
                val hostPriority =
                    SearchRankingUtils.calculateMatchPriority(
                        BookmarkUrls.displayHost(bookmark.url),
                        normalizedQuery,
                        tokens,
                    )
                val priority = minOf(namePriority, hostPriority)
                if (SearchRankingUtils.isOtherMatch(priority)) null else bookmark to priority
            }
            .sortedWith(
                compareBy<Pair<BookmarkInfo, Int>> { it.second }
                    .thenBy { it.first.name.lowercase(Locale.getDefault()) },
            )
            .map { it.first }
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
        onDatabaseThread { dao.replaceAll(restored.map(BookmarkInfo::toEntity)) }
        faviconStore.deleteAll()
        synchronized(stateLock) {
            isLoaded = true
            publish(restored)
        }
        fetchFaviconsInBackground(restored)
    }

    /** Fetches favicons for bookmarks that have none, such as after an import or restore. */
    fun refreshMissingFavicons() {
        fetchFaviconsInBackground(ensureLoaded())
    }

    private fun fetchFaviconsInBackground(bookmarks: List<BookmarkInfo>) {
        bookmarks
            .filter { !it.hasFavicon && faviconFetchesInFlight.add(it.bookmarkId) }
            .forEach { bookmark ->
                faviconFetchesState.update { it + bookmark.bookmarkId }
                faviconScope.launch {
                    try {
                        faviconFetchPermits.withPermit {
                            if (faviconStore.fetchAndSave(bookmark.bookmarkId, bookmark.url)) {
                                onFaviconSaved(bookmark.bookmarkId, bookmark.url)
                            }
                        }
                    } finally {
                        faviconFetchesInFlight.remove(bookmark.bookmarkId)
                        faviconFetchesState.update { it - bookmark.bookmarkId }
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
        mutate { list ->
            list.map { if (it.bookmarkId == bookmarkId) it.copy(faviconVersion = version) else it }
        }
    }

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

    private companion object {
        const val DB_THREAD_PREFIX = "bookmarks-store"
        const val MAX_PARALLEL_FAVICON_FETCHES = 4

        val stateLock = Any()
        val bookmarksState = MutableStateFlow<List<BookmarkInfo>>(emptyList())
        val defaultBrowserState = MutableStateFlow<String?>(null)

        @Volatile var isLoaded = false

        val databaseExecutor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "$DB_THREAD_PREFIX-1").apply { isDaemon = true }
            }
        val faviconScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val faviconFetchPermits = Semaphore(MAX_PARALLEL_FAVICON_FETCHES)
        val faviconFetchesInFlight: MutableSet<Long> = ConcurrentHashMap.newKeySet()
        val faviconFetchesState = MutableStateFlow<Set<Long>>(emptySet())
    }
}
