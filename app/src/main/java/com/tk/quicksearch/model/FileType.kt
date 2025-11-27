package com.tk.quicksearch.model

/**
 * Categories of file types that can be filtered in search results.
 */
enum class FileType {
    PHOTOS_AND_VIDEOS,
    DOCUMENTS,
    APK,
    OTHER
}

/**
 * Utility functions for categorizing files by MIME type.
 */
object FileTypeUtils {
    /**
     * Determines the file type category based on MIME type.
     * Returns OTHER if MIME type is null or doesn't match known categories.
     */
    fun getFileType(mimeType: String?): FileType {
        if (mimeType == null) return FileType.OTHER
        
        val normalizedMime = mimeType.lowercase()
        
        return when {
            normalizedMime.startsWith("image/") -> FileType.PHOTOS_AND_VIDEOS
            normalizedMime.startsWith("video/") -> FileType.PHOTOS_AND_VIDEOS
            normalizedMime.startsWith("application/vnd.android.package-archive") -> FileType.APK
            normalizedMime.startsWith("application/pdf") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/msword") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.ms-word") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.ms-excel") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.ms-powerpoint") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.openxmlformats-officedocument.presentationml") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/vnd.oasis.opendocument") -> FileType.DOCUMENTS
            normalizedMime.startsWith("text/") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/rtf") -> FileType.DOCUMENTS
            normalizedMime.startsWith("application/x-rtf") -> FileType.DOCUMENTS
            else -> FileType.OTHER
        }
    }
    
    /**
     * Gets the file type for a DeviceFile.
     */
    fun getFileType(file: DeviceFile): FileType {
        return getFileType(file.mimeType)
    }
}

