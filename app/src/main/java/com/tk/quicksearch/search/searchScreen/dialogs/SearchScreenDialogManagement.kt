package com.tk.quicksearch.search.searchScreen.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.dialogs.ContactActionsPopup
import com.tk.quicksearch.search.contacts.dialogs.ContactActionsPopupState
import com.tk.quicksearch.search.contacts.dialogs.DirectDialChoiceDialog
import com.tk.quicksearch.search.contacts.dialogs.PhoneNumberSelectionDialog
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo

/**
 * Composable that manages all dialogs for SearchScreen
 */
@Composable
internal fun SearchScreenDialogs(
    state: SearchUiState,
    nicknameDialogState: NicknameDialogState?,
    triggerDialogState: TriggerDialogState?,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismissPhoneNumberSelection: () -> Unit,
    onDirectDialChoiceSelected: (DirectDialOption, Boolean) -> Unit,
    onDismissDirectDialChoice: () -> Unit,
    onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
    onDismissContactMethods: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onReleaseNotesViewAllFeatures: () -> Unit,
    onDismissNicknameDialog: () -> Unit,
    onDismissTriggerDialog: () -> Unit,
    onSaveAppNickname: (AppInfo, String?) -> Unit,
    onSaveAppShortcutNickname: (StaticShortcut, String?) -> Unit,
    onSaveContactNickname: (ContactInfo, String?) -> Unit,
    onSaveFileNickname: (DeviceFile, String?) -> Unit,
    onSaveSettingNickname: (DeviceSetting, String?) -> Unit,
    onSaveCalendarEventNickname: (CalendarEventInfo, String?) -> Unit,
    onSaveAppTrigger: (AppInfo, com.tk.quicksearch.search.data.preferences.ResultTrigger?) -> Unit,
    onSaveAppShortcutTrigger: (StaticShortcut, com.tk.quicksearch.search.data.preferences.ResultTrigger?) -> Unit,
    onSaveContactTrigger: (ContactInfo, com.tk.quicksearch.search.data.preferences.ResultTrigger?) -> Unit,
    onSaveFileTrigger: (DeviceFile, com.tk.quicksearch.search.data.preferences.ResultTrigger?) -> Unit,
    onSaveSettingTrigger: (DeviceSetting, com.tk.quicksearch.search.data.preferences.ResultTrigger?) -> Unit,
    onSaveNoteTrigger: (NoteInfo, com.tk.quicksearch.search.data.preferences.ResultTrigger?) -> Unit,
    getLastShownPhoneNumber: (Long) -> String?,
    setLastShownPhoneNumber: (Long, String) -> Unit,
) {
    // Phone number selection dialog
    state.phoneNumberSelection?.let { selection ->
        PhoneNumberSelectionDialog(
            contactInfo = selection.contactInfo,
            isCall = selection.isCall,
            onPhoneNumberSelected = onPhoneNumberSelected,
            onDismiss = onDismissPhoneNumberSelection,
        )
    }

    state.directDialChoice?.let { choice ->
        DirectDialChoiceDialog(
            contactName = choice.contactName,
            phoneNumber = choice.phoneNumber,
            onSelectOption = onDirectDialChoiceSelected,
            onDismiss = onDismissDirectDialChoice,
        )
    }

    // Contact methods dialog
    state.contactMethodsBottomSheet?.let { contactInfo ->
        val viewInContactsLabel = stringResource(R.string.contact_method_view_in_contacts_label)
        ContactActionsPopup(
            state =
                ContactActionsPopupState.ContactActions(
                    contactInfo = contactInfo,
                    onContactMethodClick = onContactMethodClick,
                    onAvatarClick = { contact ->
                        onContactMethodClick(contact, ContactMethod.ViewInContactsApp(viewInContactsLabel))
                    },
                ),
            getLastShownPhoneNumber = getLastShownPhoneNumber,
            setLastShownPhoneNumber = setLastShownPhoneNumber,
            onDismiss = onDismissContactMethods,
        )
    }

    if (state.showReleaseNotesDialog) {
        ReleaseNotesDialog(
            versionName = state.releaseNotesVersionName,
            onAcknowledge = onReleaseNotesAcknowledged,
            onViewAllFeatures = onReleaseNotesViewAllFeatures,
        )
    }

    // Nickname dialog
    nicknameDialogState?.let { dialogState ->
        when (dialogState) {
            is NicknameDialogState.App -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveAppNickname(dialogState.app, nickname)
                    },
                    onDismiss = onDismissNicknameDialog,
                )
            }

            is NicknameDialogState.AppShortcut -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveAppShortcutNickname(dialogState.shortcut, nickname)
                    },
                    onDismiss = onDismissNicknameDialog,
                )
            }

            is NicknameDialogState.Contact -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveContactNickname(dialogState.contact, nickname)
                    },
                    onDismiss = onDismissNicknameDialog,
                )
            }

            is NicknameDialogState.File -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveFileNickname(dialogState.file, nickname)
                    },
                    onDismiss = onDismissNicknameDialog,
                )
            }

            is NicknameDialogState.Setting -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveSettingNickname(dialogState.setting, nickname)
                    },
                    onDismiss = onDismissNicknameDialog,
                )
            }

            is NicknameDialogState.CalendarEvent -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveCalendarEventNickname(dialogState.event, nickname)
                    },
                    onDismiss = onDismissNicknameDialog,
                )
            }
        }
    }

    triggerDialogState?.let { dialogState ->
        when (dialogState) {
            is TriggerDialogState.App ->
                TriggerDialog(
                    currentTrigger = dialogState.currentTrigger,
                    itemName = dialogState.itemName,
                    onSave = { onSaveAppTrigger(dialogState.app, it) },
                    onDismiss = onDismissTriggerDialog,
                )
            is TriggerDialogState.AppShortcut ->
                TriggerDialog(
                    currentTrigger = dialogState.currentTrigger,
                    itemName = dialogState.itemName,
                    onSave = { onSaveAppShortcutTrigger(dialogState.shortcut, it) },
                    onDismiss = onDismissTriggerDialog,
                )
            is TriggerDialogState.Contact ->
                TriggerDialog(
                    currentTrigger = dialogState.currentTrigger,
                    itemName = dialogState.itemName,
                    onSave = { onSaveContactTrigger(dialogState.contact, it) },
                    onDismiss = onDismissTriggerDialog,
                )
            is TriggerDialogState.File ->
                TriggerDialog(
                    currentTrigger = dialogState.currentTrigger,
                    itemName = dialogState.itemName,
                    onSave = { onSaveFileTrigger(dialogState.file, it) },
                    onDismiss = onDismissTriggerDialog,
                )
            is TriggerDialogState.Setting ->
                TriggerDialog(
                    currentTrigger = dialogState.currentTrigger,
                    itemName = dialogState.itemName,
                    onSave = { onSaveSettingTrigger(dialogState.setting, it) },
                    onDismiss = onDismissTriggerDialog,
                )
            is TriggerDialogState.Note ->
                TriggerDialog(
                    currentTrigger = dialogState.currentTrigger,
                    itemName = dialogState.itemName,
                    onSave = { onSaveNoteTrigger(dialogState.note, it) },
                    onDismiss = onDismissTriggerDialog,
                )
        }
    }
}
