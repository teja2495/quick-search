package com.tk.quicksearch.search.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.ContactMethod
import com.tk.quicksearch.search.core.MessagingApp

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
        val cardModifier = Modifier.fillMaxWidth()
        val cardContent: @Composable () -> Unit = {
            ContactList(
                displayContacts = displayContacts,
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
                shouldShowExpandButton = shouldShowExpandButton,
                onExpandClick = onExpandClick
            )
        }

        if (showWallpaperBackground) {
            Card(
                modifier = cardModifier,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                cardContent()
            }
        } else {
            ElevatedCard(
                modifier = cardModifier,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                cardContent()
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
// Contact List
// ============================================================================

@Composable
private fun ContactList(
    displayContacts: List<ContactInfo>,
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
    shouldShowExpandButton: Boolean,
    onExpandClick: () -> Unit
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
