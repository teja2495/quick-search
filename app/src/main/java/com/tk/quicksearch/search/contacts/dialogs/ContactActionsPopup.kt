package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils

internal sealed interface ContactActionsPopupState {
    data class ContactActions(
        val contactInfo: ContactInfo,
        val onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
        val onAvatarClick: (ContactInfo) -> Unit,
    ) : ContactActionsPopupState

    data class ReplaceAction(
        val contactInfo: ContactInfo,
        val currentAction: ContactCardAction?,
        val onActionSelected: (ContactCardAction) -> Unit,
    ) : ContactActionsPopupState
}

@Composable
internal fun ContactActionsPopup(
    state: ContactActionsPopupState,
    getLastShownPhoneNumber: (Long) -> String? = { null },
    setLastShownPhoneNumber: (Long, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    val contactInfo =
        when (state) {
            is ContactActionsPopupState.ContactActions -> state.contactInfo
            is ContactActionsPopupState.ReplaceAction -> state.contactInfo
        }
    val hasMultipleNumbers = contactInfo.phoneNumbers.size > 1
    val context = LocalContext.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }

    val reorderedPhoneNumbers =
        remember(contactInfo.phoneNumbers, contactInfo.contactId, hasMultipleNumbers) {
            reorderPhoneNumbersForDisplay(contactInfo, hasMultipleNumbers, getLastShownPhoneNumber)
        }
    var selectedPhoneIndex by remember { mutableStateOf(0) }
    val selectedPhoneNumber =
        reorderedPhoneNumbers.getOrNull(selectedPhoneIndex) ?: contactInfo.primaryNumber

    LaunchedEffect(selectedPhoneIndex, reorderedPhoneNumbers, hasMultipleNumbers) {
        if (hasMultipleNumbers &&
            reorderedPhoneNumbers.isNotEmpty() &&
            selectedPhoneIndex in reorderedPhoneNumbers.indices
        ) {
            val number = reorderedPhoneNumbers[selectedPhoneIndex]
            if (number.isNotBlank()) {
                setLastShownPhoneNumber(contactInfo.contactId, number)
            }
        }
    }

    val methodsForSelectedNumber =
        filterMethodsByPhoneNumber(
            contactInfo.contactMethods,
            selectedPhoneNumber,
            context,
        )
    val firstRowMethods = mutableListOf<ContactMethod>()
    methodsForSelectedNumber.find { it is ContactMethod.Phone }?.let { firstRowMethods.add(it) }
    methodsForSelectedNumber.find { it is ContactMethod.Sms }?.let { firstRowMethods.add(it) }
    methodsForSelectedNumber.find { it is ContactMethod.GoogleMeet }?.let { firstRowMethods.add(it) }

    val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ContactActionsPopupHeader(
                        state = state,
                        onDismiss = onDismiss,
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = maxCardHeight),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        val optionsScrollState = rememberScrollState()
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(optionsScrollState)
                                    .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            selectedPhoneNumber?.let { phoneNumber ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex > 0) {
                                        IconButton(
                                            onClick = {
                                                selectedPhoneIndex =
                                                    (selectedPhoneIndex - 1).coerceAtLeast(0)
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronLeft,
                                                contentDescription =
                                                    stringResource(R.string.contacts_action_previous_number),
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }

                                    Text(
                                        text = PhoneNumberUtils.formatPhoneNumberForDisplay(phoneNumber),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )

                                    if (reorderedPhoneNumbers.size > 1 &&
                                        selectedPhoneIndex < reorderedPhoneNumbers.size - 1
                                    ) {
                                        IconButton(
                                            onClick = {
                                                selectedPhoneIndex =
                                                    (selectedPhoneIndex + 1).coerceAtMost(
                                                        reorderedPhoneNumbers.size - 1,
                                                    )
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription =
                                                    stringResource(R.string.contacts_action_next_number),
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            when (state) {
                                is ContactActionsPopupState.ContactActions -> {
                                    if (firstRowMethods.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            firstRowMethods.forEach { method ->
                                                ContactActionButton(
                                                    method = method,
                                                    onClick = {
                                                        state.onContactMethodClick(contactInfo, method)
                                                        onDismiss()
                                                    },
                                                    onLongClick = {
                                                        val action =
                                                            contactMethodToCardAction(
                                                                method,
                                                                selectedPhoneNumber,
                                                            )
                                                        val actionDisplayName =
                                                            methodShortcutLabel(context, method)
                                                        if (action != null && actionDisplayName != null) {
                                                            addToHomeHandler.addContactActionToHome(
                                                                contact = contactInfo,
                                                                contactAction = action,
                                                                actionDisplayName = actionDisplayName,
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    renderMethodRow(
                                        methods = methodsForSelectedNumber,
                                        methodTypes =
                                            listOf(
                                                ContactMethod.WhatsAppCall::class,
                                                ContactMethod.WhatsAppMessage::class,
                                                ContactMethod.WhatsAppVideoCall::class,
                                            ),
                                        onMethodClick = { method ->
                                            state.onContactMethodClick(contactInfo, method)
                                            onDismiss()
                                        },
                                        onMethodLongClick = { method ->
                                            val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                            val actionDisplayName = methodShortcutLabel(context, method)
                                            if (action != null && actionDisplayName != null) {
                                                addToHomeHandler.addContactActionToHome(
                                                    contact = contactInfo,
                                                    contactAction = action,
                                                    actionDisplayName = actionDisplayName,
                                                )
                                            }
                                        },
                                    )

                                    renderMethodRow(
                                        methods = methodsForSelectedNumber,
                                        methodTypes =
                                            listOf(
                                                ContactMethod.TelegramMessage::class,
                                                ContactMethod.TelegramCall::class,
                                                ContactMethod.TelegramVideoCall::class,
                                            ),
                                        onMethodClick = { method ->
                                            state.onContactMethodClick(contactInfo, method)
                                            onDismiss()
                                        },
                                        onMethodLongClick = { method ->
                                            val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                            val actionDisplayName = methodShortcutLabel(context, method)
                                            if (action != null && actionDisplayName != null) {
                                                addToHomeHandler.addContactActionToHome(
                                                    contact = contactInfo,
                                                    contactAction = action,
                                                    actionDisplayName = actionDisplayName,
                                                )
                                            }
                                        },
                                    )

                                    renderMethodRow(
                                        methods = methodsForSelectedNumber,
                                        methodTypes =
                                            listOf(
                                                ContactMethod.SignalMessage::class,
                                                ContactMethod.SignalCall::class,
                                                ContactMethod.SignalVideoCall::class,
                                            ),
                                        onMethodClick = { method ->
                                            state.onContactMethodClick(contactInfo, method)
                                            onDismiss()
                                        },
                                        onMethodLongClick = { method ->
                                            val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                            val actionDisplayName = methodShortcutLabel(context, method)
                                            if (action != null && actionDisplayName != null) {
                                                addToHomeHandler.addContactActionToHome(
                                                    contact = contactInfo,
                                                    contactAction = action,
                                                    actionDisplayName = actionDisplayName,
                                                )
                                            }
                                        },
                                    )

                                    if (contactInfo.contactMethods.filterNot { it is ContactMethod.Email }.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.contacts_no_methods_available),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                }

                                is ContactActionsPopupState.ReplaceAction -> {
                                    val onMethodClick: (ContactMethod) -> Unit = { method ->
                                        val action = contactMethodToCardAction(method, selectedPhoneNumber)
                                        action?.let {
                                            state.onActionSelected(it)
                                            onDismiss()
                                        }
                                    }

                                    if (firstRowMethods.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            firstRowMethods.forEach { method ->
                                                ContactActionButton(
                                                    method = method,
                                                    onClick = { onMethodClick(method) },
                                                    usePhoneIconForCallActions = true,
                                                )
                                            }
                                        }
                                    }

                                    renderMethodRow(
                                        methods = methodsForSelectedNumber,
                                        methodTypes =
                                            listOf(
                                                ContactMethod.WhatsAppCall::class,
                                                ContactMethod.WhatsAppMessage::class,
                                                ContactMethod.WhatsAppVideoCall::class,
                                            ),
                                        onMethodClick = onMethodClick,
                                    )
                                    renderMethodRow(
                                        methods = methodsForSelectedNumber,
                                        methodTypes =
                                            listOf(
                                                ContactMethod.TelegramMessage::class,
                                                ContactMethod.TelegramCall::class,
                                                ContactMethod.TelegramVideoCall::class,
                                            ),
                                        onMethodClick = onMethodClick,
                                    )
                                    renderMethodRow(
                                        methods = methodsForSelectedNumber,
                                        methodTypes =
                                            listOf(
                                                ContactMethod.SignalMessage::class,
                                                ContactMethod.SignalCall::class,
                                                ContactMethod.SignalVideoCall::class,
                                            ),
                                        onMethodClick = onMethodClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactActionsPopupHeader(
    state: ContactActionsPopupState,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            is ContactActionsPopupState.ContactActions -> {
                ContactAvatar(
                    photoUri = state.contactInfo.photoUri,
                    displayName = state.contactInfo.displayName,
                    onClick = { state.onAvatarClick(state.contactInfo) },
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = state.contactInfo.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            is ContactActionsPopupState.ReplaceAction -> {
                @Composable
                fun getActionDisplayName(action: ContactCardAction?): String {
                    if (action == null) return ""
                    return when (action) {
                        is ContactCardAction.Phone -> stringResource(R.string.contacts_action_button_call)
                        is ContactCardAction.Sms -> stringResource(R.string.contacts_action_button_message)
                        is ContactCardAction.WhatsAppCall -> stringResource(R.string.contact_method_whatsapp_voice_call_label)
                        is ContactCardAction.WhatsAppMessage -> stringResource(R.string.contact_method_whatsapp_message_label)
                        is ContactCardAction.WhatsAppVideoCall -> stringResource(R.string.contact_method_whatsapp_video_call_label)
                        is ContactCardAction.TelegramMessage -> stringResource(R.string.contact_method_telegram_message_label)
                        is ContactCardAction.TelegramCall -> stringResource(R.string.contact_method_telegram_voice_call_label)
                        is ContactCardAction.TelegramVideoCall -> stringResource(R.string.contact_method_telegram_video_call_label)
                        is ContactCardAction.SignalMessage -> stringResource(R.string.contact_method_signal_message_label)
                        is ContactCardAction.SignalCall -> stringResource(R.string.contact_method_signal_voice_call_label)
                        is ContactCardAction.SignalVideoCall -> stringResource(R.string.contact_method_signal_video_call_label)
                        is ContactCardAction.GoogleMeet -> stringResource(R.string.contact_method_google_meet_label)
                    }
                }

                Text(
                    text =
                        stringResource(
                            R.string.dialog_choose_contact_action_title,
                            "\"${getActionDisplayName(state.currentAction)}\"",
                        ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.dialog_cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
