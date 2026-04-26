package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark

private const val PredictedSubmitIndicatorEnterDelayMs = 500
private const val PredictedSubmitIndicatorFadeInDurationMs = 300
private const val PredictedSubmitIndicatorFadeOutDurationMs = 240

internal fun Modifier.predictedSubmitHighlight(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
    opaqueCardTopResultBorder: Boolean = false,
): Modifier =
    composed {
        val indicatorTransition = updateTransition(targetState = isPredicted, label = "predictedSubmitIndicator")
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
                label = "predictedSubmitIndicatorAlpha",
            ) { predicted ->
                if (predicted) 1f else 0f
            }.value

        if (indicatorAlpha <= 0f) {
            this
        } else {
            val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
            val primary = MaterialTheme.colorScheme.primary
            val highlightColor =
                when (imageBackgroundIsDark) {
                    true ->
                        lerp(Color.White, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                    false ->
                        lerp(Color.Black, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                    null ->
                        if (opaqueCardTopResultBorder) {
                            val neutral =
                                if (LocalAppIsDarkTheme.current) Color.White else Color.Black
                            lerp(neutral, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                        } else {
                            primary
                        }
                }
            val (fillAlpha, borderAlpha) =
                if (opaqueCardTopResultBorder) {
                    0.055f to 0.42f
                } else {
                    0.08f to 0.22f
                }
            this
                .background(
                    color = highlightColor.copy(alpha = fillAlpha * indicatorAlpha),
                    shape = shape,
                )
                .border(
                    width = DesignTokens.BorderWidth,
                    color = highlightColor.copy(alpha = borderAlpha * indicatorAlpha),
                    shape = shape,
                )
        }
    }

internal fun Modifier.predictedSubmitCardBorder(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
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
            }.value

        if (indicatorAlpha <= 0f) {
            this
        } else {
            val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
            val primary = MaterialTheme.colorScheme.primary
            val borderColor =
                when (imageBackgroundIsDark) {
                    true ->
                        lerp(Color.White, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                            .copy(alpha = 0.24f)
                    false ->
                        lerp(Color.Black, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
                            .copy(alpha = 0.24f)
                    null -> primary.copy(alpha = 0.24f * indicatorAlpha)
                }
            this.border(
                width = DesignTokens.BorderWidth,
                color = borderColor.copy(alpha = borderColor.alpha * indicatorAlpha),
                shape = shape,
            )
        }
    }
