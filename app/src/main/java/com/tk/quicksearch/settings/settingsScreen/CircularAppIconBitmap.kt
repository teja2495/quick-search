package com.tk.quicksearch.settings.settingsScreen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

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
