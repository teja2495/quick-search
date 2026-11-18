package com.tk.quicksearch.search.contacts.utils

import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils

/**
 * Resolves the global first/second contact button action for a specific contact, falling back
 * to Call (first button) or SMS (second button) when the contact can't use the chosen action.
 */
object ContactButtonResolver {
    fun resolveForContact(
        contactInfo: ContactInfo,
        action: ContactButtonAction,
        isPrimary: Boolean,
        phoneNumber: String? = null,
    ): ContactButtonAction =
        if (isAvailableForContact(contactInfo, action, phoneNumber)) {
            action
        } else {
            ContactButtonAction.fallback(isPrimary)
        }

    fun isAvailableForContact(
        contactInfo: ContactInfo,
        action: ContactButtonAction,
        phoneNumber: String? = null,
    ): Boolean =
        action.availableWithoutMethod ||
            if (action.allowsAnyProviderMethod) {
                contactInfo.contactMethods.any { action.provider?.owns(it) == true }
            } else {
                findMethod(contactInfo, action, phoneNumber) != null
            }

    /** The contact method that performs [action], matched to [phoneNumber] when given. */
    fun findMethod(
        contactInfo: ContactInfo,
        action: ContactButtonAction,
        phoneNumber: String? = null,
    ): ContactMethod? =
        contactInfo.contactMethods.firstOrNull { method ->
            action.methodMatches(method) && (
                !action.usesNumberMatch || phoneNumber == null || method.data.isBlank() ||
                    PhoneNumberUtils.isSameNumber(method.data, phoneNumber)
            )
        }

    /** The [ContactCardAction] the contact's button performs when no per-contact action is set. */
    fun defaultCardAction(
        contactInfo: ContactInfo,
        action: ContactButtonAction,
        isPrimary: Boolean,
    ): ContactCardAction? {
        val resolved = resolveForContact(contactInfo, action, isPrimary)
        if (resolved == ContactButtonAction.EMAIL) {
            return findMethod(contactInfo, resolved)?.let { ContactCardAction.Email(it.data) }
        }
        val phoneNumber = contactInfo.phoneNumbers.firstOrNull() ?: return null
        val fallback =
            if (isPrimary) ContactCardAction.Phone(phoneNumber) else ContactCardAction.Sms(phoneNumber)
        resolved.cardAction?.let { return it(phoneNumber) }
        // Synced custom methods can differ in number format, so try the exact number before
        // falling back to another row for the same contact and action.
        val method = (findMethod(contactInfo, resolved, phoneNumber) ?: findMethod(contactInfo, resolved))
            as? ContactMethod.CustomApp ?: return fallback
        return ContactCardAction.CustomApp(
            phoneNumber = phoneNumber,
            mimeType = method.mimeType,
            packageName = method.packageName,
            dataId = method.dataId,
            displayLabel = method.displayLabel,
        )
    }
}
