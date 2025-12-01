package com.tk.quicksearch.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.util.PhoneNumberUtils
import java.util.Locale

class ContactRepository(
    private val context: Context
) {

    private val contentResolver = context.contentResolver

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getContactsByIds(contactIds: Set<Long>): List<ContactInfo> {
        if (contactIds.isEmpty() || !hasPermission()) return emptyList()

        val idList = contactIds.joinToString(",")

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($idList)"

        val contacts = LinkedHashMap<Long, MutableContact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            null,
            "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC"
        ) ?: return emptyList()

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val lookupIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (c.moveToNext()) {
                val contactId = c.getLong(idIndex)
                val lookupKey = c.getString(lookupIndex) ?: continue
                val displayName = c.getString(nameIndex) ?: continue
                val phoneNumber = c.getString(numberIndex)?.takeIf { it.isNotBlank() } ?: continue

                val existing = contacts[contactId]
                if (existing == null) {
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
        }

        // Fetch photo URIs for contacts
        if (contacts.isNotEmpty()) {
            val idListForPhotos = contacts.keys.joinToString(",")
            val photoProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.PHOTO_URI
            )
            val photoCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                photoProjection,
                "${ContactsContract.Contacts._ID} IN ($idListForPhotos)",
                null,
                null
            )

            photoCursor?.use { c ->
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

        return contacts.values
            .map { contact ->
                ContactInfo(
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    displayName = contact.displayName,
                    phoneNumbers = contact.numbers,
                    photoUri = contact.photoUri
                )
            }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    fun searchContacts(query: String, limit: Int): List<ContactInfo> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val normalizedQuery = query
            .trim()
            .lowercase(Locale.getDefault())
            .replace("%", "")
            .replace("_", "")

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val selection = "LOWER(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY}) LIKE ?"
        val selectionArgs = arrayOf("%$normalizedQuery%")

        val contacts = LinkedHashMap<Long, MutableContact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC"
        ) ?: return emptyList()

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val lookupIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (c.moveToNext()) {
                val contactId = c.getLong(idIndex)
                val lookupKey = c.getString(lookupIndex) ?: continue
                val displayName = c.getString(nameIndex) ?: continue
                val phoneNumber = c.getString(numberIndex)?.takeIf { it.isNotBlank() } ?: continue

                val existing = contacts[contactId]
                if (existing == null) {
                    contacts[contactId] = MutableContact(
                        contactId = contactId,
                        lookupKey = lookupKey,
                        displayName = displayName,
                        numbers = mutableListOf(phoneNumber)
                    )
                    if (contacts.size >= limit) {
                        break
                    }
                } else {
                    addOrUpdatePhoneNumber(existing.numbers, phoneNumber)
                }
            }
        }

        // Fetch photo URIs for contacts
        if (contacts.isNotEmpty()) {
            val contactIds = contacts.keys.joinToString(",")
            val photoProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.PHOTO_URI
            )
            val photoCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                photoProjection,
                "${ContactsContract.Contacts._ID} IN ($contactIds)",
                null,
                null
            )
            
            photoCursor?.use { c ->
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
        
        return contacts.values
            .map { contact ->
                ContactInfo(
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    displayName = contact.displayName,
                    phoneNumbers = contact.numbers,
                    photoUri = contact.photoUri
                )
            }
            .mapNotNull { contact ->
                val priority = com.tk.quicksearch.util.SearchRankingUtils.calculateMatchPriority(
                    contact.displayName,
                    query
                )
                if (com.tk.quicksearch.util.SearchRankingUtils.isOtherMatch(priority)) {
                    null
                } else {
                    contact to priority
                }
            }
            .sortedWith(compareBy(
                { it.second },
                { it.first.displayName.lowercase(Locale.getDefault()) }
            ))
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
                // Replace existing number without country code with new one that has it
                existingNumbers[duplicateIndex] = newNumber
            }
            // If existing has country code and new doesn't, ignore the new number
            // If both have or both don't have country codes, keep the existing one
        } else {
            // No duplicate found, add the new number
            existingNumbers.add(newNumber)
        }
    }

    private data class MutableContact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val numbers: MutableList<String>,
        var photoUri: String? = null
    )
}


