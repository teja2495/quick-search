package com.tk.quicksearch.search.data.preferences

import android.content.Context

/**
 * Preferences for contact-related settings such as pinned/excluded contacts and phone number
 * preferences.
 */
class ContactPreferences(
    context: Context,
) : BasePreferences(context) {
    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_PINNED_CONTACT_IDS)

    fun getExcludedContactIds(): Set<Long> = getExcludedLongItems(BasePreferences.KEY_EXCLUDED_CONTACT_IDS)

    fun pinContact(contactId: Long): Set<Long> = pinLongItem(BasePreferences.KEY_PINNED_CONTACT_IDS, contactId)

    fun unpinContact(contactId: Long): Set<Long> = unpinLongItem(BasePreferences.KEY_PINNED_CONTACT_IDS, contactId)

    fun excludeContact(contactId: Long): Set<Long> = excludeLongItem(BasePreferences.KEY_EXCLUDED_CONTACT_IDS, contactId)

    fun removeExcludedContact(contactId: Long): Set<Long> = removeExcludedLongItem(BasePreferences.KEY_EXCLUDED_CONTACT_IDS, contactId)

    fun clearAllExcludedContacts(): Set<Long> = clearAllExcludedLongItems(BasePreferences.KEY_EXCLUDED_CONTACT_IDS)

    fun getPreferredPhoneNumber(contactId: Long): String? = prefs.getString("${BasePreferences.KEY_PREFERRED_PHONE_PREFIX}$contactId", null)

    fun setPreferredPhoneNumber(
        contactId: Long,
        phoneNumber: String,
    ) {
        prefs
            .edit()
            .putString("${BasePreferences.KEY_PREFERRED_PHONE_PREFIX}$contactId", phoneNumber)
            .apply()
    }

    fun getLastShownPhoneNumber(contactId: Long): String? =
        prefs.getString("${BasePreferences.KEY_LAST_SHOWN_PHONE_PREFIX}$contactId", null)

    fun setLastShownPhoneNumber(
        contactId: Long,
        phoneNumber: String,
    ) {
        prefs
            .edit()
            .putString("${BasePreferences.KEY_LAST_SHOWN_PHONE_PREFIX}$contactId", phoneNumber)
            .apply()
    }

    fun isDirectDialEnabled(): Boolean = getBooleanPref(BasePreferences.KEY_DIRECT_DIAL_ENABLED, false)

    fun setDirectDialEnabled(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_DIRECT_DIAL_ENABLED, enabled)
    }

    fun hasSeenDirectDialChoice(): Boolean = getBooleanPref(BasePreferences.KEY_DIRECT_DIAL_CHOICE_SHOWN, false)

    fun setHasSeenDirectDialChoice(seen: Boolean) {
        setBooleanPref(BasePreferences.KEY_DIRECT_DIAL_CHOICE_SHOWN, seen)
    }

    fun isDirectDialManuallyDisabled(): Boolean = getBooleanPref(BasePreferences.KEY_DIRECT_DIAL_MANUALLY_DISABLED, false)

    fun setDirectDialManuallyDisabled(disabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_DIRECT_DIAL_MANUALLY_DISABLED, disabled)
    }

    // ============================================================================
    // Custom Contact Card Actions
    // ============================================================================

    fun getPrimaryContactCardAction(contactId: Long): com.tk.quicksearch.search.contacts.models.ContactCardAction? {
        val serialized =
            prefs.getString(
                "${BasePreferences.KEY_CONTACT_PRIMARY_ACTION_PREFIX}$contactId",
                null,
            )
        return serialized?.let {
            com.tk.quicksearch.search.contacts.models.ContactCardAction
                .fromSerializedString(it)
        }
    }

    fun setPrimaryContactCardAction(
        contactId: Long,
        action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        prefs
            .edit()
            .putString(
                "${BasePreferences.KEY_CONTACT_PRIMARY_ACTION_PREFIX}$contactId",
                action.toSerializedString(),
            ).apply()
    }

    fun clearPrimaryContactCardAction(contactId: Long) {
        prefs
            .edit()
            .remove("${BasePreferences.KEY_CONTACT_PRIMARY_ACTION_PREFIX}$contactId")
            .apply()
    }

    fun getSecondaryContactCardAction(contactId: Long): com.tk.quicksearch.search.contacts.models.ContactCardAction? {
        val serialized =
            prefs.getString(
                "${BasePreferences.KEY_CONTACT_SECONDARY_ACTION_PREFIX}$contactId",
                null,
            )
        return serialized?.let {
            com.tk.quicksearch.search.contacts.models.ContactCardAction
                .fromSerializedString(it)
        }
    }

    fun setSecondaryContactCardAction(
        contactId: Long,
        action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ) {
        prefs
            .edit()
            .putString(
                "${BasePreferences.KEY_CONTACT_SECONDARY_ACTION_PREFIX}$contactId",
                action.toSerializedString(),
            ).apply()
    }

    fun clearSecondaryContactCardAction(contactId: Long) {
        prefs
            .edit()
            .remove("${BasePreferences.KEY_CONTACT_SECONDARY_ACTION_PREFIX}$contactId")
            .apply()
    }
}
