package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.contacts.components.ContactButtonActionIcon
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppShapes

/**
 * First/second contact card button settings: a contact card preview, a picker for each button,
 * and a note on the Call/SMS fallback.
 *
 * @param availableActions Actions whose app is installed, in display order
 */
@Composable
internal fun ContactButtonsSettings(
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
    availableActions: List<ContactButtonAction>,
    onPrimaryContactButtonSelected: (ContactButtonAction) -> Unit,
    onSecondaryContactButtonSelected: (ContactButtonAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // true while picking the first button, false for the second, null when no picker is open
    var pickingPrimaryButton by remember { mutableStateOf<Boolean?>(null) }

    SegmentedGroup(modifier = modifier) {
        SegmentedTile(index = 0, count = 3) {
            ContactCardPreview(
                primaryContactButton = primaryContactButton,
                secondaryContactButton = secondaryContactButton,
            )
        }
        SegmentedTile(index = 1, count = 3, onClick = { pickingPrimaryButton = true }) {
            ContactButtonPickerRow(
                label = stringResource(R.string.settings_contact_first_button_title),
                action = primaryContactButton,
            )
        }
        SegmentedTile(index = 2, count = 3) {
            Column {
                Box(modifier = Modifier.clickable(role = Role.Button) { pickingPrimaryButton = false }) {
                    ContactButtonPickerRow(
                        label = stringResource(R.string.settings_contact_second_button_title),
                        action = secondaryContactButton,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_contact_buttons_fallback_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                        bottom = DesignTokens.SpacingLarge,
                    ),
                )
            }
        }
    }

    pickingPrimaryButton?.let { isPrimary ->
        ContactButtonPickerDialog(
            title =
                stringResource(
                    if (isPrimary) {
                        R.string.settings_contact_first_button_title
                    } else {
                        R.string.settings_contact_second_button_title
                    },
                ),
            selectedAction = if (isPrimary) primaryContactButton else secondaryContactButton,
            actions = availableActions,
            onActionSelected =
                if (isPrimary) onPrimaryContactButtonSelected else onSecondaryContactButtonSelected,
            onDismiss = { pickingPrimaryButton = null },
        )
    }
}

/** A sample contact row showing the default actions of the two buttons. */
@Composable
private fun ContactCardPreview(
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
) {
    val displayName = stringResource(R.string.settings_contact_buttons_preview_name)
    val contact = remember(displayName) {
        ContactInfo(
            contactId = -1L,
            lookupKey = "preview",
            displayName = displayName,
            // Synthetic number enables the sample buttons; all callbacks below are inert.
            phoneNumbers = listOf("+1 202-555-0100"),
        )
    }
    Box(
        modifier = Modifier.padding(
            horizontal = DesignTokens.SegmentedListTileHorizontalPadding,
            vertical = DesignTokens.SegmentedListTileVerticalPadding,
        ),
    ) {
        ContactResultRow(
            contactInfo = contact,
            primaryContactButton = primaryContactButton,
            secondaryContactButton = secondaryContactButton,
            onContactClick = {},
            onCallContact = {},
            onSmsContact = {},
            onContactMethodClick = {},
            enableLongPress = false,
        )
    }
}

/** Fixed-size slot so action icons of different sizes line up. */
@Composable
private fun ActionIconSlot(action: ContactButtonAction) {
    Box(
        modifier = Modifier
            .size(DesignTokens.Spacing40)
            .clip(AppShapes.full)
            .background(AppColors.AccentTintTonal),
        contentAlignment = Alignment.Center,
    ) {
        ContactButtonActionIcon(action = action)
    }
}

@Composable
private fun ContactButtonPickerRow(
    label: String,
    action: ContactButtonAction,
) {
    SegmentedTileRow(
        title = label,
        description = stringResource(action.labelRes),
        leading = { ActionIconSlot(action) },
    )
}

@Composable
private fun ContactButtonPickerDialog(
    title: String,
    selectedAction: ContactButtonAction,
    actions: List<ContactButtonAction>,
    onActionSelected: (ContactButtonAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AppPickerDrawer(
        title = title,
        onDismiss = onDismiss,
    ) { dismiss ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing),
        ) {
            items(actions, key = { it.name }) { action ->
                AppPickerDrawerRow(
                    title = stringResource(action.labelRes),
                    selected = action == selectedAction,
                    leading = { ActionIconSlot(action) },
                    onClick = {
                        onActionSelected(action)
                        dismiss()
                    },
                )
            }
        }
    }
}
