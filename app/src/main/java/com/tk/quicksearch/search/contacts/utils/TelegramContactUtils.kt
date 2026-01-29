package com.tk.quicksearch.search.contacts.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.models.ContactMethodMimeTypes

/**
 * Utility functions for matching Telegram contact methods to phone numbers.
 * Based on the approach from: https://stackoverflow.com/questions/63637693/make-telegram-video-call-with-specific-contact-with-android-programmatically
 *
 * The key insight is to:
 * 1. Get the contact ID from the phone number using PhoneLookup.CONTENT_FILTER_URI
 * 2. Query ContactsContract.Data with that contact ID and Telegram MIME types
 * 3. This gives us the data IDs for Telegram entries associated with that specific phone number
 */
object TelegramContactUtils {
    /**
     * Gets the contact ID for a specific phone number using PhoneLookup.
     * This is important because the same contact can have multiple phone numbers,
     * and each phone number lookup returns the contact ID that's specifically associated with it.
     *
     * @param context The application context
     * @param phoneNumber The phone number to look up
     * @return The contact ID associated with this phone number, or null if not found
     */
    private fun getContactIdByPhoneNumber(
        context: Context,
        phoneNumber: String,
    ): Long? {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val contentResolver = context.contentResolver
        val uri =
            Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber),
            )

        val cursor: Cursor? =
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)
            if (c.moveToFirst()) {
                return c.getLong(idIndex)
            }
        }

        return null
    }

    /**
     * Finds Telegram data IDs that are associated with a specific phone number.
     *
     * This follows the Stack Overflow approach:
     * 1. First get the contact ID from the phone number using PhoneLookup
     * 2. Then query ContactsContract.Data for Telegram entries with that contact ID
     * 3. Return all data IDs found (for message, call, and video call MIME types)
     *
     * @param context The application context
     * @param phoneNumber The phone number to match
     * @return Set of data IDs for Telegram entries that match the phone number
     */
    fun findTelegramDataIdsForPhoneNumber(
        context: Context,
        phoneNumber: String,
    ): Set<Long> {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptySet()
        }

        // First, get the contact ID from the phone number
        val contactId = getContactIdByPhoneNumber(context, phoneNumber) ?: return emptySet()

        val dataIds = mutableSetOf<Long>()
        val contentResolver = context.contentResolver

        // Query ContactsContract.Data for Telegram entries with this contact ID
        // Check all three Telegram MIME types: message, call, and video call
        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND (" +
                "${ContactsContract.Data.MIMETYPE} = ? OR " +
                "${ContactsContract.Data.MIMETYPE} = ? OR " +
                "${ContactsContract.Data.MIMETYPE} = ?)"
        val selectionArgs =
            arrayOf(
                contactId.toString(),
                ContactMethodMimeTypes.TELEGRAM_MESSAGE,
                ContactMethodMimeTypes.TELEGRAM_CALL,
                ContactMethodMimeTypes.TELEGRAM_VIDEO_CALL,
            )

        val cursor: Cursor? =
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                selection,
                selectionArgs,
                null,
            )

        cursor?.use { c ->
            val dataIdIndex = c.getColumnIndexOrThrow(ContactsContract.Data._ID)

            while (c.moveToNext()) {
                val dataId = c.getLong(dataIdIndex)
                dataIds.add(dataId)
            }
        }

        return dataIds
    }

    /**
     * Checks if a Telegram ContactMethod is associated with a specific phone number.
     *
     * @param context The application context
     * @param phoneNumber The phone number to check
     * @param telegramMethod The Telegram method to check
     * @return True if the Telegram method is associated with the phone number
     */
    fun isTelegramMethodForPhoneNumber(
        context: Context,
        phoneNumber: String,
        telegramMethod: com.tk.quicksearch.search.models.ContactMethod,
    ): Boolean {
        // If the method doesn't have a dataId, we can't match it
        val methodDataId = telegramMethod.dataId ?: return false

        // Find all Telegram data IDs for this phone number
        val matchingDataIds = findTelegramDataIdsForPhoneNumber(context, phoneNumber)

        // Check if the method's dataId is in the matching set
        return matchingDataIds.contains(methodDataId)
    }
}
