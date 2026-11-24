package com.tk.quicksearch.search.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.preferences.UiPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import android.graphics.Color as AndroidColor

/**
 * Zoom factor when rasterizing adaptive icons for a forced mask. Keep this slightly above 1.0
 * so masked icons look full, but avoid excessive zoom that can clip edge glyphs on some apps.
 */
private const val MaskedAdaptiveIconContentScale = 1.42f

// Process-wide so every surface and the icon cache key agree. Null until first read from
// preferences or seeded by the startup snapshot (which also re-seeds it after a backup import).
private val appIconCornerPercentFlow = MutableStateFlow<Int?>(null)

/** Corner radius of masked app icons as a percent of the icon size; 50 is a circle. */
internal fun appIconCornerPercent(context: Context): Int =
    appIconCornerPercentFlow.value
        ?: UiPreferences.normalizeAppIconCornerPercent(iconPreferences(context).getAppIconCornerPercent())
            .also { appIconCornerPercentFlow.compareAndSet(null, it) }

/** Updates the corner radius used by masked app icons; icons re-render with the new mask. */
fun setAppIconCornerPercent(percent: Int) {
    appIconCornerPercentFlow.value = UiPreferences.normalizeAppIconCornerPercent(percent)
}

@Composable
fun rememberAppIconCornerPercent(): Int {
    val context = LocalContext.current
    val initial = remember(context) { appIconCornerPercent(context) }
    val percent by appIconCornerPercentFlow.collectAsState()
    return percent ?: initial
}

/** Compose clip shape matching the bitmap mask applied when the app icon shape is forced. */
@Composable
fun rememberAppIconMaskShape(): RoundedCornerShape {
    val percent = rememberAppIconCornerPercent()
    return remember(percent) { RoundedCornerShape(percent) }
}

private fun Canvas.drawMaskShape(
    size: Float,
    cornerPercent: Int,
    paint: Paint,
) {
    val radius = size * cornerPercent / 100f
    drawRoundRect(0f, 0f, size, size, radius, radius, paint)
}

internal fun Bitmap.maskCropped(cornerPercent: Int): Bitmap {
    val size = maxOf(width, height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawMaskShape(size.toFloat(), cornerPercent, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, (size - width) / 2f, (size - height) / 2f, paint)
    return output
}

/** Rasterizes [drawable]; with [maskCornerPercent] set, applies that rounded mask itself. */
@RequiresApi(Build.VERSION_CODES.O)
internal fun adaptiveToBitmap(
    drawable: AdaptiveIconDrawable,
    targetSize: Int,
    maskCornerPercent: Int?,
): Bitmap {
    if (maskCornerPercent == null) {
        return drawable.toBitmap(width = targetSize, height = targetSize)
    }

    val composed = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val composedCanvas = Canvas(composed)
    drawable.background?.setBounds(0, 0, targetSize, targetSize)
    drawable.background?.draw(composedCanvas)
    drawable.foreground?.setBounds(0, 0, targetSize, targetSize)
    drawable.foreground?.draw(composedCanvas)

    val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val outputCanvas = Canvas(output)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.WHITE
        }
    outputCanvas.drawMaskShape(targetSize.toFloat(), maskCornerPercent, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val center = targetSize / 2f
    val matrix =
        Matrix().apply {
            postScale(MaskedAdaptiveIconContentScale, MaskedAdaptiveIconContentScale, center, center)
        }
    outputCanvas.drawBitmap(composed, matrix, paint)
    composed.recycle()

    return output
}
