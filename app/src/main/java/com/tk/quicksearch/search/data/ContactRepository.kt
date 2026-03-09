package com.tk.quicksearch.search.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Collections
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
    private val context: Context,
) {
    private val contentResolver = context.contentResolver
    private val otherLabel = context.getString(R.string.contact_method_fallback_label)

    // Cache GoogleMeet availability to avoid repeated PackageManager queries
    private val isGoogleMeetInstalled: Boolean by lazy {
        try {
            context.packageManager.getPackageInfo("com.google.android.apps.tachyon", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Contact method display labels
    private val callLabel = context.getString(R.string.contact_method_call_label)
    private val messageLabel = context.getString(R.string.contact_method_message_label)
    private val emailLabel = context.getString(R.string.contact_method_email_label)
    private val whatsAppVoiceCallLabel = context.getString(R.string.contact_method_whatsapp_voice_call_label)
    private val whatsAppMessageLabel = context.getString(R.string.contact_method_whatsapp_message_label)
    private val whatsAppVideoCallLabel = context.getString(R.string.contact_method_whatsapp_video_call_label)
    private val telegramMessageLabel = context.getString(R.string.contact_method_telegram_message_label)
    private val telegramVoiceCallLabel = context.getString(R.string.contact_method_telegram_voice_call_label)
    private val telegramVideoCallLabel = context.getString(R.string.contact_method_telegram_video_call_label)
    private val signalMessageLabel = context.getString(R.string.contact_method_signal_message_label)
    private val signalVoiceCallLabel = context.getString(R.string.contact_method_signal_voice_call_label)
    private val signalVideoCallLabel = context.getString(R.string.contact_method_signal_video_call_label)
    private val hydratedContactDetailsCache =
        Collections.synchronizedMap(
            object : LinkedHashMap<Long, CachedContactDetails>(MAX_HYDRATED_CONTACT_CACHE_SIZE, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, CachedContactDetails>?): Boolean =
                    size > MAX_HYDRATED_CONTACT_CACHE_SIZE
            },
        )

    companion object {
        // Common projection fields for phone number queries
        private val PHONE_PROJECTION =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )

        // Projection fields for photo URI queries
        private val PHOTO_PROJECTION =
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.PHOTO_URI,
            )

        // Projection fields for contact methods (Data table)
        private val DATA_PROJECTION =
            arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA4,
                ContactsContract.Data.DATA5,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.IS_SUPER_PRIMARY,
            )

        // Sort order for contacts (most contacted first)
        private const val SORT_ORDER = "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC"

        private const val TAG = "ContactRepository"

        // MIME type prefixes
        private const val VND_MIME_PREFIX = "vnd.android.cursor.item/vnd."

        // Package name extraction constants
        private const val PACKAGE_SEPARATOR = "."
        private const val PACKAGE_PARTS_MIN_COUNT = 2
        private const val PACKAGE_FIRST_INDEX = 0
        private const val PACKAGE_SECOND_INDEX = 1
        private const val MAX_HYDRATED_CONTACT_CACHE_SIZE = 600
    }

    // ==================== Public API ====================

    fun hasPermission(): Boolean = PermissionUtils.hasContactsPermission(context)

    /**
     * Performs a lightweight provider read to refresh contact data visibility from ContactsProvider.
     * Returns true when the query succeeds (even if there are no contacts).
     */
    fun refreshContactsProviderSnapshot(): Boolean {
        if (!hasPermission()) return false

        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val refreshed =
            runCatching {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    SORT_ORDER,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getLong(0)
                    }
                }
                true
            }.getOrDefault(false)

        if (refreshed) {
            hydratedContactDetailsCache.clear()
        }

        return refreshed
    }

    /**
     * @param contactIds Set of contact IDs to retrieve
     * @return List of contacts sorted alphabetically by display name
     */
    fun getContactsByIds(contactIds: Set<Long>): List<ContactInfo> {
        if (contactIds.isEmpty() || !hasPermission()) {
            return emptyList()
        }

        val contacts = queryContactsByIds(contactIds)
        hydrateContactDetails(contacts)

        return contacts.values
            .map { it.toContactInfo() }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }

    /**
     * @param query Search query string
     * @param limit Maximum number of unique contacts to return
     * @return List of contacts sorted by match priority, then alphabetically
     */
    fun searchContacts(
        query: String,
        limit: Int,
    ): List<ContactInfo> {
        if (query.isBlank() || !hasPermission()) return emptyList()

        val normalizedProviderQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        val contacts = queryContactsBySearch(normalizedProviderQuery, limit)
        return contacts.values.map { it.toMinimalContactInfo() }
    }

    /**
     * Hydrates photos + contact methods for already-ranked contacts.
     * Designed for keystroke search path: hydrate only the visible/top items.
     */
    fun hydrateContactsForDisplay(contacts: List<ContactInfo>): List<ContactInfo> {
        if (contacts.isEmpty() || !hasPermission()) return contacts

        val mutableContacts =
            LinkedHashMap<Long, MutableContact>(contacts.size).apply {
                contacts.forEach { contact ->
                    put(
                        contact.contactId,
                        MutableContact(
                            contactId = contact.contactId,
                            lookupKey = contact.lookupKey,
                            displayName = contact.displayName,
                            numbers = contact.phoneNumbers.toMutableList(),
                        ),
                    )
                }
            }

        hydrateContactDetails(mutableContacts)
        return contacts.mapNotNull { original ->
            mutableContacts[original.contactId]?.toContactInfo()
        }
    }

    // ==================== Private Helpers ====================

    private fun queryContactsByIds(contactIds: Set<Long>): LinkedHashMap<Long, MutableContact> {
        val selection =
            buildInClause(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                contactIds,
            )

        val cursor =
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                PHONE_PROJECTION,
                selection,
                null,
                SORT_ORDER,
            ) ?: return LinkedHashMap()

        return cursor.use { processPhoneCursor(it) }
    }

    private fun queryContactsBySearch(
        query: String,
        limit: Int,
    ): LinkedHashMap<Long, MutableContact> {
        val filterUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query))
        val cursor =
            contentResolver.query(
                filterUri,
                PHONE_PROJECTION,
                null,
                null,
                SORT_ORDER,
            ) ?: return LinkedHashMap()

        return cursor.use { processPhoneCursor(it, limit) }
    }

    private fun processPhoneCursor(
        cursor: Cursor,
        limit: Int? = null,
    ): LinkedHashMap<Long, MutableContact> {
        val contacts = LinkedHashMap<Long, MutableContact>()

        val columnIndices = PhoneColumnIndices.fromCursor(cursor)

        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(columnIndices.idIndex)
            val lookupKey = cursor.getString(columnIndices.lookupIndex) ?: continue
            val displayName = cursor.getString(columnIndices.nameIndex) ?: continue
            val phoneNumber =
                cursor
                    .getString(columnIndices.numberIndex)
                    ?.takeIf { it.isNotBlank() } ?: continue

            val existing = contacts[contactId]
            if (existing == null) {
                // Check limit before adding new contact
                if (limit != null && contacts.size >= limit) {
                    break
                }
                contacts[contactId] =
                    MutableContact(
                        contactId = contactId,
                        lookupKey = lookupKey,
                        displayName = displayName,
                        numbers = mutableListOf(phoneNumber),
                    )
            } else {
                addOrUpdatePhoneNumber(existing.numbers, phoneNumber)
            }
        }

        return contacts
    }

    private fun fetchPhotoUris(contacts: LinkedHashMap<Long, MutableContact>) {
        if (contacts.isEmpty()) return

        val selection = buildInClause(ContactsContract.Contacts._ID, contacts.keys)
        val cursor =
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                PHOTO_PROJECTION,
                selection,
                null,
                null,
            ) ?: return

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val photoUriIndex = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (c.moveToNext()) {
                val contactId = c.getLong(idIndex)
                val photoUri =
                    if (photoUriIndex >= 0) {
                        c.getString(photoUriIndex)
                    } else {
                        null
                    }

                contacts[contactId]?.photoUri = photoUri?.takeIf { it.isNotBlank() }
            }
        }
    }

    /**
     * Fetches all contact methods for the given contacts from the Data table.
     * This includes phone, email, and app-specific methods like WhatsApp, Telegram, etc.
     */
    private fun fetchContactMethods(contacts: LinkedHashMap<Long, MutableContact>) {
        if (contacts.isEmpty()) return

        val selection = buildInClause(ContactsContract.Data.CONTACT_ID, contacts.keys)

        val cursor =
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                DATA_PROJECTION,
                selection,
                null,
                "${ContactsContract.Data.IS_SUPER_PRIMARY} DESC, ${ContactsContract.Data.IS_PRIMARY} DESC",
            ) ?: return

        cursor.use { c ->
            val dataIdIndex = c.getColumnIndexOrThrow(ContactsContract.Data._ID)
            val contactIdIndex = c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeTypeIndex = c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Index = c.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Index = c.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data3Index = c.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
            val data4Index = c.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
            val data5Index = c.getColumnIndexOrThrow(ContactsContract.Data.DATA5)
            val isPrimaryIndex = c.getColumnIndexOrThrow(ContactsContract.Data.IS_PRIMARY)
            val isSuperPrimaryIndex = c.getColumnIndexOrThrow(ContactsContract.Data.IS_SUPER_PRIMARY)

            while (c.moveToNext()) {
                val dataId = c.getLong(dataIdIndex)
                val contactId = c.getLong(contactIdIndex)
                val contact = contacts[contactId] ?: continue

                val mimeType = c.getString(mimeTypeIndex) ?: continue
                val data1 = c.getString(data1Index) ?: continue
                val data2 = c.getString(data2Index)
                val data3 = c.getString(data3Index)
                val data4 = c.getString(data4Index)
                val data5 = c.getString(data5Index)
                val isPrimary = c.getInt(isPrimaryIndex) == 1 || c.getInt(isSuperPrimaryIndex) == 1

                val method = parseContactMethod(mimeType, data1, data2, data3, data4, data5, dataId, isPrimary)
                if (method != null) {
                    // For Phone methods, create both Phone and SMS methods for each phone number
                    if (method is ContactMethod.Phone) {
                        // Check if we already have a Phone method for this specific phone number
                        val hasPhoneForThisNumber =
                            contact.contactMethods.any {
                                it is ContactMethod.Phone && it.data == data1
                            }

                        if (!hasPhoneForThisNumber) {
                            contact.contactMethods.add(method)
                            // Also add SMS method for the same phone number
                            val smsMethod = ContactMethod.Sms(messageLabel, data1, dataId, isPrimary)
                            contact.contactMethods.add(smsMethod)
                            // Add Google Meet method if Meet is available
                            if (isGoogleMeetInstalled) {
                                val meetMethod =
                                    ContactMethod.GoogleMeet(
                                        displayLabel = context.getString(R.string.settings_calling_option_google_meet),
                                        data = data1,
                                        dataId = dataId,
                                        isPrimary = isPrimary,
                                    )
                                contact.contactMethods.add(meetMethod)
                            }
                        }
                    } else {
                        contact.contactMethods.add(method)
                    }
                }
            }
        }
    }

    private fun hydrateContactDetails(contacts: LinkedHashMap<Long, MutableContact>) {
        if (contacts.isEmpty()) return

        val missingContactIds = mutableListOf<Long>()
        contacts.forEach { (contactId, contact) ->
            val cached = hydratedContactDetailsCache[contactId]
            if (cached != null) {
                contact.photoUri = cached.photoUri
                contact.contactMethods.addAll(cached.contactMethods)
            } else {
                missingContactIds.add(contactId)
            }
        }

        if (missingContactIds.isEmpty()) return

        val missingContacts =
            LinkedHashMap<Long, MutableContact>(missingContactIds.size).apply {
                missingContactIds.forEach { contactId ->
                    contacts[contactId]?.let { put(contactId, it) }
                }
            }
        fetchPhotoUris(missingContacts)
        fetchContactMethods(missingContacts)

        missingContacts.forEach { (contactId, contact) ->
            hydratedContactDetailsCache[contactId] =
                CachedContactDetails(
                    photoUri = contact.photoUri,
                    contactMethods = contact.contactMethods.toList(),
                )
        }
    }

    private fun parseContactMethod(
        mimeType: String,
        data1: String,
        data2: String?,
        data3: String?,
        data4: String?,
        data5: String?,
        dataId: Long,
        isPrimary: Boolean,
    ): ContactMethod? =
        try {
            when (mimeType) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    ContactMethod.Phone(callLabel, data1, dataId, isPrimary)
                }

                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    ContactMethod.Email(emailLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_VOICE_CALL -> {
                    ContactMethod.WhatsAppCall(whatsAppVoiceCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_MESSAGE -> {
                    ContactMethod.WhatsAppMessage(whatsAppMessageLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL -> {
                    ContactMethod.WhatsAppVideoCall(whatsAppVideoCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.TELEGRAM_MESSAGE -> {
                    ContactMethod.TelegramMessage(telegramMessageLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.TELEGRAM_CALL -> {
                    ContactMethod.TelegramCall(telegramVoiceCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL -> {
                    ContactMethod.TelegramVideoCall(telegramVideoCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.SIGNAL_MESSAGE -> {
                    ContactMethod.SignalMessage(signalMessageLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.SIGNAL_CALL -> {
                    ContactMethod.SignalCall(signalVoiceCallLabel, data1, dataId, isPrimary)
                }

                ContactMethodMimeTypes.SIGNAL_VIDEO_CALL -> {
                    ContactMethod.SignalVideoCall(signalVideoCallLabel, data1, dataId, isPrimary)
                }

                else -> {
                    if (mimeType.startsWith("vnd.android.cursor.item/vnd.org.thoughtcrime.securesms")) {
                        when {
                            mimeType.contains("video", ignoreCase = true) ->
                                ContactMethod.SignalVideoCall(signalVideoCallLabel, data1, dataId, isPrimary)
                            mimeType.contains("call", ignoreCase = true) ->
                                ContactMethod.SignalCall(signalVoiceCallLabel, data1, dataId, isPrimary)
                            else ->
                                ContactMethod.SignalMessage(signalMessageLabel, data1, dataId, isPrimary)
                        }
                    } else
                    if (mimeType.startsWith(VND_MIME_PREFIX)) {
                        val packageName = extractPackageFromMimeType(mimeType)
                        ContactMethod.CustomApp(
                            displayLabel = packageName ?: otherLabel,
                            data = data1,
                            mimeType = mimeType,
                            packageName = packageName,
                            dataId = dataId,
                            isPrimary = isPrimary,
                        )
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing contact method: $mimeType", e)
            null
        }

    /**
     * Attempts to extract package name from custom MIME type.
     * Format: vnd.android.cursor.item/vnd.com.package.name.xxx
     */
    private fun extractPackageFromMimeType(mimeType: String): String? {
        if (!mimeType.startsWith(VND_MIME_PREFIX)) return null

        val rest = mimeType.substring(VND_MIME_PREFIX.length)
        // Extract package-like part (e.g., "com.whatsapp.profile" -> "com.whatsapp")
        val parts = rest.split(PACKAGE_SEPARATOR)
        return if (parts.size >= PACKAGE_PARTS_MIN_COUNT) {
            "${parts[PACKAGE_FIRST_INDEX]}$PACKAGE_SEPARATOR${parts[PACKAGE_SECOND_INDEX]}"
        } else {
            null
        }
    }

    private fun buildInClause(
        columnName: String,
        ids: Collection<Long>,
    ): String {
        val idList = ids.joinToString(",")
        return "$columnName IN ($idList)"
    }

    /**
     * Adds or updates a phone number in the list, prioritizing numbers with country codes.
     * If a duplicate is found (same number, one with country code, one without),
     * the number with country code is kept and the one without is removed.
     */
    private fun addOrUpdatePhoneNumber(
        existingNumbers: MutableList<String>,
        newNumber: String,
    ) {
        // Check for exact match first
        if (existingNumbers.contains(newNumber)) {
            return
        }

        // Check for duplicate (same number but different format)
        val duplicateIndex =
            existingNumbers.indexOfFirst {
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

    private data class MutableContact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val numbers: MutableList<String>,
        var photoUri: String? = null,
        val contactMethods: MutableList<ContactMethod> = mutableListOf(),
    ) {
        fun toContactInfo(): ContactInfo {
            // Reorder contact methods so email comes last
            val reorderedMethods =
                contactMethods.sortedWith(
                    compareBy<ContactMethod> {
                        when (it) {
                            is ContactMethod.Email -> 1

                            // Email comes last
                            else -> 0 // Everything else comes first
                        }
                    }.thenBy {
                        // Within the same priority group, maintain original order
                        contactMethods.indexOf(it)
                    },
                )

            return ContactInfo(
                contactId = contactId,
                lookupKey = lookupKey,
                displayName = displayName,
                phoneNumbers = numbers,
                photoUri = photoUri,
                contactMethods = reorderedMethods,
            )
        }

        fun toMinimalContactInfo(): ContactInfo =
            ContactInfo(
                contactId = contactId,
                lookupKey = lookupKey,
                displayName = displayName,
                phoneNumbers = numbers.toList(),
            )
    }

    private data class CachedContactDetails(
        val photoUri: String?,
        val contactMethods: List<ContactMethod>,
    )

    private data class PhoneColumnIndices(
        val idIndex: Int,
        val lookupIndex: Int,
        val nameIndex: Int,
        val numberIndex: Int,
    ) {
        companion object {
            fun fromCursor(cursor: Cursor): PhoneColumnIndices =
                PhoneColumnIndices(
                    idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                    lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY),
                    nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY),
                    numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER),
                )
        }
    }
}
