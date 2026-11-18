package com.tk.quicksearch.search.contacts.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.shared.util.PackageConstants

enum class ContactActionProvider(
    val packageName: String,
    @StringRes val messagingLabel: Int? = null,
    val showsMessagingSetup: Boolean = messagingLabel != null,
    private val customMethods: Boolean = false,
) {
    WHATSAPP(PackageConstants.WHATSAPP_PACKAGE, R.string.contact_method_whatsapp_message_label),
    WHATSAPP_BUSINESS(
        PackageConstants.WHATSAPP_BUSINESS_PACKAGE,
        R.string.contact_method_whatsapp_business_label,
        showsMessagingSetup = false,
        customMethods = true,
    ),
    TELEGRAM(PackageConstants.TELEGRAM_PACKAGE, R.string.contact_method_telegram_message_label),
    SIGNAL(PackageConstants.SIGNAL_PACKAGE, R.string.contact_method_signal_message_label),
    MEET(PackageConstants.GOOGLE_MEET_PACKAGE),
    ;

    fun owns(method: ContactMethod): Boolean =
        if (customMethods) {
            method is ContactMethod.CustomApp && method.packageName == packageName
        } else {
            ContactButtonAction.entries.any { it.provider == this && it.methodMatches(method) }
        }
}

internal enum class ContactActionIconStyle { LOGO, APP_VOICE_CALL, SIGNAL_MESSAGE }

/** The stable selection and behavior of a default contact-card button. */
enum class ContactButtonAction(
    @StringRes val labelRes: Int,
    internal val provider: ContactActionProvider? = null,
    @DrawableRes internal val drawableRes: Int? = null,
    internal val iconStyle: ContactActionIconStyle = ContactActionIconStyle.LOGO,
    private val messagingApp: MessagingApp? = null,
    internal val requiresCallPermission: Boolean = false,
    internal val methodMatches: (ContactMethod) -> Boolean,
    internal val cardAction: ((String) -> ContactCardAction)? = null,
    internal val usesNumberMatch: Boolean = true,
    internal val requiresDataId: Boolean = true,
    internal val availableWithoutMethod: Boolean = false,
    internal val allowsAnyProviderMethod: Boolean = false,
    internal val launch: ((ContactActionHandler, ContactInfo, String) -> Unit)? = null,
) {
    CALL(
        R.string.contact_method_call_label,
        methodMatches = { it is ContactMethod.Phone },
        cardAction = { ContactCardAction.Phone(it) },
        availableWithoutMethod = true,
        launch = { handler, contact, number -> handler.launchPhoneButton(contact, number) },
    ),
    SMS(
        R.string.contact_button_sms,
        messagingApp = MessagingApp.MESSAGES,
        methodMatches = { it is ContactMethod.Sms },
        cardAction = { ContactCardAction.Sms(it) },
        availableWithoutMethod = true,
        launch = { handler, _, number -> handler.launchSmsButton(number) },
    ),
    EMAIL(
        R.string.contact_method_email_label,
        methodMatches = { it is ContactMethod.Email },
        usesNumberMatch = false,
        requiresDataId = false,
        cardAction = { ContactCardAction.Email(it) },
    ),
    GOOGLE_MEET(
        R.string.contact_method_google_meet_label,
        provider = ContactActionProvider.MEET,
        drawableRes = R.drawable.google_meet,
        methodMatches = { it is ContactMethod.GoogleMeet },
        cardAction = { ContactCardAction.GoogleMeet(it) },
    ),
    WHATSAPP_MESSAGE(
        R.string.contact_button_whatsapp_message,
        provider = ContactActionProvider.WHATSAPP,
        drawableRes = R.drawable.whatsapp,
        messagingApp = MessagingApp.WHATSAPP,
        allowsAnyProviderMethod = true,
        methodMatches = { it is ContactMethod.WhatsAppMessage },
        cardAction = { ContactCardAction.WhatsAppMessage(it) },
        launch = { handler, _, number -> handler.launchWhatsAppButton(number) },
    ),
    WHATSAPP_CALL(
        R.string.contact_method_whatsapp_voice_call_label,
        provider = ContactActionProvider.WHATSAPP,
        drawableRes = R.drawable.whatsapp_call,
        iconStyle = ContactActionIconStyle.APP_VOICE_CALL,
        requiresCallPermission = true,
        methodMatches = { it is ContactMethod.WhatsAppCall },
        cardAction = { ContactCardAction.WhatsAppCall(it) },
    ),
    WHATSAPP_VIDEO_CALL(
        R.string.contact_method_whatsapp_video_call_label,
        provider = ContactActionProvider.WHATSAPP,
        drawableRes = R.drawable.whatsapp_video_call,
        requiresCallPermission = true,
        methodMatches = { it is ContactMethod.WhatsAppVideoCall },
        cardAction = { ContactCardAction.WhatsAppVideoCall(it) },
    ),
    WHATSAPP_BUSINESS_MESSAGE(
        R.string.contact_button_whatsapp_business_message,
        provider = ContactActionProvider.WHATSAPP_BUSINESS,
        drawableRes = R.drawable.whatsapp,
        messagingApp = MessagingApp.WHATSAPP_BUSINESS,
        allowsAnyProviderMethod = true,
        methodMatches = { it.isWhatsAppBusinessMethod(ContactMethodMimeTypes.WHATSAPP_BUSINESS_MESSAGE) },
        launch = { handler, contact, number -> handler.launchWhatsAppBusinessButton(contact, number) },
    ),
    WHATSAPP_BUSINESS_CALL(
        R.string.contact_button_whatsapp_business_voice_call,
        provider = ContactActionProvider.WHATSAPP_BUSINESS,
        drawableRes = R.drawable.whatsapp_call,
        iconStyle = ContactActionIconStyle.APP_VOICE_CALL,
        requiresCallPermission = true,
        methodMatches = { it.isWhatsAppBusinessMethod(ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL) },
    ),
    WHATSAPP_BUSINESS_VIDEO_CALL(
        R.string.contact_button_whatsapp_business_video_call,
        provider = ContactActionProvider.WHATSAPP_BUSINESS,
        drawableRes = R.drawable.whatsapp_video_call,
        requiresCallPermission = true,
        methodMatches = { it.isWhatsAppBusinessMethod(ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL) },
    ),
    TELEGRAM_MESSAGE(
        R.string.contact_button_telegram_message,
        provider = ContactActionProvider.TELEGRAM,
        drawableRes = R.drawable.telegram,
        messagingApp = MessagingApp.TELEGRAM,
        allowsAnyProviderMethod = true,
        methodMatches = { it is ContactMethod.TelegramMessage },
        cardAction = { ContactCardAction.TelegramMessage(it) },
        usesNumberMatch = false,
        launch = { handler, _, number -> handler.launchTelegramMessageButton(number) },
    ),
    TELEGRAM_CALL(
        R.string.contact_method_telegram_voice_call_label,
        provider = ContactActionProvider.TELEGRAM,
        drawableRes = R.drawable.telegram_call,
        iconStyle = ContactActionIconStyle.APP_VOICE_CALL,
        requiresCallPermission = true,
        methodMatches = { it is ContactMethod.TelegramCall },
        cardAction = { ContactCardAction.TelegramCall(it) },
        usesNumberMatch = false,
        launch = { handler, contact, number -> handler.launchTelegramCallButton(contact, number) },
    ),
    TELEGRAM_VIDEO_CALL(
        R.string.contact_method_telegram_video_call_label,
        provider = ContactActionProvider.TELEGRAM,
        drawableRes = R.drawable.telegram_video_call,
        requiresCallPermission = true,
        methodMatches = { it is ContactMethod.TelegramVideoCall },
        cardAction = { ContactCardAction.TelegramVideoCall(it) },
        usesNumberMatch = false,
        requiresDataId = false,
    ),
    SIGNAL_MESSAGE(
        R.string.contact_button_signal_message,
        provider = ContactActionProvider.SIGNAL,
        drawableRes = R.drawable.signal,
        iconStyle = ContactActionIconStyle.SIGNAL_MESSAGE,
        messagingApp = MessagingApp.SIGNAL,
        allowsAnyProviderMethod = true,
        methodMatches = { it is ContactMethod.SignalMessage },
        cardAction = { ContactCardAction.SignalMessage(it) },
        launch = { handler, _, number -> handler.launchSignalMessageButton(number) },
    ),
    SIGNAL_CALL(
        R.string.contact_method_signal_voice_call_label,
        provider = ContactActionProvider.SIGNAL,
        drawableRes = R.drawable.signal_call,
        iconStyle = ContactActionIconStyle.APP_VOICE_CALL,
        requiresCallPermission = true,
        methodMatches = { it is ContactMethod.SignalCall },
        cardAction = { ContactCardAction.SignalCall(it) },
    ),
    SIGNAL_VIDEO_CALL(
        R.string.contact_method_signal_video_call_label,
        provider = ContactActionProvider.SIGNAL,
        drawableRes = R.drawable.signal_video_call,
        requiresCallPermission = true,
        methodMatches = { it is ContactMethod.SignalVideoCall },
        cardAction = { ContactCardAction.SignalVideoCall(it) },
    ),
    ;

    val isMessage: Boolean get() = messagingApp != null

    fun isAppInstalled(installed: Set<ContactActionProvider>): Boolean = provider == null || provider in installed

    val messagingLabelRes: Int
        get() = provider?.messagingLabel ?: R.string.settings_messaging_option_messages

    fun toMessagingApp(): MessagingApp? = messagingApp

    companion object {
        fun fallback(isPrimary: Boolean): ContactButtonAction = if (isPrimary) CALL else SMS

        fun fromMessagingApp(app: MessagingApp): ContactButtonAction =
            entries.first { it.messagingApp == app }
    }
}

private fun ContactMethod.isWhatsAppBusinessMethod(mimeType: String): Boolean =
    this is ContactMethod.CustomApp &&
        packageName == ContactActionProvider.WHATSAPP_BUSINESS.packageName &&
        this.mimeType == mimeType
