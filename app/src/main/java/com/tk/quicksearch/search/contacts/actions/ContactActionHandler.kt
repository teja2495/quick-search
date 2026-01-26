package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.DirectDialChoice
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.PhoneNumberSelection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.recentSearches.RecentSearchEntry

/**
 * Handles contact action coordination logic.
 * Requires dependencies to be passed in from the ViewModel.
 */
class ContactActionHandler(
    private val context: Application,
    private val userPreferences: UserAppPreferences,
    private val getMessagingApp: () -> MessagingApp,
    private val getDirectDialEnabled: () -> Boolean,
    private val getHasSeenDirectDialChoice: () -> Boolean,
    private val getCurrentState: () -> SearchUiState,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val clearQuery: () -> Unit,
    private val showToastCallback: (Int) -> Unit
) {

    fun callContact(contactInfo: ContactInfo) {
        if (contactInfo.phoneNumbers.isEmpty()) {
            showToastCallback(R.string.error_missing_phone_number)
            return
        }
        trackRecentContactAction(contactInfo)

        // Check if there's a preferred number stored
        val preferredNumber = userPreferences.getPreferredPhoneNumber(contactInfo.contactId)
        if (preferredNumber != null && contactInfo.phoneNumbers.contains(preferredNumber)) {
            // Use preferred number directly
            beginCallFlow(contactInfo.displayName, preferredNumber)
            return
        }

        // If multiple numbers, show selection dialog
        if (contactInfo.phoneNumbers.size > 1) {
            uiStateUpdater { it.copy(phoneNumberSelection = PhoneNumberSelection(contactInfo, isCall = true)) }
            return
        }

        // Single number, use it directly
        beginCallFlow(contactInfo.displayName, contactInfo.phoneNumbers.first())
    }

    fun smsContact(contactInfo: ContactInfo) {
        if (contactInfo.phoneNumbers.isEmpty()) {
            showToastCallback(R.string.error_missing_phone_number)
            return
        }
        trackRecentContactAction(contactInfo)

        // Check if there's a preferred number stored
        val preferredNumber = userPreferences.getPreferredPhoneNumber(contactInfo.contactId)
        if (preferredNumber != null && contactInfo.phoneNumbers.contains(preferredNumber)) {
            // Use preferred number directly
            performMessaging(contactInfo, preferredNumber)
            return
        }

        // If multiple numbers, show selection dialog
        if (contactInfo.phoneNumbers.size > 1) {
            uiStateUpdater { it.copy(phoneNumberSelection = PhoneNumberSelection(contactInfo, isCall = false)) }
            return
        }

        // Single number, use it directly
            performMessaging(contactInfo, contactInfo.phoneNumbers.first())
    }

    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) {
        val selection = getCurrentState().phoneNumberSelection ?: return
        val contactInfo = selection.contactInfo

        // Store preference if requested
        if (rememberChoice) {
            userPreferences.setPreferredPhoneNumber(contactInfo.contactId, phoneNumber)
        }

        // Perform the action
        if (selection.isCall) {
            beginCallFlow(contactInfo.displayName, phoneNumber)
        } else {
            performMessaging(contactInfo, phoneNumber)
        }

        // Clear the selection dialog
        uiStateUpdater { it.copy(phoneNumberSelection = null) }
    }

    fun dismissPhoneNumberSelection() {
        uiStateUpdater { it.copy(phoneNumberSelection = null) }
    }

    private fun beginCallFlow(contactName: String, phoneNumber: String) {
        if (!getHasSeenDirectDialChoice()) {
            uiStateUpdater {
                it.copy(
                    directDialChoice = DirectDialChoice(
                        contactName = contactName,
                        phoneNumber = phoneNumber
                    )
                )
            }
            return
        }

        if (getDirectDialEnabled()) {
            startDirectCallFlow(phoneNumber)
        } else {
            openDialer(phoneNumber)
        }
    }

    private fun startDirectCallFlow(phoneNumber: String) {
        val hasPermission = PermissionRequestHandler.checkCallPermission(context)
        if (hasPermission) {
            performDirectCall(phoneNumber)
        } else {
            uiStateUpdater { it.copy(pendingDirectCallNumber = phoneNumber) }
        }
    }

    fun onDirectDialChoiceSelected(option: DirectDialOption, rememberChoice: Boolean) {
        val choice = getCurrentState().directDialChoice ?: return
        val useDirectDial = option == DirectDialOption.DIRECT_CALL

        uiStateUpdater {
            it.copy(
                directDialChoice = null,
                directDialEnabled = useDirectDial
            )
        }

        if (useDirectDial) {
            startDirectCallFlow(choice.phoneNumber)
        } else {
            openDialer(choice.phoneNumber)
        }
    }

    fun dismissDirectDialChoice() {
        uiStateUpdater { it.copy(directDialChoice = null) }
    }

    fun onCallPermissionResult(isGranted: Boolean) {
        val state = getCurrentState()
        val pendingNumber = state.pendingDirectCallNumber
        val pendingWhatsAppCallDataId = state.pendingWhatsAppCallDataId

        uiStateUpdater {
            it.copy(
                pendingDirectCallNumber = null,
                pendingWhatsAppCallDataId = null
            )
        }

        if (isGranted) {
            // Handle direct call if present
            if (pendingNumber != null) {
                performDirectCall(pendingNumber)
            }
            // Handle WhatsApp call if present
            if (pendingWhatsAppCallDataId != null) {
                val dataId = pendingWhatsAppCallDataId.toLongOrNull()
                val success = ContactIntentHelpers.openWhatsAppCall(context, dataId) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }
        } else {
            // If permission is denied for direct call, fall back to the dialer
            if (pendingNumber != null) {
                openDialer(pendingNumber)
            }
            // If permission is denied for WhatsApp call, show error
            if (pendingWhatsAppCallDataId != null) {
                showToastCallback(R.string.error_whatsapp_call_permission)
            }
        }
    }

    private fun clearQueryIfEnabled() {
        // Always clear query after contact action
        clearQuery()
    }

    fun handleContactMethod(contactInfo: ContactInfo, method: ContactMethod) {
        if (method !is ContactMethod.Phone) {
            trackRecentContactAction(contactInfo)
        }
        when (method) {
            is ContactMethod.Phone -> {
                // Use existing phone call flow with dialers/direct dial
                callContact(contactInfo)
            }

            is ContactMethod.Sms -> {
                ContactIntentHelpers.performSms(context, method.data)
                clearQueryIfEnabled()
            }

            is ContactMethod.Email -> {
                ContactIntentHelpers.composeEmail(context, method.data) { resId -> showToastCallback(resId) }
                clearQueryIfEnabled()
            }

            is ContactMethod.WhatsAppCall -> {
                handleWhatsAppCallWithPermission(method.dataId)
            }

            is ContactMethod.WhatsAppMessage -> {
                ContactIntentHelpers.openWhatsAppChat(context, method.dataId) { resId -> showToastCallback(resId) }
                clearQueryIfEnabled()
            }

            is ContactMethod.WhatsAppVideoCall -> {
                handleWhatsAppVideoCallWithPermission(method.dataId)
            }

            is ContactMethod.TelegramMessage -> {
                ContactIntentHelpers.openTelegramChat(context, method.dataId) { resId -> showToastCallback(resId) }
                clearQueryIfEnabled()
            }

            is ContactMethod.TelegramCall -> {
                val success = ContactIntentHelpers.openTelegramCall(context, method.dataId) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.TelegramVideoCall -> {
                val success = ContactIntentHelpers.openTelegramVideoCall(context, method.dataId, method.data)
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.VideoCall -> {
                ContactIntentHelpers.openVideoCall(context, method.data, method.packageName) { resId -> showToastCallback(resId) }
                clearQueryIfEnabled()
            }

            is ContactMethod.GoogleMeet -> {
                val success = ContactIntentHelpers.openGoogleMeet(context, method.dataId ?: return) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.CustomApp -> {
                // Try to use dataId approach first, fallback to data approach
                if (method.dataId != null) {
                    val success = ContactIntentHelpers.openCustomAppWithDataId(context, method.dataId, method.mimeType, method.packageName) { resId -> showToastCallback(resId) }
                    if (success) {
                        clearQueryIfEnabled()
                    }
                } else {
                    ContactIntentHelpers.openCustomApp(context, method.data, method.mimeType, method.packageName) { resId -> showToastCallback(resId) }
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.ViewInContactsApp -> {
                ContactIntentHelpers.openContact(context, contactInfo) { resId -> showToastCallback(resId) }
                clearQueryIfEnabled()
            }
        }
    }

    private fun performDirectCall(number: String) {
        ContactIntentHelpers.performDirectCall(context, number)
    }

    private fun openDialer(number: String) {
        ContactIntentHelpers.performDial(context, number)
    }

    private fun performSms(number: String) {
        ContactIntentHelpers.performSms(context, number)
    }

    private fun performMessaging(contactInfo: ContactInfo, number: String) {
        when (
            ContactMessagingAppResolver.resolveMessagingAppForContact(
                contactInfo,
                getMessagingApp()
            )
        ) {
            MessagingApp.MESSAGES -> performSms(number)
            MessagingApp.WHATSAPP -> ContactIntentHelpers.openWhatsAppChat(context, number) { resId -> showToastCallback(resId) }
            MessagingApp.TELEGRAM -> ContactIntentHelpers.openTelegramChat(context, number) { resId -> showToastCallback(resId) }
        }
    }

    private fun trackRecentContactAction(contactInfo: ContactInfo) {
        userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
    }

    /**
     * Handles WhatsApp call with permission checking.
     * WhatsApp calls require CALL_PHONE permission.
     */
    private fun handleWhatsAppCallWithPermission(dataId: Long?) {
        val hasPermission = PermissionRequestHandler.checkCallPermission(context)
        if (hasPermission) {
            val success = ContactIntentHelpers.openWhatsAppCall(context, dataId) { resId -> showToastCallback(resId) }
            if (success) {
                clearQueryIfEnabled()
            }
        } else {
            // Store pending WhatsApp call dataId and request permission
            uiStateUpdater { it.copy(pendingWhatsAppCallDataId = dataId?.toString()) }
        }
    }

    /**
     * Handles WhatsApp video call with permission checking.
     * WhatsApp video calls require CALL_PHONE permission and valid dataId.
     */
    private fun handleWhatsAppVideoCallWithPermission(dataId: Long?) {
        if (dataId == null) {
            Log.w("ContactActionHandler", "WhatsApp video call missing dataId")
            showToastCallback(R.string.error_whatsapp_video_call_missing_data)
            return
        }

        val hasPermission = PermissionRequestHandler.checkCallPermission(context)
        if (hasPermission) {
            val success = ContactIntentHelpers.openWhatsAppVideoCall(context, dataId) { resId -> showToastCallback(resId) }
            if (success) {
                clearQueryIfEnabled()
            }
        } else {
            // Store pending WhatsApp video call dataId and request permission
            uiStateUpdater { it.copy(pendingWhatsAppCallDataId = dataId.toString()) }
        }
    }
}
