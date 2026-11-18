package com.tk.quicksearch.search.contacts.models

import androidx.annotation.StringRes
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp

/**
 * Default action for the first (primary) or second (secondary) button on contact cards.
 * When a contact can't use the chosen action, the first button falls back to [CALL] and the
 * second to [SMS]. Per-contact [ContactCardAction] overrides take precedence.
 */
enum class ContactButtonAction(
    @StringRes val labelRes: Int,
) {
    CALL(R.string.contact_method_call_label),
    SMS(R.string.contact_button_sms),
    EMAIL(R.string.contact_method_email_label),
    GOOGLE_MEET(R.string.contact_method_google_meet_label),
    WHATSAPP_MESSAGE(R.string.contact_button_whatsapp_message),
    WHATSAPP_CALL(R.string.contact_method_whatsapp_voice_call_label),
    WHATSAPP_VIDEO_CALL(R.string.contact_method_whatsapp_video_call_label),
    WHATSAPP_BUSINESS_MESSAGE(R.string.contact_button_whatsapp_business_message),
    WHATSAPP_BUSINESS_CALL(R.string.contact_button_whatsapp_business_voice_call),
    WHATSAPP_BUSINESS_VIDEO_CALL(R.string.contact_button_whatsapp_business_video_call),
    TELEGRAM_MESSAGE(R.string.contact_button_telegram_message),
    TELEGRAM_CALL(R.string.contact_method_telegram_voice_call_label),
    TELEGRAM_VIDEO_CALL(R.string.contact_method_telegram_video_call_label),
    SIGNAL_MESSAGE(R.string.contact_button_signal_message),
    SIGNAL_CALL(R.string.contact_method_signal_voice_call_label),
    SIGNAL_VIDEO_CALL(R.string.contact_method_signal_video_call_label),
    ;

    val isMessage: Boolean
        get() =
            this == SMS ||
                this == WHATSAPP_MESSAGE ||
                this == WHATSAPP_BUSINESS_MESSAGE ||
                this == TELEGRAM_MESSAGE ||
                this == SIGNAL_MESSAGE

    fun isAppInstalled(
        isWhatsAppInstalled: Boolean,
        isWhatsAppBusinessInstalled: Boolean,
        isTelegramInstalled: Boolean,
        isSignalInstalled: Boolean,
        isGoogleMeetInstalled: Boolean,
    ): Boolean =
        when (this) {
            CALL, SMS, EMAIL -> true
            GOOGLE_MEET -> isGoogleMeetInstalled
            WHATSAPP_MESSAGE, WHATSAPP_CALL, WHATSAPP_VIDEO_CALL -> isWhatsAppInstalled
            WHATSAPP_BUSINESS_MESSAGE, WHATSAPP_BUSINESS_CALL, WHATSAPP_BUSINESS_VIDEO_CALL ->
                isWhatsAppBusinessInstalled
            TELEGRAM_MESSAGE, TELEGRAM_CALL, TELEGRAM_VIDEO_CALL -> isTelegramInstalled
            SIGNAL_MESSAGE, SIGNAL_CALL, SIGNAL_VIDEO_CALL -> isSignalInstalled
        }

    /** The onboarding messaging-app choice this action matches, if any. */
    fun toMessagingApp(): MessagingApp? =
        when (this) {
            SMS -> MessagingApp.MESSAGES
            WHATSAPP_MESSAGE -> MessagingApp.WHATSAPP
            WHATSAPP_BUSINESS_MESSAGE -> MessagingApp.WHATSAPP_BUSINESS
            TELEGRAM_MESSAGE -> MessagingApp.TELEGRAM
            SIGNAL_MESSAGE -> MessagingApp.SIGNAL
            else -> null
        }

    companion object {
        fun fallback(isPrimary: Boolean): ContactButtonAction = if (isPrimary) CALL else SMS

        fun fromMessagingApp(app: MessagingApp): ContactButtonAction =
            when (app) {
                MessagingApp.MESSAGES -> SMS
                MessagingApp.WHATSAPP -> WHATSAPP_MESSAGE
                MessagingApp.WHATSAPP_BUSINESS -> WHATSAPP_BUSINESS_MESSAGE
                MessagingApp.TELEGRAM -> TELEGRAM_MESSAGE
                MessagingApp.SIGNAL -> SIGNAL_MESSAGE
            }
    }
}
