package com.tk.quicksearch.search.contacts

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R

/**
 * Telegram-specific contact intent helpers for messaging and calling.
 * @deprecated Use UnifiedMessagingActions.getMessagingApp("org.telegram.messenger") instead for new code.
 */
object TelegramActions {

    private val telegram by lazy { UnifiedMessagingActions.getMessagingApp("org.telegram.messenger")!! }

    /**
     * Opens Telegram chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramChat(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) {
        telegram.openChat(context, dataId, onShowToast)
    }

    /**
     * Opens Telegram chat for a phone number with comprehensive fallback methods.
     * Uses multiple approaches to ensure maximum compatibility.
     */
    fun openTelegramChat(context: Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) {
        telegram.openChatByPhoneNumber(context, phoneNumber, onShowToast)
    }

    /**
     * Initiates a Telegram call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openTelegramCall(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean {
        return telegram.openCall(context, dataId, onShowToast)
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

        // Telegram video calls use a different approach - ACTION_VIEW with URI directly
        return try {
            val contactDataUri = Uri.parse("content://com.android.contacts/data/$dataId")
            val intent = Intent(Intent.ACTION_VIEW, contactDataUri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("MessagingService", "No activity found to handle Telegram video call intent")
                false
            }
        } catch (e: Exception) {
            Log.w("MessagingService", "Telegram video call intent failed", e)
            false
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
        } catch (e: Exception) {
            onShowToast?.invoke(R.string.error_telegram_not_installed)
        }
    }
}
