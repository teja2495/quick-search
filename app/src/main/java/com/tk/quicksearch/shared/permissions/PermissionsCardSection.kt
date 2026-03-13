package com.tk.quicksearch.shared.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.onboarding.permissionScreen.PermissionCard
import com.tk.quicksearch.onboarding.permissionScreen.PermissionCardItem
import com.tk.quicksearch.onboarding.permissionScreen.PermissionState
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository

data class PermissionCardTexts(
    val usageTitle: String,
    val usageDescription: String,
    val contactsTitle: String,
    val contactsDescription: String,
    val filesTitle: String,
    val filesDescription: String,
    val calendarTitle: String,
    val calendarDescription: String,
    val callingTitle: String,
    val callingDescription: String,
)

data class PermissionCardStates(
    val usage: PermissionState = PermissionState.initial(),
    val contacts: PermissionState = PermissionState.initial(),
    val files: PermissionState = PermissionState.initial(),
    val calendar: PermissionState = PermissionState.initial(),
    val calling: PermissionState = PermissionState.initial(),
)

@Composable
fun PermissionsCardSection(
    texts: PermissionCardTexts,
    cardContainer: @Composable (modifier: Modifier, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onRequestUsagePermission: () -> Unit = {},
    onRequestContactPermission: () -> Unit = {},
    onRequestFilePermission: () -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onRequestCallPermission: () -> Unit = {},
    onStatesChanged: (PermissionCardStates) -> Unit = {},
) {
    val context = LocalContext.current
    val appsRepository = remember { AppsRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val fileRepository = remember { FileSearchRepository(context) }
    val calendarRepository = remember { CalendarRepository(context) }

    var usagePermissionState by remember {
        mutableStateOf(createInitialPermissionState(appsRepository.hasUsageAccess()))
    }
    var contactsPermissionState by remember {
        mutableStateOf(createInitialPermissionState(contactRepository.hasPermission()))
    }
    var filesPermissionState by remember {
        mutableStateOf(createInitialPermissionState(fileRepository.hasPermission()))
    }
    var calendarPermissionState by remember {
        mutableStateOf(createInitialPermissionState(calendarRepository.hasPermission()))
    }
    var callingPermissionState by remember {
        mutableStateOf(
            createInitialPermissionState(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

    val multiplePermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            permissions[Manifest.permission.READ_CONTACTS]?.let { contactsGranted ->
                contactsPermissionState =
                    updatePermissionState(
                        isGranted = contactsGranted,
                        isEnabled = contactsGranted,
                        wasDenied = !contactsGranted,
                    )
            }

            permissions[Manifest.permission.CALL_PHONE]?.let { callingGranted ->
                callingPermissionState =
                    updatePermissionState(
                        isGranted = callingGranted,
                        isEnabled = callingGranted,
                        wasDenied = !callingGranted,
                    )
            }

            permissions[Manifest.permission.READ_CALENDAR]?.let { calendarGranted ->
                calendarPermissionState =
                    updatePermissionState(
                        isGranted = calendarGranted,
                        isEnabled = calendarGranted,
                        wasDenied = !calendarGranted,
                    )
            }

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE]?.let { filesGranted ->
                    filesPermissionState =
                        updatePermissionState(
                            filesGranted,
                            filesGranted,
                            wasDenied = !filesGranted,
                        )
                }
            }
        }

    val allFilesAccessLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val filesGranted = PermissionHelper.checkFilesPermission(context)
            filesPermissionState = updatePermissionState(filesGranted, filesGranted)
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val hasUsageAccess = appsRepository.hasUsageAccess()
                    val hasContactsPermission = contactRepository.hasPermission()
                    val hasFilesPermission = fileRepository.hasPermission()
                    val hasCalendarPermission = calendarRepository.hasPermission()

                    usagePermissionState = updatePermissionState(hasUsageAccess, hasUsageAccess)

                    if (hasContactsPermission) {
                        contactsPermissionState = updatePermissionState(hasContactsPermission, true)
                    }

                    if (hasFilesPermission) {
                        filesPermissionState = updatePermissionState(hasFilesPermission, true, wasDenied = false)
                    }

                    if (hasCalendarPermission) {
                        calendarPermissionState = updatePermissionState(hasCalendarPermission, true, wasDenied = false)
                    }

                    val hasCallingPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                            PackageManager.PERMISSION_GRANTED
                    callingPermissionState =
                        if (hasCallingPermission) {
                            updatePermissionState(isGranted = true, isEnabled = true, wasDenied = false)
                        } else {
                            callingPermissionState.copy(isGranted = false)
                        }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val states =
        PermissionCardStates(
            usage = usagePermissionState,
            contacts = contactsPermissionState,
            files = filesPermissionState,
            calendar = calendarPermissionState,
            calling = callingPermissionState,
        )
    LaunchedEffect(states) {
        onStatesChanged(states)
    }

    PermissionCard(
        items =
            listOf(
                PermissionCardItem(
                    title = texts.usageTitle,
                    description = texts.usageDescription,
                    permissionState = usagePermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        if (enabled && !usagePermissionState.isGranted) {
                            usagePermissionState = usagePermissionState.copy(isEnabled = true)
                            PermissionHelper.launchUsageAccessRequest(context)
                            onRequestUsagePermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = texts.contactsTitle,
                    description = texts.contactsDescription,
                    permissionState = contactsPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        contactsPermissionState = contactsPermissionState.copy(isEnabled = enabled)
                        if (enabled && !contactsPermissionState.isGranted) {
                            PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                context = context,
                                permission = Manifest.permission.READ_CONTACTS,
                                wasPreviouslyDenied = contactsPermissionState.wasDenied,
                                runtimeLauncher = multiplePermissionsLauncher,
                            )
                            onRequestContactPermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = texts.filesTitle,
                    description = texts.filesDescription,
                    permissionState = filesPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        filesPermissionState = filesPermissionState.copy(isEnabled = enabled)
                        if (enabled && !filesPermissionState.isGranted) {
                            PermissionHelper.requestFilesPermission(
                                context = context,
                                wasPreviouslyDenied = filesPermissionState.wasDenied,
                                runtimeLauncher = multiplePermissionsLauncher,
                                allFilesLauncher = allFilesAccessLauncher,
                            )
                            onRequestFilePermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = texts.calendarTitle,
                    description = texts.calendarDescription,
                    permissionState = calendarPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        calendarPermissionState = calendarPermissionState.copy(isEnabled = enabled)
                        if (enabled && !calendarPermissionState.isGranted) {
                            PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                context = context,
                                permission = Manifest.permission.READ_CALENDAR,
                                wasPreviouslyDenied = calendarPermissionState.wasDenied,
                                runtimeLauncher = multiplePermissionsLauncher,
                            )
                            onRequestCalendarPermission()
                        }
                    },
                ),
                PermissionCardItem(
                    title = texts.callingTitle,
                    description = texts.callingDescription,
                    permissionState = callingPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        callingPermissionState = callingPermissionState.copy(isEnabled = enabled)
                        if (enabled && !callingPermissionState.isGranted) {
                            PermissionHelper.requestRuntimePermissionOrOpenSettings(
                                context = context,
                                permission = Manifest.permission.CALL_PHONE,
                                wasPreviouslyDenied = callingPermissionState.wasDenied,
                                runtimeLauncher = multiplePermissionsLauncher,
                            )
                            onRequestCallPermission()
                        }
                    },
                ),
            ),
        modifier = modifier,
        cardContainer = cardContainer,
    )
}

private fun createInitialPermissionState(isGranted: Boolean): PermissionState =
    if (isGranted) {
        PermissionState.granted()
    } else {
        PermissionState.initial()
    }

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
