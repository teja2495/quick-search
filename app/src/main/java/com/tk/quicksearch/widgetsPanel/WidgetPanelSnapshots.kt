package com.tk.quicksearch.widgetsPanel

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.AtomicFile
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Last rendered panel widget previews, stored in the app's private, evictable cache. */
internal object WidgetPanelSnapshots {
    private const val MAX_SNAPSHOT_EDGE_PX = 768
    private const val SNAPSHOT_INTERVAL_MS = 5_000L
    private val disk = Dispatchers.IO.limitedParallelism(1)
    private val writes = CoroutineScope(SupervisorJob() + disk)
    private val captures = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    suspend fun read(context: Context, id: Int, provider: ComponentName): Bitmap? =
        withContext(disk) {
            val file = snapshotFile(context, id, provider)
            runCatching { file.openRead().use { BitmapFactory.decodeStream(it) } }.getOrNull()
        }

    fun remove(context: Context, id: Int) {
        writes.launch { widgetDirectory(context, id).deleteRecursively() }
    }

    private fun widgetDirectory(context: Context, id: Int): File =
        File(context.cacheDir, "widget-panel/$id")

    private fun snapshotFile(
        context: Context,
        id: Int,
        provider: ComponentName,
        suffix: String = "-v2",
    ): AtomicFile =
        AtomicFile(File(widgetDirectory(context, id), "${provider.flattenToString().hashCode()}$suffix.png"))

    private fun save(context: Context, id: Int, provider: ComponentName, bitmap: Bitmap) {
        writes.launch {
            try {
                val file = snapshotFile(context, id, provider)
                val stream = file.startWrite()
                try {
                    if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        file.finishWrite(stream)
                        snapshotFile(context, id, provider, suffix = "").delete()
                    } else {
                        file.failWrite(stream)
                    }
                } catch (_: Exception) {
                    file.failWrite(stream)
                }
            } catch (_: IOException) {
                // Cache eviction or a full disk must not affect the live widget.
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** The view is drawn on main; compression and writes run off main. */
    class Capture(
        private val host: View,
        private val hasContent: () -> Boolean,
        id: Int,
        provider: ComponentName,
        private val graphicsContext: GraphicsContext,
    ) {
        private val context = host.context.applicationContext
        private var nextCaptureAt = 0L
        private var scheduled = false
        private var captureJob: Job? = null
        private val capture = Runnable {
            scheduled = false
            captureJob = captures.launch {
                if (!host.isAttachedToWindow || !hasContent() || host.width <= 0 || host.height <= 0) {
                    return@launch
                }
                nextCaptureAt = SystemClock.uptimeMillis() + SNAPSHOT_INTERVAL_MS
                val bitmap = render() ?: return@launch
                save(context, id, provider, bitmap)
            }
        }

        private suspend fun render(): Bitmap? {
            val scale = minOf(1f, MAX_SNAPSHOT_EDGE_PX.toFloat() / maxOf(host.width, host.height))
            val size = IntSize(
                (host.width * scale).roundToInt().coerceAtLeast(1),
                (host.height * scale).roundToInt().coerceAtLeast(1),
            )
            val layoutDirection =
                if (host.layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
            val layer = graphicsContext.createGraphicsLayer()
            return try {
                // Draw from the host into a hardware layer so the provider's child views
                // keep their outline clips and transforms in the cached preview.
                layer.record(Density(host.resources.displayMetrics.density), layoutDirection, size) {
                    scale(scale, scale, pivot = Offset.Zero) {
                        drawIntoCanvas { host.draw(it.nativeCanvas) }
                    }
                }
                layer.toImageBitmap().asAndroidBitmap()
            } catch (error: CancellationException) {
                throw error
            } catch (_: RuntimeException) {
                null
            } finally {
                graphicsContext.releaseGraphicsLayer(layer)
            }
        }

        fun onDraw() {
            if (scheduled || captureJob?.isActive == true) return
            scheduled = host.postDelayed(capture, (nextCaptureAt - SystemClock.uptimeMillis()).coerceAtLeast(0L))
        }

        fun cancel() {
            host.removeCallbacks(capture)
            scheduled = false
            captureJob?.cancel()
            captureJob = null
        }
    }
}
