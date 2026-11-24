package com.tk.quicksearch.shared.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.tk.quicksearch.reminders.ReminderPermissions
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
    val notificationsTitle: String,
    val notificationsDescription: String,
    val exactAlarmsTitle: String,
    val exactAlarmsDescription: String,
    val notificationAccessTitle: String,
    val notificationAccessDescription: String,
    val accessibilityTitle: String,
    val accessibilityDescription: String,
)

data class PermissionCardStates(
    val usage: PermissionState = PermissionState.initial(),
    val contacts: PermissionState = PermissionState.initial(),
    val files: PermissionState = PermissionState.initial(),
    val calendar: PermissionState = PermissionState.initial(),
    val calling: PermissionState = PermissionState.initial(),
    val notifications: PermissionState = PermissionState.initial(),
    val notificationAccess: PermissionState = PermissionState.initial(),
    val accessibility: PermissionState = PermissionState.initial(),
)

@Composable
fun PermissionsCardSection(
    texts: PermissionCardTexts,
    cardContainer: @Composable (modifier: Modifier, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    itemContentPadding: PaddingValues = PaddingValues(DesignTokens.SpacingXLarge),
    internalScrollEnabled: Boolean = false,
    segmented: Boolean = false,
    showItemIcons: Boolean = true,
    showCalendarPermission: Boolean = true,
    showCallingPermission: Boolean = true,
    showNotificationsPermission: Boolean = true,
    showAccessibilityPermission: Boolean = true,
    showNotificationAccessPermission: Boolean = true,
    onRequestUsagePermission: () -> Unit = {},
    onRequestFilePermission: () -> Unit = {},
    onRequestNotificationsPermission: () -> Unit = {},
    onStatesChanged: (PermissionCardStates) -> Unit = {},
) {
    val context = LocalContext.current
    val appsRepository = remember { AppsRepository(context) }
    val fileRepository = remember { FileSearchRepository(context) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    var accessibilityPermissionState by remember {
        mutableStateOf(createInitialPermissionState(LockScreenAccessibilityService.isEnabled(context)))
    }

    var usagePermissionState by remember {
        mutableStateOf(createInitialPermissionState(appsRepository.hasUsageAccess()))
    }
    var filesPermissionState by remember {
        mutableStateOf(createInitialPermissionState(fileRepository.hasPermission()))
    }
    val runtimeStates = remember(context) {
        mutableStateMapOf<RuntimePermissionRow, PermissionState>().apply {
            RuntimePermissionRow.entries.forEach { row ->
                put(row, createInitialPermissionState(row.isGranted(context)))
            }
        }
    }
    var notificationsPermissionState by remember {
        mutableStateOf(createInitialPermissionState(hasNotificationsPermission(context)))
    }
    var exactAlarmsPermissionState by remember {
        mutableStateOf(createInitialPermissionState(ReminderPermissions.canScheduleExactAlarms(context)))
    }
    var notificationAccessPermissionState by remember {
        mutableStateOf(
            createInitialPermissionState(
                NotificationDotsPermission.hasNotificationListenerAccess(context),
            ),
        )
    }

    val notificationsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { notificationsGranted ->
            val notificationsWasPreviouslyDenied = notificationsPermissionState.wasDenied
            val shouldShowNotificationsRationale =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    PermissionHelper.shouldShowRuntimePermissionRationale(
                        context = context,
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                    )
            val shouldOpenNotificationsSettings =
                !notificationsGranted &&
                    notificationsWasPreviouslyDenied &&
                    !shouldShowNotificationsRationale
            notificationsPermissionState =
                updatePermissionState(
                    isGranted = notificationsGranted,
                    isEnabled = notificationsGranted,
                    wasDenied = !notificationsGranted,
                )

            if (shouldOpenNotificationsSettings) {
                PermissionHelper.launchAppSettingsRequest(context)
            }
        }

    val multiplePermissionsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            for (row in RuntimePermissionRow.entries) {
                val granted = permissions[row.permission] ?: continue
                val previous = runtimeStates.getValue(row)
                val rationale = PermissionHelper.shouldShowRuntimePermissionRationale(context, row.permission)
                runtimeStates[row] = updatePermissionState(
                    isGranted = granted,
                    isEnabled = granted,
                    wasDenied = !granted && (previous.wasDenied || rationale),
                )
                if (!granted && previous.wasDenied && !rationale) {
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
                    val hasFilesPermission = fileRepository.hasPermission()
                    usagePermissionState = updatePermissionState(hasUsageAccess, hasUsageAccess)

                    if (hasFilesPermission) {
                        filesPermissionState = updatePermissionState(hasFilesPermission, true, wasDenied = false)
                    }

                    for (row in RuntimePermissionRow.entries) {
                        if (row.isGranted(context)) {
                            runtimeStates[row] = PermissionState.granted()
                        } else if (row.refreshRevocations) {
                            runtimeStates[row] = runtimeStates.getValue(row).copy(isGranted = false)
                        }
                    }

                    val notificationsGranted = hasNotificationsPermission(context)
                    notificationsPermissionState =
                        if (notificationsGranted) {
                            updatePermissionState(isGranted = true, isEnabled = true, wasDenied = false)
                        } else {
                            notificationsPermissionState.copy(isGranted = false)
                        }

                    val canScheduleExactAlarms = ReminderPermissions.canScheduleExactAlarms(context)
                    exactAlarmsPermissionState =
                        updatePermissionState(canScheduleExactAlarms, canScheduleExactAlarms)

                    val hasNotificationAccess =
                        NotificationDotsPermission.hasNotificationListenerAccess(context)
                    notificationAccessPermissionState =
                        updatePermissionState(hasNotificationAccess, hasNotificationAccess)

                    val hasAccessibilityPermission = LockScreenAccessibilityService.isEnabled(context)
                    accessibilityPermissionState = updatePermissionState(hasAccessibilityPermission, hasAccessibilityPermission)

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
            contacts = runtimeStates.getValue(RuntimePermissionRow.CONTACTS),
            files = filesPermissionState,
            calendar = if (showCalendarPermission) runtimeStates.getValue(RuntimePermissionRow.CALENDAR) else PermissionState.granted(),
            calling = if (showCallingPermission) runtimeStates.getValue(RuntimePermissionRow.CALLING) else PermissionState.granted(),
            notifications =
                if (showNotificationsPermission) notificationsPermissionState else PermissionState.granted(),
            notificationAccess = notificationAccessPermissionState,
            accessibility = accessibilityPermissionState,
        )
    LaunchedEffect(states) {
        onStatesChanged(states)
    }

    fun runtimeRows(position: RuntimePermissionRow.Position): List<PermissionCardItem> =
        RuntimePermissionRow.entries.filter { row ->
            row.position == position && row.isVisible(showCallingPermission, showCalendarPermission)
        }.map { row ->
            val state = runtimeStates.getValue(row)
            PermissionCardItem(
                title = row.title(context, texts),
                icon = row.icon(),
                description = row.description(context, texts),
                permissionState = state,
                isMandatory = false,
                onToggleChange = { enabled ->
                    val current = runtimeStates.getValue(row)
                    runtimeStates[row] = current.copy(isEnabled = enabled)
                    if (enabled && !current.isGranted) {
                        runCatching { multiplePermissionsLauncher.launch(arrayOf(row.permission)) }
                            .onFailure { PermissionHelper.launchAppSettingsRequest(context) }
                    }
                },
                onGrantedClick = { PermissionHelper.launchAppSettingsRequest(context) },
            )
        }

    val items =
        buildList {
            add(
                PermissionCardItem(
                    title = texts.usageTitle,
                    icon = MaterialSymbols.History,
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
                    onGrantedClick = { PermissionHelper.launchUsageAccessRequest(context) },
                ),
            )
            addAll(runtimeRows(RuntimePermissionRow.Position.AFTER_USAGE))
            add(
                PermissionCardItem(
                    title = texts.filesTitle,
                    icon = MaterialSymbols.Folder,
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
                    onGrantedClick = { PermissionHelper.launchFilesAccessSettings(context) },
                ),
            )
            addAll(runtimeRows(RuntimePermissionRow.Position.AFTER_FILES))
            if (showAccessibilityPermission) {
                add(
                    PermissionCardItem(
                        title = texts.accessibilityTitle,
                        icon = MaterialSymbols.AdminPanelSettings,
                        description = texts.accessibilityDescription,
                        permissionState = accessibilityPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            if (enabled && !accessibilityPermissionState.isGranted) {
                                if (shouldShowAccessibilityDisclosure) {
                                    showAccessibilityDisclosure = true
                                } else {
                                    LockScreenAccessibilityService.openAccessibilitySettings(context)
                                }
                            }
                        },
                        onGrantedClick = {
                            LockScreenAccessibilityService.openAccessibilitySettings(context)
                        },
                    ),
                )
            }
            addAll(runtimeRows(RuntimePermissionRow.Position.AFTER_ACCESSIBILITY))
            if (showNotificationsPermission) {
                add(
                    PermissionCardItem(
                        title = texts.notificationsTitle,
                        icon = MaterialSymbols.Notifications,
                        description = texts.notificationsDescription,
                        permissionState = notificationsPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            if (enabled && !notificationsPermissionState.isGranted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    runCatching {
                                        notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }.onFailure {
                                        PermissionHelper.launchAppSettingsRequest(context)
                                    }
                                    onRequestNotificationsPermission()
                                } else {
                                    notificationsPermissionState = PermissionState.granted()
                                }
                            }
                        },
                        onGrantedClick = {
                            if (!ReminderPermissions.openNotificationSettings(context)) {
                                PermissionHelper.launchAppSettingsRequest(context)
                            }
                        },
                    ),
                )
            }
            // "Alarms & reminders" only exists as a separate grant on Android 12+.
            if (showNotificationsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    PermissionCardItem(
                        title = texts.exactAlarmsTitle,
                        icon = MaterialSymbols.Alarm,
                        description = texts.exactAlarmsDescription,
                        permissionState = exactAlarmsPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            if (enabled && !exactAlarmsPermissionState.isGranted) {
                                ReminderPermissions.openExactAlarmSettings(context)
                            }
                        },
                        onGrantedClick = {
                            if (!ReminderPermissions.openExactAlarmSettings(context)) {
                                PermissionHelper.launchAppSettingsRequest(context)
                            }
                        },
                    ),
                )
            }
            if (showNotificationAccessPermission) {
                add(
                    PermissionCardItem(
                        title = texts.notificationAccessTitle,
                        icon = MaterialSymbols.NotificationsActive,
                        description = texts.notificationAccessDescription,
                        permissionState = notificationAccessPermissionState,
                        isMandatory = false,
                        onToggleChange = { enabled ->
                            if (enabled && !notificationAccessPermissionState.isGranted) {
                                NotificationDotsPermission.openNotificationListenerSettings(context)
                            }
                        },
                        onGrantedClick = { NotificationDotsPermission.openNotificationListenerSettings(context) },
                    ),
                )
            }
        }

    PermissionCard(
        items = items,
        modifier = modifier,
        cardContainer = cardContainer,
        itemContentPadding = itemContentPadding,
        internalScrollEnabled = internalScrollEnabled,
        segmented = segmented,
        showItemIcons = showItemIcons,
    )

    if (showAccessibilityDisclosure) {
        LockScreenAccessibilityDisclosureDialog(
            onAgree = {
                showAccessibilityDisclosure = false
                LockScreenAccessibilityService.openAccessibilitySettings(context)
            },
            onDismiss = { showAccessibilityDisclosure = false },
        )
    }
}

private fun hasNotificationsPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

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
