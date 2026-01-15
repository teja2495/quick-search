package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.contacts.utils.TelegramContactUtils
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import kotlin.reflect.KClass

@Composable
fun ContactActionPickerDialog(
        contactInfo: ContactInfo,
        onActionSelected: (ContactCardAction) -> Unit,
        onDismiss: () -> Unit,
        getLastShownPhoneNumber: (Long) -> String? = { null },
        setLastShownPhoneNumber: (Long, String) -> Unit = { _, _ -> }
) {
    val hasMultipleNumbers = contactInfo.phoneNumbers.size > 1

    // Reorder phone numbers to show last shown number first (only for multiple numbers)
    val reorderedPhoneNumbers =
            remember(contactInfo.phoneNumbers, contactInfo.contactId, hasMultipleNumbers) {
                if (!hasMultipleNumbers) {
                    contactInfo.phoneNumbers
                } else {
                    val lastShownNumber = getLastShownPhoneNumber(contactInfo.contactId)
                    if (lastShownNumber == null || contactInfo.phoneNumbers.isEmpty()) {
                        contactInfo.phoneNumbers
                    } else {
                        val lastShownIndex =
                                contactInfo.phoneNumbers.indexOfFirst { number ->
                                    PhoneNumberUtils.isSameNumber(number, lastShownNumber)
                                }
                        if (lastShownIndex >= 0) {
                            val reordered = contactInfo.phoneNumbers.toMutableList()
                            val lastShown = reordered.removeAt(lastShownIndex)
                            reordered.add(0, lastShown)
                            reordered
                        } else {
                            contactInfo.phoneNumbers
                        }
                    }
                }
            }

    // State for phone number selection
    var selectedPhoneIndex by remember { mutableStateOf(0) }
    val selectedPhoneNumber =
            reorderedPhoneNumbers.getOrNull(selectedPhoneIndex) ?: contactInfo.primaryNumber

    LaunchedEffect(selectedPhoneIndex, reorderedPhoneNumbers, hasMultipleNumbers) {
        if (hasMultipleNumbers &&
                        reorderedPhoneNumbers.isNotEmpty() &&
                        selectedPhoneIndex >= 0 &&
                        selectedPhoneIndex < reorderedPhoneNumbers.size
        ) {
            val number = reorderedPhoneNumbers[selectedPhoneIndex]
            if (number.isNotBlank()) {
                setLastShownPhoneNumber(contactInfo.contactId, number)
            }
        }
    }

    Dialog(
            onDismissRequest = onDismiss,
            properties =
                    DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false
                    )
    ) {
        Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp
            ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header with title and close button
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                    text =
                                            stringResource(
                                                    R.string.dialog_choose_contact_action_title
                                            ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Close button
                        IconButton(onClick = { onDismiss() }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.dialog_cancel),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card encompassing options with black background
                    Card(
                            modifier = Modifier.fillMaxWidth().height(370.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(
                                                        start = 16.dp,
                                                        top = 20.dp,
                                                        end = 16.dp,
                                                        bottom = 24.dp
                                                )
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
                                    // Left arrow
                                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex > 0) {
                                        IconButton(
                                                onClick = {
                                                    selectedPhoneIndex =
                                                            (selectedPhoneIndex - 1).coerceAtLeast(
                                                                    0
                                                            )
                                                },
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                    imageVector = Icons.Rounded.ChevronLeft,
                                                    contentDescription =
                                                            stringResource(
                                                                    R.string
                                                                            .contacts_action_previous_number
                                                            ),
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
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

                                    // Right arrow
                                    if (reorderedPhoneNumbers.size > 1 &&
                                                    selectedPhoneIndex <
                                                            reorderedPhoneNumbers.size - 1
                                    ) {
                                        IconButton(
                                                onClick = {
                                                    selectedPhoneIndex =
                                                            (selectedPhoneIndex + 1).coerceAtMost(
                                                                    reorderedPhoneNumbers.size - 1
                                                            )
                                                },
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                    imageVector = Icons.Rounded.ChevronRight,
                                                    contentDescription =
                                                            stringResource(
                                                                    R.string
                                                                            .contacts_action_next_number
                                                            ),
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            // Filter methods for selected number
                            val context = LocalContext.current
                            val methodsForSelectedNumber =
                                    contactInfo.contactMethods.filter { method ->
                                        when {
                                            method is ContactMethod.TelegramMessage ||
                                                    method is ContactMethod.TelegramCall ||
                                                    method is ContactMethod.TelegramVideoCall -> {
                                                if (selectedPhoneNumber != null) {
                                                    TelegramContactUtils
                                                            .isTelegramMethodForPhoneNumber(
                                                                    context = context,
                                                                    phoneNumber =
                                                                            selectedPhoneNumber,
                                                                    telegramMethod = method
                                                            )
                                                } else true
                                            }
                                            else -> {
                                                val methodData =
                                                        method.data.takeIf { it.isNotBlank() }
                                                methodData != null &&
                                                        selectedPhoneNumber != null &&
                                                        PhoneNumberUtils.isSameNumber(
                                                                methodData,
                                                                selectedPhoneNumber
                                                        )
                                            }
                                        }
                                    }

                            // First row: call, message, google meet
                            val firstRowMethods = mutableListOf<ContactMethod>()
                            methodsForSelectedNumber.find { it is ContactMethod.Phone }?.let {
                                firstRowMethods.add(it)
                            }
                            methodsForSelectedNumber.find { it is ContactMethod.Sms }?.let {
                                firstRowMethods.add(it)
                            }
                            methodsForSelectedNumber.find { it is ContactMethod.GoogleMeet }?.let {
                                firstRowMethods.add(it)
                            }

                            val onClick: (ContactMethod) -> Unit = { method ->
                                selectedPhoneNumber?.let { phoneNumber ->
                                    val action =
                                            when (method) {
                                                is ContactMethod.Phone ->
                                                        ContactCardAction.Phone(phoneNumber)
                                                is ContactMethod.Sms ->
                                                        ContactCardAction.Sms(phoneNumber)
                                                is ContactMethod.WhatsAppCall ->
                                                        ContactCardAction.WhatsAppCall(phoneNumber)
                                                is ContactMethod.WhatsAppMessage ->
                                                        ContactCardAction.WhatsAppMessage(
                                                                phoneNumber
                                                        )
                                                is ContactMethod.WhatsAppVideoCall ->
                                                        ContactCardAction.WhatsAppVideoCall(
                                                                phoneNumber
                                                        )
                                                is ContactMethod.TelegramMessage ->
                                                        ContactCardAction.TelegramMessage(
                                                                phoneNumber
                                                        )
                                                is ContactMethod.TelegramCall ->
                                                        ContactCardAction.TelegramCall(phoneNumber)
                                                is ContactMethod.TelegramVideoCall ->
                                                        ContactCardAction.TelegramVideoCall(
                                                                phoneNumber
                                                        )
                                                is ContactMethod.GoogleMeet ->
                                                        ContactCardAction.GoogleMeet(phoneNumber)
                                                else -> null
                                            }
                                    action?.let {
                                        onActionSelected(it)
                                        onDismiss()
                                    }
                                }
                            }

                            if (firstRowMethods.isNotEmpty()) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Top
                                ) {
                                    firstRowMethods.forEach { method ->
                                        ContactActionButton(
                                                method = method,
                                                onClick = { onClick(method) },
                                                usePhoneIconForCallActions = true
                                        )
                                    }
                                }
                            }

                            // WhatsApp row
                            renderActionPickerRow(
                                    methods = methodsForSelectedNumber,
                                    methodTypes =
                                            listOf(
                                                    ContactMethod.WhatsAppCall::class,
                                                    ContactMethod.WhatsAppMessage::class,
                                                    ContactMethod.WhatsAppVideoCall::class
                                            ),
                                    onMethodClick = onClick
                            )

                            // Telegram row
                            renderActionPickerRow(
                                    methods = methodsForSelectedNumber,
                                    methodTypes =
                                            listOf(
                                                    ContactMethod.TelegramMessage::class,
                                                    ContactMethod.TelegramCall::class,
                                                    ContactMethod.TelegramVideoCall::class
                                            ),
                                    onMethodClick = onClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private inline fun renderActionPickerRow(
        methods: List<ContactMethod>,
        methodTypes: List<KClass<out ContactMethod>>,
        crossinline onMethodClick: (ContactMethod) -> Unit
) {
    val filteredMethods =
            methods.filter { method -> methodTypes.any { type -> type.isInstance(method) } }

    if (filteredMethods.isNotEmpty()) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
        ) {
            filteredMethods.forEach { method ->
                ContactActionButton(
                        method = method,
                        onClick = { onMethodClick(method) },
                        usePhoneIconForCallActions = true
                )
            }
        }
    }
}
