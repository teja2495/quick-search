package com.tk.quicksearch.search.contacts.utils

import com.tk.quicksearch.search.contacts.actions.CallSmsActions
import com.tk.quicksearch.search.contacts.actions.CustomAppActions
import com.tk.quicksearch.search.contacts.actions.GoogleMeetActions
import com.tk.quicksearch.search.contacts.actions.TelegramActions
import com.tk.quicksearch.search.contacts.actions.WhatsAppActions
import com.tk.quicksearch.search.models.ContactInfo

/**
 * Main entry point for contact-related intent helpers.
 * Organized by app type for easy reference and maintenance.
 */
object ContactIntentHelpers {
    // ===== CALL & SMS ACTIONS =====
    fun openContact(
        context: android.app.Application,
        contactInfo: ContactInfo,
        onShowToast: ((Int) -> Unit)? = null,
    ) = CallSmsActions.openContact(context, contactInfo, onShowToast)

    fun performDial(
        context: android.app.Application,
        number: String,
    ) = CallSmsActions.performDial(context, number)

    fun performDirectCall(
        context: android.app.Application,
        number: String,
    ) = CallSmsActions.performDirectCall(context, number)

    fun performCall(
        context: android.app.Application,
        number: String,
    ) = CallSmsActions.performCall(context, number)

    fun performSms(
        context: android.app.Application,
        number: String,
    ) = CallSmsActions.performSms(context, number)

    fun composeEmail(
        context: android.app.Application,
        email: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) = CallSmsActions.composeEmail(context, email, onShowToast)

    // ===== WHATSAPP ACTIONS =====
    fun openWhatsAppChat(
        context: android.app.Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ) = WhatsAppActions.openWhatsAppChat(context, dataId, onShowToast)

    fun openWhatsAppChat(
        context: android.app.Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) = WhatsAppActions.openWhatsAppChat(context, phoneNumber, onShowToast)

    fun openWhatsAppCall(
        context: android.app.Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean = WhatsAppActions.openWhatsAppCall(context, dataId, onShowToast)

    fun openWhatsAppVideoCall(
        context: android.app.Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean = WhatsAppActions.openWhatsAppVideoCall(context, dataId, onShowToast)

    // ===== TELEGRAM ACTIONS =====
    fun openTelegramChat(
        context: android.app.Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ) = TelegramActions.openTelegramChat(context, dataId, onShowToast)

    fun openTelegramChat(
        context: android.app.Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) = TelegramActions.openTelegramChat(context, phoneNumber, onShowToast)

    fun openTelegramCall(
        context: android.app.Application,
        dataId: Long?,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean = TelegramActions.openTelegramCall(context, dataId, onShowToast)

    fun openTelegramCall(
        context: android.app.Application,
        phoneNumber: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) = TelegramActions.openTelegramCall(context, phoneNumber, onShowToast)

    fun openTelegramVideoCall(
        context: android.app.Application,
        dataId: Long?,
        phoneNumber: String?,
    ): Boolean = TelegramActions.openTelegramVideoCall(context, dataId, phoneNumber)

    // ===== GOOGLE MEET ACTIONS =====
    fun openGoogleMeet(
        context: android.app.Application,
        dataId: Long,
        onShowToast: ((Int) -> Unit)? = null,
    ): Boolean = GoogleMeetActions.openGoogleMeet(context, dataId, onShowToast)

    // ===== CUSTOM APP ACTIONS =====
    fun openVideoCall(
        context: android.app.Application,
        data: String,
        packageName: String,
        onShowToast: ((Int) -> Unit)? = null,
    ) = CustomAppActions.openVideoCall(context, data, packageName, onShowToast)

    fun openCustomAppWithDataId(
        context: android.app.Application,
        dataId: Long,
        mimeType: String,
        packageName: String?,
        onShowToast: (
            (Int) -> Unit
        )? = null,
    ): Boolean {
        val success = CustomAppActions.openCustomAppWithDataId(context, dataId, mimeType, packageName)
        if (!success) {
            onShowToast?.invoke(com.tk.quicksearch.R.string.error_launch_app)
        }
        return success
    }

    fun openCustomApp(
        context: android.app.Application,
        data: String,
        mimeType: String,
        packageName: String?,
        onShowToast: ((Int) -> Unit)? = null,
    ) = CustomAppActions.openCustomApp(context, data, mimeType, packageName, onShowToast)
}
