package com.tk.quicksearch.search.overlay

import android.content.Context
import android.content.Intent
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.widgets.searchWidget.MicAction

object OverlayModeController {
    const val EXTRA_FORCE_NORMAL_LAUNCH = "overlay_force_normal_launch"
    const val EXTRA_OPEN_SETTINGS = "overlay_open_settings"
    const val EXTRA_OPEN_SETTINGS_DETAIL = "overlay_open_settings_detail"
    const val EXTRA_CLOSE_OVERLAY = "overlay_close"
    const val EXTRA_START_VOICE_SEARCH = "overlay_start_voice_search"
    const val EXTRA_MIC_ACTION = "overlay_mic_action"
    const val EXTRA_ANIMATION_TOKEN = "overlay_animation_token"

    fun startOverlay(
        context: Context,
        startVoiceSearch: Boolean = false,
        micAction: MicAction = MicAction.DEFAULT_VOICE_SEARCH,
    ) {
        val intent =
            Intent(context, OverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_START_VOICE_SEARCH, startVoiceSearch)
                putExtra(EXTRA_MIC_ACTION, micAction.value)
                putExtra(EXTRA_ANIMATION_TOKEN, System.currentTimeMillis())
            }
        context.startActivity(intent)
    }

    fun stopOverlay(context: Context) {
        val intent =
            Intent(context, OverlayActivity::class.java).apply {
                putExtra(EXTRA_CLOSE_OVERLAY, true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        context.startActivity(intent)
    }

    fun openMainActivity(
        context: Context,
        openSettings: Boolean = false,
        settingsDetailType: SettingsDetailType? = null,
    ) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FORCE_NORMAL_LAUNCH, true)
                putExtra(EXTRA_OPEN_SETTINGS, openSettings)
                settingsDetailType?.let { putExtra(EXTRA_OPEN_SETTINGS_DETAIL, it.name) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        context.startActivity(intent)
    }
}
