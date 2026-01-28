package com.tk.quicksearch.search.overlay

import android.content.Context
import android.content.Intent
import com.tk.quicksearch.app.MainActivity

object OverlayModeController {
    const val EXTRA_FORCE_NORMAL_LAUNCH = "overlay_force_normal_launch"
    const val EXTRA_OPEN_SETTINGS = "overlay_open_settings"
    const val EXTRA_FROM_OVERLAY = "extra_from_overlay"

    fun startOverlay(context: Context) {
        val intent = Intent(context, OverlayService::class.java)
        context.startService(intent)
    }

    fun stopOverlay(context: Context) {
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }

    fun openMainActivity(context: Context, openSettings: Boolean = false) {
        val intent =
                Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_FORCE_NORMAL_LAUNCH, true)
                    putExtra(EXTRA_OPEN_SETTINGS, openSettings)
                    putExtra(EXTRA_FROM_OVERLAY, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
        context.startActivity(intent)
    }
}
