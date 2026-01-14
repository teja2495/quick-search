package com.tk.quicksearch.settings.settingsScreen

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.util.hapticConfirm
import com.tk.quicksearch.settings.SettingsCard
import com.tk.quicksearch.settings.SettingsSectionTitle
import com.tk.quicksearch.settings.SettingsToggleRow
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.ui.theme.DesignTokens

// Constants for consistent spacing
private object MessagingSpacing {
    val cardHorizontalPadding = DesignTokens.CardHorizontalPadding
    val cardTopPadding = DesignTokens.CardTopPadding
    val cardBottomPadding = DesignTokens.CardBottomPadding
    val optionSpacing = DesignTokens.ItemRowSpacing
    val toggleSpacing = DesignTokens.ToggleSpacing
    val directDialColumnSpacing = DesignTokens.TextColumnSpacing
    val messagingTitleBottomPadding = DesignTokens.SectionTitleBottomPadding
    val chipVerticalPadding = DesignTokens.ChipVerticalPadding
    val chipHorizontalPadding = DesignTokens.ChipHorizontalPadding
    val chipIconSpacing = DesignTokens.ChipIconSpacing
    val iconSize = DesignTokens.LargeIconSize
    val borderWidth = DesignTokens.BorderWidth
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
 * @param onMessagingAppSelected Callback when a messaging option is selected, handles installation check
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
    onMessagingAppSelected: ((MessagingApp) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!contactsSectionEnabled) {
        return
    }

    val messagingOptions = buildList {
        add(MessagingOption(MessagingApp.MESSAGES, R.string.settings_messaging_option_messages))
        add(MessagingOption(MessagingApp.WHATSAPP, R.string.settings_messaging_option_whatsapp))
        add(MessagingOption(MessagingApp.TELEGRAM, R.string.settings_messaging_option_telegram))
    }

    Column(modifier = modifier) {
        SettingsSectionTitle(
            title = stringResource(R.string.settings_messaging_title)
        )

        MergedMessagingCard(
            messagingOptions = messagingOptions,
            selectedApp = messagingApp,
            onSetMessagingApp = onSetMessagingApp,
            onMessagingAppSelected = onMessagingAppSelected ?: onSetMessagingApp,
            directDialEnabled = directDialEnabled,
            onToggleDirectDial = onToggleDirectDial,
            hasCallPermission = hasCallPermission
        )
    }
}


@Composable
private fun MergedMessagingCard(
    messagingOptions: List<MessagingOption>,
    selectedApp: MessagingApp,
    onSetMessagingApp: (MessagingApp) -> Unit,
    onMessagingAppSelected: (MessagingApp) -> Unit,
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    hasCallPermission: Boolean
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Direct Dial Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SettingsSpacing.singleCardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = MessagingSpacing.toggleSpacing),
                    verticalArrangement = Arrangement.spacedBy(MessagingSpacing.directDialColumnSpacing)
                ) {
                    Text(
                        text = stringResource(R.string.settings_direct_dial_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_direct_dial_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = directDialEnabled && hasCallPermission,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onToggleDirectDial(enabled)
                    }
                )
            }

            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Messaging Options Section
            if (messagingOptions.isNotEmpty()) {
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
                        modifier = Modifier.padding(bottom = MessagingSpacing.messagingTitleBottomPadding)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        messagingOptions.forEach { option ->
                            MessagingOptionChip(
                                option = option,
                                selected = selectedApp == option.app,
                                onClick = { onMessagingAppSelected(option.app) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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
    val view = LocalView.current
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
                width = MessagingSpacing.borderWidth,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .selectable(
                selected = selected,
                onClick = {
                    hapticConfirm(view)()
                    onClick()
                },
                role = Role.RadioButton
            )
            .padding(vertical = MessagingSpacing.chipVerticalPadding, horizontal = MessagingSpacing.chipHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MessagingSpacing.chipIconSpacing)
    ) {
        MessagingOptionIcon(app = option.app)
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelSmall,
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
                modifier = Modifier.size(MessagingSpacing.iconSize)
            )
        }
        MessagingApp.WHATSAPP -> {
            Image(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize)
            )
        }
        MessagingApp.TELEGRAM -> {
            Image(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize)
            )
        }
    }
}