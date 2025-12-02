package com.tk.quicksearch.search

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.model.FileTypeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope

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
        scope: CoroutineScope
    ): SearchResult {
        if (query.length < MIN_QUERY_LENGTH) {
            return SearchResult(emptyList(), emptyList())
        }
        
        // Perform searches in parallel
        val contactsDeferred = scope.async {
            if (canSearchContacts) {
                searchContacts(query, excludedContactIds)
            } else {
                emptyList()
            }
        }
        
        val filesDeferred = scope.async {
            if (canSearchFiles) {
                searchFiles(query, enabledFileTypes, excludedFileUris)
            } else {
                emptyList()
            }
        }
        
        val contacts = contactsDeferred.await()
        val files = filesDeferred.await()
        
        // Optimize results: if only one type has results, fetch more for that type
        return when {
            contacts.isNotEmpty() && files.isEmpty() && canSearchFiles -> {
                SearchResult(contacts, searchFiles(query, enabledFileTypes, excludedFileUris, Int.MAX_VALUE))
            }
            files.isNotEmpty() && contacts.isEmpty() && canSearchContacts -> {
                SearchResult(searchContacts(query, excludedContactIds, Int.MAX_VALUE), files)
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
        limit: Int = FILE_RESULT_LIMIT * 2
    ): List<DeviceFile> {
        val allFiles = fileRepository.searchFiles(query, limit)
        return allFiles
            .filter { file ->
                val fileType = FileTypeUtils.getFileType(file)
                fileType in enabledFileTypes && !excludedFileUris.contains(file.uri.toString())
            }
            .take(FILE_RESULT_LIMIT)
    }
}
