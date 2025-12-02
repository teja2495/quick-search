package com.tk.quicksearch.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Constants
private const val INITIAL_RESULT_COUNT = 1
private const val CONTACT_ROW_MIN_HEIGHT = 52
private const val CONTACT_AVATAR_SIZE = 40
private const val ACTION_BUTTON_SIZE = 40
private const val ACTION_ICON_SIZE = 24
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
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    pinnedContactIds: Set<Long> = emptySet(),
    onTogglePin: (ContactInfo) -> Unit = {},
    onExclude: (ContactInfo) -> Unit = {},
    onOpenAppSettings: () -> Unit,
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit,
    resultSectionTitle: @Composable (String) -> Unit,
    permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit
) {
    val hasVisibleContent = (hasPermission && contacts.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        resultSectionTitle(stringResource(R.string.contacts_section_title))
        
        when {
            hasPermission && contacts.isNotEmpty() -> {
                ContactsResultCard(
                    contacts = contacts,
                    isExpanded = isExpanded,
                    showAllResults = showAllResults,
                    showExpandControls = showExpandControls,
                    useWhatsAppForMessages = useWhatsAppForMessages,
                    onContactClick = onContactClick,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExpandClick = onExpandClick
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
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
    onExclude: (ContactInfo) -> Unit,
    onExpandClick: () -> Unit
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
                            useWhatsAppForMessages = useWhatsAppForMessages,
                            onContactClick = onContactClick,
                            onCallContact = onCallContact,
                            onSmsContact = onSmsContact,
                            isPinned = pinnedContactIds.contains(contactInfo.contactId),
                            onTogglePin = onTogglePin,
                            onExclude = onExclude
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

@Composable
private fun ContactResultRow(
    contactInfo: ContactInfo,
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    isPinned: Boolean = false,
    onTogglePin: (ContactInfo) -> Unit = {},
    onExclude: (ContactInfo) -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }
    val hasNumber = contactInfo.primaryNumber != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CONTACT_ROW_MIN_HEIGHT.dp)
            .combinedClickable(
                onClick = { onContactClick(contactInfo) },
                onLongClick = { showOptions = true }
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            photoUri = contactInfo.photoUri,
            displayName = contactInfo.displayName
        )
        
        Text(
            text = contactInfo.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        ContactActionButtons(
            hasNumber = hasNumber,
            useWhatsAppForMessages = useWhatsAppForMessages,
            onCallClick = { onCallContact(contactInfo) },
            onSmsClick = { onSmsContact(contactInfo) }
        )

        ContactDropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            isPinned = isPinned,
            onTogglePin = { onTogglePin(contactInfo) },
            onExclude = { onExclude(contactInfo) }
        )
    }
}

// ============================================================================
// Contact Avatar
// ============================================================================

@Composable
private fun ContactAvatar(
    photoUri: String?,
    displayName: String
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
        modifier = Modifier.size(CONTACT_AVATAR_SIZE.dp),
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
    useWhatsAppForMessages: Boolean,
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
                MaterialTheme.colorScheme.primary
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
        if (useWhatsAppForMessages) {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = stringResource(R.string.contacts_action_whatsapp),
                tint = Color.Unspecified,
                modifier = Modifier.size(ACTION_ICON_SIZE.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = stringResource(R.string.contacts_action_sms),
                tint = if (hasNumber) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(ACTION_ICON_SIZE.dp)
            )
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
    onTogglePin: () -> Unit,
    onExclude: () -> Unit
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
