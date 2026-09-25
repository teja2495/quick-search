package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.Toast
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpOverlay
import com.tk.quicksearch.shared.permissions.PermissionSettingsDialog
import com.tk.quicksearch.settings.settingsDetailScreen.DefaultCalendarDialog
import com.tk.quicksearch.settings.settingsDetailScreen.SecondaryRankingDialog
import com.tk.quicksearch.settings.AppearanceSettings.IconPackPickerDialog

internal fun launchSystemWallpaperPicker(context: Context) {
    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(
                R.string.common_error_unable_to_open,
                context.getString(R.string.action_change_wallpaper),
            ),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

@Composable
internal fun SearchRouteOverlays(
    viewModel: SearchViewModel,
    uiState: SearchUiState,
    showPermissionSettingsDialog: Boolean,
    pendingPermissionSettingsType: Int?,
    onPermissionConfirm: () -> Unit,
    onPermissionDismiss: () -> Unit,
    showSecondaryRankingDialog: Boolean,
    onSecondaryRankingDismiss: () -> Unit,
    showIconPackDialog: Boolean,
    onIconPackDismiss: () -> Unit,
    showDefaultCalendarDialog: Boolean,
    defaultCalendarPackage: String?,
    onCalendarSelected: (String?) -> Unit,
    onDefaultCalendarDismiss: () -> Unit,
    speedBumpApp: AppInfo?,
    onSpeedBumpOpen: (AppInfo) -> Unit,
    onSpeedBumpCancel: () -> Unit,
    previewFile: DeviceFile?,
    onPreviewDismiss: () -> Unit,
    onPreviewOpen: (DeviceFile) -> Unit,
    onPreviewShare: (DeviceFile) -> Unit,
) {
        if (showPermissionSettingsDialog) {
            PermissionSettingsDialog(
                permissionType = stringResource(pendingPermissionSettingsType ?: R.string.settings_permissions_title),
                onConfirm = onPermissionConfirm,
                onDismiss = onPermissionDismiss,
            )
        }

        if (showSecondaryRankingDialog) {
            SecondaryRankingDialog(
                selectedSignal = uiState.secondaryRankingSignal,
                onSignalSelected = { signal ->
                    viewModel.setSecondaryRankingSignal(signal)
                    viewModel.onQueryChange(uiState.query)
                },
                onDismiss = onSecondaryRankingDismiss,
            )
        }

        if (showIconPackDialog) {
            IconPackPickerDialog(
                availableIconPacks = uiState.availableIconPacks,
                selectedPackage = uiState.selectedIconPackPackage,
                maskUnsupportedIcons = uiState.maskUnsupportedIconPackIcons,
                onSelect = { packageName ->
                    viewModel.setIconPackPackage(packageName)
                    onIconPackDismiss()
                },
                onMaskUnsupportedIconsChange = viewModel::setIconPackUnsupportedIconMaskEnabled,
                onDownloadIconPacks = viewModel::searchIconPacks,
                onResetAllIcons = viewModel::resetAllAppIconsToDefault,
                onDismiss = { onIconPackDismiss() },
            )
        }

        if (showDefaultCalendarDialog) {
            DefaultCalendarDialog(
                selectedPackageName = defaultCalendarPackage,
                onCalendarSelected = { packageName ->
                    onCalendarSelected(packageName)
                },
                onDismiss = onDefaultCalendarDismiss,
            )
        }

        speedBumpApp?.let { app ->
            SpeedBumpOverlay(
                appInfo = app,
                iconPackPackage = uiState.selectedIconPackPackage,
                appIconShape = uiState.appIconShape,
                onOpen = {
                    onSpeedBumpOpen(app)
                },
                onCancel = onSpeedBumpCancel,
            )
        }

        com.tk.quicksearch.search.apps.swipeGestures.AppSwipeGesturePickerHost(
            searchState = uiState,
            onQueryChange = viewModel::onQueryChange,
        )

        previewFile?.let { file ->
            com.tk.quicksearch.search.files.FilePreviewBottomSheet(
                deviceFile = file,
                onDismiss = onPreviewDismiss,
                onOpen = {
                    onPreviewOpen(file)
                },
                onShare = {
                    onPreviewShare(file)
                },
            )
        }
}
