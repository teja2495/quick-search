package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.onboarding.permissionScreen.PermissionState
import com.tk.quicksearch.search.data.AppUsageRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository

private val GrantedPermissionColor = Color(0xFF4CAF50)

/**
 * Data class representing a permission item.
 */
private data class PermissionItem(
    val name: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val permissionState: PermissionState,
    val onRequest: () -> Unit
)

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
    modifier: Modifier = Modifier
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
        mutableStateOf(createInitialPermissionState(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ))
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        contactsPermissionState = updatePermissionState(contactsGranted, contactsGranted)

        val callingGranted = permissions[Manifest.permission.CALL_PHONE] == true
        callingPermissionState = updatePermissionState(callingGranted, callingGranted)

        // Handle files permission for pre-R Android
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            val filesGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            filesPermissionState = updatePermissionState(
                filesGranted,
                filesGranted,
                wasDenied = !filesGranted
            )
        }
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val filesGranted = PermissionRequestHandler.checkFilesPermission(context)
        filesPermissionState = updatePermissionState(filesGranted, filesGranted)
    }

    // Refresh permissions when activity resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
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

                val hasCallingPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                callingPermissionState = updatePermissionState(hasCallingPermission, hasCallingPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.ExtraLargeCardShape
        ) {
            Column {
                val permissions = listOf(
                    PermissionItem(
                        name = stringResource(R.string.settings_usage_access_title),
                        subtitle = stringResource(R.string.permissions_usage_desc),
                        icon = Icons.Rounded.Info,
                        permissionState = usagePermissionState,
                        onRequest = {
                            usagePermissionState = usagePermissionState.copy(isEnabled = true)
                            context.startActivity(
                                PermissionRequestHandler.createUsageAccessIntent(context)
                            )
                            // Also call the original callback for backward compatibility
                            onRequestUsagePermission()
                        }
                    ),
                    PermissionItem(
                        name = stringResource(R.string.settings_contacts_permission_title),
                        subtitle = stringResource(R.string.permissions_contacts_desc),
                        icon = Icons.Rounded.Contacts,
                        permissionState = contactsPermissionState,
                        onRequest = {
                            contactsPermissionState = contactsPermissionState.copy(isEnabled = true)
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.READ_CONTACTS)
                            )
                            // Also call the original callback for backward compatibility
                            onRequestContactPermission()
                        }
                    ),
                    PermissionItem(
                        name = stringResource(R.string.settings_files_permission_title),
                        subtitle = stringResource(R.string.permissions_files_desc),
                        icon = Icons.Rounded.InsertDriveFile,
                        permissionState = filesPermissionState,
                        onRequest = {
                            filesPermissionState = filesPermissionState.copy(isEnabled = true)
                            handleFilesPermissionRequest(
                                context = context,
                                permissionState = filesPermissionState,
                                runtimeLauncher = multiplePermissionsLauncher,
                                allFilesLauncher = allFilesAccessLauncher,
                                onStateUpdate = { newState ->
                                    filesPermissionState = newState
                                }
                            )
                            // Also call the original callback for backward compatibility
                            onRequestFilePermission()
                        }
                    ),
                    PermissionItem(
                        name = stringResource(R.string.settings_call_permission_title),
                        subtitle = stringResource(R.string.permissions_calling_desc),
                        icon = Icons.Rounded.Call,
                        permissionState = callingPermissionState,
                        onRequest = {
                            callingPermissionState = callingPermissionState.copy(isEnabled = true)
                            multiplePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.CALL_PHONE)
                            )
                            // Also call the original callback for backward compatibility
                            onRequestCallPermission()
                        }
                    )
                )

                permissions.forEachIndexed { index, permission ->
                    PermissionRow(
                        permission = permission,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (index != permissions.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    permission: PermissionItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = permission.icon,
            contentDescription = permission.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)
        ) {
            Text(
                text = permission.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = permission.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .widthIn(min = 80.dp)
                .heightIn(min = 40.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (permission.permissionState.isGranted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.settings_usage_access_granted),
                    tint = GrantedPermissionColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(onClick = permission.onRequest) {
                    Text(
                        text = stringResource(R.string.settings_permission_grant),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Creates initial permission state. If permission is already granted, creates a granted state.
 * Otherwise creates an initial state (not granted, not enabled).
 */
private fun createInitialPermissionState(isGranted: Boolean): PermissionState {
    return if (isGranted) {
        PermissionState.granted()
    } else {
        PermissionState.initial()
    }
}

/**
 * Creates a new permission state with the given parameters.
 * Used to update permission states after permission checks or user interactions.
 */
private fun updatePermissionState(
    isGranted: Boolean,
    isEnabled: Boolean,
    wasDenied: Boolean = false
): PermissionState {
    return PermissionState(
        isGranted = isGranted,
        isEnabled = isEnabled,
        wasDenied = wasDenied
    )
}

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
    onStateUpdate: (PermissionState) -> Unit
) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        // Android R+ - always open settings
        PermissionRequestHandler.launchAllFilesAccessRequest(allFilesLauncher, context)
    } else {
        // Pre-R Android - check if we should open settings or request permission
        if (PermissionRequestHandler.shouldOpenSettingsForFiles(
                context,
                permissionState.wasDenied
            )
        ) {
            context.startActivity(
                PermissionRequestHandler.createAppSettingsIntent(context)
            )
        } else {
            // First time or can show rationale - request permission
            runtimeLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}