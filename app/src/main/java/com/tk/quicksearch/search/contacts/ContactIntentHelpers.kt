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
    fun openContact(context: android.app.Application, contactInfo: ContactInfo, onShowToast: ((Int) -> Unit)? = null) =
        ContactBasicActions.openContact(context, contactInfo, onShowToast)

    fun performDial(context: android.app.Application, number: String) =
        ContactBasicActions.performDial(context, number)

    fun performDirectCall(context: android.app.Application, number: String) =
        ContactBasicActions.performDirectCall(context, number)

    fun performCall(context: android.app.Application, number: String) =
        ContactBasicActions.performCall(context, number)

    fun performSms(context: android.app.Application, number: String) =
        ContactBasicActions.performSms(context, number)

    fun composeEmail(context: android.app.Application, email: String, onShowToast: ((Int) -> Unit)? = null) =
        ContactBasicActions.composeEmail(context, email, onShowToast)

    // WhatsApp actions
    fun openWhatsAppChat(context: android.app.Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) =
        WhatsAppActions.openWhatsAppChat(context, dataId, onShowToast)

    fun openWhatsAppChat(context: android.app.Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) =
        WhatsAppActions.openWhatsAppChat(context, phoneNumber, onShowToast)

    fun openWhatsAppCall(context: android.app.Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean =
        WhatsAppActions.openWhatsAppCall(context, dataId, onShowToast)

    fun openWhatsAppVideoCall(context: android.app.Application, dataId: Long, onShowToast: ((Int) -> Unit)? = null): Boolean =
        WhatsAppActions.openWhatsAppVideoCall(context, dataId, onShowToast)

    // Telegram actions
    fun openTelegramChat(context: android.app.Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) =
        TelegramActions.openTelegramChat(context, dataId, onShowToast)

    fun openTelegramChat(context: android.app.Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) =
        TelegramActions.openTelegramChat(context, phoneNumber, onShowToast)

    fun openTelegramCall(context: android.app.Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean =
        TelegramActions.openTelegramCall(context, dataId, onShowToast)

    fun openTelegramCall(context: android.app.Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) =
        TelegramActions.openTelegramCall(context, phoneNumber, onShowToast)

    fun openTelegramVideoCall(context: android.app.Application, dataId: Long?, phoneNumber: String?, onShowToast: ((Int) -> Unit)? = null): Boolean =
        TelegramActions.openTelegramVideoCall(context, dataId, phoneNumber)

    // Google Meet actions
    fun openGoogleMeet(context: android.app.Application, dataId: Long, onShowToast: ((Int) -> Unit)? = null): Boolean =
        GoogleMeetActions.openGoogleMeet(context, dataId, onShowToast)

    // Custom app actions
    fun openVideoCall(context: android.app.Application, data: String, packageName: String, onShowToast: ((Int) -> Unit)? = null) =
        CustomAppActions.openVideoCall(context, data, packageName, onShowToast)

    fun openCustomAppWithDataId(context: android.app.Application, dataId: Long, mimeType: String, packageName: String?, onShowToast: ((Int) -> Unit)? = null): Boolean =
        CustomAppActions.openCustomAppWithDataId(context, dataId, mimeType, packageName)

    fun openCustomApp(context: android.app.Application, data: String, mimeType: String, packageName: String?, onShowToast: ((Int) -> Unit)? = null) =
        CustomAppActions.openCustomApp(context, data, mimeType, packageName, onShowToast)
}
