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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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

private const val INITIAL_RESULT_COUNT = 1

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
    val orderedContacts = contacts

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        resultSectionTitle(stringResource(R.string.contacts_section_title))
        when {
            hasPermission && contacts.isNotEmpty() -> {
                val displayAsExpanded = isExpanded || showAllResults
                val canShowExpand = showExpandControls && orderedContacts.size > INITIAL_RESULT_COUNT
                val expandHandler = if (!displayAsExpanded && canShowExpand) onExpandClick else null
                val collapseHandler = if (isExpanded && showExpandControls) onExpandClick else null
                val displayContacts = if (displayAsExpanded) {
                    orderedContacts
                } else {
                    orderedContacts.take(INITIAL_RESULT_COUNT)
                }
                ContactsResultCard(
                    contacts = displayContacts,
                    allContacts = orderedContacts,
                    isExpanded = displayAsExpanded,
                    useWhatsAppForMessages = useWhatsAppForMessages,
                    onContactClick = onContactClick,
                    onCallContact = onCallContact,
                    onSmsContact = onSmsContact,
                    pinnedContactIds = pinnedContactIds,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExpandClick = expandHandler,
                    onCollapseClick = collapseHandler
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

@Composable
private fun ContactsResultCard(
    contacts: List<ContactInfo>,
    allContacts: List<ContactInfo>,
    isExpanded: Boolean,
    useWhatsAppForMessages: Boolean,
    onContactClick: (ContactInfo) -> Unit,
    onCallContact: (ContactInfo) -> Unit,
    onSmsContact: (ContactInfo) -> Unit,
    pinnedContactIds: Set<Long>,
    onTogglePin: (ContactInfo) -> Unit,
    onExclude: (ContactInfo) -> Unit,
    onExpandClick: (() -> Unit)?,
    onCollapseClick: (() -> Unit)?
) {
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
                contacts.forEachIndexed { index, contactInfo ->
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
                    if (index != contacts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                if (onExpandClick != null && !isExpanded) {
                    TextButton(
                        onClick = { onExpandClick() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(28.dp)
                            .padding(top = 2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        if (onCollapseClick != null && isExpanded) {
            TextButton(
                onClick = { onCollapseClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Collapse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = "Collapse",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

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
    val context = LocalContext.current
    val hasNumber = contactInfo.primaryNumber != null
    
    // Load contact photo
    val contactPhoto by produceState<ImageBitmap?>(initialValue = null, key1 = contactInfo.photoUri) {
        val photoUri = contactInfo.photoUri
        if (photoUri != null) {
            // Reset to avoid showing stale images while loading a new one
            value = null
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(photoUri)
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            value = bitmap
        } else {
            // Ensure we don't keep an old bitmap when this contact has no photo
            value = null
        }
    }
    
    val placeholderInitials = remember(contactInfo.displayName) {
        contactInfo.displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2).joinToString("")
    }
    
    var showOptions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .combinedClickable(
                onClick = { onContactClick(contactInfo) },
                onLongClick = { showOptions = true }
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact photo/avatar
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    Image(
                        bitmap = contactPhoto!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = placeholderInitials,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
        IconButton(
            onClick = { onCallContact(contactInfo) },
            enabled = hasNumber,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = stringResource(R.string.contacts_action_call),
                tint = if (hasNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(
            onClick = { onSmsContact(contactInfo) },
            enabled = hasNumber,
            modifier = Modifier.size(40.dp)
        ) {
            if (useWhatsAppForMessages) {
                Icon(
                    painter = painterResource(id = R.drawable.whatsapp),
                    contentDescription = stringResource(R.string.contacts_action_whatsapp),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = stringResource(R.string.contacts_action_sms),
                    tint = if (hasNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinned) Icons.Rounded.Close else Icons.Rounded.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    onTogglePin(contactInfo)
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_exclude_generic)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    onExclude(contactInfo)
                }
            )
        }
    }
}

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
                    text = stringResource(R.string.dialog_select_phone_number_message, contactInfo.displayName),
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
                Text(text = if (isCall) stringResource(R.string.dialog_call) else stringResource(R.string.dialog_sms))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

