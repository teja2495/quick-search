package com.tk.quicksearch.util

import android.content.Context
import android.content.res.Configuration
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
 * Tablets show 7 columns in portrait, 9 columns in landscape. Phones show 5 columns.
 */
@Composable
fun getAppGridColumns(): Int {
    return if (isTablet()) {
        if (isLandscape()) 9 else 7
    } else {
        5
    }
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
 * Tablets show 7 columns in portrait, 9 columns in landscape. Phones show 5 columns.
 */
fun getAppGridColumns(context: Context): Int {
    return if (isTablet(context)) {
        if (isLandscape(context)) 9 else 7
    } else {
        5
    }
}