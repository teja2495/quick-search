package com.tk.quicksearch.search.data

import android.content.SharedPreferences
import com.tk.quicksearch.search.data.preferences.ContactPreferences

/**
 * Facade for contact preference operations
 */
class ContactPreferencesFacade(
    private val contactPreferences: ContactPreferences,
    private val sharedPrefs: SharedPreferences
) {
    fun getPinnedContactIds(): Set<Long> = contactPreferences.getPinnedContactIds()

    fun getExcludedContactIds(): Set<Long> = contactPreferences.getExcludedContactIds()

    fun pinContact(contactId: Long): Set<Long> = contactPreferences.pinContact(contactId)

    fun unpinContact(contactId: Long): Set<Long> = contactPreferences.unpinContact(contactId)

    fun excludeContact(contactId: Long): Set<Long> = contactPreferences.excludeContact(contactId)

    fun removeExcludedContact(contactId: Long): Set<Long> =
            contactPreferences.removeExcludedContact(contactId)

    fun clearAllExcludedContacts(): Set<Long> = contactPreferences.clearAllExcludedContacts()

    fun getPreferredPhoneNumber(contactId: Long): String? =
            contactPreferences.getPreferredPhoneNumber(contactId)

    fun setPreferredPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactPreferences.setPreferredPhoneNumber(contactId, phoneNumber)

    fun getLastShownPhoneNumber(contactId: Long): String? =
            contactPreferences.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactPreferences.setLastShownPhoneNumber(contactId, phoneNumber)

    fun isDirectDialEnabled(): Boolean = contactPreferences.isDirectDialEnabled()

    fun setDirectDialEnabled(enabled: Boolean) = contactPreferences.setDirectDialEnabled(enabled)

    fun hasSeenDirectDialChoice(): Boolean = contactPreferences.hasSeenDirectDialChoice()

    fun setHasSeenDirectDialChoice(seen: Boolean) =
            contactPreferences.setHasSeenDirectDialChoice(seen)

    fun isDirectDialManuallyDisabled(): Boolean =
            sharedPrefs.getBoolean(
                    com.tk.quicksearch.search.data.preferences.BasePreferences
                            .KEY_DIRECT_DIAL_MANUALLY_DISABLED,
                    false,
            )

    fun setDirectDialManuallyDisabled(disabled: Boolean) {
        sharedPrefs
                .edit()
                .putBoolean(
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_DIRECT_DIAL_MANUALLY_DISABLED,
                        disabled,
                ).apply()
    }
}