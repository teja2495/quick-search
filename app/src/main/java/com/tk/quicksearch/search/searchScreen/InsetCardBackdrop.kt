package com.tk.quicksearch.search.searchScreen

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Blur applied to results showing through the bottom chrome surfaces. */
private val InsetCardBackdropBlurRadius = 20.dp

/** Surface opacity cap while the blurred backdrop is drawn, so the results stay faintly visible. */
private const val InsetCardBlurredBackgroundMaxAlpha = 0.72f

/** One bottom chrome surface (search bar, engine strip, Open Keyboard) the results show through. */
@Stable
internal class InsetCardBackdropTarget {
    var boundsInRoot by mutableStateOf<Rect?>(null)
    var shape by mutableStateOf<Shape?>(null)
}

/**
 * Lets the search results scroll beneath the surfaces pinned at the bottom of the search screen and
 * shows them frosted through those surfaces. The results area reaches down to the lowest surface's
 * bottom edge, never past it, and each surface draws a blurred copy of the results behind its own
 * background while the sharp copy is kept out of every surface's shape.
 */
@Stable
internal class InsetCardBackdrop {
    internal val targets = mutableStateListOf<InsetCardBackdropTarget>()
    private var contentSlotOriginInRoot by mutableStateOf(Offset.Zero)
    private var contentSlotBottomInRoot by mutableStateOf(0f)
    internal var contentLayer: GraphicsLayer? = null

    /** Surfaces above the results (top search bar) versus below them (bottom chrome). */
    private fun Rect.isAboveContent(): Boolean =
        center.y < (contentSlotOriginInRoot.y + contentSlotBottomInRoot) / 2f

    /** How far the results area extends below its own slot to reach the lowest surface. */
    internal val extensionPx: Int by derivedStateOf {
        val lowestBottom =
            targets.mapNotNull { it.boundsInRoot }.filterNot { it.isAboveContent() }
                .maxOfOrNull { it.bottom } ?: return@derivedStateOf 0
        (lowestBottom - contentSlotBottomInRoot).toInt().coerceAtLeast(0)
    }

    /** The surface the results extend up beneath (the top search bar), if any. */
    internal val topTarget: InsetCardBackdropTarget? by derivedStateOf {
        targets.filter { it.boundsInRoot?.isAboveContent() == true }.minByOrNull { it.boundsInRoot!!.top }
    }

    /** How far the results area extends above its own slot to reach the top search bar's top. */
    internal val topExtensionPx: Int by derivedStateOf {
        val highestTop = topTarget?.boundsInRoot?.top ?: return@derivedStateOf 0
        (contentSlotOriginInRoot.y - highestTop).toInt().coerceAtLeast(0)
    }

    /** Root position of the extended results area. */
    internal val contentOriginInRoot: Offset
        get() = contentSlotOriginInRoot - Offset(0f, topExtensionPx.toFloat())

    internal fun updateContentSlot(origin: Offset, height: Int) {
        contentSlotOriginInRoot = origin
        contentSlotBottomInRoot = origin.y + height
    }

    internal val blurSupported: Boolean
        get() = BackdropBlurSupported
}

/**
 * Blur needs RenderEffect (Android 12). Older devices skip the whole backdrop: nothing registers,
 * the results keep their original bounds, and no extra layer or clip runs, so they cost the same
 * as before the effect existed.
 */
private val BackdropBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

internal val LocalInsetCardBackdrop = staticCompositionLocalOf<InsetCardBackdrop?> { null }

/**
 * Only surfaces in the bottom chrome register as backdrop targets, so engine rows rendered inline in
 * the results stay untouched. The main search bar opts in wherever it sits.
 */
internal val LocalInsetCardBackdropTargetsEnabled = staticCompositionLocalOf { false }

@Composable
internal fun rememberInsetCardBackdrop(): InsetCardBackdrop = remember { InsetCardBackdrop() }

/** Bottom inset the results need so their last item can rest above the bottom chrome. */
@Composable
internal fun InsetCardBackdrop.contentBottomInset(): Dp =
    with(LocalDensity.current) { extensionPx.toDp() }

/** Top inset the results need so their first item starts below the top search bar. */
@Composable
internal fun InsetCardBackdrop.contentTopInset(): Dp =
    with(LocalDensity.current) { topExtensionPx.toDp() }

/**
 * Clip for the results while they extend up beneath the top search bar: the bar's own outline down
 * to where the results normally start, full width below. Nothing peeks out beside the bar's corners
 * or edges, and it follows the user's search bar corner radius. [default] otherwise.
 */
@Composable
internal fun InsetCardBackdrop?.resultsClipShape(default: Shape, edgeBleed: Dp): Shape {
    val target = this?.topTarget ?: return default
    val bounds = target.boundsInRoot ?: return default
    val barShape = target.shape ?: return default
    val bleedPx = with(LocalDensity.current) { edgeBleed.toPx() }
    val origin = contentOriginInRoot
    val barRect = bounds.translate(bleedPx - origin.x, -origin.y)
    val resultsTop = topExtensionPx.toFloat()
    return remember(barRect, barShape, resultsTop) { TopBarMatchingShape(barRect, barShape, resultsTop) }
}

private data class TopBarMatchingShape(
    val barRect: Rect,
    val barShape: Shape,
    val resultsTop: Float,
) : Shape {
    // The results resize every frame while the keyboard animates; reuse the outline per size.
    private var cachedSize: androidx.compose.ui.geometry.Size? = null
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        cachedOutline?.let { if (cachedSize == size) return it }
        return buildOutline(size, layoutDirection, density).also {
            cachedSize = size
            cachedOutline = it
        }
    }

    private fun buildOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        val barPath = Path().apply { addOutline(barShape.createOutline(barRect.size, layoutDirection, density)) }
        val path = Path()
        path.addPath(barPath, barRect.topLeft)
        val stemTop = barRect.center.y
        path.addRect(Rect(barRect.left, stemTop, barRect.right, resultsTop.coerceAtLeast(stemTop)))
        path.addRect(Rect(0f, resultsTop, size.width, size.height.coerceAtLeast(resultsTop)))
        return Outline.Generic(path)
    }
}

/**
 * Applied to the results area. Extends it down behind the bottom chrome, records it so the chrome
 * surfaces can draw it blurred, and keeps the sharp copy out of their shapes.
 */
@Composable
internal fun Modifier.insetCardBackdropSource(backdrop: InsetCardBackdrop): Modifier {
    if (!backdrop.blurSupported) return this
    val layer = rememberGraphicsLayer()
    DisposableEffect(backdrop, layer) {
        backdrop.contentLayer = layer
        onDispose { if (backdrop.contentLayer === layer) backdrop.contentLayer = null }
    }
    return this
        .onGloballyPositioned { coordinates ->
            backdrop.updateContentSlot(coordinates.positionInRoot(), coordinates.size.height)
        }
        .layout { measurable, constraints ->
            val bounded = constraints.hasBoundedHeight
            val top = if (bounded) backdrop.topExtensionPx else 0
            val extension = (if (bounded) backdrop.extensionPx else 0) + top
            val placeable =
                measurable.measure(
                    constraints.copy(
                        minHeight = constraints.minHeight + extension,
                        maxHeight = constraints.maxHeight + extension,
                    ),
                )
            layout(placeable.width, (placeable.height - extension).coerceAtLeast(0)) {
                placeable.place(0, -top)
            }
        }
        .drawWithCache {
            // Rebuilt only when a surface moves or changes shape, not on every frame.
            val cutout = Path()
            backdrop.targets.forEach { target ->
                val bounds = target.boundsInRoot ?: return@forEach
                val shape = target.shape ?: return@forEach
                val outline = Path().apply { addOutline(shape.createOutline(bounds.size, layoutDirection, this@drawWithCache)) }
                cutout.addPath(outline, bounds.topLeft - backdrop.contentOriginInRoot)
            }
            val hasCutout = !cutout.isEmpty
            onDrawWithContent {
                if (!hasCutout) {
                    drawContent()
                    return@onDrawWithContent
                }
                layer.record { this@onDrawWithContent.drawContent() }
                clipPath(cutout, clipOp = ClipOp.Difference) { drawLayer(layer) }
            }
        }
}

/**
 * Frosted results backdrop behind a pinned surface, when the search screen provides one. Pass
 * [anywhere] for the main search bar, which is a backdrop surface at the top as well as the bottom.
 */
@Composable
internal fun Modifier.insetCardBackdrop(enabled: Boolean, shape: Shape, anywhere: Boolean = false): Modifier {
    val backdrop = LocalInsetCardBackdrop.current
    val active =
        enabled && backdrop != null && backdrop.blurSupported &&
            (anywhere || LocalInsetCardBackdropTargetsEnabled.current)
    return if (active) insetCardBackdropTarget(backdrop, shape) else this
}

/** Surface colour, made translucent while it draws the blurred results behind it. */
@Composable
internal fun insetCardBackground(enabled: Boolean, color: Color, anywhere: Boolean = false): Color {
    val backdrop = LocalInsetCardBackdrop.current ?: return color
    val active =
        enabled && backdrop.blurSupported && (anywhere || LocalInsetCardBackdropTargetsEnabled.current)
    return if (active) {
        color.copy(alpha = color.alpha.coerceAtMost(InsetCardBlurredBackgroundMaxAlpha))
    } else {
        color
    }
}

@Composable
private fun Modifier.insetCardBackdropTarget(
    backdrop: InsetCardBackdrop,
    shape: Shape,
): Modifier {
    val target = remember { InsetCardBackdropTarget() }
    val blurLayer = rememberGraphicsLayer()
    DisposableEffect(backdrop, target) {
        backdrop.targets.add(target)
        onDispose { backdrop.targets.remove(target) }
    }
    SideEffect { target.shape = shape }
    return this
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            target.boundsInRoot =
                Rect(
                    position,
                    androidx.compose.ui.geometry.Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat(),
                    ),
                )
        }
        .drawWithCache {
            // Blur only this surface's own area (plus the blur radius so its edges sample real
            // pixels), not the whole results layer: blur cost scales with the blurred area.
            val radius = InsetCardBackdropBlurRadius.toPx()
            val margin = kotlin.math.ceil(radius).toInt()
            blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            val outline = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            val regionSize = IntSize(size.width.toInt() + margin * 2, size.height.toInt() + margin * 2)
            val bounds = target.boundsInRoot
            val offset = bounds?.let { backdrop.contentOriginInRoot - it.topLeft }
            onDrawBehind {
                val contentLayer = backdrop.contentLayer ?: return@onDrawBehind
                if (offset == null || contentLayer.size == IntSize.Zero) return@onDrawBehind
                blurLayer.record(size = regionSize) {
                    translate(left = offset.x + margin, top = offset.y + margin) { drawLayer(contentLayer) }
                }
                clipPath(outline) {
                    translate(left = -margin.toFloat(), top = -margin.toFloat()) { drawLayer(blurLayer) }
                }
            }
        }
}
