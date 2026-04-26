package com.tk.quicksearch.search.contacts.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.contacts.components.ContactMethodIcon
import com.tk.quicksearch.search.contacts.components.getActionButtonLabel
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.shared.ui.components.AppBottomPopup

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

private const val MOLLY_PROVIDER_NAME = "Molly"
private val TRAILING_PHONE_NUMBER_REGEX = Regex("\\s+(?:\\+?\\d[\\d()\\s.-]{5,})\\s*$")
private val INLINE_PHONE_NUMBER_REGEX = Regex("\\+?\\d[\\d()\\s.-]{5,}")
private val ACTION_SUFFIX_WORDING_REGEX =
    Regex("\\s+(?:chat|voice\\s+call|audio\\s+call|video\\s+call)\\b.*$", RegexOption.IGNORE_CASE)
private val VOICE_CALL_WORDING_REGEX = Regex("\\s+voice\\s+call\\b.*$", RegexOption.IGNORE_CASE)
private val VIDEO_CALL_WORDING_REGEX = Regex("\\s+video\\s+call\\b.*$", RegexOption.IGNORE_CASE)
private val EMPTY_BRACKETS_REGEX = Regex("\\(\\s*\\)")
private val ORPHAN_BRACKETS_REGEX = Regex("[()\\[\\]{}]")
private const val CONTACT_ACTIONS_MAX_CARD_HEIGHT_RATIO = 0.62f

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
    val configuration = LocalConfiguration.current
    val clipboardManager = LocalClipboardManager.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
    val maxInnerCardHeight = configuration.screenHeightDp.dp * CONTACT_ACTIONS_MAX_CARD_HEIGHT_RATIO

    val reorderedPhoneNumbers =
        remember(contactInfo.phoneNumbers, contactInfo.contactId, hasMultipleNumbers) {
            reorderPhoneNumbersForDisplay(contactInfo, hasMultipleNumbers, getLastShownPhoneNumber)
        }
    var selectedPhoneIndex by remember { mutableStateOf(0) }
    val selectedPhoneNumber =
        reorderedPhoneNumbers.getOrNull(selectedPhoneIndex) ?: contactInfo.primaryNumber
    val selectedPhoneNumberLabel =
        if (hasMultipleNumbers) {
            selectedPhoneNumber?.let { contactInfo.phoneNumberLabel(it) }
        } else {
            null
        }

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
            hasMultipleNumbers = hasMultipleNumbers,
        )
    val normalizedMethodsForSelectedNumber =
        remember(methodsForSelectedNumber) {
            remapSignalMessageToMollyCustomMethod(methodsForSelectedNumber)
        }
    val firstRowMethods = mutableListOf<ContactMethod>()
    normalizedMethodsForSelectedNumber.find { it is ContactMethod.Phone }?.let { firstRowMethods.add(it) }
    normalizedMethodsForSelectedNumber.find { it is ContactMethod.Sms }?.let { firstRowMethods.add(it) }
    normalizedMethodsForSelectedNumber.find { it is ContactMethod.GoogleMeet }?.let { firstRowMethods.add(it) }
    val remainingMethods =
        normalizedMethodsForSelectedNumber
            .filterNot { it.isConfiguredPopupMethod() }
            .filter { method -> method.hasDisplayNameAfterSanitization() }
            .distinctBy { method ->
                "${method::class.java.name}:${method.dataId}:${method.data}:${method.displayLabel}"
            }

    // Precompute ReplaceAction title so stringResource can be called in composable scope
    val replaceActionTitle =
        if (state is ContactActionsPopupState.ReplaceAction) {
                val actionDisplayName =
                when (val action = state.currentAction) {
                    is ContactCardAction.Phone -> stringResource(R.string.contact_method_call_label)
                    is ContactCardAction.Sms -> stringResource(R.string.contact_method_message_label)
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
                    is ContactCardAction.Email -> stringResource(R.string.contact_method_email_label)
                    is ContactCardAction.VideoCall -> stringResource(R.string.contacts_action_button_video_call)
                    is ContactCardAction.CustomApp -> action.displayLabel
                    is ContactCardAction.ViewInContactsApp -> stringResource(R.string.contacts_action_button_contacts)
                    null -> ""
                }
            stringResource(R.string.dialog_choose_contact_action_title, "\"$actionDisplayName\"")
        } else {
            ""
        }

    AppBottomPopup(
        onDismiss = onDismiss,
        leadingContent =
            if (state is ContactActionsPopupState.ContactActions) {
                {
                    ContactAvatar(
                        photoUri = state.contactInfo.photoUri,
                        displayName = state.contactInfo.displayName,
                        onClick = { state.onAvatarClick(state.contactInfo) },
                        modifier = Modifier.size(48.dp),
                    )
                }
            } else {
                null
            },
        title = {
            when (state) {
                is ContactActionsPopupState.ContactActions ->
                    Text(
                        text = state.contactInfo.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                is ContactActionsPopupState.ReplaceAction ->
                    Text(
                        text = replaceActionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 16.dp),
                    )
            }
        },
        fixedTopContent = {
            selectedPhoneNumber?.let { phoneNumber ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex > 0) {
                        IconButton(
                            onClick = {
                                selectedPhoneIndex = (selectedPhoneIndex - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = stringResource(R.string.contacts_action_previous_number),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                    ) {
                        @OptIn(ExperimentalFoundationApi::class)
                        Text(
                            text = PhoneNumberUtils.formatPhoneNumberForDisplay(phoneNumber),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {},
                                        onLongClick = {
                                            clipboardManager.setText(AnnotatedString(phoneNumber))
                                        },
                                    ),
                        )

                        selectedPhoneNumberLabel?.takeIf { it.isNotBlank() }?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

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
                                contentDescription = stringResource(R.string.contacts_action_next_number),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }
            }
        },
        showFixedTopDivider = false,
        maxInnerCardHeight = maxInnerCardHeight,
    ) {
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
                    methods = normalizedMethodsForSelectedNumber,
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
                    methods = normalizedMethodsForSelectedNumber,
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
                    methods = normalizedMethodsForSelectedNumber,
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

                if (remainingMethods.isNotEmpty()) {
                    RemainingMethodsList(
                        methods = remainingMethods,
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
                }

                if (normalizedMethodsForSelectedNumber.isEmpty()) {
                    Text(
                        text = stringResource(R.string.contacts_no_methods_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    methods = normalizedMethodsForSelectedNumber,
                    methodTypes =
                        listOf(
                            ContactMethod.WhatsAppCall::class,
                            ContactMethod.WhatsAppMessage::class,
                            ContactMethod.WhatsAppVideoCall::class,
                        ),
                    onMethodClick = onMethodClick,
                )
                renderMethodRow(
                    methods = normalizedMethodsForSelectedNumber,
                    methodTypes =
                        listOf(
                            ContactMethod.TelegramMessage::class,
                            ContactMethod.TelegramCall::class,
                            ContactMethod.TelegramVideoCall::class,
                        ),
                    onMethodClick = onMethodClick,
                )
                renderMethodRow(
                    methods = normalizedMethodsForSelectedNumber,
                    methodTypes =
                        listOf(
                            ContactMethod.SignalMessage::class,
                            ContactMethod.SignalCall::class,
                            ContactMethod.SignalVideoCall::class,
                    ),
                    onMethodClick = onMethodClick,
                )

                if (remainingMethods.isNotEmpty()) {
                    RemainingMethodsList(
                        methods = remainingMethods,
                        onMethodClick = onMethodClick,
                    )
                }

                if (normalizedMethodsForSelectedNumber.isEmpty()) {
                    Text(
                        text = stringResource(R.string.contacts_no_methods_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RemainingMethodsList(
    methods: List<ContactMethod>,
    onMethodClick: (ContactMethod) -> Unit,
    onMethodLongClick: ((ContactMethod) -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        methods.forEachIndexed { index, method ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onMethodClick(method) },
                                onLongClick = onMethodLongClick?.let { { it(method) } },
                            )
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContactMethodIcon(
                        method = method,
                        iconSize = 20.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getRemainingMethodLabel(method),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (index != methods.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

private fun remapSignalMessageToMollyCustomMethod(methods: List<ContactMethod>): List<ContactMethod> {
    val firstMollyMethod =
        methods.firstOrNull { method ->
            method is ContactMethod.CustomApp && method.isMollyProvider()
        } as? ContactMethod.CustomApp ?: return methods

    val alreadyHasMollyMessage =
        methods.any { method ->
            method is ContactMethod.CustomApp &&
                method.isMollyProvider() &&
                method.mimeType == ContactMethodMimeTypes.SIGNAL_MESSAGE
        }
    if (alreadyHasMollyMessage) return methods

    val signalMessageIndex = methods.indexOfFirst { it is ContactMethod.SignalMessage }
    if (signalMessageIndex < 0) return methods

    val signalMessage = methods[signalMessageIndex] as ContactMethod.SignalMessage
    val providerName = firstMollyMethod.sanitizedProviderNameOrNull() ?: return methods
    val remappedMethod =
        ContactMethod.CustomApp(
            displayLabel = "$providerName Chat",
            data = signalMessage.data,
            mimeType = ContactMethodMimeTypes.SIGNAL_MESSAGE,
            packageName = firstMollyMethod.packageName,
            dataId = signalMessage.dataId,
            isPrimary = signalMessage.isPrimary,
        )

    val remappedMethods = methods.toMutableList()
    remappedMethods.removeAt(signalMessageIndex)
    val firstMollyIndex =
        remappedMethods.indexOfFirst { method ->
            method is ContactMethod.CustomApp && method.isMollyProvider()
        }
    val insertIndex = if (firstMollyIndex >= 0) firstMollyIndex else remappedMethods.size
    remappedMethods.add(insertIndex, remappedMethod)
    return remappedMethods
}

private fun ContactMethod.CustomApp.isMollyProvider(): Boolean =
    packageName?.contains("molly", ignoreCase = true) == true ||
        displayLabel.contains("molly", ignoreCase = true)

private fun ContactMethod.CustomApp.providerName(): String {
    val appNameFromLabel =
        sanitizedProviderNameOrNull()
    return appNameFromLabel ?: MOLLY_PROVIDER_NAME
}

private fun ContactMethod.CustomApp.sanitizedProviderNameOrNull(): String? =
    sanitizedDisplayLabel()
        .replace(ACTION_SUFFIX_WORDING_REGEX, "")
        .trim()
        .takeIf { it.isNotBlank() }

@Composable
private fun getRemainingMethodLabel(method: ContactMethod): String =
    when (method) {
        is ContactMethod.CustomApp -> {
            val providerName = method.sanitizedProviderNameOrNull()
            when (method.mimeType) {
                ContactMethodMimeTypes.SIGNAL_MESSAGE -> providerName?.let { "$it Chat" }.orEmpty()
                ContactMethodMimeTypes.SIGNAL_CALL -> providerName?.let { "$it Audio Call" }.orEmpty()
                ContactMethodMimeTypes.SIGNAL_VIDEO_CALL -> providerName?.let { "$it Video Call" }.orEmpty()
                else -> method.sanitizedDisplayLabel()
            }
        }
        is ContactMethod.Email -> method.data
        else -> getActionButtonLabel(method)
    }

private fun ContactMethod.hasDisplayNameAfterSanitization(): Boolean =
    when (this) {
        is ContactMethod.CustomApp ->
            when (mimeType) {
                ContactMethodMimeTypes.SIGNAL_MESSAGE,
                ContactMethodMimeTypes.SIGNAL_CALL,
                ContactMethodMimeTypes.SIGNAL_VIDEO_CALL,
                -> sanitizedProviderNameOrNull() != null
                else -> sanitizedDisplayLabel().isNotBlank()
            }
        else -> true
    }

private fun ContactMethod.CustomApp.sanitizedDisplayLabel(): String =
    displayLabel
        .replace(TRAILING_PHONE_NUMBER_REGEX, "")
        .replace(INLINE_PHONE_NUMBER_REGEX, "")
        .replace(VOICE_CALL_WORDING_REGEX, "")
        .replace(VIDEO_CALL_WORDING_REGEX, "")
        .replace(EMPTY_BRACKETS_REGEX, "")
        .replace(ORPHAN_BRACKETS_REGEX, "")
        .replace("\\s+".toRegex(), " ")
        .trim()
