package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.dialogs.ContactActionsPopup
import com.tk.quicksearch.search.contacts.dialogs.ContactActionsPopupState
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchScreen.dialogs.AppShortcutIconEditDialog
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.dialogs.SearchScreenDialogs
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.settings.AppShortcutsSettings.EditCustomShortcutDialog
import com.tk.quicksearch.tools.directSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.directSearch.GeminiModelPickerDialog
import com.tk.quicksearch.tools.directSearch.GeminiTextModel
import kotlinx.coroutines.delay

@Composable
internal fun SearchScreenDialogLogic(
    state: SearchUiState,
    nicknameDialogState: NicknameDialogState?,
    contactActionPickerDialogState: ContactActionPickerDialogState?,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismissPhoneNumberSelection: () -> Unit,
    onDirectDialChoiceSelected: (DirectDialOption, Boolean) -> Unit,
    onDismissDirectDialChoice: () -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    onDismissContactMethods: () -> Unit,
    onReleaseNotesAcknowledged: () -> Unit,
    onReleaseNotesViewAllFeatures: () -> Unit,
    onDismissNicknameDialog: () -> Unit,
    onSaveAppNickname: (com.tk.quicksearch.search.models.AppInfo, String?) -> Unit,
    onSaveAppShortcutNickname: (StaticShortcut, String?) -> Unit,
    onSaveContactNickname: (ContactInfo, String?) -> Unit,
    onSaveFileNickname: (DeviceFile, String?) -> Unit,
    onSaveSettingNickname: (DeviceSetting, String?) -> Unit,
    onSaveCalendarEventNickname: (CalendarEventInfo, String?) -> Unit,
    getLastShownPhoneNumber: (Long) -> String?,
    setLastShownPhoneNumber: (Long, String) -> Unit,
    onSetPersonalContext: (String?) -> Unit,
    onSetGeminiModel: (String?) -> Unit,
    onSetGeminiGroundingEnabled: (Boolean) -> Unit,
    onRefreshAvailableGeminiModels: () -> Unit,
    getPrimaryContactCardAction: (Long) -> ContactCardAction?,
    getSecondaryContactCardAction: (Long) -> ContactCardAction?,
    onSavePrimaryContactCardAction: (Long, ContactCardAction) -> Unit,
    onSaveSecondaryContactCardAction: (Long, ContactCardAction) -> Unit,
    onDismissContactActionPicker: () -> Unit,
    showPersonalContextDialog: Boolean,
    setShowPersonalContextDialog: (Boolean) -> Unit,
    showGeminiModelDialog: Boolean,
    setShowGeminiModelDialog: (Boolean) -> Unit,
    personalContextInput: TextFieldValue,
    setPersonalContextInput: (TextFieldValue) -> Unit,
    shortcutToEdit: StaticShortcut?,
    onDismissShortcutToEdit: () -> Unit,
    onUpdateCustomAppShortcut: (StaticShortcut, String, String?, String?) -> Unit,
    onDeleteCustomAppShortcut: (StaticShortcut) -> Unit,
    shortcutIconEdit: StaticShortcut?,
    onDismissShortcutIconEdit: () -> Unit,
    onSetAppShortcutIconOverride: (StaticShortcut, String?) -> Unit,
    getAppShortcutIconOverride: (String) -> String?,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val personalContextFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showPersonalContextDialog) {
        if (showPersonalContextDialog) {
            delay(100)
            setPersonalContextInput(
                personalContextInput.copy(
                    selection = TextRange(personalContextInput.text.length),
                ),
            )
            personalContextFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Personal context dialog
    if (showPersonalContextDialog) {
        AppAlertDialog(
            onDismissRequest = { setShowPersonalContextDialog(false) },
            title = {
                Text(text = stringResource(R.string.settings_direct_search_personal_context_title))
            },
            text = {
                OutlinedTextField(
                    value = personalContextInput,
                    onValueChange = { setPersonalContextInput(it) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                            .focusRequester(personalContextFocusRequester),
                    placeholder = {
                        Text(text = stringResource(R.string.settings_direct_search_personal_context_hint))
                    },
                    shape = MaterialTheme.shapes.large,
                    singleLine = false,
                    minLines = 5,
                    colors = dialogTextFieldColors(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = personalContextInput.text.trim()
                        onSetPersonalContext(trimmed.takeIf { it.isNotEmpty() })
                        setShowPersonalContextDialog(false)
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowPersonalContextDialog(false) }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    // Gemini model dialog
    if (showGeminiModelDialog) {
        val modelOptions = remember(state.geminiModel, state.availableGeminiModels) {
            val allKnown = state.availableGeminiModels + GeminiModelCatalog.FALLBACK_TEXT_MODELS
            val currentModel = allKnown.find { it.id == state.geminiModel }
                ?: GeminiTextModel(state.geminiModel, state.geminiModel)
            (state.availableGeminiModels + currentModel).distinctBy { it.id }
                .sortedBy { it.displayName.lowercase() }
        }
        GeminiModelPickerDialog(
            selectedModelId = state.geminiModel,
            models = modelOptions,
            groundingEnabled = state.geminiGroundingEnabled,
            onGroundingChange = onSetGeminiGroundingEnabled,
            onModelSelected = { modelId ->
                onSetGeminiModel(modelId)
                val newModel = modelOptions.firstOrNull { it.id == modelId }
                if (newModel?.supportsGrounding == false && state.geminiGroundingEnabled) {
                    onSetGeminiGroundingEnabled(false)
                }
            },
            onDismiss = {
                setShowGeminiModelDialog(false)
                onRefreshAvailableGeminiModels()
            },
        )
    }

    shortcutToEdit?.let { shortcut ->
        EditCustomShortcutDialog(
            shortcut = shortcut,
            iconPackPackage = state.selectedIconPackPackage,
            onDismiss = onDismissShortcutToEdit,
            onSave = { name, value, iconBase64 ->
                onUpdateCustomAppShortcut(shortcut, name, value, iconBase64)
                onDismissShortcutToEdit()
            },
            onDelete = {
                onDeleteCustomAppShortcut(shortcut)
                onDismissShortcutToEdit()
            },
        )
    }

    shortcutIconEdit?.let { shortcut ->
        AppShortcutIconEditDialog(
            shortcut = shortcut,
            iconPackPackage = state.selectedIconPackPackage,
            currentIconBase64 = getAppShortcutIconOverride(shortcutKey(shortcut)),
            onDismiss = onDismissShortcutIconEdit,
            onSave = { iconBase64 ->
                onSetAppShortcutIconOverride(shortcut, iconBase64)
                onDismissShortcutIconEdit()
            },
        )
    }

    // All dialogs
    SearchScreenDialogs(
        state = state,
        nicknameDialogState = nicknameDialogState,
        onPhoneNumberSelected = onPhoneNumberSelected,
        onDismissPhoneNumberSelection = onDismissPhoneNumberSelection,
        onDirectDialChoiceSelected = onDirectDialChoiceSelected,
        onDismissDirectDialChoice = onDismissDirectDialChoice,
        onContactMethodClick = onContactMethodClick,
        onDismissContactMethods = onDismissContactMethods,
        onReleaseNotesAcknowledged = onReleaseNotesAcknowledged,
        onReleaseNotesViewAllFeatures = onReleaseNotesViewAllFeatures,
        onDismissNicknameDialog = onDismissNicknameDialog,
        onSaveAppNickname = onSaveAppNickname,
        onSaveAppShortcutNickname = onSaveAppShortcutNickname,
        onSaveContactNickname = onSaveContactNickname,
        onSaveFileNickname = onSaveFileNickname,
        onSaveSettingNickname = onSaveSettingNickname,
        onSaveCalendarEventNickname = onSaveCalendarEventNickname,
        getLastShownPhoneNumber = getLastShownPhoneNumber,
        setLastShownPhoneNumber = setLastShownPhoneNumber,
    )

    // Render Contact Action Picker Dialog
    contactActionPickerDialogState?.let { pickerState ->
        ContactActionsPopup(
            state =
                ContactActionsPopupState.ReplaceAction(
                    contactInfo = pickerState.contact,
                    currentAction = pickerState.currentAction,
                    onActionSelected = { action ->
                        if (pickerState.isPrimary) {
                            onSavePrimaryContactCardAction(pickerState.contact.contactId, action)
                        } else {
                            onSaveSecondaryContactCardAction(pickerState.contact.contactId, action)
                        }
                        onDismissContactActionPicker()
                    },
                ),
            getLastShownPhoneNumber = getLastShownPhoneNumber,
            setLastShownPhoneNumber = setLastShownPhoneNumber,
            onDismiss = onDismissContactActionPicker,
        )
    }
}
