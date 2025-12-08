package com.tk.quicksearch.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.MessagingApp
import com.tk.quicksearch.settings.SettingsSpacing

// Constants for consistent spacing
private object MessagingSpacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
}

/**
 * Reusable radio button row component for settings cards.
 * Provides consistent styling and layout across all radio button rows.
 */
@Composable
private fun SettingsRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MessagingSpacing.cardHorizontalPadding,
                vertical = MessagingSpacing.cardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

/**
 * Messaging section for contacts settings.
 * Allows users to choose between Messages, WhatsApp, and Telegram for contact actions.
 *
 * @param messagingApp Currently selected messaging app
 * @param onSetMessagingApp Callback when the messaging option changes
 * @param contactsSectionEnabled Whether the contacts section is enabled. If false, this section is not displayed.
 * @param isWhatsAppInstalled Whether WhatsApp is available on the device
 * @param isTelegramInstalled Whether Telegram is available on the device
 * @param modifier Modifier to be applied to the section title
 */
@Composable
fun MessagingSection(
    messagingApp: MessagingApp,
    onSetMessagingApp: (MessagingApp) -> Unit,
    contactsSectionEnabled: Boolean = true,
    isWhatsAppInstalled: Boolean = false,
    isTelegramInstalled: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!contactsSectionEnabled) {
        return
    }

    // Hide the setting completely if neither optional messaging app is installed
    if (!isWhatsAppInstalled && !isTelegramInstalled) {
        return
    }

    data class MessagingOption(val app: MessagingApp, val labelRes: Int)

    val messagingOptions = buildList {
        add(MessagingOption(MessagingApp.MESSAGES, R.string.settings_messaging_option_messages))
        if (isWhatsAppInstalled) {
            add(MessagingOption(MessagingApp.WHATSAPP, R.string.settings_messaging_option_whatsapp))
        }
        if (isTelegramInstalled) {
            add(MessagingOption(MessagingApp.TELEGRAM, R.string.settings_messaging_option_telegram))
        }
    }
    
    // Section title
    Text(
        text = stringResource(R.string.settings_messaging_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
    )
    
    // Section description
    Text(
        text = stringResource(R.string.settings_messaging_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
    )
    
    // Options card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            messagingOptions.forEachIndexed { index, option ->
                SettingsRadioRow(
                    text = stringResource(option.labelRes),
                    selected = messagingApp == option.app,
                    onClick = { onSetMessagingApp(option.app) }
                )

                if (index < messagingOptions.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

