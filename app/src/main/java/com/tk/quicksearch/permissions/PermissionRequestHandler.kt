package com.tk.quicksearch.permissions

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

/**
 * Handles permission requests for different permission types.
 */
object PermissionRequestHandler {
    
    /**
     * Creates an intent to open Usage Access settings.
     */
    fun createUsageAccessIntent(context: Context): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * Creates an intent to request all files access (Android R+).
     */
    fun createAllFilesAccessIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    
    /**
     * Fallback intent for all files access if the primary intent fails.
     */
    fun createAllFilesAccessFallbackIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    }
    
    /**
     * Creates an intent to open app settings (for denied permissions).
     */
    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * Checks if files permission is granted.
     */
    fun checkFilesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Checks if contacts permission is granted.
     */
    fun checkContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Checks if call phone permission is granted.
     */
    fun checkCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Determines if files permission request should open settings or request runtime permission.
     */
    fun shouldOpenSettingsForFiles(
        context: Context,
        wasPreviouslyDenied: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true // Always open settings for Android R+
        }
        
        val permissionDenied = !checkFilesPermission(context)
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context as android.app.Activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        // Open settings if permission was denied and we can't show rationale
        return wasPreviouslyDenied || (permissionDenied && !shouldShowRationale)
    }
    
    /**
     * Determines if contacts permission request should open settings.
     * Returns true if:
     * - Permission is already granted (no action needed, but caller should check this first)
     * - Context is not an Activity (can't request permission)
     * - Permission was permanently denied (can't show rationale after a denial)
     * 
     * This is used as a fallback check - the primary flow should always try the popup first.
     */
    fun shouldOpenSettingsForContacts(context: Context): Boolean {
        val permissionDenied = !checkContactsPermission(context)
        if (!permissionDenied) {
            return false // Permission already granted, no need to open settings
        }
        
        // If context is not an Activity, we can't request permission, so open settings
        if (context !is android.app.Activity) {
            return true
        }
        
        // Check if we can show rationale
        // shouldShowRequestPermissionRationale returns:
        // - true: User previously denied and we can show rationale (can try popup)
        // - false: Either first time OR permanently denied
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.READ_CONTACTS
        )
        
        // If we can show rationale, we can try popup (return false to indicate don't open settings yet)
        // If we can't show rationale AND permission is denied, it might be permanently denied
        // But we'll let the caller try popup first, and check again after if needed
        return false // Always try popup first, check again after if needed
    }
    
    /**
     * Launches all files access request with fallback handling.
     */
    fun launchAllFilesAccessRequest(
        launcher: ActivityResultLauncher<Intent>,
        context: Context
    ) {
        val manageIntent = createAllFilesAccessIntent(context)
        kotlin.runCatching {
            launcher.launch(manageIntent)
        }.onFailure {
            launcher.launch(createAllFilesAccessFallbackIntent())
        }
    }
}
