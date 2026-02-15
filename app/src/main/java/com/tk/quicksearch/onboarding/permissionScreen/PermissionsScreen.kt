package com.tk.quicksearch.onboarding.permissionScreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.OnboardingHeader
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository

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
    val context = LocalContext.current
    val appsRepository = remember { AppsRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val fileRepository = remember { FileSearchRepository(context) }

    var usagePermissionState by remember {
        mutableStateOf(createInitialPermissionState(appsRepository.hasUsageAccess()))
    }
    var contactsPermissionState by remember {
        mutableStateOf(createInitialPermissionState(contactRepository.hasPermission()))
    }
    var filesPermissionState by remember {
        mutableStateOf(createInitialPermissionState(fileRepository.hasPermission()))
    }
    var callingPermissionState by remember {
        mutableStateOf(
            createInitialPermissionState(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

    var showPermissionReminderDialog by remember { mutableStateOf(false) }

    val totalSteps = if (!contactsPermissionState.isGranted && !filesPermissionState.isGranted) 2 else 3

    val multiplePermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
            contactsPermissionState = updatePermissionState(contactsGranted, contactsGranted)

            val callingGranted = permissions[Manifest.permission.CALL_PHONE] == true
            callingPermissionState = updatePermissionState(callingGranted, callingGranted)

            // Handle files permission for pre-R Android
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                val filesGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                filesPermissionState =
                    updatePermissionState(
                        filesGranted,
                        filesGranted,
                        wasDenied = !filesGranted,
                    )
            }
        }

    val allFilesAccessLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val filesGranted = PermissionRequestHandler.checkFilesPermission(context)
            filesPermissionState = updatePermissionState(filesGranted, filesGranted)
        }

    // Refresh permissions when activity resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val hasUsageAccess = appsRepository.hasUsageAccess()
                    val hasContactsPermission = contactRepository.hasPermission()
                    val hasFilesPermission = fileRepository.hasPermission()

                    usagePermissionState = updatePermissionState(hasUsageAccess, hasUsageAccess)

                    if (hasContactsPermission) {
                        contactsPermissionState = updatePermissionState(hasContactsPermission, true)
                    }

                    if (hasFilesPermission) {
                        filesPermissionState = updatePermissionState(hasFilesPermission, true, wasDenied = false)
                    }

                    val hasCallingPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    callingPermissionState = updatePermissionState(hasCallingPermission, hasCallingPermission)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
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
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                shape = RoundedCornerShape(20.dp), // Slightly more rounded as per modern design
            ) {
                Column {
                    // Usage Permission Item
                    PermissionItem(
                        title = stringResource(R.string.permissions_usage_title),
                        description = stringResource(R.string.permissions_usage_desc),
                        permissionState = usagePermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            if (enabled && !usagePermissionState.isGranted) {
                                PermissionRequestHandler.launchUsageAccessRequest(context)
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // Contacts Permission Item (Optional)
                    PermissionItem(
                        title = stringResource(R.string.permissions_contacts_title),
                        description = stringResource(R.string.permissions_contacts_desc),
                        permissionState = contactsPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            contactsPermissionState = contactsPermissionState.copy(isEnabled = enabled)
                            if (enabled && !contactsPermissionState.isGranted) {
                                multiplePermissionsLauncher.launch(
                                    arrayOf(Manifest.permission.READ_CONTACTS),
                                )
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // Files Permission Item (Optional)
                    PermissionItem(
                        title = stringResource(R.string.permissions_files_title),
                        description = stringResource(R.string.permissions_files_desc),
                        permissionState = filesPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            filesPermissionState = filesPermissionState.copy(isEnabled = enabled)
                            if (enabled && !filesPermissionState.isGranted) {
                                handleFilesPermissionRequest(
                                    context = context,
                                    permissionState = filesPermissionState,
                                    runtimeLauncher = multiplePermissionsLauncher,
                                    allFilesLauncher = allFilesAccessLauncher,
                                    onStateUpdate = { newState ->
                                        filesPermissionState = newState
                                    },
                                )
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    // Calling Permission Item (Optional)
                    PermissionItem(
                        title = stringResource(R.string.permissions_calling_title),
                        description = stringResource(R.string.permissions_calling_desc),
                        permissionState = callingPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            callingPermissionState = callingPermissionState.copy(isEnabled = enabled)
                            if (enabled && !callingPermissionState.isGranted) {
                                multiplePermissionsLauncher.launch(
                                    arrayOf(Manifest.permission.CALL_PHONE),
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.permissions_privacy_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val hasUngrantedPermissions =
                    !usagePermissionState.isGranted ||
                        !contactsPermissionState.isGranted ||
                        !filesPermissionState.isGranted ||
                        !callingPermissionState.isGranted

                if (hasUngrantedPermissions) {
                    showPermissionReminderDialog = true
                } else {
                    onPermissionsComplete()
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp), // Premium rounded button
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_action_next),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Permission reminder dialog
    if (showPermissionReminderDialog) {
        PermissionReminderDialog(
            usagePermissionState = usagePermissionState,
            contactsPermissionState = contactsPermissionState,
            filesPermissionState = filesPermissionState,
            callingPermissionState = callingPermissionState,
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
    callingPermissionState: PermissionState,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val permissionsList =
        listOfNotNull(
            stringResource(R.string.permissions_usage_title).takeIf { !usagePermissionState.isGranted },
            stringResource(R.string.permissions_contacts_title).takeIf { !contactsPermissionState.isGranted },
            stringResource(R.string.permissions_files_title).takeIf { !filesPermissionState.isGranted },
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
                    .background(Color.Black.copy(alpha = 0.75f))
                    .blur(radius = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            AlertDialog(
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

/**
 * Creates initial permission state. If permission is already granted, creates a granted state.
 * Otherwise creates an initial state (not granted, not enabled).
 */
private fun createInitialPermissionState(isGranted: Boolean): PermissionState =
    if (isGranted) {
        PermissionState.granted()
    } else {
        PermissionState.initial()
    }

/**
 * Creates a new permission state with the given parameters.
 * Used to update permission states after permission checks or user interactions.
 */
private fun updatePermissionState(
    isGranted: Boolean,
    isEnabled: Boolean,
    wasDenied: Boolean = false,
): PermissionState =
    PermissionState(
        isGranted = isGranted,
        isEnabled = isEnabled,
        wasDenied = wasDenied,
    )

/**
 * Handles files permission request with different logic for Android versions.
 * - Android R+: Always opens settings to request "All files access" permission
 * - Pre-R: Opens settings if permission was previously denied, otherwise requests runtime permission
 */
private fun handleFilesPermissionRequest(
    context: Context,
    permissionState: PermissionState,
    runtimeLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    allFilesLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStateUpdate: (PermissionState) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android R+ - always open settings
        PermissionRequestHandler.launchAllFilesAccessRequest(allFilesLauncher, context)
    } else {
        // Pre-R Android - check if we should open settings or request permission
        if (PermissionRequestHandler.shouldOpenSettingsForFiles(
                context,
                permissionState.wasDenied,
            )
        ) {
            PermissionRequestHandler.launchAppSettingsRequest(context)
        } else {
            // First time or can show rationale - request permission
            runtimeLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}
