package com.tk.quicksearch.settings.shared

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.shared.util.hapticToggle

/**
 * Handles permission requests and section toggling for the settings screen. Returns a callback
 * function for toggling sections that handles permission requests.
 */
@Composable
fun rememberSectionToggleHandler(
    viewModel: SearchViewModel,
    disabledSections: Set<SearchSection>,
): (SearchSection, Boolean) -> Unit {
    val context = LocalContext.current
    val pendingSectionEnable = remember { mutableStateOf<SearchSection?>(null) }

    // Launcher for runtime permissions (contacts, files on pre-R)
    val runtimePermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
            val filesGranted =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                } else {
                    false
                }

            // Refresh permission state
            viewModel.handleOptionalPermissionChange()

            // If permission was granted and we have a pending section, enable it
            pendingSectionEnable.value?.let { section ->
                when (section) {
                    SearchSection.CONTACTS -> {
                        if (contactsGranted) {
                            viewModel.setSectionEnabled(section, true)
                        }
                    }

                    SearchSection.FILES -> {
                        if (filesGranted ||
                            (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                    Environment.isExternalStorageManager()
                            )
                        ) {
                            viewModel.setSectionEnabled(section, true)
                        }
                    }

                    else -> {}
                }
                pendingSectionEnable.value = null
            }
        }

    // Launcher for all files access (Android R+)
    val allFilesAccessLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val filesGranted = PermissionRequestHandler.checkFilesPermission(context)

            // Refresh permission state
            viewModel.handleOptionalPermissionChange()

            // If permission was granted and we have a pending section, enable it
            if (pendingSectionEnable.value == SearchSection.FILES && filesGranted) {
                viewModel.setSectionEnabled(SearchSection.FILES, true)
                pendingSectionEnable.value = null
            }
        }

    return androidx.compose.runtime.remember(viewModel, disabledSections) {
        { section: SearchSection, enabled: Boolean ->
            if (enabled) {
                // Check if section can be enabled (has permissions)
                if (viewModel.canEnableSection(section)) {
                    viewModel.setSectionEnabled(section, true)
                } else {
                    // Request permissions based on section
                    when (section) {
                        SearchSection.CONTACTS -> {
                            pendingSectionEnable.value = section
                            runtimePermissionsLauncher.launch(
                                arrayOf(Manifest.permission.READ_CONTACTS),
                            )
                        }

                        SearchSection.FILES -> {
                            pendingSectionEnable.value = section
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                PermissionRequestHandler.launchAllFilesAccessRequest(
                                    allFilesAccessLauncher,
                                    context,
                                )
                            } else {
                                runtimePermissionsLauncher.launch(
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                )
                            }
                        }

                        SearchSection.APPS -> {
                            // Apps section doesn't require permissions
                            viewModel.setSectionEnabled(section, true)
                        }

                        SearchSection.APP_SHORTCUTS -> {
                            viewModel.setSectionEnabled(section, true)
                        }

                        SearchSection.SETTINGS -> {
                            viewModel.setSectionEnabled(section, true)
                        }
                    }
                }
            } else {
                // Check if disabling this section would leave no sections enabled
                val enabledSectionsCount = SearchSection.values().count { it !in disabledSections }
                if (enabledSectionsCount <= 1 && section !in disabledSections) {
                    // This is the last enabled section, prevent disabling
                    Toast
                        .makeText(
                            context,
                            context.getString(
                                R.string.settings_sections_at_least_one_required,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    viewModel.setSectionEnabled(section, false)
                }
            }
        }
    }
}

/** Creates a standardized permission request handler that tries popup first, then settings. */
@Composable
fun createPermissionRequestHandler(
    context: Context,
    permissionLauncher: ActivityResultLauncher<String>,
    permission: String,
    fallbackAction: () -> Unit,
): () -> Unit =
    androidx.compose.runtime.remember(context, permissionLauncher, permission, fallbackAction) {
        {
            if (context !is Activity) {
                fallbackAction()
            } else {
                permissionLauncher.launch(permission)
            }
        }
    }

/** Handles the result of a permission request with standardized logic. */
fun handlePermissionResult(
    isGranted: Boolean,
    context: Context,
    permission: String,
    onPermanentlyDenied: () -> Unit,
    onPermissionChanged: () -> Unit,
    onGranted: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
) {
    onPermissionChanged()

    if (isGranted) {
        onGranted?.invoke()
    } else if (context is Activity) {
        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
        if (!shouldShowRationale) {
            // Permission permanently denied, open settings
            onPermanentlyDenied()
        }
    }

    onComplete?.invoke()
}