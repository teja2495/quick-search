package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import kotlin.reflect.KClass

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Reorders phone numbers to prioritize the last shown number for better UX.
 * Only applies when there are multiple phone numbers.
 */
internal fun reorderPhoneNumbersForDisplay(
    contactInfo: ContactInfo,
    hasMultipleNumbers: Boolean,
    getLastShownPhoneNumber: (Long) -> String?,
): List<String> {
    if (!hasMultipleNumbers) {
        return contactInfo.phoneNumbers
    }

    val lastShownNumber = getLastShownPhoneNumber(contactInfo.contactId)
    if (lastShownNumber == null || contactInfo.phoneNumbers.isEmpty()) {
        return contactInfo.phoneNumbers
    }

    // Find the index of the last shown number (using phone number matching)
    val lastShownIndex =
        contactInfo.phoneNumbers.indexOfFirst { number ->
            PhoneNumberUtils.isSameNumber(number, lastShownNumber)
        }

    return if (lastShownIndex >= 0) {
        // Move the last shown number to the front
        val reordered = contactInfo.phoneNumbers.toMutableList()
        val lastShown = reordered.removeAt(lastShownIndex)
        reordered.add(0, lastShown)
        reordered
    } else {
        contactInfo.phoneNumbers
    }
}

/**
 * Filters contact methods to only include those that match the selected phone number.
 * Telegram methods use special utility functions for matching, while other methods
 * use phone number normalization for comparison.
 */
internal fun filterMethodsByPhoneNumber(
    contactMethods: List<ContactMethod>,
    selectedPhoneNumber: String?,
    context: android.content.Context,
): List<ContactMethod> =
    contactMethods.filter { method ->
        when {
            // Telegram methods require special handling with utility functions
            method is ContactMethod.TelegramMessage ||
                method is ContactMethod.TelegramCall ||
                method is ContactMethod.TelegramVideoCall -> {
                if (selectedPhoneNumber != null) {
                    TelegramContactUtils.isTelegramMethodForPhoneNumber(
                        context = context,
                        phoneNumber = selectedPhoneNumber,
                        telegramMethod = method,
                    )
                } else {
                    // If no phone number is selected, show all Telegram methods
                    true
                }
            }

            method is ContactMethod.SignalMessage ||
                method is ContactMethod.SignalCall ||
                method is ContactMethod.SignalVideoCall -> {
                if (selectedPhoneNumber == null) {
                    true
                } else {
                    method.data.isBlank() || PhoneNumberUtils.isSameNumber(method.data, selectedPhoneNumber)
                }
            }

            // For other methods, require phone number match with the selected number
            else -> {
                val methodData = method.data?.takeIf { it.isNotBlank() }
                methodData != null && selectedPhoneNumber != null &&
                    PhoneNumberUtils.isSameNumber(methodData, selectedPhoneNumber)
            }
        }
    }

/**
 * Renders a row of contact methods if any methods of the specified types are available.
 */
@Composable
internal inline fun renderMethodRow(
    methods: List<ContactMethod>,
    methodTypes: List<KClass<out ContactMethod>>,
    crossinline onMethodClick: (ContactMethod) -> Unit,
    noinline onMethodLongClick: ((ContactMethod) -> Unit)? = null,
) {
    val filteredMethods =
        methods.filter { method ->
            methodTypes.any { type -> type.isInstance(method) }
        }

    if (filteredMethods.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            filteredMethods.forEach { method ->
                ContactActionButton(
                    method = method,
                    onClick = { onMethodClick(method) },
                    onLongClick = onMethodLongClick?.let { { it(method) } },
                )
            }
        }
    }
}

internal fun contactMethodToCardAction(
    method: ContactMethod,
    selectedPhoneNumber: String?,
): ContactCardAction? {
    val phoneNumber = selectedPhoneNumber ?: method.data.takeIf { it.isNotBlank() } ?: return null
    return when (method) {
        is ContactMethod.Phone -> ContactCardAction.Phone(phoneNumber)
        is ContactMethod.Sms -> ContactCardAction.Sms(phoneNumber)
        is ContactMethod.WhatsAppCall -> ContactCardAction.WhatsAppCall(phoneNumber)
        is ContactMethod.WhatsAppMessage -> ContactCardAction.WhatsAppMessage(phoneNumber)
        is ContactMethod.WhatsAppVideoCall -> ContactCardAction.WhatsAppVideoCall(phoneNumber)
        is ContactMethod.TelegramMessage -> ContactCardAction.TelegramMessage(phoneNumber)
        is ContactMethod.TelegramCall -> ContactCardAction.TelegramCall(phoneNumber)
        is ContactMethod.TelegramVideoCall -> ContactCardAction.TelegramVideoCall(phoneNumber)
        is ContactMethod.SignalMessage -> ContactCardAction.SignalMessage(phoneNumber)
        is ContactMethod.SignalCall -> ContactCardAction.SignalCall(phoneNumber)
        is ContactMethod.SignalVideoCall -> ContactCardAction.SignalVideoCall(phoneNumber)
        is ContactMethod.GoogleMeet -> ContactCardAction.GoogleMeet(phoneNumber)
        else -> null
    }
}

internal fun methodShortcutLabel(
    context: android.content.Context,
    method: ContactMethod,
): String? =
    when (method) {
        is ContactMethod.Phone -> context.getString(R.string.contacts_action_button_call)
        is ContactMethod.Sms -> context.getString(R.string.contacts_action_button_message)
        is ContactMethod.WhatsAppCall,
        is ContactMethod.TelegramCall,
        is ContactMethod.SignalCall,
        -> context.getString(R.string.contacts_action_button_voice_call)
        is ContactMethod.WhatsAppMessage,
        is ContactMethod.TelegramMessage,
        is ContactMethod.SignalMessage,
        -> context.getString(R.string.contacts_action_button_chat)
        is ContactMethod.WhatsAppVideoCall,
        is ContactMethod.TelegramVideoCall,
        is ContactMethod.SignalVideoCall,
        -> context.getString(R.string.contacts_action_button_video_call)
        is ContactMethod.GoogleMeet -> context.getString(R.string.contacts_action_button_meet)
        else -> null
    }