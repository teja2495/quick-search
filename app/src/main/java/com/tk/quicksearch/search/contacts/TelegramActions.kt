package com.tk.quicksearch.search.contacts

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactMethodMimeTypes
import com.tk.quicksearch.util.PhoneNumberUtils

/**
 * Telegram-specific contact intent helpers for messaging and calling.
 */
object TelegramActions {

    /**
     * Opens Telegram chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramChat(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) {
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
                onShowToast?.invoke(R.string.error_telegram_not_installed)
            }
        } catch (e: Exception) {
            Log.e("IntentHelpers", "Failed to open Telegram chat", e)
            onShowToast?.invoke(R.string.error_telegram_chat_failed)
        }
    }

    /**
     * Opens Telegram chat for a phone number with comprehensive fallback methods.
     * Uses multiple approaches to ensure maximum compatibility.
     */
    fun openTelegramChat(context: Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("IntentHelpers", "Invalid phone number for Telegram: $phoneNumber")
            onShowToast?.invoke(R.string.error_telegram_invalid_phone)
            return
        }

        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("IntentHelpers", "Could not clean phone number for Telegram: $phoneNumber")
            onShowToast?.invoke(R.string.error_telegram_process_phone)
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
            onShowToast?.invoke(R.string.error_telegram_web_fallback_failed)
        }
    }

    /**
     * Initiates a Telegram call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramCall(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean {
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
                onShowToast?.invoke(R.string.error_telegram_not_installed)
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to initiate Telegram call", e)
            onShowToast?.invoke(R.string.error_telegram_call_failed)
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
    fun openTelegramCall(context: Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) {
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
            onShowToast?.invoke(R.string.error_telegram_not_installed)
        }
    }
}
