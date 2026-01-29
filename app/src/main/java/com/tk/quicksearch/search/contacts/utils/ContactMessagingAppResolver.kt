package com.tk.quicksearch.search.contacts.utils

import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod

object ContactMessagingAppResolver {
    fun resolveMessagingAppForContact(
        contactInfo: ContactInfo,
        defaultApp: MessagingApp,
    ): MessagingApp =
        when (defaultApp) {
            MessagingApp.WHATSAPP -> {
                if (contactInfo.hasWhatsAppMethods()) MessagingApp.WHATSAPP else MessagingApp.MESSAGES
            }

            MessagingApp.TELEGRAM -> {
                if (contactInfo.hasTelegramMethods()) MessagingApp.TELEGRAM else MessagingApp.MESSAGES
            }

            MessagingApp.MESSAGES -> {
                MessagingApp.MESSAGES
            }
        }

    private fun ContactInfo.hasWhatsAppMethods(): Boolean =
        contactMethods.any { method ->
            method is ContactMethod.WhatsAppMessage ||
                method is ContactMethod.WhatsAppCall ||
                method is ContactMethod.WhatsAppVideoCall
        }

    private fun ContactInfo.hasTelegramMethods(): Boolean =
        contactMethods.any { method ->
            method is ContactMethod.TelegramMessage ||
                method is ContactMethod.TelegramCall ||
                method is ContactMethod.TelegramVideoCall
        }
}
