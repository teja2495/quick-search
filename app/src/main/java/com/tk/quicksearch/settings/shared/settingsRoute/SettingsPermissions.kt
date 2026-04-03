package com.tk.quicksearch.settings.shared

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionPermissionRequirement
import com.tk.quicksearch.search.core.SearchSectionPermissionRequirements
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.shared.permissions.PermissionSettingsDialog
import com.tk.quicksearch.shared.permissions.PermissionHelper

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
    val contactsWasDenied = remember { mutableStateOf(false) }
    val filesWasDenied = remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var settingsDialogTargetSection by remember { mutableStateOf<SearchSection?>(null) }

    val openSettingsWithDialog: (SearchSection) -> Unit = { section ->
        settingsDialogTargetSection = section
        showPermissionSettingsDialog = true
    }

    val openPermissionSettingsForSection: (SearchSection?) -> Unit = sectionHandler@{ section ->
        val requirement =
            section?.let(SearchSectionPermissionRequirements::requirementFor)
                ?: return@sectionHandler
        when (requirement) {
            SearchSectionPermissionRequirement.CONTACTS -> viewModel.openContactPermissionSettings()
            SearchSectionPermissionRequirement.FILES -> viewModel.openFilesPermissionSettings()
            SearchSectionPermissionRequirement.CALENDAR -> viewModel.openCalendarPermissionSettings()
        }
    }

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
                when (SearchSectionPermissionRequirements.requirementFor(section)) {
                    SearchSectionPermissionRequirement.CONTACTS -> {
                        contactsWasDenied.value = !contactsGranted
                        if (contactsGranted) {
                            viewModel.setSectionEnabled(section, true)
                        } else {
                            PermissionHelper.handleDeniedRuntimePermission(
                                context = context,
                                permission = Manifest.permission.READ_CONTACTS,
                                wasPreviouslyDenied = true,
                                onOpenSettings = {
                                    openSettingsWithDialog(SearchSection.CONTACTS)
                                },
                            )
                        }
                    }

                    SearchSectionPermissionRequirement.FILES -> {
                        filesWasDenied.value = !filesGranted
                        if (filesGranted ||
                            (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                    Environment.isExternalStorageManager()
                            )
                        ) {
                            viewModel.setSectionEnabled(section, true)
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            PermissionHelper.handleDeniedRuntimePermission(
                                context = context,
                                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                                wasPreviouslyDenied = true,
                                onOpenSettings = {
                                    openSettingsWithDialog(SearchSection.FILES)
                                },
                            )
                        }
                    }

                    SearchSectionPermissionRequirement.CALENDAR -> {
                        val calendarGranted = permissions[Manifest.permission.READ_CALENDAR] == true
                        if (calendarGranted) {
                            viewModel.setSectionEnabled(section, true)
                        } else {
                            PermissionHelper.handleDeniedRuntimePermission(
                                context = context,
                                permission = Manifest.permission.READ_CALENDAR,
                                wasPreviouslyDenied = true,
                                onOpenSettings = {
                                    openSettingsWithDialog(SearchSection.CALENDAR)
                                },
                            )
                        }
                    }

                    null -> Unit
                }
                pendingSectionEnable.value = null
            }
        }

    // Launcher for all files access (Android R+)
    val allFilesAccessLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val filesGranted = PermissionHelper.checkFilesPermission(context)

            // Refresh permission state
            viewModel.handleOptionalPermissionChange()

            // If permission was granted and we have a pending section, enable it
            if (pendingSectionEnable.value == SearchSection.FILES && filesGranted) {
                viewModel.setSectionEnabled(SearchSection.FILES, true)
                pendingSectionEnable.value = null
            }
        }

    val onToggleSection = androidx.compose.runtime.remember(viewModel, disabledSections) {
        { section: SearchSection, enabled: Boolean ->
            if (enabled) {
                // Check if section can be enabled (has permissions)
                if (viewModel.canEnableSection(section)) {
                    viewModel.setSectionEnabled(section, true)
                } else {
                    // Request permissions based on section
                    when (SearchSectionPermissionRequirements.requirementFor(section)) {
                        SearchSectionPermissionRequirement.CONTACTS -> {
                            pendingSectionEnable.value = section
                            PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                context = context,
                                permission = Manifest.permission.READ_CONTACTS,
                                wasPreviouslyDenied = contactsWasDenied.value,
                                runtimeLauncher = runtimePermissionsLauncher,
                                onOpenSettings = {
                                    openSettingsWithDialog(SearchSection.CONTACTS)
                                },
                            )
                        }

                        SearchSectionPermissionRequirement.FILES -> {
                            pendingSectionEnable.value = section
                            PermissionHelper.requestFilesPermission(
                                context = context,
                                wasPreviouslyDenied = filesWasDenied.value,
                                runtimeLauncher = runtimePermissionsLauncher,
                                allFilesLauncher = allFilesAccessLauncher,
                                onOpenSettings = {
                                    openSettingsWithDialog(SearchSection.FILES)
                                },
                            )
                        }

                        SearchSectionPermissionRequirement.CALENDAR -> {
                            pendingSectionEnable.value = section
                            PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                context = context,
                                permission = Manifest.permission.READ_CALENDAR,
                                wasPreviouslyDenied = false,
                                runtimeLauncher = runtimePermissionsLauncher,
                                onOpenSettings = {
                                    openSettingsWithDialog(SearchSection.CALENDAR)
                                },
                            )
                        }

                        null -> {
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

    if (showPermissionSettingsDialog) {
        val permissionRequirement =
            settingsDialogTargetSection?.let(SearchSectionPermissionRequirements::requirementFor)
        val permissionType =
            when (permissionRequirement) {
                SearchSectionPermissionRequirement.CONTACTS ->
                    context.getString(R.string.settings_contacts_permission_title)
                SearchSectionPermissionRequirement.FILES ->
                    context.getString(R.string.settings_files_permission_title)
                SearchSectionPermissionRequirement.CALENDAR ->
                    context.getString(R.string.settings_calendar_permission_title)
                null -> context.getString(R.string.settings_permissions_title)
            }
        PermissionSettingsDialog(
            permissionType = permissionType,
            onConfirm = {
                showPermissionSettingsDialog = false
                openPermissionSettingsForSection(settingsDialogTargetSection)
                settingsDialogTargetSection = null
            },
            onDismiss = {
                showPermissionSettingsDialog = false
                settingsDialogTargetSection = null
            },
        )
    }

    return onToggleSection
}

/** Creates a standardized permission request handler that tries popup first, then settings. */
@Composable
fun createPermissionRequestHandler(
    context: Context,
    permissionLauncher: ActivityResultLauncher<String>,
    permission: String,
    fallbackAction: () -> Unit,
): () -> Unit =
    PermissionHelper.rememberPermissionRequestHandler(
        context = context,
        permissionLauncher = permissionLauncher,
        permission = permission,
        fallbackAction = fallbackAction,
    )

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
    PermissionHelper.handlePermissionResult(
        isGranted = isGranted,
        context = context,
        permission = permission,
        onPermanentlyDenied = onPermanentlyDenied,
        onPermissionChanged = onPermissionChanged,
        onGranted = onGranted,
        onComplete = onComplete,
    )
}
