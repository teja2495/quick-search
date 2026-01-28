package com.tk.quicksearch.search.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.ui.components.TipBanner
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import kotlinx.coroutines.delay

@Composable
fun OverlayRoot(
        viewModel: SearchViewModel,
        onCloseRequested: () -> Unit,
        modifier: Modifier = Modifier
) {
        var isVisible by remember { mutableStateOf(false) }

        // Trigger entry animation
        LaunchedEffect(Unit) { isVisible = true }

        // Handle closing with animation
        val handleClose = { isVisible = false }

        // Wait for exit animation to complete before calling onCloseRequested
        LaunchedEffect(isVisible) {
                if (!isVisible) {
                        delay(DesignTokens.AnimationDurationMedium.toLong())
                        onCloseRequested()
                }
        }

        BackHandler(enabled = isVisible) { handleClose() }

        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val availableHeight = (screenHeight - imeBottomPadding).coerceAtLeast(0.dp)
        val isKeyboardVisible = imeBottomPadding > 0.dp
        val maxHeightRatio = if (isKeyboardVisible) 0.5f else 0.75f
        val minHeightRatio = 0f
        val targetMaxOverlayHeight = minOf(screenHeight * maxHeightRatio, availableHeight)
        val targetMinOverlayHeight = minOf(screenHeight * minHeightRatio, targetMaxOverlayHeight)

        // Animate height changes
        val maxOverlayHeight by
                animateDpAsState(
                        targetValue = targetMaxOverlayHeight,
                        animationSpec = tween(durationMillis = DesignTokens.AnimationDurationShort),
                        label = "maxOverlayHeight"
                )
        val minOverlayHeight by
                animateDpAsState(
                        targetValue = targetMinOverlayHeight,
                        animationSpec = tween(durationMillis = DesignTokens.AnimationDurationShort),
                        label = "minOverlayHeight"
                )

        val tipAlpha by
                animateFloatAsState(
                        targetValue = if (isVisible) 1f else 0f,
                        animationSpec =
                                tween(durationMillis = DesignTokens.AnimationDurationMedium),
                        label = "tipAlpha"
                )

        QuickSearchTheme {
                Box(
                        modifier =
                                modifier.fillMaxSize().clickable(
                                                interactionSource =
                                                        remember { MutableInteractionSource() },
                                                indication = null
                                        ) { handleClose() }
                ) {
                        AnimatedVisibility(
                                visible = isVisible,
                                enter =
                                        fadeIn(tween(DesignTokens.AnimationDurationMedium)) +
                                                slideInVertically(
                                                        tween(DesignTokens.AnimationDurationMedium)
                                                ) { -it / 10 },
                                exit =
                                        fadeOut(tween(DesignTokens.AnimationDurationMedium)) +
                                                slideOutVertically(
                                                        tween(DesignTokens.AnimationDurationMedium)
                                                ) { -it / 10 },
                                modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.padding(
                                                                horizontal =
                                                                        DesignTokens
                                                                                .ContentHorizontalPadding,
                                                                vertical = DesignTokens.SpacingLarge
                                                        )
                                                        .fillMaxWidth()
                                                        .heightIn(
                                                                min = minOverlayHeight,
                                                                max = maxOverlayHeight
                                                        )
                                                        .animateContentSize(
                                                                animationSpec =
                                                                        tween(
                                                                                durationMillis =
                                                                                        DesignTokens
                                                                                                .AnimationDurationShort
                                                                        )
                                                        )
                                                        .background(
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .background,
                                                                shape =
                                                                        DesignTokens
                                                                                .ExtraLargeCardShape
                                                        )
                                                        .clickable(
                                                                interactionSource =
                                                                        remember {
                                                                                MutableInteractionSource()
                                                                        },
                                                                indication = null
                                                        ) {}
                                ) {
                                        SearchRoute(
                                                viewModel = viewModel,
                                                isOverlayPresentation = true,
                                                onSettingsClick = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true
                                                        )
                                                        handleClose()
                                                },
                                                onSearchEngineLongPress = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true
                                                        )
                                                        handleClose()
                                                },
                                                onCustomizeSearchEnginesClick = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true
                                                        )
                                                        handleClose()
                                                }
                                        )
                                }
                        }

                        val uiState by viewModel.uiState.collectAsState()

                        if (uiState.showOverlayCloseTip) {
                                TipBanner(
                                        text = stringResource(R.string.overlay_close_tip),
                                        onDismiss = { viewModel.dismissOverlayCloseTip() },
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .navigationBarsPadding()
                                                        .imePadding()
                                                        .padding(
                                                                start =
                                                                        DesignTokens
                                                                                .ContentHorizontalPadding,
                                                                end =
                                                                        DesignTokens
                                                                                .ContentHorizontalPadding,
                                                                bottom = DesignTokens.SpacingLarge
                                                        )
                                                        .alpha(tipAlpha)
                                )
                        }
                }
        }
}
