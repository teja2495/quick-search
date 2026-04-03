package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod

internal interface SearchViewModelContactActionsApi {
    val contactActionsApiDelegate: SearchViewModelContactActionsApiDelegate

    fun getPrimaryContactCardAction(contactId: Long) =
        contactActionsApiDelegate.getPrimaryContactCardAction(contactId)

    fun setPrimaryContactCardAction(contactId: Long, action: ContactCardAction) =
        contactActionsApiDelegate.setPrimaryContactCardAction(contactId, action)

    fun getSecondaryContactCardAction(contactId: Long) =
        contactActionsApiDelegate.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(contactId: Long, action: ContactCardAction) =
        contactActionsApiDelegate.setSecondaryContactCardAction(contactId, action)

    fun requestContactActionPicker(contactId: Long, isPrimary: Boolean, serializedAction: String?) =
        contactActionsApiDelegate.requestContactActionPicker(contactId, isPrimary, serializedAction)

    fun clearContactActionPickerRequest() = contactActionsApiDelegate.clearContactActionPickerRequest()

    fun onCustomAction(contactInfo: ContactInfo, action: ContactCardAction) =
        contactActionsApiDelegate.onCustomAction(contactInfo, action)

    fun callContact(contactInfo: ContactInfo) = contactActionsApiDelegate.callContact(contactInfo)

    fun smsContact(contactInfo: ContactInfo) = contactActionsApiDelegate.smsContact(contactInfo)

    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) =
        contactActionsApiDelegate.onPhoneNumberSelected(phoneNumber, rememberChoice)

    fun dismissPhoneNumberSelection() = contactActionsApiDelegate.dismissPhoneNumberSelection()

    fun dismissDirectDialChoice() = contactActionsApiDelegate.dismissDirectDialChoice()

    fun handleContactMethod(contactInfo: ContactInfo, method: ContactMethod) =
        contactActionsApiDelegate.handleContactMethod(contactInfo, method)

    fun trackRecentContactTap(contactInfo: ContactInfo) =
        contactActionsApiDelegate.trackRecentContactTap(contactInfo)

    fun trackRecentSettingTap(settingId: String) = contactActionsApiDelegate.trackRecentSettingTap(settingId)

    fun trackRecentAppSettingTap(settingId: String) =
        contactActionsApiDelegate.trackRecentAppSettingTap(settingId)

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) =
        contactActionsApiDelegate.showContactMethodsBottomSheet(contactInfo)

    fun dismissContactMethodsBottomSheet() = contactActionsApiDelegate.dismissContactMethodsBottomSheet()

    fun onDirectDialChoiceSelected(option: DirectDialOption, rememberChoice: Boolean) =
        contactActionsApiDelegate.onDirectDialChoiceSelected(option, rememberChoice)

    fun onCallPermissionResult(isGranted: Boolean, shouldShowPermissionError: Boolean = true) =
        contactActionsApiDelegate.onCallPermissionResult(isGranted, shouldShowPermissionError)

    fun getLastShownPhoneNumber(contactId: Long): String? =
        contactActionsApiDelegate.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(contactId: Long, phoneNumber: String) =
        contactActionsApiDelegate.setLastShownPhoneNumber(contactId, phoneNumber)

    fun onContactActionHintDismissed() = contactActionsApiDelegate.onContactActionHintDismissed()
}

class SearchViewModelContactActionsApiDelegate internal constructor(
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val resultsStateProvider: () -> SearchResultsState,
    private val contactActionsDelegate: SearchContactActionsDelegate,
    private val contactActionHandler: () -> com.tk.quicksearch.search.contacts.actions.ContactActionHandler,
    private val historyDelegate: SearchHistoryDelegate,
    private val legacyPreferenceState: SearchViewModelLegacyPreferenceState,
    private val lockedAliasSearchSectionProvider: () -> SearchSection?,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    fun getPrimaryContactCardAction(contactId: Long) =
        contactActionsDelegate.getPrimaryContactCardAction(contactId)

    fun setPrimaryContactCardAction(contactId: Long, action: ContactCardAction) {
        contactActionsDelegate.setPrimaryContactCardAction(contactId, action)
    }

    fun getSecondaryContactCardAction(contactId: Long) =
        contactActionsDelegate.getSecondaryContactCardAction(contactId)

    fun setSecondaryContactCardAction(contactId: Long, action: ContactCardAction) {
        contactActionsDelegate.setSecondaryContactCardAction(contactId, action)
    }

    fun requestContactActionPicker(contactId: Long, isPrimary: Boolean, serializedAction: String?) {
        contactActionsDelegate.requestContactActionPicker(contactId, isPrimary, serializedAction)
    }

    fun clearContactActionPickerRequest() {
        contactActionsDelegate.clearContactActionPickerRequest()
    }

    fun onCustomAction(contactInfo: ContactInfo, action: ContactCardAction) {
        contactActionsDelegate.onCustomAction(contactInfo, action)
    }

    fun callContact(contactInfo: ContactInfo) = contactActionsDelegate.callContact(contactInfo)

    fun smsContact(contactInfo: ContactInfo) = contactActionsDelegate.smsContact(contactInfo)

    fun onPhoneNumberSelected(phoneNumber: String, rememberChoice: Boolean) =
        contactActionHandler().onPhoneNumberSelected(phoneNumber, rememberChoice)

    fun dismissPhoneNumberSelection() = contactActionHandler().dismissPhoneNumberSelection()

    fun dismissDirectDialChoice() = contactActionHandler().dismissDirectDialChoice()

    fun handleContactMethod(contactInfo: ContactInfo, method: ContactMethod) =
        contactActionsDelegate.handleContactMethod(contactInfo, method)

    fun trackRecentContactTap(contactInfo: ContactInfo) = historyDelegate.trackRecentContactTap(contactInfo)

    fun trackRecentSettingTap(settingId: String) = historyDelegate.trackRecentSettingTap(settingId)

    fun trackRecentAppSettingTap(settingId: String) =
        historyDelegate.trackRecentAppSettingTap(settingId, lockedAliasSearchSectionProvider())

    fun showContactMethodsBottomSheet(contactInfo: ContactInfo) =
        contactActionsDelegate.showContactMethodsBottomSheet(contactInfo)

    fun dismissContactMethodsBottomSheet() = contactActionsDelegate.dismissContactMethodsBottomSheet()

    fun onDirectDialChoiceSelected(option: DirectDialOption, rememberChoice: Boolean) {
        contactActionsDelegate.onDirectDialChoiceSelected(option, rememberChoice)
        if (!rememberChoice) {
            legacyPreferenceState.hasSeenDirectDialChoice = true
        }
    }

    fun onCallPermissionResult(isGranted: Boolean, shouldShowPermissionError: Boolean) =
        contactActionsDelegate.onCallPermissionResult(isGranted, shouldShowPermissionError)

    fun getLastShownPhoneNumber(contactId: Long): String? =
        contactActionsDelegate.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(contactId: Long, phoneNumber: String) =
        contactActionsDelegate.setLastShownPhoneNumber(contactId, phoneNumber)

    fun onContactActionHintDismissed() {
        userPreferences.setHasSeenContactActionHint(true)
        updateUiState { it.copy(showContactActionHint = false) }
    }
}
