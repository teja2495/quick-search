package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.notchGesture.NotchGestureSupport
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.tk.quicksearch.R
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
        // Each feature starts on its own: if one throws, the others keep working and the service
        // stays connected. A crash here would leave the system toggle on with nothing running.
        runFeature("SpeedBump") {
            // Window events are only read by SpeedBump outside Quick Search, so subscribe only while
            // it has work; otherwise the XML's subscription would wake the app on every screen change.
            SpeedBumpLaunchWatcher.attach(this, ::updateWindowEventSubscription)
            updateWindowEventSubscription(SpeedBumpLaunchWatcher.isActive)
        }
        runFeature("edge gesture") {
            edgeGestureOverlay =
                EdgeGestureOverlay(this).also { overlay ->
                    overlay.setPreviewVisible(edgeGesturePreviewVisible)
                    overlay.attach()
                }
        }
        runFeature("floating button") {
            floatingButtonOverlay =
                FloatingButtonOverlay(this).also { overlay ->
                    overlay.setPreviewVisible(floatingButtonPreviewVisible)
                    overlay.setAppVisible(resumedAppSurfaceCount > 0)
                    overlay.attach()
                }
        }
        runFeature("notch gesture") {
            if (NotchGestureSupport.isSupported(this)) {
                notchGestureOverlay = NotchGestureOverlay(this).also { overlay -> overlay.attach() }
            }
        }
    }

    private inline fun runFeature(name: String, block: () -> Unit) {
        runCatching(block).onFailure { error -> Log.e(TAG, "$name failed to start", error) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runFeature("edge gesture") { edgeGestureOverlay?.refresh() }
        runFeature("floating button") { floatingButtonOverlay?.refresh() }
        runFeature("notch gesture") { notchGestureOverlay?.refresh() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            runFeature("SpeedBump") { SpeedBumpLaunchWatcher.onWindowStateChanged(event) }
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

        /**
         * Locks the screen, or explains why it can't when the service is turned on in system
         * settings but Android has stopped it.
         */
        fun lockScreen(context: Context) {
            val service = instance
            if (service != null) {
                service.lockScreen()
            } else if (isTurnedOnInSettings(context)) {
                showStoppedHint(context)
            }
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

        /** Whether the service is running and its features work. */
        fun isEnabled(context: Context): Boolean {
            if (instance != null) return true
            val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
                ?: return false
            return accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { service ->
                    service.resolveInfo.serviceInfo.packageName == context.packageName &&
                        service.resolveInfo.serviceInfo.name == LockScreenAccessibilityService::class.java.name
                }
        }

        /**
         * Whether the user turned the service on in system settings. It can be on while the service
         * isn't running: after a crash Android shows it as "not working" until it is toggled again,
         * and it is unbound for a moment while the app restarts.
         */
        fun isTurnedOnInSettings(context: Context): Boolean {
            val enabledServices =
                runCatching {
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    )
                }.getOrNull() ?: return false
            val component = ComponentName(context, LockScreenAccessibilityService::class.java)
            return enabledServices.split(':').any { entry ->
                ComponentName.unflattenFromString(entry.trim()) == component
            }
        }

        /** Turned on in system settings but not running, so it needs to be toggled off and on. */
        fun isStopped(context: Context): Boolean = !isEnabled(context) && isTurnedOnInSettings(context)

        /**
         * Opens the system accessibility settings. When the service is on there but stopped, first
         * tells the user to turn it off and on, since the settings page will already show it on.
         */
        fun openAccessibilitySettings(context: Context) {
            if (isStopped(context)) showStoppedHint(context)
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        private fun showStoppedHint(context: Context) {
            Toast.makeText(context, R.string.accessibility_service_stopped_hint, Toast.LENGTH_LONG).show()
        }

        private const val TAG = "QSAccessibility"
    }
}
