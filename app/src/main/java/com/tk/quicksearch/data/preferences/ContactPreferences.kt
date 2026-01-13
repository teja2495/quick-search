package com.tk.quicksearch.data.preferences

import android.content.Context

/**
 * Preferences for contact-related settings such as pinned/excluded contacts and phone number preferences.
 */
class ContactPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = getLongSet(ContactPreferences.KEY_PINNED_CONTACT_IDS)

    fun getExcludedContactIds(): Set<Long> = getLongSet(ContactPreferences.KEY_EXCLUDED_CONTACT_IDS)

    fun pinContact(contactId: Long): Set<Long> = updateLongSet(ContactPreferences.KEY_PINNED_CONTACT_IDS) {
        it.add(contactId.toString())
    }

    fun unpinContact(contactId: Long): Set<Long> = updateLongSet(ContactPreferences.KEY_PINNED_CONTACT_IDS) {
        it.remove(contactId.toString())
    }

    fun excludeContact(contactId: Long): Set<Long> = updateLongSet(ContactPreferences.KEY_EXCLUDED_CONTACT_IDS) {
        it.add(contactId.toString())
    }

    fun removeExcludedContact(contactId: Long): Set<Long> = updateLongSet(ContactPreferences.KEY_EXCLUDED_CONTACT_IDS) {
        it.remove(contactId.toString())
    }

    fun clearAllExcludedContacts(): Set<Long> = clearLongSet(ContactPreferences.KEY_EXCLUDED_CONTACT_IDS)

    fun getPreferredPhoneNumber(contactId: Long): String? {
        return prefs.getString("${ContactPreferences.KEY_PREFERRED_PHONE_PREFIX}$contactId", null)
    }

    fun setPreferredPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("${ContactPreferences.KEY_PREFERRED_PHONE_PREFIX}$contactId", phoneNumber).apply()
    }

    fun getLastShownPhoneNumber(contactId: Long): String? {
        return prefs.getString("${ContactPreferences.KEY_LAST_SHOWN_PHONE_PREFIX}$contactId", null)
    }

    fun setLastShownPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("${ContactPreferences.KEY_LAST_SHOWN_PHONE_PREFIX}$contactId", phoneNumber).apply()
    }

    fun isDirectDialEnabled(): Boolean = getBooleanPref(ContactPreferences.KEY_DIRECT_DIAL_ENABLED, false)

    fun setDirectDialEnabled(enabled: Boolean) {
        setBooleanPref(ContactPreferences.KEY_DIRECT_DIAL_ENABLED, enabled)
    }

    fun hasSeenDirectDialChoice(): Boolean = getBooleanPref(ContactPreferences.KEY_DIRECT_DIAL_CHOICE_SHOWN, false)

    fun setHasSeenDirectDialChoice(seen: Boolean) {
        setBooleanPref(ContactPreferences.KEY_DIRECT_DIAL_CHOICE_SHOWN, seen)
    }

    fun isDirectDialManuallyDisabled(): Boolean = getBooleanPref(ContactPreferences.KEY_DIRECT_DIAL_MANUALLY_DISABLED, false)

    fun setDirectDialManuallyDisabled(disabled: Boolean) {
        setBooleanPref(ContactPreferences.KEY_DIRECT_DIAL_MANUALLY_DISABLED, disabled)
    }

    companion object {
        // Contact preferences keys
        const val KEY_PINNED_CONTACT_IDS = "pinned_contact_ids"
        const val KEY_EXCLUDED_CONTACT_IDS = "excluded_contact_ids"
        const val KEY_PREFERRED_PHONE_PREFIX = "preferred_phone_"
        const val KEY_LAST_SHOWN_PHONE_PREFIX = "last_shown_phone_"
        const val KEY_DIRECT_DIAL_ENABLED = "direct_dial_enabled"
        const val KEY_DIRECT_DIAL_CHOICE_SHOWN = "direct_dial_choice_shown"
        const val KEY_DIRECT_DIAL_MANUALLY_DISABLED = "direct_dial_manually_disabled"
    }
}
