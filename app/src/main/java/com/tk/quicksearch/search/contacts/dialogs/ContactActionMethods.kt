package com.tk.quicksearch.search.contacts.dialogs

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
import com.tk.quicksearch.shared.util.PackageConstants.WHATSAPP_BUSINESS_PACKAGE
import com.tk.quicksearch.shared.util.PackageConstants.WHATSAPP_PACKAGE
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.basicMarquee

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun RemainingMethodsList(
    methods: List<ContactMethod>,
    onMethodClick: (ContactMethod) -> Unit,
    onMethodLongClick: ((ContactMethod) -> Unit)? = null,
    contactInfo: ContactInfo? = null,
    selectedPhoneNumber: String? = null,
    addToHomeHandler: AddToHomeHandler? = null,
    getContactActionTrigger: (ContactInfo, ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? =
        { _, _ -> null },
    showTriggerAction: Boolean = false,
    onContactActionTriggerClick: (ContactInfo, ContactCardAction, String) -> Unit =
        { _, _, _ -> },
) {
    val view = LocalView.current
    Column(modifier = Modifier.fillMaxWidth()) {
        methods.forEach { method ->
            val action = contactMethodToCardAction(method, selectedPhoneNumber)
            val actionDisplayName = methodShortcutLabel(LocalContext.current, method)
            val hasLongPressMenu =
                contactInfo != null &&
                    addToHomeHandler != null &&
                    action != null &&
                    actionDisplayName != null
            var showMenu by remember { mutableStateOf(false) }
            val onLongClick: (() -> Unit)? =
                if (hasLongPressMenu) {
                    { showMenu = true }
                } else {
                    onMethodLongClick?.let { { it(method) } }
                }
            // Matches the list rows in the shared item long-press menu.
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(DesignTokens.ShapeSmall)
                            .combinedClickable(
                                onClick = { onMethodClick(method) },
                                onLongClick =
                                    onLongClick?.let { onLongClick ->
                                        {
                                            hapticConfirm(view)()
                                            onLongClick()
                                        }
                                    },
                            ).padding(vertical = DesignTokens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                ) {
                    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        ContactMethodIcon(
                            method = method,
                            iconSize = 22.dp,
                            tintOverride = AppColors.DialogText,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text =
                                if (method.isWhatsAppBusinessMethod()) {
                                    getActionButtonLabel(method)
                                } else {
                                    getRemainingMethodLabel(method)
                                },
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.DialogText,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee(),
                        )
                        if (method.isWhatsAppBusinessMethod()) {
                            Text(
                                text = stringResource(R.string.contact_method_whatsapp_business_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (
                    contactInfo != null &&
                    addToHomeHandler != null &&
                    action != null &&
                    actionDisplayName != null
                ) {
                    // Matches the row's bounds so the dropdown positions against it.
                    Box(modifier = Modifier.matchParentSize()) {
                        ContactActionLongPressMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            contact = contactInfo,
                            action = action,
                            actionDisplayName = actionDisplayName,
                            addToHomeHandler = addToHomeHandler,
                            hasTrigger =
                                getContactActionTrigger(
                                    contactInfo,
                                    action,
                                )?.word?.isNotBlank() == true,
                            showTriggerAction = showTriggerAction,
                            onContactActionTriggerClick = onContactActionTriggerClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ContactActionMethodRow(
    methods: List<ContactMethod>,
    methodTypes: List<kotlin.reflect.KClass<out ContactMethod>>,
    onMethodClick: (ContactMethod) -> Unit,
    contactInfo: ContactInfo,
    selectedPhoneNumber: String?,
    addToHomeHandler: AddToHomeHandler,
    getContactActionTrigger: (ContactInfo, ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    showTriggerAction: Boolean,
    onContactActionTriggerClick: (ContactInfo, ContactCardAction, String) -> Unit,
) {
    val methodTypeOrder = methodTypes.withIndex().associate { (index, type) -> type to index }
    val filteredMethods =
        methods.filter { method ->
            methodTypes.any { type -> type.isInstance(method) }
        }.sortedBy { method ->
            methodTypeOrder.entries.firstOrNull { (type, _) -> type.isInstance(method) }?.value
                ?: Int.MAX_VALUE
        }

    if (filteredMethods.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.Top,
        ) {
            filteredMethods.forEach { method ->
                ContactActionButtonWithLongPressMenu(
                    contact = contactInfo,
                    method = method,
                    selectedPhoneNumber = selectedPhoneNumber,
                    addToHomeHandler = addToHomeHandler,
                    getContactActionTrigger = getContactActionTrigger,
                    showTriggerAction = showTriggerAction,
                    onContactActionTriggerClick = onContactActionTriggerClick,
                    onClick = { onMethodClick(method) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun ContactActionButtonWithLongPressMenu(
    contact: ContactInfo,
    method: ContactMethod,
    selectedPhoneNumber: String?,
    addToHomeHandler: AddToHomeHandler,
    getContactActionTrigger: (ContactInfo, ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    showTriggerAction: Boolean,
    onContactActionTriggerClick: (ContactInfo, ContactCardAction, String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val action = contactMethodToCardAction(method, selectedPhoneNumber)
    val actionDisplayName = methodShortcutLabel(context, method)
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ContactActionButton(
            method = method,
            onClick = onClick,
            onLongClick =
                if (action != null && actionDisplayName != null) {
                    { showMenu = true }
                } else {
                    null
                },
        )
        if (action != null && actionDisplayName != null) {
            ContactActionLongPressMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                contact = contact,
                action = action,
                actionDisplayName = actionDisplayName,
                addToHomeHandler = addToHomeHandler,
                hasTrigger = getContactActionTrigger(contact, action)?.word?.isNotBlank() == true,
                showTriggerAction = showTriggerAction,
                onContactActionTriggerClick = onContactActionTriggerClick,
            )
        }
    }
}

@Composable
private fun ContactActionLongPressMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    contact: ContactInfo,
    action: ContactCardAction,
    actionDisplayName: String,
    addToHomeHandler: AddToHomeHandler,
    hasTrigger: Boolean,
    showTriggerAction: Boolean,
    onContactActionTriggerClick: (ContactInfo, ContactCardAction, String) -> Unit,
) {
    val context = LocalContext.current
    val isDefaultLauncher = context.cachedDefaultHomeAppStatus()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 8.dp),
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false),
        containerColor = if (LocalAppIsDarkTheme.current) Color.Black else Color.White,
    ) {
        if (!isDefaultLauncher) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_add_to_home)) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = null) },
                onClick = {
                    onDismissRequest()
                    addToHomeHandler.addContactActionToHome(
                        contact = contact,
                        contactAction = action,
                        actionDisplayName = actionDisplayName,
                    )
                },
            )
        }
        if (showTriggerAction) {
            if (!isDefaultLauncher) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text =
                            stringResource(
                                if (hasTrigger) {
                                    R.string.action_edit_trigger
                                } else {
                                    R.string.action_add_trigger
                                },
                            ),
                    )
                },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null) },
                onClick = {
                    onDismissRequest()
                    onContactActionTriggerClick(contact, action, actionDisplayName)
                },
            )
        }
    }
}

internal fun remapSignalMessageToMollyCustomMethod(methods: List<ContactMethod>): List<ContactMethod> {
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

internal fun ContactMethod.hasDisplayNameAfterSanitization(): Boolean =
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

internal fun ContactMethod.isWhatsAppBusinessMethod(): Boolean =
    this is ContactMethod.CustomApp &&
        packageName == WHATSAPP_BUSINESS_PACKAGE &&
        mimeType in
            setOf(
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL,
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_MESSAGE,
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL,
            )

internal fun ContactMethod.isEmailOrWhatsAppBusinessMethod(): Boolean =
    this is ContactMethod.Email || isWhatsAppBusinessMethod()

/** True when the method's app is installed, or another app can handle its contact data. */
internal fun android.content.Context.canOpenCustomApp(method: ContactMethod.CustomApp): Boolean {
    if (method.packageName?.let(::isPackageInstalled) == true) return true
    val dataUri =
        method.dataId?.let { ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, it) }
            ?: runCatching { Uri.parse(method.data) }.getOrNull()
            ?: return false
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(dataUri, method.mimeType)
    return runCatching { intent.resolveActivity(packageManager) != null }.getOrDefault(false)
}

internal fun android.content.Context.isPackageInstalled(packageName: String): Boolean =
    runCatching {
        packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess
