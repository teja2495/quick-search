package com.tk.quicksearch.search.contacts.dialogs

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.contacts.components.ContactActionButton
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.contacts.components.getActionButtonLabel
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.PackageConstants.WHATSAPP_BUSINESS_PACKAGE
import com.tk.quicksearch.shared.util.PackageConstants.WHATSAPP_PACKAGE
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.runtime.rememberCoroutineScope
import com.tk.quicksearch.shared.util.writeText
import kotlinx.coroutines.launch

internal sealed interface ContactActionsPopupState {
    data class ContactActions(
        val contactInfo: ContactInfo,
        val onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
        val onAvatarClick: (ContactInfo) -> Unit,
        val enableContactActionTriggers: Boolean = false,
        val getContactActionTrigger: (ContactInfo, ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
            { _, _ -> null },
        val onContactActionTriggerClick: (ContactInfo, ContactCardAction, String) -> Unit =
            { _, _, _ -> },
    ) : ContactActionsPopupState

    data class ReplaceAction(
        val contactInfo: ContactInfo,
        val currentAction: ContactCardAction?,
        val onActionSelected: (ContactCardAction) -> Unit,
    ) : ContactActionsPopupState
}

internal const val MOLLY_PROVIDER_NAME = "Molly"
internal val TRAILING_PHONE_NUMBER_REGEX = Regex("\\s+(?:\\+?\\d[\\d()\\s.-]{5,})\\s*$")
internal val INLINE_PHONE_NUMBER_REGEX = Regex("\\+?\\d[\\d()\\s.-]{5,}")
internal val ACTION_SUFFIX_WORDING_REGEX =
    Regex("\\s+(?:chat|voice\\s+call|audio\\s+call|video\\s+call)\\b.*$", RegexOption.IGNORE_CASE)
internal val VOICE_CALL_WORDING_REGEX = Regex("\\s+voice\\s+call\\b.*$", RegexOption.IGNORE_CASE)
internal val VIDEO_CALL_WORDING_REGEX = Regex("\\s+video\\s+call\\b.*$", RegexOption.IGNORE_CASE)
internal val EMPTY_BRACKETS_REGEX = Regex("\\(\\s*\\)")
internal val ORPHAN_BRACKETS_REGEX = Regex("[()\\[\\]{}]")
private const val CONTACT_ACTIONS_MAX_CARD_HEIGHT_RATIO = 0.62f
private val NUMBER_SWIPE_THRESHOLD = 48.dp

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
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
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
    val normalizedContactMethods =
        remember(methodsForSelectedNumber) {
            // Contacts can keep data from apps that were uninstalled, which can't be opened anymore.
            remapSignalMessageToMollyCustomMethod(methodsForSelectedNumber)
                .filter { method -> method !is ContactMethod.CustomApp || context.canOpenCustomApp(method) }
        }
    val isRegularWhatsAppInstalled =
        remember(context) { context.isPackageInstalled(WHATSAPP_PACKAGE) }
    val isWhatsAppBusinessInstalled =
        remember(context) { context.isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE) }
    val normalizedMethodsForSelectedNumber = normalizedContactMethods
    val whatsAppBusinessMethods =
        normalizedMethodsForSelectedNumber.filter { method ->
            isWhatsAppBusinessInstalled && method.isWhatsAppBusinessMethod()
        }
    val showWhatsAppBusinessAsActions =
        whatsAppBusinessMethods.isNotEmpty() && !isRegularWhatsAppInstalled
    val firstRowMethods = mutableListOf<ContactMethod>()
    normalizedMethodsForSelectedNumber.find { it is ContactMethod.Phone }?.let { firstRowMethods.add(it) }
    normalizedMethodsForSelectedNumber.find { it is ContactMethod.Sms }?.let { firstRowMethods.add(it) }
    normalizedMethodsForSelectedNumber.find { it is ContactMethod.GoogleMeet }?.let { firstRowMethods.add(it) }
    val remainingMethods =
        normalizedMethodsForSelectedNumber
            .filterNot { it.isConfiguredPopupMethod() }
            .filterNot {
                it.isWhatsAppBusinessMethod() && !isWhatsAppBusinessInstalled
            }
            .filterNot { showWhatsAppBusinessAsActions && it.isWhatsAppBusinessMethod() }
            .filter { method -> method.hasDisplayNameAfterSanitization() }
            .sortedWith(compareBy<ContactMethod> { !it.isEmailOrWhatsAppBusinessMethod() })
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

    val dialogBackground = AppColors.DialogBackground
    val numberSwipeThresholdPx = with(LocalDensity.current) { NUMBER_SWIPE_THRESHOLD.toPx() }
    val numberSwipeDirection = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val switchNumberOnSwipe =
        if (reorderedPhoneNumbers.size > 1) {
            Modifier.pointerInput(reorderedPhoneNumbers, numberSwipeDirection) {
                var dragAmount = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onDragEnd = {
                        when {
                            dragAmount <= -numberSwipeThresholdPx ->
                                selectedPhoneIndex =
                                    (selectedPhoneIndex + 1).coerceAtMost(reorderedPhoneNumbers.lastIndex)
                            dragAmount >= numberSwipeThresholdPx ->
                                selectedPhoneIndex = (selectedPhoneIndex - 1).coerceAtLeast(0)
                        }
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragAmount += amount * numberSwipeDirection
                    },
                )
            }
        } else {
            Modifier
        }
    AppBottomPopup(
        onDismiss = onDismiss,
        // Swiping anywhere on the popup switches between the contact's numbers in reading order.
        modifier = switchNumberOnSwipe,
        animateSizeChanges = true,
        contentCardColor = dialogBackground,
        contentSpacing = DesignTokens.SpacingSmall,
        contentTopPadding = 0.dp,
        contentBottomPadding = 0.dp,
        contentHorizontalPadding = 0.dp,
        leadingContent =
            if (state is ContactActionsPopupState.ContactActions) {
                {
                    ContactAvatar(
                        photoUri = state.contactInfo.photoUri,
                        displayName = state.contactInfo.displayName,
                        onClick = { state.onAvatarClick(state.contactInfo) },
                        modifier = Modifier.size(DesignTokens.Spacing48),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                is ContactActionsPopupState.ReplaceAction ->
                    Text(
                        text = replaceActionTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
                    )
            }
        },
        aboveCardContent = {
            selectedPhoneNumber?.let { phoneNumber ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(DesignTokens.SpacingXSmall),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (reorderedPhoneNumbers.size > 1 && selectedPhoneIndex > 0) {
                        AppIconButton(
                            onClick = {
                                selectedPhoneIndex = (selectedPhoneIndex - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier.size(DesignTokens.Spacing48),
                            tooltip = stringResource(R.string.contacts_action_previous_number),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.ChevronLeft,
                                contentDescription = stringResource(R.string.contacts_action_previous_number),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(DesignTokens.Spacing48))
                    }

                    @OptIn(ExperimentalFoundationApi::class)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = DesignTokens.Spacing48)
                            .combinedClickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                                onLongClickLabel = stringResource(R.string.calculator_copy_result),
                                onLongClick = {
                                    clipboardScope.launch { clipboard.writeText(phoneNumber) }
                                },
                            )
                            .wrapContentHeight(Alignment.CenterVertically),
                    ) {
                        Text(
                            text = PhoneNumberUtils.formatPhoneNumberForDisplay(phoneNumber),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        selectedPhoneNumberLabel?.takeIf { it.isNotBlank() }?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (reorderedPhoneNumbers.size > 1 &&
                        selectedPhoneIndex < reorderedPhoneNumbers.size - 1
                    ) {
                        AppIconButton(
                            onClick = {
                                selectedPhoneIndex =
                                    (selectedPhoneIndex + 1).coerceAtMost(
                                        reorderedPhoneNumbers.size - 1,
                                    )
                            },
                            modifier = Modifier.size(DesignTokens.Spacing48),
                            tooltip = stringResource(R.string.contacts_action_next_number),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.ChevronRight,
                                contentDescription = stringResource(R.string.contacts_action_next_number),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(DesignTokens.Spacing48))
                    }
                }
            }
        },
        maxInnerCardHeight = maxInnerCardHeight,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
        when (state) {
            is ContactActionsPopupState.ContactActions -> {
                if (firstRowMethods.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        verticalAlignment = Alignment.Top,
                    ) {
                        firstRowMethods.forEach { method ->
                            ContactActionButtonWithLongPressMenu(
                                contact = contactInfo,
                                method = method,
                                selectedPhoneNumber = selectedPhoneNumber,
                                addToHomeHandler = addToHomeHandler,
                                getContactActionTrigger = state.getContactActionTrigger,
                                showTriggerAction = state.enableContactActionTriggers,
                                onContactActionTriggerClick = state.onContactActionTriggerClick,
                                onClick = {
                                    state.onContactMethodClick(contactInfo, method)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                ContactActionMethodRow(
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
                    contactInfo = contactInfo,
                    selectedPhoneNumber = selectedPhoneNumber,
                    addToHomeHandler = addToHomeHandler,
                    getContactActionTrigger = state.getContactActionTrigger,
                    showTriggerAction = state.enableContactActionTriggers,
                    onContactActionTriggerClick = state.onContactActionTriggerClick,
                )

                if (showWhatsAppBusinessAsActions) {
                    ContactActionMethodRow(
                        methods = whatsAppBusinessMethods,
                        methodTypes = listOf(ContactMethod.CustomApp::class),
                        onMethodClick = { method ->
                            state.onContactMethodClick(contactInfo, method)
                            onDismiss()
                        },
                        contactInfo = contactInfo,
                        selectedPhoneNumber = selectedPhoneNumber,
                        addToHomeHandler = addToHomeHandler,
                        getContactActionTrigger = state.getContactActionTrigger,
                        showTriggerAction = state.enableContactActionTriggers,
                        onContactActionTriggerClick = state.onContactActionTriggerClick,
                    )
                }

                ContactActionMethodRow(
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
                    contactInfo = contactInfo,
                    selectedPhoneNumber = selectedPhoneNumber,
                    addToHomeHandler = addToHomeHandler,
                    getContactActionTrigger = state.getContactActionTrigger,
                    showTriggerAction = state.enableContactActionTriggers,
                    onContactActionTriggerClick = state.onContactActionTriggerClick,
                )

                ContactActionMethodRow(
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
                    contactInfo = contactInfo,
                    selectedPhoneNumber = selectedPhoneNumber,
                    addToHomeHandler = addToHomeHandler,
                    getContactActionTrigger = state.getContactActionTrigger,
                    showTriggerAction = state.enableContactActionTriggers,
                    onContactActionTriggerClick = state.onContactActionTriggerClick,
                )

                if (remainingMethods.isNotEmpty()) {
                    RemainingMethodsList(
                        methods = remainingMethods,
                        onMethodClick = { method ->
                            state.onContactMethodClick(contactInfo, method)
                            onDismiss()
                        },
                        contactInfo = contactInfo,
                        selectedPhoneNumber = selectedPhoneNumber,
                        addToHomeHandler = addToHomeHandler,
                        getContactActionTrigger = state.getContactActionTrigger,
                        showTriggerAction = state.enableContactActionTriggers,
                        onContactActionTriggerClick = state.onContactActionTriggerClick,
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
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        verticalAlignment = Alignment.Top,
                    ) {
                        firstRowMethods.forEach { method ->
                            ContactActionButton(
                                method = method,
                                onClick = { onMethodClick(method) },
                                usePhoneIconForCallActions = true,
                                modifier = Modifier.weight(1f),
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
                if (showWhatsAppBusinessAsActions) {
                    renderMethodRow(
                        methods = whatsAppBusinessMethods,
                        methodTypes = listOf(ContactMethod.CustomApp::class),
                        onMethodClick = onMethodClick,
                    )
                }
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
}
