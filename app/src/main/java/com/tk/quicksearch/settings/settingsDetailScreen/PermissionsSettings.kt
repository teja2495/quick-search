package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionItem
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.onboarding.permissionScreen.PermissionState
import com.tk.quicksearch.search.data.AppUsageRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Permissions settings screen with permission status and request options.
 * Permission status is checked actively using PermissionUtils.
 */
@Composable
fun PermissionsSettings(
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appUsageRepository = remember { AppUsageRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val fileRepository = remember { FileSearchRepository(context) }

    var usagePermissionState by remember {
        mutableStateOf(createInitialPermissionState(appUsageRepository.hasUsageAccess()))
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
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

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
                    val hasUsageAccess = appUsageRepository.hasUsageAccess()
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
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    callingPermissionState = updatePermissionState(hasCallingPermission, hasCallingPermission)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.permissions_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.SpacingLarge),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column {
                // Usage Permission Item
                PermissionItem(
                    title = stringResource(R.string.settings_usage_access_title),
                    description = stringResource(R.string.permissions_usage_desc),
                    permissionState = usagePermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        if (enabled && !usagePermissionState.isGranted) {
                            usagePermissionState = usagePermissionState.copy(isEnabled = true)
                            context.startActivity(
                                PermissionRequestHandler.createUsageAccessIntent(context),
                            )
                            // Also call the original callback for backward compatibility
                            onRequestUsagePermission()
                        }
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = DesignTokens.SpacingXLarge),
                    thickness = DesignTokens.DividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Contacts Permission Item (Optional)
                PermissionItem(
                    title = stringResource(R.string.settings_contacts_permission_title),
                    description = stringResource(R.string.permissions_contacts_desc),
                    permissionState = contactsPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        contactsPermissionState = contactsPermissionState.copy(isEnabled = enabled)
                        if (enabled && !contactsPermissionState.isGranted) {
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.READ_CONTACTS),
                            )
                            // Also call the original callback for backward compatibility
                            onRequestContactPermission()
                        }
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = DesignTokens.SpacingXLarge),
                    thickness = DesignTokens.DividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Files Permission Item (Optional)
                PermissionItem(
                    title = stringResource(R.string.settings_files_permission_title),
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
                            // Also call the original callback for backward compatibility
                            onRequestFilePermission()
                        }
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = DesignTokens.SpacingXLarge),
                    thickness = DesignTokens.DividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Calling Permission Item (Optional)
                PermissionItem(
                    title = stringResource(R.string.settings_call_permission_title),
                    description = stringResource(R.string.permissions_calling_desc),
                    permissionState = callingPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        callingPermissionState = callingPermissionState.copy(isEnabled = enabled)
                        if (enabled && !callingPermissionState.isGranted) {
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.CALL_PHONE),
                            )
                            // Also call the original callback for backward compatibility
                            onRequestCallPermission()
                        }
                    },
                )
            }
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
    context: android.content.Context,
    permissionState: PermissionState,
    runtimeLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    allFilesLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    onStateUpdate: (PermissionState) -> Unit,
) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        // Android R+ - always open settings
        PermissionRequestHandler.launchAllFilesAccessRequest(allFilesLauncher, context)
    } else {
        // Pre-R Android - check if we should open settings or request permission
        if (PermissionRequestHandler.shouldOpenSettingsForFiles(
                context,
                permissionState.wasDenied,
            )
        ) {
            context.startActivity(
                PermissionRequestHandler.createAppSettingsIntent(context),
            )
        } else {
            // First time or can show rationale - request permission
            runtimeLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}
