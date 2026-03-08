package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.contacts.ContactSearchPolicy
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.files.FileSearchPolicy
import com.tk.quicksearch.search.files.FolderPathPatternMatcher
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileClassifier
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class UnifiedSearchResults(
        val contactResults: List<ContactInfo> = emptyList(),
        val fileResults: List<DeviceFile> = emptyList(),
        val settingResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val appShortcutResults: List<StaticShortcut> = emptyList(),
)

class UnifiedSearchHandler(
        private val contactRepository: ContactRepository,
        private val fileRepository: FileSearchRepository,
        private val userPreferences: UserAppPreferences,
        private val settingsSearchHandler: DeviceSettingsSearchHandler,
        private val appShortcutSearchHandler: AppShortcutSearchHandler,
        private val fileSearchHandler: FileSearchHandler,
        private val searchOperations: SearchOperations,
) {
        companion object {
                private const val NICKNAME_ONLY_FILE_URI_HYDRATION_LIMIT =
                        FileSearchHandler.FILE_SEARCH_RESULT_LIMIT * 2
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
                showHiddenFiles: Boolean = false,
        ): UnifiedSearchResults =
                withContext(Dispatchers.IO) {
                        val trimmedQuery = query.trim()
                        if (shouldSkipSearch(trimmedQuery)) {
                                return@withContext UnifiedSearchResults()
                        }

                        val queryContext = SearchQueryContext.fromRawQuery(trimmedQuery)

                        // Read all per-query preferences once to avoid repeated SharedPreferences
                        // I/O.
                        val excludedContactIds =
                                if (canSearchContacts) userPreferences.getExcludedContactIds()
                                else emptySet()
                        val excludedFileUris =
                                if (canSearchFiles) userPreferences.getExcludedFileUris()
                                else emptySet()
                        val excludedFileExtensions =
                                if (canSearchFiles) userPreferences.getExcludedFileExtensions()
                                else emptySet()
                        val folderWhitelistPatterns =
                                if (canSearchFiles) userPreferences.getFolderWhitelistPatterns()
                                else emptySet()
                        val folderBlacklistPatterns =
                                if (canSearchFiles) userPreferences.getFolderBlacklistPatterns()
                                else emptySet()
                        val recencyIndex =
                                RecentResultRankingUtils.buildRecencyIndex(
                                        userPreferences.getRecentResultOpens()
                                )

                        val (contactResults, fileResults, settingsMatches, appShortcutMatches) =
                                coroutineScope {
                                        val contactsDeferred = async {
                                                if (canSearchContacts) {
                                                        searchOperations.searchContacts(
                                                                queryContext,
                                                                excludedContactIds,
                                                        )
                                                } else {
                                                        emptyList()
                                                }
                                        }
                                        val filesDeferred = async {
                                                if (canSearchFiles) {
                                                        fileSearchHandler.searchFiles(
                                                                queryContext,
                                                                enabledFileTypes,
                                                                excludedFileUris,
                                                                excludedFileExtensions,
                                                                folderWhitelistPatterns,
                                                                folderBlacklistPatterns,
                                                                showFolders,
                                                                showSystemFiles,
                                                                showHiddenFiles,
                                                                recencyIndex.fileScores,
                                                        )
                                                } else {
                                                        emptyList()
                                                }
                                        }
                                        val settingsDeferred = async {
                                                if (canSearchSettings) {
                                                        settingsSearchHandler.searchSettings(
                                                                queryContext,
                                                                recencyIndex.settingScores,
                                                        )
                                                } else {
                                                        emptyList()
                                                }
                                        }
                                        val appShortcutsDeferred = async {
                                                if (canSearchAppShortcuts) {
                                                        appShortcutSearchHandler.searchShortcuts(
                                                                queryContext,
                                                                recencyIndex.appShortcutScores,
                                                        )
                                                } else {
                                                        emptyList()
                                                }
                                        }

                                        SecondarySearchResults(
                                                contactsDeferred.await(),
                                                filesDeferred.await(),
                                                settingsDeferred.await(),
                                                appShortcutsDeferred.await(),
                                        )
                                }

                        // Add nickname matches that weren't found by display name
                        val nicknameContacts =
                                findNicknameOnlyContacts(
                                        contactResults,
                                        queryContext,
                                        canSearchContacts,
                                        excludedContactIds,
                                )
                        val nicknameFiles =
                                findNicknameOnlyFiles(
                                        fileResults,
                                        queryContext,
                                        enabledFileTypes,
                                        canSearchFiles,
                                        excludedFileUris,
                                        excludedFileExtensions,
                                        folderWhitelistPatterns,
                                        folderBlacklistPatterns,
                                        showFolders,
                                        showSystemFiles,
                                        showHiddenFiles,
                                )

                        // Combine and filter results
                        val filteredContacts =
                                filterAndRankContacts(
                                                contactResults + nicknameContacts,
                                                queryContext,
                                                recencyIndex.contactScores,
                                        )
                                        .take(SearchOperations.CONTACT_RESULT_LIMIT)
                        val filteredFiles =
                                filterAndRankFiles(
                                        fileResults + nicknameFiles,
                                        queryContext,
                                        recencyIndex.fileScores,
                                )

                        val hydratedContacts =
                                contactRepository.hydrateContactsForDisplay(filteredContacts)

                        return@withContext UnifiedSearchResults(
                                contactResults = hydratedContacts,
                                fileResults = filteredFiles,
                                settingResults = settingsMatches,
                                appShortcutResults = appShortcutMatches,
                        )
                }

        private fun shouldSkipSearch(query: String): Boolean = query.isBlank()

        private data class SecondarySearchResults(
                val contactResults: List<ContactInfo>,
                val fileResults: List<DeviceFile>,
                val settingsResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
                val appShortcutResults: List<StaticShortcut>,
        )

        private suspend fun findNicknameOnlyContacts(
                displayNameContacts: List<ContactInfo>,
                queryContext: SearchQueryContext,
                canSearchContacts: Boolean,
                excludedContactIds: Set<Long>,
        ): List<ContactInfo> {
                if (!canSearchContacts) return emptyList()

                val nicknameMatchingIds =
                        userPreferences.findContactsWithMatchingNickname(queryContext.normalizedQuery).filterNot {
                                excludedContactIds.contains(it)
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
                queryContext: SearchQueryContext,
                enabledFileTypes: Set<FileType>,
                canSearchFiles: Boolean,
                excludedFileUris: Set<String>,
                excludedFileExtensions: Set<String>,
                folderWhitelistPatterns: Set<String>,
                folderBlacklistPatterns: Set<String>,
                showFolders: Boolean,
                showSystemFiles: Boolean,
                showHiddenFiles: Boolean,
        ): List<DeviceFile> {
                if (!canSearchFiles) return emptyList()

                val nicknameMatchingUris =
                        userPreferences.findFilesWithMatchingNickname(queryContext.normalizedQuery).filterNot {
                                excludedFileUris.contains(it)
                        }

                if (nicknameMatchingUris.isEmpty()) return emptyList()

                val displayNameMatchedUris = displayNameFiles.map { it.uri.toString() }.toSet()
                val nicknameOnlyUris =
                        nicknameMatchingUris.filterNot { displayNameMatchedUris.contains(it) }
                val limitedNicknameOnlyUris =
                        nicknameOnlyUris.take(NICKNAME_ONLY_FILE_URI_HYDRATION_LIMIT)
                val pathMatcher =
                        FolderPathPatternMatcher.createPathMatcher(
                                whitelistPatterns = folderWhitelistPatterns,
                                blacklistPatterns = folderBlacklistPatterns,
                        )

                return if (limitedNicknameOnlyUris.isNotEmpty()) {
                        fileRepository.getFilesByUris(limitedNicknameOnlyUris.toSet()).filter { file ->
                                val fileType =
                                        com.tk.quicksearch.search.models.FileTypeUtils.getFileType(
                                                file,
                                        )

                                if (file.isDirectory && !showFolders) return@filter false

                                val isSystem =
                                        FileClassifier.isSystemFolder(file) ||
                                                FileClassifier.isSystemFile(file)
                                if (isSystem && !showSystemFiles) return@filter false

                                val isHidden = file.displayName.startsWith(".")
                                if (isHidden && !showHiddenFiles) return@filter false

                                if (!showHiddenFiles && FileClassifier.isInTrashFolder(file))
                                        return@filter false

                                fileType in enabledFileTypes &&
                                        !excludedFileUris.contains(file.uri.toString()) &&
                                        pathMatcher(file) &&
                                        !FileUtils.isFileExtensionExcluded(
                                                file.displayName,
                                                excludedFileExtensions,
                                        ) &&
                                        file.displayName.contains(".")
                        }
                } else {
                        emptyList()
                }
        }

        private fun filterAndRankContacts(
                contacts: List<ContactInfo>,
                queryContext: SearchQueryContext,
                recentContactScores: Map<Long, Int>,
        ): List<ContactInfo> {
                if (contacts.isEmpty()) return emptyList()

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
                                        ContactSearchPolicy.matchPriority(
                                                displayName = contact.displayName,
                                                nickname = nickname,
                                                query = queryContext,
                                        )
                                if (!DefaultSearchMatcher.isMatch(priority)) {
                                        null
                                } else {
                                        Pair(contact, priority)
                                }
                        }
                        .sortedWith(
                                RecentResultRankingUtils.matchThenRecencyThenAlphabeticalComparator(
                                        recencyScores = recentContactScores,
                                        keySelector = { it.contactId },
                                        labelSelector = { it.displayName },
                                ),
                        )
                        .map { it.first }
        }

        private fun filterAndRankFiles(
                files: List<DeviceFile>,
                queryContext: SearchQueryContext,
                recentFileScores: Map<String, Int>,
        ): List<DeviceFile> {
                if (files.isEmpty()) return emptyList()

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
                                        FileSearchPolicy.matchPriority(
                                                displayName = file.displayName,
                                                nickname = nickname,
                                                query = queryContext,
                                        )
                                if (!DefaultSearchMatcher.isMatch(priority)) {
                                        null
                                } else {
                                        Pair(file, priority)
                                }
                        }
                        .sortedWith(
                                RecentResultRankingUtils.matchThenRecencyThenAlphabeticalComparator(
                                        recencyScores = recentFileScores,
                                        keySelector = { it.uri.toString() },
                                        labelSelector = { it.displayName },
                                ),
                        )
                        .map { it.first }
                        .take(FileSearchHandler.FILE_SEARCH_RESULT_LIMIT)
        }
}
