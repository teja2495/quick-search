package com.tk.quicksearch.search.contacts

import android.app.Application

/**
 * WhatsApp-specific contact intent helpers for messaging and calling.
 * @deprecated Use UnifiedMessagingActions.getMessagingApp("com.whatsapp") instead for new code.
 */
object WhatsAppActions {

    private val whatsApp by lazy { UnifiedMessagingActions.getMessagingApp("com.whatsapp")!! }

    /**
     * Opens WhatsApp chat using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppChat(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null) {
        whatsApp.openChat(context, dataId, onShowToast)
    }

    /**
     * Opens WhatsApp chat for a phone number (legacy method for backward compatibility).
     */
    fun openWhatsAppChat(context: Application, phoneNumber: String, onShowToast: ((Int) -> Unit)? = null) {
        whatsApp.openChatByPhoneNumber(context, phoneNumber, onShowToast)
    }

    /**
     * Initiates a WhatsApp call using the contact data URI approach.
     * This method uses ACTION_VIEW with the specific contact data MIME type.
     */
    fun openWhatsAppCall(context: Application, dataId: Long?, onShowToast: ((Int) -> Unit)? = null): Boolean {
        return whatsApp.openCall(context, dataId, onShowToast)
    }

    /**
     * Opens a WhatsApp video call using the specific dataId and MIME type pattern.
     */
    fun openWhatsAppVideoCall(context: Application, dataId: Long, onShowToast: ((Int) -> Unit)? = null): Boolean {
        return whatsApp.openVideoCall(context, dataId, onShowToast)
    }
}
