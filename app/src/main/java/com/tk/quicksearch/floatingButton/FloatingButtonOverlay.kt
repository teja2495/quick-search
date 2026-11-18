package com.tk.quicksearch.floatingButton

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.tk.quicksearch.R
import com.tk.quicksearch.edgeGesture.EdgeGestureOverlay
import com.tk.quicksearch.search.data.preferences.FloatingButtonConfig
import com.tk.quicksearch.search.data.preferences.FloatingButtonPreferences
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Round search button drawn as an accessibility overlay. Tapping it opens Quick Search from any
 * app; long pressing it lets the user drag it anywhere on screen. It never reads screen content.
 */
class FloatingButtonOverlay(
    private val service: AccessibilityService,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val preferences = FloatingButtonPreferences(service)
    private var buttonView: ButtonView? = null
    private var previewVisible = false
    private var appVisible = false
    private var screenReceiverRegistered = false

    // Held as a field: SharedPreferences keeps listeners only weakly.
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (FloatingButtonPreferences.isFloatingButtonKey(key)) refresh()
        }

    // Keeps the button off the lock screen.
    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) = refresh()
        }

    fun attach() {
        preferences.registerListener(preferenceListener)
        refresh()
    }

    fun detach() {
        preferences.unregisterListener(preferenceListener)
        setScreenReceiverRegistered(false)
        removeButton()
    }

    /** Keeps the button visible over Quick Search and prevents it from relaunching the app. */
    fun setPreviewVisible(visible: Boolean) {
        if (previewVisible == visible) return
        previewVisible = visible
        refresh()
    }

    /** Hides the button while Quick Search itself is in the foreground. */
    fun setAppVisible(visible: Boolean) {
        if (appVisible == visible) return
        appVisible = visible
        refresh()
    }

    fun refresh() {
        val config = preferences.getConfig()
        setScreenReceiverRegistered(config.enabled)
        if (!config.enabled || (appVisible && !previewVisible) || isScreenLockedOrOff()) {
            removeButton()
            return
        }
        val view = buttonView
        if (view == null) {
            val newView = ButtonView(service)
            newView.applyAppearance(config)
            runCatching { windowManager.addView(newView, layoutParams(config)) }
                .onSuccess { buttonView = newView }
                .onFailure { error -> Log.w(TAG, "button not added", error) }
        } else if (!view.isDragging) {
            view.applyAppearance(config)
            runCatching { windowManager.updateViewLayout(view, layoutParams(config)) }
        }
    }

    private fun removeButton() {
        val view = buttonView ?: return
        buttonView = null
        view.cancelPendingLongPress()
        runCatching { windowManager.removeView(view) }
    }

    private fun setScreenReceiverRegistered(registered: Boolean) {
        if (screenReceiverRegistered == registered) return
        screenReceiverRegistered = registered
        if (registered) {
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
            ContextCompat.registerReceiver(
                service,
                screenStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } else {
            runCatching { service.unregisterReceiver(screenStateReceiver) }
        }
    }

    private fun isScreenLockedOrOff(): Boolean {
        val powerManager = service.getSystemService(PowerManager::class.java)
        val keyguardManager = service.getSystemService(KeyguardManager::class.java)
        return powerManager?.isInteractive == false || keyguardManager?.isKeyguardLocked == true
    }

    private fun touchSizePx(config: FloatingButtonConfig): Int =
        maxOf(dpToPx(config.sizeDp), dpToPx(DesignTokens.MinTouchTarget.value.toInt()))

    private fun layoutParams(config: FloatingButtonConfig): WindowManager.LayoutParams {
        val size = touchSizePx(config)
        val area = movableArea(size)
        return WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = area.left + (area.width() * config.positionX).roundToInt()
            y = area.top + (area.height() * config.positionY).roundToInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Positions are computed against the full screen, with system bars excluded
                // explicitly by movableArea().
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    /** Range of top-left positions that keep a button of [size] clear of the system bars. */
    private fun movableArea(size: Int): Rect {
        val bounds: Rect
        val insetLeft: Int
        val insetTop: Int
        val insetRight: Int
        val insetBottom: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            bounds = metrics.bounds
            val insets =
                metrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
                )
            insetLeft = insets.left
            insetTop = insets.top
            insetRight = insets.right
            insetBottom = insets.bottom
        } else {
            val displayMetrics = service.resources.displayMetrics
            bounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
            insetLeft = 0
            // heightPixels already excludes the navigation bar; only the status bar needs clearing.
            insetTop = statusBarHeightPx()
            insetRight = 0
            insetBottom = 0
        }
        val left = insetLeft
        val top = insetTop
        val right = (bounds.width() - insetRight - size).coerceAtLeast(left)
        val bottom = (bounds.height() - insetBottom - size).coerceAtLeast(top)
        return Rect(left, top, right, bottom)
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun statusBarHeightPx(): Int {
        val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) service.resources.getDimensionPixelSize(id) else 0
    }

    private fun isNightMode(): Boolean =
        service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    private fun dpToPx(dp: Int): Int =
        TypedValue
            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), service.resources.displayMetrics)
            .roundToInt()

    private fun onTap(view: ButtonView) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        if (previewVisible) {
            Log.d(TAG, "tap ignored: customization preview")
            return
        }
        runCatching { service.startActivity(EdgeGestureOverlay.launchIntent(service)) }
            .onSuccess { Log.d(TAG, "launch requested") }
            .onFailure { error -> Log.w(TAG, "launch failed", error) }
    }

    private fun onDragFinished(
        view: ButtonView,
        params: WindowManager.LayoutParams,
    ) {
        val area = movableArea(view.width)
        val positionX = if (area.width() > 0) (params.x - area.left).toFloat() / area.width() else 0f
        val positionY = if (area.height() > 0) (params.y - area.top).toFloat() / area.height() else 0f
        preferences.setPosition(positionX.coerceIn(0f, 1f), positionY.coerceIn(0f, 1f))
    }

    @SuppressLint("ViewConstructor")
    private inner class ButtonView(
        context: Context,
    ) : ImageView(context) {
        private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        private var downRawX = 0f
        private var downRawY = 0f
        private var downParamsX = 0
        private var downParamsY = 0
        private var movedBeforeLongPress = false
        var isDragging = false
            private set

        private val startDrag =
            Runnable {
                isPressed = false
                isDragging = true
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                animate().scaleX(DRAG_SCALE).scaleY(DRAG_SCALE).setDuration(SCALE_ANIMATION_MS).start()
            }

        init {
            setImageResource(R.drawable.ic_widget_search)
            scaleType = ScaleType.FIT_CENTER
            contentDescription = context.getString(R.string.settings_gesture_open_quick_search)
            isClickable = true
        }

        fun applyAppearance(config: FloatingButtonConfig) {
            // Contrast with the system theme: white in dark mode, black in light mode.
            val night = isNightMode()
            val containerColor = if (night) Color.WHITE else Color.BLACK
            val iconColor = if (night) Color.BLACK else Color.WHITE
            val visibleSize = dpToPx(config.sizeDp)
            val inset = (touchSizePx(config) - visibleSize) / 2
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(containerColor)
            }
            val stateLayerAlpha = (255 * AppColors.TintTonalAlpha).roundToInt()
            val stateLayer = ColorStateList.valueOf(ColorUtils.setAlphaComponent(iconColor, stateLayerAlpha))
            background = InsetDrawable(RippleDrawable(stateLayer, circle, null), inset)
            imageTintList = ColorStateList.valueOf(iconColor)
            val padding = inset + (visibleSize * ICON_PADDING_FRACTION).roundToInt()
            setPadding(padding, padding, padding, padding)
            alpha = config.opacity
        }

        fun cancelPendingLongPress() {
            removeCallbacks(startDrag)
        }

        override fun performClick(): Boolean {
            super.performClick()
            onTap(this)
            return true
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val params = layoutParams as? WindowManager.LayoutParams ?: return true
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isPressed = true
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downParamsX = params.x
                    downParamsY = params.y
                    movedBeforeLongPress = false
                    isDragging = false
                    postDelayed(startDrag, LONG_PRESS_DELAY_MS)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (isDragging) {
                        val area = movableArea(width)
                        params.x = (downParamsX + dx.roundToInt()).coerceIn(area.left, area.right)
                        params.y = (downParamsY + dy.roundToInt()).coerceIn(area.top, area.bottom)
                        runCatching { windowManager.updateViewLayout(this, params) }
                    } else if (!movedBeforeLongPress && (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx)) {
                        isPressed = false
                        movedBeforeLongPress = true
                        cancelPendingLongPress()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    isPressed = false
                    cancelPendingLongPress()
                    if (isDragging) {
                        endDrag(params)
                    } else if (!movedBeforeLongPress) {
                        performClick()
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    isPressed = false
                    cancelPendingLongPress()
                    if (isDragging) endDrag(params)
                }
            }
            return true
        }

        private fun endDrag(params: WindowManager.LayoutParams) {
            isDragging = false
            animate().scaleX(1f).scaleY(1f).setDuration(SCALE_ANIMATION_MS).start()
            onDragFinished(this, params)
        }
    }

    companion object {
        private const val TAG = "FloatingButton"
        private const val ICON_PADDING_FRACTION = 0.25f
        // Shrinks rather than grows: the window is sized exactly to the button and would clip it.
        private const val DRAG_SCALE = 0.9f
        private val SCALE_ANIMATION_MS = AppMotion.DurationQuick.toLong()
        // Shorter than the system long-press timeout so picking the button up feels quick.
        private const val LONG_PRESS_DELAY_MS = 250L
    }
}
