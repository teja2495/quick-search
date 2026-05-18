package com.tk.quicksearch.search.models

/** Categories of file types that can be filtered in search results. */
enum class FileType {
    DOCUMENTS,
    PICTURES,
    VIDEOS,
    AUDIO,
    APKS,
    OTHER,
}

/** Utility functions for categorizing files by MIME type. */
object FileTypeUtils {
    private val IMAGE_PREFIX = "image/"
    private val VIDEO_PREFIX = "video/"
    private val AUDIO_PREFIX = "audio/"
    private val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private val BINARY_MIME_TYPE = "application/octet-stream"

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
            "text/",
        )

    private val DOCUMENT_EXTENSIONS =
        setOf(
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "ppt",
            "pptx",
            "odt",
            "ods",
            "odp",
            "rtf",
            "txt",
            "md",
            "csv",
            "json",
            "xml",
            "yaml",
            "yml",
            "html",
            "htm",
            "log",
            "epub",
        )

    private val IMAGE_EXTENSIONS =
        setOf(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "bmp",
            "webp",
            "heic",
            "heif",
            "svg",
            "avif",
            "tiff",
            "tif",
            "ico",
            "raw",
            "dng",
            "cr2",
            "nef",
            "arw",
        )

    private val VIDEO_EXTENSIONS =
        setOf(
            "mp4",
            "mkv",
            "avi",
            "mov",
            "wmv",
            "flv",
            "webm",
            "m4v",
            "3gp",
            "mpeg",
            "mpg",
            "ts",
        )

    private val AUDIO_EXTENSIONS =
        setOf(
            "mp3",
            "wav",
            "aac",
            "m4a",
            "flac",
            "ogg",
            "oga",
            "wma",
            "opus",
            "amr",
            "aiff",
            "mid",
            "midi",
        )

    fun getFileType(mimeType: String?): FileType {
        if (mimeType == null) return FileType.OTHER

        val normalizedMime = mimeType.lowercase()

        return when {
            normalizedMime.startsWith(IMAGE_PREFIX) -> FileType.PICTURES
            normalizedMime.startsWith(VIDEO_PREFIX) -> FileType.VIDEOS
            normalizedMime.startsWith(AUDIO_PREFIX) -> FileType.AUDIO
            normalizedMime == APK_MIME_TYPE -> FileType.APKS
            isDocumentType(normalizedMime) -> FileType.DOCUMENTS
            else -> FileType.OTHER
        }
    }

    fun getFileTypeFromName(fileName: String): FileType? {
        val extension = extensionOf(fileName) ?: return null
        return getFileTypeByExtension(extension).takeIf { it != FileType.OTHER }
    }

    private fun isDocumentType(normalizedMime: String): Boolean = DOCUMENT_PREFIXES.any { normalizedMime.startsWith(it) }

    fun getFileType(file: DeviceFile): FileType {
        if (file.isDirectory) {
            return FileType.OTHER
        }
        val typeFromMime = getFileType(file.mimeType)
        val normalizedMime = file.mimeType?.lowercase()
        val shouldTrustMime = normalizedMime != null && normalizedMime != BINARY_MIME_TYPE
        if (typeFromMime != FileType.OTHER && shouldTrustMime) {
            return typeFromMime
        }
        return getFileTypeFromName(file.displayName) ?: typeFromMime
    }

    private fun extensionOf(fileName: String): String? =
        fileName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it.isNotBlank() }

    private fun getFileTypeByExtension(extension: String): FileType =
        when {
            extension == "apk" -> FileType.APKS
            extension in IMAGE_EXTENSIONS -> FileType.PICTURES
            extension in VIDEO_EXTENSIONS -> FileType.VIDEOS
            extension in AUDIO_EXTENSIONS -> FileType.AUDIO
            extension in DOCUMENT_EXTENSIONS -> FileType.DOCUMENTS
            else -> FileType.OTHER
        }

    fun isPdf(file: DeviceFile): Boolean {
        if (file.isDirectory) return false
        if (file.mimeType?.lowercase() == "application/pdf") return true
        return file.displayName.lowercase().endsWith(".pdf")
    }

    fun isImage(file: DeviceFile): Boolean {
        if (file.isDirectory) return false
        val mime = file.mimeType?.lowercase()
        if (mime != null && mime.startsWith(IMAGE_PREFIX)) return true
        val ext = file.displayName.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXTENSIONS
    }
}
