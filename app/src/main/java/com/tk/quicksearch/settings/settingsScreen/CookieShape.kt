package com.tk.quicksearch.settings.settingsScreen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Scalloped "cookie" outline with [lobes] soft bumps, in the spirit of the Material 3 Expressive
 * shape set (not available in the material3 version this app uses).
 *
 * [depth] is how far each valley dips toward the center, as a fraction of the radius.
 */
internal class CookieShape(
    private val lobes: Int,
    private val depth: Float = 0.12f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = min(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()
        for (i in 0..SAMPLES) {
            val theta = 2.0 * PI * i / SAMPLES - PI / 2
            // Cosine bumps keep both peaks and valleys rounded.
            val r = radius * (1f - depth * (0.5f - 0.5f * cos(lobes * theta).toFloat()))
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        const val SAMPLES = 240
    }
}

/**
 * Renders [icon] unmasked so it can be clipped to a circle. Adaptive icons draw their full-bleed
 * background and foreground layers, scaled so the visible 72/108 center fills the bitmap; legacy
 * icons are drawn as-is.
 */
internal fun circularAppIconBitmap(
    icon: Drawable,
    sizePx: Int = 192,
): Bitmap {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || icon !is AdaptiveIconDrawable) {
        return icon.toBitmap(sizePx, sizePx)
    }
    val layers = LayerDrawable(arrayOf(icon.background, icon.foreground).filterNotNull().toTypedArray())
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val inset = sizePx / 4 // (108 - 72) / 2 / 72 of the visible size
    layers.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
    layers.draw(Canvas(bitmap))
    return bitmap
}
