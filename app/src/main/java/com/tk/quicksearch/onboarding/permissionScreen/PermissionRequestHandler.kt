package com.tk.quicksearch.onboarding.permissionScreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionRequestHandler {
    fun createUsageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun createAllFilesAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun createAllFilesAccessFallbackIntent(): Intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

    fun createAppSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun checkFilesPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    fun checkCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun checkWallpaperPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older Android versions
        }

    fun shouldOpenSettingsForFiles(
        context: Context,
        wasPreviouslyDenied: Boolean,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true // Always open settings for Android R+
        }

        val permissionDenied = !checkFilesPermission(context)
        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as android.app.Activity,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )

        // Open settings if permission was denied and we can't show rationale
        return wasPreviouslyDenied || (permissionDenied && !shouldShowRationale)
    }

    fun launchAllFilesAccessRequest(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
    ) {
        val manageIntent = createAllFilesAccessIntent(context)
        kotlin
            .runCatching {
                launcher.launch(manageIntent)
            }.onFailure {
                launcher.launch(createAllFilesAccessFallbackIntent())
            }
    }

    fun shouldRequestWallpaperPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !checkWallpaperPermission(context)
}
