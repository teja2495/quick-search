package com.tk.quicksearch.tools.aiSearch

import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.tk.quicksearch.shared.ui.theme.AppPolygons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Loading state for AI-backed results: an M3 Expressive-style morphing shape indicator next to
 * softly shimmering text placeholders. Built on stable androidx.graphics:graphics-shapes because
 * Material 3's LoadingIndicator is still experimental.
 */
@Composable
fun InformationCardLoadingAnimation(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Row(
        modifier = modifier.fillMaxWidth().progressSemantics().padding(vertical = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        MorphingShapeLoadingIndicator(
            color = primaryColor,
            modifier = Modifier.size(32.dp),
        )
        ShimmerPlaceholderLines(
            baseColor = containerColor,
            highlightColor = lerp(containerColor, primaryColor, 0.28f),
            modifier = Modifier.weight(1f),
        )
    }
}

private const val MorphIntervalMillis = 650L
private const val FullRotationMillis = 4666
private const val RotationPerMorph = 90f

private val LoadingShapes: List<RoundedPolygon> by lazy {
    with(AppPolygons) { listOf(softBurst, cookie9, pentagon, pill, sunny, cookie4, oval) }
}

private val LoadingMorphs: List<Morph> by lazy {
    LoadingShapes.indices.map { index ->
        Morph(LoadingShapes[index], LoadingShapes[(index + 1) % LoadingShapes.size])
    }
}

@Composable
private fun MorphingShapeLoadingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val morphProgress = remember { Animatable(0f) }
    val step = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            launch {
                morphProgress.snapTo(0f)
                morphProgress.animateTo(1f, AppMotion.loadingMorph())
            }
            delay(MorphIntervalMillis)
            step.intValue += 1
        }
    }
    val baseRotation by
        rememberInfiniteTransition(label = "LoadingIndicatorRotation").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(FullRotationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "baseRotation",
        )
    val path = remember { android.graphics.Path() }
    val composePath = remember(path) { path.asComposePath() }

    Canvas(modifier = modifier) {
        val currentStep = step.intValue
        val progress = morphProgress.value
        val morph = LoadingMorphs[currentStep % LoadingMorphs.size]
        morph.toPath(progress = progress.coerceIn(0f, 1f), path = path)
        val rotation = (baseRotation + (currentStep + progress) * RotationPerMorph) % 360f
        // Keep slight padding so pointy shapes don't clip while rotating.
        val inset = size.minDimension * 0.06f
        val drawSize = size.minDimension - inset * 2f
        rotate(rotation) {
            translate(left = (size.width - drawSize) / 2f, top = (size.height - drawSize) / 2f) {
                scale(scale = drawSize, pivot = Offset.Zero) {
                    drawPath(path = composePath, color = color)
                }
            }
        }
    }
}

private val PlaceholderLineFractions = listOf(1f, 0.92f, 0.58f)
private const val ShimmerDurationMillis = 1400
private const val ShimmerStagger = 0.12f

@Composable
private fun ShimmerPlaceholderLines(
    baseColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
) {
    val shimmer =
        rememberInfiniteTransition(label = "PlaceholderShimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1f + ShimmerStagger * PlaceholderLineFractions.size,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(ShimmerDurationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmerProgress",
        )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        PlaceholderLineFractions.forEachIndexed { index, fraction ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(12.dp)
                        .shimmerLine(shimmer, index, baseColor, highlightColor),
            )
        }
    }
}

private fun Modifier.shimmerLine(
    shimmer: State<Float>,
    index: Int,
    baseColor: Color,
    highlightColor: Color,
): Modifier =
    drawWithCache {
        val band = max(size.width * 0.45f, 48.dp.toPx())
        val brush =
            Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(-band, 0f),
                end = Offset(band, 0f),
                tileMode = TileMode.Clamp,
            )
        val radius = CornerRadius(size.height / 2f)
        onDrawBehind {
            val lineProgress = shimmer.value - index * ShimmerStagger
            val center = -band + lineProgress * (size.width + band * 2f)
            translate(left = center) {
                drawRoundRect(brush, topLeft = Offset(-center, 0f), size = size, cornerRadius = radius)
            }
        }
    }
