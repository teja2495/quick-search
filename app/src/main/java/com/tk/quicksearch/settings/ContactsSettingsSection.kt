package com.tk.quicksearch.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.MessagingApp

// Constants for consistent spacing
private object MessagingSpacing {
    val cardHorizontalPadding = 20.dp
    val cardTopPadding = 20.dp
    val cardBottomPadding = 16.dp
    val optionSpacing = 12.dp
}

private data class MessagingOption(val app: MessagingApp, val labelRes: Int)

/**
 * Calls & Texts section.
 * Lets users choose direct dial behavior and default messaging app.
 *
 * @param messagingApp Currently selected messaging app
 * @param onSetMessagingApp Callback when the messaging option changes
 * @param directDialEnabled Whether direct dial is enabled
 * @param onToggleDirectDial Callback when the direct dial option changes
 * @param hasCallPermission Whether the CALL_PHONE permission is granted
 * @param contactsSectionEnabled Whether the contacts section is enabled. If false, this section is not displayed.
 * @param isWhatsAppInstalled Whether WhatsApp is available on the device
 * @param isTelegramInstalled Whether Telegram is available on the device
 * @param modifier Modifier to be applied to the section title
 */
@Composable
fun MessagingSection(
    messagingApp: MessagingApp,
    onSetMessagingApp: (MessagingApp) -> Unit,
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    hasCallPermission: Boolean,
    contactsSectionEnabled: Boolean = true,
    isWhatsAppInstalled: Boolean = false,
    isTelegramInstalled: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!contactsSectionEnabled) {
        return
    }

    val messagingOptions = buildList {
        add(MessagingOption(MessagingApp.MESSAGES, R.string.settings_messaging_option_messages))
        if (isWhatsAppInstalled) {
            add(MessagingOption(MessagingApp.WHATSAPP, R.string.settings_messaging_option_whatsapp))
        }
        if (isTelegramInstalled) {
            add(MessagingOption(MessagingApp.TELEGRAM, R.string.settings_messaging_option_telegram))
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_messaging_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
        )

        Text(
            text = stringResource(R.string.settings_messaging_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
        )

        DirectDialCard(
            enabled = directDialEnabled,
            hasCallPermission = hasCallPermission,
            onToggle = onToggleDirectDial
        )

        MessagingOptionsCard(
            options = messagingOptions,
            selectedApp = messagingApp,
            onSelect = onSetMessagingApp,
            modifier = Modifier.padding(top = SettingsSpacing.sectionTitleBottomPadding)
        )
    }
}

@Composable
private fun DirectDialCard(
    enabled: Boolean,
    hasCallPermission: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.singleCardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_direct_dial_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_direct_dial_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled && hasCallPermission,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun MessagingOptionsCard(
    options: List<MessagingOption>,
    selectedApp: MessagingApp,
    onSelect: (MessagingApp) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MessagingSpacing.cardHorizontalPadding,
                    end = MessagingSpacing.cardHorizontalPadding,
                    top = MessagingSpacing.cardTopPadding,
                    bottom = MessagingSpacing.cardBottomPadding
                ),
            verticalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing)
        ) {
            Text(
                text = stringResource(R.string.settings_messaging_card_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    MessagingOptionChip(
                        option = option,
                        selected = selectedApp == option.app,
                        onClick = { onSelect(option.app) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessagingOptionChip(
    option: MessagingOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MessagingOptionIcon(app = option.app)
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessagingOptionIcon(app: MessagingApp) {
    when (app) {
        MessagingApp.MESSAGES -> {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        MessagingApp.WHATSAPP -> {
            Image(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
        MessagingApp.TELEGRAM -> {
            Image(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}