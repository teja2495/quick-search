package com.tk.quicksearch.shared.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    private tailrec fun findActivity(context: Context): Activity? =
        when (context) {
            is Activity -> context
            is ContextWrapper -> context.baseContext?.let(::findActivity)
            else -> null
        }

    private fun createUsageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun createUsageAccessFallbackIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun createAllFilesAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    private fun createAllFilesAccessFallbackIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

    private fun createAppSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun createGeneralSettingsIntent(): Intent =
        Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun launchIfResolvable(
        context: Context,
        intent: Intent,
    ): Boolean {
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun checkFilesPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun checkCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED

    fun checkCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    fun checkWallpaperPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun requestWallpaperPermission(
        context: Context,
        requiresImagePermissionAfterSecurityError: Boolean,
        imagePermissionLauncher: ActivityResultLauncher<String>,
        legacyFilesPermissionLauncher: ActivityResultLauncher<String>,
        allFilesLauncher: ActivityResultLauncher<Intent>,
        onRequestingFilesPermission: (() -> Unit)? = null,
        onFilesPermissionAlreadyGranted: (() -> Unit)? = null,
    ) {
        if (
            requiresImagePermissionAfterSecurityError &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            imagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            return
        }

        if (!checkFilesPermission(context)) {
            onRequestingFilesPermission?.invoke()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                launchAllFilesAccessRequest(allFilesLauncher, context)
            } else {
                legacyFilesPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            return
        }

        onFilesPermissionAlreadyGranted?.invoke()
    }

    fun requestWallpaperGalleryPermission(
        imagePermissionLauncher: ActivityResultLauncher<String>,
        onUnsupportedVersion: (() -> Unit)? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            onUnsupportedVersion?.invoke()
        }
    }

    fun shouldOpenSettingsForFiles(
        context: Context,
        wasPreviouslyDenied: Boolean,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true
        }
        val activity = findActivity(context) ?: return false
        val permissionDenied = !checkFilesPermission(context)
        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )

        return wasPreviouslyDenied || (permissionDenied && !shouldShowRationale)
    }

    fun shouldOpenSettingsForRuntimePermission(
        context: Context,
        permission: String,
        wasPreviouslyDenied: Boolean,
    ): Boolean {
        val activity = findActivity(context) ?: return false
        val permissionDenied =
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        if (!permissionDenied) return false

        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

        return wasPreviouslyDenied && !shouldShowRationale
    }

    fun handleDeniedRuntimePermission(
        context: Context,
        permission: String,
        wasPreviouslyDenied: Boolean,
        onOpenSettings: (() -> Unit)? = null,
    ): Boolean {
        val shouldOpenSettings =
            shouldOpenSettingsForRuntimePermission(
                context = context,
                permission = permission,
                wasPreviouslyDenied = wasPreviouslyDenied,
            )
        if (!shouldOpenSettings) return false

        onOpenSettings?.invoke() ?: launchAppSettingsRequest(context)
        return true
    }

    fun requestRuntimePermissionOrOpenSettings(
        context: Context,
        permission: String,
        wasPreviouslyDenied: Boolean,
        runtimeLauncher: ActivityResultLauncher<Array<String>>,
        onOpenSettings: (() -> Unit)? = null,
    ) {
        val openedSettings =
            handleDeniedRuntimePermission(
                context = context,
                permission = permission,
                wasPreviouslyDenied = wasPreviouslyDenied,
                onOpenSettings = onOpenSettings,
            )
        if (!openedSettings) {
            val launcherSucceeded =
                runCatching {
                    runtimeLauncher.launch(arrayOf(permission))
                    true
                }.getOrDefault(false)
            if (!launcherSucceeded) {
                onOpenSettings?.invoke() ?: launchAppSettingsRequest(context)
            }
        }
    }

    fun requestFilesPermission(
        context: Context,
        wasPreviouslyDenied: Boolean,
        runtimeLauncher: ActivityResultLauncher<Array<String>>,
        allFilesLauncher: ActivityResultLauncher<Intent>,
        onOpenSettings: (() -> Unit)? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            launchAllFilesAccessRequest(allFilesLauncher, context)
            return
        }

        if (shouldOpenSettingsForFiles(context, wasPreviouslyDenied)) {
            onOpenSettings?.invoke() ?: launchAppSettingsRequest(context)
        } else {
            runtimeLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    fun launchAllFilesAccessRequest(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
    ) {
        val manageIntent = createAllFilesAccessIntent(context)
        runCatching {
            launcher.launch(manageIntent)
        }.onFailure {
            launcher.launch(createAllFilesAccessFallbackIntent())
        }
    }

    fun launchUsageAccessRequest(context: Context): Boolean =
        launchIfResolvable(context, createUsageAccessIntent(context)) ||
            launchIfResolvable(context, createUsageAccessFallbackIntent()) ||
            launchIfResolvable(context, createAppSettingsIntent(context)) ||
            launchIfResolvable(context, createGeneralSettingsIntent())

    fun launchAppSettingsRequest(context: Context): Boolean =
        launchIfResolvable(context, createAppSettingsIntent(context)) ||
            launchIfResolvable(context, createGeneralSettingsIntent())

    @Composable
    fun rememberPermissionRequestHandler(
        context: Context,
        permissionLauncher: ActivityResultLauncher<String>,
        permission: String,
        fallbackAction: () -> Unit,
    ): () -> Unit =
        remember(context, permissionLauncher, permission, fallbackAction) {
            {
                runCatching {
                    permissionLauncher.launch(permission)
                }.onFailure {
                    fallbackAction()
                }
            }
        }

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
        } else {
            handleDeniedRuntimePermission(
                context = context,
                permission = permission,
                wasPreviouslyDenied = true,
                onOpenSettings = onPermanentlyDenied,
            )
        }

        onComplete?.invoke()
    }
}
