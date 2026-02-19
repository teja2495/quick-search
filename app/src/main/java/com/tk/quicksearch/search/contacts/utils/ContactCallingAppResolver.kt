package com.tk.quicksearch.search.contacts.utils

import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils

object ContactCallingAppResolver {
    fun resolveCallingAppForContact(
        contactInfo: ContactInfo,
        defaultApp: CallingApp,
        phoneNumber: String? = null,
    ): CallingApp =
        when (defaultApp) {
            CallingApp.WHATSAPP -> {
                if (contactInfo.hasMethod<ContactMethod.WhatsAppCall>(phoneNumber)) CallingApp.WHATSAPP else CallingApp.CALL
            }

            CallingApp.TELEGRAM -> {
                if (contactInfo.hasTelegramCallMethod()) CallingApp.TELEGRAM else CallingApp.CALL
            }

            CallingApp.SIGNAL -> {
                if (contactInfo.hasMethod<ContactMethod.SignalCall>(phoneNumber)) CallingApp.SIGNAL else CallingApp.CALL
            }

            CallingApp.GOOGLE_MEET -> {
                if (contactInfo.hasMethod<ContactMethod.GoogleMeet>(phoneNumber)) CallingApp.GOOGLE_MEET else CallingApp.CALL
            }

            CallingApp.CALL -> CallingApp.CALL
        }

    private inline fun <reified T : ContactMethod> ContactInfo.hasMethod(phoneNumber: String?): Boolean =
        contactMethods.any { method ->
            method is T &&
                (
                    phoneNumber == null ||
                        method.data.isBlank() ||
                        PhoneNumberUtils.isSameNumber(method.data, phoneNumber)
                )
        }

    private fun ContactInfo.hasTelegramCallMethod(): Boolean =
        contactMethods.any { method -> method is ContactMethod.TelegramCall }
}
