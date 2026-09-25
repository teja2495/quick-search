package com.tk.quicksearch.search.core

import android.content.Context
import android.os.SystemClock
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.contacts.ContactSearchPolicy
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
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

internal class UnifiedResultRanker(private val userPreferences: UserAppPreferences) {
        private companion object {
                const val CONTACT_FUZZY_CANDIDATE_BUFFER_MULTIPLIER = 10
                const val FILE_FUZZY_CANDIDATE_BUFFER_MULTIPLIER = 10
        }

        internal fun filterAndRankContacts(
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

        internal fun filterAndRankFiles(
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

        internal fun filterAndRankCalendarEvents(
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
