package com.tk.quicksearch.model

/**
 * Surface-level snapshot of a contact that is renderable inside the quick search sheet.
 */
data class ContactInfo(
    val contactId: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<String>
) {
    val primaryNumber: String? = phoneNumbers.firstOrNull()
}


