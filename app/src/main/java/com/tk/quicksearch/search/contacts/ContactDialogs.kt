package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectDialOption
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import kotlin.reflect.KClass

// ============================================================================
// Phone Number Selection Dialog
// ============================================================================

@Composable
fun PhoneNumberSelectionDialog(
    contactInfo: ContactInfo,
    isCall: Boolean,
    onPhoneNumberSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }
    var selectedNumber by remember { mutableStateOf<String?>(contactInfo.phoneNumbers.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_select_phone_number_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.dialog_select_phone_number_message,
                        contactInfo.displayName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // List of phone numbers
                contactInfo.phoneNumbers.forEach { number ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedNumber = number },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedNumber == number,
                            onClick = { selectedNumber = number }
                        )
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remember choice checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Text(
                        text = stringResource(R.string.dialog_remember_choice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
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
                enabled = selectedNumber != null
            ) {
                Text(
                    text = if (isCall) {
                        stringResource(R.string.dialog_call)
                    } else {
                        stringResource(R.string.dialog_sms)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun DirectDialChoiceDialog(
    contactName: String,
    phoneNumber: String,
    onSelectOption: (DirectDialOption, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(DirectDialOption.DIRECT_CALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_direct_dial_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DirectDialOption.values().forEach { option ->
                        val title = when (option) {
                            DirectDialOption.DIRECT_CALL -> stringResource(R.string.dialog_direct_dial_option_direct)
                            DirectDialOption.DIALER -> stringResource(R.string.dialog_direct_dial_option_dialer)
                        }
                        val description = when (option) {
                            DirectDialOption.DIRECT_CALL -> stringResource(R.string.dialog_direct_dial_option_direct_desc)
                            DirectDialOption.DIALER -> stringResource(R.string.dialog_direct_dial_option_dialer_desc)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = option },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = { selectedOption = option }
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.dialog_direct_dial_change_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    )
}

// ============================================================================
// Contact Methods Dialog
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactMethodsDialog(
    contactInfo: ContactInfo,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    onDismiss: () -> Unit,
    getLastShownPhoneNumber: (Long) -> String? = { null },
    setLastShownPhoneNumber: (Long, String) -> Unit = { _, _ -> }
) {
    val hasMultipleNumbers = contactInfo.phoneNumbers.size > 1

    // Reorder phone numbers to show last shown number first (only for multiple numbers)
    val reorderedPhoneNumbers = remember(contactInfo.phoneNumbers, contactInfo.contactId, hasMultipleNumbers) {
        reorderPhoneNumbersForDisplay(contactInfo, hasMultipleNumbers, getLastShownPhoneNumber)
    }

    // State for phone number selection (always start at 0 since we reordered)
    var selectedPhoneIndex by remember { mutableStateOf(0) }
    val selectedPhoneNumber = reorderedPhoneNumbers.getOrNull(selectedPhoneIndex)
        ?: contactInfo.primaryNumber

    // Save the selected number when it changes (only for multiple numbers)
    LaunchedEffect(selectedPhoneIndex, reorderedPhoneNumbers, hasMultipleNumbers) {
        if (hasMultipleNumbers &&
            reorderedPhoneNumbers.isNotEmpty() &&
            selectedPhoneIndex >= 0 &&
            selectedPhoneIndex < reorderedPhoneNumbers.size) {
            val number = reorderedPhoneNumbers[selectedPhoneIndex]
            if (number.isNotBlank()) {
                setLastShownPhoneNumber(contactInfo.contactId, number)
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header with contact info (photo and name outside the card)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(
                            photoUri = contactInfo.photoUri,
                            displayName = contactInfo.displayName,
                            onClick = {
                                onContactMethodClick(contactInfo, ContactMethod.ViewInContactsApp())
                                onDismiss()
                            },
                            modifier = Modifier.size(48.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = contactInfo.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = {
                                onDismiss()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dialog_cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card encompassing options with black background
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Phone number with navigation arrows
                            selectedPhoneNumber?.let { phoneNumber ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left arrow (only show if there are multiple numbers and not at first)
                                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex > 0) {
                                        IconButton(
                                            onClick = {
                                                selectedPhoneIndex = (selectedPhoneIndex - 1).coerceAtLeast(0)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronLeft,
                                                contentDescription = stringResource(R.string.contacts_action_previous_number),
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        // Spacer to maintain alignment
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }

                                    // Phone number
                                    Text(
                                        text = phoneNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Right arrow (only show if there are multiple numbers and not at last)
                                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex < reorderedPhoneNumbers.size - 1) {
                                        IconButton(
                                            onClick = {
                                                selectedPhoneIndex = (selectedPhoneIndex + 1).coerceAtMost(reorderedPhoneNumbers.size - 1)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = stringResource(R.string.contacts_action_next_number),
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        // Spacer to maintain alignment
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            // First row: call, message, google meet (filtered by selected phone number)
                            val firstRowMethods = mutableListOf<ContactMethod>()

                            // Filter methods by selected phone number (using phone number normalization)
                            val context = LocalContext.current
                            val methodsForSelectedNumber = filterMethodsByPhoneNumber(
                                contactInfo.contactMethods,
                                selectedPhoneNumber,
                                context
                            )

                            // Always add call if available for selected number
                            methodsForSelectedNumber.find { it is ContactMethod.Phone }?.let { firstRowMethods.add(it) }

                            // Always add message if available for selected number
                            methodsForSelectedNumber.find { it is ContactMethod.Sms }?.let { firstRowMethods.add(it) }

                            // Add Google Meet if available for selected number
                            methodsForSelectedNumber.find { it is ContactMethod.GoogleMeet }?.let { firstRowMethods.add(it) }

                            if (firstRowMethods.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    firstRowMethods.forEach { method ->
                                        ContactActionButton(
                                            method = method,
                                            onClick = {
                                                onContactMethodClick(contactInfo, method)
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }

                            // Render method rows for different app types
                            renderMethodRow(methodsForSelectedNumber, listOf(
                                ContactMethod.WhatsAppCall::class,
                                ContactMethod.WhatsAppMessage::class,
                                ContactMethod.WhatsAppVideoCall::class
                            )) { method ->
                                onContactMethodClick(contactInfo, method)
                                onDismiss()
                            }

                            renderMethodRow(methodsForSelectedNumber, listOf(
                                ContactMethod.TelegramMessage::class,
                                ContactMethod.TelegramCall::class,
                                ContactMethod.TelegramVideoCall::class
                            )) { method ->
                                onContactMethodClick(contactInfo, method)
                                onDismiss()
                            }

                            // Show message if no methods available
                            if (contactInfo.contactMethods.filterNot { it is ContactMethod.Email }.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.contacts_no_methods_available),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Reorders phone numbers to prioritize the last shown number for better UX.
 * Only applies when there are multiple phone numbers.
 */
private fun reorderPhoneNumbersForDisplay(
    contactInfo: ContactInfo,
    hasMultipleNumbers: Boolean,
    getLastShownPhoneNumber: (Long) -> String?
): List<String> {
    if (!hasMultipleNumbers) {
        return contactInfo.phoneNumbers
    }

    val lastShownNumber = getLastShownPhoneNumber(contactInfo.contactId)
    if (lastShownNumber == null || contactInfo.phoneNumbers.isEmpty()) {
        return contactInfo.phoneNumbers
    }

    // Find the index of the last shown number (using phone number matching)
    val lastShownIndex = contactInfo.phoneNumbers.indexOfFirst { number ->
        PhoneNumberUtils.isSameNumber(number, lastShownNumber)
    }

    return if (lastShownIndex >= 0) {
        // Move the last shown number to the front
        val reordered = contactInfo.phoneNumbers.toMutableList()
        val lastShown = reordered.removeAt(lastShownIndex)
        reordered.add(0, lastShown)
        reordered
    } else {
        contactInfo.phoneNumbers
    }
}

/**
 * Filters contact methods to only include those that match the selected phone number.
 * Telegram methods use special utility functions for matching, while other methods
 * use phone number normalization for comparison.
 */
private fun filterMethodsByPhoneNumber(
    contactMethods: List<ContactMethod>,
    selectedPhoneNumber: String?,
    context: android.content.Context
): List<ContactMethod> {
    return contactMethods.filter { method ->
        when {
            // Telegram methods require special handling with utility functions
            method is ContactMethod.TelegramMessage ||
            method is ContactMethod.TelegramCall ||
            method is ContactMethod.TelegramVideoCall -> {
                if (selectedPhoneNumber != null) {
                    TelegramContactUtils.isTelegramMethodForPhoneNumber(
                        context = context,
                        phoneNumber = selectedPhoneNumber,
                        telegramMethod = method
                    )
                } else {
                    // If no phone number is selected, show all Telegram methods
                    true
                }
            }

            // For other methods, require phone number match with the selected number
            else -> {
                val methodData = method.data?.takeIf { it.isNotBlank() }
                methodData != null && selectedPhoneNumber != null &&
                    PhoneNumberUtils.isSameNumber(methodData, selectedPhoneNumber)
            }
        }
    }
}

/**
 * Renders a row of contact methods if any methods of the specified types are available.
 */
@Composable
private inline fun renderMethodRow(
    methods: List<ContactMethod>,
    methodTypes: List<KClass<out ContactMethod>>,
    crossinline onMethodClick: (ContactMethod) -> Unit
) {
    val filteredMethods = methods.filter { method ->
        methodTypes.any { type -> type.isInstance(method) }
    }

    if (filteredMethods.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            filteredMethods.forEach { method ->
                ContactActionButton(
                    method = method,
                    onClick = { onMethodClick(method) }
                )
            }
        }
    }
}
