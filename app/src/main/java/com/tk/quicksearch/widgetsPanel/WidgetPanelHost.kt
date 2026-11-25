package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.RemoteViews
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.GraphicsContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.CrashLogManager
import com.tk.quicksearch.shared.util.MemoryDiagnostics
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext

private const val WIDGET_EDIT_LONG_PRESS_CONFIRMATION_MS = 100L

// Matches AppShapes.widgetPanelCard used by the Quick Note card.
private val widgetCornerRadiusDp = DesignTokens.WidgetPanelCornerRadius.value

/**
 * Host that vends a host view capable of detecting long-press regardless of whether the widget's
 * own children consume touches, and forwarding the in-progress drag after long-press so the user
 * doesn't have to lift and re-press.
 */
internal class WidgetPanelHost(
    context: Context,
    hostId: Int,
    private val isPanelHost: Boolean = false,
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
     * Whether each widget's provider has supplied content yet. Kept with the views, so a surface
     * that shows its kept views again does not flash a loading placeholder over them.
     */
    val widgetContentAvailable: SnapshotStateMap<Int, Boolean> = mutableStateMapOf()

    /**
     * Provider for whether the surrounding scroll container is currently scrolling/flinging.
     * When true, ACTION_DOWN on a widget should not arm the long-press timer (e.g. the user is
     * tapping the widget to halt a fling, not asking to enter edit mode).
     */
    var isScrollInProgressProvider: () -> Boolean = { false }

    private val liveViews = mutableSetOf<WidgetPanelHostView>()

    /** The latest view made for each widget, kept while detached so showing it again reuses it. */
    private val viewsById = mutableMapOf<Int, WidgetPanelHostView>()

    private val appContext = context.applicationContext
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)

    /** Provider info by widget id; refreshed by the provider-change callbacks below. */
    private val providerInfoCache = ConcurrentHashMap<Int, AppWidgetProviderInfo>()

    private var isLifecycleStarted = false
    private var isSuspended = false

    /**
     * The provider of [appWidgetId], or null when the id is no longer bound. Answers repeat lookups
     * from memory; the first one is a call into the system, so prefer [preloadProviderInfo] off the
     * main thread where a surface can wait for it.
     */
    fun providerInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        providerInfoCache[appWidgetId]
            ?: appWidgetManager.getAppWidgetInfo(appWidgetId)?.also { providerInfoCache[appWidgetId] = it }

    /** Looks up [appWidgetIds] on a background thread; returns the ids that are still bound. */
    suspend fun preloadProviderInfo(appWidgetIds: Collection<Int>): Set<Int> =
        withContext(Dispatchers.IO) {
            appWidgetIds.filterTo(HashSet()) { providerInfo(it) != null }
        }

    /**
     * Returns the host view for [appWidgetId]. A view this host made earlier that is no longer on
     * screen (Home hides its widgets while a query is typed) is reused as is, instead of fetching
     * and inflating the provider's views again.
     */
    fun obtainView(
        context: Context,
        appWidgetId: Int,
        info: AppWidgetProviderInfo,
        graphicsContext: GraphicsContext,
    ): AppWidgetHostView {
        val cached = viewsById[appWidgetId]
        if (
            cached != null &&
            !cached.isAttachedToWindow &&
            cached.context === context &&
            cached.appWidgetInfo?.provider == info.provider
        ) {
            (cached.parent as? ViewGroup)?.removeView(cached)
            if (liveViews.add(cached)) MemoryDiagnostics.widgetViewCreated()
            return cached
        }
        return createView(context, appWidgetId, info).also { view ->
            if (isPanelHost && info.profile == Process.myUserHandle()) {
                (view as WidgetPanelHostView).startSnapshotCapture(appWidgetId, info.provider, graphicsContext)
            }
        }
    }

    /** Ids of widgets whose kept view can be shown again without fetching and inflating it. */
    fun keptViewIds(): Set<Int> =
        viewsById.filterValues { !it.isAttachedToWindow }.keys.toSet()

    /** Drops kept views of widgets that are no longer shown by this surface. */
    fun retainViews(appWidgetIds: Set<Int>) {
        val iterator = viewsById.entries.iterator()
        while (iterator.hasNext()) {
            val (appWidgetId, view) = iterator.next()
            if (appWidgetId !in appWidgetIds && !view.isAttachedToWindow) {
                view.releaseCallbacks()
                iterator.remove()
            }
        }
    }

    override fun deleteAppWidgetId(appWidgetId: Int) {
        super.deleteAppWidgetId(appWidgetId)
        removeSnapshot(appWidgetId)
        providerInfoCache.remove(appWidgetId)
        widgetContentAvailable.remove(appWidgetId)
        viewsById.remove(appWidgetId)?.let { view -> if (!view.isAttachedToWindow) view.releaseCallbacks() }
    }

    fun removeSnapshot(appWidgetId: Int) {
        viewsById[appWidgetId]?.stopSnapshotCapture()
        WidgetPanelSnapshots.remove(appContext, appWidgetId)
    }

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView =
        WidgetPanelHostView(context, isPanelHost).also { view ->
            appWidget?.provider?.packageName?.let(CrashLogManager::registerHostedWidgetPackage)
            view.onLongPress = { onWidgetLongPress?.invoke(appWidgetId) }
            view.isLongPressEnabled = { onWidgetLongPress != null }
            view.onDragMove = { dx, dy -> onWidgetDragMove?.invoke(appWidgetId, dx, dy) }
            view.onDragEnd = { onWidgetDragEnd?.invoke(appWidgetId) }
            view.onTouchStarted = { onWidgetTouch?.invoke(appWidgetId) ?: false }
            if (isPanelHost) {
                viewsById[appWidgetId]?.stopSnapshotCapture()
                widgetContentAvailable.remove(appWidgetId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    view.setExecutor(panelInflationExecutor)
                }
            }
            view.onContentChanged = { hasContent ->
                if (viewsById[appWidgetId] === view) widgetContentAvailable[appWidgetId] = hasContent
            }
            view.isScrollInProgressProvider = { isScrollInProgressProvider() }
            view.onDetached = {
                if (liveViews.remove(view)) MemoryDiagnostics.widgetViewsReleased(1)
            }
            liveViews.add(view)
            viewsById[appWidgetId] = view
            MemoryDiagnostics.widgetViewCreated()
        }

    override fun onProviderChanged(
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo,
    ) {
        providerInfoCache[appWidgetId] = appWidget
        super.onProviderChanged(appWidgetId, appWidget)
    }

    override fun onProvidersChanged() {
        providerInfoCache.clear()
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
        // Listening again would make the system resend, and this host re-apply, every widget's views.
        if (activeHosts.lastOrNull() === this) return
        activeHosts.remove(this)
        val current = activeHosts.lastOrNull()
        if (!isPanelHost && current != null && current.isPanelHost && !current.isSuspended) {
            // The panel is on screen. Returning from a widget's bind or configure screen starts
            // every host again; taking updates from the panel here would send a just-added
            // widget's first views to this host, leaving the panel's view loading. Queue behind
            // the panel instead, taking over when it stops listening.
            activeHosts.add(activeHosts.size - 1, this)
            return
        }
        activeHosts.add(this)
        startListening()
    }

    /**
     * Listens for widget updates only while [lifecycle] is started, so a hidden surface does not
     * apply other apps' widget updates in the background; the system sends the missed ones when
     * listening starts again. Returns how to stop following [lifecycle].
     */
    fun listenWhileStarted(lifecycle: Lifecycle): () -> Unit {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        isLifecycleStarted = true
                        applyListening()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        isLifecycleStarted = false
                        applyListening()
                    }
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        return { lifecycle.removeObserver(observer) }
    }

    /**
     * Stops applying widget updates while [suspended], such as while Home hides its widgets for a
     * query. Like a stopped surface, the system sends the missed updates when listening resumes.
     */
    fun setSuspended(suspended: Boolean) {
        if (isSuspended == suspended) return
        isSuspended = suspended
        applyListening()
    }

    private fun applyListening() {
        when {
            isLifecycleStarted && !isSuspended -> startListeningShared()
            // While the app is in the foreground, the panel keeps listening after its page closes.
            // Listening again makes the system resend every widget's views, and re-applying them
            // on entry visibly reloads the widgets (lists blank, backgrounds flash). Applying the
            // occasional update to the kept views off screen is cheaper than that.
            isPanelHost && isLifecycleStarted && viewsById.isNotEmpty() -> listenInBackground()
            else -> pauseListening()
        }
    }

    /** Listens without taking updates from a surface that is showing its widgets now. */
    private fun listenInBackground() {
        if (this in activeHosts) return
        activeHosts.add(0, this)
        if (activeHosts.size == 1) startListening()
    }

    /** Stops this host's updates, handing them back to the previous surface if it was the listener. */
    private fun pauseListening() {
        if (this !in activeHosts) return
        val wasActiveListener = activeHosts.lastOrNull() === this
        activeHosts.remove(this)
        val nextListener = activeHosts.lastOrNull()
        if (nextListener == null) {
            stopListening()
        } else if (wasActiveListener) {
            nextListener.startListening()
        }
    }

    /**
     * Called when the surface showing this host is disposed but the host is kept for its next
     * showing: drops the surface's callbacks, keeping the widget views (and, for the panel, their
     * updates while the app is in the foreground).
     */
    fun detachSurface() {
        onWidgetLongPress = null
        onWidgetDragMove = null
        onWidgetDragEnd = null
        onWidgetTouch = null
        isScrollInProgressProvider = { false }
        setSuspended(true)
    }

    fun release() {
        val releasedViewCount = liveViews.size
        liveViews.toList().forEach { it.releaseCallbacks() }
        liveViews.clear()
        viewsById.values.forEach { it.releaseCallbacks() }
        viewsById.clear()
        providerInfoCache.clear()
        widgetContentAvailable.clear()
        isLifecycleStarted = false
        onWidgetLongPress = null
        onWidgetDragMove = null
        onWidgetDragEnd = null
        onWidgetTouch = null
        isScrollInProgressProvider = { false }
        pauseListening()
        clearViews()
        MemoryDiagnostics.widgetViewsReleased(releasedViewCount)
        MemoryDiagnostics.widgetHostReleased()
    }

    private companion object {
        val panelInflationExecutor = Dispatchers.Default.limitedParallelism(2).asExecutor()

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
    private val isPanelHost: Boolean,
) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null
    var isLongPressEnabled: () -> Boolean = { false }
    var onDragMove: ((deltaX: Float, deltaY: Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    var onTouchStarted: (() -> Boolean)? = null
    var onContentChanged: ((Boolean) -> Unit)? = null
    var onDetached: (() -> Unit)? = null
    var isScrollInProgressProvider: () -> Boolean = { false }
    private var snapshotCapture: WidgetPanelSnapshots.Capture? = null
    private var fallbackView: View? = null
    private var reportedContent = false
    private val reportDrawnContent = Runnable {
        val hasContent = renderedContent() != null
        if (reportedContent != hasContent) {
            reportedContent = hasContent
            onContentChanged?.invoke(hasContent)
        }
    }

    fun startSnapshotCapture(appWidgetId: Int, provider: ComponentName, graphicsContext: GraphicsContext) {
        snapshotCapture =
            WidgetPanelSnapshots.Capture(this, { renderedContent() != null }, appWidgetId, provider, graphicsContext)
    }

    fun stopSnapshotCapture() {
        snapshotCapture?.cancel()
        snapshotCapture = null
    }

    private fun renderedContent(): View? =
        getChildAt(0)?.takeIf { it !== fallbackView }

    override fun getDefaultView(): View = super.getDefaultView().also(::rememberFallbackView)

    override fun getErrorView(): View = super.getErrorView().also(::rememberFallbackView)

    private fun rememberFallbackView(view: View) {
        if (!isPanelHost) return
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        fallbackView = view
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!isPanelHost) {
            super.dispatchDraw(canvas)
            return
        }
        // Android may still be inflating the widget or showing its default/error view.
        // Keep the preview until the provider's content is actually drawn.
        val hasContent = renderedContent() != null
        if (hasContent) {
            super.dispatchDraw(canvas)
            snapshotCapture?.onDraw()
        }
        if (hasContent != reportedContent) {
            removeCallbacks(reportDrawnContent)
            post(reportDrawnContent)
        }
    }

    fun cancelPendingLongPress() {
        longPressArmed = false
        removeCallbacks(longPressRunnable)
    }

    fun releaseCallbacks() {
        removeCallbacks(longPressRunnable)
        removeCallbacks(reportDrawnContent)
        stopSnapshotCapture()
        if (isPanelHost && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setExecutor(null)
        onLongPress = null
        isLongPressEnabled = { false }
        onDragMove = null
        onDragEnd = null
        onTouchStarted = null
        onContentChanged = null
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

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        if (!isPanelHost) onContentChanged?.invoke(remoteViews != null)
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
            if (!isLongPressEnabled()) return@Runnable
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
                if (isLongPressEnabled() && !isScrollInProgressProvider()) {
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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        longPressFired || (isPanelHost && renderedContent() == null)

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
        removeCallbacks(reportDrawnContent)
        snapshotCapture?.cancel()
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
