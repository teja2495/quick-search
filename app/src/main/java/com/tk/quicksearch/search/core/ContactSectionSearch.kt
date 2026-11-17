package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.withQueryCancellation
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Contacts section of the secondary search: provider lookup, nickname-only matches, ranking, and
 * display hydration. Hydration is split so the top rows show before every row's photo and
 * app-specific contact methods are loaded; the rest follow in a second emission.
 */
internal class ContactSectionSearch(
        private val contactRepository: ContactRepository,
        private val userPreferences: UserAppPreferences,
        private val searchOperations: SearchOperations,
        private val resultRanker: UnifiedResultRanker,
) {
        internal companion object {
                /** Rows hydrated before the first emission; covers an expanded card's first screen. */
                const val EAGER_HYDRATION_COUNT = 12
        }

        internal class Request(
                val queryContext: SearchQueryContext,
                val excludedContactIds: Set<Long>,
                val resultLimit: Int,
                val enableFuzzyMatching: Boolean,
                val fuzzyCandidateLimit: Int,
                val fuzzyMinScore: Int,
                val fuzzyMaxEditDistance: Int,
                val allowNumberSearch: Boolean,
                val recencyIndex: RecentResultRankingUtils.RecencyIndex,
                val secondaryRankingSignal: SecondaryRankingSignal,
        )

        /**
         * Builds the section spec for one query. [onResults] receives every list the section
         * emits, so the caller's final results end up fully hydrated.
         */
        fun sectionSpec(
                shouldSearch: Boolean,
                request: Request,
                onResults: (List<ContactInfo>) -> Unit,
        ): SectionSearchSpec {
                // Ranked rows still waiting for their full hydration pass; null when none are left.
                var pendingRanked: List<ContactInfo>? = null
                return SectionSearchSpec(
                        section = SearchSection.CONTACTS,
                        shouldSearch = shouldSearch,
                        search = { SectionSearchResultPayload.Contacts(search(request)) },
                        applyResult = { payload ->
                                if (payload is SectionSearchResultPayload.Contacts) {
                                        val ranked = rank(request, payload.results)
                                        val shown =
                                                if (ranked.size > EAGER_HYDRATION_COUNT) {
                                                        pendingRanked = ranked
                                                        hydrate(ranked.take(EAGER_HYDRATION_COUNT)) +
                                                                ranked.drop(EAGER_HYDRATION_COUNT)
                                                } else {
                                                        hydrate(ranked)
                                                }
                                        onResults(shown)
                                        UnifiedSectionSearchResult.Contacts(shown)
                                } else {
                                        UnifiedSectionSearchResult.Skipped(SearchSection.CONTACTS)
                                }
                        },
                        followUp = {
                                val ranked = pendingRanked
                                pendingRanked = null
                                if (ranked == null) {
                                        null
                                } else {
                                        // The eager rows are in the repository's details cache, so
                                        // this pass only queries the remaining ones.
                                        val hydrated = hydrate(ranked)
                                        onResults(hydrated)
                                        UnifiedSectionSearchResult.Contacts(hydrated)
                                }
                        },
                )
        }

        /** Provider candidates; the provider query stops when the search is superseded. */
        private suspend fun search(request: Request): List<ContactInfo> =
                withQueryCancellation { cancellationSignal ->
                        searchOperations.searchContacts(
                                request.queryContext,
                                request.excludedContactIds,
                                limit = request.resultLimit,
                                enableFuzzyMatching = request.enableFuzzyMatching,
                                fuzzyCandidateLimit = request.fuzzyCandidateLimit,
                                allowNumberSearch = request.allowNumberSearch,
                                cancellationSignal = cancellationSignal,
                        )
                }

        private suspend fun rank(
                request: Request,
                candidates: List<ContactInfo>,
        ): List<ContactInfo> {
                val nicknameResults = findNicknameOnlyContacts(candidates, request)
                currentCoroutineContext().ensureActive()
                return resultRanker.filterAndRankContacts(
                        candidates + nicknameResults,
                        request.queryContext,
                        request.recencyIndex.contactScores,
                        request.recencyIndex.contactOpenCounts,
                        request.secondaryRankingSignal,
                        request.enableFuzzyMatching,
                        request.fuzzyMinScore,
                        request.fuzzyMaxEditDistance,
                        request.resultLimit,
                        request.allowNumberSearch,
                )
        }

        private suspend fun hydrate(contacts: List<ContactInfo>): List<ContactInfo> {
                if (contacts.isEmpty()) return contacts
                currentCoroutineContext().ensureActive()
                return withQueryCancellation { cancellationSignal ->
                        contactRepository.hydrateContactsForDisplay(contacts, cancellationSignal)
                }
        }

        /**
         * Contacts reachable only by nickname. They are loaded without photos or contact methods
         * because ranking needs just names; the shown rows are hydrated afterwards.
         */
        private fun findNicknameOnlyContacts(
                displayNameContacts: List<ContactInfo>,
                request: Request,
        ): List<ContactInfo> {
                val nicknameMatchingIds =
                        userPreferences
                                .findContactsWithMatchingNickname(request.queryContext.normalizedQuery)
                                .filterNot { request.excludedContactIds.contains(it) }
                if (nicknameMatchingIds.isEmpty()) return emptyList()

                val displayNameMatchedIds = displayNameContacts.mapTo(HashSet()) { it.contactId }
                val nicknameOnlyIds = nicknameMatchingIds.filterNotTo(HashSet()) { it in displayNameMatchedIds }
                return if (nicknameOnlyIds.isNotEmpty()) {
                        contactRepository.getContactsByIdsMinimal(nicknameOnlyIds)
                } else {
                        emptyList()
                }
        }
}
