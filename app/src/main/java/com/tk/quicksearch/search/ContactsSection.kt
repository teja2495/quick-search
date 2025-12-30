package com.tk.quicksearch.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import android.graphics.BitmapFactory
import android.net.Uri
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.ContactMethod
import com.tk.quicksearch.util.PhoneNumberUtils
import com.tk.quicksearch.util.TelegramContactUtils
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Constants
private const val INITIAL_RESULT_COUNT = 1
private const val CONTACT_ROW_MIN_HEIGHT = 52
private const val CONTACT_AVATAR_SIZE = 40
private const val ACTION_BUTTON_SIZE = 44
private const val ACTION_ICON_SIZE = 28
private const val EXPAND_BUTTON_HEIGHT = 28
private const val EXPAND_ICON_SIZE = 18

// ============================================================================
// Public API
// ============================================================================

@Composable
fun ContactResultsSection(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    contacts: List<ContactInfo>,
    isExpanded: Boolean,
    messagingApp: MessagingApp,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit = {},
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit = { _, _ -> },
    pinnedContactIds: Set<Long> = emptySet(),
    onTogglePin: (ContactInfo) -> Unit = {},
    onExclude: (ContactInfo) -> Unit = {},
    onNicknameClick: (ContactInfo) -> Unit = {},
    getContactNickname: (Long) -> String? = { null },
    onOpenAppSettings: () -> Unit,
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit,
    permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
    showWallpaperBackground: Boolean = false
) {
    val hasVisibleContent = (hasPermission && contacts.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            hasPermission && contacts.isNotEmpty() -> {
                ContactsResultCard(
                    contacts = contacts,
                    isExpanded = isExpanded,
                    showAllResults = showAllResults,
                    showExpandControls = showExpandControls,
                    messagingApp = messagingApp,
                    onContactClick = onContactClick,
                    onShowContactMethods = onShowContactMethods,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    onContactMethodClick = onContactMethodClick,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onNicknameClick = onNicknameClick,
                    getContactNickname = getContactNickname,
                    onExpandClick = onExpandClick,
                    showWallpaperBackground = showWallpaperBackground
                )
            }

            !hasPermission -> {
                permissionDisabledCard(
                    stringResource(R.string.contacts_permission_title),
                    stringResource(R.string.contacts_permission_subtitle),
                    stringResource(R.string.permission_action_manage_android),
                    onOpenAppSettings
                )
            }
        }
    }
}

// ============================================================================
// Result Card
// ============================================================================

@Composable
private fun ContactsResultCard(
    contacts: List<ContactInfo>,
    isExpanded: Boolean,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    messagingApp: MessagingApp,
    onContactClick: (ContactInfo) -> Unit,
    onShowContactMethods: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    onContactMethodClick: (ContactInfo, ContactMethod) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
    onExclude: (ContactInfo) -> Unit,
    onNicknameClick: (ContactInfo) -> Unit,
    getContactNickname: (Long) -> String?,
    onExpandClick: () -> Unit,
    showWallpaperBackground: Boolean = false
) {
    val displayAsExpanded = isExpanded || showAllResults
    val canShowExpand = showExpandControls && contacts.size > INITIAL_RESULT_COUNT
    val shouldShowExpandButton = !displayAsExpanded && canShowExpand
    val shouldShowCollapseButton = isExpanded && showExpandControls
    
    val displayContacts = if (displayAsExpanded) {
        contacts
    } else {
        contacts.take(INITIAL_RESULT_COUNT)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showWallpaperBackground) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                displayContacts.forEachIndexed { index, contactInfo ->
                    key(contactInfo.contactId) {
                        ContactResultRow(
                            contactInfo = contactInfo,
                            messagingApp = messagingApp,
                            onContactClick = onContactClick,
                            onShowContactMethods = onShowContactMethods,
                            onCallContact = onCallContact,
                            onSmsContact = onSmsContact,
                            onContactMethodClick = { method -> onContactMethodClick(contactInfo, method) },
                            isPinned = pinnedContactIds.contains(contactInfo.contactId),
                            onTogglePin = onTogglePin,
                            onExclude = onExclude,
                            onNicknameClick = onNicknameClick,
                            hasNickname = !getContactNickname(contactInfo.contactId).isNullOrBlank()
                        )
                    }
                    if (index != displayContacts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                
                if (shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(EXPAND_BUTTON_HEIGHT.dp)
                            .padding(top = 2.dp)
                    )
                }
            }
            }
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    displayContacts.forEachIndexed { index, contactInfo ->
                        key(contactInfo.contactId) {
                            ContactResultRow(
                                contactInfo = contactInfo,
                                messagingApp = messagingApp,
                                onContactClick = onContactClick,
                                onShowContactMethods = onShowContactMethods,
                                onCallContact = onCallContact,
                                onSmsContact = onSmsContact,
                                onContactMethodClick = { method -> onContactMethodClick(contactInfo, method) },
                                isPinned = pinnedContactIds.contains(contactInfo.contactId),
                                onTogglePin = onTogglePin,
                                onExclude = onExclude,
                                onNicknameClick = onNicknameClick,
                                hasNickname = !getContactNickname(contactInfo.contactId).isNullOrBlank()
                            )
                        }
                        if (index != displayContacts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    
                    if (shouldShowExpandButton) {
                        ExpandButton(
                            onClick = onExpandClick,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(EXPAND_BUTTON_HEIGHT.dp)
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
        
        if (shouldShowCollapseButton) {
            CollapseButton(
                onClick = onExpandClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// Contact Row
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactResultRow(
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
private fun ContactAvatar(
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
// Contact Action Button (Messaging App Style)
// ============================================================================

@Composable
private fun ContactActionButton(
    method: ContactMethod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = when (method) {
        is ContactMethod.Phone -> Color(0xFF4CAF50)
        is ContactMethod.Sms -> Color(0xFF2196F3)
        is ContactMethod.WhatsAppCall,
        is ContactMethod.WhatsAppMessage,
        is ContactMethod.WhatsAppVideoCall -> Color(0xFF25D366)
        is ContactMethod.TelegramMessage,
        is ContactMethod.TelegramCall,
        is ContactMethod.TelegramVideoCall -> Color(0xFF0088CC)
        is ContactMethod.GoogleMeet -> Color.Unspecified
        is ContactMethod.Email -> Color(0xFFFF9800)
        is ContactMethod.VideoCall -> Color(0xFF9C27B0)
        is ContactMethod.CustomApp -> Color(0xFF607D8B)
        is ContactMethod.ViewInContactsApp -> Color(0xFF9E9E9E)
    }

    Surface(
        modifier = modifier
            .width(90.dp)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ContactActionIcon(method = method, tint = iconColor)
            Text(
                text = getActionButtonLabel(method),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9f
            )
        }
    }
}

@Composable
private fun ContactActionIcon(method: ContactMethod, tint: Color) {
    when (method) {
        is ContactMethod.Phone -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.Sms -> {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.WhatsAppCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.WhatsAppMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.WhatsAppVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.TelegramMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.TelegramCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.TelegramVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.GoogleMeet -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.Email -> {
            Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.VideoCall -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.CustomApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.ViewInContactsApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun getActionButtonLabel(method: ContactMethod): String {
    return when (method) {
        is ContactMethod.Phone -> "Call"
        is ContactMethod.Sms -> "Message"
        is ContactMethod.WhatsAppCall -> "Voice Call"
        is ContactMethod.WhatsAppMessage -> "Chat"
        is ContactMethod.WhatsAppVideoCall -> "Video Call"
        is ContactMethod.TelegramMessage -> "Chat"
        is ContactMethod.TelegramCall -> "Voice Call"
        is ContactMethod.TelegramVideoCall -> "Video Call"
        is ContactMethod.GoogleMeet -> "Meet"
        is ContactMethod.Email -> "Email"
        is ContactMethod.VideoCall -> "Video Call"
        is ContactMethod.CustomApp -> method.displayLabel
        is ContactMethod.ViewInContactsApp -> "Contacts"
    }
}

// ============================================================================
// Contact Methods List
// ============================================================================

@Composable
private fun ContactMethodsList(
    contactMethods: List<ContactMethod>,
    onMethodClick: (ContactMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        contactMethods.forEach { method ->
            ContactMethodItem(
                method = method,
                onClick = { onMethodClick(method) }
            )
        }
    }
}

@Composable
private fun ContactMethodItem(
    method: ContactMethod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on method type
        ContactMethodIcon(method = method)
        
        // Method label and data (show subtext only for email)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Show subtext only for Email methods
            if (method is ContactMethod.Email) {
                Text(
                    text = method.data,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactMethodIcon(method: ContactMethod) {
    val (icon, tint) = when (method) {
        is ContactMethod.Phone -> Pair(Icons.Rounded.Call, Color.White)
        is ContactMethod.Sms -> Pair(Icons.Rounded.Sms, Color.White)
        is ContactMethod.WhatsAppCall,
        is ContactMethod.WhatsAppMessage,
        is ContactMethod.WhatsAppVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = Color(0xFF25D366),
                modifier = Modifier.size(24.dp)
            )
            return
        }
        is ContactMethod.TelegramMessage,
        is ContactMethod.TelegramCall,
        is ContactMethod.TelegramVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = Color(0xFF0088CC),
                modifier = Modifier.size(24.dp)
            )
            return
        }
        is ContactMethod.GoogleMeet -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                tint = Color.Unspecified, // Use original colors from the drawable
                modifier = Modifier.size(24.dp)
            )
            return
        }
        is ContactMethod.Email -> Pair(Icons.Rounded.Email, Color.White)
        is ContactMethod.VideoCall -> Pair(Icons.Rounded.Call, Color.White) // TODO: Use video icon
        is ContactMethod.CustomApp -> Pair(Icons.Rounded.Person, Color.White) // TODO: Use generic icon
        is ContactMethod.ViewInContactsApp -> Pair(Icons.Rounded.Person, Color.White)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
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

// ============================================================================
// Expand/Collapse Buttons
// ============================================================================

@Composable
private fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(
            text = stringResource(R.string.action_expand_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp)
        )
    }
}

@Composable
private fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.action_collapse),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.ExpandLess,
            contentDescription = stringResource(R.string.desc_collapse),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp)
        )
    }
}

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasMultipleNumbers = contactInfo.phoneNumbers.size > 1

    // Reorder phone numbers to show last shown number first (only for multiple numbers)
    val reorderedPhoneNumbers = remember(contactInfo.phoneNumbers, contactInfo.contactId, hasMultipleNumbers) {
        if (hasMultipleNumbers) {
            val lastShownNumber = getLastShownPhoneNumber(contactInfo.contactId)
            if (lastShownNumber != null && contactInfo.phoneNumbers.isNotEmpty()) {
                // Find the index of the last shown number (using phone number matching)
                val lastShownIndex = contactInfo.phoneNumbers.indexOfFirst { number ->
                    PhoneNumberUtils.isSameNumber(number, lastShownNumber)
                }
                if (lastShownIndex >= 0) {
                    // Move the last shown number to the front
                    val reordered = contactInfo.phoneNumbers.toMutableList()
                    val lastShown = reordered.removeAt(lastShownIndex)
                    reordered.add(0, lastShown)
                    reordered
                } else {
                    contactInfo.phoneNumbers
                }
            } else {
                contactInfo.phoneNumbers
            }
        } else {
            contactInfo.phoneNumbers
        }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header with contact info (photo and name outside the card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ContactAvatar(
                    photoUri = contactInfo.photoUri,
                    displayName = contactInfo.displayName,
                    onClick = null
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = contactInfo.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Info icon to open contact in contacts app
                IconButton(
                    onClick = {
                        onContactMethodClick(contactInfo, ContactMethod.ViewInContactsApp())
                        onDismiss()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "View contact",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card encompassing options with black background
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                                        contentDescription = "Previous phone number",
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
                                style = MaterialTheme.typography.bodyLarge,
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
                                        contentDescription = "Next phone number",
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
                    val hasMultipleNumbers = reorderedPhoneNumbers.size > 1
                    val context = LocalContext.current
                    val methodsForSelectedNumber = contactInfo.contactMethods.filter { method ->
                        val matches = if (method is ContactMethod.TelegramMessage ||
                            method is ContactMethod.TelegramCall ||
                            method is ContactMethod.TelegramVideoCall) {
                            // Use the utility function to match Telegram methods to phone numbers
                            // This follows the Stack Overflow approach:
                            // 1. Get contact ID from phone number using PhoneLookup
                            // 2. Query ContactsContract.Data for Telegram entries with that contact ID
                            // 3. Match the method's dataId to the found data IDs
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
                        } else {
                            // For other methods, require phone number match with the selected number
                            val methodData = method.data?.takeIf { it.isNotBlank() }
                            methodData != null && selectedPhoneNumber != null &&
                                PhoneNumberUtils.isSameNumber(methodData, selectedPhoneNumber)
                        }

                        matches
                    }

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

                    // Second row: WhatsApp options (filtered by selected phone number)
                    val whatsappMethods = methodsForSelectedNumber.filter {
                        it is ContactMethod.WhatsAppCall ||
                        it is ContactMethod.WhatsAppMessage ||
                        it is ContactMethod.WhatsAppVideoCall
                    }
                    if (whatsappMethods.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            whatsappMethods.forEach { method ->
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

                    // Third row: Telegram options (filtered by selected phone number)
                    // Now that we can distinguish Telegram accounts by phone number,
                    // we can show all Telegram options (chat, call, video call) for the selected number
                    val telegramMethods = methodsForSelectedNumber.filter {
                        it is ContactMethod.TelegramMessage ||
                        it is ContactMethod.TelegramCall ||
                        it is ContactMethod.TelegramVideoCall
                    }
                    if (telegramMethods.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            telegramMethods.forEach { method ->
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
            
            // Add bottom padding for navigation bar
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
