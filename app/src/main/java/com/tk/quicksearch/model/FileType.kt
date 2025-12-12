package com.tk.quicksearch.model

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
    
    // MIME type prefixes for media files
    private val MEDIA_PREFIXES = setOf("image/", "video/")
    
    // MIME type prefixes for document files
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
    
    
    /**
     * Determines the file type category based on MIME type.
     * Returns OTHER if MIME type is null or doesn't match known categories.
     */
    fun getFileType(mimeType: String?): FileType {
        if (mimeType == null) return FileType.OTHER
        
        val normalizedMime = mimeType.lowercase()
        
        return when {
            isMediaType(normalizedMime) -> FileType.PHOTOS_AND_VIDEOS
            isDocumentType(normalizedMime) -> FileType.DOCUMENTS
            else -> FileType.OTHER
        }
    }
    
    /**
     * Checks if the MIME type represents a media file (image or video).
     */
    private fun isMediaType(normalizedMime: String): Boolean {
        return MEDIA_PREFIXES.any { normalizedMime.startsWith(it) }
    }
    
    /**
     * Checks if the MIME type represents a document file.
     */
    private fun isDocumentType(normalizedMime: String): Boolean {
        return DOCUMENT_PREFIXES.any { normalizedMime.startsWith(it) }
    }
    
    /**
     * Gets the file type for a DeviceFile.
     */
    fun getFileType(file: DeviceFile): FileType {
        return getFileType(file.mimeType)
    }
}
