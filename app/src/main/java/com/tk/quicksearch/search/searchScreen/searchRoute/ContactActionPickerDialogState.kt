package com.tk.quicksearch.search.searchScreen.searchRoute

import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.ContactInfo

data class ContactActionPickerDialogState(
    val contact: ContactInfo,
    val isPrimary: Boolean,
    val currentAction: com.tk.quicksearch.search.contacts.models.ContactCardAction?,
)