package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.SearchRankingUtils
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UnifiedSearchResults(
        val contactResults: List<ContactInfo> = emptyList(),
        val fileResults: List<DeviceFile> = emptyList(),
        val settingResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val appShortcutResults: List<StaticShortcut> = emptyList()
)

class UnifiedSearchHandler(
        private val contactRepository: ContactRepository,
        private val fileRepository: FileSearchRepository,
        private val userPreferences: UserAppPreferences,
        private val settingsSearchHandler: DeviceSettingsSearchHandler,
        private val appShortcutSearchHandler: AppShortcutSearchHandler,
        private val fileSearchHandler: FileSearchHandler,
        private val searchOperations: SearchOperations
) {

        companion object {
                private val WHITESPACE_REGEX = "\\s+".toRegex()
        }

        suspend fun performSearch(
                query: String,
                enabledFileTypes: Set<FileType>,
                canSearchContacts: Boolean,
                canSearchFiles: Boolean,
                canSearchSettings: Boolean,
                canSearchAppShortcuts: Boolean,
                showFolders: Boolean = true,
                showSystemFiles: Boolean = false,
                showHiddenFiles: Boolean = false
        ): UnifiedSearchResults =
                withContext(Dispatchers.IO) {
                        val trimmedQuery = query.trim()
                        if (shouldSkipSearch(trimmedQuery)) {
                                return@withContext UnifiedSearchResults()
                        }

                        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())

                        // Search contacts by display name
                        val contactResults =
                                if (canSearchContacts) {
                                        searchOperations.searchContacts(
                                                trimmedQuery,
                                                userPreferences.getExcludedContactIds()
                                        )
                                } else {
                                        emptyList()
                                }

                        // Search files using FileSearchHandler
                        val fileResults =
                                if (canSearchFiles) {
                                        fileSearchHandler.searchFiles(
                                                trimmedQuery,
                                                enabledFileTypes,
                                                showFolders,
                                                showSystemFiles,
                                                showHiddenFiles
                                        )
                                } else {
                                        emptyList()
                                }

                        // Add nickname matches that weren't found by display name
                        val nicknameContacts =
                                findNicknameOnlyContacts(
                                        contactResults,
                                        trimmedQuery,
                                        canSearchContacts
                                )
                        val nicknameFiles =
                                findNicknameOnlyFiles(
                                        fileResults,
                                        trimmedQuery,
                                        enabledFileTypes,
                                        canSearchFiles
                                )

                        // Combine and filter results
                        val filteredContacts =
                                filterAndRankContacts(
                                        contactResults + nicknameContacts,
                                        normalizedQuery
                                )
                        val filteredFiles =
                                filterAndRankFiles(fileResults + nicknameFiles, normalizedQuery)

                        val settingsMatches =
                                if (canSearchSettings) {
                                        settingsSearchHandler.searchSettings(trimmedQuery)
                                } else {
                                        emptyList()
                                }

                        val appShortcutMatches =
                                if (canSearchAppShortcuts) {
                                        appShortcutSearchHandler.searchShortcuts(trimmedQuery)
                                } else {
                                        emptyList()
                                }

                        return@withContext UnifiedSearchResults(
                                contactResults = filteredContacts,
                                fileResults = filteredFiles,
                                settingResults = settingsMatches,
                                appShortcutResults = appShortcutMatches
                        )
                }

        private fun shouldSkipSearch(query: String): Boolean {
                return query.isBlank() || query.length == 1
        }

        private suspend fun findNicknameOnlyContacts(
                displayNameContacts: List<ContactInfo>,
                query: String,
                canSearchContacts: Boolean
        ): List<ContactInfo> {
                if (!canSearchContacts) return emptyList()

                val nicknameMatchingIds =
                        userPreferences.findContactsWithMatchingNickname(query).filterNot {
                                userPreferences.getExcludedContactIds().contains(it)
                        }

                if (nicknameMatchingIds.isEmpty()) return emptyList()

                val displayNameMatchedIds = displayNameContacts.map { it.contactId }.toSet()
                val nicknameOnlyIds =
                        nicknameMatchingIds.filterNot { displayNameMatchedIds.contains(it) }

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

                val nicknameMatchingUris =
                        userPreferences.findFilesWithMatchingNickname(query).filterNot {
                                userPreferences.getExcludedFileUris().contains(it)
                        }

                if (nicknameMatchingUris.isEmpty()) return emptyList()

                val displayNameMatchedUris = displayNameFiles.map { it.uri.toString() }.toSet()
                val nicknameOnlyUris =
                        nicknameMatchingUris.filterNot { displayNameMatchedUris.contains(it) }

                return if (nicknameOnlyUris.isNotEmpty()) {
                        fileRepository.getFilesByUris(nicknameOnlyUris.toSet()).filter { file ->
                                val fileType =
                                        com.tk.quicksearch.search.models.FileTypeUtils.getFileType(
                                                file
                                        )
                                fileType in enabledFileTypes &&
                                        !userPreferences
                                                .getExcludedFileUris()
                                                .contains(file.uri.toString()) &&
                                        !FileUtils.isFileExtensionExcluded(
                                                file.displayName,
                                                userPreferences.getExcludedFileExtensions()
                                        ) &&
                                        file.displayName.contains(".")
                        }
                } else {
                        emptyList()
                }
        }

        private fun filterAndRankContacts(
                contacts: List<ContactInfo>,
                normalizedQuery: String
        ): List<ContactInfo> {
                if (contacts.isEmpty()) return emptyList()

                // Pre-tokenize query once for efficient matching
                val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

                // Pre-fetch all contact nicknames in a single call to reduce SharedPreferences
                // reads
                val distinctContacts = contacts.distinctBy { it.contactId }
                val contactNicknames =
                        distinctContacts.associate { contact ->
                                contact.contactId to
                                        userPreferences.getContactNickname(contact.contactId)
                        }

                return distinctContacts
                        .mapNotNull { contact: ContactInfo ->
                                val nickname = contactNicknames[contact.contactId]
                                val priority =
                                        SearchRankingUtils.calculateMatchPriorityWithNickname(
                                                contact.displayName,
                                                nickname,
                                                normalizedQuery,
                                                queryTokens
                                        )
                                if (SearchRankingUtils.isOtherMatch(priority)) null
                                else Pair(contact, priority)
                        }
                        .sortedWith(
                                compareBy<Pair<ContactInfo, Int>> { it.second }.thenBy {
                                        it.first.displayName.lowercase(Locale.getDefault())
                                }
                        )
                        .map { it.first }
        }

        private fun filterAndRankFiles(
                files: List<DeviceFile>,
                normalizedQuery: String
        ): List<DeviceFile> {
                if (files.isEmpty()) return emptyList()

                // Pre-tokenize query once for efficient matching
                val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }

                // Pre-fetch all file nicknames in a single call to reduce SharedPreferences reads
                val distinctFiles = files.distinctBy { it.uri.toString() }
                val fileNicknames =
                        distinctFiles.associate { file ->
                                file.uri.toString() to
                                        userPreferences.getFileNickname(file.uri.toString())
                        }

                return distinctFiles
                        .mapNotNull { file: DeviceFile ->
                                val uriString = file.uri.toString()
                                val nickname = fileNicknames[uriString]
                                val priority =
                                        SearchRankingUtils.calculateMatchPriorityWithNickname(
                                                file.displayName,
                                                nickname,
                                                normalizedQuery,
                                                queryTokens
                                        )
                                if (SearchRankingUtils.isOtherMatch(priority)) null
                                else Pair(file, priority)
                        }
                        .sortedWith(
                                compareBy<Pair<DeviceFile, Int>> { it.second }.thenBy {
                                        it.first.displayName.lowercase(Locale.getDefault())
                                }
                        )
                        .map { it.first }
        }
}
