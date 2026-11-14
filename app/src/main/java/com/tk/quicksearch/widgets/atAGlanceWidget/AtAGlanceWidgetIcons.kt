package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import com.tk.quicksearch.search.apps.appIconCacheVersion
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.customButtonsWidget.loadAppIconBitmap
import kotlin.math.roundToInt

/** Every row icon is parceled into the widget's RemoteViews, so icons stay small. */
private const val MAX_ICON_PX = 64

/**
 * Bitmaps for row icons. Glance can't draw a Compose [ImageVector], so Material icons are drawn
 * here as alpha masks that the widget tints. Both kinds are cached for the process, and a cached
 * bitmap reused across rows is sent once.
 */
internal object AtAGlanceWidgetIcons {
    private val vectorBitmaps = mutableMapOf<String, Bitmap>()
    private val appBitmaps = mutableMapOf<String, Bitmap?>()
    private var appBitmapsVersion = appIconCacheVersion.value

    fun iconSizePx(context: Context): Int =
        (AtAGlanceWidgetDimens.ICON_SIZE.value * context.resources.displayMetrics.density)
            .roundToInt()
            .coerceIn(1, MAX_ICON_PX)

    @Synchronized
    fun vector(
        context: Context,
        vector: ImageVector,
    ): Bitmap {
        val sizePx = iconSizePx(context)
        return vectorBitmaps.getOrPut("${vector.name}@$sizePx") { renderVector(vector, sizePx) }
    }

    /**
     * The app's icon as the app draws it (per-app override, then icon pack), or null when the app
     * is gone. Cached until the app's icons change.
     */
    @Synchronized
    fun app(
        context: Context,
        packageName: String,
    ): Bitmap? {
        val sizePx = iconSizePx(context)
        val version = appIconCacheVersion.value
        if (version != appBitmapsVersion) {
            appBitmaps.clear()
            appBitmapsVersion = version
        }
        val iconPackPackage = UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        return appBitmaps.getOrPut("$packageName@$sizePx@$iconPackPackage") {
            runCatching { loadAppIconBitmap(context, packageName, sizePx, iconPackPackage) }.getOrNull()
        }
    }

    fun fallbackAppIcon(context: Context): Bitmap = vector(context, MaterialSymbols.Apps)

    /** Home's low battery outline and nub, as a mask. */
    @Synchronized
    fun batteryOutline(context: Context): Bitmap {
        val sizePx = iconSizePx(context)
        return vectorBitmaps.getOrPut("battery-outline@$sizePx") {
            renderBattery(sizePx, context.resources.displayMetrics.density, fillPercent = null)
        }
    }

    /** Home's low battery fill for [percent], as a mask. */
    @Synchronized
    fun batteryFill(
        context: Context,
        percent: Int,
    ): Bitmap {
        val sizePx = iconSizePx(context)
        return vectorBitmaps.getOrPut("battery-fill-$percent@$sizePx") {
            renderBattery(sizePx, context.resources.displayMetrics.density, fillPercent = percent)
        }
    }

    /** The same shapes as home's BatteryLevelIcon: the outline when [fillPercent] is null, else the fill. */
    private fun renderBattery(
        sizePx: Int,
        density: Float,
        fillPercent: Int?,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        val size = sizePx.toFloat()
        val stroke = 1.5f * density
        val nubWidth = 2f * density
        val bodyWidth = size - nubWidth - density
        val bodyHeight = size * 0.5f
        val top = (size - bodyHeight) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (fillPercent == null) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            val radius = 3f * density
            canvas.drawRoundRect(stroke / 2f, top, bodyWidth - stroke / 2f, top + bodyHeight, radius, radius, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                bodyWidth,
                top + bodyHeight * 0.3f,
                bodyWidth + nubWidth,
                top + bodyHeight * 0.7f,
                density,
                density,
                paint,
            )
        } else {
            val inset = stroke + 1.5f * density
            // Keep a sliver visible at 0-1% so the icon never reads as an empty outline.
            val fillWidth = (bodyWidth - inset * 2) * (fillPercent / 100f).coerceIn(0.08f, 1f)
            val radius = minOf(1.5f * density, fillWidth / 2f)
            canvas.drawRoundRect(inset, top + inset, inset + fillWidth, top + bodyHeight - inset, radius, radius, paint)
        }
        return bitmap
    }

    private fun renderVector(
        vector: ImageVector,
        sizePx: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)
        canvas.concat(
            Matrix().apply { setScale(sizePx / vector.viewportWidth, sizePx / vector.viewportHeight) },
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        drawNode(canvas, vector.root, paint)
        return bitmap
    }

    /**
     * Material icons are filled paths, so strokes and clip paths are not drawn. Group transforms
     * are, since Material Symbols shift their paths into the viewport with a group translation.
     */
    private fun drawNode(
        canvas: Canvas,
        node: VectorNode,
        paint: Paint,
    ) {
        when (node) {
            is VectorGroup -> {
                canvas.save()
                // The same order as Compose's VectorGroup: translate, rotate and scale around the pivot.
                canvas.translate(node.translationX + node.pivotX, node.translationY + node.pivotY)
                canvas.rotate(node.rotation)
                canvas.scale(node.scaleX, node.scaleY)
                canvas.translate(-node.pivotX, -node.pivotY)
                node.forEach { drawNode(canvas, it, paint) }
                canvas.restore()
            }
            is VectorPath -> {
                val path = PathParser().addPathNodes(node.pathData).toPath()
                path.fillType = node.pathFillType
                paint.alpha = (node.fillAlpha * 255).roundToInt()
                canvas.drawPath(path.asAndroidPath(), paint)
            }
        }
    }
}
