package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.utils.FileUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

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
    
    data class SearchResult(
        val contacts: List<ContactInfo>,
        val files: List<DeviceFile>
    )
    
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
    
    private suspend fun searchContacts(
        query: String,
        excludedContactIds: Set<Long>,
        limit: Int = CONTACT_RESULT_LIMIT
    ): List<ContactInfo> {
        return contactRepository.searchContacts(query, limit)
            .filterNot { excludedContactIds.contains(it.contactId) }
    }
    
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
                !isSystemFolder(file) &&
                !isApkFile(file) &&
                !isSystemFile(file)
            }
            .take(FILE_RESULT_LIMIT)
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

    private fun isSystemFolder(deviceFile: DeviceFile): Boolean {
        if (!deviceFile.isDirectory) return false

        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.startsWith("com.")
    }
}
