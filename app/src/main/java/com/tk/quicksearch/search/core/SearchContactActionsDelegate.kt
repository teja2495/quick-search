package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SearchContactActionsDelegate(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val contactPreferences: com.tk.quicksearch.search.data.preferences.ContactPreferences,
    private val contactRepository: ContactRepository,
    private val contactActionHandler: ContactActionHandler,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val directSearchActiveProvider: () -> Boolean,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    private val showToastRes: (Int) -> Unit,
    private val setDirectDialEnabled: (Boolean, Boolean) -> Unit,
    private val handleOptionalPermissionChange: () -> Unit,
) {
    fun getPrimaryContactCardAction(contactId: Long): ContactCardAction? =
        contactPreferences.getPrimaryContactCardAction(contactId)

    fun setPrimaryContactCardAction(
        contactId: Long,
        action: ContactCardAction,
    ) {
        contactPreferences.setPrimaryContactCardAction(contactId, action)
        updateResultsState { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun getSecondaryContactCardAction(contactId: Long): ContactCardAction? =
        contactPreferences.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(
        contactId: Long,
        action: ContactCardAction,
    ) {
        contactPreferences.setSecondaryContactCardAction(contactId, action)
        updateResultsState { it.copy(contactActionsVersion = it.contactActionsVersion + 1) }
    }

    fun requestContactActionPicker(
        contactId: Long,
        isPrimary: Boolean,
        serializedAction: String?,
    ) {
        scope.launch(Dispatchers.IO) {
            val contact =
                contactRepository.getContactsByIds(setOf(contactId)).firstOrNull() ?: return@launch
            val requestedAction = serializedAction?.let { ContactCardAction.fromSerializedString(it) }
            val resolvedAction =
                requestedAction ?: if (isPrimary) {
                    getPrimaryContactCardAction(contactId)
                } else {
                    getSecondaryContactCardAction(contactId)
                        ?: getDefaultContactCardAction(contact, isPrimary)
                }
            updateConfigState {
                it.copy(
                    contactActionPickerRequest =
                        ContactActionPickerRequest(contact, isPrimary, resolvedAction),
                )
            }
        }
    }

    fun clearContactActionPickerRequest() {
        updateConfigState { it.copy(contactActionPickerRequest = null) }
    }

    fun onCustomAction(
        contactInfo: ContactInfo,
        action: ContactCardAction,
    ) {
        val trackHistory = directSearchActiveProvider().not()

        when (action) {
            is ContactCardAction.Phone -> {
                contactActionHandler.callContact(contactInfo, trackHistory = trackHistory)
                return
            }
            is ContactCardAction.Sms -> {
                contactActionHandler.smsContact(contactInfo, trackHistory = trackHistory)
                return
            }
            else -> Unit
        }

        scope.launch(Dispatchers.IO) {
            val contact = contactRepository.getContactsByIds(setOf(contactInfo.contactId)).firstOrNull()
            val methods = contact?.contactMethods ?: emptyList()

            fun matchesPhoneNumber(method: ContactMethod): Boolean {
                if (method.data.isBlank()) return false
                return PhoneNumberUtils.isSameNumber(method.data, action.phoneNumber)
            }

            fun matchesTelegramNumber(method: ContactMethod): Boolean =
                TelegramContactUtils.isTelegramMethodForPhoneNumber(
                    context = appContext,
                    phoneNumber = action.phoneNumber,
                    telegramMethod = method,
                ) || matchesPhoneNumber(method)

            fun matchesSignalNumber(method: ContactMethod): Boolean =
                method.data.isBlank() || matchesPhoneNumber(method)

            val matchedMethod: ContactMethod? =
                methods.find { method ->
                    when (action) {
                        is ContactCardAction.Phone ->
                            method is ContactMethod.Phone && matchesPhoneNumber(method)
                        is ContactCardAction.Sms ->
                            method is ContactMethod.Sms && matchesPhoneNumber(method)
                        is ContactCardAction.WhatsAppCall ->
                            method is ContactMethod.WhatsAppCall && matchesPhoneNumber(method)
                        is ContactCardAction.WhatsAppMessage ->
                            method is ContactMethod.WhatsAppMessage && matchesPhoneNumber(method)
                        is ContactCardAction.WhatsAppVideoCall ->
                            method is ContactMethod.WhatsAppVideoCall && matchesPhoneNumber(method)
                        is ContactCardAction.TelegramMessage ->
                            method is ContactMethod.TelegramMessage && matchesTelegramNumber(method)
                        is ContactCardAction.TelegramCall ->
                            method is ContactMethod.TelegramCall && matchesTelegramNumber(method)
                        is ContactCardAction.TelegramVideoCall ->
                            method is ContactMethod.TelegramVideoCall && matchesTelegramNumber(method)
                        is ContactCardAction.SignalMessage ->
                            method is ContactMethod.SignalMessage && matchesSignalNumber(method)
                        is ContactCardAction.SignalCall ->
                            method is ContactMethod.SignalCall && matchesSignalNumber(method)
                        is ContactCardAction.SignalVideoCall ->
                            method is ContactMethod.SignalVideoCall && matchesSignalNumber(method)
                        is ContactCardAction.GoogleMeet ->
                            method is ContactMethod.GoogleMeet && matchesPhoneNumber(method)
                        is ContactCardAction.Email ->
                            method is ContactMethod.Email &&
                                (method.data == action.phoneNumber || matchesPhoneNumber(method))
                        is ContactCardAction.VideoCall ->
                            method is ContactMethod.VideoCall &&
                                method.packageName == action.packageName &&
                                (method.data == action.phoneNumber || matchesPhoneNumber(method))
                        is ContactCardAction.CustomApp ->
                            method is ContactMethod.CustomApp &&
                                (
                                    (action.dataId != null && method.dataId == action.dataId) ||
                                        (
                                            method.mimeType == action.mimeType &&
                                                method.packageName == action.packageName &&
                                                (
                                                    method.data == action.phoneNumber ||
                                                        matchesPhoneNumber(method)
                                                )
                                        )
                                    )
                        is ContactCardAction.ViewInContactsApp ->
                            method is ContactMethod.ViewInContactsApp
                    }
                }

            withContext(Dispatchers.Main) {
                if (matchedMethod != null) {
                    contactActionHandler.handleContactMethod(
                        contactInfo,
                        matchedMethod,
                        trackHistory = trackHistory,
                    )
                } else {
                    when (action) {
                        is ContactCardAction.Phone -> {
                            contactActionHandler.handleContactMethod(
                                contactInfo,
                                ContactMethod.Phone(
                                    appContext.getString(R.string.contact_method_call_label),
                                    action.phoneNumber,
                                ),
                                trackHistory = trackHistory,
                            )
                        }
                        is ContactCardAction.Sms -> {
                            contactActionHandler.handleContactMethod(
                                contactInfo,
                                ContactMethod.Sms(
                                    appContext.getString(R.string.contact_method_message_label),
                                    action.phoneNumber,
                                ),
                                trackHistory = trackHistory,
                            )
                        }
                        is ContactCardAction.Email -> {
                            contactActionHandler.handleContactMethod(
                                contactInfo,
                                ContactMethod.Email(
                                    displayLabel = appContext.getString(R.string.contact_method_email_label),
                                    data = action.phoneNumber,
                                ),
                                trackHistory = trackHistory,
                            )
                        }
                        is ContactCardAction.ViewInContactsApp -> {
                            contactActionHandler.handleContactMethod(
                                contactInfo,
                                ContactMethod.ViewInContactsApp(
                                    displayLabel = appContext.getString(R.string.contacts_action_button_contacts),
                                ),
                                trackHistory = trackHistory,
                            )
                        }
                        is ContactCardAction.VideoCall -> {
                            contactActionHandler.handleContactMethod(
                                contactInfo,
                                ContactMethod.VideoCall(
                                    displayLabel = appContext.getString(R.string.contacts_action_button_video_call),
                                    data = action.phoneNumber,
                                    packageName = action.packageName,
                                ),
                                trackHistory = trackHistory,
                            )
                        }
                        is ContactCardAction.CustomApp -> {
                            contactActionHandler.handleContactMethod(
                                contactInfo,
                                ContactMethod.CustomApp(
                                    displayLabel = action.displayLabel,
                                    data = action.phoneNumber,
                                    mimeType = action.mimeType,
                                    packageName = action.packageName,
                                    dataId = action.dataId,
                                ),
                                trackHistory = trackHistory,
                            )
                        }
                        else -> {
                            showToastRes(R.string.error_action_not_available)
                        }
                    }
                }
            }
        }
    }

    fun callContact(contactInfo: ContactInfo) {
        contactActionHandler.callContact(contactInfo, trackHistory = directSearchActiveProvider().not())
    }

    fun smsContact(contactInfo: ContactInfo) {
        contactActionHandler.smsContact(contactInfo, trackHistory = directSearchActiveProvider().not())
    }

    fun handleContactMethod(
        contactInfo: ContactInfo,
        method: ContactMethod,
    ) {
        contactActionHandler.handleContactMethod(
            contactInfo,
            method,
            trackHistory = directSearchActiveProvider().not(),
        )
    }

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) {
        updateConfigState { it.copy(contactMethodsBottomSheet = contactInfo) }
    }

    fun dismissContactMethodsBottomSheet() {
        updateConfigState { it.copy(contactMethodsBottomSheet = null) }
    }

    fun onDirectDialChoiceSelected(
        option: DirectDialOption,
        rememberChoice: Boolean,
    ) {
        contactActionHandler.onDirectDialChoiceSelected(option, rememberChoice)
        val useDirectDial = option == DirectDialOption.DIRECT_CALL
        if (rememberChoice) {
            setDirectDialEnabled(useDirectDial, true)
        } else {
            userPreferences.setHasSeenDirectDialChoice(true)
        }
    }

    fun onCallPermissionResult(
        isGranted: Boolean,
        shouldShowPermissionError: Boolean = true,
    ) {
        contactActionHandler.onCallPermissionResult(
            isGranted = isGranted,
            shouldShowPermissionError = shouldShowPermissionError,
        )
        handleOptionalPermissionChange()
    }

    fun getLastShownPhoneNumber(contactId: Long): String? =
        userPreferences.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(
        contactId: Long,
        phoneNumber: String,
    ) {
        userPreferences.setLastShownPhoneNumber(contactId, phoneNumber)
    }

    private fun getDefaultContactCardAction(
        contact: ContactInfo,
        isPrimary: Boolean,
    ): ContactCardAction? {
        val phoneNumber = contact.phoneNumbers.firstOrNull() ?: return null
        return if (isPrimary) {
            when (
                ContactCallingAppResolver.resolveCallingAppForContact(
                    contactInfo = contact,
                    defaultApp = permissionStateProvider().callingApp,
                )
            ) {
                CallingApp.CALL -> ContactCardAction.Phone(phoneNumber)
                CallingApp.WHATSAPP -> ContactCardAction.WhatsAppCall(phoneNumber)
                CallingApp.TELEGRAM -> ContactCardAction.TelegramCall(phoneNumber)
                CallingApp.SIGNAL -> ContactCardAction.SignalCall(phoneNumber)
                CallingApp.GOOGLE_MEET -> ContactCardAction.GoogleMeet(phoneNumber)
            }
        } else {
            when (
                ContactMessagingAppResolver.resolveMessagingAppForContact(
                    contactInfo = contact,
                    defaultApp = permissionStateProvider().messagingApp,
                )
            ) {
                MessagingApp.MESSAGES -> ContactCardAction.Sms(phoneNumber)
                MessagingApp.WHATSAPP -> ContactCardAction.WhatsAppMessage(phoneNumber)
                MessagingApp.TELEGRAM -> ContactCardAction.TelegramMessage(phoneNumber)
                MessagingApp.SIGNAL -> ContactCardAction.SignalMessage(phoneNumber)
            }
        }
    }
}
