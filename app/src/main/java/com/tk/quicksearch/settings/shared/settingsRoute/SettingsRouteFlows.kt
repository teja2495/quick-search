package com.tk.quicksearch.settings.shared

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.AppShortcutsSettings.AppActivityPickerDialog
import com.tk.quicksearch.settings.AppShortcutsSettings.AppActivitySource
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSourcePickerDialog
import com.tk.quicksearch.settings.AppShortcutsSettings.isAppActivitySource
import com.tk.quicksearch.settings.AppShortcutsSettings.queryAppActivitiesForPackage
import com.tk.quicksearch.shared.util.WallpaperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class WallpaperPermissionController(
    val hasWallpaperPermission: Boolean,
    val onRequestPermission: () -> Unit,
    val onRefreshPermissionState: () -> Unit,
    val showFallbackDialog: Boolean,
    val onDismissFallbackDialog: () -> Unit,
    val onConfirmFallbackDialog: () -> Unit,
    val onCancelFallbackDialog: () -> Unit,
)

@Composable
internal fun rememberWallpaperPermissionController(
    onSetWallpaperAvailable: (Boolean) -> Unit,
    onSetBackgroundSource: (BackgroundSource) -> Unit,
    onOptionalPermissionChanged: () -> Unit,
): WallpaperPermissionController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWallpaperFallbackDialog by remember { mutableStateOf(false) }
    var requiresImagePermissionAfterSecurityError by remember { mutableStateOf(false) }
    var wallpaperButtonHasPermission by
        remember { mutableStateOf(PermissionHelper.checkFilesPermission(context)) }

    suspend fun tryFetchWallpaperWithFilesPermission() {
        when (WallpaperUtils.getWallpaperBitmapResult(context)) {
            is WallpaperUtils.WallpaperLoadResult.Success -> {
                requiresImagePermissionAfterSecurityError = false
                wallpaperButtonHasPermission = true
                onSetWallpaperAvailable(true)
                onSetBackgroundSource(BackgroundSource.SYSTEM_WALLPAPER)
            }

            WallpaperUtils.WallpaperLoadResult.SecurityError -> {
                requiresImagePermissionAfterSecurityError = true
                wallpaperButtonHasPermission = false
                onSetWallpaperAvailable(false)
                showWallpaperFallbackDialog = true
            }

            WallpaperUtils.WallpaperLoadResult.PermissionRequired -> {
                wallpaperButtonHasPermission = false
                onSetWallpaperAvailable(false)
            }

            WallpaperUtils.WallpaperLoadResult.Unavailable -> {
                onSetWallpaperAvailable(false)
            }
        }
    }

    val wallpaperFilesAccessLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            val filesGranted = PermissionHelper.checkFilesPermission(context)
            wallpaperButtonHasPermission = filesGranted && !requiresImagePermissionAfterSecurityError
            if (filesGranted) {
                scope.launch { tryFetchWallpaperWithFilesPermission() }
            } else {
                onSetWallpaperAvailable(false)
            }
            onOptionalPermissionChanged()
        }

    val wallpaperPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                scope.launch { tryFetchWallpaperWithFilesPermission() }
            } else {
                wallpaperButtonHasPermission = false
                onSetWallpaperAvailable(false)
            }
            onOptionalPermissionChanged()
        }

    val legacyFilesPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                wallpaperButtonHasPermission = !requiresImagePermissionAfterSecurityError
                scope.launch { tryFetchWallpaperWithFilesPermission() }
            } else {
                wallpaperButtonHasPermission = false
                onSetWallpaperAvailable(false)
            }
            onOptionalPermissionChanged()
        }

    val onRequestWallpaperPermission: () -> Unit = {
        PermissionHelper.requestWallpaperPermission(
            context = context,
            requiresImagePermissionAfterSecurityError = requiresImagePermissionAfterSecurityError,
            imagePermissionLauncher = wallpaperPermissionLauncher,
            legacyFilesPermissionLauncher = legacyFilesPermissionLauncher,
            allFilesLauncher = wallpaperFilesAccessLauncher,
            onRequestingFilesPermission = {
                wallpaperButtonHasPermission = false
            },
            onFilesPermissionAlreadyGranted = {
                scope.launch { tryFetchWallpaperWithFilesPermission() }
            },
        )
    }

    return WallpaperPermissionController(
        hasWallpaperPermission = wallpaperButtonHasPermission,
        onRequestPermission = onRequestWallpaperPermission,
        onRefreshPermissionState = {
            wallpaperButtonHasPermission =
                PermissionHelper.checkFilesPermission(context) &&
                    !requiresImagePermissionAfterSecurityError
        },
        showFallbackDialog = showWallpaperFallbackDialog,
        onDismissFallbackDialog = {
            showWallpaperFallbackDialog = false
            wallpaperButtonHasPermission = false
        },
        onConfirmFallbackDialog = {
            showWallpaperFallbackDialog = false
            PermissionHelper.requestWallpaperGalleryPermission(
                imagePermissionLauncher = wallpaperPermissionLauncher,
                onUnsupportedVersion = {
                    scope.launch { tryFetchWallpaperWithFilesPermission() }
                },
            )
        },
        onCancelFallbackDialog = {
            showWallpaperFallbackDialog = false
            wallpaperButtonHasPermission = false
            requiresImagePermissionAfterSecurityError = true
            onSetWallpaperAvailable(false)
        },
    )
}

@Composable
internal fun WallpaperPermissionFallbackDialog(controller: WallpaperPermissionController) {
    if (!controller.showFallbackDialog) return
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = controller.onDismissFallbackDialog,
        title = {
            Text(text = context.getString(R.string.wallpaper_permission_fallback_title))
        },
        text = {
            Text(text = context.getString(R.string.wallpaper_permission_fallback_message))
        },
        confirmButton = {
            TextButton(onClick = controller.onConfirmFallbackDialog) {
                Text(text = context.getString(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = controller.onCancelFallbackDialog) {
                Text(text = context.getString(R.string.dialog_cancel))
            }
        },
    )
}

internal data class AppShortcutSourceFlowState(
    val showSourcePicker: Boolean,
    val appActivityDialogSource: AppShortcutSource?,
    val appActivityDialogItems: List<AppActivitySource>,
    val openSourcePicker: () -> Unit,
    val dismissSourcePicker: () -> Unit,
    val selectSource: (AppShortcutSource) -> Unit,
    val dismissActivityPicker: () -> Unit,
    val selectActivity: (AppActivitySource) -> Unit,
)

@Composable
internal fun rememberAppShortcutSourceFlow(
    context: Context,
    onAddShortcutFromPickerResult: (Intent?, String?) -> Unit,
    onAddCustomAppActivityShortcut: (AppShortcutSource, AppActivitySource) -> Unit,
    onSourceLaunchUnsupported: () -> Unit,
): AppShortcutSourceFlowState {
    val scope = rememberCoroutineScope()
    var showSourcePicker by remember { mutableStateOf(false) }
    var pendingShortcutSourcePackage by remember { mutableStateOf<String?>(null) }
    var appActivityDialogSource by remember { mutableStateOf<AppShortcutSource?>(null) }
    var appActivityDialogItems by remember { mutableStateOf<List<AppActivitySource>>(emptyList()) }
    var activityCache by remember { mutableStateOf<Map<String, List<AppActivitySource>>>(emptyMap()) }

    val addAppShortcutLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                onAddShortcutFromPickerResult(result.data, pendingShortcutSourcePackage)
            }
            pendingShortcutSourcePackage = null
        }

    return AppShortcutSourceFlowState(
        showSourcePicker = showSourcePicker,
        appActivityDialogSource = appActivityDialogSource,
        appActivityDialogItems = appActivityDialogItems,
        openSourcePicker = { showSourcePicker = true },
        dismissSourcePicker = { showSourcePicker = false },
        selectSource = { source ->
            if (isAppActivitySource(source)) {
                val cached = activityCache[source.packageName]
                if (cached != null) {
                    appActivityDialogSource = source
                    appActivityDialogItems = cached
                } else {
                    scope.launch {
                        val activities =
                            withContext(Dispatchers.IO) {
                                queryAppActivitiesForPackage(
                                    context.packageManager,
                                    source.packageName,
                                )
                            }
                        activityCache =
                            activityCache.toMutableMap().apply {
                                put(source.packageName, activities)
                            }
                        appActivityDialogSource = source
                        appActivityDialogItems = activities
                    }
                }
            } else {
                pendingShortcutSourcePackage = source.packageName
                runCatching { addAppShortcutLauncher.launch(source.launchIntent) }
                    .onFailure {
                        pendingShortcutSourcePackage = null
                        onSourceLaunchUnsupported()
                    }
            }
        },
        dismissActivityPicker = {
            appActivityDialogSource = null
            appActivityDialogItems = emptyList()
        },
        selectActivity = { activity ->
            val source = appActivityDialogSource
            if (source != null) {
                appActivityDialogSource = null
                appActivityDialogItems = emptyList()
                onAddCustomAppActivityShortcut(source, activity)
            }
        },
    )
}

@Composable
internal fun AppShortcutSourceFlowDialogs(
    flowState: AppShortcutSourceFlowState,
    sources: List<AppShortcutSource>,
) {
    if (flowState.showSourcePicker) {
        AppShortcutSourcePickerDialog(
            sources = sources,
            onDismiss = flowState.dismissSourcePicker,
            onSourceSelected = { source ->
                flowState.dismissSourcePicker()
                flowState.selectSource(source)
            },
        )
    }

    flowState.appActivityDialogSource?.let {
        AppActivityPickerDialog(
            activities = flowState.appActivityDialogItems,
            onDismiss = flowState.dismissActivityPicker,
            onActivitySelected = flowState.selectActivity,
        )
    }
}
