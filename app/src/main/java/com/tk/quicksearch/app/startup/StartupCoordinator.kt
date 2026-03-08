package com.tk.quicksearch.app.startup

import android.app.Activity
import android.content.Context
import android.os.Trace
import android.view.Window
import androidx.lifecycle.LifecycleCoroutineScope
import com.tk.quicksearch.app.UpdateHelper
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.shared.util.WallpaperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StartupMode {
    MAIN,
    OVERLAY,
}

class StartupCoordinator(
    private val context: Context,
    private val activity: Activity? = null,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val viewModel: SearchViewModel,
    private val userPreferences: UserAppPreferences,
    private val mode: StartupMode,
    private val onReviewPromptEligible: (() -> Unit)? = null,
) {
    companion object {
        private const val TRACE_FIRST_FRAME_GATE = "QS.Startup.FirstFrameGate"
        private const val TRACE_PERMISSION_SYNC = "QS.Startup.PermissionSync"
        private const val TRACE_WALLPAPER_PRELOAD = "QS.Startup.WallpaperPreload"
        private const val TRACE_NON_CRITICAL = "QS.Startup.NonCritical"
        private const val POST_FIRST_FRAME_PERMISSION_SYNC_DELAY_MS = 250L
        private const val POST_FIRST_FRAME_WALLPAPER_PRELOAD_DELAY_MS = 500L
        private const val NON_CRITICAL_STARTUP_DELAY_MS = 2_500L
    }

    fun scheduleAfterFirstFrame(window: Window) {
        window.decorView.post {
            Trace.beginSection(TRACE_FIRST_FRAME_GATE)
            try {
                viewModel.startStartupPhasesAfterFirstFrame()
                when (mode) {
                    StartupMode.MAIN -> scheduleMainPostFrameWork()
                    StartupMode.OVERLAY -> scheduleOverlayPostFrameWork()
                }
            } finally {
                Trace.endSection()
            }
        }
    }

    private fun scheduleMainPostFrameWork() {
        if (userPreferences.isFirstLaunch()) return

        lifecycleScope.launch {
            delay(POST_FIRST_FRAME_PERMISSION_SYNC_DELAY_MS)
            Trace.beginSection(TRACE_PERMISSION_SYNC)
            try {
                viewModel.refreshPermissionSnapshotAtLaunch()
            } finally {
                Trace.endSection()
            }
        }

        scheduleWallpaperPreload()

        lifecycleScope.launch {
            delay(NON_CRITICAL_STARTUP_DELAY_MS)
            Trace.beginSection(TRACE_NON_CRITICAL)
            try {
                userPreferences.recordFirstAppOpenTime()
                userPreferences.incrementAppOpenCount()
                userPreferences.resetUpdateCheckSession()

                val targetActivity = activity ?: return@launch
                UpdateHelper.checkForUpdates(targetActivity, userPreferences)

                if (!userPreferences.hasShownUpdateCheckThisSession() &&
                    userPreferences.shouldShowReviewPrompt()
                ) {
                    onReviewPromptEligible?.invoke()
                }
            } finally {
                Trace.endSection()
            }
        }
    }

    private fun scheduleOverlayPostFrameWork() {
        scheduleWallpaperPreload()
    }

    private fun scheduleWallpaperPreload() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(POST_FIRST_FRAME_WALLPAPER_PRELOAD_DELAY_MS)
            if (userPreferences.getBackgroundSource() == BackgroundSource.SYSTEM_WALLPAPER) {
                Trace.beginSection(TRACE_WALLPAPER_PRELOAD)
                try {
                    WallpaperUtils.preloadWallpaper(context)
                } finally {
                    Trace.endSection()
                }
            }
        }
    }
}
