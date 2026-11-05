package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.notchGesture.NotchGestureSupport
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityEvent
import com.tk.quicksearch.edgeGesture.EdgeGestureOverlay
import com.tk.quicksearch.floatingButton.FloatingButtonOverlay
import com.tk.quicksearch.notchGesture.NotchGestureOverlay
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpLaunchWatcher

/**
 * Hosts the Home double-tap lock action, the system-wide edge swipe handle, the floating button and
 * the notch tap target, and spots bumped apps opened outside Quick Search for SpeedBump.
 */
class LockScreenAccessibilityService : AccessibilityService() {
    private var edgeGestureOverlay: EdgeGestureOverlay? = null
    private var floatingButtonOverlay: FloatingButtonOverlay? = null
    private var notchGestureOverlay: NotchGestureOverlay? = null

    override fun onServiceConnected() {
        instance = this
        // Window events are only read by SpeedBump outside Quick Search, so subscribe only while it
        // has work; otherwise the XML's subscription would wake the app on every screen change.
        SpeedBumpLaunchWatcher.attach(this, ::updateWindowEventSubscription)
        updateWindowEventSubscription(SpeedBumpLaunchWatcher.isActive)
        edgeGestureOverlay =
            EdgeGestureOverlay(this).also { overlay ->
                overlay.setPreviewVisible(edgeGesturePreviewVisible)
                overlay.attach()
            }
        floatingButtonOverlay =
            FloatingButtonOverlay(this).also { overlay ->
                overlay.setPreviewVisible(floatingButtonPreviewVisible)
                overlay.setAppVisible(resumedAppSurfaceCount > 0)
                overlay.attach()
            }
        if (NotchGestureSupport.isSupported(this)) {
            notchGestureOverlay = NotchGestureOverlay(this).also { overlay -> overlay.attach() }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        edgeGestureOverlay?.refresh()
        floatingButtonOverlay?.refresh()
        notchGestureOverlay?.refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            SpeedBumpLaunchWatcher.onWindowStateChanged(event)
        }
    }

    private fun updateWindowEventSubscription(active: Boolean) {
        val eventTypes = if (active) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0
        runCatching { serviceInfo = serviceInfo?.apply { this.eventTypes = eventTypes } }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        SpeedBumpLaunchWatcher.detach()
        edgeGestureOverlay?.detach()
        edgeGestureOverlay = null
        floatingButtonOverlay?.detach()
        floatingButtonOverlay = null
        notchGestureOverlay?.detach()
        notchGestureOverlay = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    companion object {
        @Volatile
        private var instance: LockScreenAccessibilityService? = null

        @Volatile
        private var edgeGesturePreviewVisible = false

        @Volatile
        private var floatingButtonPreviewVisible = false

        @Volatile
        private var resumedAppSurfaceCount = 0

        fun lockScreen() {
            instance?.lockScreen()
        }

        /** Must be called on the main thread. */
        fun setEdgeGesturePreviewVisible(visible: Boolean) {
            edgeGesturePreviewVisible = visible
            instance?.edgeGestureOverlay?.setPreviewVisible(visible)
        }

        /** Must be called on the main thread. */
        fun setFloatingButtonPreviewVisible(visible: Boolean) {
            floatingButtonPreviewVisible = visible
            instance?.floatingButtonOverlay?.setPreviewVisible(visible)
        }

        /**
         * Tracks whether a Quick Search activity is in the foreground so the floating button stays
         * out of the way there. Must be called on the main thread.
         */
        fun onAppSurfaceResumed() {
            resumedAppSurfaceCount++
            instance?.floatingButtonOverlay?.setAppVisible(true)
        }

        /** Must be called on the main thread. */
        fun onAppSurfacePaused() {
            resumedAppSurfaceCount = (resumedAppSurfaceCount - 1).coerceAtLeast(0)
            instance?.floatingButtonOverlay?.setAppVisible(resumedAppSurfaceCount > 0)
        }

        fun isEnabled(context: Context): Boolean {
            val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
                ?: return false
            return accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { service ->
                    service.resolveInfo.serviceInfo.packageName == context.packageName &&
                        service.resolveInfo.serviceInfo.name == LockScreenAccessibilityService::class.java.name
                }
        }
    }
}
