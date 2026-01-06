package com.tk.quicksearch.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tk.quicksearch.MainActivity

import com.tk.quicksearch.R

/**
 * Quick Settings tile that launches the Quick Search home screen.
 */
class QuickSearchTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        // Use a PendingIntent so the system can grant background launch exemption.
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            startActivityAndCollapse(pendingIntent)
        } catch (_: Exception) {
            // Fallback if the platform disallows PendingIntent variant
            try {
                startActivityAndCollapse(launchIntent)
            } catch (_: Exception) {
                startActivity(launchIntent)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(Tile.STATE_ACTIVE)
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState(Tile.STATE_ACTIVE)
    }

    private fun updateTileState(state: Int) {
        qsTile?.let { tile ->
            tile.label = getString(R.string.quick_settings_tile_label)
            tile.state = state
            tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_search)
            tile.updateTile()
        }
    }
}
