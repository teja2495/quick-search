package com.tk.quicksearch.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.mtp.MtpConstants
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.core.SearchRankingUtils
import java.util.Locale

class FileSearchRepository(
    private val context: Context
) {

    private val contentResolver = context.contentResolver

    companion object {
        private val FILE_PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        private const val DATE_MODIFIED_SORT = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getFilesByUris(uris: Set<String>): List<DeviceFile> {
        if (uris.isEmpty() || !hasPermission()) return emptyList()

        val results = mutableListOf<DeviceFile>()

        for (uriString in uris) {
            val parsedUri = parseUri(uriString) ?: continue
            
            contentResolver.query(
                parsedUri,
                FILE_PROJECTION,
                null,
                null,
                null
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

    fun searchFiles(query: String, limit: Int): List<DeviceFile> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val normalizedQuery = normalizeQuery(query)
        val selection = "LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ? AND format != ${MtpConstants.FORMAT_ASSOCIATION} AND LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE '%.%'"
        val selectionArgs = arrayOf("%$normalizedQuery%")
        val uri = getFilesContentUri()

        val results = mutableListOf<DeviceFile>()

        contentResolver.query(
            uri,
            FILE_PROJECTION,
            selection,
            selectionArgs,
            DATE_MODIFIED_SORT
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
                { it.displayName.lowercase(Locale.getDefault()) }
            )
        )
    }

    private fun normalizeQuery(query: String): String {
        return query
            .trim()
            .lowercase(Locale.getDefault())
            .replace("%", "")
            .replace("_", "")
    }

    private fun getFilesContentUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
    }

    private fun parseUri(uriString: String): Uri? {
        return runCatching { Uri.parse(uriString) }.getOrNull()
    }

    private data class ColumnIndices(
        val idIndex: Int,
        val nameIndex: Int,
        val mimeIndex: Int,
        val modifiedIndex: Int
    )

    private fun getColumnIndices(cursor: Cursor): ColumnIndices {
        return ColumnIndices(
            idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID),
            nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME),
            mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE),
            modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        )
    }

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
        useUriDirectly: Boolean = false
    ): DeviceFile? {
        val name = cursor.getString(columnIndices.nameIndex) ?: return null
        val mimeType = cursor.getString(columnIndices.mimeIndex)
        val modified = if (cursor.isNull(columnIndices.modifiedIndex)) {
            0L
        } else {
            cursor.getLong(columnIndices.modifiedIndex)
        }

        // For URI-based queries, use the original URI to maintain consistency with pinned files.
        // For search queries, build the URI from the file ID.
        val fileUri = if (useUriDirectly) {
            baseUri
        } else {
            val id = cursor.getLong(columnIndices.idIndex)
            ContentUris.withAppendedId(baseUri, id)
        }

        return DeviceFile(
            uri = fileUri,
            displayName = name,
            mimeType = mimeType,
            lastModified = modified
        )
    }
}


