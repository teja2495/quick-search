package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.settings.shared.SettingsNestedCheckbox
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

// ============================================================================
// Phone Number Selection Drawer
// ============================================================================

/**
 * Picks which of a contact's numbers to call or message. Tapping a number acts on it at once, so
 * the drawer needs no confirm button; "Remember choice" applies to the number tapped.
 */
@Composable
fun PhoneNumberSelectionDialog(
    contactInfo: ContactInfo,
    isCall: Boolean,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var rememberChoice by remember { mutableStateOf(false) }
    val actionIcon = if (isCall) MaterialSymbols.Call else MaterialSymbols.Sms

    AppPickerDrawer(
        title = stringResource(R.string.dialog_select_phone_number_title),
        onDismiss = onDismiss,
        header = {
            Text(
                text = stringResource(R.string.dialog_select_phone_number_message, contactInfo.displayName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        footer = {
            SettingsNestedCheckbox(
                label = stringResource(R.string.dialog_remember_choice),
                checked = rememberChoice,
                onCheckedChange = { rememberChoice = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing),
        ) {
            contactInfo.phoneNumbers.forEach { number ->
                AppPickerDrawerRow(
                    title = PhoneNumberUtils.formatPhoneNumberForDisplay(number),
                    onClick = { onPhoneNumberSelected(number, rememberChoice) },
                    leading = { Icon(imageVector = actionIcon, contentDescription = null) },
                )
            }
        }
    }
}

// ============================================================================
// Direct Dial Choice Drawer
// ============================================================================

/** Picks whether calls start directly or open the dialer. The choice is saved as the default. */
@Composable
fun DirectDialChoiceDialog(
    contactName: String,
    phoneNumber: String,
    onSelectOption: (DirectDialOption, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AppPickerDrawer(
        title = stringResource(R.string.dialog_direct_dial_title),
        onDismiss = onDismiss,
        footer = {
            Text(
                text = stringResource(R.string.dialog_direct_dial_change_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing),
        ) {
            DirectDialOption.entries.forEach { option ->
                val (titleRes, descriptionRes, icon) =
                    when (option) {
                        DirectDialOption.DIRECT_CALL ->
                            Triple(
                                R.string.dialog_direct_dial_option_direct,
                                R.string.dialog_direct_dial_option_direct_desc,
                                MaterialSymbols.Call,
                            )
                        DirectDialOption.DIALER ->
                            Triple(
                                R.string.dialog_direct_dial_option_dialer,
                                R.string.dialog_direct_dial_option_dialer_desc,
                                MaterialSymbols.OpenInNew,
                            )
                    }
                AppPickerDrawerRow(
                    title = stringResource(titleRes),
                    subtitle = stringResource(descriptionRes),
                    onClick = { onSelectOption(option, true) },
                    leading = { Icon(imageVector = icon, contentDescription = null) },
                )
            }
        }
    }
}
