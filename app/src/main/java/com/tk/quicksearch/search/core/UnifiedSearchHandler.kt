package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.settings.SettingsSearchHandler
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
        if (trimmedQuery.isBlank() || trimmedQuery.length == 1) {
            return@withContext UnifiedSearchResults()
        }

        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())

        // Search by display name (existing behavior)
        val result = searchOperations.performSearches(
            query = trimmedQuery,
            canSearchContacts = canSearchContacts,
            canSearchFiles = canSearchFiles,
            enabledFileTypes = enabledFileTypes,
            excludedContactIds = userPreferences.getExcludedContactIds(),
            excludedFileUris = userPreferences.getExcludedFileUris(),
            excludedFileExtensions = userPreferences.getExcludedFileExtensions(),
            scope = this
        )

        // Also search for contacts/files with matching nicknames
        val nicknameMatchingContactIds = if (canSearchContacts) {
            userPreferences.findContactsWithMatchingNickname(trimmedQuery)
                .filterNot { userPreferences.getExcludedContactIds().contains(it) }
        } else {
            emptySet()
        }

        val nicknameMatchingFileUris = if (canSearchFiles) {
            userPreferences.findFilesWithMatchingNickname(trimmedQuery)
                .filterNot { userPreferences.getExcludedFileUris().contains(it) }
        } else {
            emptySet()
        }

        // Fetch contacts/files that match by nickname but not by display name
        val nicknameOnlyContacts = if (nicknameMatchingContactIds.isNotEmpty()) {
            val displayNameMatchedIds = result.contacts.map { it.contactId }.toSet()
            val nicknameOnlyIds = nicknameMatchingContactIds.filterNot { displayNameMatchedIds.contains(it) }
            if (nicknameOnlyIds.isNotEmpty()) {
                contactRepository.getContactsByIds(nicknameOnlyIds.toSet())
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        val nicknameOnlyFiles = if (nicknameMatchingFileUris.isNotEmpty()) {
            val displayNameMatchedUris = result.files.map { it.uri.toString() }.toSet()
            val nicknameOnlyUris = nicknameMatchingFileUris.filterNot { displayNameMatchedUris.contains(it) }
            if (nicknameOnlyUris.isNotEmpty()) {
                fileRepository.getFilesByUris(nicknameOnlyUris.toSet())
                    .filter { file ->
                        val fileType = com.tk.quicksearch.model.FileTypeUtils.getFileType(file)
                        fileType in enabledFileTypes &&
                        !userPreferences.getExcludedFileUris().contains(file.uri.toString()) &&
                        !FileUtils.isFileExtensionExcluded(file.displayName, userPreferences.getExcludedFileExtensions())
                    }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Combine display name matches and nickname-only matches
        val allContacts = (result.contacts + nicknameOnlyContacts).distinctBy { it.contactId }
        val allFiles = (result.files + nicknameOnlyFiles).distinctBy { it.uri.toString() }

        // Filter and rank by nickname matches
        val filteredContacts = allContacts.mapNotNull { contact ->
            val nickname = userPreferences.getContactNickname(contact.contactId)
            val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                contact.displayName,
                nickname,
                normalizedQuery
            )
            if (SearchRankingUtils.isOtherMatch(priority)) {
                null
            } else {
                contact to priority
            }
        }.sortedWith(
            compareBy<Pair<ContactInfo, Int>> { it.second }
                .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
        ).map { it.first }

        val filteredFiles = allFiles.mapNotNull { file ->
            val nickname = userPreferences.getFileNickname(file.uri.toString())
            val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                file.displayName,
                nickname,
                normalizedQuery
            )
            if (SearchRankingUtils.isOtherMatch(priority)) {
                null
            } else {
                file to priority
            }
        }.sortedWith(
            compareBy<Pair<DeviceFile, Int>> { it.second }
                .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
        ).map { it.first }

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
}
