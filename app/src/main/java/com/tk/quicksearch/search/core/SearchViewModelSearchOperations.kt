package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.FileTypeUtils
import com.tk.quicksearch.util.FileUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

/**
 * Handles secondary search operations (contacts and files).
 * Simplifies the complex search logic and makes it more maintainable.
 */
class SearchOperations(
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository
) {
    
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
    
    /**
     * Search result data class.
     */
    data class SearchResult(
        val contacts: List<ContactInfo>,
        val files: List<DeviceFile>
    )
    
    /**
     * Performs searches for contacts and files in parallel.
     *
     * @param query The search query
     * @param canSearchContacts Whether contacts search is enabled
     * @param canSearchFiles Whether files search is enabled
     * @param enabledFileTypes Set of enabled file types
     * @param excludedContactIds Set of excluded contact IDs
     * @param excludedFileUris Set of excluded file URIs
     * @param excludedFileExtensions Set of excluded file extensions
     * @param scope Coroutine scope for async operations
     * @return SearchResult containing contacts and files
     */
    suspend fun performSearches(
        query: String,
        canSearchContacts: Boolean,
        canSearchFiles: Boolean,
        enabledFileTypes: Set<FileType>,
        excludedContactIds: Set<Long>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>,
        scope: CoroutineScope
    ): SearchResult {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_QUERY_LENGTH) {
            return SearchResult(emptyList(), emptyList())
        }
        
        // Perform searches in parallel
        val contactsDeferred = scope.async {
            if (canSearchContacts) {
                searchContacts(trimmedQuery, excludedContactIds)
            } else {
                emptyList()
            }
        }
        
        val filesDeferred = scope.async {
            if (canSearchFiles) {
                searchFiles(trimmedQuery, enabledFileTypes, excludedFileUris, excludedFileExtensions)
            } else {
                emptyList()
            }
        }
        
        val contacts = contactsDeferred.await()
        val files = filesDeferred.await()
        
        // Optimize results: if only one type has results, fetch more for that type
        return when {
            contacts.isNotEmpty() && files.isEmpty() && canSearchFiles -> {
                SearchResult(contacts, searchFiles(trimmedQuery, enabledFileTypes, excludedFileUris, excludedFileExtensions, Int.MAX_VALUE))
            }
            files.isNotEmpty() && contacts.isEmpty() && canSearchContacts -> {
                SearchResult(searchContacts(trimmedQuery, excludedContactIds, Int.MAX_VALUE), files)
            }
            else -> SearchResult(contacts, files)
        }
    }
    
    /**
     * Searches contacts with filtering.
     */
    private suspend fun searchContacts(
        query: String,
        excludedContactIds: Set<Long>,
        limit: Int = CONTACT_RESULT_LIMIT
    ): List<ContactInfo> {
        return contactRepository.searchContacts(query, limit)
            .filterNot { excludedContactIds.contains(it.contactId) }
    }
    
    /**
     * Searches files with filtering by enabled types and exclusions.
     */
    private suspend fun searchFiles(
        query: String,
        enabledFileTypes: Set<FileType>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>,
        limit: Int = FILE_RESULT_LIMIT * 2
    ): List<DeviceFile> {
        val allFiles = fileRepository.searchFiles(query, limit)
        return allFiles
            .filter { file ->
                val fileType = FileTypeUtils.getFileType(file)
                fileType in enabledFileTypes &&
                !excludedFileUris.contains(file.uri.toString()) &&
                !FileUtils.isFileExtensionExcluded(file.displayName, excludedFileExtensions) &&
                !isApkFile(file) &&
                !isSystemFile(file)
            }
            .take(FILE_RESULT_LIMIT)
    }

    /**
     * Checks if a file is an APK file.
     */
    private fun isApkFile(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") {
            return true
        }
        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.endsWith(".apk")
    }

    /**
     * Checks if a file is a system or internal file that should be hidden.
     */
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
