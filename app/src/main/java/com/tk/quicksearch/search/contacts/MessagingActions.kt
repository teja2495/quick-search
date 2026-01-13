package com.tk.quicksearch.search.contacts

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactMethodMimeTypes
import com.tk.quicksearch.util.PhoneNumberUtils

/**
 * Configuration for a messaging app's intent patterns and error messages.
 */
data class MessagingAppConfig(
    val packageName: String,
    val messageMimeType: String,
    val voiceCallMimeType: String,
    val videoCallMimeType: String,
    val errorNotInstalled: Int,
    val errorChatFailed: Int,
    val errorCallFailed: Int,
    val errorVideoCallUnavailable: Int,
    val errorVideoCallFailed: Int
)

/**
 * Unified messaging action system that handles different messaging apps generically.
 */
object UnifiedMessagingActions {

    private val messagingApps = mapOf(
        "com.whatsapp" to MessagingAppConfig(
            packageName = "com.whatsapp",
            messageMimeType = ContactMethodMimeTypes.WHATSAPP_MESSAGE,
            voiceCallMimeType = ContactMethodMimeTypes.WHATSAPP_VOICE_CALL,
            videoCallMimeType = "vnd.android.cursor.item/vnd.com.whatsapp.video.call",
            errorNotInstalled = R.string.error_whatsapp_not_installed,
            errorChatFailed = R.string.error_whatsapp_chat_failed,
            errorCallFailed = R.string.error_whatsapp_call_failed,
            errorVideoCallUnavailable = R.string.error_whatsapp_video_call_unavailable,
            errorVideoCallFailed = R.string.error_whatsapp_video_call_failed
        ),
        "org.telegram.messenger" to MessagingAppConfig(
            packageName = "org.telegram.messenger",
            messageMimeType = ContactMethodMimeTypes.TELEGRAM_MESSAGE,
            voiceCallMimeType = ContactMethodMimeTypes.TELEGRAM_CALL,
            videoCallMimeType = "", // Telegram video calls use implicit MIME type from URI
            errorNotInstalled = R.string.error_telegram_not_installed,
            errorChatFailed = R.string.error_telegram_chat_failed,
            errorCallFailed = R.string.error_telegram_call_failed,
            errorVideoCallUnavailable = R.string.error_telegram_call_failed,
            errorVideoCallFailed = R.string.error_telegram_call_failed
        )
    )

    fun getMessagingApp(packageName: String): MessagingApp? = messagingApps[packageName]?.let { MessagingApp(packageName, it) }

    fun getAvailableMessagingApps(): List<MessagingApp> = messagingApps.map { MessagingApp(it.key, it.value) }
}

/**
 * Data class representing a messaging app with its configuration.
 */
data class MessagingApp(val packageName: String, val config: MessagingAppConfig) {

    fun openChat(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) {
        MessagingActions.openChat(context, dataId, config, onShowToast)
    }

    fun openChatByPhoneNumber(context: Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) {
        MessagingActions.openChatByPhoneNumber(context, phoneNumber, config, onShowToast)
    }

    fun openCall(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean {
        return MessagingActions.openCall(context, dataId, config, onShowToast)
    }

    fun openVideoCall(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean {
        return MessagingActions.openVideoCall(context, dataId, config, onShowToast)
    }
}

/**
 * Legacy messaging actions that work with different messaging apps.
 * @deprecated Use UnifiedMessagingActions instead for new code.
 */
object MessagingActions {

    /**
     * Opens a chat using the contact data URI approach.
     */
    fun openChat(context: Application, dataId: Long?, config: MessagingAppConfig, onShowToast: ((Int) -> Unit)? = null) {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for ${config.packageName} chat, using fallback")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    config.messageMimeType
                )
                setPackage(config.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
            } else {
                Log.w("MessagingService", "${config.packageName} chat intent cannot be resolved")
                onShowToast?.invoke(config.errorNotInstalled)
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open ${config.packageName} chat", e)
            onShowToast?.invoke(config.errorChatFailed)
        }
    }

    /**
     * Opens a chat for a phone number with multiple fallback methods.
     */
    fun openChatByPhoneNumber(context: Application, phoneNumber: String, config: MessagingAppConfig, onShowToast: ((Int) -> Unit)? = null) {
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Log.w("MessagingService", "Invalid phone number for ${config.packageName}: $phoneNumber")
            return
        }

        val cleanNumber = PhoneNumberUtils.cleanPhoneNumber(phoneNumber)
        if (cleanNumber == null) {
            Log.w("MessagingService", "Could not clean phone number for ${config.packageName}: $phoneNumber")
            return
        }

        // Try different methods based on the app
        when (config.packageName) {
            "org.telegram.messenger" -> openTelegramChatByPhone(context, cleanNumber)
            "com.whatsapp" -> openWhatsAppChatByPhone(context, cleanNumber)
            else -> openGenericChatByPhone(context, cleanNumber, config, onShowToast)
        }
    }

    private fun openTelegramChatByPhone(context: Application, cleanNumber: String) {
        // Method 1: Use tg://resolve?phone= (preferred for Telegram app)
        try {
            val tgUri = Uri.parse("tg://resolve?phone=${Uri.encode(cleanNumber)}")
            val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return
            } else {
                Log.w("MessagingService", "Telegram app not installed, trying web fallback")
            }
        } catch (e: Exception) {
            Log.w("MessagingService", "Telegram tg:// method failed", e)
        }

        // Method 2: Fallback to web URL https://t.me/+
        try {
            val webUri = Uri.parse("https://t.me/${Uri.encode(cleanNumber)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.w("MessagingService", "Telegram web fallback failed", e)
        }
    }

    private fun openWhatsAppChatByPhone(context: Application, cleanNumber: String) {
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
                    ContactBasicActions.performSms(context, cleanNumber)
                }
            }
        }
    }

    private fun openGenericChatByPhone(context: Application, cleanNumber: String, config: MessagingAppConfig, onShowToast: ((Int) -> Unit)?) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                setPackage(config.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("MessagingService", "Generic messaging method failed for ${config.packageName}", e)
            // Fallback to SMS
            ContactBasicActions.performSms(context, cleanNumber)
        }
    }

    /**
     * Initiates a call using the contact data URI approach.
     */
    fun openCall(context: Application, dataId: Long?, config: MessagingAppConfig, onShowToast: ((Int) -> Unit)? = null): Boolean {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for ${config.packageName} call")
            return false
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    config.voiceCallMimeType
                )
                setPackage(config.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            } else {
                Log.w("MessagingService", "${config.packageName} call intent cannot be resolved")
                onShowToast?.invoke(config.errorNotInstalled)
                return false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to initiate ${config.packageName} call", e)
            onShowToast?.invoke(config.errorCallFailed)
            return false
        }
    }

    /**
     * Opens a video call using the specific dataId and MIME type pattern.
     */
    fun openVideoCall(context: Application, dataId: Long?, config: MessagingAppConfig, onShowToast: ((Int) -> Unit)? = null): Boolean {
        if (dataId == null) {
            Log.w("MessagingService", "No dataId provided for ${config.packageName} video call")
            return false
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://com.android.contacts/data/$dataId"),
                    config.videoCallMimeType
                )
                setPackage(config.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w("MessagingService", "No activity found to handle ${config.packageName} video call")
                onShowToast?.invoke(config.errorVideoCallUnavailable)
                false
            }
        } catch (e: Exception) {
            Log.e("MessagingService", "Failed to open ${config.packageName} video call", e)
            onShowToast?.invoke(config.errorVideoCallFailed)
            false
        }
    }
}