package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PhoneNumberUtils

/**
 * WhatsApp-specific contact actions.
 * All WhatsApp functionality consolidated in one place.
 */
object WhatsAppActions {
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_MESSAGE_MIME = ContactMethodMimeTypes.WHATSAPP_MESSAGE
    private const val WHATSAPP_VOICE_CALL_MIME = ContactMethodMimeTypes.WHATSAPP_VOICE_CALL
    private const val WHATSAPP_VIDEO_CALL_MIME = ContactMethodMimeTypes.WHATSAPP_VIDEO_CALL

    /**
     * Opens WhatsApp chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppChat(
        context: Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp chat")
            return
        }

        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$dataId"),
                        WHATSAPP_MESSAGE_MIME,
                    )
                    setPackage(WHATSAPP_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("WhatsAppActions", "WhatsApp chat intent cannot be resolved")
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to open WhatsApp chat", e)
            onShowToast?.invoke(R.string.error_whatsapp_chat_failed)
        }
    }

    /**
     * Opens WhatsApp chat for a phone number with multiple fallback methods.
     */
    fun openWhatsAppChat(
        context: Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("WhatsAppActions", "Invalid phone number for WhatsApp: $phoneNumber")
            return
        }

        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("WhatsAppActions", "Could not clean phone number for WhatsApp: $phoneNumber")
            return
        }

        try {
            // Method 1: Use ACTION_SENDTO with smsto scheme for direct chat
            val smsIntent =
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$cleanNumber")
                    setPackage(WHATSAPP_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            Log.w("WhatsAppActions", "WhatsApp method 1 failed", e)
            try {
                // Method 2: Use ACTION_SEND with WhatsApp package
                val sendIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra("jid", "$cleanNumber@s.whatsapp.net")
                        setPackage(WHATSAPP_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(sendIntent)
            } catch (e2: Exception) {
                Log.w("WhatsAppActions", "WhatsApp method 2 failed", e2)
                try {
                    // Method 3: Try standard messaging intent
                    val messageIntent =
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:$cleanNumber")
                            setPackage(WHATSAPP_PACKAGE)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    context.startActivity(messageIntent)
                } catch (e3: Exception) {
                    Log.w("WhatsAppActions", "WhatsApp method 3 failed", e3)
                    // Final fallback to SMS
                    CallSmsActions.performSms(context, cleanNumber)
                }
            }
        }
    }

    /**
     * Initiates a WhatsApp call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppCall(
        context: Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp call")
            return false
        }

        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$dataId"),
                        WHATSAPP_VOICE_CALL_MIME,
                    )
                    setPackage(WHATSAPP_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("WhatsAppActions", "WhatsApp call intent cannot be resolved")
                onShowToast?.invoke(R.string.error_whatsapp_not_installed)
                return false
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to initiate WhatsApp call", e)
            onShowToast?.invoke(R.string.error_whatsapp_call_failed)
            return false
        }
    }

    /**
     * Opens a WhatsApp video call using the specific dataId and MIME type pattern.
     */
    fun openWhatsAppVideoCall(
        context: Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean {
        if (dataId == null) {
            Log.w("WhatsAppActions", "No dataId provided for WhatsApp video call")
            return false
        }

        return try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$dataId"),
                        WHATSAPP_VIDEO_CALL_MIME,
                    )
                    setPackage(WHATSAPP_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("WhatsAppActions", "No activity found to handle WhatsApp video call")
                onShowToast?.invoke(R.string.error_whatsapp_video_call_unavailable)
                false
            }
        } catch (e: Exception) {
            Log.e("WhatsAppActions", "Failed to open WhatsApp video call", e)
            onShowToast?.invoke(R.string.error_whatsapp_video_call_failed)
            false
        }
    }
}
