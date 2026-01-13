package com.tk.quicksearch.services

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.interfaces.SearchOperationsService
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.FileTypeUtils
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.util.FileUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import java.util.Locale

/**
 * Implementation of SearchOperationsService for handling search operations
 */
class SearchOperationsServiceImpl(
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences
) : SearchOperationsService {

    companion object {
        private const val CONTACT_RESULT_LIMIT = 5
        private const val FILE_RESULT_LIMIT = 5
        private const val MIN_QUERY_LENGTH = 2

        // System/Internal file extensions to ignore
        private val SYSTEM_EXCLUDED_EXTENSIONS = setOf(
            "db", "db-shm", "db-wal", "sql", "sqlite",
            "log", "tmp", "temp", "dat", "bak", "ini",
            "nomedia", "thumbnails", "thumb",
            "lck", "lock", "sys", "bin", "cfg", "conf", "prop"
        )
    }

    override suspend fun searchContacts(
        query: String,
        enabledSections: Set<SearchSection>
    ): List<ContactInfo> {
        if (query.trim().length < MIN_QUERY_LENGTH || SearchSection.CONTACTS !in enabledSections) {
            return emptyList()
        }

        val excludedContactIds = userPreferences.getExcludedContactIds().toSet()
        return contactRepository.searchContacts(query, CONTACT_RESULT_LIMIT)
            .filterNot { excludedContactIds.contains(it.contactId) }
    }

    override suspend fun searchFiles(
        query: String,
        enabledSections: Set<SearchSection>,
        enabledFileTypes: Set<String>
    ): List<DeviceFile> {
        if (query.trim().length < MIN_QUERY_LENGTH || SearchSection.FILES !in enabledSections) {
            return emptyList()
        }

        val excludedFileUris = userPreferences.getExcludedFileUris().toSet()
        val excludedFileExtensions = userPreferences.getExcludedFileExtensions().toSet()
        val enabledTypes = enabledFileTypes.mapNotNull { fileTypeString ->
            when (fileTypeString.uppercase()) {
                "PHOTOS_AND_VIDEOS" -> FileType.PHOTOS_AND_VIDEOS
                "DOCUMENTS" -> FileType.DOCUMENTS
                "OTHER" -> FileType.OTHER
                else -> null
            }
        }.toSet()

        val allFiles = fileRepository.searchFiles(query, FILE_RESULT_LIMIT * 2)
        return allFiles
            .filter { file ->
                val fileType = FileTypeUtils.getFileType(file)
                fileType in enabledTypes &&
                !excludedFileUris.contains(file.uri.toString()) &&
                !FileUtils.isFileExtensionExcluded(file.displayName, excludedFileExtensions) &&
                !isApkFile(file) &&
                !isSystemFile(file)
            }
            .take(FILE_RESULT_LIMIT)
    }

    override suspend fun searchSettings(
        query: String,
        enabledSections: Set<SearchSection>
    ): List<SettingShortcut> {
        // This will be implemented when we extract settings search logic
        return emptyList()
    }

    override suspend fun getPinnedContacts(): List<ContactInfo> {
        val pinnedIds = userPreferences.getPinnedContactIds().toSet()
        return if (pinnedIds.isNotEmpty()) {
            contactRepository.getContactsByIds(pinnedIds)
        } else {
            emptyList()
        }
    }

    override suspend fun getPinnedFiles(): List<DeviceFile> {
        val pinnedUris = userPreferences.getPinnedFileUris().toSet()
        return pinnedUris.mapNotNull { uriString ->
            // This would need to be implemented to get files by URI
            // For now, return empty list
            null
        }
    }

    override suspend fun getExcludedContacts(): List<ContactInfo> {
        val excludedIds = userPreferences.getExcludedContactIds().toSet()
        return if (excludedIds.isNotEmpty()) {
            contactRepository.getContactsByIds(excludedIds)
        } else {
            emptyList()
        }
    }

    override suspend fun getExcludedFiles(): List<DeviceFile> {
        val excludedUris = userPreferences.getExcludedFileUris().toSet()
        return excludedUris.mapNotNull { uriString ->
            // This would need to be implemented to get files by URI
            // For now, return empty list
            null
        }
    }

    private fun isApkFile(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") {
            return true
        }
        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.endsWith(".apk")
    }

    private fun isSystemFile(deviceFile: DeviceFile): Boolean {
        val name = deviceFile.displayName
        // Check for hidden files (starting with dot)
        if (name.startsWith(".")) return true

        val extension = FileUtils.getFileExtension(name)?.lowercase(Locale.getDefault()) ?: return false

        // WhatsApp/Signal backup files (crypt, crypt12, crypt14, etc.)
        if (extension.startsWith("crypt")) {
            // Check if it's "crypt" or "crypt" followed by numbers
            return extension == "crypt" || extension.drop(5).all { it.isDigit() }
        }

        return extension in SYSTEM_EXCLUDED_EXTENSIONS
    }
}