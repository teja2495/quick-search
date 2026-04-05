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
    showCalendarPermission: Boolean = true,
    showCallingPermission: Boolean = true,
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
                val contactsWasPreviouslyDenied = contactsPermissionState.wasDenied
                val shouldShowContactsRationale =
                    PermissionHelper.shouldShowRuntimePermissionRationale(
                        context = context,
                        permission = Manifest.permission.READ_CONTACTS,
                    )
                val shouldOpenContactsSettings =
                    !contactsGranted &&
                        contactsWasPreviouslyDenied &&
                        !shouldShowContactsRationale
                contactsPermissionState =
                    updatePermissionState(
                        isGranted = contactsGranted,
                        isEnabled = contactsGranted,
                        wasDenied =
                            !contactsGranted &&
                                (
                                    contactsWasPreviouslyDenied ||
                                        shouldShowContactsRationale
                                ),
                    )

                if (shouldOpenContactsSettings) {
                    PermissionHelper.launchAppSettingsRequest(context)
                }
            }

            permissions[Manifest.permission.CALL_PHONE]?.let { callingGranted ->
                val callingWasPreviouslyDenied = callingPermissionState.wasDenied
                val shouldShowCallingRationale =
                    PermissionHelper.shouldShowRuntimePermissionRationale(
                        context = context,
                        permission = Manifest.permission.CALL_PHONE,
                    )
                val shouldOpenCallingSettings =
                    !callingGranted &&
                        callingWasPreviouslyDenied &&
                        !shouldShowCallingRationale
                callingPermissionState =
                    updatePermissionState(
                        isGranted = callingGranted,
                        isEnabled = callingGranted,
                        wasDenied =
                            !callingGranted &&
                                (
                                    callingWasPreviouslyDenied ||
                                        shouldShowCallingRationale
                                ),
                    )

                if (shouldOpenCallingSettings) {
                    PermissionHelper.launchAppSettingsRequest(context)
                }
            }

            permissions[Manifest.permission.READ_CALENDAR]?.let { calendarGranted ->
                val calendarWasPreviouslyDenied = calendarPermissionState.wasDenied
                val shouldShowCalendarRationale =
                    PermissionHelper.shouldShowRuntimePermissionRationale(
                        context = context,
                        permission = Manifest.permission.READ_CALENDAR,
                    )
                val shouldOpenCalendarSettings =
                    !calendarGranted &&
                        calendarWasPreviouslyDenied &&
                        !shouldShowCalendarRationale
                calendarPermissionState =
                    updatePermissionState(
                        isGranted = calendarGranted,
                        isEnabled = calendarGranted,
                        wasDenied =
                            !calendarGranted &&
                                (
                                    calendarWasPreviouslyDenied ||
                                        shouldShowCalendarRationale
                                ),
                    )

                if (shouldOpenCalendarSettings) {
                    PermissionHelper.launchAppSettingsRequest(context)
                }
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
            calendar = if (showCalendarPermission) calendarPermissionState else PermissionState.granted(),
            calling = if (showCallingPermission) callingPermissionState else PermissionState.granted(),
        )
    LaunchedEffect(states) {
        onStatesChanged(states)
    }

    val items =
        buildList {
            add(
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
            )
            add(
                PermissionCardItem(
                    title = texts.contactsTitle,
                    description = texts.contactsDescription,
                    permissionState = contactsPermissionState,
                    isMandatory = false,
                    onToggleChange = { enabled ->
                        contactsPermissionState = contactsPermissionState.copy(isEnabled = enabled)
                        if (enabled && !contactsPermissionState.isGranted) {
                            runCatching {
                                multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                            }.onFailure {
                                PermissionHelper.launchAppSettingsRequest(context)
                            }
                        }
                    },
                ),
            )
            add(
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
            )
            if (showCalendarPermission) {
                add(
                    PermissionCardItem(
                        title = texts.calendarTitle,
                        description = texts.calendarDescription,
                        permissionState = calendarPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            calendarPermissionState = calendarPermissionState.copy(isEnabled = enabled)
                            if (enabled && !calendarPermissionState.isGranted) {
                                runCatching {
                                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
                                }.onFailure {
                                    PermissionHelper.launchAppSettingsRequest(context)
                                }
                            }
                        },
                    ),
                )
            }
            if (showCallingPermission) {
                add(
                    PermissionCardItem(
                        title = texts.callingTitle,
                        description = texts.callingDescription,
                        permissionState = callingPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            callingPermissionState = callingPermissionState.copy(isEnabled = enabled)
                            if (enabled && !callingPermissionState.isGranted) {
                                runCatching {
                                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                                }.onFailure {
                                    PermissionHelper.launchAppSettingsRequest(context)
                                }
                            }
                        },
                    ),
                )
            }
        }

    PermissionCard(
        items = items,
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
