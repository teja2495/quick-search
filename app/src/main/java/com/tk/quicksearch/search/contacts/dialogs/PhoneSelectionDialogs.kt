package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.utils.PhoneNumberUtils

// ============================================================================
// Phone Number Selection Dialog
// ============================================================================

@Composable
fun PhoneNumberSelectionDialog(
    contactInfo: ContactInfo,
    isCall: Boolean,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var rememberChoice by remember { mutableStateOf(false) }
    var selectedNumber by remember { mutableStateOf<String?>(contactInfo.phoneNumbers.firstOrNull()) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_select_phone_number_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.dialog_select_phone_number_message,
                            contactInfo.displayName,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // List of phone numbers
                contactInfo.phoneNumbers.forEach { number ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedNumber = number },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = selectedNumber == number,
                            onClick = { selectedNumber = number },
                        )
                        Text(
                            text = PhoneNumberUtils.formatPhoneNumberForDisplay(number),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remember choice checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it },
                    )
                    Text(
                        text = stringResource(R.string.dialog_remember_choice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedNumber?.let { number ->
                        onPhoneNumberSelected(number, rememberChoice)
                    }
                },
                enabled = selectedNumber != null,
            ) {
                Text(
                    text =
                        if (isCall) {
                            stringResource(R.string.contact_method_call_label)
                        } else {
                            stringResource(R.string.contact_method_message_label)
                        },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
fun DirectDialChoiceDialog(
    contactName: String,
    phoneNumber: String,
    onSelectOption: (DirectDialOption, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedOption by remember { mutableStateOf(DirectDialOption.DIRECT_CALL) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_direct_dial_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DirectDialOption.values().forEach { option ->
                        val title =
                            when (option) {
                                DirectDialOption.DIRECT_CALL -> stringResource(R.string.dialog_direct_dial_option_direct)
                                DirectDialOption.DIALER -> stringResource(R.string.dialog_direct_dial_option_dialer)
                            }
                        val description =
                            when (option) {
                                DirectDialOption.DIRECT_CALL -> stringResource(R.string.dialog_direct_dial_option_direct_desc)
                                DirectDialOption.DIALER -> stringResource(R.string.dialog_direct_dial_option_dialer_desc)
                            }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = option },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option },
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dialog_direct_dial_change_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSelectOption(selectedOption, true) }) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}