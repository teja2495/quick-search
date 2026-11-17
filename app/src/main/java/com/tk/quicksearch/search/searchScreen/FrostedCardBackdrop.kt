package com.tk.quicksearch.search.searchScreen

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.util.isLowRamDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Frost blur added on top of the user's wallpaper blur, so cards always read as frosted. */
private val FrostedCardExtraBlurRadius = 24.dp

/** Box radius of each pass on the downscaled bitmap; three passes approximate a Gaussian. */
private const val FROST_BOX_RADIUS = 3

/** Sigma of three box passes of [FROST_BOX_RADIUS]: sqrt(3 * ((2r + 1)^2 - 1) / 12). */
private const val FROST_SMALL_SIGMA = 3.46f

/** Converts a blur radius to its Gaussian sigma, the same way Android's RenderEffect does. */
private const val RADIUS_TO_SIGMA = 0.57735f

private const val FROST_MIN_WIDTH_PX = 16

/** A blurred, downscaled copy of the wallpaper plus the full image size it stands in for. */
@Immutable
internal class FrostedWallpaper(
    val image: ImageBitmap,
    val sourceSize: IntSize,
)

/**
 * The wallpaper behind the search screen, shared with result cards so each one can show a frosted
 * copy of the part of the wallpaper behind it. The wallpaper never moves, so it is blurred once
 * (when it or the blur setting changes) and each card only draws a region of that small bitmap,
 * which costs about the same as a plain image draw while scrolling.
 */
@Stable
internal class FrostedCardSource {
    var frost: FrostedWallpaper? by mutableStateOf(null)
    var tint: Color by mutableStateOf(Color.Transparent)
    var alpha: Float by mutableFloatStateOf(0f)

    /** The wallpaper image's layout; cards map their position into it. Not observed state. */
    internal var backgroundCoordinates: LayoutCoordinates? = null

    val isActive: Boolean
        get() = frost != null
}

internal val LocalFrostedCardSource = staticCompositionLocalOf<FrostedCardSource?> { null }

/** [ComponentActivity.setContent] with a [FrostedCardSource] shared by the whole window. */
internal fun ComponentActivity.setContentWithFrostedCards(content: @Composable () -> Unit) =
    setContent {
        CompositionLocalProvider(
            LocalFrostedCardSource provides remember { FrostedCardSource() },
            content = content,
        )
    }

/**
 * Android 12+ only, and off on low-RAM devices: they get the plain translucent card color, as
 * before. The wallpaper itself is only blurred on Android 12+ too.
 */
@Composable
private fun rememberFrostedCardsSupported(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isLowRamDevice(context)
    }
}

/**
 * Publishes the drawn wallpaper to [LocalFrostedCardSource]: bakes the blurred copy off the main
 * thread and clears it when the wallpaper layer goes away. Called by the search background only.
 */
@Composable
internal fun PublishFrostedWallpaper(
    wallpaperBitmap: ImageBitmap?,
    enabled: Boolean,
    wallpaperBlurRadiusDp: Float,
    tint: Color,
    alpha: Float,
) {
    val source = LocalFrostedCardSource.current ?: return
    val supported = rememberFrostedCardsSupported()
    val active = enabled && supported && wallpaperBitmap != null
    val density = LocalDensity.current
    val windowWidthPx = LocalWindowInfo.current.containerSize.width

    SideEffect {
        source.tint = tint
        source.alpha = alpha
    }

    val sigmaPx =
        with(density) {
            (wallpaperBlurRadiusDp.dp + FrostedCardExtraBlurRadius).toPx() * RADIUS_TO_SIGMA
        }
    LaunchedEffect(source, active, wallpaperBitmap, sigmaPx, windowWidthPx) {
        if (!active || windowWidthPx <= 0) {
            source.frost = null
            return@LaunchedEffect
        }
        // Width of the downscaled copy so FROST_SMALL_SIGMA there equals sigmaPx on screen.
        val displayedWidth = max(windowWidthPx, 1)
        val smallWidth =
            (displayedWidth * FROST_SMALL_SIGMA / sigmaPx).roundToInt().coerceAtLeast(FROST_MIN_WIDTH_PX)
        val baked =
            withContext(Dispatchers.Default) {
                runCatching { bakeFrostedWallpaper(wallpaperBitmap.asAndroidBitmap(), smallWidth) }
                    .getOrNull()
            }
        source.frost =
            baked?.let {
                FrostedWallpaper(
                    image = it.asImageBitmap(),
                    sourceSize = IntSize(wallpaperBitmap.width, wallpaperBitmap.height),
                )
            }
    }
    DisposableEffect(source) {
        onDispose {
            source.frost = null
            source.backgroundCoordinates = null
        }
    }
}

/** Downscales [source] to [targetWidth] (keeping its aspect ratio) and blurs it. */
private fun bakeFrostedWallpaper(
    source: Bitmap,
    targetWidth: Int,
): Bitmap {
    val readable =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }
    val width = targetWidth.coerceAtMost(readable.width)
    val height = (readable.height.toFloat() * width / readable.width).roundToInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(readable, width, height, true)
    val pixels = IntArray(width * height)
    small.getPixels(pixels, 0, width, 0, 0, width, height)
    if (small !== source) small.recycle()
    if (readable !== source && readable !== small) readable.recycle()
    repeat(3) { boxBlurArgb(pixels, width, height, FROST_BOX_RADIUS) }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

/** One horizontal then vertical box-blur pass over ARGB [pixels], clamping at the edges. */
internal fun boxBlurArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    if (radius <= 0 || width <= 0 || height <= 0) return
    val buffer = IntArray(max(width, height))
    for (y in 0 until height) blurLine(pixels, buffer, start = y * width, step = 1, length = width, radius = radius)
    for (x in 0 until width) blurLine(pixels, buffer, start = x, step = width, length = height, radius = radius)
}

private fun blurLine(
    pixels: IntArray,
    buffer: IntArray,
    start: Int,
    step: Int,
    length: Int,
    radius: Int,
) {
    val window = radius * 2 + 1
    var a = 0
    var r = 0
    var g = 0
    var b = 0
    fun at(i: Int): Int = pixels[start + i.coerceIn(0, length - 1) * step]
    for (i in -radius..radius) {
        val c = at(i)
        a += c ushr 24
        r += (c shr 16) and 0xFF
        g += (c shr 8) and 0xFF
        b += c and 0xFF
    }
    for (i in 0 until length) {
        buffer[i] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
        val out = at(i - radius)
        val inc = at(i + radius + 1)
        a += (inc ushr 24) - (out ushr 24)
        r += ((inc shr 16) and 0xFF) - ((out shr 16) and 0xFF)
        g += ((inc shr 8) and 0xFF) - ((out shr 8) and 0xFF)
        b += (inc and 0xFF) - (out and 0xFF)
    }
    for (i in 0 until length) pixels[start + i * step] = buffer[i]
}

/** Marks the wallpaper image whose layout the frosted cards line up with. */
internal fun Modifier.frostedWallpaperAnchor(source: FrostedCardSource?): Modifier =
    if (source == null) this else then(FrostedAnchorElement(source))

private data class FrostedAnchorElement(val source: FrostedCardSource) : ModifierNodeElement<FrostedAnchorNode>() {
    override fun create() = FrostedAnchorNode(source)

    override fun update(node: FrostedAnchorNode) {
        node.source = source
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "frostedWallpaperAnchor"
    }
}

private class FrostedAnchorNode(var source: FrostedCardSource) : Modifier.Node(), GlobalPositionAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        source.backgroundCoordinates = coordinates
    }

    override fun onDetach() {
        if (source.backgroundCoordinates?.isAttached == false) source.backgroundCoordinates = null
    }
}

/**
 * Draws the frosted wallpaper behind a card, clipped to [shape]. Draws nothing unless the search
 * background published a frosted wallpaper (Android 12+, not low-RAM, image background on).
 * Place it before the card's own background so the translucent container sits on top.
 */
@Composable
internal fun Modifier.frostedCardBackground(shape: Shape): Modifier =
    // Attached only while a frosted wallpaper exists: position tracking on every card caused
    // stale-frame ghosting with the transparent system wallpaper window (Samsung, Android 16).
    if (LocalFrostedCardSource.current?.isActive == true) then(FrostedCardElement(shape)) else this

private data class FrostedCardElement(val shape: Shape) : ModifierNodeElement<FrostedCardNode>() {
    override fun create() = FrostedCardNode(shape)

    override fun update(node: FrostedCardNode) {
        if (node.shape != shape) {
            node.shape = shape
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "frostedCardBackground"
    }
}

private class FrostedCardNode(var shape: Shape) :
    Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var lastOffset = Offset.Unspecified
    private val clip = Path()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // Scrolling moves the card without redrawing it; redraw so the frost stays in place.
        val offset = offsetInBackground(coordinates) ?: return
        if (offset != lastOffset) {
            lastOffset = offset
            invalidateDraw()
        }
    }

    private fun offsetInBackground(coordinates: LayoutCoordinates): Offset? {
        val background = currentValueOf(LocalFrostedCardSource)?.backgroundCoordinates ?: return null
        if (!background.isAttached || !coordinates.isAttached) return null
        return background.localPositionOf(coordinates, Offset.Zero)
    }

    override fun ContentDrawScope.draw() {
        val source = currentValueOf(LocalFrostedCardSource)
        val frost = source?.frost
        val alpha = source?.alpha ?: 0f
        val background = source?.backgroundCoordinates
        val offset = if (frost != null) offsetInBackground(requireLayoutCoordinates()) else null
        if (frost != null && background != null && offset != null && alpha > 0f) {
            lastOffset = offset
            clip.reset()
            clip.addOutline(shape.createOutline(size, layoutDirection, this))
            clipPath(clip) {
                translate(-offset.x, -offset.y) {
                    // Same placement as the wallpaper Image: ContentScale.Crop, Alignment.TopCenter.
                    val bgWidth = background.size.width.toFloat()
                    val bgHeight = background.size.height.toFloat()
                    val scale =
                        max(bgWidth / frost.sourceSize.width, bgHeight / frost.sourceSize.height)
                    val drawnWidth = frost.sourceSize.width * scale
                    val drawnHeight = frost.sourceSize.height * scale
                    drawImage(
                        image = frost.image,
                        dstOffset = IntOffset(((bgWidth - drawnWidth) / 2f).roundToInt(), 0),
                        dstSize = IntSize(drawnWidth.roundToInt(), drawnHeight.roundToInt()),
                        alpha = alpha,
                        filterQuality = FilterQuality.Low,
                    )
                }
                drawRect(source.tint, size = Size(size.width, size.height), alpha = alpha)
            }
        }
        drawContent()
    }
}
