package com.tk.quicksearch.search.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.mtp.MtpConstants
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * MediaStore ids and normalized display names, newest-modified first. File search matches names
 * here instead of running an unindexable GLOB scan over the whole MediaStore table per keystroke,
 * then reads full rows only for the matched ids.
 */
internal class FileNameIndex(
    private val ids: LongArray,
    private val normalizedNames: Array<String>,
    private val isFolder: BooleanArray,
) {
    /**
     * Ids in index order whose name contains every token, or contains the joined tokens once spaces,
     * hyphens, underscores, and dots are ignored. With [extension], only `.<extension>` names match;
     * otherwise only folders and names with a dot, mirroring the provider query this replaces.
     */
    fun matchingIds(
        queryTokens: List<String>,
        extension: String?,
        foldersOnly: Boolean,
        limit: Int,
        cancellationSignal: CancellationSignal?,
    ): List<Long> {
        if (limit <= 0) return emptyList()
        val compactQuery = queryTokens.joinToString("")
        val extensionSuffix = extension?.let { ".${SearchTextNormalizer.normalizeForSearch(it)}" }
        val results = ArrayList<Long>(minOf(limit, 64))
        for (index in ids.indices) {
            if (index and CANCELLATION_CHECK_MASK == 0) cancellationSignal?.throwIfCanceled()
            val name = normalizedNames[index]
            val folder = isFolder[index]
            val matchesType =
                when {
                    foldersOnly -> folder
                    extensionSuffix != null -> name.endsWith(extensionSuffix)
                    else -> folder || name.indexOf('.') >= 0
                }
            if (!matchesType) continue
            if (queryTokens.isNotEmpty() &&
                !queryTokens.all(name::contains) &&
                !containsIgnoringSeparators(name, compactQuery)
            ) {
                continue
            }
            results.add(ids[index])
            if (results.size >= limit) break
        }
        return results
    }

    companion object {
        private const val CANCELLATION_CHECK_MASK = 0x3FF
        private const val MAX_AGE_MILLIS = 10L * 60L * 1000L
        private const val WAIT_POLL_MILLIS = 50L

        // Camera bursts, downloads, and syncs notify MediaStore many times in a row; rebuild once.
        private const val CHANGE_REBUILD_DELAY_MILLIS = 3_000L

        // Past this size the index costs more memory than the provider scan is worth.
        private const val MAX_INDEXED_FILES = 150_000

        private class Cached(
            val index: FileNameIndex?,
            val loadedAtElapsedMillis: Long,
            val changeGeneration: Long,
        )

        private val changeGeneration = AtomicLong()
        private val isObserverRegistered = AtomicBoolean(false)
        private val loadLock = Any()
        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
        private val loadExecutor by lazy {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "FileNameIndexLoad").apply { isDaemon = true }
            }
        }
        private val rebuildAfterChange = Runnable { startLoadIfNeeded(rebuildStale = true) }

        @Volatile private var cached: Cached? = null
        @Volatile private var loadContentResolver: ContentResolver? = null
        @Volatile private var loadFilesUri: Uri? = null

        // Guarded by loadLock: the load every waiting search shares.
        private var pendingLoad: Future<FileNameIndex?>? = null

        /**
         * The current index, or null when the library is too large to index or the read failed.
         * After a MediaStore change the previous index is still returned while a rebuild runs in
         * the background; deleted ids simply find no row and new files appear once it finishes.
         * Without any index the caller waits for the shared load, but stops waiting (throwing
         * [android.os.OperationCanceledException]) once [cancellationSignal] is cancelled, leaving
         * the load running for the next keystroke.
         */
        fun get(
            contentResolver: ContentResolver,
            filesUri: Uri,
            cancellationSignal: CancellationSignal?,
        ): FileNameIndex? {
            loadContentResolver = contentResolver
            loadFilesUri = filesUri
            ensureObserverRegistered(contentResolver, filesUri)
            usableOrNull()?.let { snapshot ->
                if (snapshot.changeGeneration != changeGeneration.get()) scheduleRebuildAfterChange()
                return snapshot.index
            }
            val load = startLoadIfNeeded(rebuildStale = false) ?: return usableOrNull()?.index
            while (true) {
                try {
                    return load.get(WAIT_POLL_MILLIS, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    cancellationSignal?.throwIfCanceled()
                } catch (_: ExecutionException) {
                    return null
                }
            }
        }

        /** Drops the index and forces the next file search to rebuild it before matching. */
        fun invalidate() {
            changeGeneration.incrementAndGet()
            release()
        }

        /** Drops the index; the next file search rebuilds it. */
        fun release() {
            cached = null
        }

        /**
         * Starts the shared load unless one is running. Returns null when no load is needed: a
         * change-triggered rebuild with no index to refresh, or one that is already current.
         */
        private fun startLoadIfNeeded(rebuildStale: Boolean): Future<FileNameIndex?>? {
            val contentResolver = loadContentResolver ?: return null
            val filesUri = loadFilesUri ?: return null
            synchronized(loadLock) {
                pendingLoad?.takeUnless { it.isDone }?.let { return it }
                if (rebuildStale) {
                    val snapshot = cached ?: return null
                    if (snapshot.changeGeneration == changeGeneration.get()) return null
                }
                val load = loadExecutor.submit<FileNameIndex?> { loadAndCache(contentResolver, filesUri) }
                pendingLoad = load
                return load
            }
        }

        private fun loadAndCache(
            contentResolver: ContentResolver,
            filesUri: Uri,
        ): FileNameIndex? {
            val generation = changeGeneration.get()
            val startElapsed = SystemClock.elapsedRealtime()
            val index = runCatching { load(contentResolver, filesUri) }.getOrNull()
            SnapshotLoadLog.loaded("File name index", index?.ids?.size ?: -1, startElapsed)
            val snapshot = Cached(index, SystemClock.elapsedRealtime(), generation)
            cached = snapshot
            scheduleRelease(snapshot)
            // Changes during the scan may be missing from it; pick them up with one more rebuild.
            if (generation != changeGeneration.get()) scheduleRebuildAfterChange()
            return index
        }

        // A stale index is never reused, so free it when it expires rather than at the next search.
        private fun scheduleRelease(snapshot: Cached) {
            mainHandler.postDelayed(
                { if (cached === snapshot) cached = null },
                MAX_AGE_MILLIS,
            )
        }

        private fun scheduleRebuildAfterChange() {
            mainHandler.removeCallbacks(rebuildAfterChange)
            mainHandler.postDelayed(rebuildAfterChange, CHANGE_REBUILD_DELAY_MILLIS)
        }

        /** The cached index if it is within its TTL, even when MediaStore changed since. */
        private fun usableOrNull(): Cached? {
            val snapshot = cached ?: return null
            val age = SystemClock.elapsedRealtime() - snapshot.loadedAtElapsedMillis
            return snapshot.takeIf { age in 0 until MAX_AGE_MILLIS }
        }

        private fun load(
            contentResolver: ContentResolver,
            filesUri: Uri,
        ): FileNameIndex? {
            val projection =
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    "format",
                )
            return contentResolver
                .query(
                    filesUri,
                    projection,
                    null,
                    null,
                    "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    if (cursor.count > MAX_INDEXED_FILES) return null
                    val ids = LongArray(cursor.count)
                    val names = arrayOfNulls<String>(cursor.count)
                    val folders = BooleanArray(cursor.count)
                    var size = 0
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1) ?: continue
                        ids[size] = cursor.getLong(0)
                        names[size] = SearchTextNormalizer.normalizeForSearch(name)
                        folders[size] = !cursor.isNull(2) && cursor.getInt(2) == MtpConstants.FORMAT_ASSOCIATION
                        size++
                    }
                    @Suppress("UNCHECKED_CAST")
                    FileNameIndex(
                        ids = ids.copyOf(size),
                        normalizedNames = names.copyOf(size) as Array<String>,
                        isFolder = folders.copyOf(size),
                    )
                }
        }

        private fun ensureObserverRegistered(
            contentResolver: ContentResolver,
            filesUri: Uri,
        ) {
            if (!isObserverRegistered.compareAndSet(false, true)) return
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        changeGeneration.incrementAndGet()
                        scheduleRebuildAfterChange()
                    }
                }
            val registered =
                runCatching { contentResolver.registerContentObserver(filesUri, true, observer) }.isSuccess
            // Without an observer the TTL alone bounds staleness; retry registration next search.
            if (!registered) isObserverRegistered.set(false)
        }

        private fun isFileNameSeparator(char: Char): Boolean =
            char == ' ' || char == '-' || char == '_' || char == '.'

        /** Whether [needle] occurs in [text] once file-name separators in [text] are skipped. */
        internal fun containsIgnoringSeparators(
            text: String,
            needle: String,
        ): Boolean {
            if (needle.isEmpty()) return true
            for (start in text.indices) {
                if (isFileNameSeparator(text[start]) || text[start] != needle[0]) continue
                var textIndex = start
                var needleIndex = 0
                while (textIndex < text.length && needleIndex < needle.length) {
                    val char = text[textIndex++]
                    if (isFileNameSeparator(char)) continue
                    if (char != needle[needleIndex]) break
                    needleIndex++
                }
                if (needleIndex == needle.length) return true
            }
            return false
        }
    }
}
