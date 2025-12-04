package com.tk.quicksearch.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.data.AppUsageRepository
import com.tk.quicksearch.data.ContactRepository
import com.tk.quicksearch.data.FileSearchRepository

@Composable
fun PermissionsScreen(
    onPermissionsComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appUsageRepository = remember { AppUsageRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val fileRepository = remember { FileSearchRepository(context) }

    // Permission states
    var usagePermissionState by remember {
        mutableStateOf(createInitialPermissionState(appUsageRepository.hasUsageAccess()))
    }
    var contactsPermissionState by remember {
        mutableStateOf(createInitialPermissionState(contactRepository.hasPermission()))
    }
    var filesPermissionState by remember {
        mutableStateOf(createInitialPermissionState(fileRepository.hasPermission()))
    }

    // Runtime permissions launcher (for contacts and files on pre-R)
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        contactsPermissionState = updatePermissionState(contactsGranted, contactsGranted)
        
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

    // All files access launcher (for Android R+)
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
                val newUsagePermission = appUsageRepository.hasUsageAccess()
                val newContactsPermission = contactRepository.hasPermission()
                val newFilesPermission = fileRepository.hasPermission()

                usagePermissionState = updatePermissionState(newUsagePermission, newUsagePermission)
                
                if (newContactsPermission) {
                    contactsPermissionState = updatePermissionState(newContactsPermission, true)
                }
                
                if (newFilesPermission) {
                    filesPermissionState = updatePermissionState(newFilesPermission, true, wasDenied = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.permissions_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.permissions_screen_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(1.dp))

                // Usage Permission Card
                PermissionCard(
                    title = stringResource(R.string.permissions_usage_title),
                    description = stringResource(R.string.permissions_usage_desc),
                    permissionState = usagePermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        if (enabled && !usagePermissionState.isGranted) {
                            context.startActivity(
                                PermissionRequestHandler.createUsageAccessIntent(context)
                            )
                        }
                    }
                )

                // Contacts Permission Card (Optional)
                PermissionCard(
                    title = stringResource(R.string.permissions_contacts_title),
                    description = stringResource(R.string.permissions_contacts_desc),
                    permissionState = contactsPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        contactsPermissionState = contactsPermissionState.copy(isEnabled = enabled)
                        if (enabled && !contactsPermissionState.isGranted) {
                            runtimePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.READ_CONTACTS)
                            )
                        }
                    }
                )

                // Files Permission Card (Optional)
                PermissionCard(
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
                                runtimeLauncher = runtimePermissionsLauncher,
                                allFilesLauncher = allFilesAccessLauncher,
                                onStateUpdate = { newState ->
                                    filesPermissionState = newState
                                }
                            )
                        }
                    }
                )
            }

            // Continue button - enabled when at least one permission is granted
            Button(
                onClick = onPermissionsComplete,
                enabled = usagePermissionState.isGranted || 
                         contactsPermissionState.isGranted || 
                         filesPermissionState.isGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.permissions_action_continue),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Creates initial permission state based on whether permission is granted.
 */
private fun createInitialPermissionState(isGranted: Boolean): PermissionState {
    return if (isGranted) {
        PermissionState.granted()
    } else {
        PermissionState.initial()
    }
}

/**
 * Updates permission state based on grant status and enabled flag.
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
 * Handles files permission request based on Android version and current state.
 */
private fun handleFilesPermissionRequest(
    context: Context,
    permissionState: PermissionState,
    runtimeLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    allFilesLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStateUpdate: (PermissionState) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
