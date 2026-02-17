package com.tk.quicksearch.search.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.mtp.MtpConstants
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

class FileSearchRepository(
    private val context: Context,
) {
    private val contentResolver = context.contentResolver

    companion object {
        private const val COLUMN_FORMAT = "format"

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
    }

    fun hasPermission(): Boolean = PermissionUtils.hasFileAccessPermission(context)

    /**
     * Performs a lightweight MediaStore read to refresh file data visibility from the provider.
     * Returns true when the query succeeds (even if no files are returned).
     */
    fun refreshFilesProviderSnapshot(): Boolean {
        if (!hasPermission()) return false

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

        val results = mutableListOf<DeviceFile>()

        for (uriString in uris) {
            val parsedUri = parseUri(uriString) ?: continue

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
                        val file = createDeviceFileFromCursor(cursor, parsedUri, columnIndices, useUriDirectly = true)
                        if (file != null) {
                            results.add(file)
                        }
                    }
                }
        }

        return results.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    fun searchFiles(
        query: String,
        limit: Int,
    ): List<DeviceFile> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val normalizedQuery = normalizeQuery(query)
        val escapedQuery = escapeLikeQuery(normalizedQuery)
        val selection =
            "LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ? ESCAPE '\\' AND " +
                "(format = ${MtpConstants.FORMAT_ASSOCIATION} OR LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE '%.%')"
        val selectionArgs = arrayOf("%$escapedQuery%")
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

        // Pre-tokenize the already-normalized query for efficient ranking
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        return results.sortedWith(
            compareBy(
                { SearchRankingUtils.calculateMatchPriority(it.displayName, normalizedQuery, queryTokens) },
                { it.displayName.lowercase(Locale.getDefault()) },
            ),
        )
    }

    private fun normalizeQuery(query: String): String =
        SearchTextNormalizer.normalizeForSearch(
            SearchTextNormalizer.normalizeQueryWhitespace(query),
        )

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
