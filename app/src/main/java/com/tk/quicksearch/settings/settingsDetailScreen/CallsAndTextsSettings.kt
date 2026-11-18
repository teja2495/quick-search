package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.search.contacts.models.ContactActionProvider
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.Manifest
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.models.ContactActionIconStyle
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.shared.permissions.PermissionSettingsDialog
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile

// Constants for consistent spacing
private object MessagingSpacing {
    val optionSpacing = DesignTokens.ItemRowSpacing
    val messagingTitleBottomPadding = DesignTokens.SectionTitleBottomPadding
    val chipVerticalPadding = DesignTokens.ChipVerticalPadding
    val chipHorizontalPadding = DesignTokens.ChipHorizontalPadding
    val chipIconSpacing = DesignTokens.ChipIconSpacing
    val iconSize = DesignTokens.LargeIconSize
    val selectedBorderWidth = 2.dp
}


private data class MessagingOption(
    val app: MessagingApp,
    val labelRes: Int,
)

/** A contact button choice waiting on the CALL_PHONE permission. */
private data class PendingContactButton(
    val action: ContactButtonAction,
    val isPrimary: Boolean,
)

/**
 * Default messaging app picker used during onboarding. Hidden when no third-party messaging app
 * is installed.
 *
 * @param messagingApp Currently selected messaging app, or null when the second contact button
 * isn't a messaging action
 * @param onMessagingAppSelected Callback when a messaging option is selected, handles installation check
 */
@Composable
fun MessagingSection(
    messagingApp: MessagingApp?,
    onMessagingAppSelected: (MessagingApp) -> Unit,
    installedContactProviders: Set<ContactActionProvider> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val hasAnyThirdPartyMessagingApp = installedContactProviders.any { it.messagingLabel != null }
    if (!hasAnyThirdPartyMessagingApp) {
        return
    }

    val messagingOptions =
        buildList {
            for (app in MessagingApp.entries) {
                val action = ContactButtonAction.fromMessagingApp(app)
                if (action.isAppInstalled(installedContactProviders)) {
                    add(MessagingOption(app, action.messagingLabelRes))
                }
            }
        }

    DefaultMessagingAppCard(
        messagingOptions = messagingOptions,
        selectedApp = messagingApp,
        onMessagingAppSelected = onMessagingAppSelected,
        modifier = modifier,
    )
}

@Composable
private fun DirectDialCard(
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    numberSearchEnabled: Boolean,
    onToggleNumberSearch: (Boolean) -> Unit,
    hasCallPermission: Boolean,
) {
    val context = LocalContext.current
    var showCallPermissionSettingsDialog by remember { mutableStateOf(false) }
    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                onToggleDirectDial(true)
            } else {
                PermissionHelper.handleDeniedRuntimePermission(
                    context = context,
                    permission = Manifest.permission.CALL_PHONE,
                    wasPreviouslyDenied = true,
                    onOpenSettings = { showCallPermissionSettingsDialog = true },
                )
            }
        }

    SegmentedGroup(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SegmentedToggleTile(
            title = stringResource(R.string.settings_direct_dial_title),
            description = stringResource(R.string.settings_direct_dial_desc),
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
            icon = MaterialSymbols.Call,
            index = 0,
            count = 2,
        )
        SegmentedToggleTile(
            title = stringResource(R.string.settings_number_search_title),
            description = stringResource(R.string.settings_number_search_desc),
            checked = numberSearchEnabled,
            onCheckedChange = onToggleNumberSearch,
            icon = MaterialSymbols.Search,
            index = 1,
            count = 2,
        )
    }

    if (showCallPermissionSettingsDialog) {
        PermissionSettingsDialog(
            permissionType = stringResource(R.string.settings_call_permission_title),
            onConfirm = {
                showCallPermissionSettingsDialog = false
                PermissionHelper.launchAppSettingsRequest(context)
            },
            onDismiss = {
                showCallPermissionSettingsDialog = false
            },
        )
    }
}

@Composable
private fun DefaultMessagingAppCard(
    messagingOptions: List<MessagingOption>,
    selectedApp: MessagingApp?,
    onMessagingAppSelected: (MessagingApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messagingOptions.isEmpty()) return

    SegmentedTile(index = 0, count = 1, modifier = modifier) {
        SegmentedTileContent(
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
private fun MessagingOptionChip(
    option: MessagingOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    // Tonal options like the settings choice group: a light accent tint, stronger with an accent
    // outline when selected, so brand icons stay legible on the fill.
    val borderColor = if (selected) AppColors.Accent else Color.Transparent
    val backgroundColor =
        if (selected) AppColors.AccentTintStrong else AppColors.AccentTintSubtle

    Column(
        modifier =
            modifier
                .clip(AppShapes.large)
                .background(backgroundColor)
                .border(
                    width = MessagingSpacing.selectedBorderWidth,
                    color = borderColor,
                    shape = AppShapes.large,
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
    val action = ContactButtonAction.fromMessagingApp(app)
    val drawable = action.drawableRes
    if (drawable != null) {
        Image(
            painter = painterResource(id = drawable),
            contentDescription = null,
            modifier = Modifier.size(
                if (action.iconStyle == ContactActionIconStyle.SIGNAL_MESSAGE) DesignTokens.SignalMessageIconSize
                else MessagingSpacing.iconSize,
            ),
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Sms,
            contentDescription = null,
            tint = AppColors.IconTintSecondary,
            modifier = Modifier.size(MessagingSpacing.iconSize),
        )
    }
}

/**
 * Calls & Texts settings: direct dial and number search, plus the default actions of the first
 * and second contact card buttons.
 */
@Composable
fun CallsTextsSettingsSection(
    primaryContactButton: ContactButtonAction,
    secondaryContactButton: ContactButtonAction,
    onSetPrimaryContactButton: (ContactButtonAction) -> Unit,
    onSetSecondaryContactButton: (ContactButtonAction) -> Unit,
    directDialEnabled: Boolean,
    onToggleDirectDial: (Boolean) -> Unit,
    numberSearchEnabled: Boolean,
    onToggleNumberSearch: (Boolean) -> Unit,
    hasCallPermission: Boolean,
    contactsSectionEnabled: Boolean = true,
    installedContactProviders: Set<ContactActionProvider> = emptySet(),
    modifier: Modifier = Modifier,
) {
    if (!contactsSectionEnabled) {
        return
    }

    val context = LocalContext.current
    var pendingContactButton by remember { mutableStateOf<PendingContactButton?>(null) }
    var showCallPermissionSettingsDialog by remember { mutableStateOf(false) }

    fun applyContactButton(
        action: ContactButtonAction,
        isPrimary: Boolean,
    ) {
        if (isPrimary) onSetPrimaryContactButton(action) else onSetSecondaryContactButton(action)
    }

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            val pending = pendingContactButton
            pendingContactButton = null
            if (isGranted && pending != null) {
                applyContactButton(pending.action, pending.isPrimary)
                return@rememberLauncherForActivityResult
            }
            if (!isGranted) {
                PermissionHelper.handleDeniedRuntimePermission(
                    context = context,
                    permission = Manifest.permission.CALL_PHONE,
                    wasPreviouslyDenied = true,
                    onOpenSettings = { showCallPermissionSettingsDialog = true },
                )
            }
        }

    fun onContactButtonSelected(
        action: ContactButtonAction,
        isPrimary: Boolean,
    ) {
        if (action.requiresCallPermission && !PermissionHelper.checkCallPermission(context)) {
            pendingContactButton = PendingContactButton(action, isPrimary)
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        } else {
            applyContactButton(action, isPrimary)
        }
    }

    val availableActions = remember(installedContactProviders) {
        ContactButtonAction.entries.filter { it.isAppInstalled(installedContactProviders) }
    }

    Column(modifier = modifier) {
        DirectDialCard(
            directDialEnabled = directDialEnabled,
            onToggleDirectDial = onToggleDirectDial,
            numberSearchEnabled = numberSearchEnabled,
            onToggleNumberSearch = onToggleNumberSearch,
            hasCallPermission = hasCallPermission,
        )

        ContactButtonsSettings(
            primaryContactButton = primaryContactButton,
            secondaryContactButton = secondaryContactButton,
            availableActions = availableActions,
            onPrimaryContactButtonSelected = { action -> onContactButtonSelected(action, isPrimary = true) },
            onSecondaryContactButtonSelected = { action -> onContactButtonSelected(action, isPrimary = false) },
            modifier = Modifier.padding(top = DesignTokens.SpacingMedium),
        )
    }

    if (showCallPermissionSettingsDialog) {
        PermissionSettingsDialog(
            permissionType = stringResource(R.string.settings_call_permission_title),
            onConfirm = {
                showCallPermissionSettingsDialog = false
                PermissionHelper.launchAppSettingsRequest(context)
            },
            onDismiss = {
                showCallPermissionSettingsDialog = false
            },
        )
    }
}
