package com.tk.quicksearch.tiles

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
        val tileComponent = ComponentName(context, QuickSearchTileService::class.java)
        try {
            val icon = Icon.createWithResource(context, R.drawable.ic_widget_search)
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            statusBarManager?.requestAddTileService(
                tileComponent,
                context.getString(R.string.quick_settings_tile_label),
                icon,
                ContextCompat.getMainExecutor(context)
            ) { result ->
                val message = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                        context.getString(R.string.quick_settings_tile_added)
                    }
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                        context.getString(R.string.quick_settings_tile_already_added)
                    }
                    else -> {
                        context.getString(R.string.quick_settings_tile_not_added)
                    }
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(
                context,
                context.getString(R.string.quick_settings_tile_error),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.quick_settings_tile_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.quick_settings_tile_not_supported),
            Toast.LENGTH_LONG
        ).show()
    }
}

