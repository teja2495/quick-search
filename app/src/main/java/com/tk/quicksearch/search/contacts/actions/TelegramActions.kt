package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PhoneNumberUtils

/**
 * Telegram-specific contact actions.
 * All Telegram functionality consolidated in one place.
 */
object TelegramActions {
    private const val TELEGRAM_PACKAGE = "org.telegram.messenger"

    /**
     * Opens Telegram chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramChat(
        context: Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (dataId == null) {
            Log.w("TelegramActions", "No dataId provided for Telegram chat")
            return
        }

        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$dataId"),
                        ContactMethodMimeTypes.TELEGRAM_MESSAGE,
                    )
                    setPackage(TELEGRAM_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("TelegramActions", "Telegram chat intent cannot be resolved")
                onShowToast?.invoke(R.string.error_telegram_not_installed)
            }
        } catch (e: Exception) {
            Log.e("TelegramActions", "Failed to open Telegram chat", e)
            onShowToast?.invoke(R.string.error_telegram_chat_failed)
        }
    }

    /**
     * Opens Telegram chat for a phone number with comprehensive fallback methods.
     * Uses multiple approaches to ensure maximum compatibility.
     */
    fun openTelegramChat(
        context: Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("TelegramActions", "Invalid phone number for Telegram: $phoneNumber")
            return
        }

        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("TelegramActions", "Could not clean phone number for Telegram: $phoneNumber")
            return
        }

        // Method 1: Use tg://resolve?phone= (preferred for Telegram app)
        try {
            val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(cleanNumber)}")
            val intent =
                Intent(Intent.ACTION_VIEW, tgUri).apply {
                    setPackage(TELEGRAM_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return
            } else {
                Log.w("TelegramActions", "Telegram app not installed, trying web fallback")
            }
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram tg:// method failed", e)
        }

        // Method 2: Fallback to web URL https://t.me/+
        try {
            val webUri = Uri.parse("https://t.me/${Uri.encode(cleanNumber)}")
            val webIntent =
                Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram web fallback failed", e)
        }
    }

    /**
     * Initiates a Telegram call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramCall(
        context: Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("TelegramActions", "No dataId provided for Telegram call")
            return false
        }

        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$dataId"),
                        ContactMethodMimeTypes.TELEGRAM_CALL,
                    )
                    setPackage(TELEGRAM_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("TelegramActions", "Telegram call intent cannot be resolved")
                onShowToast?.invoke(R.string.error_telegram_not_installed)
                return false
            }
        } catch (e: Exception) {
            Log.e("TelegramActions", "Failed to initiate Telegram call", e)
            onShowToast?.invoke(R.string.error_telegram_call_failed)
            return false
        }
    }

    /**
     * Initiates a Telegram call for a phone number.
     */
    fun openTelegramCall(
        context: Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (phoneNumber.isBlank()) return

        val normalizedNumber =
            phoneNumber
                .trim()
                .replace(" ", "")
                .replace("-", "")
                .let { if (it.startsWith("+")) it else "+$it" }

        // Try tg:// scheme for call
        val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(normalizedNumber)}&call=1")
        val intent =
            Intent(Intent.ACTION_VIEW, tgUri).apply {
                setPackage(TELEGRAM_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            onShowToast?.invoke(R.string.error_telegram_not_installed)
        }
    }

    /**
     * Initiates a Telegram video call.
     * Uses the contact data URI for the specific video call row from Contacts provider.
     */
    fun openTelegramVideoCall(
        context: Application,
        dataId: Long?,
        phoneNumber: String?,
    ): Boolean {
        if (dataId == null) {
            Log.w("TelegramActions", "No dataId provided for Telegram video call")
            return false
        }

        // Telegram video calls use a different approach - ACTION_VIEW with URI directly
        return try {
            val contactDataUri = Uri.parse("content://com.android.contacts/data/$dataId")
            val intent =
                Intent(Intent.ACTION_VIEW, contactDataUri).apply {
                    setPackage(TELEGRAM_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("TelegramActions", "No activity found to handle Telegram video call intent")
                false
            }
        } catch (e: Exception) {
            Log.w("TelegramActions", "Telegram video call intent failed", e)
            false
        }
    }
}
