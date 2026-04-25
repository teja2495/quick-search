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
    private val packageNameByMimeCache = Collections.synchronizedMap(mutableMapOf<String, String?>())
    private val customAppLabelCache = Collections.synchronizedMap(mutableMapOf<String, String>())
    private val discoveredMollyPackageName: String? by lazy {
        runCatching {
            context.packageManager
                .getInstalledApplications(0)
                .firstOrNull { appInfo ->
                    appInfo.packageName.contains("molly", ignoreCase = true)
                }?.packageName
        }.getOrNull()
    }
    private val isSignalPackageBrandedAsMolly: Boolean by lazy {
        runCatching {
            val appInfo = context.packageManager.getApplicationInfo(SIGNAL_PACKAGE, 0)
            val label = context.packageManager.getApplicationLabel(appInfo)?.toString().orEmpty()
            label.contains("molly", ignoreCase = true)
        }.getOrDefault(false)
    }
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
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
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
                ContactsContract.Data.RES_PACKAGE,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.IS_SUPER_PRIMARY,
            )

        // Sort order for contacts (most contacted first)
        private const val SORT_ORDER = "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC"

        private const val TAG = "ContactRepository"

        // MIME type prefixes
        private const val VND_MIME_PREFIX = "vnd.android.cursor.item/vnd."
        private const val SIGNAL_PACKAGE = "org.thoughtcrime.securesms"
        private val PACKAGE_NAME_PATTERN = Regex("[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+){1,}")
        private val MOLLY_PACKAGE_CANDIDATES =
            listOf(
                "im.molly.app",
                "im.molly.im",
            )

        // Package name extraction constants
        private const val PACKAGE_SEPARATOR = "."
        private const val PACKAGE_PARTS_MIN_COUNT = 2
        private const val MAX_HYDRATED_CONTACT_CACHE_SIZE = 600
        private val WHITESPACE_REGEX = "\\s+".toRegex()
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
        if (query.isBlank() || limit <= 0 || !hasPermission()) return emptyList()

        val normalizedProviderQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        val contacts = queryContactsBySearch(normalizedProviderQuery, limit)
        if (contacts.size < limit) {
            val remaining = limit - contacts.size
            val fallbackContacts =
                queryContactsByNameTokenFallback(
                    query = normalizedProviderQuery,
                    limit = remaining,
                    existingContactIds = contacts.keys,
                )
            contacts.putAll(fallbackContacts)
        }
        return contacts.values.take(limit).map { it.toMinimalContactInfo() }
    }

    /**
     * Returns recent contacts without provider-side query filtering.
     * Used by alias-triggered fuzzy search to broaden candidate coverage.
     */
    fun getRecentContacts(limit: Int): List<ContactInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()

        val cursor =
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                PHONE_PROJECTION,
                null,
                null,
                SORT_ORDER,
            ) ?: return emptyList()

        return cursor.use { processPhoneCursor(it, limit) }.values.map { it.toMinimalContactInfo() }
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
                            phoneNumberLabels = contact.phoneNumberLabels.toMutableMap(),
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

    /**
     * Fallback query for devices/providers where CONTENT_FILTER_URI may miss middle/last name
     * matches. Requires every query token to be present somewhere in the contact display name.
     */
    private fun queryContactsByNameTokenFallback(
        query: String,
        limit: Int,
        existingContactIds: Collection<Long>,
    ): LinkedHashMap<Long, MutableContact> {
        if (limit <= 0 || query.isBlank()) return LinkedHashMap()

        val queryTokens = query.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (queryTokens.isEmpty()) return LinkedHashMap()

        val tokenClauses =
            queryTokens.joinToString(" AND ") {
                "(LOWER(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY}) LIKE ? " +
                    "OR LOWER(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_ALTERNATIVE}) LIKE ?)"
            }
        val exclusionsClause =
            if (existingContactIds.isNotEmpty()) {
                " AND ${buildNotInClause(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, existingContactIds)}"
            } else {
                ""
            }
        val selection = tokenClauses + exclusionsClause
        val selectionArgs =
            queryTokens
                .flatMap { token ->
                    val likeToken = "%${token.lowercase(Locale.getDefault())}%"
                    listOf(likeToken, likeToken)
                }.toTypedArray()

        val cursor =
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                PHONE_PROJECTION,
                selection,
                selectionArgs,
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
            val phoneNumberLabel = cursor.getPhoneNumberLabel(columnIndices)

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
                        phoneNumberLabels = mutableMapOf(phoneNumber to phoneNumberLabel),
                    )
            } else {
                addOrUpdatePhoneNumber(existing.numbers, existing.phoneNumberLabels, phoneNumber, phoneNumberLabel)
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
            val resPackageIndex = c.getColumnIndex(ContactsContract.Data.RES_PACKAGE)
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
                val resPackage = if (resPackageIndex >= 0) c.getString(resPackageIndex) else null
                val isPrimary = c.getInt(isPrimaryIndex) == 1 || c.getInt(isSuperPrimaryIndex) == 1

                val method =
                    parseContactMethod(
                        mimeType = mimeType,
                        data1 = data1,
                        data2 = data2,
                        data3 = data3,
                        data4 = data4,
                        data5 = data5,
                        resPackage = resPackage,
                        dataId = dataId,
                        isPrimary = isPrimary,
                    )
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
                                        displayLabel = context.getString(R.string.contact_method_google_meet_label),
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
        resPackage: String?,
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
                    val packageName =
                        resolveSignalLikePackage(
                            mimeType = mimeType,
                            data3 = data3,
                            data4 = data4,
                            data5 = data5,
                            resPackage = resPackage,
                            includeMimeTypeHints = false,
                        )
                    if (packageName == null || (packageName == SIGNAL_PACKAGE && !isSignalPackageBrandedAsMolly)) {
                        ContactMethod.SignalMessage(signalMessageLabel, data1, dataId, isPrimary)
                    } else {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            packageNameOverride = packageName,
                            displayLabelOverride = data3,
                        )
                    }
                }

                ContactMethodMimeTypes.SIGNAL_CALL -> {
                    val packageName =
                        resolveSignalLikePackage(
                            mimeType = mimeType,
                            data3 = data3,
                            data4 = data4,
                            data5 = data5,
                            resPackage = resPackage,
                            includeMimeTypeHints = false,
                        )
                    if (packageName == null || (packageName == SIGNAL_PACKAGE && !isSignalPackageBrandedAsMolly)) {
                        ContactMethod.SignalCall(signalVoiceCallLabel, data1, dataId, isPrimary)
                    } else {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            packageNameOverride = packageName,
                            displayLabelOverride = data3,
                        )
                    }
                }

                ContactMethodMimeTypes.SIGNAL_VIDEO_CALL -> {
                    val packageName =
                        resolveSignalLikePackage(
                            mimeType = mimeType,
                            data3 = data3,
                            data4 = data4,
                            data5 = data5,
                            resPackage = resPackage,
                            includeMimeTypeHints = false,
                        )
                    if (packageName == null || (packageName == SIGNAL_PACKAGE && !isSignalPackageBrandedAsMolly)) {
                        ContactMethod.SignalVideoCall(signalVideoCallLabel, data1, dataId, isPrimary)
                    } else {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            packageNameOverride = packageName,
                            displayLabelOverride = data3,
                        )
                    }
                }

                else -> {
                    if (mimeType.startsWith("vnd.android.cursor.item/vnd.org.thoughtcrime.securesms")) {
                        val packageName =
                            resolveSignalLikePackage(
                                mimeType = mimeType,
                                data3 = data3,
                                data4 = data4,
                                data5 = data5,
                                resPackage = resPackage,
                            )

                        if (packageName != null && (packageName != SIGNAL_PACKAGE || isSignalPackageBrandedAsMolly)) {
                            parseCustomAppMethod(
                                mimeType = mimeType,
                                data = data1,
                                dataId = dataId,
                                isPrimary = isPrimary,
                                packageNameOverride = packageName,
                                displayLabelOverride = data3,
                            )
                        } else {
                            when {
                                mimeType.contains("video", ignoreCase = true) ->
                                    ContactMethod.SignalVideoCall(signalVideoCallLabel, data1, dataId, isPrimary)
                                mimeType.contains("call", ignoreCase = true) ->
                                    ContactMethod.SignalCall(signalVoiceCallLabel, data1, dataId, isPrimary)
                                else ->
                                    ContactMethod.SignalMessage(signalMessageLabel, data1, dataId, isPrimary)
                            }
                        }
                    } else if (mimeType.startsWith(VND_MIME_PREFIX)) {
                        parseCustomAppMethod(
                            mimeType = mimeType,
                            data = data1,
                            dataId = dataId,
                            isPrimary = isPrimary,
                            displayLabelOverride = data3,
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

        val cached = packageNameByMimeCache[mimeType]
        if (cached != null || packageNameByMimeCache.containsKey(mimeType)) {
            return cached
        }

        val resolved =
            run {
                val rest = mimeType.substring(VND_MIME_PREFIX.length)
                val parts = rest.split(PACKAGE_SEPARATOR)
                if (parts.size < PACKAGE_PARTS_MIN_COUNT) {
                    return@run null
                }

                // Prefer the longest prefix that maps to an installed package.
                for (partCount in parts.size - 1 downTo PACKAGE_PARTS_MIN_COUNT) {
                    val candidate = parts.take(partCount).joinToString(PACKAGE_SEPARATOR)
                    if (isPackageInstalled(candidate)) {
                        return@run candidate
                    }
                }

                // Fallback to first two segments when installation cannot be resolved.
                parts.take(PACKAGE_PARTS_MIN_COUNT).joinToString(PACKAGE_SEPARATOR)
            }

        packageNameByMimeCache[mimeType] = resolved
        return resolved
    }

    private fun resolveCustomAppDisplayLabel(packageName: String?): String {
        if (packageName.isNullOrBlank()) return otherLabel

        val cached = customAppLabelCache[packageName]
        if (cached != null) return cached

        val resolved =
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo)?.toString()?.takeIf { it.isNotBlank() }
            }.getOrNull() ?: packageName

        customAppLabelCache[packageName] = resolved
        return resolved
    }

    private fun parseCustomAppMethod(
        mimeType: String,
        data: String,
        dataId: Long,
        isPrimary: Boolean,
        packageNameOverride: String? = null,
        displayLabelOverride: String? = null,
    ): ContactMethod.CustomApp {
        val packageName = packageNameOverride ?: extractPackageFromMimeType(mimeType)
        val displayLabel =
            displayLabelOverride?.takeIf { it.isNotBlank() } ?: resolveCustomAppDisplayLabel(packageName)
        return ContactMethod.CustomApp(
            displayLabel = displayLabel,
            data = data,
            mimeType = mimeType,
            packageName = packageName,
            dataId = dataId,
            isPrimary = isPrimary,
        )
    }

    private fun resolveSignalLikePackage(
        mimeType: String,
        data3: String?,
        data4: String?,
        data5: String?,
        resPackage: String?,
        includeMimeTypeHints: Boolean = true,
    ): String? {
        val fromResPackage = resPackage?.takeIf { it.isNotBlank() }
        if (fromResPackage != null) return fromResPackage

        val fields =
            if (includeMimeTypeHints) {
                listOf(data5, data4, data3, mimeType)
            } else {
                listOf(data5, data4, data3)
            }
        val packageCandidates =
            fields
                .flatMap { field -> extractPackageCandidatesFromField(field) }
                .map { candidate -> normalizeSignalLikePackageCandidate(candidate) }
                .filterNotNull()
                .distinct()

        val installedCandidate = packageCandidates.firstOrNull(::isPackageInstalled)
        if (installedCandidate != null) return installedCandidate
        if (packageCandidates.isNotEmpty()) return packageCandidates.first()

        val hasMollyHint = fields.any { it?.contains("molly", ignoreCase = true) == true }
        if (hasMollyHint) {
            return resolveInstalledMollyPackageName() ?: MOLLY_PACKAGE_CANDIDATES.first()
        }

        // Some Molly contact methods reuse Signal MIME types without explicit package metadata.
        // If Signal is unavailable but Molly is installed, classify it as Molly custom app.
        if (!isPackageInstalled(SIGNAL_PACKAGE)) {
            resolveInstalledMollyPackageName()?.let { return it }
        }

        // Some Molly builds reuse the Signal package name but expose Molly branding.
        if (isSignalPackageBrandedAsMolly) {
            return SIGNAL_PACKAGE
        }

        return null
    }

    private fun normalizeSignalLikePackageCandidate(candidate: String): String? {
        val normalized = candidate.trim()
        if (normalized.isBlank()) return null

        if (MOLLY_PACKAGE_CANDIDATES.any { normalized.contains(it) }) {
            return MOLLY_PACKAGE_CANDIDATES.first { normalized.contains(it) }
        }
        if (normalized.contains(SIGNAL_PACKAGE)) {
            return SIGNAL_PACKAGE
        }

        // Ignore MIME namespace-like tokens (for example "vnd.android.cursor.item").
        if (normalized.startsWith("vnd.")) {
            return null
        }

        return normalized
    }

    private fun resolveInstalledMollyPackageName(): String? =
        MOLLY_PACKAGE_CANDIDATES.firstOrNull(::isPackageInstalled) ?: discoveredMollyPackageName

    private fun extractPackageCandidatesFromField(field: String?): List<String> {
        if (field.isNullOrBlank()) return emptyList()
        return PACKAGE_NAME_PATTERN.findAll(field).map { it.value }.toList()
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)

    private fun buildInClause(
        columnName: String,
        ids: Collection<Long>,
    ): String {
        val idList = ids.joinToString(",")
        return "$columnName IN ($idList)"
    }

    private fun buildNotInClause(
        columnName: String,
        ids: Collection<Long>,
    ): String {
        val idList = ids.joinToString(",")
        return "$columnName NOT IN ($idList)"
    }

    /**
     * Adds or updates a phone number in the list, prioritizing numbers with country codes.
     * If a duplicate is found (same number, one with country code, one without),
     * the number with country code is kept and the one without is removed.
     */
    private fun addOrUpdatePhoneNumber(
        existingNumbers: MutableList<String>,
        phoneNumberLabels: MutableMap<String, String>,
        newNumber: String,
        newLabel: String,
    ) {
        // Check for exact match first
        if (existingNumbers.contains(newNumber)) {
            phoneNumberLabels.putIfAbsent(newNumber, newLabel)
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
                phoneNumberLabels.remove(existingNumber)
                phoneNumberLabels[newNumber] = newLabel
            } else {
                phoneNumberLabels.putIfAbsent(existingNumber, newLabel)
            }
            // If existing has country code and new doesn't, ignore the new number
            // If both have or both don't have country codes, keep the existing one
        } else {
            // No duplicate found, add the new number
            existingNumbers.add(newNumber)
            phoneNumberLabels[newNumber] = newLabel
        }
    }

    private fun Cursor.getPhoneNumberLabel(columnIndices: PhoneColumnIndices): String {
        val type = getInt(columnIndices.typeIndex)
        val customLabel = getString(columnIndices.labelIndex)
        return ContactsContract.CommonDataKinds.Phone
            .getTypeLabel(context.resources, type, customLabel)
            .toString()
    }

    private data class MutableContact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val numbers: MutableList<String>,
        val phoneNumberLabels: MutableMap<String, String> = mutableMapOf(),
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
                phoneNumberLabels = phoneNumberLabels.toMap(),
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
                phoneNumberLabels = phoneNumberLabels.toMap(),
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
        val typeIndex: Int,
        val labelIndex: Int,
    ) {
        companion object {
            fun fromCursor(cursor: Cursor): PhoneColumnIndices =
                PhoneColumnIndices(
                    idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                    lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY),
                    nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY),
                    numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE),
                    labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL),
                )
        }
    }
}
