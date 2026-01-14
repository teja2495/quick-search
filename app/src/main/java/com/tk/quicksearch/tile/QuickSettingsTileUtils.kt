package com.tk.quicksearch.tile

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import android.app.StatusBarManager

/**
 * Shows the system dialog (Android 13+) to add the Quick Search Quick Settings tile.
 * Falls back to a toast for older versions where the dialog is not available.
 */
fun requestAddQuickSearchTile(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requestTileForAndroid13Plus(context)
    } else {
        showUnsupportedVersionToast(context)
    }
}

private fun requestTileForAndroid13Plus(context: Context) {
    val tileComponent = ComponentName(context, QuickSearchTileService::class.java)
    try {
        val icon = Icon.createWithResource(context, R.drawable.ic_widget_search)
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        statusBarManager?.requestAddTileService(
            tileComponent,
            context.getString(R.string.quick_settings_tile_label),
            icon,
            ContextCompat.getMainExecutor(context)
        ) { result -> handleTileAddResult(context, result) } ?: showErrorToast(context)
    } catch (e: Exception) {
        showErrorToast(context)
    }
}

private fun handleTileAddResult(context: Context, result: Int) {
    val messageResId = when (result) {
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.quick_settings_tile_added
        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.quick_settings_tile_already_added
        else -> R.string.quick_settings_tile_not_added
    }
    showToast(context, messageResId)
}

private fun showUnsupportedVersionToast(context: Context) {
    showToast(context, R.string.quick_settings_tile_not_supported, Toast.LENGTH_LONG)
}

private fun showErrorToast(context: Context) {
    showToast(context, R.string.quick_settings_tile_error)
}

private fun showToast(context: Context, messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, context.getString(messageResId), duration).show()
}

