package com.tk.quicksearch.model

/**
 * Represents a specific method of contacting someone.
 * This includes phone calls, SMS, and app-specific methods like WhatsApp, Telegram, etc.
 */
sealed class ContactMethod {
    abstract val displayLabel: String
    abstract val data: String // Phone number, email, etc.
    abstract val dataId: Long? // Contact data row ID for direct intent resolution
    abstract val isPrimary: Boolean

    data class Phone(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class Sms(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class WhatsAppCall(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class WhatsAppMessage(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class WhatsAppVideoCall(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class TelegramMessage(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class TelegramCall(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class TelegramVideoCall(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class Email(
        override val displayLabel: String,
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class VideoCall(
        override val displayLabel: String,
        override val data: String,
        val packageName: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class GoogleMeet(
        override val displayLabel: String = "Google Meet",
        override val data: String,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class CustomApp(
        override val displayLabel: String,
        override val data: String,
        val mimeType: String,
        val packageName: String?,
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()

    data class ViewInContactsApp(
        override val displayLabel: String = "View in contacts app",
        override val data: String = "",
        override val dataId: Long? = null,
        override val isPrimary: Boolean = false
    ) : ContactMethod()
}

/**
 * Known MIME types for various communication apps
 */
object ContactMethodMimeTypes {
    const val WHATSAPP_VOICE_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
    const val WHATSAPP_MESSAGE = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    const val WHATSAPP_VIDEO_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
    
    const val TELEGRAM_MESSAGE = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile"
    const val TELEGRAM_CALL = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call"
    const val TELEGRAM_VIDEO_CALL = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call.video"
    
    // Standard Android types
    const val PHONE = "vnd.android.cursor.item/phone_v2"
    const val EMAIL = "vnd.android.cursor.item/email_v2"
    const val IM = "vnd.android.cursor.item/im"
    const val SIP_ADDRESS = "vnd.android.cursor.item/sip_address"
}

