package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.contacts.models.ContactActionProvider
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.ContactButtonResolver
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactButtonDefinitionTest {
    @Test
    fun messagingPreferencesRoundTripAndProviderPackagesStayDistinct() {
        MessagingApp.entries.forEach { assertEquals(it, ContactButtonAction.fromMessagingApp(it).toMessagingApp()) }
        val business = setOf(ContactActionProvider.WHATSAPP_BUSINESS)
        assertTrue(ContactButtonAction.WHATSAPP_BUSINESS_MESSAGE.isAppInstalled(business))
        assertFalse(ContactButtonAction.WHATSAPP_MESSAGE.isAppInstalled(business))
        assertTrue(ContactButtonAction.SMS.isAppInstalled(emptySet()))
    }

    @Test
    fun voiceOnlyWhatsappContactCanStillOpenAChat() {
        val method = ContactMethod.WhatsAppCall("Voice", "123", dataId = 10L)
        val contact = ContactInfo(1, "person", "Person", listOf("123"), contactMethods = listOf(method))
        assertTrue(ContactButtonResolver.isAvailableForContact(contact, ContactButtonAction.WHATSAPP_MESSAGE))
        assertFalse(ContactButtonResolver.isAvailableForContact(contact, ContactButtonAction.WHATSAPP_BUSINESS_MESSAGE))
        assertEquals(
            ContactCardAction.WhatsAppMessage("123"),
            ContactButtonResolver.defaultCardAction(contact, ContactButtonAction.WHATSAPP_MESSAGE, isPrimary = false),
        )
    }

    @Test
    fun businessActionKeepsTheProviderDataRow() {
        val method = ContactMethod.CustomApp(
            displayLabel = "Business",
            data = "123",
            mimeType = ContactMethodMimeTypes.WHATSAPP_BUSINESS_MESSAGE,
            packageName = ContactActionProvider.WHATSAPP_BUSINESS.packageName,
            dataId = 12L,
        )
        val contact = ContactInfo(1, "person", "Person", listOf("123"), contactMethods = listOf(method))
        assertEquals(method, ContactButtonResolver.findMethod(contact, ContactButtonAction.WHATSAPP_BUSINESS_MESSAGE))
        val action = ContactButtonResolver.defaultCardAction(contact, ContactButtonAction.WHATSAPP_BUSINESS_MESSAGE, false)
            as ContactCardAction.CustomApp
        assertEquals(method.dataId, action.dataId)
        assertEquals(method.packageName, action.packageName)
    }
}
