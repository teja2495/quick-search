package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.utils.SearchQueryContext

class SearchOperations(
    private val contactRepository: ContactRepository,
) {
    companion object {
        const val CONTACT_RESULT_LIMIT = 25

        private const val SHORT_QUERY_PREFETCH_MULTIPLIER = 10
        private const val DEFAULT_CONTACT_PREFETCH_MULTIPLIER = 4
        private const val SHORT_QUERY_LENGTH_THRESHOLD = 3
        private const val FUZZY_CONTACT_CANDIDATE_LIMIT = 300
    }

    suspend fun searchContacts(
        queryContext: SearchQueryContext,
        excludedContactIds: Set<Long>,
        limit: Int = CONTACT_RESULT_LIMIT,
        enableFuzzyMatching: Boolean = false,
        fuzzyCandidateLimit: Int = FUZZY_CONTACT_CANDIDATE_LIMIT,
        allowNumberSearch: Boolean = false,
    ): List<ContactInfo> {
        val prefetchMultiplier =
            if (queryContext.normalizedQuery.length <= SHORT_QUERY_LENGTH_THRESHOLD) {
                SHORT_QUERY_PREFETCH_MULTIPLIER
            } else {
                DEFAULT_CONTACT_PREFETCH_MULTIPLIER
            }

        val prefetchLimit =
            if (limit == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                limit * prefetchMultiplier
            }

        val exactResults =
            contactRepository
                .searchContacts(
                    query = queryContext.normalizedQuery,
                    limit = prefetchLimit,
                    allowNumberSearch = allowNumberSearch,
                )
                .filterNot { excludedContactIds.contains(it.contactId) }

        if (!enableFuzzyMatching) return exactResults

        val fuzzyCandidates =
            contactRepository
                .getRecentContacts(fuzzyCandidateLimit)
                .filterNot { excludedContactIds.contains(it.contactId) }

        return (exactResults + fuzzyCandidates).distinctBy { it.contactId }
    }
}
