package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Full-screen overlay for search engine onboarding. Uses a gradient scrim that fades towards the
 * bottom, leaving the search engine section visible and highlighted. Shows explanatory text
 * directly above the search engines.
 */
@Composable
internal fun SearchEngineOnboardingOverlay(
        visible: Boolean,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
        isOverlayPresentation: Boolean = false,
        showStartSearchingButton: Boolean = false,
) {
    AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier,
    ) {
        val boxModifier =
                if (isOverlayPresentation) {
                    Modifier.fillMaxSize().clip(DesignTokens.ExtraLargeCardShape)
                } else {
                    Modifier.fillMaxSize()
                }
        Box(modifier = boxModifier) {
            // Gradient scrim (no onClick - only close button dismisses)
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    AppColors.OnboardingScrimTop,
                                                                    AppColors.OnboardingScrimMiddle,
                                                                    AppColors.OnboardingScrimBottom,
                                                                    AppColors.AppBackgroundTransparent,
                                                            ),
                                                    startY = 0f,
                                                    endY = Float.POSITIVE_INFINITY,
                                            ),
                                    ),
            )

            // Onboarding content positioned at the bottom, just above search engines
            OnboardingContent(
                    onDismiss = onDismiss,
                    showStartSearchingButton = showStartSearchingButton,
                    modifier =
                            Modifier.align(Alignment.BottomCenter)
                                    .let { if (isOverlayPresentation) it else it.imePadding() }
                                    .padding(bottom = 70.dp)
                                    .padding(horizontal = DesignTokens.OnboardingSectionSpacing),
            )
        }
    }
}

@Composable
private fun OnboardingContent(
        onDismiss: () -> Unit,
        showStartSearchingButton: Boolean = false,
        modifier: Modifier = Modifier,
) {
    val bubbleBackgroundColor = AppColors.DialogBackground
    val bubbleBorderColor = AppColors.OnboardingBubbleBorder

    Box(
            modifier = modifier.fillMaxWidth(),
    ) {
        // Speech bubble with arrow pointing down
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .drawBehind {
                                    val cornerRadius =
                                            DesignTokens.OnboardingPermissionCardCornerRadius.toPx()
                                    val arrowWidth = DesignTokens.Spacing40.toPx()
                                    val arrowHeight = DesignTokens.SpacingXLarge.toPx()
                                    val borderWidth = DesignTokens.SearchFieldBorderWidth.toPx()

                                    val rect =
                                            Rect(
                                                    offset = Offset(0f, 0f),
                                                    size =
                                                            Size(
                                                                    size.width,
                                                                    size.height - arrowHeight
                                                            ),
                                            )

                                    // Create path for speech bubble with arrow
                                    val path =
                                            Path().apply {
                                                // Start from top left, going clockwise
                                                moveTo(cornerRadius, 0f)
                                                // Top edge
                                                lineTo(rect.width - cornerRadius, 0f)
                                                // Top right corner
                                                arcTo(
                                                        rect =
                                                                Rect(
                                                                        left =
                                                                                rect.width -
                                                                                        cornerRadius *
                                                                                                2,
                                                                        top = 0f,
                                                                        right = rect.width,
                                                                        bottom = cornerRadius * 2,
                                                                ),
                                                        startAngleDegrees = 270f,
                                                        sweepAngleDegrees = 90f,
                                                        forceMoveTo = false,
                                                )
                                                // Right edge
                                                lineTo(rect.width, rect.height - cornerRadius)
                                                // Bottom right corner
                                                arcTo(
                                                        rect =
                                                                Rect(
                                                                        left =
                                                                                rect.width -
                                                                                        cornerRadius *
                                                                                                2,
                                                                        top =
                                                                                rect.height -
                                                                                        cornerRadius *
                                                                                                2,
                                                                        right = rect.width,
                                                                        bottom = rect.height,
                                                                ),
                                                        startAngleDegrees = 0f,
                                                        sweepAngleDegrees = 90f,
                                                        forceMoveTo = false,
                                                )
                                                // Bottom edge to arrow start
                                                val arrowPosition =
                                                        rect.width *
                                                                0.25f // Position arrow at 1/4 from
                                                // left
                                                lineTo(
                                                        arrowPosition + (arrowWidth / 2),
                                                        rect.height
                                                )
                                                // Arrow pointing down
                                                lineTo(arrowPosition, size.height)
                                                lineTo(
                                                        arrowPosition - (arrowWidth / 2),
                                                        rect.height
                                                )
                                                // Bottom edge after arrow
                                                lineTo(cornerRadius, rect.height)
                                                // Bottom left corner
                                                arcTo(
                                                        rect =
                                                                Rect(
                                                                        left = 0f,
                                                                        top =
                                                                                rect.height -
                                                                                        cornerRadius *
                                                                                                2,
                                                                        right = cornerRadius * 2,
                                                                        bottom = rect.height,
                                                                ),
                                                        startAngleDegrees = 90f,
                                                        sweepAngleDegrees = 90f,
                                                        forceMoveTo = false,
                                                )
                                                // Left edge
                                                lineTo(0f, cornerRadius)
                                                // Top left corner
                                                arcTo(
                                                        rect =
                                                                Rect(
                                                                        left = 0f,
                                                                        top = 0f,
                                                                        right = cornerRadius * 2,
                                                                        bottom = cornerRadius * 2,
                                                                ),
                                                        startAngleDegrees = 180f,
                                                        sweepAngleDegrees = 90f,
                                                        forceMoveTo = false,
                                                )
                                                close()
                                            }

                                    // Fill
                                    drawPath(
                                            path = path,
                                            color = bubbleBackgroundColor,
                                    )

                                    // Border
                                    drawPath(
                                            path = path,
                                            color = bubbleBorderColor,
                                            style = Stroke(width = borderWidth),
                                    )
                                }
                                .padding(bottom = DesignTokens.SpacingXLarge) // Account for arrow height
                                .padding(
                                        horizontal = DesignTokens.SpacingLarge,
                                        vertical = DesignTokens.SpacingLarge
                                ) // Reduced horizontal padding
                                .padding(end = DesignTokens.SpacingXLarge), // Extra padding for close button
        ) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingXSmall),
                    // Add padding below text
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                // Title
                Text(
                        text = stringResource(R.string.setup_search_engines_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.DialogText,
                        textAlign = TextAlign.Center,
                )

                // Description
                Text(
                        text = stringResource(R.string.search_engine_onboarding_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.OnboardingBubbleBodyText,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f,
                )
                if (showStartSearchingButton) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
                        contentPadding =
                            PaddingValues(
                                horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                                vertical = DesignTokens.OnboardingButtonVerticalPadding,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.setup_action_start),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Close button in top right corner
        IconButton(
                onClick = onDismiss,
                modifier =
                        Modifier.align(Alignment.TopEnd)
                                .padding(
                                    top = DesignTokens.SpacingXSmall,
                                    end = DesignTokens.SpacingSmall,
                                )
                                .size(DesignTokens.SpacingHuge),
        ) {
            Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.desc_close),
                    tint = AppColors.DialogText,
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }
}
