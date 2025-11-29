package com.tk.quicksearch.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    var hasUsagePermission by remember { mutableStateOf(appUsageRepository.hasUsageAccess()) }
    var hasContactsPermission by remember { mutableStateOf(contactRepository.hasPermission()) }
    var hasFilesPermission by remember { mutableStateOf(fileRepository.hasPermission()) }
    var hasRequestedAllFilesAccess by remember { mutableStateOf(false) }
    var filesPermissionDenied by remember { mutableStateOf(false) }
    
    // Toggle states - all disabled (OFF) by default
    // Usage toggle only enabled when permission is actually granted
    var usageToggleEnabled by remember { mutableStateOf(appUsageRepository.hasUsageAccess()) }
    var contactsToggleEnabled by remember { mutableStateOf(false) }
    var filesToggleEnabled by remember { mutableStateOf(false) }

    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        hasContactsPermission = contactsGranted
        contactsToggleEnabled = contactsGranted
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val filesGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            hasFilesPermission = filesGranted
            filesToggleEnabled = filesGranted
            // Track if permission was denied
            if (!filesGranted) {
                filesPermissionDenied = true
            } else {
                filesPermissionDenied = false
            }
        }
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val filesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        hasFilesPermission = filesGranted
        filesToggleEnabled = filesGranted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Refresh permissions when activity resumes (e.g., user returns from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newUsagePermission = appUsageRepository.hasUsageAccess()
                val newContactsPermission = contactRepository.hasPermission()
                val newFilesPermission = fileRepository.hasPermission()
                
                // Update permission states
                hasUsagePermission = newUsagePermission
                hasContactsPermission = newContactsPermission
                hasFilesPermission = newFilesPermission
                
                // Update toggle states to match actual permissions
                // Usage toggle only enabled when permission is actually granted
                usageToggleEnabled = newUsagePermission
                if (newContactsPermission) contactsToggleEnabled = true
                if (newFilesPermission) {
                    filesToggleEnabled = true
                    filesPermissionDenied = false // Reset denied flag if granted
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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

            // Usage Permission Card (Mandatory)
            PermissionToggleCard(
                title = stringResource(R.string.permissions_usage_title),
                description = stringResource(R.string.permissions_usage_desc),
                isGranted = hasUsagePermission,
                isEnabled = usageToggleEnabled,
                isMandatory = true,
                onToggleChange = { enabled ->
                    if (enabled && !hasUsagePermission) {
                        // Request usage permission - toggle will be updated when permission is granted
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    // Toggle state is controlled by actual permission, not user action
                }
            )

            // Contacts Permission Card (Optional)
            PermissionToggleCard(
                title = stringResource(R.string.permissions_contacts_title),
                description = stringResource(R.string.permissions_contacts_desc),
                isGranted = hasContactsPermission,
                isEnabled = contactsToggleEnabled,
                isMandatory = false,
                onToggleChange = { enabled ->
                    contactsToggleEnabled = enabled
                    if (enabled && !hasContactsPermission) {
                        // Request contacts permission
                        runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                    }
                }
            )

            // Files Permission Card (Optional)
            PermissionToggleCard(
                title = stringResource(R.string.permissions_files_title),
                description = stringResource(R.string.permissions_files_desc),
                isGranted = hasFilesPermission,
                isEnabled = filesToggleEnabled,
                isMandatory = false,
                onToggleChange = { enabled ->
                    filesToggleEnabled = enabled
                    if (enabled && !hasFilesPermission) {
                        // Request files permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Always open settings for Android R+
                            val manageIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching {
                                allFilesAccessLauncher.launch(manageIntent)
                            }.onFailure {
                                val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                allFilesAccessLauncher.launch(fallback)
                            }
                        } else {
                            // For pre-R, check if permission was previously denied
                            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                                context as android.app.Activity,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            val permissionDenied = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            // If permission was denied and we can't show rationale, open settings
                            if (filesPermissionDenied || (permissionDenied && !shouldShowRationale)) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } else {
                                // First time or can show rationale - request permission
                                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Continue button - only enabled when usage permission is granted
            Button(
                onClick = onPermissionsComplete,
                enabled = hasUsagePermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.permissions_action_continue),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PermissionToggleCard(
    title: String,
    description: String,
    isGranted: Boolean,
    isEnabled: Boolean,
    isMandatory: Boolean,
    onToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isMandatory) {
                        Text(
                            text = "*",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isGranted) {
                // Show green checkmark when permission is granted
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.permissions_granted),
                    tint = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green color
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(28.dp)
                )
            } else {
                // Show toggle when permission is not granted
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { newValue ->
                        if (newValue) {
                            // User wants to enable - request permission
                            onToggleChange(true)
                        } else if (!isMandatory) {
                            // User wants to disable optional permission (just update UI state)
                            onToggleChange(false)
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

