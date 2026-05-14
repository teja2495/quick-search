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
 * own children consume touches.
 */
internal class WidgetPanelHost(
    context: Context,
    hostId: Int,
) : AppWidgetHost(context, hostId) {
    var onWidgetLongPress: ((appWidgetId: Int) -> Unit)? = null

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView =
        WidgetPanelHostView(context).also { view ->
            view.onLongPress = { onWidgetLongPress?.invoke(appWidgetId) }
        }
}

/**
 * Detects a long-press at the parent level via `onInterceptTouchEvent`. This works even when the
 * widget contains clickable children (which would otherwise swallow `setOnLongClickListener`).
 */
private class WidgetPanelHostView(
    context: Context,
) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressFired = false
    private val longPressRunnable =
        Runnable {
            if (longPressFired) return@Runnable
            longPressFired = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongPress?.invoke()
            cancelChildren()
        }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                longPressFired = false
                removeCallbacks(longPressRunnable)
                postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> {
                if (
                    abs(ev.x - downX) > touchSlop ||
                    abs(ev.y - downY) > touchSlop
                ) {
                    removeCallbacks(longPressRunnable)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
            }
        }
        return longPressFired
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }

    private fun cancelChildren() {
        val now = SystemClock.uptimeMillis()
        val cancel =
            MotionEvent.obtain(
                now,
                now,
                MotionEvent.ACTION_CANCEL,
                downX,
                downY,
                0,
            )
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }
}
