package com.tk.quicksearch.search.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.ExcludeUndoSnackbarHost
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.ui.components.TipBanner
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.WallpaperUtils
import kotlinx.coroutines.delay

private const val OVERLAY_WIDTH_PERCENT = 0.9f
private const val OVERLAY_DEFAULT_HEIGHT_PERCENT = 0.488f
private const val OVERLAY_HEIGHT_PERCENT_WITH_KEYBOARD = 0.8f
private const val OVERLAY_HEIGHT_PERCENT_WITHOUT_KEYBOARD = 0.8f
private const val KEYBOARD_HEIGHT_CALCULATION_DELAY_MS = 250L
private const val KEYBOARD_APPEARANCE_GRACE_PERIOD_MS = 250L
private val OVERLAY_TOP_OFFSET = 16.dp

@Composable
fun OverlayRoot(
        viewModel: SearchViewModel,
        onCloseRequested: () -> Unit,
        modifier: Modifier = Modifier,
) {
        var isVisible by remember { mutableStateOf(false) }

        // Trigger entry animation
        LaunchedEffect(Unit) { isVisible = true }

        // Handle closing with animation
        val handleClose = { isVisible = false }

        // Call onCloseRequested immediately when isVisible becomes false
        val animationDuration = 250
        LaunchedEffect(isVisible) {
                if (!isVisible) {
                        delay(animationDuration.toLong())
                        onCloseRequested()
                }
        }

        BackHandler(enabled = isVisible) { handleClose() }

        var useKeyboardAwareHeight by remember { mutableStateOf(false) }
        var allowKeyboardLessHeight by remember { mutableStateOf(false) }
        LaunchedEffect(isVisible) {
                if (isVisible) {
                        useKeyboardAwareHeight = false
                        allowKeyboardLessHeight = false
                        delay(KEYBOARD_HEIGHT_CALCULATION_DELAY_MS)
                        useKeyboardAwareHeight = true
                        delay(KEYBOARD_APPEARANCE_GRACE_PERIOD_MS)
                        allowKeyboardLessHeight = true
                } else {
                        useKeyboardAwareHeight = false
                        allowKeyboardLessHeight = false
                }
        }

        val context = LocalContext.current
        val tipAlpha = if (isVisible) 1f else 0f

        val overlaySnackbarHostState = remember { SnackbarHostState() }

        var wallpaperBitmap by remember {
                mutableStateOf<ImageBitmap?>(
                        WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                )
        }

        LaunchedEffect(Unit) {
                if (wallpaperBitmap == null) {
                        val bitmap = WallpaperUtils.getWallpaperBitmap(context)
                        wallpaperBitmap = bitmap?.asImageBitmap()
                }
        }

        QuickSearchTheme {
                BoxWithConstraints(
                        modifier =
                                modifier.fillMaxSize().clickable(
                                                interactionSource =
                                                        remember { MutableInteractionSource() },
                                                indication = null,
                                        ) { handleClose() },
                ) {
                        val layoutDirection = LocalLayoutDirection.current
                        val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                        val imeBottomPadding =
                                WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                        val topSafePadding = systemBarsPadding.calculateTopPadding()
                        val leftSafePadding = systemBarsPadding.calculateLeftPadding(layoutDirection)
                        val rightSafePadding =
                                systemBarsPadding.calculateRightPadding(layoutDirection)
                        val bottomSafePadding =
                                maxOf(
                                        systemBarsPadding.calculateBottomPadding(),
                                        imeBottomPadding,
                                )
                        val overlayTopPadding = topSafePadding + OVERLAY_TOP_OFFSET
                        val availableHeight =
                                (maxHeight - overlayTopPadding - bottomSafePadding).coerceAtLeast(
                                        0.dp
                                )
                        val availableWidth =
                                (maxWidth - leftSafePadding - rightSafePadding).coerceAtLeast(0.dp)
                        val isKeyboardVisible = imeBottomPadding > 0.dp
                        val keyboardAwareOverlayHeightPercent =
                                if (isKeyboardVisible) {
                                        OVERLAY_HEIGHT_PERCENT_WITH_KEYBOARD
                                } else if (allowKeyboardLessHeight) {
                                        OVERLAY_HEIGHT_PERCENT_WITHOUT_KEYBOARD
                                } else {
                                        OVERLAY_DEFAULT_HEIGHT_PERCENT
                                }
                        val overlayHeightPercent =
                                if (useKeyboardAwareHeight) {
                                        keyboardAwareOverlayHeightPercent
                                } else {
                                        OVERLAY_DEFAULT_HEIGHT_PERCENT
                                }
                        val targetOverlayHeight =
                                (availableHeight * overlayHeightPercent).coerceAtLeast(0.dp)
                        val targetOverlayWidth =
                                (availableWidth * OVERLAY_WIDTH_PERCENT).coerceAtLeast(0.dp)

                        val overlayHeight by animateDpAsState(
                                targetValue = targetOverlayHeight,
                                animationSpec = tween(durationMillis = 300),
                                label = "overlayHeight",
                        )
                        val overlayWidth by animateDpAsState(
                                targetValue = targetOverlayWidth,
                                animationSpec = tween(durationMillis = 300),
                                label = "overlayWidth",
                        )

                        val uiState by viewModel.uiState.collectAsState()

                        androidx.compose.animation.AnimatedVisibility(
                                visible = isVisible,
                                enter =
                                        androidx.compose.animation.fadeIn(
                                                androidx.compose.animation.core.tween(
                                                        animationDuration
                                                )
                                        ),
                                exit =
                                        androidx.compose.animation.fadeOut(
                                                androidx.compose.animation.core.tween(
                                                        animationDuration
                                                )
                                        )
                        ) {
                                Box(
                                        modifier =
                                                Modifier.align(Alignment.TopCenter)
                                                        .padding(
                                                                start = leftSafePadding,
                                                                end = rightSafePadding,
                                                                top = overlayTopPadding,
                                                        )
                                                        .fillMaxSize(),
                                        contentAlignment = Alignment.TopCenter,
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.width(overlayWidth)
                                                                .height(overlayHeight)
                                                                .clip(
                                                                        DesignTokens
                                                                                .ExtraLargeCardShape
                                                                )
                                                                .then(
                                                                        if (uiState.showWallpaperBackground
                                                                        ) {
                                                                                Modifier
                                                                        } else {
                                                                                Modifier.background(
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .background,
                                                                                )
                                                                        }
                                                                )
                                                                .clickable(
                                                                        interactionSource =
                                                                                remember {
                                                                                        MutableInteractionSource()
                                                                                },
                                                                        indication = null,
                                                                ) {},
                                        ) {
                                                SearchScreenBackground(
                                                        showWallpaperBackground =
                                                                uiState.showWallpaperBackground,
                                                        wallpaperBitmap = wallpaperBitmap,
                                                        wallpaperBackgroundAlpha =
                                                                uiState.wallpaperBackgroundAlpha,
                                                        wallpaperBlurRadius =
                                                                uiState.wallpaperBlurRadius,
                                                        modifier = Modifier.fillMaxSize(),
                                                )

                                                SearchRoute(
                                                        modifier = Modifier.fillMaxSize(),
                                                        viewModel = viewModel,
                                                        isOverlayPresentation = true,
                                                        overlaySnackbarHostState =
                                                                overlaySnackbarHostState,
                                                        onWelcomeAnimationCompleted = {
                                                                viewModel
                                                                        .onSearchBarWelcomeAnimationCompleted()
                                                        },
                                                        onOverlayDismissRequest = { handleClose() },
                                                        onSettingsClick = {
                                                                OverlayModeController
                                                                        .openMainActivity(
                                                                                context,
                                                                                openSettings = true,
                                                                        )
                                                                handleClose()
                                                        },
                                                        onSearchEngineLongPress = {
                                                                OverlayModeController
                                                                        .openMainActivity(
                                                                                context,
                                                                                openSettings = true,
                                                                        )
                                                                handleClose()
                                                        },
                                                        onCustomizeSearchEnginesClick = {
                                                                OverlayModeController
                                                                        .openMainActivity(
                                                                                context,
                                                                                openSettings = true,
                                                                        )
                                                                handleClose()
                                                        },
                                                )
                                        }
                                }
                        }

                        var tipDelayElapsed by remember { mutableStateOf(false) }
                        LaunchedEffect(isVisible, uiState.showOverlayCloseTip) {
                                when {
                                        !isVisible || !uiState.showOverlayCloseTip ->
                                                tipDelayElapsed = false
                                        else -> {
                                                delay(2000)
                                                tipDelayElapsed = true
                                        }
                                }
                        }

                        if (uiState.showOverlayCloseTip && tipDelayElapsed) {
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
                                                                bottom = DesignTokens.SpacingLarge,
                                                        )
                                                        .alpha(tipAlpha),
                                )
                        }

                        ExcludeUndoSnackbarHost(
                                hostState = overlaySnackbarHostState,
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .navigationBarsPadding()
                                                .imePadding()
                                                .padding(
                                                        start = DesignTokens.SpacingLarge,
                                                        end = DesignTokens.SpacingLarge,
                                                        bottom = DesignTokens.SpacingHuge,
                                                ),
                        )
                }
        }
}
