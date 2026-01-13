package com.tk.quicksearch.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object WidgetBitmapUtils {
    fun createWidgetBitmap(
        widthPx: Int,
        heightPx: Int,
        backgroundColor: Color,
        borderColor: Color?,
        borderWidthPx: Int,
        cornerRadiusPx: Float
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
        cornerRadiusPx: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        cornerRadiusPx: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
