package com.tk.quicksearch.search.files

import android.content.Context
import android.mtp.MtpConstants
import android.provider.MediaStore
import com.tk.quicksearch.search.models.DeviceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What a folder holds, counted recursively from MediaStore. */
data class FolderContentsSummary(
    val itemCount: Int,
    val totalSizeBytes: Long,
)

object FolderContentsLoader {
    private const val COLUMN_FORMAT = "format"

    /** The relative path MediaStore stores for items directly inside [folder], like `Documents/Invoices/`. */
    fun folderRelativePath(folder: DeviceFile): String {
        val parents = FileFolderPathFormatter.format(folder)?.segments.orEmpty()
        return (parents + folder.displayName.trim()).joinToString(separator = "/", postfix = "/")
    }

    suspend fun load(
        context: Context,
        folder: DeviceFile,
    ): FolderContentsSummary? =
        withContext(Dispatchers.IO) {
            val prefix = escapeLike(folderRelativePath(folder))
            val volume = folder.volumeName?.takeIf { it.isNotBlank() } ?: MediaStore.VOLUME_EXTERNAL
            runCatching {
                context.contentResolver
                    .query(
                        MediaStore.Files.getContentUri(volume),
                        arrayOf(MediaStore.MediaColumns.SIZE, COLUMN_FORMAT),
                        "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? ESCAPE '\\'",
                        arrayOf("$prefix%"),
                        null,
                    )?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                        val formatIndex = cursor.getColumnIndex(COLUMN_FORMAT)
                        var count = 0
                        var totalSize = 0L
                        while (cursor.moveToNext()) {
                            count++
                            val isDirectory =
                                formatIndex != -1 &&
                                    !cursor.isNull(formatIndex) &&
                                    cursor.getInt(formatIndex) == MtpConstants.FORMAT_ASSOCIATION
                            if (!isDirectory && sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                                totalSize += cursor.getLong(sizeIndex)
                            }
                        }
                        FolderContentsSummary(itemCount = count, totalSizeBytes = totalSize)
                    }
            }.getOrNull()
        }

    private fun escapeLike(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}
