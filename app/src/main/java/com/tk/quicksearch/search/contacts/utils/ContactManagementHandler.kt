package com.tk.quicksearch.search.contacts.utils

import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.ContactManagementConfig
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope

/**
 * Handles contact management operations like pinning, excluding, and nicknames.
 */
class ContactManagementHandler(
    userPreferences: UserAppPreferences,
    scope: CoroutineScope,
    onStateChanged: () -> Unit,
    onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) : ManagementHandler<ContactInfo> by GenericManagementHandler(
    ContactManagementConfig(),
    userPreferences,
    scope,
    onStateChanged,
    onUiStateUpdate
) {

    // Convenience methods that delegate to the interface
    fun pinContact(contactInfo: ContactInfo) = pinItem(contactInfo)
    fun unpinContact(contactInfo: ContactInfo) = unpinItem(contactInfo)
    fun excludeContact(contactInfo: ContactInfo) = excludeItem(contactInfo)
    fun removeExcludedContact(contactInfo: ContactInfo) = removeExcludedItem(contactInfo)
    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) = setItemNickname(contactInfo, nickname)
    fun getContactNickname(contactId: Long): String? = getItemNickname(ContactInfo(contactId, "", "", emptyList()))
    fun clearAllExcludedContacts() = clearAllExcludedItems()
}
