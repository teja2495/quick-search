package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.SystemClock
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

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView =
        WidgetPanelHostView(context).also { view ->
            view.onLongPress = { onWidgetLongPress?.invoke(appWidgetId) }
            view.onDragMove = { dx, dy -> onWidgetDragMove?.invoke(appWidgetId, dx, dy) }
            view.onDragEnd = { onWidgetDragEnd?.invoke(appWidgetId) }
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

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
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
                postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!longPressFired) {
                    if (
                        abs(ev.rawX - downRawX) > touchSlop ||
                        abs(ev.rawY - downRawY) > touchSlop
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
