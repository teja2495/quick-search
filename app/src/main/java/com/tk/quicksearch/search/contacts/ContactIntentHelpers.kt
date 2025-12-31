package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.search.contacts.ContactBasicActions
import com.tk.quicksearch.search.contacts.CustomAppActions
import com.tk.quicksearch.search.contacts.GoogleMeetActions
import com.tk.quicksearch.search.contacts.TelegramActions
import com.tk.quicksearch.search.contacts.WhatsAppActions

/**
 * Main entry point for contact-related intent helpers.
 * This object delegates to specialized action classes for better organization.
 */
object ContactIntentHelpers {

    // Basic contact actions (open contact, call, SMS, email)
    fun openContact(context: android.app.Application, contactInfo: ContactInfo) =
        ContactBasicActions.openContact(context, contactInfo)

    fun performDial(context: android.app.Application, number: String) =
        ContactBasicActions.performDial(context, number)

    fun performDirectCall(context: android.app.Application, number: String) =
        ContactBasicActions.performDirectCall(context, number)

    fun performCall(context: android.app.Application, number: String) =
        ContactBasicActions.performCall(context, number)

    fun performSms(context: android.app.Application, number: String) =
        ContactBasicActions.performSms(context, number)

    fun composeEmail(context: android.app.Application, email: String) =
        ContactBasicActions.composeEmail(context, email)

    // WhatsApp actions
    fun openWhatsAppChat(context: android.app.Application, dataId: Long?) =
        WhatsAppActions.openWhatsAppChat(context, dataId)

    fun openWhatsAppChat(context: android.app.Application, phoneNumber: String) =
        WhatsAppActions.openWhatsAppChat(context, phoneNumber)

    fun openWhatsAppCall(context: android.app.Application, dataId: Long?): Boolean =
        WhatsAppActions.openWhatsAppCall(context, dataId)

    fun openWhatsAppVideoCall(context: android.app.Application, dataId: Long): Boolean =
        WhatsAppActions.openWhatsAppVideoCall(context, dataId)

    // Telegram actions
    fun openTelegramChat(context: android.app.Application, dataId: Long?) =
        TelegramActions.openTelegramChat(context, dataId)

    fun openTelegramChat(context: android.app.Application, phoneNumber: String) =
        TelegramActions.openTelegramChat(context, phoneNumber)

    fun openTelegramCall(context: android.app.Application, dataId: Long?): Boolean =
        TelegramActions.openTelegramCall(context, dataId)

    fun openTelegramCall(context: android.app.Application, phoneNumber: String) =
        TelegramActions.openTelegramCall(context, phoneNumber)

    fun openTelegramVideoCall(context: android.app.Application, dataId: Long?, phoneNumber: String?): Boolean =
        TelegramActions.openTelegramVideoCall(context, dataId, phoneNumber)

    // Google Meet actions
    fun openGoogleMeet(context: android.app.Application, dataId: Long): Boolean =
        GoogleMeetActions.openGoogleMeet(context, dataId)

    // Custom app actions
    fun openVideoCall(context: android.app.Application, data: String, packageName: String) =
        CustomAppActions.openVideoCall(context, data, packageName)

    fun openCustomAppWithDataId(context: android.app.Application, dataId: Long, mimeType: String, packageName: String?): Boolean =
        CustomAppActions.openCustomAppWithDataId(context, dataId, mimeType, packageName)

    fun openCustomApp(context: android.app.Application, data: String, mimeType: String, packageName: String?) =
        CustomAppActions.openCustomApp(context, data, mimeType, packageName)
}
