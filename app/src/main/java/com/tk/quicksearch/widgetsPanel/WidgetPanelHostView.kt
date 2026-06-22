package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.SystemClock
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Host that vends a host view capable of detecting long-press regardless of whether the widget's
 * own children consume touches, and forwarding the in-progress drag after long-press so the user
 * doesn't have to lift and re-press.
 */
internal class WidgetPanelHost(
    context: Context,
    hostId: Int,
) : AppWidgetHost(context, hostId) {
    var onWidgetLongPress: ((appWidgetId: Int) -> Unit)? = null
    var onWidgetDragMove: ((appWidgetId: Int, totalDeltaX: Float, totalDeltaY: Float) -> Unit)? =
        null
    var onWidgetDragEnd: ((appWidgetId: Int) -> Unit)? = null

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
            view.isScrollInProgressProvider = { isScrollInProgressProvider() }
            view.onDetached = { liveViews.remove(view) }
            liveViews.add(view)
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
}

/**
 * Detects long-press at the parent level via `onInterceptTouchEvent`, then takes over the gesture
 * and forwards drag deltas via callbacks so the user can long-press-and-drag in a single motion.
 *
 * Cancels the pending long-press aggressively in three ways: explicit ACTION_CANCEL, drag past
 * touch slop, or a child / ancestor calling `requestDisallowInterceptTouchEvent(true)` (e.g. a
 * Compose `verticalScroll` taking over the gesture).
 */
private class WidgetPanelHostView(
    context: Context,
) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null
    var onDragMove: ((deltaX: Float, deltaY: Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    var onDetached: (() -> Unit)? = null
    var isScrollInProgressProvider: () -> Boolean = { false }

    fun cancelPendingLongPress() {
        removeCallbacks(longPressRunnable)
    }

    override fun setAppWidget(
        appWidgetId: Int,
        info: AppWidgetProviderInfo?,
    ) {
        super.setAppWidget(appWidgetId, info)
    }

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    // Tighter Y-axis cancellation than Compose's scroll slop so that gradual vertical scrolls
    // cancel the long-press timer before Compose claims the gesture and dispatches ACTION_CANCEL.
    // Without this, a slow scroll that takes >500ms to exceed scaledTouchSlop can fire long-press
    // mid-scroll. Picked small but above natural finger tremor (~1-3px).
    private val verticalScrollCancelSlop =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics,
        )
    private var downRawX = 0f
    private var downRawY = 0f
    private var longPressFired = false
    private var dragHandled = false
    private val longPressRunnable =
        Runnable {
            if (longPressFired) return@Runnable
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
                downRawX = ev.rawX
                downRawY = ev.rawY
                longPressFired = false
                dragHandled = false
                removeCallbacks(longPressRunnable)
                // Don't arm long-press if the surrounding scroll is mid-fling — the user is
                // tapping to stop momentum, not asking to edit.
                if (!isScrollInProgressProvider()) {
                    postDelayed(longPressRunnable, longPressTimeout)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!longPressFired) {
                    if (
                        abs(ev.rawX - downRawX) > touchSlop ||
                        abs(ev.rawY - downRawY) > verticalScrollCancelSlop
                    ) {
                        removeCallbacks(longPressRunnable)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = longPressFired

    override fun onTouchEvent(event: MotionEvent): Boolean {
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

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept && !longPressFired) {
            // A child (or Compose scroll above us) has taken over the gesture — bail out of the
            // pending long-press so scrolling never escalates to edit mode.
            removeCallbacks(longPressRunnable)
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
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
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }
}
