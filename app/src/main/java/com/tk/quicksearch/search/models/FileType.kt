package com.tk.quicksearch.search.models

/**
 * Categories of file types that can be filtered in search results.
 */
enum class FileType {
    PHOTOS_AND_VIDEOS,
    DOCUMENTS,
    OTHER
}

/**
 * Utility functions for categorizing files by MIME type.
 */
object FileTypeUtils {

    private val MEDIA_PREFIXES = setOf("image/", "video/")

    private val DOCUMENT_PREFIXES = setOf(
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
            isMediaType(normalizedMime) -> FileType.PHOTOS_AND_VIDEOS
            isDocumentType(normalizedMime) -> FileType.DOCUMENTS
            else -> FileType.OTHER
        }
    }
    
    private fun isMediaType(normalizedMime: String): Boolean {
        return MEDIA_PREFIXES.any { normalizedMime.startsWith(it) }
    }

    private fun isDocumentType(normalizedMime: String): Boolean {
        return DOCUMENT_PREFIXES.any { normalizedMime.startsWith(it) }
    }
    
    fun getFileType(file: DeviceFile): FileType {
        return getFileType(file.mimeType)
    }
}
