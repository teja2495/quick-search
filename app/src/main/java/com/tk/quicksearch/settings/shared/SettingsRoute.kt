package com.tk.quicksearch.settings.shared

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shortcut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.permissions.PermissionSettingsDialog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget
import com.tk.quicksearch.settings.AppShortcutsSettings.*
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.SectionSettingsSection
import com.tk.quicksearch.settings.shared.createPermissionRequestHandler
import com.tk.quicksearch.settings.shared.handlePermissionResult
import com.tk.quicksearch.settings.shared.rememberSectionToggleHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SETTINGS_BACK_SWIPE_THRESHOLD_PX = 140f

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onNavigateToDetail: (com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType) -> Unit =
        {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userPreferences = remember { UserAppPreferences(context) }

    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)

    val state = uiState.toSettingsScreenState()

    val shouldShowBanner = remember { mutableStateOf(uiState.shouldShowUsagePermissionBanner) }
    val hasSeenSettingsSearchTip = remember {
        mutableStateOf(userPreferences.hasSeenSettingsSearchTip())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val pendingEnableDirectDial = remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var pendingPermissionSettingsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPermissionSettingsType by remember { mutableStateOf<String?>(null) }
    val requestSettingsPermissionConfirmation: (String, () -> Unit) -> Unit = { permissionType, action ->
        pendingPermissionSettingsType = permissionType
        pendingPermissionSettingsAction = action
        showPermissionSettingsDialog = true
    }
    val wallpaperPermissionController =
        rememberWallpaperPermissionController(
            onSetWallpaperAvailable = viewModel::setWallpaperAvailable,
            onSetBackgroundSource = viewModel::setBackgroundSource,
            onOptionalPermissionChanged = viewModel::handleOptionalPermissionChange,
        )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshIconPacks()
        viewModel.handleOptionalPermissionChange()
    }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            handlePermissionResult(
                isGranted = isGranted,
                context = context,
                permission = Manifest.permission.READ_CONTACTS,
                onPermanentlyDenied = {
                    requestSettingsPermissionConfirmation(
                        context.getString(R.string.contacts_action_button_contacts),
                        viewModel::openContactPermissionSettings,
                    )
                },
                onPermissionChanged = viewModel::handleOptionalPermissionChange,
            )
        }

    val onRequestContactPermission =
        createPermissionRequestHandler(
            context = context,
            permissionLauncher = contactsPermissionLauncher,
            permission = Manifest.permission.READ_CONTACTS,
            fallbackAction = {
                requestSettingsPermissionConfirmation(
                    context.getString(R.string.contacts_action_button_contacts),
                    viewModel::openContactPermissionSettings,
                )
            },
        )

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            handlePermissionResult(
                isGranted = isGranted,
                context = context,
                permission = Manifest.permission.CALL_PHONE,
                onPermanentlyDenied = {
                    requestSettingsPermissionConfirmation(
                        context.getString(R.string.settings_call_permission_title),
                        viewModel::openAppSettings,
                    )
                },
                onPermissionChanged = viewModel::handleOptionalPermissionChange,
                onGranted = {
                    if (pendingEnableDirectDial.value) {
                        viewModel.setDirectDialEnabled(true)
                    }
                },
                onComplete = { pendingEnableDirectDial.value = false },
            )
        }

    val onRequestCallPermission =
        createPermissionRequestHandler(
            context = context,
            permissionLauncher = callPermissionLauncher,
            permission = Manifest.permission.CALL_PHONE,
            fallbackAction = {
                requestSettingsPermissionConfirmation(
                    context.getString(R.string.settings_call_permission_title),
                    viewModel::openAppSettings,
                )
            },
        )

    val onRequestWallpaperPermission: () -> Unit = wallpaperPermissionController.onRequestPermission

    val onToggleDirectDial: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (PermissionHelper.checkCallPermission(context)) {
                viewModel.setDirectDialEnabled(true)
            } else {
                pendingEnableDirectDial.value = true
                onRequestCallPermission()
            }
        } else {
            pendingEnableDirectDial.value = false
            viewModel.setDirectDialEnabled(false)
        }
    }

    val onRequestAddHomeScreenWidget = { requestAddQuickSearchWidget(context) }
    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }

    val onToggleOverlayMode: (Boolean) -> Unit = { enabled ->
        viewModel.setOverlayModeEnabled(enabled)
        if (enabled) {
            com.tk.quicksearch.overlay.OverlayModeController.startOverlay(context)
            (context as? android.app.Activity)?.finish()
        }
    }
    val appShortcutSourceFlow =
        rememberAppShortcutSourceFlow(
            context = context,
            onAddShortcutFromPickerResult = { resultData, sourcePackageName ->
                viewModel.addCustomAppShortcutFromPickerResult(
                    resultData = resultData,
                    sourcePackageName = sourcePackageName,
                )
            },
            onAddCustomAppActivityShortcut = { source, activity ->
                viewModel.addCustomAppActivityShortcut(
                    packageName = source.packageName,
                    activityClassName = activity.className,
                    activityLabel = activity.label,
                )
            },
            onSourceLaunchUnsupported = {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.settings_app_shortcuts_create_not_supported),
                        Toast.LENGTH_SHORT,
                    ).show()
            },
        )
    var filteredAppShortcutSources by remember { mutableStateOf<List<AppShortcutSource>>(emptyList()) }

    androidx.compose.runtime.LaunchedEffect(
        appShortcutSourceFlow.showSourcePicker,
        state.allApps,
        state.allAppShortcuts,
        context.packageName,
    ) {
        if (!appShortcutSourceFlow.showSourcePicker) return@LaunchedEffect

        val appShortcutSources =
            withContext(Dispatchers.IO) {
                queryAppShortcutSources(
                    packageManager = context.packageManager,
                    repositoryApps = state.allApps,
                )
            }
        filteredAppShortcutSources =
            withContext(Dispatchers.Default) {
                filterAppShortcutSources(
                    sources = appShortcutSources,
                    existingShortcuts = state.allAppShortcuts,
                )
            }
    }

    // Define permission request handlers
    val onRequestUsagePermission = {
        requestSettingsPermissionConfirmation(
            context.getString(R.string.settings_shortcut_usage_access),
            viewModel::openUsageAccessSettings,
        )
    }
    val onRequestFilePermission = {
        requestSettingsPermissionConfirmation(
            context.getString(R.string.section_files),
            viewModel::openFilesPermissionSettings,
        )
    }
    val onRequestCalendarPermission = {
        requestSettingsPermissionConfirmation(
            context.getString(R.string.settings_calendar_permission_title),
            viewModel::openCalendarPermissionSettings,
        )
    }

    val callbacks =
        buildSettingsScreenCallbacks(
            viewModel = viewModel,
            handlers =
                SettingsRouteHandlers(
                    onBack = onBack,
                    onToggleOverlayMode = onToggleOverlayMode,
                    onPickCustomImage = {},
                    onToggleDirectDial = onToggleDirectDial,
                    onToggleSection = onToggleSection,
                    onOpenAiSearchConfigure = {
                        onNavigateToDetail(com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType.GEMINI_API_CONFIG)
                    },
                    onOpenAddAppShortcutDialog = appShortcutSourceFlow.openSourcePicker,
                    onAddAppShortcutFromSource = appShortcutSourceFlow.selectSource,
                    onAddAppDeepLinkShortcut = { packageName, shortcutName, deepLink, iconBase64 ->
                        viewModel.addCustomAppDeepLinkShortcut(
                            packageName = packageName,
                            shortcutName = shortcutName,
                            deepLink = deepLink,
                            iconBase64 = iconBase64,
                        )
                    },
                    onAddSearchTargetQueryShortcut = { target, shortcutName, shortcutQuery, mode ->
                        viewModel.addSearchTargetQueryShortcut(
                            target = target,
                            shortcutName = shortcutName,
                            shortcutQuery = shortcutQuery,
                            mode = mode,
                        )
                    },
                    onAddHomeScreenWidget = onRequestAddHomeScreenWidget,
                    onAddQuickSettingsTile = onRequestAddQuickSettingsTile,
                    onSetDefaultAssistant = {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.settings_unable_to_open_settings),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    },
                    onToggleAssistantLaunchVoiceMode = { enabled ->
                        userPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
                    },
                    onRefreshApps = { showToast ->
                        if (SearchSection.APPS in uiState.disabledSections) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.settings_refresh_apps_disabled),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        } else {
                            viewModel.refreshApps(showToast)
                        }
                    },
                    onRefreshContacts = { showToast ->
                        if (SearchSection.CONTACTS in uiState.disabledSections) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.settings_refresh_contacts_disabled),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        } else {
                            viewModel.refreshContacts(showToast)
                        }
                    },
                    onRefreshFiles = { showToast ->
                        if (SearchSection.FILES in uiState.disabledSections) {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.settings_refresh_files_disabled),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        } else {
                            viewModel.refreshFiles(showToast)
                        }
                    },
                    onRequestUsagePermission = onRequestUsagePermission,
                    onRequestContactPermission = onRequestContactPermission,
                    onRequestFilePermission = onRequestFilePermission,
                    onRequestCalendarPermission = onRequestCalendarPermission,
                    onRequestCallPermission = onRequestCallPermission,
                    onRequestWallpaperPermission = onRequestWallpaperPermission,
                ),
        )

    // Refresh permission state and reset banner session dismissed flag when activity starts/resumes
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        viewModel.resetUsagePermissionBannerSessionDismissed()
                        shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        viewModel.handleOnResume()
                        wallpaperPermissionController.onRefreshPermissionState()
                        shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onDismissBanner = {
        viewModel.incrementUsagePermissionBannerDismissCount()
        viewModel.setUsagePermissionBannerSessionDismissed(true)
        shouldShowBanner.value = viewModel.uiState.value.shouldShowUsagePermissionBanner
    }
    val onDismissSettingsSearchTip = {
        userPreferences.setHasSeenSettingsSearchTip(true)
        hasSeenSettingsSearchTip.value = true
    }

    val resolvedState = state.copy(hasWallpaperPermission = wallpaperPermissionController.hasWallpaperPermission)
    val swipeNavigationModifier =
        Modifier.pointerInput(onBack) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    totalHorizontalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalHorizontalDrag >= SETTINGS_BACK_SWIPE_THRESHOLD_PX) {
                        onBack()
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
            )
        }

    com.tk.quicksearch.settings.settingsScreen.SettingsScreen(
        modifier = modifier.then(swipeNavigationModifier),
        state = resolvedState,
        callbacks = callbacks,
        hasUsagePermission = uiState.hasUsagePermission,
        hasContactPermission = uiState.hasContactPermission,
        hasFilePermission = uiState.hasFilePermission,
        hasCallPermission = uiState.hasCallPermission,
        shouldShowBanner = shouldShowBanner.value,
        onRequestUsagePermission = onRequestUsagePermission,
        onRequestContactPermission = onRequestContactPermission,
        onRequestFilePermission = onRequestFilePermission,
        onRequestCalendarPermission = onRequestCalendarPermission,
        onRequestCallPermission = onRequestCallPermission,
        onDismissBanner = onDismissBanner,
        shouldShowSettingsSearchTip = !hasSeenSettingsSearchTip.value,
        onDismissSettingsSearchTip = onDismissSettingsSearchTip,
        onNavigateToDetail = onNavigateToDetail,
        onSettingsImported = viewModel::onSettingsImported,
        scrollState = scrollState,
    )

    WallpaperPermissionFallbackDialog(controller = wallpaperPermissionController)
    AppShortcutSourceFlowDialogs(
        flowState = appShortcutSourceFlow,
        sources = filteredAppShortcutSources,
    )
    if (showPermissionSettingsDialog) {
        PermissionSettingsDialog(
            permissionType = pendingPermissionSettingsType ?: context.getString(R.string.settings_permissions_title),
            onConfirm = {
                showPermissionSettingsDialog = false
                pendingPermissionSettingsAction?.invoke()
                pendingPermissionSettingsAction = null
                pendingPermissionSettingsType = null
            },
            onDismiss = {
                showPermissionSettingsDialog = false
                pendingPermissionSettingsAction = null
                pendingPermissionSettingsType = null
            },
        )
    }
}
