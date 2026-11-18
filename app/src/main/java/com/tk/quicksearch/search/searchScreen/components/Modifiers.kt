package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
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
import com.tk.quicksearch.shared.ui.theme.homeBackgroundIsDark

private const val PredictedSubmitIndicatorEnterDelayMs = 500
private const val PredictedSubmitIndicatorFadeInDurationMs = AppMotion.DurationMedium
private const val PredictedSubmitIndicatorFadeOutDurationMs = AppMotion.DurationStandard

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
                easing = AppMotion.EmphasizedEasing,
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
            // Over an image the neutral follows the image's luminance, not the app theme.
            when (imageBackgroundIsDark) {
                true, false ->
                    lerp(AppColors.indicatorBase(lightFill = imageBackgroundIsDark == true), primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                null ->
                    if (opaqueCardTopResultBorder) {
                        lerp(MaterialTheme.colorScheme.onSurface, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                    } else {
                        primary
                    }
            }
        val (fillAlpha, borderAlpha) =
            if (opaqueCardTopResultBorder) {
                AppColors.TintFaintAlpha to AppColors.DisabledContentAlpha
            } else {
                AppColors.TintSubtleAlpha to AppColors.TintStrongAlpha
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
