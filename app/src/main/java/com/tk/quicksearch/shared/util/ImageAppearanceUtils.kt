package com.tk.quicksearch.shared.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlin.math.roundToInt

data class ImageAppearance(
    val isDark: Boolean,
    val accentColorArgb: Int,
)

object ImageAppearanceUtils {
    private const val ANALYSIS_SAMPLE_DIMENSION = 64
    private const val ANALYSIS_HUE_BUCKETS = 36
    private const val DEFAULT_ACCENT_COLOR_ARGB = -10011996 // #6750A4

    fun fromImageBitmap(bitmap: ImageBitmap): ImageAppearance? =
        runCatching {
            analyze(bitmap.asAndroidBitmap())
        }.getOrNull()

    fun analyze(bitmap: Bitmap): ImageAppearance {
        val sampled = sampleForAnalysis(bitmap)
        val width = sampled.width
        val height = sampled.height
        if (width <= 0 || height <= 0) {
            if (sampled !== bitmap) sampled.recycle()
            return ImageAppearance(isDark = true, accentColorArgb = DEFAULT_ACCENT_COLOR_ARGB)
        }

        return try {
            val pixels = IntArray(width * height)
            sampled.getPixels(pixels, 0, width, 0, 0, width, height)

            val hueWeights = FloatArray(ANALYSIS_HUE_BUCKETS)
            val hueR = FloatArray(ANALYSIS_HUE_BUCKETS)
            val hueG = FloatArray(ANALYSIS_HUE_BUCKETS)
            val hueB = FloatArray(ANALYSIS_HUE_BUCKETS)

            var baseWeightSum = 0f
            var luminanceSum = 0f
            var avgR = 0f
            var avgG = 0f
            var avgB = 0f
            val hsv = FloatArray(3)

            pixels.forEach { pixel ->
                val alpha = Color.alpha(pixel) / 255f
                if (alpha < 0.04f) return@forEach

                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                val weight = alpha

                val luminance = (0.299f * r) + (0.587f * g) + (0.114f * b)
                baseWeightSum += weight
                luminanceSum += luminance * weight
                avgR += r * weight
                avgG += g * weight
                avgB += b * weight

                Color.colorToHSV(pixel, hsv)
                val saturation = hsv[1]
                val value = hsv[2]
                val chromaWeight = weight * saturation * (0.35f + (0.65f * value))
                if (chromaWeight <= 0.001f) return@forEach

                val hueBucket =
                    ((hsv[0] / 360f) * ANALYSIS_HUE_BUCKETS)
                        .toInt()
                        .coerceIn(0, ANALYSIS_HUE_BUCKETS - 1)
                hueWeights[hueBucket] += chromaWeight
                hueR[hueBucket] += r * chromaWeight
                hueG[hueBucket] += g * chromaWeight
                hueB[hueBucket] += b * chromaWeight
            }

            if (baseWeightSum <= 0f) {
                return ImageAppearance(isDark = true, accentColorArgb = DEFAULT_ACCENT_COLOR_ARGB)
            }

            val isDark = (luminanceSum / baseWeightSum) < 0.5f
            var accentR = avgR / baseWeightSum
            var accentG = avgG / baseWeightSum
            var accentB = avgB / baseWeightSum

            val dominantBucket = hueWeights.indices.maxByOrNull { hueWeights[it] } ?: -1
            if (dominantBucket >= 0 && hueWeights[dominantBucket] > baseWeightSum * 0.015f) {
                val bucketWeight = hueWeights[dominantBucket]
                accentR = hueR[dominantBucket] / bucketWeight
                accentG = hueG[dominantBucket] / bucketWeight
                accentB = hueB[dominantBucket] / bucketWeight
            }

            ImageAppearance(
                isDark = isDark,
                accentColorArgb = normalizeAccentColor(accentR, accentG, accentB, isDark),
            )
        } finally {
            if (sampled !== bitmap) {
                sampled.recycle()
            }
        }
    }

    private fun sampleForAnalysis(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap
        val maxDimension = maxOf(width, height)
        if (maxDimension <= ANALYSIS_SAMPLE_DIMENSION) return bitmap
        val scale = ANALYSIS_SAMPLE_DIMENSION.toFloat() / maxDimension.toFloat()
        val sampledWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val sampledHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, sampledWidth, sampledHeight, true)
    }

    private fun normalizeAccentColor(
        red: Float,
        green: Float,
        blue: Float,
        isDarkBackground: Boolean,
    ): Int {
        val redInt = (red * 255f).roundToInt().coerceIn(0, 255)
        val greenInt = (green * 255f).roundToInt().coerceIn(0, 255)
        val blueInt = (blue * 255f).roundToInt().coerceIn(0, 255)
        val color = Color.rgb(redInt, greenInt, blueInt)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        hsv[1] = hsv[1].coerceIn(0.24f, 0.9f)
        hsv[2] =
            if (isDarkBackground) {
                hsv[2].coerceIn(0.48f, 0.9f)
            } else {
                hsv[2].coerceIn(0.34f, 0.82f)
            }
        return Color.HSVToColor(hsv)
    }
}
