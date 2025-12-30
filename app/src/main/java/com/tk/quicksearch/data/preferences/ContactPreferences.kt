package com.tk.quicksearch.data.preferences

import android.content.Context

/**
 * Preferences for contact-related settings such as pinned/excluded contacts and phone number preferences.
 */
class ContactPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = getLongSet(KEY_PINNED_CONTACT_IDS)

    fun getExcludedContactIds(): Set<Long> = getLongSet(KEY_EXCLUDED_CONTACT_IDS)

    fun pinContact(contactId: Long): Set<Long> = updateLongSet(KEY_PINNED_CONTACT_IDS) {
        it.add(contactId.toString())
    }

    fun unpinContact(contactId: Long): Set<Long> = updateLongSet(KEY_PINNED_CONTACT_IDS) {
        it.remove(contactId.toString())
    }

    fun excludeContact(contactId: Long): Set<Long> = updateLongSet(KEY_EXCLUDED_CONTACT_IDS) {
        it.add(contactId.toString())
    }

    fun removeExcludedContact(contactId: Long): Set<Long> = updateLongSet(KEY_EXCLUDED_CONTACT_IDS) {
        it.remove(contactId.toString())
    }

    fun clearAllExcludedContacts(): Set<Long> = clearLongSet(KEY_EXCLUDED_CONTACT_IDS)

    fun getPreferredPhoneNumber(contactId: Long): String? {
        return prefs.getString("$KEY_PREFERRED_PHONE_PREFIX$contactId", null)
    }

    fun setPreferredPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("$KEY_PREFERRED_PHONE_PREFIX$contactId", phoneNumber).apply()
    }

    fun getLastShownPhoneNumber(contactId: Long): String? {
        return prefs.getString("$KEY_LAST_SHOWN_PHONE_PREFIX$contactId", null)
    }

    fun setLastShownPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("$KEY_LAST_SHOWN_PHONE_PREFIX$contactId", phoneNumber).apply()
    }

    fun isDirectDialEnabled(): Boolean = getBooleanPref(KEY_DIRECT_DIAL_ENABLED, false)

    fun setDirectDialEnabled(enabled: Boolean) {
        setBooleanPref(KEY_DIRECT_DIAL_ENABLED, enabled)
    }

    fun hasSeenDirectDialChoice(): Boolean = getBooleanPref(KEY_DIRECT_DIAL_CHOICE_SHOWN, false)

    fun setHasSeenDirectDialChoice(seen: Boolean) {
        setBooleanPref(KEY_DIRECT_DIAL_CHOICE_SHOWN, seen)
    }
}
