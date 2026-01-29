package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.models.ContactInfo
import kotlinx.coroutines.CoroutineScope

class SearchOperations(
    private val contactRepository: ContactRepository,
) {
    companion object {
        private const val CONTACT_RESULT_LIMIT = 5

        // Prefetch more than we display so ranking isn't biased by provider sort order.
        private const val CONTACT_PREFETCH_MULTIPLIER = 10
        private const val MIN_QUERY_LENGTH = 2
    }

    suspend fun performSearches(
        query: String,
        canSearchContacts: Boolean,
        excludedContactIds: Set<Long>,
        scope: CoroutineScope,
    ): List<ContactInfo> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_QUERY_LENGTH) {
            return emptyList()
        }

        return if (canSearchContacts) {
            searchContacts(trimmedQuery, excludedContactIds)
        } else {
            emptyList()
        }
    }

    suspend fun searchContacts(
        query: String,
        excludedContactIds: Set<Long>,
        limit: Int = CONTACT_RESULT_LIMIT,
    ): List<ContactInfo> {
        val prefetchLimit =
            if (limit == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                limit * CONTACT_PREFETCH_MULTIPLIER
            }

        val results =
            contactRepository
                .searchContacts(query, prefetchLimit)
                .filterNot { excludedContactIds.contains(it.contactId) }

        return if (limit == Int.MAX_VALUE) results else results.take(limit)
    }
}
