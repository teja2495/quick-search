package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Outline
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.MemoryDiagnostics
import kotlin.math.abs

private const val WIDGET_EDIT_LONG_PRESS_CONFIRMATION_MS = 100L

// Matches DesignTokens.WidgetPanelCardShape used by the Quick Note card.
private val widgetCornerRadiusDp = DesignTokens.WidgetPanelCornerRadius.value

/**
 * Host that vends a host view capable of detecting long-press regardless of whether the widget's
 * own children consume touches, and forwarding the in-progress drag after long-press so the user
 * doesn't have to lift and re-press.
 */
internal class WidgetPanelHost(
    context: Context,
    hostId: Int,
) : AppWidgetHost(context, hostId) {
    init {
        MemoryDiagnostics.widgetHostCreated()
    }

    var onWidgetLongPress: ((appWidgetId: Int) -> Unit)? = null
    var onWidgetDragMove: ((appWidgetId: Int, totalDeltaX: Float, totalDeltaY: Float) -> Unit)? =
        null
    var onWidgetDragEnd: ((appWidgetId: Int) -> Unit)? = null
    var onWidgetTouch: ((appWidgetId: Int) -> Boolean)? = null

    /**
     * Provider for whether the surrounding scroll container is currently scrolling/flinging.
     * When true, ACTION_DOWN on a widget should not arm the long-press timer (e.g. the user is
     * tapping the widget to halt a fling, not asking to enter edit mode).
     */
    var isScrollInProgressProvider: () -> Boolean = { false }

    private val liveViews = mutableSetOf<WidgetPanelHostView>()

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView =
        WidgetPanelHostView(context).also { view ->
            view.onLongPress = { onWidgetLongPress?.invoke(appWidgetId) }
            view.onDragMove = { dx, dy -> onWidgetDragMove?.invoke(appWidgetId, dx, dy) }
            view.onDragEnd = { onWidgetDragEnd?.invoke(appWidgetId) }
            view.onTouchStarted = { onWidgetTouch?.invoke(appWidgetId) ?: false }
            view.isScrollInProgressProvider = { isScrollInProgressProvider() }
            view.onDetached = {
                if (liveViews.remove(view)) MemoryDiagnostics.widgetViewsReleased(1)
            }
            liveViews.add(view)
            MemoryDiagnostics.widgetViewCreated()
        }

    override fun onProviderChanged(
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ) {
        super.onProviderChanged(appWidgetId, appWidget)
    }

    override fun onProvidersChanged() {
        super.onProvidersChanged()
    }

    /**
     * Cancel any pending long-press timers across every live widget host view. Called when the
     * surrounding scroll starts so a finger that landed on a widget right before a scroll began
     * doesn't accidentally promote to edit mode after the 500ms long-press timeout. Necessary
     * because once Compose's scroll consumes pointer events the host view may not see further
     * ACTION_MOVE events to trigger its own slop-based cancellation.
     */
    fun cancelAllPendingLongPresses() {
        liveViews.forEach { it.cancelPendingLongPress() }
    }

    /**
     * Starts listening for widget updates. Every surface that hosts panel widgets (the panel and
     * Home) uses the same host id, and the system keeps only the most recent listener per host id,
     * so the newest surface takes over updates and hands them back when it is released.
     */
    fun startListeningShared() {
        activeHosts.remove(this)
        activeHosts.add(this)
        startListening()
    }

    fun release() {
        val wasActiveListener = activeHosts.lastOrNull() === this
        activeHosts.remove(this)
        val releasedViewCount = liveViews.size
        liveViews.toList().forEach { it.releaseCallbacks() }
        liveViews.clear()
        onWidgetLongPress = null
        onWidgetDragMove = null
        onWidgetDragEnd = null
        onWidgetTouch = null
        isScrollInProgressProvider = { false }
        val nextListener = activeHosts.lastOrNull()
        if (nextListener == null) {
            stopListening()
        } else if (wasActiveListener) {
            nextListener.startListening()
        }
        clearViews()
        MemoryDiagnostics.widgetViewsReleased(releasedViewCount)
        MemoryDiagnostics.widgetHostReleased()
    }

    private companion object {
        // Main-thread only: hosts are created and released from composition effects.
        val activeHosts = mutableListOf<WidgetPanelHost>()
    }
}

/**
 * Detects long-press at the parent level via `onInterceptTouchEvent`, then takes over the gesture
 * and forwards drag deltas via callbacks so the user can long-press-and-drag in a single motion.
 *
 * Cancels the pending long-press on explicit ACTION_CANCEL or after movement passes Android's
 * standard touch slop. The surrounding Compose scroll also cancels pending presses as soon as it
 * starts scrolling.
 */
private class WidgetPanelHostView(
    context: Context,
) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null
    var onDragMove: ((deltaX: Float, deltaY: Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    var onTouchStarted: (() -> Boolean)? = null
    var onDetached: (() -> Unit)? = null
    var isScrollInProgressProvider: () -> Boolean = { false }

    fun cancelPendingLongPress() {
        longPressArmed = false
        removeCallbacks(longPressRunnable)
    }

    fun releaseCallbacks() {
        removeCallbacks(longPressRunnable)
        onLongPress = null
        onDragMove = null
        onDragEnd = null
        onTouchStarted = null
        onDetached = null
        isScrollInProgressProvider = { false }
    }

    override fun setAppWidget(
        appWidgetId: Int,
        info: AppWidgetProviderInfo?,
    ) {
        super.setAppWidget(appWidgetId, info)
        // Drop the framework's default widget padding so the widget fills its grid cell and lines
        // up with the Quick Note card; the rounded outline below then clips the widget's own
        // background instead of an inset square.
        setPadding(0, 0, 0, 0)
    }

    init {
        val cornerRadiusPx = widgetCornerRadiusDp * resources.displayMetrics.density
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(
                    view: View,
                    outline: Outline,
                ) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                }
            }
        clipToOutline = true
    }

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downRawX = 0f
    private var downRawY = 0f
    private var longPressFired = false
    private var longPressArmed = false
    private var dragHandled = false
    private var focusClearTouch = false
    private var cancellingChildren = false
    private val longPressRunnable =
        Runnable {
            if (!longPressArmed || longPressFired) return@Runnable
            longPressArmed = false
            longPressFired = true
            dragHandled = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongPress?.invoke()
            cancelChildren()
            parent?.requestDisallowInterceptTouchEvent(true)
        }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                focusClearTouch = onTouchStarted?.invoke() == true
                if (focusClearTouch) {
                    longPressArmed = false
                    removeCallbacks(longPressRunnable)
                    return true
                }
                downRawX = ev.rawX
                downRawY = ev.rawY
                longPressFired = false
                dragHandled = false
                longPressArmed = false
                removeCallbacks(longPressRunnable)
                // Don't arm long-press if the surrounding scroll is mid-fling — the user is
                // tapping to stop momentum, not asking to edit. The short confirmation buffer
                // prevents a delayed RemoteViews touch-up from being mistaken for a long press.
                if (!isScrollInProgressProvider()) {
                    longPressArmed = true
                    postDelayed(
                        longPressRunnable,
                        longPressTimeout + WIDGET_EDIT_LONG_PRESS_CONFIRMATION_MS,
                    )
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!longPressFired) {
                    if (
                        abs(ev.rawX - downRawX) > touchSlop ||
                        abs(ev.rawY - downRawY) > touchSlop
                    ) {
                        longPressArmed = false
                        removeCallbacks(longPressRunnable)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressArmed = false
                removeCallbacks(longPressRunnable)
                if (focusClearTouch) {
                    focusClearTouch = false
                    return true
                }
            }
        }
        if (focusClearTouch) return true
        val handled = super.dispatchTouchEvent(ev)
        // Claim the gesture on down while a long-press can still fire. Otherwise a press on a
        // non-clickable part of the widget is declined, the host (e.g. Compose interop) stops
        // delivering the rest of the gesture, and the drag after the long-press never arrives.
        return handled || (ev.actionMasked == MotionEvent.ACTION_DOWN && longPressArmed)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = longPressFired

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // The synthetic cancel sent to the widget's children must not end the drag that the
        // long-press just started.
        if (cancellingChildren) return true
        if (!dragHandled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                onDragMove?.invoke(event.rawX - downRawX, event.rawY - downRawY)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onDragEnd?.invoke()
                dragHandled = false
                longPressFired = false
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        onDetached?.invoke()
        super.onDetachedFromWindow()
    }

    private fun cancelChildren() {
        val now = SystemClock.uptimeMillis()
        val cancel =
            MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        // With no child touch target (the press landed on a non-clickable part of the widget),
        // ViewGroup delivers this cancel to our own onTouchEvent.
        cancellingChildren = true
        try {
            super.dispatchTouchEvent(cancel)
        } finally {
            cancellingChildren = false
            cancel.recycle()
        }
    }
}
