package com.tk.quicksearch.app.startup

import android.app.Activity
import android.content.Context
import android.os.Trace
import android.view.Window
import android.view.ViewTreeObserver
import androidx.lifecycle.LifecycleCoroutineScope
import com.tk.quicksearch.app.UpdateHelper
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.BootstrapPreferences
import com.tk.quicksearch.searchEngines.AmazonRegionDetector
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.MemoryDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

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
    private val isFirstLaunch: Boolean = false,
    private val mode: StartupMode,
) {
    companion object {
        private const val TRACE_FIRST_FRAME_GATE = "QS.Startup.FirstFrameGate"
        private const val TRACE_FIRST_DRAW = "QS.Startup.FirstDraw"
        private const val TRACE_PERMISSION_SYNC = "QS.Startup.PermissionSync"
        private const val TRACE_BACKGROUND_PRELOAD = "QS.Startup.BackgroundPreload"
        private const val TRACE_NON_CRITICAL = "QS.Startup.NonCritical"
        private const val POST_FIRST_FRAME_PERMISSION_SYNC_DELAY_MS = 250L
        private const val UPDATE_CHECK_DELAY_MS = 3_000L
        private const val NON_CRITICAL_STARTUP_DELAY_MS = 15_000L
    }

    private val hasReleasedPostDrawWork = AtomicBoolean(false)

    /** Releases startup work only after this window has completed an actual draw. */
    fun scheduleAfterFirstDraw(window: Window) {
        val decorView = window.decorView
        val observer = decorView.viewTreeObserver
        if (!observer.isAlive) return

        observer.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    if (!hasReleasedPostDrawWork.compareAndSet(false, true)) return

                    Trace.beginSection(TRACE_FIRST_DRAW)
                    Trace.endSection()
                    decorView.post {
                        if (decorView.viewTreeObserver.isAlive) {
                            decorView.viewTreeObserver.removeOnDrawListener(this)
                        }
                        releasePostDrawWork()
                    }
                }
            },
        )
    }

    private fun releasePostDrawWork() {
        Trace.beginSection(TRACE_FIRST_FRAME_GATE)
        try {
                viewModel.startStartupPhasesAfterFirstFrame()
                scheduleDeferredDiagnostics()
            when (mode) {
                StartupMode.MAIN -> scheduleMainPostFrameWork()
                StartupMode.OVERLAY -> scheduleOverlayPostFrameWork()
            }
        } finally {
            Trace.endSection()
        }
    }

    private fun scheduleMainPostFrameWork() {
        scheduleAmazonRegionDetection()
        if (isFirstLaunch) return

        lifecycleScope.launch(Dispatchers.IO) {
            val reconciledFirstLaunch = userPreferences.isFirstLaunch()
            BootstrapPreferences.setFirstLaunch(context, reconciledFirstLaunch)
            if (reconciledFirstLaunch != isFirstLaunch) {
                activity?.runOnUiThread { activity.recreate() }
            }
        }

        lifecycleScope.launch {
            delay(POST_FIRST_FRAME_PERMISSION_SYNC_DELAY_MS)
            Trace.beginSection(TRACE_PERMISSION_SYNC)
            try {
                viewModel.refreshPermissionSnapshotAtLaunch()
            } finally {
                Trace.endSection()
            }
        }

        scheduleBackgroundPreload()
        scheduleUpdateCheck()

        lifecycleScope.launch {
            delay(NON_CRITICAL_STARTUP_DELAY_MS)
            Trace.beginSection(TRACE_NON_CRITICAL)
            try {
                userPreferences.recordFirstAppOpenTime()
                userPreferences.incrementAppOpenCount()
            } finally {
                Trace.endSection()
            }
        }
    }

    /**
     * Once per install (new and existing users), default the Amazon engine to the user's regional
     * storefront if they are still on the untouched amazon.com default.
     */
    private fun scheduleAmazonRegionDetection() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (userPreferences.hasDetectedAmazonRegion()) return@launch
            val detectedDomain =
                if (userPreferences.getAmazonDomain() == null) {
                    AmazonRegionDetector.detectDomain(context)
                } else {
                    null
                }
            userPreferences.markAmazonRegionDetected()
            if (detectedDomain != null) {
                withContext(Dispatchers.Main) { viewModel.setAmazonDomain(detectedDomain) }
            }
        }
    }

    /**
     * Check shortly after the first frame so an available Play update can be represented by the
     * Home card without waiting for unrelated deferred startup work or an empty search query.
     */
    private fun scheduleUpdateCheck() {
        lifecycleScope.launch {
            delay(UPDATE_CHECK_DELAY_MS)
            val targetActivity = activity ?: return@launch
            UpdateHelper.checkForUpdates(targetActivity, viewModel::onUpdateStateChanged)
        }
    }

    private fun scheduleDeferredDiagnostics() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(NON_CRITICAL_STARTUP_DELAY_MS)
            MemoryDiagnostics.install(context)
        }
    }

    private fun scheduleOverlayPostFrameWork() {
        scheduleBackgroundPreload()
    }

    /**
     * The search screen picks its size once the theme is known; preloading the finer of the light and
     * dark sizes lets it only scale down. Without blur Home lets Android draw the wallpaper.
     */
    private fun preloadWallpaperDivisor(): Int {
        val lightBlur = userPreferences.getWallpaperBlurRadius(isDarkMode = false)
        val darkBlur = userPreferences.getWallpaperBlurRadius(isDarkMode = true)
        val showsSystemBackdrop = (activity as? MainActivity)?.canShowSystemWallpaperBackdrop == true
        if (showsSystemBackdrop && lightBlur <= 0f && darkBlur <= 0f) {
            return WallpaperUtils.WALLPAPER_THUMBNAIL_DIVISOR
        }
        return minOf(
            WallpaperUtils.wallpaperDivisorForBlur(lightBlur),
            WallpaperUtils.wallpaperDivisorForBlur(darkBlur),
        )
    }

    private fun preloadCustomImageDivisor(): Int =
        minOf(
            WallpaperUtils.wallpaperDivisorForBlur(userPreferences.getWallpaperBlurRadius(isDarkMode = false)),
            WallpaperUtils.wallpaperDivisorForBlur(userPreferences.getWallpaperBlurRadius(isDarkMode = true)),
        )

    private fun scheduleBackgroundPreload() {
        lifecycleScope.launch(Dispatchers.IO) {
            val backgroundSource = userPreferences.getBackgroundSource()
            Trace.beginSection(TRACE_BACKGROUND_PRELOAD)
            try {
                when (backgroundSource) {
                    BackgroundSource.SYSTEM_WALLPAPER ->
                        WallpaperUtils.preloadWallpaper(context, preloadWallpaperDivisor())
                    BackgroundSource.CUSTOM_IMAGE ->
                        WallpaperUtils.preloadCustomImage(
                            context = context,
                            uriString = userPreferences.getCustomImageUri(),
                            divisor = preloadCustomImageDivisor(),
                        )
                    BackgroundSource.THEME -> Unit
                }
            } finally {
                Trace.endSection()
            }
        }
    }
}
