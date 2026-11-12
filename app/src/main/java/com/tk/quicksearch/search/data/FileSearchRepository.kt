package com.tk.quicksearch.search.data

import android.app.ActivityManager
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.mtp.MtpConstants
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.os.SystemClock
import android.provider.MediaStore
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.utils.FileSearchTextNormalizer
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class FileSearchRepository(
    private val context: Context,
) {
    private val contentResolver = context.contentResolver
    private val recentFilesByUri = buildRecentFilesCache()
    private val isLowRamDevice by lazy {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }

    // The newest files, which fuzzy search scores on every keystroke; read once per typing session
    // rather than scanning MediaStore again each time.
    private var recentFilesSnapshot: List<DeviceFile> = emptyList()
    private var recentFilesSnapshotLimit = 0
    private var recentFilesSnapshotAtMillis = 0L

    companion object {
        private const val COLUMN_FORMAT = "format"
        private const val BATCH_ID_QUERY_CHUNK_SIZE = 200
        private const val MAX_RECENT_FILE_SNAPSHOT_SIZE = 400
        private const val SQL_DOT = "'.'"
        private const val SQL_EMPTY = "''"
        private const val SQL_HYPHEN = "'-'"
        private const val SQL_SPACE = "' '"
        private const val SQL_UNDERSCORE = "'_'"
        private const val RECENT_FILES_SNAPSHOT_MAX_AGE_MILLIS = 30_000L

        private val FILE_PROJECTION = buildFileProjection()

        private const val DATE_MODIFIED_SORT = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        private fun buildFileProjection(): Array<String> {
            val projection =
                mutableListOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    COLUMN_FORMAT,
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection.add(MediaStore.MediaColumns.RELATIVE_PATH)
                projection.add(MediaStore.MediaColumns.VOLUME_NAME)
            }

            return projection.toTypedArray()
        }

        private fun buildRecentFilesCache(): LinkedHashMap<String, DeviceFile> =
            object : LinkedHashMap<String, DeviceFile>(MAX_RECENT_FILE_SNAPSHOT_SIZE, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, DeviceFile>?,
                ): Boolean = size > MAX_RECENT_FILE_SNAPSHOT_SIZE
            }
    }

    fun hasPermission(): Boolean = PermissionUtils.hasFileAccessPermission(context)

    /**
     * Performs a lightweight MediaStore read to refresh file data visibility from the provider.
     * Returns true when the query succeeds (even if no files are returned).
     */
    fun refreshFilesProviderSnapshot(): Boolean {
        if (!hasPermission()) return false
        synchronized(this) { recentFilesSnapshotAtMillis = 0L }
        FileNameIndex.invalidate()

        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        return runCatching {
            contentResolver
                .query(
                    getFilesContentUri(),
                    projection,
                    null,
                    null,
                    DATE_MODIFIED_SORT,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getLong(0)
                    }
                }
            true
        }.getOrDefault(false)
    }

    fun getFilesByUris(uris: Set<String>): List<DeviceFile> {
        if (uris.isEmpty() || !hasPermission()) return emptyList()

        val orderedUris = uris.toList()
        val resultsByUri = LinkedHashMap<String, DeviceFile>(orderedUris.size)
        val pendingUris = mutableListOf<String>()

        orderedUris.forEach { uriString ->
            val cachedFile = getCachedFile(uriString)
            if (cachedFile != null) {
                resultsByUri[uriString] = cachedFile
            } else {
                pendingUris.add(uriString)
            }
        }

        if (pendingUris.isNotEmpty()) {
            val batchableKeys = mutableMapOf<MediaStoreLookupKey, String>()
            val nonBatchableUris = mutableListOf<String>()

            pendingUris.forEach { uriString ->
                val lookupKey = parseMediaStoreLookupKey(uriString)
                if (lookupKey != null) {
                    // Keep original URI for consistency with persisted pin/exclude sets.
                    batchableKeys.putIfAbsent(lookupKey, uriString)
                } else {
                    nonBatchableUris.add(uriString)
                }
            }

            if (batchableKeys.isNotEmpty()) {
                queryBatchableMediaStoreUris(batchableKeys).forEach { (originalUri, file) ->
                    resultsByUri[originalUri] = file
                    cacheFile(file)
                }
            }

            nonBatchableUris.forEach { uriString ->
                val parsedUri = parseUri(uriString) ?: return@forEach
                contentResolver
                    .query(
                        parsedUri,
                        FILE_PROJECTION,
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndices = getColumnIndices(cursor)
                            val file =
                                createDeviceFileFromCursor(
                                    cursor,
                                    parsedUri,
                                    columnIndices,
                                    useUriDirectly = true,
                                )
                            if (file != null) {
                                resultsByUri[uriString] = file
                                cacheFile(file)
                            }
                        }
                    }
            }
        }

        return resultsByUri.values.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    /** With [extension], only files ending in `.<extension>` match and [query] may be blank. */
    fun searchFiles(
        query: String,
        limit: Int,
        extension: String? = null,
        cancellationSignal: CancellationSignal? = null,
    ): List<DeviceFile> {
        if ((query.isBlank() && extension == null) || !hasPermission()) return emptyList()

        val queryTokens = if (query.isBlank()) emptyList() else normalizeQueryTokens(query)
        if (queryTokens.isEmpty() && extension == null) return emptyList()
        searchIndexedFiles(queryTokens, extension, foldersOnly = false, limit, cancellationSignal)
            ?.let { return it }
        val nameSelection =
            if (queryTokens.isEmpty()) {
                null
            } else {
                val displayNameTokenSelection =
                    queryTokens.joinToString(" AND ") {
                        "${MediaStore.Files.FileColumns.DISPLAY_NAME} GLOB ?"
                    }
                val compactDisplayNameSelection = "${compactDisplayNameExpression()} GLOB ?"
                "($displayNameTokenSelection OR $compactDisplayNameSelection)"
            }
        val typeSelection =
            if (extension == null) {
                "(format = ${MtpConstants.FORMAT_ASSOCIATION} OR LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE '%.%')"
            } else {
                "LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ? ESCAPE '\\'"
            }
        val selection = listOfNotNull(nameSelection, typeSelection).joinToString(" AND ")
        val nameSelectionArgs =
            if (queryTokens.isEmpty()) {
                emptyList()
            } else {
                nameGlobArgs(queryTokens)
            }
        val selectionArgs =
            (
                nameSelectionArgs +
                    listOfNotNull(extension?.let { "%.${escapeLikeQuery(it.lowercase(Locale.ROOT))}" })
            ).toTypedArray()
        val uri = getFilesContentUri()

        val results = mutableListOf<DeviceFile>()

        contentResolver
            .query(
                uri,
                FILE_PROJECTION,
                selection,
                selectionArgs,
                DATE_MODIFIED_SORT,
                cancellationSignal,
            )?.use { cursor ->
                val columnIndices = getColumnIndices(cursor)

                while (cursor.moveToNext() && results.size < limit) {
                    cancellationSignal?.throwIfCanceled()
                    val file = createDeviceFileFromCursor(cursor, uri, columnIndices)
                    if (file != null) {
                        results.add(file)
                    }
                }
            }

        cacheFiles(results)
        return results
    }

    fun searchFolders(
        query: String,
        limit: Int,
    ): List<DeviceFile> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val queryTokens = normalizeQueryTokens(query)
        if (queryTokens.isEmpty()) return emptyList()
        searchIndexedFiles(queryTokens, extension = null, foldersOnly = true, limit, cancellationSignal = null)
            ?.let { return it }
        val displayNameTokenSelection =
            queryTokens.joinToString(" AND ") {
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} GLOB ?"
            }
        val compactDisplayNameSelection = "${compactDisplayNameExpression()} GLOB ?"
        val selection =
            "($displayNameTokenSelection OR $compactDisplayNameSelection) AND " +
                "format = ${MtpConstants.FORMAT_ASSOCIATION}"
        val selectionArgs =
            (
                nameGlobArgs(queryTokens)
            ).toTypedArray()
        val uri = getFilesContentUri()

        val results = mutableListOf<DeviceFile>()

        contentResolver
            .query(
                uri,
                FILE_PROJECTION,
                selection,
                selectionArgs,
                DATE_MODIFIED_SORT,
            )?.use { cursor ->
                val columnIndices = getColumnIndices(cursor)

                while (cursor.moveToNext() && results.size < limit) {
                    val file = createDeviceFileFromCursor(cursor, uri, columnIndices)
                    if (file != null) {
                        results.add(file)
                    }
                }
            }

        cacheFiles(results)
        return results
    }

    /** Newest-modified files first; [include] is applied while scanning so [limit] counts matches. */
    fun getRecentFiles(
        limit: Int,
        cancellationSignal: CancellationSignal? = null,
        include: (DeviceFile) -> Boolean = { true },
    ): List<DeviceFile> {
        if (limit <= 0 || !hasPermission()) return emptyList()

        val uri = getFilesContentUri()
        val results = mutableListOf<DeviceFile>()
        contentResolver
            .query(
                uri,
                FILE_PROJECTION,
                null,
                null,
                DATE_MODIFIED_SORT,
                cancellationSignal,
            )?.use { cursor ->
                val columnIndices = getColumnIndices(cursor)
                while (cursor.moveToNext() && results.size < limit) {
                    cancellationSignal?.throwIfCanceled()
                    val file = createDeviceFileFromCursor(cursor, uri, columnIndices)
                    if (file != null && include(file)) {
                        results.add(file)
                    }
                }
            }

        cacheFiles(results)
        return results
    }

    /** [getRecentFiles] without a filter, reused for a short while across keystrokes. */
    fun getRecentFilesSnapshot(
        limit: Int,
        cancellationSignal: CancellationSignal? = null,
    ): List<DeviceFile> {
        val now = SystemClock.elapsedRealtime()
        synchronized(this) {
            if (recentFilesSnapshotLimit >= limit &&
                recentFilesSnapshotAtMillis != 0L &&
                now - recentFilesSnapshotAtMillis < RECENT_FILES_SNAPSHOT_MAX_AGE_MILLIS
            ) {
                return recentFilesSnapshot.take(limit)
            }
        }
        val files = getRecentFiles(limit, cancellationSignal)
        synchronized(this) {
            recentFilesSnapshot = files
            recentFilesSnapshotLimit = limit
            recentFilesSnapshotAtMillis = now
        }
        return files
    }

    /** Matches names in memory, then reads full rows for matched ids; null falls back to a provider scan. */
    private fun searchIndexedFiles(
        queryTokens: List<String>,
        extension: String?,
        foldersOnly: Boolean,
        limit: Int,
        cancellationSignal: CancellationSignal?,
    ): List<DeviceFile>? {
        if (isLowRamDevice) return null
        val uri = getFilesContentUri()
        val index = FileNameIndex.get(contentResolver, uri, cancellationSignal) ?: return null
        val ids = index.matchingIds(queryTokens, extension, foldersOnly, limit, cancellationSignal)
        if (ids.isEmpty()) return emptyList()

        val filesById = HashMap<Long, DeviceFile>(ids.size)
        ids.chunked(BATCH_ID_QUERY_CHUNK_SIZE).forEach { chunkIds ->
            val selection = "${MediaStore.Files.FileColumns._ID} IN (${chunkIds.joinToString(",")})"
            contentResolver
                .query(uri, FILE_PROJECTION, selection, null, null, cancellationSignal)
                ?.use { cursor ->
                    val columnIndices = getColumnIndices(cursor)
                    while (cursor.moveToNext()) {
                        val file = createDeviceFileFromCursor(cursor, uri, columnIndices) ?: continue
                        filesById[cursor.getLong(columnIndices.idIndex)] = file
                    }
                }
        }
        val results = ids.mapNotNull(filesById::get)
        cacheFiles(results)
        return results
    }

    private fun normalizeQuery(query: String): String =
        SearchTextNormalizer.normalizeForSearch(
            SearchTextNormalizer.normalizeQueryWhitespace(query),
        )

    private fun normalizeQueryTokens(query: String): List<String> =
        FileSearchTextNormalizer.queryTokens(normalizeQuery(query))

    /** Per-token patterns, then one for the tokens joined (matched against the compact name). */
    private fun nameGlobArgs(queryTokens: List<String>): List<String> =
        queryTokens.map(FileSearchTextNormalizer::containsGlobPattern) +
            FileSearchTextNormalizer.containsGlobPattern(queryTokens.joinToString(""))

    private fun compactDisplayNameExpression(): String {
        val withoutSpaces = "REPLACE(${MediaStore.Files.FileColumns.DISPLAY_NAME}, $SQL_SPACE, $SQL_EMPTY)"
        val withoutHyphens = "REPLACE($withoutSpaces, $SQL_HYPHEN, $SQL_EMPTY)"
        val withoutUnderscores = "REPLACE($withoutHyphens, $SQL_UNDERSCORE, $SQL_EMPTY)"
        return "REPLACE($withoutUnderscores, $SQL_DOT, $SQL_EMPTY)"
    }

    private fun escapeLikeQuery(query: String): String =
        buildString(query.length) {
            query.forEach { char ->
                when (char) {
                    '\\', '%', '_' -> {
                        append('\\')
                        append(char)
                    }
                    else -> append(char)
                }
            }
        }

    private fun getFilesContentUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

    private fun parseUri(uriString: String): Uri? = runCatching { Uri.parse(uriString) }.getOrNull()

    private data class MediaStoreLookupKey(
        val volumeName: String,
        val id: Long,
    )

    private fun parseMediaStoreLookupKey(uriString: String): MediaStoreLookupKey? {
        val parsedUri = parseUri(uriString) ?: return null
        if (parsedUri.scheme != "content") return null
        if (parsedUri.authority != MediaStore.AUTHORITY) return null

        val pathSegments = parsedUri.pathSegments
        if (pathSegments.size < 3) return null

        val volumeName = pathSegments[0]
        val collection = pathSegments[1]
        if (collection != "file") return null

        val id = runCatching { ContentUris.parseId(parsedUri) }.getOrNull() ?: return null
        if (id <= 0L) return null

        return MediaStoreLookupKey(volumeName = volumeName, id = id)
    }

    private fun queryBatchableMediaStoreUris(
        keysToOriginalUri: Map<MediaStoreLookupKey, String>,
    ): Map<String, DeviceFile> {
        val filesByUri = mutableMapOf<String, DeviceFile>()

        keysToOriginalUri.keys
            .groupBy { it.volumeName }
            .forEach { (volumeName, volumeKeys) ->
                val baseUri = MediaStore.Files.getContentUri(volumeName)
                val ids = volumeKeys.map { it.id }

                ids.chunked(BATCH_ID_QUERY_CHUNK_SIZE).forEach { chunkIds ->
                    val placeholders = chunkIds.joinToString(",") { "?" }
                    val selection = "${MediaStore.Files.FileColumns._ID} IN ($placeholders)"
                    val selectionArgs = chunkIds.map { it.toString() }.toTypedArray()

                    val cursor = runCatching {
                        contentResolver.query(baseUri, FILE_PROJECTION, selection, selectionArgs, null)
                    }.getOrElse { e ->
                        // Some devices don't expose volume-specific URIs (e.g. external_primary).
                        // Fall back to the virtual external volume which covers all external storage.
                        if (e is IllegalArgumentException && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            runCatching {
                                contentResolver.query(
                                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                                    FILE_PROJECTION,
                                    selection,
                                    selectionArgs,
                                    null,
                                )
                            }.getOrNull()
                        } else {
                            null
                        }
                    }

                    cursor?.use { cursor ->
                            val columnIndices = getColumnIndices(cursor)
                            while (cursor.moveToNext()) {
                                val rowId = cursor.getLong(columnIndices.idIndex)
                                val key = MediaStoreLookupKey(volumeName = volumeName, id = rowId)
                                val originalUriString = keysToOriginalUri[key] ?: continue
                                val originalUri = parseUri(originalUriString) ?: continue
                                val file =
                                    createDeviceFileFromCursor(
                                        cursor,
                                        originalUri,
                                        columnIndices,
                                        useUriDirectly = true,
                                    ) ?: continue
                                filesByUri[originalUriString] = file
                            }
                        }
                }
            }

        return filesByUri
    }

    private fun getCachedFile(uriString: String): DeviceFile? =
        synchronized(recentFilesByUri) { recentFilesByUri[uriString] }

    private fun cacheFile(file: DeviceFile) {
        synchronized(recentFilesByUri) {
            recentFilesByUri[file.uri.toString()] = file
        }
    }

    private fun cacheFiles(files: List<DeviceFile>) {
        if (files.isEmpty()) return
        synchronized(recentFilesByUri) {
            files.forEach { file ->
                recentFilesByUri[file.uri.toString()] = file
            }
        }
    }

    private data class ColumnIndices(
        val idIndex: Int,
        val nameIndex: Int,
        val mimeIndex: Int,
        val modifiedIndex: Int,
        val formatIndex: Int,
        val relativePathIndex: Int,
        val volumeNameIndex: Int,
    )

    private fun getColumnIndices(cursor: Cursor): ColumnIndices =
        ColumnIndices(
            idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID),
            nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME),
            mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE),
            modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED),
            formatIndex = cursor.getColumnIndex(COLUMN_FORMAT),
            relativePathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH),
            volumeNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME),
        )

    /**
     * Creates a DeviceFile from a cursor row.
     * @param cursor The cursor positioned at the row to read
     * @param baseUri The base URI for building the file URI
     * @param columnIndices Pre-computed column indices for efficiency
     * @param useUriDirectly If true, uses baseUri directly; if false, builds URI from file ID
     */
    private fun createDeviceFileFromCursor(
        cursor: Cursor,
        baseUri: Uri,
        columnIndices: ColumnIndices,
        useUriDirectly: Boolean = false,
    ): DeviceFile? {
        val name = cursor.getString(columnIndices.nameIndex) ?: return null
        val mimeType = cursor.getString(columnIndices.mimeIndex)
        val modified =
            if (cursor.isNull(columnIndices.modifiedIndex)) {
                0L
            } else {
                cursor.getLong(columnIndices.modifiedIndex)
            }
        val isDirectory =
            columnIndices.formatIndex != -1 &&
                !cursor.isNull(columnIndices.formatIndex) &&
                cursor.getInt(columnIndices.formatIndex) == MtpConstants.FORMAT_ASSOCIATION
        val relativePath =
            if (columnIndices.relativePathIndex != -1 && !cursor.isNull(columnIndices.relativePathIndex)) {
                cursor.getString(columnIndices.relativePathIndex)
            } else {
                null
            }
        val volumeName =
            if (columnIndices.volumeNameIndex != -1 && !cursor.isNull(columnIndices.volumeNameIndex)) {
                cursor.getString(columnIndices.volumeNameIndex)
            } else {
                null
            }

        // For URI-based queries, use the original URI to maintain consistency with pinned files.
        // For search queries, build the URI from the file ID.
        val fileUri =
            if (useUriDirectly) {
                baseUri
            } else {
                val id = cursor.getLong(columnIndices.idIndex)
                ContentUris.withAppendedId(baseUri, id)
            }

        return DeviceFile(
            uri = fileUri,
            displayName = name,
            mimeType = mimeType,
            lastModified = modified,
            isDirectory = isDirectory,
            relativePath = relativePath,
            volumeName = volumeName,
        )
    }
}

/**
 * Runs a blocking MediaStore read with a [CancellationSignal] that is cancelled with the calling
 * coroutine, so a superseded keystroke's query stops instead of running to the end.
 */
internal suspend fun <T> withQueryCancellation(block: (CancellationSignal) -> T): T =
    coroutineScope {
        val signal = CancellationSignal()
        val result =
            async {
                try {
                    block(signal)
                } catch (e: OperationCanceledException) {
                    throw CancellationException("MediaStore query cancelled").apply { initCause(e) }
                }
            }
        try {
            result.await()
        } catch (e: CancellationException) {
            signal.cancel()
            throw e
        }
    }
