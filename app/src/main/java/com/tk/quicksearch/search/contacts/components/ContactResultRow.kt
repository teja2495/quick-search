package com.tk.quicksearch.search.contacts.components

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============================================================================
// Contact Row
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactResultRow(
        contactInfo: ContactInfo,
        messagingApp: MessagingApp,
        primaryAction: com.tk.quicksearch.search.contacts.models.ContactCardAction? = null,
        secondaryAction: com.tk.quicksearch.search.contacts.models.ContactCardAction? = null,
        onContactClick: (ContactInfo) -> Unit,
        onShowContactMethods: (ContactInfo) -> Unit = {},
        onCallContact: (ContactInfo) -> Unit,
        onSmsContact: (ContactInfo) -> Unit,
        onPrimaryActionLongPress: (ContactInfo) -> Unit = {},
        onSecondaryActionLongPress: (ContactInfo) -> Unit = {},
        onCustomAction:
                (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit =
                { _, _ ->
                },
        onContactMethodClick: (ContactMethod) -> Unit,
        isPinned: Boolean = false,
        onTogglePin: (ContactInfo) -> Unit = {},
        onExclude: (ContactInfo) -> Unit = {},
        onNicknameClick: (ContactInfo) -> Unit = {},
        hasNickname: Boolean = false,
        enableLongPress: Boolean = true,
        onLongPressOverride: (() -> Unit)? = null,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
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
                                                                        .dp
                                                )
                                                .combinedClickable(
                                                        onClick = {
                                                                // Show contact methods bottom sheet
                                                                // if available,
                                                                // otherwise open contact
                                                                if (contactInfo.hasContactMethods) {
                                                                        onShowContactMethods(
                                                                                contactInfo
                                                                        )
                                                                } else {
                                                                        onContactClick(contactInfo)
                                                                }
                                                        },
                                                        onLongClick =
                                                                onLongPressOverride
                                                                        ?: if (enableLongPress) {
                                                                                { showOptions = true }
                                                                        } else {
                                                                                null
                                                                        }
                                                )
                                                .padding(vertical = DesignTokens.SpacingSmall),
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Box(
                                        modifier =
                                                Modifier.padding(
                                                        start = DesignTokens.SpacingXSmall
                                                )
                                ) {
                                        if (icon != null) {
                                                Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier
                                                                .size(30.dp)
                                                                .padding(start = DesignTokens.SpacingXSmall)
                                                )
                                        } else {
                                                ContactAvatar(
                                                        photoUri = contactInfo.photoUri,
                                                        displayName = contactInfo.displayName,
                                                        onClick = { onContactClick(contactInfo) }
                                                )
                                        }
                                }

                                Text(
                                        text = contactInfo.displayName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )

                                // Always show call and message action buttons
                                ContactActionButtons(
                                        hasNumber = hasNumber,
                                        messagingApp = messagingApp,
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
                                        }
                                )
                        }
                }

                if (enableLongPress && onLongPressOverride == null) {
                        ContactDropdownMenu(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                isPinned = isPinned,
                                hasNickname = hasNickname,
                                onTogglePin = { onTogglePin(contactInfo) },
                                onExclude = { onExclude(contactInfo) },
                                onNicknameClick = { onNicknameClick(contactInfo) }
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
        modifier: Modifier = Modifier.size(ContactUiConstants.CONTACT_AVATAR_SIZE.dp)
) {
        val context = LocalContext.current
        val contactPhoto by
                produceState<ImageBitmap?>(initialValue = null, key1 = photoUri) {
                        value =
                                photoUri?.let { uri ->
                                        withContext(Dispatchers.IO) {
                                                runCatching {
                                                                val parsedUri = Uri.parse(uri)
                                                                context.contentResolver
                                                                        .openInputStream(parsedUri)
                                                                        ?.use { stream ->
                                                                                BitmapFactory
                                                                                        .decodeStream(
                                                                                                stream
                                                                                        )
                                                                                        ?.asImageBitmap()
                                                                        }
                                                        }
                                                        .getOrNull()
                                        }
                                }
                }

        val placeholderInitials =
                remember(displayName) {
                        displayName
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .take(2)
                                .joinToString("")
                }

        Surface(
                modifier =
                        modifier.then(
                                if (onClick != null) Modifier.clickable(onClick = onClick)
                                else Modifier
                        ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
        ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        contactPhoto?.let { photo ->
                                Image(
                                        bitmap = photo,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                )
                        }
                                ?: run {
                                        Text(
                                                text = placeholderInitials,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color =
                                                        MaterialTheme.colorScheme
                                                                .onPrimaryContainer,
                                                fontWeight = FontWeight.SemiBold
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
        messagingApp: MessagingApp,
        primaryAction: com.tk.quicksearch.search.contacts.models.ContactCardAction?,
        secondaryAction: com.tk.quicksearch.search.contacts.models.ContactCardAction?,
        onCallClick: () -> Unit,
        onSmsClick: () -> Unit,
        onPrimaryLongPress: () -> Unit,
        onSecondaryLongPress: () -> Unit,
        onCustomAction: (com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit
) {
        val view = LocalView.current

        // Helper to render action button with consistent styling and long press support
        @Composable
        fun ActionButton(
                icon: @Composable () -> Unit, // Icon content
                contentDescription: String,
                onClick: () -> Unit,
                onLongClick: () -> Unit,
                enabled: Boolean = true
        ) {
                Box(
                        modifier =
                                Modifier.size(ContactUiConstants.ACTION_BUTTON_SIZE.dp)
                                        .then(
                                                if (enabled) {
                                                        Modifier.combinedClickable(
                                                                onClick = {
                                                                        hapticConfirm(view)()
                                                                        onClick()
                                                                },
                                                                onLongClick = {
                                                                        hapticConfirm(view)()
                                                                        onLongClick()
                                                                }
                                                        )
                                                } else Modifier
                                        ),
                        contentAlignment = Alignment.Center
                ) { icon() }
        }

        // --- Primary Action (Left) ---
        ActionButton(
                icon = {
                        if (primaryAction != null) {
                                ContactActionIconForButton(
                                        action = primaryAction,
                                        enabled = hasNumber
                                )
                        } else {
                                Icon(
                                        imageVector = Icons.Rounded.Call, // Default phone icon
                                        contentDescription =
                                                stringResource(R.string.contacts_action_call),
                                        tint =
                                                if (hasNumber) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier =
                                                Modifier.size(
                                                        ContactUiConstants.ACTION_ICON_SIZE.dp
                                                )
                                )
                        }
                },
                contentDescription = stringResource(R.string.contacts_action_call),
                onClick = {
                        if (primaryAction != null) {
                                onCustomAction(primaryAction)
                        } else {
                                onCallClick()
                        }
                },
                onLongClick = onPrimaryLongPress,
                enabled = hasNumber
        )

        // --- Secondary Action (Right) ---
        ActionButton(
                icon = {
                        if (secondaryAction != null) {
                                // Use custom icon logic
                                ContactActionIconForButton(
                                        action = secondaryAction,
                                        enabled = hasNumber
                                )
                        } else {
                                // Default messaging logic
                                when (messagingApp) {
                                        MessagingApp.MESSAGES -> {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Sms,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string.contacts_action_sms
                                                                ),
                                                                tint =
                                                                        if (hasNumber) Color.White
                                                                        else
                                                                                MaterialTheme.colorScheme
                                                                                        .onSurfaceVariant,
                                                                modifier =
                                                                        Modifier.size(
                                                                                (ContactUiConstants
                                                                                        .ACTION_ICON_SIZE *
                                                                                        0.9f)
                                                                                        .dp
                                                                        )
                                                        )
                                                }
                                        MessagingApp.WHATSAPP -> {
                                                Icon(
                                                        painter =
                                                                painterResource(
                                                                        id = R.drawable.whatsapp
                                                                ),
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string
                                                                                .contacts_action_whatsapp
                                                                ),
                                                        tint =
                                                                if (hasNumber) Color.Unspecified
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                        modifier =
                                                                Modifier.size(
                                                                        ContactUiConstants
                                                                                .ACTION_ICON_SIZE
                                                                                .dp
                                                                )
                                                )
                                        }
                                        MessagingApp.TELEGRAM -> {
                                                Icon(
                                                        painter =
                                                                painterResource(
                                                                        id = R.drawable.telegram
                                                                ),
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string
                                                                                .contacts_action_telegram
                                                                ),
                                                        tint =
                                                                if (hasNumber) Color.Unspecified
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                        modifier =
                                                                Modifier.size(
                                                                        ContactUiConstants
                                                                                .ACTION_ICON_SIZE
                                                                                .dp
                                                                )
                                                )
                                        }
                                }
                        }
                },
                contentDescription = stringResource(R.string.contacts_action_sms),
                onClick = {
                        if (secondaryAction != null) {
                                onCustomAction(secondaryAction)
                        } else {
                                onSmsClick()
                        }
                },
                onLongClick = onSecondaryLongPress,
                enabled = hasNumber
        )
}

@Composable
private fun ContactActionIconForButton(
        action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
        enabled: Boolean
) {
        val tint = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant
        val whiteTint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        val modifier = Modifier.size(ContactUiConstants.ACTION_ICON_SIZE.dp)
        val smsModifier =
                Modifier.size((ContactUiConstants.ACTION_ICON_SIZE * 0.9f).dp)

        when (action) {
                // Calls -> Phone Icon
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.Phone -> {
                        Icon(
                                imageVector = Icons.Rounded.Call, // Explicit request for phone icon
                                contentDescription = null,
                                tint = whiteTint,
                                modifier = modifier
                        )
                }
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppCall,
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppVideoCall -> {
                        Icon(
                                painter = painterResource(id = if (action is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppVideoCall) R.drawable.whatsapp_video_call else R.drawable.whatsapp_call),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier
                        )
                }
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramCall,
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramVideoCall -> {
                        Icon(
                                painter = painterResource(id = if (action is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramVideoCall) R.drawable.telegram_video_call else R.drawable.telegram_call),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier
                        )
                }

                // Messaging / Meet -> App Icon
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.Sms -> {
                        Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = whiteTint, // SMS icon usually white based on existing code
                                modifier = smsModifier
                        )
                }
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.WhatsAppMessage -> {
                        Icon(
                                painter = painterResource(id = R.drawable.whatsapp),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier
                        )
                }
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.TelegramMessage -> {
                        Icon(
                                painter = painterResource(id = R.drawable.telegram),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier
                        )
                }
                is com.tk.quicksearch.search.contacts.models.ContactCardAction.GoogleMeet -> {
                        Icon(
                                painter = painterResource(id = R.drawable.google_meet),
                                contentDescription = null,
                                tint = tint,
                                modifier = modifier
                        )
                }
        }
}
