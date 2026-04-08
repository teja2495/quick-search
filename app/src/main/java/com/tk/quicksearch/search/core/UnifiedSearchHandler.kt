package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.contacts.ContactSearchPolicy
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.CustomCalendarEventRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.files.FileSearchPolicy
import com.tk.quicksearch.search.files.FolderPathPatternMatcher
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileClassifier
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.shared.util.isLowRamDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class UnifiedSearchResults(
        val contactResults: List<ContactInfo> = emptyList(),
        val fileResults: List<DeviceFile> = emptyList(),
        val settingResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val calendarEvents: List<CalendarEventInfo> = emptyList(),
        val noteResults: List<NoteInfo> = emptyList(),
        val appSettingResults: List<AppSettingResult> = emptyList(),
        val appShortcutResults: List<StaticShortcut> = emptyList(),
)

data class UnifiedSectionSearchConfig(
        val shouldSearch: Boolean = false,
        val enableFuzzyMatching: Boolean = false,
)

class UnifiedSearchHandler(
        private val context: Context,
        private val contactRepository: ContactRepository,
        private val calendarRepository: CalendarRepository,
        private val customCalendarEventRepository: CustomCalendarEventRepository,
        private val fileRepository: FileSearchRepository,
        private val notesRepository: NotesRepository,
        private val userPreferences: UserAppPreferences,
        private val settingsSearchHandler: DeviceSettingsSearchHandler,
        private val appSettingsSearchHandler: AppSettingsSearchHandler,
        private val appShortcutSearchHandler: AppShortcutSearchHandler,
        private val fileSearchHandler: FileSearchHandler,
        private val searchOperations: SearchOperations,
) {
        companion object {
                private const val DEFAULT_FUZZY_MIN_SCORE = 78
                private const val ALIAS_FUZZY_MIN_SCORE = 72
                private const val ALIAS_CONTACT_RESULT_LIMIT = 60
                private const val ALIAS_FILE_RESULT_LIMIT = 60
                private const val ALIAS_CONTACT_FUZZY_CANDIDATE_LIMIT = 600
                private const val ALIAS_FILE_FUZZY_CANDIDATE_LIMIT = 700
                private const val LOW_RAM_ALIAS_FUZZY_MIN_SCORE = 76
                private const val LOW_RAM_ALIAS_CONTACT_RESULT_LIMIT = 35
                private const val LOW_RAM_ALIAS_FILE_RESULT_LIMIT = 35
                private const val LOW_RAM_ALIAS_CONTACT_FUZZY_CANDIDATE_LIMIT = 360
                private const val LOW_RAM_ALIAS_FILE_FUZZY_CANDIDATE_LIMIT = 420
        }

        private val isLowRamDevice by lazy { isLowRamDevice(context) }
        private val calendarPreferences by lazy { CalendarPreferences(context) }

        suspend fun performSearch(
                query: String,
                enabledFileTypes: Set<FileType>,
                sectionSearchConfig: Map<SearchSection, UnifiedSectionSearchConfig>,
                showFolders: Boolean = true,
                showSystemFiles: Boolean = false,
                aliasSection: SearchSection? = null,
        ): UnifiedSearchResults =
                withContext(Dispatchers.IO) {
                        val trimmedQuery = query.trim()
                        if (shouldSkipSearch(trimmedQuery)) {
                                return@withContext UnifiedSearchResults()
                        }

                        val queryContext = SearchQueryContext.fromRawQuery(trimmedQuery)
                        val contactsConfig =
                                sectionSearchConfig[SearchSection.CONTACTS]
                                        ?: UnifiedSectionSearchConfig()
                        val filesConfig =
                                sectionSearchConfig[SearchSection.FILES]
                                        ?: UnifiedSectionSearchConfig()
                        val settingsConfig =
                                sectionSearchConfig[SearchSection.SETTINGS]
                                        ?: UnifiedSectionSearchConfig()
                        val calendarConfig =
                                sectionSearchConfig[SearchSection.CALENDAR]
                                        ?: UnifiedSectionSearchConfig()
                        val appSettingsConfig =
                                sectionSearchConfig[SearchSection.APP_SETTINGS]
                                        ?: UnifiedSectionSearchConfig()
                        val notesConfig =
                                sectionSearchConfig[SearchSection.NOTES]
                                        ?: UnifiedSectionSearchConfig()
                        val appShortcutsConfig =
                                sectionSearchConfig[SearchSection.APP_SHORTCUTS]
                                        ?: UnifiedSectionSearchConfig()

                        val canSearchContacts = contactsConfig.shouldSearch
                        val canSearchFiles = filesConfig.shouldSearch
                        val canSearchSettings = settingsConfig.shouldSearch
                        val canSearchCalendar = calendarConfig.shouldSearch
                        val canSearchAppSettings = appSettingsConfig.shouldSearch
                        val canSearchNotes = notesConfig.shouldSearch
                        val canSearchAppShortcuts = appShortcutsConfig.shouldSearch
                        val enableFuzzyContactSearch = contactsConfig.enableFuzzyMatching
                        val enableFuzzyFileSearch = filesConfig.enableFuzzyMatching
                        val enableFuzzySettingsSearch = settingsConfig.enableFuzzyMatching
                        val enableFuzzyAppSettingsSearch = appSettingsConfig.enableFuzzyMatching
                        val isContactsAliasSearch = aliasSection == SearchSection.CONTACTS
                        val isFilesAliasSearch = aliasSection == SearchSection.FILES
                        val isCalendarAliasSearch = aliasSection == SearchSection.CALENDAR
                        val contactResultLimit =
                                if (isContactsAliasSearch) {
                                        if (isLowRamDevice) LOW_RAM_ALIAS_CONTACT_RESULT_LIMIT
                                        else ALIAS_CONTACT_RESULT_LIMIT
                                }
                                else SearchOperations.CONTACT_RESULT_LIMIT
                        val fileResultLimit =
                                if (isFilesAliasSearch) {
                                        if (isLowRamDevice) LOW_RAM_ALIAS_FILE_RESULT_LIMIT
                                        else ALIAS_FILE_RESULT_LIMIT
                                }
                                else FileSearchHandler.FILE_SEARCH_RESULT_LIMIT
                        val fuzzyMinScore =
                                if (isContactsAliasSearch || isFilesAliasSearch) {
                                        if (isLowRamDevice) LOW_RAM_ALIAS_FUZZY_MIN_SCORE
                                        else ALIAS_FUZZY_MIN_SCORE
                                } else {
                                        DEFAULT_FUZZY_MIN_SCORE
                                }
                        val contactFuzzyCandidateLimit =
                                if (isContactsAliasSearch) {
                                        if (isLowRamDevice) LOW_RAM_ALIAS_CONTACT_FUZZY_CANDIDATE_LIMIT
                                        else ALIAS_CONTACT_FUZZY_CANDIDATE_LIMIT
                                }
                                else SearchOperations.CONTACT_RESULT_LIMIT * 12
                        val fileFuzzyCandidateLimit =
                                if (isFilesAliasSearch) {
                                        if (isLowRamDevice) LOW_RAM_ALIAS_FILE_FUZZY_CANDIDATE_LIMIT
                                        else ALIAS_FILE_FUZZY_CANDIDATE_LIMIT
                                }
                                else FileSearchHandler.FILE_SEARCH_RESULT_LIMIT * 14
                        val nicknameOnlyFileUriHydrationLimit = fileResultLimit * 2
                        val calendarResultLimit =
                                if (isCalendarAliasSearch) {
                                        if (isLowRamDevice) 35 else 60
                                } else {
                                        25
                                }

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
                        val excludedCalendarEventIds =
                                if (canSearchCalendar)
                                        userPreferences.getExcludedCalendarEventIds()
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

                        var contactResults: List<ContactInfo> = emptyList()
                        var fileResults: List<DeviceFile> = emptyList()
                        var settingsMatches: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                                emptyList()
                        var calendarMatches: List<CalendarEventInfo> = emptyList()
                        var appSettingsMatches: List<AppSettingResult> = emptyList()
                        var noteMatches: List<NoteInfo> = emptyList()
                        var appShortcutMatches: List<StaticShortcut> = emptyList()

                        coroutineScope {
                                val sectionSearchSpecBySection =
                                        mapOf(
                                                SearchSection.CONTACTS to
                                                        SectionSearchSpec(
                                                                section = SearchSection.CONTACTS,
                                                                shouldSearch = canSearchContacts,
                                                                search = {
                                                                        SectionSearchResultPayload.Contacts(
                                                                                searchOperations.searchContacts(
                                                                                        queryContext,
                                                                                        excludedContactIds,
                                                                                        limit =
                                                                                                contactResultLimit,
                                                                                        enableFuzzyMatching =
                                                                                                enableFuzzyContactSearch,
                                                                                        fuzzyCandidateLimit =
                                                                                                contactFuzzyCandidateLimit,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Contacts) {
                                                                                contactResults =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.FILES to
                                                        SectionSearchSpec(
                                                                section = SearchSection.FILES,
                                                                shouldSearch = canSearchFiles,
                                                                search = {
                                                                        SectionSearchResultPayload.Files(
                                                                                fileSearchHandler.searchFiles(
                                                                                        queryContext,
                                                                                        enabledFileTypes,
                                                                                        excludedFileUris,
                                                                                        excludedFileExtensions,
                                                                                        folderWhitelistPatterns,
                                                                                        folderBlacklistPatterns,
                                                                                        showFolders,
                                                                                        showSystemFiles,
                                                                                        recencyIndex.fileScores,
                                                                                        includeFuzzyCandidates =
                                                                                                enableFuzzyFileSearch,
                                                                                        resultLimit =
                                                                                                fileResultLimit,
                                                                                        fuzzyCandidateLimit =
                                                                                                fileFuzzyCandidateLimit,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Files) {
                                                                                fileResults =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.SETTINGS to
                                                        SectionSearchSpec(
                                                                section = SearchSection.SETTINGS,
                                                                shouldSearch = canSearchSettings,
                                                                search = {
                                                                        SectionSearchResultPayload.Settings(
                                                                                settingsSearchHandler.searchSettings(
                                                                                        queryContext,
                                                                                        recencyIndex.settingScores,
                                                                                        enableFuzzyMatching =
                                                                                                enableFuzzySettingsSearch,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Settings) {
                                                                                settingsMatches =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.CALENDAR to
                                                        SectionSearchSpec(
                                                                section = SearchSection.CALENDAR,
                                                                shouldSearch = canSearchCalendar,
                                                                search = {
                                                                        SectionSearchResultPayload.Calendar(
                                                                                (calendarRepository.searchFutureEventsByTitle(
                                                                                        query =
                                                                                                queryContext.normalizedQuery,
                                                                                        limit =
                                                                                                calendarResultLimit *
                                                                                                        4,
                                                                                        includePastEvents =
                                                                                                calendarPreferences.getIncludePastEvents(),
                                                                                ) + customCalendarEventRepository.searchCustomEvents(
                                                                                        query = queryContext.normalizedQuery,
                                                                                        includePastEvents = calendarPreferences.getIncludePastEvents(),
                                                                                )).filterNot {
                                                                                        excludedCalendarEventIds.contains(
                                                                                                it.eventId
                                                                                        )
                                                                                }
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Calendar) {
                                                                                calendarMatches =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.APP_SETTINGS to
                                                        SectionSearchSpec(
                                                                section = SearchSection.APP_SETTINGS,
                                                                shouldSearch = canSearchAppSettings,
                                                                search = {
                                                                        SectionSearchResultPayload.AppSettings(
                                                                                appSettingsSearchHandler.searchSettings(
                                                                                        queryContext =
                                                                                                queryContext,
                                                                                        recentSettingScores =
                                                                                                recencyIndex.settingScores,
                                                                                        enableFuzzyMatching =
                                                                                                enableFuzzyAppSettingsSearch,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.AppSettings) {
                                                                                appSettingsMatches =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.NOTES to
                                                        SectionSearchSpec(
                                                                section = SearchSection.NOTES,
                                                                shouldSearch = canSearchNotes,
                                                                search = {
                                                                        SectionSearchResultPayload.Notes(
                                                                                notesRepository.searchNotes(
                                                                                        trimmedQuery,
                                                                                ),
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Notes) {
                                                                                noteMatches =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.APP_SHORTCUTS to
                                                        SectionSearchSpec(
                                                                section = SearchSection.APP_SHORTCUTS,
                                                                shouldSearch = canSearchAppShortcuts,
                                                                search = {
                                                                        SectionSearchResultPayload.AppShortcuts(
                                                                                appShortcutSearchHandler.searchShortcuts(
                                                                                        queryContext,
                                                                                        recencyIndex.appShortcutScores,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.AppShortcuts) {
                                                                                appShortcutMatches =
                                                                                        payload.results
                                                                        }
                                                                },
                                                        ),
                                        )

                                val sectionSearchSpecs =
                                        SearchSectionRegistry.secondarySearchDefinitions.mapNotNull {
                                                sectionSearchSpecBySection[it.section]
                                        }

                                sectionSearchSpecs
                                        .map { spec ->
                                                async {
                                                        spec to
                                                                if (spec.shouldSearch) spec.search()
                                                                else SectionSearchResultPayload.Skipped
                                                }
                                        }
                                        .awaitAll()
                                        .forEach { (spec, payload) ->
                                                spec.applyResult(payload)
                                        }
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
                                        nicknameOnlyFileUriHydrationLimit,
                                )
                        val nicknameCalendarEvents =
                                findNicknameOnlyCalendarEvents(
                                        displayNameEvents = calendarMatches,
                                        queryContext = queryContext,
                                        canSearchCalendar = canSearchCalendar,
                                        excludedCalendarEventIds = excludedCalendarEventIds,
                                )

                        // Combine and filter results
                        val filteredContacts =
                                        filterAndRankContacts(
                                                contactResults + nicknameContacts,
                                                queryContext,
                                                recencyIndex.contactScores,
                                                enableFuzzyContactSearch,
                                                fuzzyMinScore,
                                        )
                                        .take(contactResultLimit)
                        val filteredFiles =
                                filterAndRankFiles(
                                        fileResults + nicknameFiles,
                                        queryContext,
                                        recencyIndex.fileScores,
                                        enableFuzzyFileSearch,
                                        fuzzyMinScore,
                                        fileResultLimit,
                                )
                        val filteredCalendarEvents =
                                filterAndRankCalendarEvents(
                                        events = calendarMatches + nicknameCalendarEvents,
                                        queryContext = queryContext,
                                        resultLimit = calendarResultLimit,
                                )

                        val hydratedContacts =
                                contactRepository.hydrateContactsForDisplay(filteredContacts)

                        return@withContext UnifiedSearchResults(
                                contactResults = hydratedContacts,
                                fileResults = filteredFiles,
                                settingResults = settingsMatches,
                                calendarEvents = filteredCalendarEvents,
                                noteResults = noteMatches,
                                appSettingResults = appSettingsMatches,
                                appShortcutResults = appShortcutMatches,
                        )
                }

        private fun shouldSkipSearch(query: String): Boolean = query.isBlank()

        private data class SectionSearchSpec(
                val section: SearchSection,
                val shouldSearch: Boolean,
                val search: suspend () -> SectionSearchResultPayload,
                val applyResult: (SectionSearchResultPayload) -> Unit,
        )

        private sealed interface SectionSearchResultPayload {
                data object Skipped : SectionSearchResultPayload

                data class Contacts(
                        val results: List<ContactInfo>,
                ) : SectionSearchResultPayload

                data class Files(
                        val results: List<DeviceFile>,
                ) : SectionSearchResultPayload

                data class Settings(
                        val results: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
                ) : SectionSearchResultPayload

                data class Calendar(
                        val results: List<CalendarEventInfo>,
                ) : SectionSearchResultPayload

                data class AppSettings(
                        val results: List<AppSettingResult>,
                ) : SectionSearchResultPayload

                data class Notes(
                        val results: List<NoteInfo>,
                ) : SectionSearchResultPayload

                data class AppShortcuts(
                        val results: List<StaticShortcut>,
                ) : SectionSearchResultPayload
        }

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
                nicknameOnlyFileUriHydrationLimit: Int,
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
                        nicknameOnlyUris.take(nicknameOnlyFileUriHydrationLimit)
                val pathMatcher =
                        FolderPathPatternMatcher.createPathMatcher(
                                whitelistPatterns = folderWhitelistPatterns,
                                blacklistPatterns = folderBlacklistPatterns,
                        )

                return if (limitedNicknameOnlyUris.isNotEmpty()) {
                        fileRepository.getFilesByUris(limitedNicknameOnlyUris.toSet()).filter { file ->
                                val isSystem =
                                        FileClassifier.isSystemFolder(file) ||
                                                FileClassifier.isSystemFile(file)
                                if (file.isDirectory) {
                                        if (!showFolders) return@filter false
                                } else {
                                        val fileType =
                                                com.tk.quicksearch.search.models.FileTypeUtils
                                                        .getFileType(file)
                                        if (fileType !in enabledFileTypes) return@filter false
                                        if (fileType == FileType.OTHER && isSystem) return@filter false
                                }

                                if (isSystem && !showSystemFiles) return@filter false

                                val isHidden = file.displayName.startsWith(".")
                                if (isHidden && !showSystemFiles) return@filter false

                                if (!showSystemFiles && FileClassifier.isInTrashFolder(file))
                                        return@filter false

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

        private suspend fun findNicknameOnlyCalendarEvents(
                displayNameEvents: List<CalendarEventInfo>,
                queryContext: SearchQueryContext,
                canSearchCalendar: Boolean,
                excludedCalendarEventIds: Set<Long>,
        ): List<CalendarEventInfo> {
                if (!canSearchCalendar) return emptyList()

                val nicknameMatchingEventIds =
                        userPreferences.findCalendarEventsWithMatchingNickname(queryContext.normalizedQuery).filterNot {
                                excludedCalendarEventIds.contains(it)
                        }
                if (nicknameMatchingEventIds.isEmpty()) return emptyList()

                val displayNameMatchedIds = displayNameEvents.map { it.eventId }.toSet()
                val nicknameOnlyIds =
                        nicknameMatchingEventIds.filterNot { displayNameMatchedIds.contains(it) }

                return if (nicknameOnlyIds.isNotEmpty()) {
                        calendarRepository.getEventsByIds(nicknameOnlyIds.toSet())
                } else {
                        emptyList()
                }
        }

        private fun filterAndRankContacts(
                contacts: List<ContactInfo>,
                queryContext: SearchQueryContext,
                recentContactScores: Map<Long, Int>,
                enableFuzzyMatching: Boolean,
                fuzzyMinScore: Int,
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

                val exactMatches =
                        distinctContacts
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

                if (!enableFuzzyMatching) return exactMatches

                val exactContactIds = exactMatches.map { it.contactId }.toSet()
                val fuzzyMatches =
                        distinctContacts
                                .asSequence()
                                .filterNot { exactContactIds.contains(it.contactId) }
                                .mapNotNull { contact ->
                                        val normalizedName =
                                                SearchTextNormalizer.normalizeForSearch(
                                                        contact.displayName
                                                )
                                        val normalizedNickname =
                                                contactNicknames[contact.contactId]
                                                        ?.let { SearchTextNormalizer.normalizeForSearch(it) }
                                        val fuzzyScore =
                                                FuzzyMatcher.score(
                                                        query = queryContext.normalizedQuery,
                                                        primaryTarget = normalizedName,
                                                        secondaryTarget = normalizedNickname,
                                                )
                                        if (fuzzyScore < fuzzyMinScore) {
                                                null
                                        } else {
                                                contact to fuzzyScore
                                        }
                                }
                                .sortedWith(
                                        compareByDescending<Pair<ContactInfo, Int>> { it.second }
                                                .thenBy { it.first.displayName.lowercase() },
                                )
                                .map { it.first }
                                .toList()

                return exactMatches + fuzzyMatches
        }

        private fun filterAndRankFiles(
                files: List<DeviceFile>,
                queryContext: SearchQueryContext,
                recentFileScores: Map<String, Int>,
                enableFuzzyMatching: Boolean,
                fuzzyMinScore: Int,
                resultLimit: Int,
        ): List<DeviceFile> {
                if (files.isEmpty()) return emptyList()

                // Pre-fetch all file nicknames in a single call to reduce SharedPreferences reads
                val distinctFiles = files.distinctBy { it.uri.toString() }
                val fileNicknames =
                        distinctFiles.associate { file ->
                                file.uri.toString() to
                                        userPreferences.getFileNickname(file.uri.toString())
                        }

                val exactMatches =
                        distinctFiles
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
                        .take(resultLimit)

                if (!enableFuzzyMatching) return exactMatches

                val exactFileUris = exactMatches.map { it.uri.toString() }.toSet()
                val fuzzyMatches =
                        distinctFiles
                                .asSequence()
                                .filterNot { exactFileUris.contains(it.uri.toString()) }
                                .mapNotNull { file ->
                                        val uriString = file.uri.toString()
                                        val normalizedName =
                                                SearchTextNormalizer.normalizeForSearch(
                                                        file.displayName
                                                )
                                        val normalizedNickname =
                                                fileNicknames[uriString]
                                                        ?.let { SearchTextNormalizer.normalizeForSearch(it) }
                                        val fuzzyScore =
                                                FuzzyMatcher.score(
                                                        query = queryContext.normalizedQuery,
                                                        primaryTarget = normalizedName,
                                                        secondaryTarget = normalizedNickname,
                                                )
                                        if (fuzzyScore < fuzzyMinScore) {
                                                null
                                        } else {
                                                file to fuzzyScore
                                        }
                                }
                                .sortedWith(
                                        compareByDescending<Pair<DeviceFile, Int>> { it.second }
                                                .thenBy { it.first.displayName.lowercase() },
                                )
                                .map { it.first }
                                .toList()

                return (exactMatches + fuzzyMatches).take(resultLimit)
        }

        private fun filterAndRankCalendarEvents(
                events: List<CalendarEventInfo>,
                queryContext: SearchQueryContext,
                resultLimit: Int,
        ): List<CalendarEventInfo> {
                if (events.isEmpty()) return emptyList()

                val distinctEvents = events.distinctBy { it.eventId }
                val now = System.currentTimeMillis()
                return distinctEvents
                        .mapNotNull { event ->
                                val nickname = userPreferences.getCalendarEventNickname(event.eventId)
                                val priority =
                                        com.tk.quicksearch.search.utils.SearchRankingUtils
                                                .calculateMatchPriorityWithNickname(
                                                        primaryText = event.title,
                                                        nickname = nickname,
                                                        normalizedQuery = queryContext.normalizedQuery,
                                                        queryTokens = queryContext.tokens,
                                                )
                                if (com.tk.quicksearch.search.utils.SearchRankingUtils.isOtherMatch(priority)) {
                                        null
                                } else {
                                        event to priority
                                }
                        }
                        .sortedWith(
                                compareBy<Pair<CalendarEventInfo, Int>> { it.second }
                                        .thenBy {
                                                calendarFutureFirstGroup(
                                                        startMillis = it.first.startMillis,
                                                        now = now,
                                                )
                                        }
                                        .thenBy {
                                                calendarFutureFirstOrderKey(
                                                        startMillis = it.first.startMillis,
                                                        now = now,
                                                )
                                        }
                                        .thenBy { it.first.title.lowercase() },
                        )
                        .map { it.first }
                        .take(resultLimit)
        }

        private fun calendarFutureFirstGroup(
                startMillis: Long,
                now: Long,
        ): Int = if (startMillis >= now) 0 else 1

        private fun calendarFutureFirstOrderKey(
                startMillis: Long,
                now: Long,
        ): Long = if (startMillis >= now) startMillis else Long.MAX_VALUE - startMillis
}
