package com.tk.quicksearch.shared.util

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Utility functions for device detection and configuration.
 */

/**
 * Checks if the current device is a tablet based on screen size.
 * Tablets are typically devices with smallest screen width >= 600dp.
 */
@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}

/**
 * Checks if the device is in landscape orientation.
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Gets the appropriate number of columns for app grid based on device type and orientation.
 * Tablets show 7 columns in portrait, 9 columns in landscape. Phones use [phoneColumnOverride].
 */
@Composable
fun getAppGridColumns(phoneColumnOverride: Int = 5): Int =
    if (isTablet()) {
        if (isLandscape()) 9 else 7
    } else {
        phoneColumnOverride
    }

/**
 * Checks if the device is a tablet using Context.
 * Useful when not in a Composable context.
 */
fun isTablet(context: Context): Boolean {
    val configuration = context.resources.configuration
    return configuration.smallestScreenWidthDp >= 600
}

/**
 * Checks if the device is in landscape orientation using Context.
 * Useful when not in a Composable context.
 */
fun isLandscape(context: Context): Boolean {
    val configuration = context.resources.configuration
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Gets the appropriate number of columns for app grid based on device type and orientation using Context.
 * Tablets show 7 columns in portrait, 9 columns in landscape. Phones use [phoneColumnOverride].
 */
fun getAppGridColumns(context: Context, phoneColumnOverride: Int = 5): Int =
    if (isTablet(context)) {
        if (isLandscape(context)) 9 else 7
    } else {
        phoneColumnOverride
    }

/**
 * Checks whether the device is classified by Android as low-RAM.
 * Useful for reducing expensive search workloads on constrained devices.
 */
fun isLowRamDevice(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice == true
}

/**
 * Returns whether system-level cross-window blur is currently enabled.
 * Available on Android 12+ only; returns false on older versions.
 */
fun isCrossWindowBlurEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val windowManager = context.getSystemService(WindowManager::class.java)
    return windowManager?.isCrossWindowBlurEnabled == true
}

/**
 * Returns whether the device platform can support cross-window blur APIs.
 * This does not reflect runtime enablement (battery saver / system toggles).
 */
fun supportsCrossWindowBlur(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
