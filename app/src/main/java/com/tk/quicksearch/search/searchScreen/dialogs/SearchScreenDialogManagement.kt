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
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile

/**
 * Composable that manages all dialogs for SearchScreen
 */
@Composable
internal fun SearchScreenDialogs(
    state: SearchUiState,
    nicknameDialogState: NicknameDialogState?,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismissPhoneNumberSelection: () -> Unit,
    onDirectDialChoiceSelected: (DirectDialOption, Boolean) -> Unit,
    onDismissDirectDialChoice: () -> Unit,
    onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
    onDismissContactMethods: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onDismissNicknameDialog: () -> Unit,
    onSaveAppNickname: (AppInfo, String?) -> Unit,
    onSaveAppShortcutNickname: (StaticShortcut, String?) -> Unit,
    onSaveContactNickname: (ContactInfo, String?) -> Unit,
    onSaveFileNickname: (DeviceFile, String?) -> Unit,
    onSaveSettingNickname: (DeviceSetting, String?) -> Unit,
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
        }
    }
}
