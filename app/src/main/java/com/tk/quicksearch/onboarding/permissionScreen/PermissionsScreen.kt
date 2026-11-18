package com.tk.quicksearch.onboarding.permissionScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.onboarding.OnboardingBottomActionButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.OnboardingHeader
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.shared.permissions.PermissionCardStates
import com.tk.quicksearch.shared.permissions.PermissionsPageContent
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.components.PreserveHostKeyboardStateEffect
import com.tk.quicksearch.shared.ui.components.AppTextButton

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

    val hasAnyGrantedPermission =
        with(permissionStates) {
            listOf(usage, contacts, files, calendar, notificationAccess)
                .any { it.isGranted }
        }

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
            title = stringResource(R.string.settings_permissions_title),
            topPadding = DesignTokens.SpacingMedium,
        )

        PermissionsPageContent(
            isOnboarding = true,
            modifier = Modifier.weight(1f),
            onStatesChanged = { permissionStates = it },
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingXXLarge))

        OnboardingBottomActionButton(
            text =
                stringResource(
                    if (hasAnyGrantedPermission) R.string.setup_action_next else R.string.setup_action_skip_permissions,
                ),
            onClick = {
                val hasUngrantedPermissions =
                    !permissionStates.usage.isGranted ||
                        !permissionStates.contacts.isGranted ||
                        !permissionStates.files.isGranted ||
                        !permissionStates.calendar.isGranted ||
                        !permissionStates.notificationAccess.isGranted

                if (hasUngrantedPermissions) {
                    showPermissionReminderDialog = true
                } else {
                    onPermissionsComplete()
                }
            },
        )

        Spacer(modifier = Modifier.height(DesignTokens.SpacingXLarge))
    }
    } // end SettingsScreenBackground

    // Permission reminder dialog
    if (showPermissionReminderDialog) {
        PermissionReminderDialog(
            usagePermissionState = permissionStates.usage,
            contactsPermissionState = permissionStates.contacts,
            filesPermissionState = permissionStates.files,
            calendarPermissionState = permissionStates.calendar,
            notificationAccessPermissionState = permissionStates.notificationAccess,
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
    notificationAccessPermissionState: PermissionState,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val permissionsList =
        listOfNotNull(
            stringResource(R.string.settings_shortcut_usage_access).takeIf { !usagePermissionState.isGranted },
            stringResource(R.string.contacts_action_button_contacts).takeIf { !contactsPermissionState.isGranted },
            stringResource(R.string.permissions_files_title).takeIf { !filesPermissionState.isGranted },
            stringResource(R.string.settings_calendar_permission_title).takeIf { !calendarPermissionState.isGranted },
            stringResource(R.string.settings_shortcut_notification_listener).takeIf {
                !notificationAccessPermissionState.isGranted
            },
        ).joinToString(", ")

    PreserveHostKeyboardStateEffect()

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
                    Text(text = stringResource(R.string.permissions_reminder_dialog_title))
                },
                text = {
                    Text(
                        text =
                            stringResource(
                                R.string.permissions_reminder_dialog_message,
                                permissionsList,
                            ),
                    )
                },
                confirmButton = {
                    AppTextButton(onClick = onContinue) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    AppTextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}
