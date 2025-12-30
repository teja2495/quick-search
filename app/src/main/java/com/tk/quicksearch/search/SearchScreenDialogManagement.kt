package com.tk.quicksearch.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.contacts.PhoneNumberSelectionDialog
import com.tk.quicksearch.search.contacts.DirectDialChoiceDialog
import com.tk.quicksearch.search.contacts.ContactMethodsDialog

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
    onContactMethodClick: (ContactInfo, com.tk.quicksearch.model.ContactMethod) -> Unit,
    onDismissContactMethods: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onSaveAppNickname: (AppInfo, String?) -> Unit,
    onSaveContactNickname: (ContactInfo, String?) -> Unit,
    onSaveFileNickname: (DeviceFile, String?) -> Unit,
    onSaveSettingNickname: (SettingShortcut, String?) -> Unit
) {
    // Phone number selection dialog
    state.phoneNumberSelection?.let { selection ->
        PhoneNumberSelectionDialog(
            contactInfo = selection.contactInfo,
            isCall = selection.isCall,
            onPhoneNumberSelected = onPhoneNumberSelected,
            onDismiss = onDismissPhoneNumberSelection
        )
    }

    state.directDialChoice?.let { choice ->
        DirectDialChoiceDialog(
            contactName = choice.contactName,
            phoneNumber = choice.phoneNumber,
            onSelectOption = onDirectDialChoiceSelected,
            onDismiss = onDismissDirectDialChoice
        )
    }

    // Contact methods dialog
    state.contactMethodsBottomSheet?.let { contactInfo ->
        val context = LocalContext.current
        val userPreferences = remember { com.tk.quicksearch.data.UserAppPreferences(context) }
        ContactMethodsDialog(
            contactInfo = contactInfo,
            onContactMethodClick = onContactMethodClick,
            onDismiss = onDismissContactMethods,
            getLastShownPhoneNumber = { contactId -> userPreferences.getLastShownPhoneNumber(contactId) },
            setLastShownPhoneNumber = { contactId, phoneNumber -> userPreferences.setLastShownPhoneNumber(contactId, phoneNumber) }
        )
    }

    if (state.showReleaseNotesDialog) {
        ReleaseNotesDialog(
            versionName = state.releaseNotesVersionName,
            onAcknowledge = onReleaseNotesAcknowledged
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
                    onDismiss = { /* This will be handled by parent */ }
                )
            }
            is NicknameDialogState.Contact -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveContactNickname(dialogState.contact, nickname)
                    },
                    onDismiss = { /* This will be handled by parent */ }
                )
            }
            is NicknameDialogState.File -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveFileNickname(dialogState.file, nickname)
                    },
                    onDismiss = { /* This will be handled by parent */ }
                )
            }
            is NicknameDialogState.Setting -> {
                NicknameDialog(
                    currentNickname = dialogState.currentNickname,
                    itemName = dialogState.itemName,
                    onSave = { nickname ->
                        onSaveSettingNickname(dialogState.setting, nickname)
                    },
                    onDismiss = { /* This will be handled by parent */ }
                )
            }
        }
    }
}
