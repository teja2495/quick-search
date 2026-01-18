package com.tk.quicksearch.search.models

/** Categories of file types that can be filtered in search results. */
enum class FileType {
    DOCUMENTS,
    PICTURES,
    VIDEOS,
    MUSIC,
    APKS,
    OTHER
}

/** Utility functions for categorizing files by MIME type. */
object FileTypeUtils {

    private val IMAGE_PREFIX = "image/"
    private val VIDEO_PREFIX = "video/"
    private val AUDIO_PREFIX = "audio/"
    private val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private val DOCUMENT_PREFIXES =
            setOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.ms-word",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml",
                    "application/vnd.oasis.opendocument",
                    "application/rtf",
                    "application/x-rtf",
                    "text/"
            )

    fun getFileType(mimeType: String?): FileType {
        if (mimeType == null) return FileType.OTHER

        val normalizedMime = mimeType.lowercase()

        return when {
            normalizedMime.startsWith(IMAGE_PREFIX) -> FileType.PICTURES
            normalizedMime.startsWith(VIDEO_PREFIX) -> FileType.VIDEOS
            normalizedMime.startsWith(AUDIO_PREFIX) -> FileType.MUSIC
            normalizedMime == APK_MIME_TYPE -> FileType.APKS
            isDocumentType(normalizedMime) -> FileType.DOCUMENTS
            else -> FileType.OTHER
        }
    }

    fun getFileTypeFromName(fileName: String): FileType? {
        val normalizedName = fileName.lowercase()
        return when {
            normalizedName.endsWith(".apk") -> FileType.APKS
            else -> null
        }
    }

    private fun isDocumentType(normalizedMime: String): Boolean {
        return DOCUMENT_PREFIXES.any { normalizedMime.startsWith(it) }
    }

    fun getFileType(file: DeviceFile): FileType {
        if (file.isDirectory) {
            return FileType.DOCUMENTS
        }
        val typeFromMime = getFileType(file.mimeType)
        if (typeFromMime != FileType.OTHER) {
            return typeFromMime
        }
        return getFileTypeFromName(file.displayName) ?: FileType.OTHER
    }
}
