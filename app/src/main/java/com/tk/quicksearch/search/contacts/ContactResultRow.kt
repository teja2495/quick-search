package com.tk.quicksearch.search.contacts

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import android.graphics.BitmapFactory
import android.net.Uri
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.ContactMethod
import com.tk.quicksearch.search.MessagingApp
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
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit = {},
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactMethod) -> Unit,
    isPinned: Boolean = false,
    onTogglePin: (ContactInfo) -> Unit = {},
    onExclude: (ContactInfo) -> Unit = {},
    onNicknameClick: (ContactInfo) -> Unit = {},
    hasNickname: Boolean = false
) {
    var showOptions by remember { mutableStateOf(false) }
    val hasNumber = contactInfo.primaryNumber != null

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CONTACT_ROW_MIN_HEIGHT.dp)
                    .combinedClickable(
                        onClick = {
                            // Show contact methods bottom sheet if available, otherwise open contact
                            if (contactInfo.hasContactMethods) {
                                onShowContactMethods(contactInfo)
                            } else {
                                onContactClick(contactInfo)
                            }
                        },
                        onLongClick = { showOptions = true }
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(
                    photoUri = contactInfo.photoUri,
                    displayName = contactInfo.displayName,
                    onClick = { onContactClick(contactInfo) }
                )

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
                    onCallClick = { onCallContact(contactInfo) },
                    onSmsClick = { onSmsContact(contactInfo) }
                )
            }
        }

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

// ============================================================================
// Contact Avatar
// ============================================================================

@Composable
internal fun ContactAvatar(
    photoUri: String?,
    displayName: String,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val contactPhoto by produceState<ImageBitmap?>(initialValue = null, key1 = photoUri) {
        value = photoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val parsedUri = Uri.parse(uri)
                    context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    val placeholderInitials = remember(displayName) {
        displayName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
    }

    Surface(
        modifier = Modifier
            .size(CONTACT_AVATAR_SIZE.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            contactPhoto?.let { photo ->
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Text(
                    text = placeholderInitials,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit
) {
    IconButton(
        onClick = onCallClick,
        enabled = hasNumber,
        modifier = Modifier.size(ACTION_BUTTON_SIZE.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = stringResource(R.string.contacts_action_call),
            tint = if (hasNumber) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(ACTION_ICON_SIZE.dp)
        )
    }

    IconButton(
        onClick = onSmsClick,
        enabled = hasNumber,
        modifier = Modifier.size(ACTION_BUTTON_SIZE.dp)
    ) {
        when (messagingApp) {
            MessagingApp.MESSAGES -> {
                Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = stringResource(R.string.contacts_action_sms),
                    tint = if (hasNumber) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(ACTION_ICON_SIZE.dp)
                )
            }
            MessagingApp.WHATSAPP -> {
                Icon(
                    painter = painterResource(id = R.drawable.whatsapp),
                    contentDescription = stringResource(R.string.contacts_action_whatsapp),
                    tint = if (hasNumber) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(ACTION_ICON_SIZE.dp)
                )
            }
            MessagingApp.TELEGRAM -> {
                Icon(
                    painter = painterResource(id = R.drawable.telegram),
                    contentDescription = stringResource(R.string.contacts_action_telegram),
                    tint = if (hasNumber) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(ACTION_ICON_SIZE.dp)
                )
            }
        }
    }
}

// ============================================================================
// Dropdown Menu
// ============================================================================

@Composable
private fun ContactDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isPinned: Boolean,
    hasNickname: Boolean,
    onTogglePin: () -> Unit,
    onExclude: () -> Unit,
    onNicknameClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isPinned) Icons.Rounded.Close else Icons.Rounded.PushPin,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onTogglePin()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (hasNickname) R.string.action_edit_nickname else R.string.action_add_nickname
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onNicknameClick()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.action_exclude_generic))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onExclude()
            }
        )
    }
}
