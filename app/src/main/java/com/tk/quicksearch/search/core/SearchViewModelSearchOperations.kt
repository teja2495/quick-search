package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.models.ContactInfo

class SearchOperations(
    private val contactRepository: ContactRepository,
) {
    companion object {
        const val CONTACT_RESULT_LIMIT = 25

        private const val SHORT_QUERY_PREFETCH_MULTIPLIER = 10
        private const val DEFAULT_CONTACT_PREFETCH_MULTIPLIER = 4
        private const val SHORT_QUERY_LENGTH_THRESHOLD = 3
    }

    suspend fun searchContacts(
        query: String,
        excludedContactIds: Set<Long>,
        limit: Int = CONTACT_RESULT_LIMIT,
    ): List<ContactInfo> {
        val prefetchMultiplier =
            if (query.trim().length <= SHORT_QUERY_LENGTH_THRESHOLD) {
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

        val results =
            contactRepository
                .searchContacts(query, prefetchLimit)
                .filterNot { excludedContactIds.contains(it.contactId) }

        return results
    }
}
