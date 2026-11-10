package com.tk.quicksearch.notchGesture

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.DisplayCutout
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.edgeGesture.EdgeGestureOverlay
import com.tk.quicksearch.media.MediaCommand
import com.tk.quicksearch.media.MediaControls
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.data.preferences.GesturesPreferences
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Invisible touch target laid over the top display cutout (the notch or camera hole) as an
 * accessibility overlay. Single taps, double taps, and long presses run the user's notch gesture actions from any app.
 * It never reads screen content.
 */
class NotchGestureOverlay(
    private val service: AccessibilityService,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val preferences = GesturesPreferences(service)
    private var touchView: NotchTouchView? = null
    private var singleTap = NotchTapAction()
    private var doubleTap = NotchTapAction()
    private var longPress = NotchTapAction()
    private var notchCenterX: Int? = null

    // Held as a field: SharedPreferences keeps listeners only weakly.
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (GesturesPreferences.isNotchGestureKey(key)) refresh()
        }

    fun attach() {
        preferences.registerListener(preferenceListener)
        refresh()
    }

    fun detach() {
        preferences.unregisterListener(preferenceListener)
        removeView()
    }

    /** Re-reads the actions and re-measures the cutout, which moves with rotation. */
    fun refresh() {
        singleTap =
            NotchTapAction(
                preferences.getNotchSingleTapAction(),
                preferences.getNotchSingleTapCustomAction(),
                preferences.getNotchSingleTapAliasTarget(),
            )
        doubleTap =
            NotchTapAction(
                preferences.getNotchDoubleTapAction(),
                preferences.getNotchDoubleTapCustomAction(),
                preferences.getNotchDoubleTapAliasTarget(),
            )
        longPress =
            NotchTapAction(
                preferences.getNotchLongPressAction(),
                preferences.getNotchLongPressCustomAction(),
                preferences.getNotchLongPressAliasTarget(),
            )
        if (listOf(singleTap, doubleTap, longPress).all { it.action == HomeSwipeGestureAction.NONE }) {
            removeView()
            return
        }
        val cutout = topCutoutRect()
        if (cutout == null) {
            Log.d(TAG, "no top display cutout; notch gestures inactive")
            removeView()
            return
        }
        notchCenterX = cutout.centerX()
        val params = layoutParams(cutout)
        val view = touchView
        if (view == null) {
            val newView = NotchTouchView(service)
            runCatching { windowManager.addView(newView, params) }
                .onSuccess {
                    touchView = newView
                    Log.d(TAG, "notch target added: ${params.width}x${params.height} at x=${params.x}")
                }.onFailure { error -> Log.w(TAG, "notch target not added", error) }
        } else {
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    private fun removeView() {
        val view = touchView ?: return
        touchView = null
        view.cancelPendingTap()
        runCatching { windowManager.removeView(view) }
    }

    /** The cutout touching the top edge in the current rotation; null on devices without one. */
    private fun topCutoutRect(): Rect? {
        val cutout: DisplayCutout? =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    windowManager.currentWindowMetrics.windowInsets.displayCutout
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.cutout
                else -> null
            }
        return cutout?.boundingRects
            ?.filter { rect -> rect.top <= 0 && !rect.isEmpty }
            ?.maxByOrNull { rect -> rect.width() }
    }

    private fun layoutParams(cutout: Rect): WindowManager.LayoutParams {
        val screenWidth = screenWidthPx()
        val padding = dpToPx(TOUCH_PADDING_DP)
        // Camera holes are small; give the finger at least a regular touch target around them.
        val width = maxOf(cutout.width() + padding * 2, dpToPx(MIN_TOUCH_SIZE_DP)).coerceAtMost(screenWidth)
        val height = maxOf(cutout.bottom + padding, dpToPx(MIN_TOUCH_SIZE_DP) / 2)
        val left = (cutout.centerX() - width / 2).coerceIn(0, screenWidth - width)
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = left
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun screenWidthPx(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            service.resources.displayMetrics.widthPixels
        }

    private fun dpToPx(dp: Int): Int =
        TypedValue
            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), service.resources.displayMetrics)
            .roundToInt()

    private fun perform(view: View, tap: NotchTapAction) {
        if (tap.action == HomeSwipeGestureAction.NONE) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val keyguardManager = service.getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true && tap.action != HomeSwipeGestureAction.PLAY_PAUSE_MEDIA) {
            Log.d(TAG, "tap ignored: keyguard locked")
            return
        }
        when (tap.action) {
            HomeSwipeGestureAction.OPEN_QUICK_SEARCH -> openQuickSearch()
            HomeSwipeGestureAction.PINNED_GLANCE ->
                runCatching { OverlayModeController.startOverlay(service, pinnedGlanceOnly = true, anchorX = notchCenterX) }
                    .onFailure { error -> Log.w(TAG, "pinned glance overlay failed", error) }
            HomeSwipeGestureAction.ALL_APPS -> openQuickSearch(NotchGestureRequest.AllApps)
            HomeSwipeGestureAction.SEARCH_ENGINE ->
                tap.aliasTarget?.let { openQuickSearch(NotchGestureRequest.SearchEngine(it)) }
            HomeSwipeGestureAction.TOOL -> tap.aliasTarget?.let { openQuickSearch(NotchGestureRequest.Tool(it)) }
            HomeSwipeGestureAction.WIDGETS_PANEL ->
                startActivity(
                    Intent(service, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_OPEN_WIDGETS_PANEL
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    },
                )
            HomeSwipeGestureAction.LOCK_SCREEN ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                }
            HomeSwipeGestureAction.PLAY_PAUSE_MEDIA -> MediaControls.dispatch(service, MediaCommand.PLAY_PAUSE)
            HomeSwipeGestureAction.NOTIFICATION_PANEL ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            HomeSwipeGestureAction.QUICK_SETTINGS ->
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            HomeSwipeGestureAction.CUSTOM ->
                CustomWidgetButtonAction.fromJson(tap.customActionJson)?.let { action ->
                    runCatching { WidgetActionActivity.launch(service, action) }
                        .onFailure { error -> Log.w(TAG, "custom action failed", error) }
                }
            HomeSwipeGestureAction.CLOSE_QUICK_SEARCH,
            HomeSwipeGestureAction.NONE,
            -> Unit
        }
    }

    private fun openQuickSearch(request: NotchGestureRequest? = null) {
        request?.let(NotchGestureRequests::post)
        startActivity(EdgeGestureOverlay.launchIntent(service))
    }

    private fun startActivity(intent: Intent) {
        runCatching { service.startActivity(intent) }
            .onFailure { error -> Log.w(TAG, "launch failed", error) }
    }

    private data class NotchTapAction(
        val action: HomeSwipeGestureAction = HomeSwipeGestureAction.NONE,
        val customActionJson: String? = null,
        val aliasTarget: String? = null,
    )

    @SuppressLint("ViewConstructor")
    private inner class NotchTouchView(
        context: Context,
    ) : View(context) {
        private val tapSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        private val doubleTapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()
        private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
        private var downX = 0f
        private var downY = 0f
        private var downTime = 0L
        private var pendingSingleTap: Runnable? = null
        private var pendingLongPress: Runnable? = null
        private var longPressFired = false

        fun cancelPendingTap() {
            pendingSingleTap?.let(::removeCallbacks)
            pendingSingleTap = null
            cancelPendingLongPress()
        }

        private fun cancelPendingLongPress() {
            pendingLongPress?.let(::removeCallbacks)
            pendingLongPress = null
        }

        private fun scheduleLongPress() {
            cancelPendingLongPress()
            if (longPress.action == HomeSwipeGestureAction.NONE) return
            val runnable =
                Runnable {
                    pendingLongPress = null
                    longPressFired = true
                    // A long press ends any half-finished double tap.
                    pendingSingleTap?.let(::removeCallbacks)
                    pendingSingleTap = null
                    perform(this, longPress)
                }
            pendingLongPress = runnable
            postDelayed(runnable, longPressTimeoutMs)
        }

        private fun onTap() {
            // Without a double-tap action there is nothing to wait for, so the single tap is instant.
            if (doubleTap.action == HomeSwipeGestureAction.NONE) {
                perform(this, singleTap)
                return
            }
            if (pendingSingleTap != null) {
                cancelPendingTap()
                perform(this, doubleTap)
                return
            }
            val runnable =
                Runnable {
                    pendingSingleTap = null
                    perform(this, singleTap)
                }
            pendingSingleTap = runnable
            postDelayed(runnable, doubleTapTimeoutMs)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    downTime = event.eventTime
                    longPressFired = false
                    scheduleLongPress()
                }

                MotionEvent.ACTION_MOVE ->
                    if (abs(event.rawX - downX) > tapSlopPx || abs(event.rawY - downY) > tapSlopPx) {
                        cancelPendingLongPress()
                    }

                MotionEvent.ACTION_CANCEL -> cancelPendingLongPress()

                MotionEvent.ACTION_UP -> {
                    cancelPendingLongPress()
                    if (!longPressFired &&
                        abs(event.rawX - downX) <= tapSlopPx &&
                        abs(event.rawY - downY) <= tapSlopPx &&
                        event.eventTime - downTime <= longPressTimeoutMs
                    ) {
                        onTap()
                    }
                }
            }
            return true
        }

        override fun onDetachedFromWindow() {
            cancelPendingTap()
            super.onDetachedFromWindow()
        }
    }

    private companion object {
        const val TAG = "NotchGesture"
        const val TOUCH_PADDING_DP = 8
        const val MIN_TOUCH_SIZE_DP = 48
    }
}
