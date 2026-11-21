package com.tk.quicksearch.search.glance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Maneuver arrows drawn as one filled path (the widget rasterizer ignores strokes): a straight
 * stem, a rounded bend and an arrowhead, so turns read as a smooth road curve instead of the
 * square corner of the stock turn symbols.
 */
internal object NavigationArrows {
    val TurnLeft by lazy { arrow("NavTurnLeft", left = true, turnDegrees = 90f, stem = 300f, radius = 160f, tail = 120f, x0 = 285f, y0 = 765f) }
    val TurnRight by lazy { arrow("NavTurnRight", left = false, turnDegrees = 90f, stem = 300f, radius = 160f, tail = 120f, x0 = 285f, y0 = 765f) }
    val SlightLeft by lazy { arrow("NavSlightLeft", left = true, turnDegrees = 45f, stem = 320f, radius = 320f, tail = 100f, x0 = 340f, y0 = 840f) }
    val SlightRight by lazy { arrow("NavSlightRight", left = false, turnDegrees = 45f, stem = 320f, radius = 320f, tail = 100f, x0 = 340f, y0 = 840f) }
    val SharpLeft by lazy { arrow("NavSharpLeft", left = true, turnDegrees = 135f, stem = 260f, radius = 130f, tail = 90f, x0 = 304f, y0 = 695f) }
    val SharpRight by lazy { arrow("NavSharpRight", left = false, turnDegrees = 135f, stem = 260f, radius = 130f, tail = 90f, x0 = 304f, y0 = 695f) }
    val UTurnLeft by lazy { arrow("NavUTurnLeft", left = true, turnDegrees = 180f, stem = 380f, radius = 130f, tail = 200f, x0 = 350f, y0 = 800f) }
    val UTurnRight by lazy { arrow("NavUTurnRight", left = false, turnDegrees = 180f, stem = 380f, radius = 130f, tail = 200f, x0 = 350f, y0 = 800f) }

    private const val HALF_WIDTH = 40f
    private const val HEAD_LENGTH = 150f
    private const val HEAD_HALF_WIDTH = 110f

    /**
     * Builds an arrow that starts at the bottom heading up, goes [stem] straight, bends [turnDegrees]
     * along an arc of [radius] and runs [tail] more before the head. [x0]/[y0] place the start of the
     * stem for a right turn; a left turn mirrors x around the viewport centre.
     */
    private fun arrow(
        name: String,
        left: Boolean,
        turnDegrees: Float,
        stem: Float,
        radius: Float,
        tail: Float,
        x0: Float,
        y0: Float,
    ): ImageVector {
        val side = if (left) -1f else 1f
        val startX = if (left) 960f - x0 else x0
        val turn = Math.toRadians((side * turnDegrees).toDouble())
        // Heading (0 = up, clockwise positive) as a direction and its right-hand normal.
        fun dirX(a: Double) = sin(a).toFloat()
        fun dirY(a: Double) = -cos(a).toFloat()
        fun nX(a: Double) = cos(a).toFloat()
        fun nY(a: Double) = sin(a).toFloat()

        val p1x = startX
        val p1y = y0 - stem
        // The arc's centre is on the inside of the bend, which is the turn's side.
        val cx = p1x + side * nX(0.0) * radius
        val cy = p1y + side * nY(0.0) * radius
        val p2x = cx - side * nX(turn) * radius
        val p2y = cy - side * nY(turn) * radius
        val p3x = p2x + dirX(turn) * tail
        val p3y = p2y + dirY(turn) * tail
        val tipX = p3x + dirX(turn) * HEAD_LENGTH
        val tipY = p3y + dirY(turn) * HEAD_LENGTH
        val nx = nX(turn)
        val ny = nY(turn)
        val clockwise = !left

        return ImageVector
            .Builder(name = "NavigationArrows.$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 960f, viewportHeight = 960f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    // Outer edge, then the head, then back along the inner edge.
                    moveTo(startX - side * HALF_WIDTH, y0)
                    lineTo(p1x - side * HALF_WIDTH, p1y)
                    arcTo(radius + HALF_WIDTH, radius + HALF_WIDTH, 0f, false, clockwise, p2x - side * nx * HALF_WIDTH, p2y - side * ny * HALF_WIDTH)
                    lineTo(p3x - side * nx * HALF_WIDTH, p3y - side * ny * HALF_WIDTH)
                    lineTo(p3x - side * nx * HEAD_HALF_WIDTH, p3y - side * ny * HEAD_HALF_WIDTH)
                    lineTo(tipX, tipY)
                    lineTo(p3x + side * nx * HEAD_HALF_WIDTH, p3y + side * ny * HEAD_HALF_WIDTH)
                    lineTo(p3x + side * nx * HALF_WIDTH, p3y + side * ny * HALF_WIDTH)
                    lineTo(p2x + side * nx * HALF_WIDTH, p2y + side * ny * HALF_WIDTH)
                    arcTo(radius - HALF_WIDTH, radius - HALF_WIDTH, 0f, false, !clockwise, p1x + side * HALF_WIDTH, p1y)
                    lineTo(startX + side * HALF_WIDTH, y0)
                    close()
                }
            }.build()
    }
}
