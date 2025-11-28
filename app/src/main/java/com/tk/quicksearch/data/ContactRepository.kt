package com.tk.quicksearch.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.ContactInfo
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
                } else if (!existing.numbers.contains(phoneNumber)) {
                    existing.numbers.add(phoneNumber)
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
        
        return contacts.values.map { contact ->
            ContactInfo(
                contactId = contact.contactId,
                lookupKey = contact.lookupKey,
                displayName = contact.displayName,
                phoneNumbers = contact.numbers,
                photoUri = contact.photoUri
            )
        }.sortedWith(compareBy(
            { com.tk.quicksearch.util.SearchRankingUtils.calculateMatchPriority(it.displayName, query) },
            { it.displayName.lowercase(Locale.getDefault()) }
        ))
    }

    private data class MutableContact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val numbers: MutableList<String>,
        var photoUri: String? = null
    )
}


