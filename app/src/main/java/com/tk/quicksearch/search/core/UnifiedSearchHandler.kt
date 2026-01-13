package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.options.SettingsSearchHandler
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.util.FileUtils
import com.tk.quicksearch.util.SearchRankingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class UnifiedSearchResults(
    val contactResults: List<ContactInfo> = emptyList(),
    val fileResults: List<DeviceFile> = emptyList(),
    val settingResults: List<com.tk.quicksearch.model.SettingShortcut> = emptyList()
)

class UnifiedSearchHandler(
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
    private val settingsSearchHandler: SettingsSearchHandler,
    private val searchOperations: SearchOperations
) {

    suspend fun performSearch(
        query: String,
        enabledFileTypes: Set<FileType>,
        canSearchContacts: Boolean,
        canSearchFiles: Boolean,
        canSearchSettings: Boolean
    ): UnifiedSearchResults = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (shouldSkipSearch(trimmedQuery)) {
            return@withContext UnifiedSearchResults()
        }

        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())

        // Search by display name (existing behavior)
        val searchResult = performBaseSearch(trimmedQuery, enabledFileTypes, canSearchContacts, canSearchFiles, this@withContext)

        // Add nickname matches that weren't found by display name
        val nicknameContacts = findNicknameOnlyContacts(searchResult.contacts, trimmedQuery, canSearchContacts)
        val nicknameFiles = findNicknameOnlyFiles(searchResult.files, trimmedQuery, enabledFileTypes, canSearchFiles)

        // Combine and filter results
        val filteredContacts = filterAndRankContacts(searchResult.contacts + nicknameContacts, normalizedQuery)
        val filteredFiles = filterAndRankFiles(searchResult.files + nicknameFiles, normalizedQuery)

        val settingsMatches = if (canSearchSettings) {
            settingsSearchHandler.searchSettings(trimmedQuery)
        } else {
            emptyList()
        }

        return@withContext UnifiedSearchResults(
            contactResults = filteredContacts,
            fileResults = filteredFiles,
            settingResults = settingsMatches
        )
    }

    private fun shouldSkipSearch(query: String): Boolean {
        return query.isBlank() || query.length == 1
    }

    private suspend fun performBaseSearch(
        query: String,
        enabledFileTypes: Set<FileType>,
        canSearchContacts: Boolean,
        canSearchFiles: Boolean,
        scope: CoroutineScope
    ): SearchOperations.SearchResult {
        return searchOperations.performSearches(
            query = query,
            canSearchContacts = canSearchContacts,
            canSearchFiles = canSearchFiles,
            enabledFileTypes = enabledFileTypes,
            excludedContactIds = userPreferences.getExcludedContactIds(),
            excludedFileUris = userPreferences.getExcludedFileUris(),
            excludedFileExtensions = userPreferences.getExcludedFileExtensions(),
            scope = scope
        )
    }

    private suspend fun findNicknameOnlyContacts(
        displayNameContacts: List<ContactInfo>,
        query: String,
        canSearchContacts: Boolean
    ): List<ContactInfo> {
        if (!canSearchContacts) return emptyList()

        val nicknameMatchingIds = userPreferences.findContactsWithMatchingNickname(query)
            .filterNot { userPreferences.getExcludedContactIds().contains(it) }

        if (nicknameMatchingIds.isEmpty()) return emptyList()

        val displayNameMatchedIds = displayNameContacts.map { it.contactId }.toSet()
        val nicknameOnlyIds = nicknameMatchingIds.filterNot { displayNameMatchedIds.contains(it) }

        return if (nicknameOnlyIds.isNotEmpty()) {
            contactRepository.getContactsByIds(nicknameOnlyIds.toSet())
        } else {
            emptyList()
        }
    }

    private suspend fun findNicknameOnlyFiles(
        displayNameFiles: List<DeviceFile>,
        query: String,
        enabledFileTypes: Set<FileType>,
        canSearchFiles: Boolean
    ): List<DeviceFile> {
        if (!canSearchFiles) return emptyList()

        val nicknameMatchingUris = userPreferences.findFilesWithMatchingNickname(query)
            .filterNot { userPreferences.getExcludedFileUris().contains(it) }

        if (nicknameMatchingUris.isEmpty()) return emptyList()

        val displayNameMatchedUris = displayNameFiles.map { it.uri.toString() }.toSet()
        val nicknameOnlyUris = nicknameMatchingUris.filterNot { displayNameMatchedUris.contains(it) }

        return if (nicknameOnlyUris.isNotEmpty()) {
            fileRepository.getFilesByUris(nicknameOnlyUris.toSet())
                .filter { file ->
                    val fileType = com.tk.quicksearch.model.FileTypeUtils.getFileType(file)
                    fileType in enabledFileTypes &&
                    !userPreferences.getExcludedFileUris().contains(file.uri.toString()) &&
                    !FileUtils.isFileExtensionExcluded(file.displayName, userPreferences.getExcludedFileExtensions()) &&
                    file.displayName.contains(".")
                }
        } else {
            emptyList()
        }
    }

    private fun filterAndRankContacts(contacts: List<ContactInfo>, normalizedQuery: String): List<ContactInfo> {
        if (contacts.isEmpty()) return emptyList()
        
        // Pre-tokenize query once for efficient matching
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        // Pre-fetch all contact nicknames in a single call to reduce SharedPreferences reads
        val distinctContacts = contacts.distinctBy { it.contactId }
        val contactNicknames = distinctContacts.associate { contact ->
            contact.contactId to userPreferences.getContactNickname(contact.contactId)
        }
        
        return distinctContacts
            .mapNotNull { contact ->
                val nickname = contactNicknames[contact.contactId]
                val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                    contact.displayName, nickname, normalizedQuery, queryTokens
                )
                if (SearchRankingUtils.isOtherMatch(priority)) null else contact to priority
            }
            .sortedWith(
                compareBy<Pair<ContactInfo, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
    }

    private fun filterAndRankFiles(files: List<DeviceFile>, normalizedQuery: String): List<DeviceFile> {
        if (files.isEmpty()) return emptyList()
        
        // Pre-tokenize query once for efficient matching
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        // Pre-fetch all file nicknames in a single call to reduce SharedPreferences reads
        val distinctFiles = files.distinctBy { it.uri.toString() }
        val fileNicknames = distinctFiles.associate { file ->
            file.uri.toString() to userPreferences.getFileNickname(file.uri.toString())
        }
        
        return distinctFiles
            .mapNotNull { file ->
                val uriString = file.uri.toString()
                val nickname = fileNicknames[uriString]
                val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                    file.displayName, nickname, normalizedQuery, queryTokens
                )
                if (SearchRankingUtils.isOtherMatch(priority)) null else file to priority
            }
            .sortedWith(
                compareBy<Pair<DeviceFile, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
    }
}
