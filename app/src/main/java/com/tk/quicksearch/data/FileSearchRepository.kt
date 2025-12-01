package com.tk.quicksearch.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.DeviceFile
import java.util.Locale

class FileSearchRepository(
    private val context: Context
) {

    private val contentResolver = context.contentResolver

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
            val parsed = runCatching { android.net.Uri.parse(uriString) }.getOrNull() ?: continue
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val cursor = contentResolver.query(
                parsed,
                projection,
                null,
                null,
                null
            ) ?: continue

            cursor.use { c ->
                if (!c.moveToFirst()) return@use
                val idIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val modifiedIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex) ?: return@use
                val mimeType = c.getString(mimeIndex)
                val modified = if (!c.isNull(modifiedIndex)) c.getLong(modifiedIndex) else 0L
                // Use the original stored URI to ensure consistency with what was pinned
                val fileUri = parsed

                results.add(
                    DeviceFile(
                        uri = fileUri,
                        displayName = name,
                        mimeType = mimeType,
                        lastModified = modified
                    )
                )
            }
        }

        return results.sortedWith(
            compareBy(
                { it.displayName.lowercase(Locale.getDefault()) }
            )
        )
    }

    fun searchFiles(query: String, limit: Int): List<DeviceFile> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val normalizedQuery = query
            .trim()
            .lowercase(Locale.getDefault())
            .replace("%", "")
            .replace("_", "")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ?"
        val selectionArgs = arrayOf("%$normalizedQuery%")

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        ) ?: return emptyList()

        val results = mutableListOf<DeviceFile>()

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val modifiedIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (c.moveToNext() && results.size < limit) {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex) ?: continue
                val mimeType = c.getString(mimeIndex)
                val modified = if (!c.isNull(modifiedIndex)) c.getLong(modifiedIndex) else 0L
                val fileUri = ContentUris.withAppendedId(uri, id)
                results.add(
                    DeviceFile(
                        uri = fileUri,
                        displayName = name,
                        mimeType = mimeType,
                        lastModified = modified
                    )
                )
            }
        }

        return results.sortedWith(compareBy(
            { com.tk.quicksearch.util.SearchRankingUtils.calculateMatchPriority(it.displayName, query) },
            { it.displayName.lowercase(Locale.getDefault()) }
        ))
    }
}


