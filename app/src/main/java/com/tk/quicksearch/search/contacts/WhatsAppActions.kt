package com.tk.quicksearch.search.contacts

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactMethodMimeTypes
import com.tk.quicksearch.util.PhoneNumberUtils

/**
 * WhatsApp-specific contact intent helpers for messaging and calling.
 */
object WhatsAppActions {

    /**
     * Opens WhatsApp chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppChat(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) {
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
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open WhatsApp chat", e)
            onShowToast?.invoke(R.string.error_whatsapp_chat_failed)
        }
    }

    /**
     * Opens WhatsApp chat for a phone number (legacy method for backward compatibility).
     */
    fun openWhatsAppChat(context: Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) {
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
                    ContactBasicActions.performSms(context, phoneNumber)
                }
            }
        }
    }

    /**
     * Initiates a WhatsApp call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppCall(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean {
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
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to initiate WhatsApp call", e)
            onShowToast?.invoke(R.string.error_whatsapp_call_failed)
            return false
        }
    }

    /**
     * Opens a WhatsApp video call using the specific dataId and MIME type pattern.
     */
    fun openWhatsAppVideoCall(context: Application, dataId: Long, onShowToast: ((Int) -> Unit)? = null): Boolean {
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
                onShowToast?.invoke(R.string.error_whatsapp_video_call_unavailable)
                false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open WhatsApp video call", e)
            onShowToast?.invoke(R.string.error_whatsapp_video_call_failed)
            false
        }
    }
}
