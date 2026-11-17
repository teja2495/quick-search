package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.homeBackgroundIsDark

private const val PredictedSubmitIndicatorEnterDelayMs = 500
private const val PredictedSubmitIndicatorFadeInDurationMs = 300
private const val PredictedSubmitIndicatorFadeOutDurationMs = 240

@Composable
internal fun rememberPredictedSubmitIndicatorAlpha(isPredicted: Boolean): State<Float> {
    val indicatorTransition =
        updateTransition(targetState = isPredicted, label = "predictedSubmitIndicator")
    return indicatorTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis =
                    if (targetState) {
                        PredictedSubmitIndicatorFadeInDurationMs
                    } else {
                        PredictedSubmitIndicatorFadeOutDurationMs
                    },
                delayMillis = if (targetState) PredictedSubmitIndicatorEnterDelayMs else 0,
                easing = FastOutSlowInEasing,
            )
        },
        label = "predictedSubmitIndicatorAlpha",
    ) { predicted -> if (predicted) 1f else 0f }
}

internal fun Modifier.predictedSubmitHighlight(
    isPredicted: Boolean,
    shape: Shape = AppShapes.card,
    opaqueCardTopResultBorder: Boolean = false,
): Modifier =
    composed {
        val indicatorAlpha = rememberPredictedSubmitIndicatorAlpha(isPredicted)
        val imageBackgroundIsDark = homeBackgroundIsDark()
        val primary = MaterialTheme.colorScheme.primary
        val highlightColor =
            when (imageBackgroundIsDark) {
                true -> lerp(Color.White, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                false -> lerp(Color.Black, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                null ->
                    if (opaqueCardTopResultBorder) {
                        val neutral = if (LocalAppIsDarkTheme.current) Color.White else Color.Black
                        lerp(neutral, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                    } else {
                        primary
                    }
            }
        val (fillAlpha, borderAlpha) =
            if (opaqueCardTopResultBorder) {
                0.055f to 0.36f
            } else {
                0.08f to 0.22f
            }

        this.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = outline.toPath()
            val borderWidthPx = DesignTokens.BorderWidth.toPx()
            onDrawWithContent {
                // Read animation state here so each tick invalidates drawing, not composition.
                val alpha = indicatorAlpha.value
                if (alpha > 0f) {
                    drawPath(
                        path = outlinePath,
                        color = highlightColor.copy(alpha = fillAlpha * alpha),
                    )
                }
                drawContent()
                if (alpha > 0f) {
                    drawInsideBorder(
                        path = outlinePath,
                        color = highlightColor.copy(alpha = borderAlpha * alpha),
                        width = borderWidthPx,
                    )
                }
            }
        }
    }

internal fun Modifier.predictedSubmitCardBorder(
    isPredicted: Boolean,
    shape: Shape = AppShapes.card,
): Modifier =
    composed {
        val indicatorTransition = updateTransition(targetState = isPredicted, label = "predictedSubmitCardBorder")
        val indicatorAlpha =
            indicatorTransition.animateFloat(
                transitionSpec = {
                    tween(
                        durationMillis =
                            if (targetState) {
                                PredictedSubmitIndicatorFadeInDurationMs
                            } else {
                                PredictedSubmitIndicatorFadeOutDurationMs
                            },
                        delayMillis = if (targetState) PredictedSubmitIndicatorEnterDelayMs else 0,
                        easing = FastOutSlowInEasing,
                    )
                },
                label = "predictedSubmitBorderAlpha",
            ) { predicted ->
                if (predicted) 1f else 0f
            }
        val imageBackgroundIsDark = homeBackgroundIsDark()
        val primary = MaterialTheme.colorScheme.primary
        val borderColor =
            when (imageBackgroundIsDark) {
                true ->
                    lerp(Color.White, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                        .copy(alpha = AppColors.TintStrongAlpha)
                false ->
                    lerp(Color.Black, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                        .copy(alpha = AppColors.TintStrongAlpha)
                null -> primary
            }

        this.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val outlinePath = outline.toPath()
            val borderWidthPx = DesignTokens.BorderWidth.toPx()
            onDrawWithContent {
                drawContent()
                // Keep the animated alpha read in the draw phase for draw-only invalidation.
                val alpha = indicatorAlpha.value
                if (alpha > 0f) {
                    val effectiveAlpha =
                        if (imageBackgroundIsDark == null) {
                            0.24f * alpha * alpha
                        } else {
                            borderColor.alpha * alpha
                        }
                    drawInsideBorder(
                        path = outlinePath,
                        color = borderColor.copy(alpha = effectiveAlpha),
                        width = borderWidthPx,
                    )
                }
            }
        }
    }

private fun DrawScope.drawInsideBorder(
    path: Path,
    color: Color,
    width: Float,
) {
    clipPath(path) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = width * 2f),
        )
    }
}

private fun Outline.toPath(): Path =
    when (this) {
        is Outline.Rectangle -> Path().apply { addRect(rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
        is Outline.Generic -> path
    }
