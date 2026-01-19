package com.tk.quicksearch.settings.shared

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat

/** Creates a standardized permission request handler that tries popup first, then settings. */
@Composable
fun createPermissionRequestHandler(
    context: Context,
    permissionLauncher: ActivityResultLauncher<String>,
    permission: String,
    fallbackAction: () -> Unit
): () -> Unit =
    androidx.compose.runtime.remember(context, permissionLauncher, permission, fallbackAction) {
        {
            if (context !is Activity) {
                fallbackAction()
            } else {
                permissionLauncher.launch(permission)
            }
        }
    }

/** Handles the result of a permission request with standardized logic. */
fun handlePermissionResult(
    isGranted: Boolean,
    context: Context,
    permission: String,
    onPermanentlyDenied: () -> Unit,
    onPermissionChanged: () -> Unit,
    onGranted: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null
) {
    onPermissionChanged()

    if (isGranted) {
        onGranted?.invoke()
    } else if (context is Activity) {
        val shouldShowRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
        if (!shouldShowRationale) {
            // Permission permanently denied, open settings
            onPermanentlyDenied()
        }
    }

    onComplete?.invoke()
}