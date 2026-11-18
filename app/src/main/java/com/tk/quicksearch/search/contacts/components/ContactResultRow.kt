package com.tk.quicksearch.search.contacts.components

import com.tk.quicksearch.search.contacts.models.ContactCardAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.search.contacts.contactInitials
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.core.LocalItemCustomizationRemover
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.resultRowVerticalPadding
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.components.AppVoiceCallIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.theme.AppShapes

// ============================================================================
// Contact Row
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactResultRow(
        contactInfo: ContactInfo,
        primaryContactButton: ContactButtonAction,
        secondaryContactButton: ContactButtonAction,
        primaryAction: ContactCardAction? = null,
        secondaryAction: ContactCardAction? = null,
        onContactClick: (ContactInfo) -> Unit,
        onShowContactMethods: (ContactInfo) -> Unit = {},
        onCallContact: (ContactInfo) -> Unit,
        onSmsContact: (ContactInfo) -> Unit,
        onPrimaryActionLongPress: (ContactInfo) -> Unit = {},
        onSecondaryActionLongPress: (ContactInfo) -> Unit = {},
        onCustomAction:
                (ContactInfo, ContactCardAction) -> Unit =
                { _, _ ->
                },
        onContactMethodClick: (ContactMethod) -> Unit,
        isPinned: Boolean = false,
        onTogglePin: (ContactInfo) -> Unit = {},
        onMovePinned: (ContactInfo, Boolean) -> Unit = { _, _ -> },
        onExclude: (ContactInfo) -> Unit = {},
        onNicknameClick: (ContactInfo) -> Unit = {},
        hasNickname: Boolean = false,
        onTriggerClick: (ContactInfo) -> Unit = {},
        hasTrigger: Boolean = false,
        enableLongPress: Boolean = true,
        onLongPressOverride: (() -> Unit)? = null,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        iconTint: Color = MaterialTheme.colorScheme.secondary,
        isPredicted: Boolean = false,
        showPinnedItemMenu: Boolean = false,
) {
        val context = LocalContext.current
        val customizationRemover = LocalItemCustomizationRemover.current
        val addToHomeHandler =
                remember(context) { com.tk.quicksearch.search.common.AddToHomeHandler(context) }
        var showOptions by remember { mutableStateOf(false) }
        val view = LocalView.current
        val hasNumber = contactInfo.primaryNumber != null
        Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .heightIn(
                                                        min =
                                                                ContactUiConstants
                                                                        .CONTACT_ROW_MIN_HEIGHT
                                                                        .dp,
                                                )
                                                .topPredictedRowContainer(
                                                        isTopPredicted = isPredicted,
                                                )
                                                .combinedClickable(
                                                        role = Role.Button,
                                                        onClick = {
                                                                if (contactInfo.hasContactMethods) {
                                                                        onShowContactMethods(
                                                                                contactInfo,
                                                                        )
                                                                } else {
                                                                        onContactClick(contactInfo)
                                                                }
                                                        },
                                                        onLongClickLabel = stringResource(R.string.desc_show_options),
                                                        onLongClick = onLongPressOverride
                                                                        ?: if (enableLongPress) {
                                                                                {
                                                                                        showOptions =
                                                                                                true
                                                                                }
                                                                        } else {
                                                                                null
                                                                        },
                                                )
                                                .padding(vertical = resultRowVerticalPadding(DesignTokens.SpacingSmall)),
                                horizontalArrangement =
                                        Arrangement.spacedBy(DesignTokens.SpacingMedium),
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                                Box(
                                        modifier =
                                                Modifier.padding(
                                                        start = DesignTokens.SpacingXSmall
                                                ),
                                ) {
                                        if (icon != null) {
                                                Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = iconTint,
                                                        modifier =
                                                                Modifier.size(DesignTokens.IconSize + DesignTokens.SpacingXSmall)
                                                                        .padding(
                                                                                start =
                                                                                        DesignTokens
                                                                                                .SpacingXSmall,
                                                                        ),
                                                )
                                        } else {
                                                ContactAvatar(
                                                        photoUri = contactInfo.photoUri,
                                                        displayName = contactInfo.displayName,
                                                        onClick = { onContactClick(contactInfo) },
                                                )
                                        }
                                }

                                Text(
                                        text = rememberQueryHighlightedText(contactInfo.displayName),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                )

                                // Always show call and message action buttons
                                ContactActionButtons(
                                        hasNumber = hasNumber,
                                        primaryContactButton = primaryContactButton,
                                        secondaryContactButton = secondaryContactButton,
                                        primaryAction = primaryAction,
                                        secondaryAction = secondaryAction,
                                        onCallClick = { onCallContact(contactInfo) },
                                        onSmsClick = { onSmsContact(contactInfo) },
                                        onPrimaryLongPress = {
                                                onPrimaryActionLongPress(contactInfo)
                                        },
                                        onSecondaryLongPress = {
                                                onSecondaryActionLongPress(contactInfo)
                                        },
                                        onCustomAction = { action ->
                                                onCustomAction(contactInfo, action)
                                        },
                                )
                        }
                }

                if (enableLongPress && onLongPressOverride == null) {
                        val notificationAction =
                                CustomWidgetButtonAction.Contact(
                                        contactId = contactInfo.contactId,
                                        lookupKey = contactInfo.lookupKey,
                                        displayName = contactInfo.displayName,
                                        photoUri = contactInfo.photoUri,
                                )
                        ContactDropdownMenu(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                displayName = contactInfo.displayName,
                                photoUri = contactInfo.photoUri,
                                isPinned = isPinned,
                                hasNickname = hasNickname,
                                hasTrigger = hasTrigger,
                                onTogglePin = { onTogglePin(contactInfo) },
                                onMoveUp = { onMovePinned(contactInfo, true) },
                                onMoveDown = { onMovePinned(contactInfo, false) },
                                onExclude = { onExclude(contactInfo) },
                                onNicknameClick = { onNicknameClick(contactInfo) },
                                onTriggerClick = { onTriggerClick(contactInfo) },
                                onRemoveNickname = customizationRemover?.let { remover -> { remover.removeContactNickname(contactInfo) } },
                                onRemoveTrigger = customizationRemover?.let { remover -> { remover.removeContactTrigger(contactInfo) } },
                                onAddToHome = { addToHomeHandler.addContactToHome(contactInfo) },
                                onPinToNotifications = { PinnedNotifications.toggle(context, notificationAction) },
                                isPinnedToNotifications = PinnedNotifications.isPinned(context, notificationAction),
                                showPinnedItemMenu = showPinnedItemMenu,
                        )
                }
        }
}

// ============================================================================
// Contact Avatar
// ============================================================================

@Composable
internal fun ContactAvatar(
        photoUri: String?,
        displayName: String,
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier.size(ContactUiConstants.CONTACT_AVATAR_SIZE.dp),
        textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
        val context = LocalContext.current
        // Decode at the drawn size; full-resolution display photos would otherwise fill the heap.
        var photoSizePx by remember { mutableIntStateOf(0) }
        val contactPhoto by
                produceState(
                        initialValue =
                                photoUri?.takeIf { photoSizePx > 0 }?.let {
                                        ContactImageCache.cachedPhoto(it, photoSizePx)
                                },
                        key1 = photoUri,
                        key2 = photoSizePx,
                ) {
                        val uri = photoUri
                        if (uri == null || photoSizePx <= 0) {
                                value = null
                                return@produceState
                        }
                        value =
                                ContactImageCache.cachedPhoto(uri, photoSizePx)
                                        ?: withContext(Dispatchers.IO) {
                                                ContactImageCache.loadPhoto(context, uri, photoSizePx)
                                        }
                }

        val placeholderInitials =
                remember(displayName) {
                        contactInitials(displayName)
                }

        Surface(
                modifier =
                        modifier
                                .onSizeChanged { size ->
                                        val sizePx = maxOf(size.width, size.height)
                                        if (sizePx > 0 && sizePx != photoSizePx) photoSizePx = sizePx
                                }
                                .then(
                                        if (onClick != null) {
                                                // Keeps the press ripple inside the circle.
                                                Modifier.clip(AppShapes.full)
                                                    .clickable(role = Role.Button, onClick = onClick)
                                                    .semantics { contentDescription = displayName }
                                        } else {
                                                Modifier
                                        },
                                ),
                shape = AppShapes.full,
                color = MaterialTheme.colorScheme.primaryContainer,
        ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        contactPhoto?.let { photo ->
                                Image(
                                        bitmap = photo,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                )
                        }
                                ?: run {
                                        Text(
                                                text = placeholderInitials,
                                                style = textStyle,
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .onPrimaryContainer,
                                                fontWeight = FontWeight.SemiBold,
                                        )
                                }
                }
        }
}

// ============================================================================
// Action Buttons
// ============================================================================

@Composable
private fun ContactActionButtons(
        hasNumber: Boolean,
        primaryContactButton: ContactButtonAction,
        secondaryContactButton: ContactButtonAction,
        primaryAction: ContactCardAction?,
        secondaryAction: ContactCardAction?,
        onCallClick: () -> Unit,
        onSmsClick: () -> Unit,
        onPrimaryLongPress: () -> Unit,
        onSecondaryLongPress: () -> Unit,
        onCustomAction: (ContactCardAction) -> Unit,
) {
        val view = LocalView.current
        // A resolved Email action means the contact has an email address, even without a number.
        val primaryEnabled =
                hasNumber || (primaryAction == null && primaryContactButton == ContactButtonAction.EMAIL)
        val secondaryEnabled =
                hasNumber || (secondaryAction == null && secondaryContactButton == ContactButtonAction.EMAIL)

        // Helper to render action button with consistent styling and long press support
        @Composable
        fun ActionButton(
                icon: @Composable () -> Unit, // Icon content
                contentDescription: String,
                onClick: () -> Unit,
                onLongClick: () -> Unit,
                enabled: Boolean = true,
        ) {
                Box(
                        modifier =
                                Modifier.size(DesignTokens.MinTouchTarget)
                                        .clip(AppShapes.full)
                                        .then(
                                                if (enabled) {
                                                        Modifier.combinedClickable(
                                                                role = Role.Button,
                                                                onClickLabel = contentDescription,
                                                                onClick = {
                                                                        hapticConfirm(view)()
                                                                        onClick()
                                                                },
                                                                onLongClickLabel = stringResource(R.string.desc_show_options),
                                                                onLongClick = {
                                                                        hapticConfirm(view)()
                                                                        onLongClick()
                                                                },
                                                        )
                                                } else {
                                                        Modifier
                                                },
                                        ).semantics { this.contentDescription = contentDescription },
                        contentAlignment = Alignment.Center,
                ) { icon() }
        }

        // --- Primary Action (Left) ---
        ActionButton(
                icon = {
                        if (primaryAction != null) {
                                ContactActionIconForButton(
                                        action = primaryAction,
                                        enabled = hasNumber,
                                )
                        } else {
                                ContactButtonActionIcon(
                                        action = primaryContactButton,
                                        enabled = primaryEnabled,
                                )
                        }
                },
                contentDescription = contactActionLabel(primaryAction, primaryContactButton),
                onClick = {
                        if (primaryAction != null) {
                                onCustomAction(primaryAction)
                        } else {
                                onCallClick()
                        }
                },
                onLongClick = onPrimaryLongPress,
                enabled = primaryEnabled,
        )

        // --- Secondary Action (Right) ---
        ActionButton(
                icon = {
                        if (secondaryAction != null) {
                                // Use custom icon logic
                                ContactActionIconForButton(
                                        action = secondaryAction,
                                        enabled = hasNumber,
                                )
                        } else {
                                ContactButtonActionIcon(
                                        action = secondaryContactButton,
                                        enabled = secondaryEnabled,
                                )
                        }
                },
                contentDescription = contactActionLabel(secondaryAction, secondaryContactButton),
                onClick = {
                        if (secondaryAction != null) {
                                onCustomAction(secondaryAction)
                        } else {
                                onSmsClick()
                        }
                },
                onLongClick = onSecondaryLongPress,
                enabled = secondaryEnabled,
        )
}

@Composable
private fun contactActionLabel(action: ContactCardAction?, default: ContactButtonAction): String =
    when (action) {
        null -> stringResource(default.labelRes)
        is ContactCardAction.Phone -> stringResource(ContactButtonAction.CALL.labelRes)
        is ContactCardAction.Sms -> stringResource(ContactButtonAction.SMS.labelRes)
        is ContactCardAction.Email -> stringResource(ContactButtonAction.EMAIL.labelRes)
        is ContactCardAction.WhatsAppCall -> stringResource(ContactButtonAction.WHATSAPP_CALL.labelRes)
        is ContactCardAction.WhatsAppMessage -> stringResource(ContactButtonAction.WHATSAPP_MESSAGE.labelRes)
        is ContactCardAction.WhatsAppVideoCall -> stringResource(ContactButtonAction.WHATSAPP_VIDEO_CALL.labelRes)
        is ContactCardAction.TelegramCall -> stringResource(ContactButtonAction.TELEGRAM_CALL.labelRes)
        is ContactCardAction.TelegramMessage -> stringResource(ContactButtonAction.TELEGRAM_MESSAGE.labelRes)
        is ContactCardAction.TelegramVideoCall -> stringResource(ContactButtonAction.TELEGRAM_VIDEO_CALL.labelRes)
        is ContactCardAction.SignalCall -> stringResource(ContactButtonAction.SIGNAL_CALL.labelRes)
        is ContactCardAction.SignalMessage -> stringResource(ContactButtonAction.SIGNAL_MESSAGE.labelRes)
        is ContactCardAction.SignalVideoCall -> stringResource(ContactButtonAction.SIGNAL_VIDEO_CALL.labelRes)
        is ContactCardAction.GoogleMeet -> stringResource(ContactButtonAction.GOOGLE_MEET.labelRes)
        is ContactCardAction.VideoCall -> stringResource(R.string.contacts_action_button_video_call)
        is ContactCardAction.ViewInContactsApp -> stringResource(R.string.contact_method_view_in_contacts_label)
        is ContactCardAction.CustomApp -> action.displayLabel.ifBlank { stringResource(R.string.contact_method_fallback_label) }
    }

@Composable
private fun ContactActionIconForButton(
        action: ContactCardAction,
        enabled: Boolean,
) {
        val tint = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant
        val callIconTint = if (enabled) AppColors.CallIconTint else MaterialTheme.colorScheme.onSurfaceVariant
        val modifier = Modifier.size(ContactUiConstants.ACTION_ICON_SIZE.dp)
        val smsModifier = Modifier.size((ContactUiConstants.ACTION_ICON_SIZE * 0.9f).dp)

        when (action) {
                // Calls -> Phone Icon
                is ContactCardAction.Phone -> {
                        Icon(
                                imageVector = MaterialSymbols.Call, // Explicit request for phone icon
                                contentDescription = null,
                                tint = callIconTint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.WhatsAppCall -> {
                        AppVoiceCallIcon(
                                logoPainterRes = R.drawable.whatsapp_call,
                                size = ContactUiConstants.ACTION_ICON_SIZE.dp,
                                enabled = enabled,
                        )
                }
                is ContactCardAction.WhatsAppVideoCall -> {
                        Icon(
                                painter = painterResource(id = R.drawable.whatsapp_video_call),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.TelegramCall -> {
                        AppVoiceCallIcon(
                                logoPainterRes = R.drawable.telegram_call,
                                size = ContactUiConstants.ACTION_ICON_SIZE.dp,
                                enabled = enabled,
                        )
                }
                is ContactCardAction.TelegramVideoCall -> {
                        Icon(
                                painter = painterResource(id = R.drawable.telegram_video_call),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier,
                        )
                }

                // Messaging / Meet -> App Icon
                is ContactCardAction.Sms -> {
                        Icon(
                                imageVector = MaterialSymbols.Sms,
                                contentDescription = null,
                                tint = callIconTint,
                                modifier = smsModifier,
                        )
                }
                is ContactCardAction.WhatsAppMessage -> {
                        Icon(
                                painter = painterResource(id = R.drawable.whatsapp),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.TelegramMessage -> {
                        Icon(
                                painter = painterResource(id = R.drawable.telegram),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.SignalVideoCall -> {
                        Icon(
                                painter = painterResource(id = R.drawable.signal_video_call),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.SignalCall -> {
                        AppVoiceCallIcon(
                                logoPainterRes = R.drawable.signal_call,
                                size = ContactUiConstants.ACTION_ICON_SIZE.dp,
                                enabled = enabled,
                        )
                }
                is ContactCardAction.SignalMessage -> {
                        Icon(
                                painter = painterResource(id = R.drawable.signal),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(DesignTokens.SignalMessageIconSize),
                        )
                }
                is ContactCardAction.GoogleMeet -> {
                        Icon(
                                painter = painterResource(id = R.drawable.google_meet),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.Email -> {
                        Icon(
                                imageVector = MaterialSymbols.Email,
                                contentDescription = null,
                                tint = callIconTint,
                                modifier = modifier,
                        )
                }
                is ContactCardAction.VideoCall -> {
                        AppPackageIcon(
                                packageName = action.packageName,
                                modifier = modifier,
                                enabled = enabled,
                        )
                }
                is ContactCardAction.CustomApp -> {
                        AppPackageIcon(
                                packageName = action.packageName,
                                modifier = modifier,
                                enabled = enabled,
                        )
                }
                is ContactCardAction.ViewInContactsApp -> {
                        Icon(
                                imageVector = MaterialSymbols.Person,
                                contentDescription = null,
                                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = modifier,
                        )
                }
        }
}

@Composable
private fun AppPackageIcon(
        packageName: String?,
        modifier: Modifier,
        enabled: Boolean,
) {
        val context = LocalContext.current
        val appIcon by
                produceState(
                        initialValue = packageName?.let(ContactImageCache::cachedAppIcon),
                        key1 = packageName,
                ) {
                        value =
                                packageName?.let { targetPackage ->
                                        ContactImageCache.cachedAppIcon(targetPackage)
                                                ?: withContext(Dispatchers.IO) {
                                                        ContactImageCache.loadAppIcon(context, targetPackage)
                                                }
                                }
                }

        val icon = appIcon
        if (icon != null) {
                        Image(
                                bitmap = icon,
                                contentDescription = null,
                                modifier = modifier,
                        )
        } else {
                Icon(
                        imageVector = MaterialSymbols.Call,
                        contentDescription = null,
                        tint =
                                if (enabled) {
                                        AppColors.CallIconTint
                                } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        modifier = modifier,
                )
        }
}
