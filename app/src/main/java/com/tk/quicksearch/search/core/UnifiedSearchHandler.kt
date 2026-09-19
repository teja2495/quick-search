package com.tk.quicksearch.search.core

import android.content.Context
import android.os.SystemClock
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.contacts.ContactSearchPolicy
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.files.FileSearchPolicy
import com.tk.quicksearch.search.files.FolderPathPatternMatcher
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicyResolver
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.data.ReminderRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.tk.quicksearch.searchEngines.SecondarySearchDataSource

data class UnifiedSearchResults(
        val contactResults: List<ContactInfo> = emptyList(),
        val fileResults: List<DeviceFile> = emptyList(),
        val settingResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val calendarEvents: List<CalendarEventInfo> = emptyList(),
        val reminderResults: List<ReminderInfo> = emptyList(),
        val noteResults: List<NoteInfo> = emptyList(),
        val appSettingResults: List<AppSettingResult> = emptyList(),
        val appShortcutResults: List<StaticShortcut> = emptyList(),
        val recencyIndex: RecentResultRankingUtils.RecencyIndex =
                RecentResultRankingUtils.RecencyIndex(),
)

data class UnifiedSectionSearchConfig(
        val shouldSearch: Boolean = false,
        val enableFuzzyMatching: Boolean = false,
)

sealed interface UnifiedSectionSearchResult {
        val section: SearchSection

        data class Skipped(override val section: SearchSection) : UnifiedSectionSearchResult

        data class Contacts(val results: List<ContactInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.CONTACTS
        }

        data class Files(val results: List<DeviceFile>) : UnifiedSectionSearchResult {
                override val section = SearchSection.FILES
        }

        data class Settings(
                val results: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
        ) : UnifiedSectionSearchResult {
                override val section = SearchSection.SETTINGS
        }

        data class Calendar(val results: List<CalendarEventInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.CALENDAR
        }

        data class Reminders(val results: List<ReminderInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.REMINDERS
        }

        data class Notes(val results: List<NoteInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.NOTES
        }

        data class AppSettings(val results: List<AppSettingResult>) : UnifiedSectionSearchResult {
                override val section = SearchSection.APP_SETTINGS
        }

        data class AppShortcuts(val results: List<StaticShortcut>) : UnifiedSectionSearchResult {
                override val section = SearchSection.APP_SHORTCUTS
        }
}

class UnifiedSearchHandler(
        private val context: Context,
        private val contactRepository: ContactRepository,
        private val calendarRepository: CalendarRepository,
        private val fileRepository: FileSearchRepository,
        private val notesRepository: NotesRepository,
        private val userPreferences: UserAppPreferences,
        private val settingsSearchHandler: DeviceSettingsSearchHandler,
        private val appSettingsSearchHandler: AppSettingsSearchHandler,
        private val appShortcutSearchHandler: AppShortcutSearchHandler,
        private val fileSearchHandler: FileSearchHandler,
        private val searchOperations: SearchOperations,
) : SecondarySearchDataSource {
        companion object {
                private const val ALIAS_CONTACT_RESULT_LIMIT = 60
                private const val ALIAS_FILE_RESULT_LIMIT = 60
                private const val LOW_RAM_ALIAS_CONTACT_RESULT_LIMIT = 35
                private const val LOW_RAM_ALIAS_FILE_RESULT_LIMIT = 35
                private const val CONTACT_FUZZY_CANDIDATE_BUFFER_MULTIPLIER = 10
                private const val FILE_FUZZY_CANDIDATE_BUFFER_MULTIPLIER = 10
        }

        private val isLowRamDevice by lazy { isLowRamDevice(context) }
        private val calendarPreferences by lazy { CalendarPreferences(context) }
        private val reminderRepository by lazy { ReminderRepository(context) }

        override suspend fun performSearch(
                query: String,
                enabledFileTypes: Set<FileType>,
                sectionSearchConfig: Map<SearchSection, UnifiedSectionSearchConfig>,
                showFolders: Boolean,
                showSystemFiles: Boolean,
                aliasSection: SearchSection?,
                sectionSearchDelayMillis: Map<SearchSection, Long>,
                onSectionResult: suspend (
                        result: UnifiedSectionSearchResult,
                        recencyIndex: RecentResultRankingUtils.RecencyIndex,
                ) -> Unit,
        ): UnifiedSearchResults =
                withContext(Dispatchers.IO) {
                        val searchStartedAt = SystemClock.elapsedRealtime()
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
                        val remindersConfig =
                                sectionSearchConfig[SearchSection.REMINDERS]
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
                        val canSearchReminders = remindersConfig.shouldSearch
                        val canSearchAppShortcuts = appShortcutsConfig.shouldSearch
                        val enableFuzzyContactSearch = contactsConfig.enableFuzzyMatching
                        val enableFuzzyFileSearch = filesConfig.enableFuzzyMatching
                        val enableFuzzySettingsSearch = settingsConfig.enableFuzzyMatching
                        val enableFuzzyAppSettingsSearch = appSettingsConfig.enableFuzzyMatching
                        val enableFuzzyAppShortcutSearch = appShortcutsConfig.enableFuzzyMatching
                        val isContactsAliasSearch = aliasSection == SearchSection.CONTACTS
                        val isFilesAliasSearch = aliasSection == SearchSection.FILES
                        val isCalendarAliasSearch = aliasSection == SearchSection.CALENDAR
                        val useLowRamAliasLimits = false
                        val contactResultLimit =
                                if (isContactsAliasSearch) {
                                        if (useLowRamAliasLimits) LOW_RAM_ALIAS_CONTACT_RESULT_LIMIT
                                        else ALIAS_CONTACT_RESULT_LIMIT
                                }
                                else SearchOperations.CONTACT_RESULT_LIMIT
                        val fileResultLimit =
                                if (isFilesAliasSearch) {
                                        if (useLowRamAliasLimits) LOW_RAM_ALIAS_FILE_RESULT_LIMIT
                                        else ALIAS_FILE_RESULT_LIMIT
                                }
                                else FileSearchHandler.FILE_SEARCH_RESULT_LIMIT
                        val contactFuzzyPolicy =
                                FuzzySearchPolicyResolver.effectivePolicy(
                                        section = SearchSection.CONTACTS,
                                        query = queryContext.normalizedQuery,
                                        isLowRamDevice = isLowRamDevice,
                                )
                        val fileFuzzyPolicy =
                                FuzzySearchPolicyResolver.effectivePolicy(
                                        section = SearchSection.FILES,
                                        query = queryContext.normalizedQuery,
                                        isLowRamDevice = isLowRamDevice,
                                )
                        val contactFuzzyCandidateLimit = contactFuzzyPolicy.candidateLimit
                        val fileFuzzyCandidateLimit = fileFuzzyPolicy.candidateLimit
                        val canUseFileFuzzySearch =
                                enableFuzzyFileSearch &&
                                        fileFuzzyPolicy.enabled
                        val nicknameOnlyFileUriHydrationLimit = fileResultLimit * 2
                        val calendarResultLimit =
                                if (isCalendarAliasSearch) {
                                        if (useLowRamAliasLimits) 35 else 60
                                } else {
                                        25
                                }

                        // Read all per-query preferences once to avoid repeated SharedPreferences
                        // I/O.
                        val excludedContactIds =
                                if (canSearchContacts) userPreferences.getExcludedContactIds()
                                else emptySet()
                        val allowNumberSearch =
                                canSearchContacts && userPreferences.isNumberSearchEnabled()
                        val useFuzzyContactSearch =
                                enableFuzzyContactSearch &&
                                        contactFuzzyPolicy.enabled &&
                                        !(allowNumberSearch && queryContext.normalizedQuery.any(Char::isDigit))
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
                                        userPreferences.getRecentResultOpens(),
                                        userPreferences.getRecentResultOpenCounts(),
                                        userPreferences.getRecentResultLastOpenedTimes(),
                                )
                        val secondaryRankingSignal = userPreferences.getSecondaryRankingSignal()

                        AppSearchPerformanceLogger.logTiming(
                                event = "secondarySearchPrepared",
                                elapsedMs = SystemClock.elapsedRealtime() - searchStartedAt,
                                slowThresholdMs = 25L,
                        ) {
                                "queryLength=${trimmedQuery.length} sections=${sectionSearchConfig.count { it.value.shouldSearch }}"
                        }

                        var contactResults: List<ContactInfo> = emptyList()
                        var fileResults: List<DeviceFile> = emptyList()
                        var settingsMatches: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                                emptyList()
                        var calendarMatches: List<CalendarEventInfo> = emptyList()
                        var appSettingsMatches: List<AppSettingResult> = emptyList()
                        var noteMatches: List<NoteInfo> = emptyList()
                        var reminderMatches: List<ReminderInfo> = emptyList()
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
                                                                                                useFuzzyContactSearch,
                                                                                        fuzzyCandidateLimit =
                                                                                                contactFuzzyCandidateLimit,
                                                                                        allowNumberSearch = allowNumberSearch,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Contacts) {
                                                                                val nicknameResults =
                                                                                        findNicknameOnlyContacts(
                                                                                                payload.results,
                                                                                                queryContext,
                                                                                                canSearchContacts,
                                                                                                excludedContactIds,
                                                                                        )
                                                                                val filteredResults =
                                                                                        filterAndRankContacts(
                                                                                                payload.results + nicknameResults,
                                                                                                queryContext,
                                                                                                recencyIndex.contactScores,
                                                                                                recencyIndex.contactOpenCounts,
                                                                                                secondaryRankingSignal,
                                                                                                useFuzzyContactSearch,
                                                                                                contactFuzzyPolicy.minimumScore,
                                                                                                contactFuzzyPolicy.maximumEditDistance,
                                                                                                contactResultLimit,
                                                                                                allowNumberSearch,
                                                                                        )
                                                                                contactResults =
                                                                                        contactRepository.hydrateContactsForDisplay(
                                                                                                filteredResults
                                                                                        )
                                                                                UnifiedSectionSearchResult.Contacts(
                                                                                        contactResults
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.CONTACTS
                                                                                )
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
                                                                                        recencyIndex.fileOpenCounts,
                                                                                        secondaryRankingSignal,
                                                                                        includeFuzzyCandidates =
                                                                                                canUseFileFuzzySearch,
                                                                                        resultLimit =
                                                                                                fileResultLimit,
                                                                                        fuzzyCandidateLimit =
                                                                                                fileFuzzyCandidateLimit,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Files) {
                                                                                val nicknameResults =
                                                                                        findNicknameOnlyFiles(
                                                                                                payload.results,
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
                                                                                fileResults =
                                                                                        filterAndRankFiles(
                                                                                                payload.results + nicknameResults,
                                                                                                queryContext,
                                                                                                recencyIndex.fileScores,
                                                                                                recencyIndex.fileOpenCounts,
                                                                                                secondaryRankingSignal,
                                                                                                canUseFileFuzzySearch,
                                                                                                fileFuzzyPolicy.minimumScore,
                                                                                                fileFuzzyPolicy.maximumEditDistance,
                                                                                                fileResultLimit,
                                                                                        )
                                                                                UnifiedSectionSearchResult.Files(
                                                                                        fileResults
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.FILES
                                                                                )
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
                                                                                        recentSettingScores = recencyIndex.settingScores,
                                                                                        settingOpenCounts = recencyIndex.settingOpenCounts,
                                                                                        secondaryRankingSignal = secondaryRankingSignal,
                                                                                        enableFuzzyMatching =
                                                                                                enableFuzzySettingsSearch,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Settings) {
                                                                                settingsMatches =
                                                                                        payload.results
                                                                                UnifiedSectionSearchResult.Settings(
                                                                                        settingsMatches
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.SETTINGS
                                                                                )
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.CALENDAR to
                                                        SectionSearchSpec(
                                                                section = SearchSection.CALENDAR,
                                                                shouldSearch = canSearchCalendar,
                                                                search = {
                                                                        SectionSearchResultPayload.Calendar(
                                                                                calendarRepository.searchFutureEventsByTitle(
                                                                                        query =
                                                                                                queryContext.normalizedQuery,
                                                                                        limit =
                                                                                                calendarResultLimit *
                                                                                                        4,
                                                                                        includePastEvents =
                                                                                                calendarPreferences.getIncludePastEvents(),
                                                                                ).filterNot {
                                                                                        excludedCalendarEventIds.contains(
                                                                                                it.eventId
                                                                                        )
                                                                                }
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Calendar) {
                                                                                val nicknameResults =
                                                                                        findNicknameOnlyCalendarEvents(
                                                                                                displayNameEvents = payload.results,
                                                                                                queryContext = queryContext,
                                                                                                canSearchCalendar = canSearchCalendar,
                                                                                                excludedCalendarEventIds = excludedCalendarEventIds,
                                                                                        )
                                                                                calendarMatches =
                                                                                        filterAndRankCalendarEvents(
                                                                                                events = payload.results + nicknameResults,
                                                                                                queryContext = queryContext,
                                                                                                recencyIndex = recencyIndex,
                                                                                                secondaryRankingSignal = secondaryRankingSignal,
                                                                                                resultLimit = calendarResultLimit,
                                                                                        )
                                                                                UnifiedSectionSearchResult.Calendar(
                                                                                        calendarMatches
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.CALENDAR
                                                                                )
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
                                                                                                recencyIndex.appSettingScores,
                                                                                        settingOpenCounts =
                                                                                                recencyIndex.appSettingOpenCounts,
                                                                                        secondaryRankingSignal =
                                                                                                secondaryRankingSignal,
                                                                                        enableFuzzyMatching =
                                                                                                enableFuzzyAppSettingsSearch,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.AppSettings) {
                                                                                appSettingsMatches =
                                                                                        payload.results
                                                                                UnifiedSectionSearchResult.AppSettings(
                                                                                        appSettingsMatches
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.APP_SETTINGS
                                                                                )
                                                                        }
                                                                },
                                                        ),
                                                SearchSection.REMINDERS to
                                                        SectionSearchSpec(
                                                                section = SearchSection.REMINDERS,
                                                                shouldSearch = canSearchReminders,
                                                                search = {
                                                                        SectionSearchResultPayload.Reminders(
                                                                                reminderRepository.searchReminders(
                                                                                        query = trimmedQuery,
                                                                                        includePastReminders =
                                                                                                userPreferences.getIncludePastReminders(),
                                                                                ),
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Reminders) {
                                                                                reminderMatches =
                                                                                        payload.results
                                                                                UnifiedSectionSearchResult.Reminders(
                                                                                        reminderMatches
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.REMINDERS
                                                                                )
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
                                                                                        recencyIndex.noteScores,
                                                                                        recencyIndex.noteOpenCounts,
                                                                                        secondaryRankingSignal,
                                                                                        includeContent =
                                                                                                aliasSection ==
                                                                                                        SearchSection.NOTES,
                                                                                ),
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.Notes) {
                                                                                noteMatches =
                                                                                        payload.results
                                                                                UnifiedSectionSearchResult.Notes(
                                                                                        noteMatches
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.NOTES
                                                                                )
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
                                                                                        recentShortcutScores = recencyIndex.appShortcutScores,
                                                                                        shortcutOpenCounts = recencyIndex.appShortcutOpenCounts,
                                                                                        secondaryRankingSignal = secondaryRankingSignal,
                                                                                        enableFuzzyMatching =
                                                                                                enableFuzzyAppShortcutSearch,
                                                                                )
                                                                        )
                                                                },
                                                                applyResult = { payload ->
                                                                        if (payload is SectionSearchResultPayload.AppShortcuts) {
                                                                                appShortcutMatches =
                                                                                        payload.results
                                                                                UnifiedSectionSearchResult.AppShortcuts(
                                                                                        appShortcutMatches
                                                                                )
                                                                        } else {
                                                                                UnifiedSectionSearchResult.Skipped(
                                                                                        SearchSection.APP_SHORTCUTS
                                                                                )
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
                                                        val delayMillis =
                                                                sectionSearchDelayMillis[spec.section] ?: 0L
                                                        if (spec.shouldSearch && delayMillis > 0L) {
                                                                delay(delayMillis)
                                                        }
                                                        val sectionStartedAt = SystemClock.elapsedRealtime()
                                                        val payload =
                                                                if (spec.shouldSearch) spec.search()
                                                                else SectionSearchResultPayload.Skipped
                                                        val result = spec.applyResult(payload)
                                                        AppSearchPerformanceLogger.logTiming(
                                                                event = "secondarySectionReady",
                                                                elapsedMs =
                                                                        SystemClock.elapsedRealtime() - sectionStartedAt,
                                                                slowThresholdMs = 100L,
                                                        ) {
                                                                "section=${spec.section} debounceMs=$delayMillis results=${result.resultCount()}"
                                                        }
                                                        onSectionResult(result, recencyIndex)
                                                        result
                                                }
                                        }
                                        .awaitAll()
                        }

                        AppSearchPerformanceLogger.logTiming(
                                event = "secondarySearchCompleted",
                                elapsedMs = SystemClock.elapsedRealtime() - searchStartedAt,
                                slowThresholdMs = 250L,
                        ) {
                                "queryLength=${trimmedQuery.length} sections=${sectionSearchConfig.count { it.value.shouldSearch }}"
                        }

                        return@withContext UnifiedSearchResults(
                                contactResults = contactResults,
                                fileResults = fileResults,
                                settingResults = settingsMatches,
                                calendarEvents = calendarMatches,
                                reminderResults = reminderMatches,
                                noteResults = noteMatches,
                                appSettingResults = appSettingsMatches,
                                appShortcutResults = appShortcutMatches,
                                recencyIndex = recencyIndex,
                        )
                }

        private fun shouldSkipSearch(query: String): Boolean = query.isBlank()

        private data class SectionSearchSpec(
                val section: SearchSection,
                val shouldSearch: Boolean,
                val search: suspend () -> SectionSearchResultPayload,
                val applyResult: suspend (SectionSearchResultPayload) -> UnifiedSectionSearchResult,
        )

        private fun UnifiedSectionSearchResult.resultCount(): Int =
                when (this) {
                        is UnifiedSectionSearchResult.Skipped -> 0
                        is UnifiedSectionSearchResult.Contacts -> results.size
                        is UnifiedSectionSearchResult.Files -> results.size
                        is UnifiedSectionSearchResult.Settings -> results.size
                        is UnifiedSectionSearchResult.Calendar -> results.size
                        is UnifiedSectionSearchResult.Reminders -> results.size
                        is UnifiedSectionSearchResult.Notes -> results.size
                        is UnifiedSectionSearchResult.AppSettings -> results.size
                        is UnifiedSectionSearchResult.AppShortcuts -> results.size
                }

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

                data class Reminders(
                        val results: List<ReminderInfo>,
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
                        calendarRepository.getEventsByIds(nicknameOnlyIds.filter { it > 0L }.toSet())
                } else {
                        emptyList()
                }
        }

        private fun filterAndRankContacts(
                contacts: List<ContactInfo>,
                queryContext: SearchQueryContext,
                recentContactScores: Map<Long, Int>,
                contactOpenCounts: Map<Long, Int>,
                secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal,
                enableFuzzyMatching: Boolean,
                fuzzyMinScore: Int,
                fuzzyMaxEditDistance: Int,
                resultLimit: Int,
                allowNumberSearch: Boolean,
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
                                                phoneNumbers = contact.phoneNumbers,
                                                allowNumberSearch = allowNumberSearch,
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
                                        openCounts = contactOpenCounts,
                                        secondaryRankingSignal = secondaryRankingSignal,
                                        keySelector = { it.contactId },
                                        labelSelector = { it.displayName },
                                ),
                        )
                        .map { it.first }
                        .take(resultLimit)

                if (!enableFuzzyMatching) return exactMatches
                if (exactMatches.size >= resultLimit) return exactMatches

                val remainingSlots = (resultLimit - exactMatches.size).coerceAtLeast(0)
                if (remainingSlots == 0) return exactMatches
                val exactContactIds = exactMatches.map { it.contactId }.toSet()
                val fuzzyCandidateBudget =
                        (remainingSlots * CONTACT_FUZZY_CANDIDATE_BUFFER_MULTIPLIER)
                                .coerceAtLeast(remainingSlots)
                val fuzzyMatches =
                        distinctContacts
                                .asSequence()
                                .filterNot { exactContactIds.contains(it.contactId) }
                                .take(fuzzyCandidateBudget)
                                .mapNotNull { contact ->
                                        val normalizedName =
                                                SearchTextNormalizer.normalizeForSearch(
                                                        contact.displayName
                                                )
                                        val nickname = contactNicknames[contact.contactId]
                                        val normalizedNickname =
                                                nickname
                                                        ?.let { SearchTextNormalizer.normalizeForSearch(it) }
                                        val fuzzyScore =
                                                FuzzyMatcher.score(
                                                        query = queryContext.normalizedQuery,
                                                        primaryTarget = normalizedName,
                                                        secondaryTarget = normalizedNickname,
                                                        maxEditDistance = fuzzyMaxEditDistance,
                                                )
                                        if (fuzzyScore < fuzzyMinScore) {
                                                null
                                        } else if (
                                                !ContactSearchPolicy.areAllQueryTokensCovered(
                                                        query = queryContext,
                                                        displayName = contact.displayName,
                                                        nickname = nickname,
                                                        fuzzyMinScore = fuzzyMinScore,
                                                        fuzzyMaxEditDistance = fuzzyMaxEditDistance,
                                                )
                                        ) {
                                                null
                                        } else {
                                                contact to fuzzyScore
                                        }
                                }
                                .sortedWith(
                                        compareByDescending<Pair<ContactInfo, Int>> { it.second }
                                                .thenByDescending {
                                                        when (secondaryRankingSignal) {
                                                                com.tk.quicksearch.search.models.SecondaryRankingSignal.RECENCY ->
                                                                        recentContactScores[it.first.contactId] ?: 0
                                                                com.tk.quicksearch.search.models.SecondaryRankingSignal.MOST_OPENED ->
                                                                        contactOpenCounts[it.first.contactId] ?: 0
                                                                com.tk.quicksearch.search.models.SecondaryRankingSignal.NONE -> 0
                                                        }
                                                }
                                                .thenBy { it.first.displayName.lowercase() },
                                )
                                .map { it.first }
                                .toList()

                return exactMatches + fuzzyMatches.take(remainingSlots)
        }

        private fun filterAndRankFiles(
                files: List<DeviceFile>,
                queryContext: SearchQueryContext,
                recentFileScores: Map<String, Int>,
                fileOpenCounts: Map<String, Int>,
                secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal,
                enableFuzzyMatching: Boolean,
                fuzzyMinScore: Int,
                fuzzyMaxEditDistance: Int,
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
                                        openCounts = fileOpenCounts,
                                        secondaryRankingSignal = secondaryRankingSignal,
                                        keySelector = { it.uri.toString() },
                                        labelSelector = { it.displayName },
                                ),
                        )
                        .map { it.first }
                        .take(resultLimit)

                if (!enableFuzzyMatching) return exactMatches
                if (exactMatches.size >= resultLimit) return exactMatches

                val remainingSlots = (resultLimit - exactMatches.size).coerceAtLeast(0)
                if (remainingSlots == 0) return exactMatches
                val exactFileUris = exactMatches.map { it.uri.toString() }.toSet()
                val fuzzyCandidateBudget =
                        (remainingSlots * FILE_FUZZY_CANDIDATE_BUFFER_MULTIPLIER)
                                .coerceAtLeast(remainingSlots)
                val fuzzyMatches =
                        distinctFiles
                                .asSequence()
                                .filterNot { exactFileUris.contains(it.uri.toString()) }
                                .take(fuzzyCandidateBudget)
                                .mapNotNull { file ->
                                        val uriString = file.uri.toString()
                                        val nickname = fileNicknames[uriString]
                                        val normalizedName =
                                                SearchTextNormalizer.normalizeForSearch(
                                                        file.displayName
                                                )
                                        val normalizedNickname =
                                                nickname
                                                        ?.let { SearchTextNormalizer.normalizeForSearch(it) }
                                        val fuzzyScore =
                                                FuzzyMatcher.score(
                                                        query = queryContext.normalizedQuery,
                                                        primaryTarget = normalizedName,
                                                        secondaryTarget = normalizedNickname,
                                                        maxEditDistance = fuzzyMaxEditDistance,
                                                )
                                        if (fuzzyScore < fuzzyMinScore) {
                                                null
                                        } else if (
                                                !FileSearchPolicy.areAllQueryTokensCovered(
                                                        query = queryContext,
                                                        displayName = file.displayName,
                                                        nickname = nickname,
                                                        fuzzyMinScore = fuzzyMinScore,
                                                        fuzzyMaxEditDistance = fuzzyMaxEditDistance,
                                                )
                                        ) {
                                                null
                                        } else {
                                                file to fuzzyScore
                                        }
                                }
                                .sortedWith(
                                        compareByDescending<Pair<DeviceFile, Int>> { it.second }
                                                .thenByDescending {
                                                        val key = it.first.uri.toString()
                                                        when (secondaryRankingSignal) {
                                                                com.tk.quicksearch.search.models.SecondaryRankingSignal.RECENCY ->
                                                                        recentFileScores[key] ?: 0
                                                                com.tk.quicksearch.search.models.SecondaryRankingSignal.MOST_OPENED ->
                                                                        fileOpenCounts[key] ?: 0
                                                                com.tk.quicksearch.search.models.SecondaryRankingSignal.NONE -> 0
                                                        }
                                                }
                                                .thenBy { it.first.displayName.lowercase() },
                                )
                                .map { it.first }
                                .toList()

                return exactMatches + fuzzyMatches.take(remainingSlots)
        }

        private fun filterAndRankCalendarEvents(
                events: List<CalendarEventInfo>,
                queryContext: SearchQueryContext,
                recencyIndex: RecentResultRankingUtils.RecencyIndex,
                secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal,
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
                                        .thenByDescending {
                                                when (secondaryRankingSignal) {
                                                        com.tk.quicksearch.search.models.SecondaryRankingSignal.RECENCY ->
                                                                recencyIndex.calendarLastOpenedTimes[it.first.eventId] ?: 0L
                                                        com.tk.quicksearch.search.models.SecondaryRankingSignal.MOST_OPENED ->
                                                                (recencyIndex.calendarOpenCounts[it.first.eventId] ?: 0).toLong()
                                                        com.tk.quicksearch.search.models.SecondaryRankingSignal.NONE -> 0L
                                                }
                                        }
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
