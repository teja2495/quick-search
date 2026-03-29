package com.tk.quicksearch.onboarding.permissionScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.OnboardingHeader
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.shared.permissions.PermissionCardStates
import com.tk.quicksearch.shared.permissions.PermissionCardTexts
import com.tk.quicksearch.shared.permissions.PermissionsCardSection
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsScreenBackground

/**
 * Main permissions screen that allows users to grant optional permissions for enhanced functionality.
 * Displays cards for usage access, contacts, and files permissions with toggle controls.
 * Shows a reminder dialog if user tries to continue without granting all permissions.
 *
 * @param onPermissionsComplete Callback invoked when user wants to proceed (with or without permissions)
 * @param modifier Modifier for the composable
 */
@Composable
fun PermissionsScreen(
    onPermissionsComplete: () -> Unit,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    var permissionStates by remember { mutableStateOf(PermissionCardStates()) }

    var showPermissionReminderDialog by remember { mutableStateOf(false) }

    val totalSteps = if (!permissionStates.contacts.isGranted && !permissionStates.files.isGranted) 3 else 4

    SettingsScreenBackground(
        appTheme = AppTheme.MONOCHROME,
        overlayThemeIntensity = 0.5f,
        modifier = modifier,
    ) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = DesignTokens.OnboardingHorizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        OnboardingHeader(
            title = stringResource(R.string.permissions_screen_title),
            currentStep = currentStep,
            totalSteps = totalSteps,
        )

        Text(
            text = stringResource(R.string.permissions_screen_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
        )

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            PermissionsCardSection(
                texts =
                    PermissionCardTexts(
                        usageTitle = stringResource(R.string.permissions_usage_title),
                        usageDescription = stringResource(R.string.permissions_usage_desc),
                        contactsTitle = stringResource(R.string.permissions_contacts_title),
                        contactsDescription = stringResource(R.string.permissions_contacts_desc),
                        filesTitle = stringResource(R.string.permissions_files_title),
                        filesDescription = stringResource(R.string.permissions_files_desc),
                        calendarTitle = stringResource(R.string.permissions_calendar_title),
                        calendarDescription = stringResource(R.string.permissions_calendar_desc),
                        callingTitle = stringResource(R.string.permissions_calling_title),
                        callingDescription = stringResource(R.string.permissions_calling_desc),
                ),
                modifier = Modifier.fillMaxWidth(),
                showCalendarPermission = true,
                showCallingPermission = false,
                cardContainer = { cardModifier, content ->
                    SettingsCard(modifier = cardModifier) {
                        content()
                    }
                },
                onStatesChanged = { permissionStates = it },
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))

        Button(
            onClick = {
                val hasUngrantedPermissions =
                    !permissionStates.usage.isGranted ||
                        !permissionStates.contacts.isGranted ||
                        !permissionStates.files.isGranted ||
                        !permissionStates.calendar.isGranted ||
                        !permissionStates.calling.isGranted

                if (hasUngrantedPermissions) {
                    showPermissionReminderDialog = true
                } else {
                    onPermissionsComplete()
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
            shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
            contentPadding =
                PaddingValues(
                    horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                    vertical = DesignTokens.OnboardingButtonVerticalPadding,
                ),
        ) {
            Text(
                text = stringResource(R.string.setup_action_next),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
    }
    } // end SettingsScreenBackground

    // Permission reminder dialog
    if (showPermissionReminderDialog) {
        PermissionReminderDialog(
            usagePermissionState = permissionStates.usage,
            contactsPermissionState = permissionStates.contacts,
            filesPermissionState = permissionStates.files,
            calendarPermissionState = permissionStates.calendar,
            callingPermissionState = permissionStates.calling,
            onDismiss = { showPermissionReminderDialog = false },
            onContinue = {
                showPermissionReminderDialog = false
                onPermissionsComplete()
            },
        )
    }
}

/**
 * Dialog that reminds users they can grant permissions later from app settings.
 */
@Composable
private fun PermissionReminderDialog(
    usagePermissionState: PermissionState,
    contactsPermissionState: PermissionState,
    filesPermissionState: PermissionState,
    calendarPermissionState: PermissionState,
    callingPermissionState: PermissionState,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val permissionsList =
        listOfNotNull(
            stringResource(R.string.permissions_usage_title).takeIf { !usagePermissionState.isGranted },
            stringResource(R.string.permissions_contacts_title).takeIf { !contactsPermissionState.isGranted },
            stringResource(R.string.permissions_files_title).takeIf { !filesPermissionState.isGranted },
            stringResource(R.string.permissions_calendar_title).takeIf { !calendarPermissionState.isGranted },
            stringResource(R.string.permissions_calling_title).takeIf { !callingPermissionState.isGranted },
        ).joinToString(", ")

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AppColors.OverlayVeryHigh)
                    .blur(radius = DesignTokens.OnboardingDialogBlurRadius),
            contentAlignment = Alignment.Center,
        ) {
            AppAlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = stringResource(R.string.permissions_reminder_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Text(
                        text =
                            stringResource(
                                R.string.permissions_reminder_dialog_message,
                                permissionsList,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                confirmButton = {
                    Button(onClick = onContinue) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}
