package com.tk.quicksearch.search

import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.ContactMethodMimeTypes
import com.tk.quicksearch.util.PhoneNumberUtils

/**
 * Helper functions for contact-related intents (calls, messages, emails, etc.).
 */
object ContactIntentHelpers {

    /**
     * Opens a contact's details.
     */
    fun openContact(context: Application, contactInfo: ContactInfo) {
        val lookupUri = ContactsContract.Contacts.getLookupUri(contactInfo.contactId, contactInfo.lookupKey)
        if (lookupUri == null) {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_contact),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, lookupUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Opens the dialer without requiring call permission.
     */
    fun performDial(context: Application, number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Initiates a direct phone call. Requires CALL_PHONE permission.
     */
    fun performDirectCall(context: Application, number: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Initiates a phone call.
     * Uses ACTION_CALL if CALL_PHONE permission is granted, otherwise falls back to ACTION_DIAL.
     */
    fun performCall(context: Application, number: String) {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            performDirectCall(context, number)
        } else {
            performDial(context, number)
        }
    }

    /**
     * Opens SMS app for a phone number.
     */
    fun performSms(context: Application, number: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Opens the default email app with a prefilled recipient.
     */
    fun composeEmail(context: Application, email: String) {
        if (email.isBlank()) return

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(email)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_email),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Opens WhatsApp chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppChat(context: Application, dataId: Long?) {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for WhatsApp chat, using fallback")
            // Fallback to old method - this shouldn't happen in normal flow
            return
        }

        try {
            // WhatsApp message using contact data URI
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactMethodMimeTypes.WHATSAPP_MESSAGE
                )
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("MessagingService", "WhatsApp chat intent cannot be resolved")
                Toast.makeText(
                    context,
                    "WhatsApp is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open WhatsApp chat", e)
            Toast.makeText(
                context,
                "Failed to open WhatsApp chat",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Opens Telegram chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramChat(context: Application, dataId: Long?) {
        if (dataId == null) {
            Log.w("IntentHelpers", "No dataId provided for Telegram chat, using fallback")
            // Fallback to old method - this shouldn't happen in normal flow
            return
        }

        try {
            // Telegram message using contact data URI
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactMethodMimeTypes.TELEGRAM_MESSAGE
                )
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("IntentHelpers", "Telegram chat intent cannot be resolved")
                Toast.makeText(
                    context,
                    "Telegram is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("IntentHelpers", "Failed to open Telegram chat", e)
            Toast.makeText(
                context,
                "Failed to open Telegram chat",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Opens WhatsApp chat for a phone number (legacy method for backward compatibility).
     */
    fun openWhatsAppChat(context: Application, phoneNumber: String) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("MessagingService", "Invalid phone number for WhatsApp: $phoneNumber")
            return
        }

        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("MessagingService", "Could not clean phone number for WhatsApp: $phoneNumber")
            return
        }

        try {
            // Method 1: Use ACTION_SENDTO with smsto scheme for direct chat
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            Log.w("MessagingService", "WhatsApp method 1 failed", e)
            try {
                // Method 2: Use ACTION_SEND with WhatsApp package
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra("jid", "$cleanNumber@s.whatsapp.net")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
            } catch (e2: Exception) {
                Log.w("MessagingService", "WhatsApp method 2 failed", e2)
                try {
                    // Method 3: Try standard messaging intent
                    val messageIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("sms:$cleanNumber")
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(messageIntent)
                } catch (e3: Exception) {
                    Log.w("MessagingService", "WhatsApp method 3 failed", e3)
                    // Final fallback to SMS
                    performSms(context, phoneNumber)
                }
            }
        }
    }

    /**
     * Opens Telegram chat for a phone number with comprehensive fallback methods.
     * Uses multiple approaches to ensure maximum compatibility.
     */
    fun openTelegramChat(context: Application, phoneNumber: String) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("IntentHelpers", "Invalid phone number for Telegram: $phoneNumber")
            Toast.makeText(
                context,
                "Invalid phone number for Telegram",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("IntentHelpers", "Could not clean phone number for Telegram: $phoneNumber")
            Toast.makeText(
                context,
                "Could not process phone number for Telegram",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Method 1: Use tg://resolve?phone= (preferred for Telegram app)
        try {
            val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(cleanNumber)}")
            val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if Telegram is installed
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return
            } else {
                Log.w("IntentHelpers", "Telegram app not installed, trying web fallback")
            }
        } catch (e: Exception) {
            Log.w("IntentHelpers", "Telegram tg:// method failed", e)
        }

        // Method 2: Fallback to web URL https://t.me/+
        try {
            val webUri = Uri.parse("https://t.me/${Uri.encode(cleanNumber)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.w("IntentHelpers", "Telegram web fallback failed", e)
            Toast.makeText(
                context,
                "Telegram is not installed and web fallback failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Initiates a WhatsApp call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppCall(context: Application, dataId: Long?): Boolean {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for WhatsApp call")
            return false
        }

        try {
            // Voice call using contact data URI
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactMethodMimeTypes.WHATSAPP_VOICE_CALL
                )
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("MessagingService", "WhatsApp call intent cannot be resolved")
                Toast.makeText(
                    context,
                    "WhatsApp is not installed or doesn't support voice calls",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to initiate WhatsApp call", e)
            Toast.makeText(
                context,
                "WhatsApp call failed. Please ensure WhatsApp is installed.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    /**
     * Initiates a Telegram call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramCall(context: Application, dataId: Long?): Boolean {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for Telegram call")
            return false
        }

        try {
            // Voice call using contact data URI
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    ContactMethodMimeTypes.TELEGRAM_CALL
                )
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("MessagingService", "Telegram call intent cannot be resolved")
                Toast.makeText(
                    context,
                    "Telegram is not installed or doesn't support voice calls",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to initiate Telegram call", e)
            Toast.makeText(
                context,
                "Telegram call failed. Please ensure Telegram is installed.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    /**
     * Initiates a Telegram video call.
     * Uses the contact data URI for the specific video call row from Contacts provider.
     */
    fun openTelegramVideoCall(context: Application, dataId: Long?, phoneNumber: String?): Boolean {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for Telegram video call")
            return false
        }

        try {
            // Create the contact data URI for the video call row
            val contactDataUri = Uri.parse("content://com.android.contacts/data/$dataId")

            // Use ACTION_VIEW with the URI directly - the MIME type is implicit from the Contacts provider row
            val intent = Intent(Intent.ACTION_VIEW, contactDataUri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("MessagingService", "No activity found to handle Telegram video call intent")
                return false
            }
        } catch (e: Exception) {
            Log.w("MessagingService", "Telegram video call intent failed", e)
            return false
        }
    }

    /**
     * Initiates a Telegram call for a phone number (legacy method for backward compatibility).
     */
    fun openTelegramCall(context: Application, phoneNumber: String) {
        if (phoneNumber.isBlank()) return

        val normalizedNumber = phoneNumber.trim()
            .replace(" ", "")
            .replace("-", "")
            .let { if (it.startsWith("+")) it else "+$it" }

        // Try tg:// scheme for call
        val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(normalizedNumber)}&call=1")
        val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
            setPackage("org.telegram.messenger")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Telegram is not installed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Opens a WhatsApp video call using the specific dataId and MIME type pattern.
     */
    fun openWhatsAppVideoCall(context: Application, dataId: Long): Boolean {
        return try {
            val mime = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    mime
                )
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("MessagingService", "No activity found to handle WhatsApp video call")
                Toast.makeText(
                    context,
                    "WhatsApp not available for video calls",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open WhatsApp video call", e)
            Toast.makeText(
                context,
                "Failed to open WhatsApp video call",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    /**
     * Opens a video call app with the specified package and data.
     */
    fun openVideoCall(context: Application, data: String, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setData(Uri.parse("tel:$data"))
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open video call", e)
            Toast.makeText(
                context,
                "Failed to open video call",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Opens a Google Meet video call using the working method identified through testing.
     * Uses the custom action "com.google.android.apps.tachyon.action.CALL" which was confirmed to work.
     */
    fun openGoogleMeet(context: Application, dataId: Long): Boolean {
        return try {
            val pm = context.packageManager

            val meetPackage = "com.google.android.apps.tachyon"

            if (pm.getLaunchIntentForPackage(meetPackage) == null) {
                Toast.makeText(
                    context,
                    "Google Meet is not installed",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // Extract phone number from contact data
            var phoneNumber: String? = null
            try {
                val phoneUri = Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, dataId.toString())
                val phoneCursor = context.contentResolver.query(
                    phoneUri,
                    arrayOf(ContactsContract.Data.DATA1),
                    null, null, null
                )

                phoneCursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        phoneNumber = cursor.getString(0)
                    }
                }
            } catch (e: Exception) {
                // Failed to extract phone number, continue without it
            }

            if (phoneNumber.isNullOrBlank()) {
                Log.w("ContactIntentHelpers", "No phone number found, cannot make call")
                Toast.makeText(
                    context,
                    "Cannot make Google Meet call - no phone number found",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            // Use the confirmed working method: custom action with Google Meet package
            val callIntent = Intent("com.google.android.apps.tachyon.action.CALL").apply {
                data = Uri.parse("tel:$phoneNumber")
                setPackage(meetPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (callIntent.resolveActivity(pm) != null) {
                context.startActivity(callIntent)
                return true
            } else {
                Log.w("ContactIntentHelpers", "Google Meet custom action not resolved - app may not be installed or updated")
                Toast.makeText(
                    context,
                    "Google Meet call failed - app may not support this method",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

        } catch (e: Exception) {
            Log.e("ContactIntentHelpers", "Failed to open Google Meet video call", e)
            Toast.makeText(
                context,
                "Failed to open Google Meet video call",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    /**
     * Opens a custom app using contact data ID and MIME type.
     */
    fun openCustomAppWithDataId(context: Application, dataId: Long, mimeType: String, packageName: String?): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    mimeType
                )
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if the intent can be resolved
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("MessagingService", "Custom app intent cannot be resolved for mimeType: $mimeType")
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open custom app with dataId", e)
            return false
        }
    }

    /**
     * Opens a custom app with MIME type (fallback method).
     */
    fun openCustomApp(context: Application, data: String, mimeType: String, packageName: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(data), mimeType)
                packageName?.let { setPackage(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open custom app", e)
            Toast.makeText(
                context,
                "Failed to open app",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
