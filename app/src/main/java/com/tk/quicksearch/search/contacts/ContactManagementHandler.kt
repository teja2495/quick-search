package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles contact management operations like pinning, excluding, and nicknames.
 */
class ContactManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    fun pinContact(contactInfo: ContactInfo) {
        // Update UI immediately (optimistic)
        onUiStateUpdate { state ->
            if (state.pinnedContacts.any { it.contactId == contactInfo.contactId }) {
                state
            } else {
                state.copy(pinnedContacts = state.pinnedContacts + contactInfo)
            }
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.pinContact(contactInfo.contactId)
            onStateChanged()
        }
    }

    fun unpinContact(contactInfo: ContactInfo) {
        // Update UI immediately
        onUiStateUpdate {
            it.copy(
                pinnedContacts = it.pinnedContacts.filterNot { pinned -> pinned.contactId == contactInfo.contactId }
            )
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.unpinContact(contactInfo.contactId)
            onStateChanged()
        }
    }

    fun excludeContact(contactInfo: ContactInfo) {
        scope.launch(Dispatchers.IO) {
            userPreferences.excludeContact(contactInfo.contactId)
            // Removing from pinned if present
            if (userPreferences.getPinnedContactIds().contains(contactInfo.contactId)) {
                userPreferences.unpinContact(contactInfo.contactId)
            }

            // Update current state to reflect exclusion immediately
            onUiStateUpdate {
                it.copy(
                    contactResults = it.contactResults.filterNot { result -> result.contactId == contactInfo.contactId },
                    pinnedContacts = it.pinnedContacts.filterNot { pinned -> pinned.contactId == contactInfo.contactId }
                )
            }
            onStateChanged()
        }
    }

    fun removeExcludedContact(contactInfo: ContactInfo) {
        // Update UI immediately (optimistic)
        onUiStateUpdate { state ->
            state.copy(
                excludedContacts = state.excludedContacts.filterNot { it.contactId == contactInfo.contactId }
            )
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.removeExcludedContact(contactInfo.contactId)
            onStateChanged()
        }
    }

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setContactNickname(contactInfo.contactId, nickname)
            onStateChanged()
        }
    }

    fun getContactNickname(contactId: Long): String? {
        return userPreferences.getContactNickname(contactId)
    }

    fun clearAllExcludedContacts() {
        scope.launch(Dispatchers.IO) {
            userPreferences.clearAllExcludedContacts()
            onStateChanged()
        }
    }
}
