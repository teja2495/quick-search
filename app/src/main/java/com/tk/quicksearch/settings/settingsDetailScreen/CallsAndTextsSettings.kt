package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.components.TipBanner
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticConfirm

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

private data class MessagingOption(
    val app: MessagingApp,
    val labelRes: Int,
)

private data class CallingOption(
    val app: CallingApp,
    val labelRes: Int,
)

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
 * @param isSignalInstalled Whether Signal is available on the device
 * @param onMessagingAppSelected Callback when a messaging option is selected, handles installation check
 * @param showDirectDial Whether to show the direct dial toggle (e.g. hidden in onboarding)
 * @param modifier Modifier to be applied to the section title
 */
@Composable
fun MessagingSection(
    messagingApp: MessagingApp,
    onSetMessagingApp: (MessagingApp) -> Unit,
    callingApp: CallingApp = CallingApp.CALL,
    onSetCallingApp: (CallingApp) -> Unit = {},
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    hasCallPermission: Boolean,
    contactsSectionEnabled: Boolean = true,
    isWhatsAppInstalled: Boolean = false,
    isTelegramInstalled: Boolean = false,
    isSignalInstalled: Boolean = false,
    isGoogleMeetInstalled: Boolean = false,
    onCallingAppSelected: ((CallingApp) -> Unit)? = null,
    onMessagingAppSelected: ((MessagingApp) -> Unit)? = null,
    showTitle: Boolean = true,
    showCallingApp: Boolean = true,
    showDirectDial: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!contactsSectionEnabled) {
        return
    }

    val hasAnyThirdPartyMessagingApp = isWhatsAppInstalled || isTelegramInstalled || isSignalInstalled
    val hasAnyThirdPartyCallingApp =
        isGoogleMeetInstalled || isWhatsAppInstalled || isTelegramInstalled || isSignalInstalled
    val shouldShowCallingCard = showCallingApp && hasAnyThirdPartyCallingApp
    val shouldShowMessagingCard = hasAnyThirdPartyMessagingApp
    val shouldShowDirectDialCard = showDirectDial

    if (!showTitle && !shouldShowDirectDialCard && !shouldShowCallingCard && !shouldShowMessagingCard) {
        return
    }

    val messagingOptions =
        buildList {
            add(MessagingOption(MessagingApp.MESSAGES, R.string.settings_messaging_option_messages))
            if (isWhatsAppInstalled) {
                add(MessagingOption(MessagingApp.WHATSAPP, R.string.settings_messaging_option_whatsapp))
            }
            if (isTelegramInstalled) {
                add(MessagingOption(MessagingApp.TELEGRAM, R.string.settings_messaging_option_telegram))
            }
            if (isSignalInstalled) {
                add(MessagingOption(MessagingApp.SIGNAL, R.string.settings_messaging_option_signal))
            }
        }
    val callingOptions =
        buildList {
            add(CallingOption(CallingApp.CALL, R.string.settings_calling_option_call))
            if (isGoogleMeetInstalled) {
                add(CallingOption(CallingApp.GOOGLE_MEET, R.string.settings_calling_option_google_meet))
            }
            if (isWhatsAppInstalled) {
                add(CallingOption(CallingApp.WHATSAPP, R.string.settings_calling_option_whatsapp))
            }
            if (isTelegramInstalled) {
                add(CallingOption(CallingApp.TELEGRAM, R.string.settings_calling_option_telegram))
            }
            if (isSignalInstalled) {
                add(CallingOption(CallingApp.SIGNAL, R.string.settings_calling_option_signal))
            }
        }

    Column(modifier = modifier) {
        if (showTitle) {
            Column {
                Text(
                    text = stringResource(R.string.settings_messaging_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
                )
            }
        }

        if (shouldShowDirectDialCard) {
            DirectDialCard(
                directDialEnabled = directDialEnabled,
                onToggleDirectDial = onToggleDirectDial,
                hasCallPermission = hasCallPermission,
            )
        }

        if (shouldShowCallingCard) {
            val callingCardModifier =
                if (shouldShowDirectDialCard) {
                    Modifier.padding(top = DesignTokens.SpacingMedium)
                } else {
                    Modifier
                }
            DefaultCallingAppCard(
                callingOptions = callingOptions,
                selectedApp = callingApp,
                onCallingAppSelected = onCallingAppSelected ?: onSetCallingApp,
                modifier = callingCardModifier,
            )
        }

        if (shouldShowMessagingCard) {
            DefaultMessagingAppCard(
                messagingOptions = messagingOptions,
                selectedApp = messagingApp,
                onMessagingAppSelected = onMessagingAppSelected ?: onSetMessagingApp,
                modifier =
                    if (shouldShowCallingCard) {
                        Modifier.padding(top = DesignTokens.SpacingMedium)
                    } else {
                        Modifier
                    },
            )
        }
    }
}

@Composable
private fun DirectDialCard(
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    hasCallPermission: Boolean,
) {
    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                onToggleDirectDial(true)
            }
        }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        SettingsToggleRow(
            title = stringResource(R.string.settings_direct_dial_title),
            subtitle = stringResource(R.string.settings_direct_dial_desc),
            checked = directDialEnabled,
            onCheckedChange = { newValue ->
                if (newValue) {
                    if (hasCallPermission) {
                        onToggleDirectDial(true)
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                } else {
                    onToggleDirectDial(false)
                }
            },
            isFirstItem = true,
            isLastItem = true,
        )
    }
}

@Composable
private fun DefaultMessagingAppCard(
    messagingOptions: List<MessagingOption>,
    selectedApp: MessagingApp,
    onMessagingAppSelected: (MessagingApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messagingOptions.isEmpty()) return

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MessagingSpacing.cardHorizontalPadding,
                        end = MessagingSpacing.cardHorizontalPadding,
                        top = MessagingSpacing.cardTopPadding,
                        bottom = MessagingSpacing.cardBottomPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
        ) {
            Text(
                text = stringResource(R.string.settings_messaging_card_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MessagingSpacing.messagingTitleBottomPadding),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val rowSize = if (messagingOptions.size > 3) 2 else messagingOptions.size
                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
                ) {
                    messagingOptions.chunked(rowSize).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowOptions.forEach { option ->
                                MessagingOptionChip(
                                    option = option,
                                    selected = selectedApp == option.app,
                                    onClick = { onMessagingAppSelected(option.app) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(rowSize - rowOptions.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultCallingAppCard(
    callingOptions: List<CallingOption>,
    selectedApp: CallingApp,
    onCallingAppSelected: (CallingApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (callingOptions.isEmpty()) return

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MessagingSpacing.cardHorizontalPadding,
                        end = MessagingSpacing.cardHorizontalPadding,
                        top = MessagingSpacing.cardTopPadding,
                        bottom = MessagingSpacing.cardBottomPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
        ) {
            Text(
                text = stringResource(R.string.settings_calling_card_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MessagingSpacing.messagingTitleBottomPadding),
            )

            val rowSize = 2
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
            ) {
                callingOptions.chunked(rowSize).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MessagingSpacing.optionSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowOptions.forEach { option ->
                            CallingOptionChip(
                                option = option,
                                selected = selectedApp == option.app,
                                onClick = { onCallingAppSelected(option.app) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(rowSize - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallingOptionChip(
    option: CallingOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val backgroundColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }

    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
                .border(
                    width = MessagingSpacing.borderWidth,
                    color = borderColor,
                    shape = MaterialTheme.shapes.large,
                ).selectable(
                    selected = selected,
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                    role = Role.RadioButton,
                ).padding(vertical = MessagingSpacing.chipVerticalPadding, horizontal = MessagingSpacing.chipHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MessagingSpacing.chipIconSpacing),
    ) {
        CallingOptionIcon(app = option.app)
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CallingOptionIcon(app: CallingApp) {
    when (app) {
        CallingApp.CALL -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }
        CallingApp.GOOGLE_MEET -> {
            Image(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }
        CallingApp.WHATSAPP -> {
            Image(
                painter = painterResource(id = R.drawable.whatsapp_call),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }
        CallingApp.TELEGRAM -> {
            Image(
                painter = painterResource(id = R.drawable.telegram_call),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }
        CallingApp.SIGNAL -> {
            Image(
                painter = painterResource(id = R.drawable.signal_call),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }
    }
}

@Composable
private fun MessagingOptionChip(
    option: MessagingOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val backgroundColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }

    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .background(backgroundColor)
                .border(
                    width = MessagingSpacing.borderWidth,
                    color = borderColor,
                    shape = MaterialTheme.shapes.large,
                ).selectable(
                    selected = selected,
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                    role = Role.RadioButton,
                ).padding(vertical = MessagingSpacing.chipVerticalPadding, horizontal = MessagingSpacing.chipHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MessagingSpacing.chipIconSpacing),
    ) {
        MessagingOptionIcon(app = option.app)
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }

        MessagingApp.WHATSAPP -> {
            Image(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }

        MessagingApp.TELEGRAM -> {
            Image(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                modifier = Modifier.size(MessagingSpacing.iconSize),
            )
        }

        MessagingApp.SIGNAL -> {
            Image(
                painter = painterResource(id = R.drawable.signal),
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.SignalMessageIconSize),
            )
        }
    }
}

/**
 * Consolidated Calls & Texts settings section that includes all messaging-related logic.
 * This combines the UI components with the business logic for messaging app selection and direct dial.
 */
@Composable
fun CallsTextsSettingsSection(
    messagingApp: MessagingApp,
    callingApp: CallingApp,
    onSetMessagingApp: (MessagingApp) -> Unit,
    onSetCallingApp: (CallingApp) -> Unit,
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    hasCallPermission: Boolean,
    hasContactPermission: Boolean,
    onNavigateToPermissions: () -> Unit,
    contactsSectionEnabled: Boolean = true,
    isWhatsAppInstalled: Boolean = false,
    isTelegramInstalled: Boolean = false,
    isSignalInstalled: Boolean = false,
    isGoogleMeetInstalled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!contactsSectionEnabled) {
        return
    }

    val context = LocalContext.current

    // Show tip banner if contacts permission is not granted
    if (!hasContactPermission) {
        val linkText = stringResource(R.string.settings_contacts_permission_give_permission)
        val fullText = stringResource(R.string.settings_contacts_permission_needed_message, linkText)
        val linkTag = "give_permission"

        val annotatedText =
            buildAnnotatedString {
                append(fullText.replace(linkText, ""))
                withStyle(
                    style =
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                ) {
                    append(linkText)
                    addStringAnnotation(
                        tag = linkTag,
                        annotation = "give_permission",
                        start = length - linkText.length,
                        end = length,
                    )
                }
            }

        TipBanner(
            annotatedText = annotatedText,
            onTextClick = { offset ->
                val annotations =
                    annotatedText.getStringAnnotations(
                        tag = linkTag,
                        start = offset,
                        end = offset,
                    )
                if (annotations.isNotEmpty()) {
                    onNavigateToPermissions()
                }
            },
            showDismissButton = false,
            modifier = Modifier.padding(bottom = DesignTokens.SpacingMedium),
        )
    }

    // Callback for messaging app selection with installation check
    val onCallingAppSelected: (CallingApp) -> Unit = { app ->
        val isInstalled =
            when (app) {
                CallingApp.CALL -> true
                CallingApp.GOOGLE_MEET -> isGoogleMeetInstalled
                CallingApp.WHATSAPP -> isWhatsAppInstalled
                CallingApp.TELEGRAM -> isTelegramInstalled
                CallingApp.SIGNAL -> isSignalInstalled
            }

        if (isInstalled) {
            onSetCallingApp(app)
        } else {
            val appName =
                when (app) {
                    CallingApp.CALL -> context.getString(R.string.settings_calling_option_call)
                    CallingApp.GOOGLE_MEET -> context.getString(R.string.settings_calling_option_google_meet)
                    CallingApp.WHATSAPP -> context.getString(R.string.settings_calling_option_whatsapp)
                    CallingApp.TELEGRAM -> context.getString(R.string.settings_calling_option_telegram)
                    CallingApp.SIGNAL -> context.getString(R.string.settings_calling_option_signal)
                }
            Toast
                .makeText(
                    context,
                    context.getString(
                        R.string.settings_messaging_app_not_installed,
                        appName,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    // Callback for messaging app selection with installation check
    val onMessagingAppSelected: (MessagingApp) -> Unit = { app ->
        val isInstalled =
            when (app) {
                MessagingApp.MESSAGES -> true

                // Messages is always available
                MessagingApp.WHATSAPP -> isWhatsAppInstalled

                MessagingApp.TELEGRAM -> isTelegramInstalled

                MessagingApp.SIGNAL -> isSignalInstalled
            }

        if (isInstalled) {
            onSetMessagingApp(app)
        } else {
            val appName =
                when (app) {
                    MessagingApp.WHATSAPP -> {
                        context.getString(R.string.settings_messaging_option_whatsapp)
                    }

                    MessagingApp.TELEGRAM -> {
                        context.getString(R.string.settings_messaging_option_telegram)
                    }

                    MessagingApp.SIGNAL -> {
                        context.getString(R.string.settings_messaging_option_signal)
                    }

                    MessagingApp.MESSAGES -> {
                        context.getString(R.string.settings_messaging_option_messages)
                    }
                }
            Toast
                .makeText(
                    context,
                    context.getString(
                        R.string.settings_messaging_app_not_installed,
                        appName,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    MessagingSection(
        messagingApp = messagingApp,
        callingApp = callingApp,
        onSetMessagingApp = onSetMessagingApp,
        onSetCallingApp = onSetCallingApp,
        directDialEnabled = directDialEnabled,
        onToggleDirectDial = onToggleDirectDial,
        hasCallPermission = hasCallPermission,
        contactsSectionEnabled = contactsSectionEnabled,
        isWhatsAppInstalled = isWhatsAppInstalled,
        isTelegramInstalled = isTelegramInstalled,
        isSignalInstalled = isSignalInstalled,
        isGoogleMeetInstalled = isGoogleMeetInstalled,
        onCallingAppSelected = onCallingAppSelected,
        onMessagingAppSelected = onMessagingAppSelected,
        showTitle = false,
        modifier = modifier,
    )
}
