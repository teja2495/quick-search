package com.tk.quicksearch.widgets.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object WidgetBitmapUtils {
    private const val SHAPE_CACHE_MAX_BYTES = 8 * 1024 * 1024

    private data class ShapeKey(
        val widthPx: Int,
        val heightPx: Int,
        val backgroundArgb: Int,
        val borderArgb: Int?,
        val borderWidthPx: Int,
        val cornerRadiusPx: Float,
    )

    /**
     * Widgets redraw the same surface on every update (a media widget several times per play/pause),
     * so identical shapes are drawn once. The bitmaps are only ever read after drawing.
     */
    private val shapeCache =
        object : LruCache<ShapeKey, Bitmap>(SHAPE_CACHE_MAX_BYTES) {
            override fun sizeOf(
                key: ShapeKey,
                value: Bitmap,
            ): Int = value.allocationByteCount
        }

    /** Returns a shared, read-only bitmap: callers must not draw on or recycle it. */
    fun createWidgetBitmap(
        widthPx: Int,
        heightPx: Int,
        backgroundColor: Color,
        borderColor: Color?,
        borderWidthPx: Int,
        cornerRadiusPx: Float,
    ): Bitmap {
        val key =
            ShapeKey(
                widthPx = widthPx,
                heightPx = heightPx,
                backgroundArgb = backgroundColor.toArgb(),
                borderArgb = borderColor?.toArgb(),
                borderWidthPx = borderWidthPx,
                cornerRadiusPx = cornerRadiusPx,
            )
        shapeCache.get(key)?.takeUnless { it.isRecycled }?.let { return it }
        return drawWidgetBitmap(widthPx, heightPx, backgroundColor, borderColor, borderWidthPx, cornerRadiusPx)
            .also { shapeCache.put(key, it) }
    }

    private fun drawWidgetBitmap(
        widthPx: Int,
        heightPx: Int,
        backgroundColor: Color,
        borderColor: Color?,
        borderWidthPx: Int,
        cornerRadiusPx: Float,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        drawBackground(canvas, widthPx, heightPx, backgroundColor, cornerRadiusPx)

        // Draw border if needed
        if (borderWidthPx > 0 && borderColor != null) {
            drawBorder(canvas, widthPx, heightPx, borderColor, borderWidthPx, cornerRadiusPx)
        }

        return bitmap
    }

    private fun drawBackground(
        canvas: Canvas,
        widthPx: Int,
        heightPx: Int,
        backgroundColor: Color,
        cornerRadiusPx: Float,
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = backgroundColor.toArgb()
            }
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

        if (cornerRadiusPx > 0f) {
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        } else {
            canvas.drawRect(rect, paint)
        }
    }

    private fun drawBorder(
        canvas: Canvas,
        widthPx: Int,
        heightPx: Int,
        borderColor: Color,
        borderWidthPx: Int,
        cornerRadiusPx: Float,
    ) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidthPx.toFloat()
                color = borderColor.toArgb()
            }
        val inset = borderWidthPx / 2f
        val rect = RectF(inset, inset, widthPx - inset, heightPx - inset)

        if (cornerRadiusPx > 0f) {
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        } else {
            canvas.drawRect(rect, paint)
        }
    }
}
