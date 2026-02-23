package com.tk.quicksearch.search.overlay

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.ExcludeUndoSnackbarHost
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.ui.components.TipBanner
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.WallpaperUtils
import kotlinx.coroutines.delay

private const val OVERLAY_WIDTH_PERCENT = 0.9f
private const val OVERLAY_HEIGHT_PERCENT = 0.85f
private const val OVERLAY_FALLBACK_GRADIENT_ALPHA = 0.98f
private val OVERLAY_TOP_OFFSET = 16.dp

@Composable
fun OverlayRoot(
        viewModel: SearchViewModel,
        onCloseRequested: () -> Unit,
        modifier: Modifier = Modifier,
) {
        // Keep initial frame visible to avoid cold-start transparent flash.
        var isVisible by remember { mutableStateOf(true) }

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

        val context = LocalContext.current
        val tipAlpha = if (isVisible) 1f else 0f

        val overlaySnackbarHostState = remember { SnackbarHostState() }

        QuickSearchTheme {
                BoxWithConstraints(
                        modifier =
                                modifier
                                        .fillMaxSize()
                                        .clickable(
                                                interactionSource =
                                                        remember { MutableInteractionSource() },
                                                indication = null,
                                        ) { handleClose() },
                ) {
                        val persistedKeyboardOpenHeightDp =
                                remember { viewModel.getLastOverlayKeyboardOpenHeightDp() }
                        var learnedKeyboardOpenHeightDp by
                                remember { mutableStateOf<Float?>(persistedKeyboardOpenHeightDp) }
                        val hasPersistedHeightAtLaunch = persistedKeyboardOpenHeightDp != null
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
                        // Let the OS handle IME resize (adjustResize); the card just tracks
                        // current available window height.
                        val targetOverlayHeight =
                                (availableHeight * OVERLAY_HEIGHT_PERCENT).coerceAtLeast(0.dp)
                        val targetOverlayWidth =
                                (availableWidth * OVERLAY_WIDTH_PERCENT).coerceAtLeast(0.dp)

                        val overlayHeight =
                                if (hasPersistedHeightAtLaunch) {
                                        minOf(
                                                persistedKeyboardOpenHeightDp!!.dp,
                                                availableHeight,
                                        )
                                } else {
                                        targetOverlayHeight
                                }
                        val overlayWidth by animateDpAsState(
                                targetValue = targetOverlayWidth,
                                animationSpec = tween(durationMillis = 300),
                                label = "overlayWidth",
                        )

                        LaunchedEffect(imeBottomPadding, targetOverlayHeight) {
                                if (hasPersistedHeightAtLaunch) return@LaunchedEffect
                                if (imeBottomPadding <= 0.dp || targetOverlayHeight <= 0.dp) {
                                        return@LaunchedEffect
                                }

                                val currentHeightDp = targetOverlayHeight.value
                                val previousHeightDp = learnedKeyboardOpenHeightDp
                                val shouldPersist =
                                        previousHeightDp == null ||
                                                currentHeightDp < previousHeightDp - 0.5f
                                if (shouldPersist) {
                                        learnedKeyboardOpenHeightDp = currentHeightDp
                                        viewModel.setLastOverlayKeyboardOpenHeightDp(
                                                currentHeightDp
                                        )
                                }
                        }

                        val uiState by viewModel.uiState.collectAsState()
                        val overlayWallpaperBitmap by
                                produceState<ImageBitmap?>(
                                        initialValue = null,
                                        key1 = uiState.backgroundSource,
                                ) {
                                        value =
                                                if (
                                                        uiState.backgroundSource ==
                                                                        BackgroundSource
                                                                                .SYSTEM_WALLPAPER
                                                ) {
                                                        WallpaperUtils.getCachedWallpaperBitmap()
                                                                ?.asImageBitmap()
                                                                ?: WallpaperUtils
                                                                        .getWallpaperBitmap(context)
                                                                        ?.asImageBitmap()
                                                } else {
                                                        null
                                                }
                                }
                        val overlayCustomBitmap by
                                produceState<ImageBitmap?>(
                                        initialValue = null,
                                        key1 = uiState.backgroundSource,
                                        key2 = uiState.customImageUri,
                                ) {
                                        value =
                                                if (
                                                        uiState.backgroundSource ==
                                                                BackgroundSource.CUSTOM_IMAGE
                                                ) {
                                                        WallpaperUtils.getOverlayCustomImageBitmap(
                                                                context,
                                                                uiState.customImageUri,
                                                        )
                                                } else {
                                                        null
                                                }
                                }
                        val overlayImageBitmap =
                                when (uiState.backgroundSource) {
                                        BackgroundSource.SYSTEM_WALLPAPER ->
                                                overlayWallpaperBitmap
                                        BackgroundSource.CUSTOM_IMAGE -> overlayCustomBitmap
                                        BackgroundSource.THEME -> null
                                }
                        val useImageBackground =
                                uiState.backgroundSource != BackgroundSource.THEME &&
                                        overlayImageBitmap != null

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
                                                                .clickable(
                                                                        interactionSource =
                                                                                remember {
                                                                                        MutableInteractionSource()
                                                                                },
                                                                        indication = null,
                                                                ) {},
                                        ) {
                                                SearchScreenBackground(
                                                        showWallpaperBackground = useImageBackground,
                                                        wallpaperBitmap = overlayImageBitmap,
                                                        wallpaperBackgroundAlpha =
                                                                uiState.wallpaperBackgroundAlpha,
                                                        wallpaperBlurRadius =
                                                                uiState.wallpaperBlurRadius,
                                                        fallbackBackgroundAlpha =
                                                                OVERLAY_FALLBACK_GRADIENT_ALPHA,
                                                        useGradientFallback =
                                                                uiState.backgroundSource ==
                                                                        BackgroundSource
                                                                                .THEME,
                                                        overlayGradientTheme =
                                                                uiState.overlayGradientTheme,
                                                        overlayThemeIntensity =
                                                                uiState.overlayThemeIntensity,
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
                                                                                settingsDetailType =
                                                                                        SettingsDetailType
                                                                                                .SEARCH_ENGINES,
                                                                        )
                                                                handleClose()
                                                        },
                                                        onCustomizeSearchEnginesClick = {
                                                                OverlayModeController
                                                                        .openMainActivity(
                                                                                context,
                                                                                openSettings = true,
                                                                                settingsDetailType =
                                                                                        SettingsDetailType
                                                                                                .SEARCH_ENGINES,
                                                                        )
                                                                handleClose()
                                                        },
                                                        onOpenDirectSearchConfigure = {
                                                                OverlayModeController
                                                                        .openMainActivity(
                                                                                context,
                                                                                openSettings = true,
                                                                                settingsDetailType =
                                                                                        SettingsDetailType
                                                                                                .DIRECT_SEARCH_CONFIGURE,
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
