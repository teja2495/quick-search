package com.tk.quicksearch.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.util.PhoneNumberUtils
import com.tk.quicksearch.util.SearchRankingUtils
import java.util.Locale

/**
 * Repository for querying and managing contact data from the device's contacts provider.
 * 
 * Responsibilities:
 * - Querying contacts by IDs or search query
 * - Aggregating multiple phone numbers per contact
 * - Fetching contact photos
 * - Managing contact permissions
 */
class ContactRepository(
    private val context: Context
) {

    private val contentResolver = context.contentResolver

    companion object {
        // Common projection fields for phone number queries
        private val PHONE_PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // Projection fields for photo URI queries
        private val PHOTO_PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.PHOTO_URI
        )

        // Sort order for contacts (most contacted first)
        private const val SORT_ORDER = "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC"
    }

    // ==================== Public API ====================

    /**
     * Checks if the app has been granted READ_CONTACTS permission.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retrieves contacts by their IDs.
     * 
     * @param contactIds Set of contact IDs to retrieve
     * @return List of contacts sorted alphabetically by display name
     */
    fun getContactsByIds(contactIds: Set<Long>): List<ContactInfo> {
        if (contactIds.isEmpty() || !hasPermission()) return emptyList()

        val contacts = queryContactsByIds(contactIds)
        fetchPhotoUris(contacts)
        
        return contacts.values
            .map { it.toContactInfo() }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    /**
     * Searches for contacts matching the given query.
     * 
     * @param query Search query string
     * @param limit Maximum number of unique contacts to return
     * @return List of contacts sorted by match priority, then alphabetically
     */
    fun searchContacts(query: String, limit: Int): List<ContactInfo> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val normalizedQuery = normalizeSearchQuery(query)
        val contacts = queryContactsBySearch(normalizedQuery, limit)
        fetchPhotoUris(contacts)
        
        return contacts.values
            .map { it.toContactInfo() }
            .filterAndRank(query)
    }

    // ==================== Private Helpers ====================

    /**
     * Queries contacts by their IDs from the contacts provider.
     */
    private fun queryContactsByIds(contactIds: Set<Long>): LinkedHashMap<Long, MutableContact> {
        val selection = buildInClause(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            contactIds
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PHONE_PROJECTION,
            selection,
            null,
            SORT_ORDER
        ) ?: return LinkedHashMap()

        return cursor.use { processPhoneCursor(it) }
    }

    /**
     * Queries contacts matching the search query.
     * Stops after reaching the specified limit of unique contacts.
     */
    private fun queryContactsBySearch(
        normalizedQuery: String,
        limit: Int
    ): LinkedHashMap<Long, MutableContact> {
        val selection = "LOWER(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY}) LIKE ?"
        val selectionArgs = arrayOf("%$normalizedQuery%")

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PHONE_PROJECTION,
            selection,
            selectionArgs,
            SORT_ORDER
        ) ?: return LinkedHashMap()

        return cursor.use { processPhoneCursor(it, limit) }
    }

    /**
     * Processes a phone number cursor and builds a map of contacts.
     * 
     * @param cursor Cursor containing phone number data
     * @param limit Optional limit on number of unique contacts (null = no limit)
     */
    private fun processPhoneCursor(
        cursor: Cursor,
        limit: Int? = null
    ): LinkedHashMap<Long, MutableContact> {
        val contacts = LinkedHashMap<Long, MutableContact>()
        
        val columnIndices = PhoneColumnIndices.fromCursor(cursor)

        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(columnIndices.idIndex)
            val lookupKey = cursor.getString(columnIndices.lookupIndex) ?: continue
            val displayName = cursor.getString(columnIndices.nameIndex) ?: continue
            val phoneNumber = cursor.getString(columnIndices.numberIndex)
                ?.takeIf { it.isNotBlank() } ?: continue

            val existing = contacts[contactId]
            if (existing == null) {
                // Check limit before adding new contact
                if (limit != null && contacts.size >= limit) {
                    break
                }
                contacts[contactId] = MutableContact(
                    contactId = contactId,
                    lookupKey = lookupKey,
                    displayName = displayName,
                    numbers = mutableListOf(phoneNumber)
                )
            } else {
                addOrUpdatePhoneNumber(existing.numbers, phoneNumber)
            }
        }

        return contacts
    }

    /**
     * Fetches photo URIs for the given contacts and updates them in place.
     */
    private fun fetchPhotoUris(contacts: LinkedHashMap<Long, MutableContact>) {
        if (contacts.isEmpty()) return

        val selection = buildInClause(ContactsContract.Contacts._ID, contacts.keys)
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            PHOTO_PROJECTION,
            selection,
            null,
            null
        ) ?: return

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val photoUriIndex = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (c.moveToNext()) {
                val contactId = c.getLong(idIndex)
                val photoUri = if (photoUriIndex >= 0) {
                    c.getString(photoUriIndex)
                } else null

                contacts[contactId]?.photoUri = photoUri?.takeIf { it.isNotBlank() }
            }
        }
    }

    /**
     * Normalizes a search query by trimming, lowercasing, and removing SQL wildcards.
     */
    private fun normalizeSearchQuery(query: String): String {
        return query
            .trim()
            .lowercase(Locale.getDefault())
            .replace("%", "")
            .replace("_", "")
    }

    /**
     * Builds a safe IN clause for SQL queries.
     * Since contact IDs are Long values, we can safely join them.
     */
    private fun buildInClause(columnName: String, ids: Collection<Long>): String {
        val idList = ids.joinToString(",")
        return "$columnName IN ($idList)"
    }

    /**
     * Filters contacts by match priority and sorts them accordingly.
     */
    private fun List<ContactInfo>.filterAndRank(query: String): List<ContactInfo> {
        return mapNotNull { contact ->
            val priority = SearchRankingUtils.calculateMatchPriority(
                contact.displayName,
                query
            )
            if (SearchRankingUtils.isOtherMatch(priority)) {
                null
            } else {
                contact to priority
            }
        }
        .sortedWith(
            compareBy(
                { it.second },
                { it.first.displayName.lowercase(Locale.getDefault()) }
            )
        )
        .map { it.first }
    }

    /**
     * Adds or updates a phone number in the list, prioritizing numbers with country codes.
     * If a duplicate is found (same number, one with country code, one without),
     * the number with country code is kept and the one without is removed.
     */
    private fun addOrUpdatePhoneNumber(existingNumbers: MutableList<String>, newNumber: String) {
        // Check for exact match first
        if (existingNumbers.contains(newNumber)) {
            return
        }

        // Check for duplicate (same number but different format)
        val duplicateIndex = existingNumbers.indexOfFirst {
            PhoneNumberUtils.isSameNumber(it, newNumber)
        }

        if (duplicateIndex >= 0) {
            val existingNumber = existingNumbers[duplicateIndex]
            val newHasCountryCode = PhoneNumberUtils.hasCountryCode(newNumber)
            val existingHasCountryCode = PhoneNumberUtils.hasCountryCode(existingNumber)

            // Prioritize number with country code
            if (newHasCountryCode && !existingHasCountryCode) {
                existingNumbers[duplicateIndex] = newNumber
            }
            // If existing has country code and new doesn't, ignore the new number
            // If both have or both don't have country codes, keep the existing one
        } else {
            // No duplicate found, add the new number
            existingNumbers.add(newNumber)
        }
    }

    /**
     * Data class for building contacts before converting to ContactInfo.
     */
    private data class MutableContact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val numbers: MutableList<String>,
        var photoUri: String? = null
    ) {
        fun toContactInfo(): ContactInfo {
            return ContactInfo(
                contactId = contactId,
                lookupKey = lookupKey,
                displayName = displayName,
                phoneNumbers = numbers,
                photoUri = photoUri
            )
        }
    }

    /**
     * Holds column indices for phone number cursor to avoid repeated lookups.
     */
    private data class PhoneColumnIndices(
        val idIndex: Int,
        val lookupIndex: Int,
        val nameIndex: Int,
        val numberIndex: Int
    ) {
        companion object {
            fun fromCursor(cursor: Cursor): PhoneColumnIndices {
                return PhoneColumnIndices(
                    idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                    lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY),
                    nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY),
                    numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
            }
        }
    }
}


