package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.DirectDialChoice
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.PendingThirdPartyCall
import com.tk.quicksearch.search.core.PhoneNumberSelection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.shared.permissions.PermissionHelper

/**
 * Handles contact action coordination logic.
 * Requires dependencies to be passed in from the ViewModel.
 */
class ContactActionHandler(
    private val context: Application,
    private val userPreferences: UserAppPreferences,
    private val getCallingApp: () -> CallingApp,
    private val getMessagingApp: () -> MessagingApp,
    private val getDirectDialEnabled: () -> Boolean,
    private val getHasSeenDirectDialChoice: () -> Boolean,
    private val getCurrentState: () -> SearchUiState,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
    private val clearQuery: () -> Unit,
    private val showToastCallback: (Int) -> Unit,
) {
    fun callContact(contactInfo: ContactInfo, trackHistory: Boolean = true) {
        if (contactInfo.phoneNumbers.isEmpty()) {
            showToastCallback(R.string.error_missing_phone_number)
            return
        }
        if (trackHistory) trackRecentContactAction(contactInfo)

        // Check if there's a preferred number stored
        val preferredNumber = userPreferences.getPreferredPhoneNumber(contactInfo.contactId)
        if (preferredNumber != null && contactInfo.phoneNumbers.contains(preferredNumber)) {
            // Use preferred number directly
            performCalling(contactInfo, preferredNumber)
            return
        }

        // If multiple numbers, show selection dialog
        if (contactInfo.phoneNumbers.size > 1) {
            uiStateUpdater { it.copy(phoneNumberSelection = PhoneNumberSelection(contactInfo, isCall = true)) }
            return
        }

        // Single number, use it directly
        performCalling(contactInfo, contactInfo.phoneNumbers.first())
    }

    fun smsContact(contactInfo: ContactInfo, trackHistory: Boolean = true) {
        if (contactInfo.phoneNumbers.isEmpty()) {
            showToastCallback(R.string.error_missing_phone_number)
            return
        }
        if (trackHistory) trackRecentContactAction(contactInfo)

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

    fun onPhoneNumberSelected(
        phoneNumber: String,
        rememberChoice: Boolean,
    ) {
        val selection = getCurrentState().phoneNumberSelection ?: return
        val contactInfo = selection.contactInfo

        // Store preference if requested
        if (rememberChoice) {
            userPreferences.setPreferredPhoneNumber(contactInfo.contactId, phoneNumber)
        }

        // Perform the action
        if (selection.isCall) {
            performCalling(contactInfo, phoneNumber)
        } else {
            performMessaging(contactInfo, phoneNumber)
        }

        // Clear the selection dialog
        uiStateUpdater { it.copy(phoneNumberSelection = null) }
    }

    fun dismissPhoneNumberSelection() {
        uiStateUpdater { it.copy(phoneNumberSelection = null) }
    }

    private fun trackRecentContactAction(contactInfo: ContactInfo) {
        userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
    }

    private fun beginRegularCallFlow(
        contactName: String,
        phoneNumber: String,
    ) {
        if (!getHasSeenDirectDialChoice()) {
            uiStateUpdater {
                it.copy(
                    directDialChoice =
                        DirectDialChoice(
                            contactName = contactName,
                            phoneNumber = phoneNumber,
                        ),
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
        val hasPermission = PermissionHelper.checkCallPermission(context)
        if (hasPermission) {
            performDirectCall(phoneNumber)
        } else {
            uiStateUpdater { it.copy(pendingDirectCallNumber = phoneNumber) }
        }
    }

    fun onDirectDialChoiceSelected(
        option: DirectDialOption,
        rememberChoice: Boolean,
    ) {
        val choice = getCurrentState().directDialChoice ?: return
        val useDirectDial = option == DirectDialOption.DIRECT_CALL

        uiStateUpdater {
            it.copy(
                directDialChoice = null,
                directDialEnabled = useDirectDial,
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

    fun onCallPermissionResult(
        isGranted: Boolean,
        shouldShowPermissionError: Boolean = true,
    ) {
        val state = getCurrentState()
        val pendingNumber = state.pendingDirectCallNumber
        val pendingThirdPartyCall = state.pendingThirdPartyCall

        uiStateUpdater {
            it.copy(
                pendingDirectCallNumber = null,
                pendingThirdPartyCall = null,
            )
        }

        if (isGranted) {
            // Handle direct call if present
            if (pendingNumber != null) {
                performDirectCall(pendingNumber)
            }
            // Handle third-party call if present
            if (pendingThirdPartyCall != null) {
                executePendingThirdPartyCall(pendingThirdPartyCall)
            }
        } else {
            // If permission is denied for direct call, fall back to the dialer
            if (pendingNumber != null) {
                openDialer(pendingNumber)
            }
            // If permission is denied for third-party call, show guidance toast.
            if (pendingThirdPartyCall != null && shouldShowPermissionError) {
                showToastCallback(R.string.error_call_permission_required)
            }
        }
    }

    private fun clearQueryIfEnabled() {
        // Always clear query after contact action
        clearQuery()
    }

    fun handleContactMethod(
        contactInfo: ContactInfo,
        method: ContactMethod,
        trackHistory: Boolean = true,
    ) {
        if (trackHistory) trackRecentContactAction(contactInfo)
        when (method) {
            is ContactMethod.Phone -> {
                val phoneNumber =
                    method.data.takeIf { it.isNotBlank() }
                        ?: contactInfo.primaryNumber
                        ?: contactInfo.phoneNumbers.firstOrNull()
                if (phoneNumber.isNullOrBlank()) {
                    showToastCallback(R.string.error_missing_phone_number)
                    return
                }
                beginRegularCallFlow(contactInfo.displayName, phoneNumber)
            }

            is ContactMethod.Sms -> {
                ContactIntentHelpers.performSms(context, method.data)
                clearQueryIfEnabled()
            }

            is ContactMethod.Email -> {
                val success = ContactIntentHelpers.composeEmail(context, method.data) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.WhatsAppCall -> {
                handleWhatsAppCallWithPermission(method.dataId)
            }

            is ContactMethod.WhatsAppMessage -> {
                val success = ContactIntentHelpers.openWhatsAppChat(context, method.dataId) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.WhatsAppVideoCall -> {
                handleWhatsAppVideoCallWithPermission(method.dataId)
            }

            is ContactMethod.TelegramMessage -> {
                val success = ContactIntentHelpers.openTelegramChat(context, method.dataId) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.TelegramCall -> {
                handleTelegramCallWithPermission(method.dataId)
            }

            is ContactMethod.TelegramVideoCall -> {
                handleTelegramVideoCallWithPermission(method.dataId, method.data)
            }

            is ContactMethod.SignalMessage -> {
                val success = ContactIntentHelpers.openSignalChat(context, method.dataId) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.SignalCall -> {
                handleSignalCallWithPermission(method.dataId)
            }

            is ContactMethod.SignalVideoCall -> {
                handleSignalVideoCallWithPermission(method.dataId)
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
                    val success =
                        ContactIntentHelpers.openCustomAppWithDataId(
                            context,
                            method.dataId,
                            method.mimeType,
                            method.packageName,
                        ) { resId -> showToastCallback(resId) }
                    if (success) {
                        clearQueryIfEnabled()
                    }
                } else {
                    ContactIntentHelpers.openCustomApp(
                        context,
                        method.data,
                        method.mimeType,
                        method.packageName,
                    ) { resId -> showToastCallback(resId) }
                    clearQueryIfEnabled()
                }
            }

            is ContactMethod.ViewInContactsApp -> {
                val success = ContactIntentHelpers.openContact(context, contactInfo) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
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

    private fun performMessaging(
        contactInfo: ContactInfo,
        number: String,
    ) {
        when (
            ContactMessagingAppResolver.resolveMessagingAppForContact(
                contactInfo,
                getMessagingApp(),
            )
        ) {
            MessagingApp.MESSAGES -> performSms(number)
            MessagingApp.WHATSAPP -> ContactIntentHelpers.openWhatsAppChat(context, number) { resId -> showToastCallback(resId) }
            MessagingApp.TELEGRAM -> ContactIntentHelpers.openTelegramChat(context, number) { resId -> showToastCallback(resId) }
            MessagingApp.SIGNAL -> ContactIntentHelpers.openSignalChat(context, number) { resId -> showToastCallback(resId) }
        }
    }

    private fun performCalling(
        contactInfo: ContactInfo,
        number: String,
    ) {
        when (
            ContactCallingAppResolver.resolveCallingAppForContact(
                contactInfo = contactInfo,
                defaultApp = getCallingApp(),
                phoneNumber = number,
            )
        ) {
            CallingApp.CALL -> beginRegularCallFlow(contactInfo.displayName, number)
            CallingApp.WHATSAPP -> {
                val method =
                    contactInfo.contactMethods.firstOrNull { it is ContactMethod.WhatsAppCall && it.dataId != null && (it.data.isBlank() || com.tk.quicksearch.search.utils.PhoneNumberUtils.isSameNumber(it.data, number)) } as? ContactMethod.WhatsAppCall
                if (method?.dataId != null) {
                    handleWhatsAppCallWithPermission(method.dataId)
                } else {
                    beginRegularCallFlow(contactInfo.displayName, number)
                }
            }
            CallingApp.TELEGRAM -> {
                val preferredMethod =
                    contactInfo.contactMethods
                        .firstOrNull { it is ContactMethod.TelegramCall && it.dataId != null } as? ContactMethod.TelegramCall
                if (preferredMethod?.dataId != null) {
                    handleTelegramCallWithPermission(preferredMethod.dataId)
                } else {
                    handleTelegramCallWithPermission(phoneNumber = number)
                }
            }
            CallingApp.SIGNAL -> {
                val method =
                    contactInfo.contactMethods.firstOrNull { it is ContactMethod.SignalCall && it.dataId != null && (it.data.isBlank() || com.tk.quicksearch.search.utils.PhoneNumberUtils.isSameNumber(it.data, number)) } as? ContactMethod.SignalCall
                if (method?.dataId != null) {
                    handleSignalCallWithPermission(method.dataId)
                } else {
                    beginRegularCallFlow(contactInfo.displayName, number)
                }
            }
            CallingApp.GOOGLE_MEET -> {
                val method =
                    contactInfo.contactMethods.firstOrNull { it is ContactMethod.GoogleMeet && it.dataId != null && (it.data.isBlank() || com.tk.quicksearch.search.utils.PhoneNumberUtils.isSameNumber(it.data, number)) } as? ContactMethod.GoogleMeet
                if (method?.dataId != null) {
                    val success = ContactIntentHelpers.openGoogleMeet(context, method.dataId) { resId -> showToastCallback(resId) }
                    if (success) {
                        clearQueryIfEnabled()
                    }
                } else {
                    beginRegularCallFlow(contactInfo.displayName, number)
                }
            }
        }
    }

    /**
     * Handles WhatsApp call with permission checking.
     * WhatsApp calls require CALL_PHONE permission.
     */
    private fun handleWhatsAppCallWithPermission(dataId: Long?) {
        val hasPermission = PermissionHelper.checkCallPermission(context)
        if (hasPermission) {
            val success = ContactIntentHelpers.openWhatsAppCall(context, dataId) { resId -> showToastCallback(resId) }
            if (success) {
                clearQueryIfEnabled()
            }
        } else {
            queuePendingThirdPartyCall(
                PendingThirdPartyCall(
                    app = CallingApp.WHATSAPP,
                    dataId = dataId,
                ),
            )
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

        val hasPermission = PermissionHelper.checkCallPermission(context)
        if (hasPermission) {
            val success = ContactIntentHelpers.openWhatsAppVideoCall(context, dataId) { resId -> showToastCallback(resId) }
            if (success) {
                clearQueryIfEnabled()
            }
        } else {
            queuePendingThirdPartyCall(
                PendingThirdPartyCall(
                    app = CallingApp.WHATSAPP,
                    dataId = dataId,
                    isVideoCall = true,
                ),
            )
        }
    }

    private fun handleTelegramCallWithPermission(
        dataId: Long? = null,
        phoneNumber: String? = null,
    ) {
        if (PermissionHelper.checkCallPermission(context)) {
            if (dataId != null) {
                val success = ContactIntentHelpers.openTelegramCall(context, dataId) { resId -> showToastCallback(resId) }
                if (success) {
                    clearQueryIfEnabled()
                }
            } else if (!phoneNumber.isNullOrBlank()) {
                ContactIntentHelpers.openTelegramCall(context, phoneNumber) { resId -> showToastCallback(resId) }
                clearQueryIfEnabled()
            }
            return
        }

        queuePendingThirdPartyCall(
            PendingThirdPartyCall(
                app = CallingApp.TELEGRAM,
                dataId = dataId,
                phoneNumber = phoneNumber,
            ),
        )
    }

    private fun handleTelegramVideoCallWithPermission(
        dataId: Long?,
        phoneNumber: String?,
    ) {
        if (PermissionHelper.checkCallPermission(context)) {
            val success = ContactIntentHelpers.openTelegramVideoCall(context, dataId, phoneNumber)
            if (success) {
                clearQueryIfEnabled()
            }
            return
        }

        queuePendingThirdPartyCall(
            PendingThirdPartyCall(
                app = CallingApp.TELEGRAM,
                dataId = dataId,
                phoneNumber = phoneNumber,
                isVideoCall = true,
            ),
        )
    }

    private fun handleSignalCallWithPermission(dataId: Long?) {
        if (PermissionHelper.checkCallPermission(context)) {
            val success = ContactIntentHelpers.openSignalCall(context, dataId) { resId -> showToastCallback(resId) }
            if (success) {
                clearQueryIfEnabled()
            }
            return
        }

        queuePendingThirdPartyCall(
            PendingThirdPartyCall(
                app = CallingApp.SIGNAL,
                dataId = dataId,
            ),
        )
    }

    private fun handleSignalVideoCallWithPermission(dataId: Long?) {
        if (PermissionHelper.checkCallPermission(context)) {
            val success = ContactIntentHelpers.openSignalVideoCall(context, dataId) { resId -> showToastCallback(resId) }
            if (success) {
                clearQueryIfEnabled()
            }
            return
        }

        queuePendingThirdPartyCall(
            PendingThirdPartyCall(
                app = CallingApp.SIGNAL,
                dataId = dataId,
                isVideoCall = true,
            ),
        )
    }

    private fun queuePendingThirdPartyCall(pendingCall: PendingThirdPartyCall) {
        uiStateUpdater { it.copy(pendingThirdPartyCall = pendingCall) }
    }

    private fun executePendingThirdPartyCall(pendingCall: PendingThirdPartyCall) {
        when (pendingCall.app) {
            CallingApp.WHATSAPP -> {
                val success =
                    if (pendingCall.isVideoCall) {
                        ContactIntentHelpers.openWhatsAppVideoCall(context, pendingCall.dataId) { resId -> showToastCallback(resId) }
                    } else {
                        ContactIntentHelpers.openWhatsAppCall(context, pendingCall.dataId) { resId -> showToastCallback(resId) }
                    }
                if (success) {
                    clearQueryIfEnabled()
                }
            }
            CallingApp.TELEGRAM -> {
                val success =
                    if (pendingCall.isVideoCall) {
                        ContactIntentHelpers.openTelegramVideoCall(context, pendingCall.dataId, pendingCall.phoneNumber)
                    } else {
                        pendingCall.dataId?.let { dataId ->
                            ContactIntentHelpers.openTelegramCall(context, dataId) { resId -> showToastCallback(resId) }
                        } ?: run {
                            pendingCall.phoneNumber?.let { number ->
                                ContactIntentHelpers.openTelegramCall(context, number) { resId -> showToastCallback(resId) }
                                true
                            } ?: false
                        }
                    }
                if (success) {
                    clearQueryIfEnabled()
                }
            }
            CallingApp.SIGNAL -> {
                val success =
                    if (pendingCall.isVideoCall) {
                        ContactIntentHelpers.openSignalVideoCall(context, pendingCall.dataId) { resId -> showToastCallback(resId) }
                    } else {
                        ContactIntentHelpers.openSignalCall(context, pendingCall.dataId) { resId -> showToastCallback(resId) }
                    }
                if (success) {
                    clearQueryIfEnabled()
                }
            }
            else -> Unit
        }
    }
}
