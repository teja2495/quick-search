package com.tk.quicksearch.onboarding

import android.graphics.Matrix
import android.graphics.SweepGradient
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** One full turn of the glow around its shape. Not slowed by [WelcomeMorphSlowMotion]. */
private const val GlowPeriodMs = 2400L

/**
 * Glow angle shared by every surface that draws it, so the Start button, the travelling bar, and
 * the landed search bar continue one rotation instead of restarting.
 */
internal fun welcomeGlowAngle(): Float = (SystemClock.uptimeMillis() % GlowPeriodMs) * 360f / GlowPeriodMs

/** Frame clock that keeps a glow redrawing while it is composed. */
@Composable
internal fun rememberGlowFrameTick(): () -> Long {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { tick = it }
    }
    return { tick }
}

/** The search field palette without its white tail, closed into a loop for the sweep. */
private val GlowColors: IntArray by lazy {
    val colors = AppColors.SearchFieldGooglePalette.filter { it != Color.White }
    IntArray(colors.size + 1) { colors[it % colors.size].toArgb() }
}

/**
 * Reusable shader and stroke for one glowing surface, so drawing a frame allocates nothing: the
 * sweep is built once around the origin and only its rotation and position change.
 */
internal class WelcomeGlowPainter {
    private val matrix = Matrix()
    private val shader = SweepGradient(0f, 0f, GlowColors, null)
    val brush = ShaderBrush(shader)
    private var stroke = Stroke(0f)

    fun stroke(width: Float): Stroke {
        if (stroke.width != width) stroke = Stroke(width)
        return stroke
    }

    fun aim(center: Offset) {
        matrix.setRotate(welcomeGlowAngle())
        matrix.postTranslate(center.x, center.y)
        shader.setLocalMatrix(matrix)
    }
}

/**
 * Draws the rotating gradient as a single line just inside the rounded rect's edge, so it reads
 * as the shape's own border.
 */
internal fun DrawScope.drawWelcomeGlow(
    painter: WelcomeGlowPainter,
    topLeft: Offset,
    size: Size,
    cornerRadius: Float,
    alpha: Float,
) {
    if (alpha <= 0f || size.width <= 0f || size.height <= 0f) return
    val stroke = DesignTokens.SearchFieldBorderWidth.toPx()
    painter.aim(Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f))
    val inset = stroke / 2f
    drawRoundRect(
        brush = painter.brush,
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius((cornerRadius - inset).coerceAtLeast(0f)),
        alpha = alpha,
        style = painter.stroke(stroke),
    )
}

/** Rotating gradient border on a fully rounded element, such as the button that finishes setup. */
fun Modifier.welcomeGlow(): Modifier =
    composed {
        val tick = rememberGlowFrameTick()
        val painter = remember { WelcomeGlowPainter() }
        Modifier.drawWithContent {
            drawContent()
            tick()
            drawWelcomeGlow(painter, Offset.Zero, size, size.height / 2f, alpha = 1f)
        }
    }
