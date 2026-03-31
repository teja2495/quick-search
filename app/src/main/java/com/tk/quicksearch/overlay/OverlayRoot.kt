package com.tk.quicksearch.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tk.quicksearch.app.navigation.SettingsNavigationMemory
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.ExcludeUndoSnackbarHost
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.search.searchScreen.components.NumberKeyboardOperatorPills
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet
import com.tk.quicksearch.shared.util.WallpaperUtils
import kotlinx.coroutines.delay

private const val OVERLAY_WIDTH_PERCENT = 0.9f
private const val OVERLAY_TABLET_PORTRAIT_WIDTH_PERCENT = 0.85f
private const val OVERLAY_TABLET_WIDTH_PERCENT = 0.65f
private const val OVERLAY_MAX_HEIGHT_PERCENT = 0.95f
private const val OVERLAY_FALLBACK_GRADIENT_ALPHA = 0.98f
private const val OVERLAY_CONTENT_RESIZE_ANIMATION_MS = 140
private const val OVERLAY_ENTER_ANIMATION_MS = 420
private const val OVERLAY_EXIT_ANIMATION_MS = 220
private const val OVERLAY_ENTER_START_DELAY_MS = 32
private const val OVERLAY_ENTER_START_SCALE = 0.9f
private val OVERLAY_BORDER_WIDTH = 1.25.dp
private val OVERLAY_OPERATOR_ROW_ESTIMATED_HEIGHT = 48.dp
private val OVERLAY_TOP_OFFSET = 16.dp
private val OVERLAY_ENTER_START_OFFSET = 56.dp

@Composable
fun OverlayRoot(
        viewModel: SearchViewModel,
        animationToken: Long = 0L,
        onCloseRequested: () -> Unit,
        modifier: Modifier = Modifier,
) {
        // Keep initial frame visible to avoid cold-start transparent flash.
        var isVisible by remember(animationToken) { mutableStateOf(true) }
        var canPlayEnterAnimation by remember(animationToken) { mutableStateOf(false) }

        // Handle closing with animation
        val handleClose = { isVisible = false }

        // Call onCloseRequested immediately when isVisible becomes false
        val exitAnimationDuration = OVERLAY_EXIT_ANIMATION_MS
        LaunchedEffect(isVisible) {
                if (!isVisible) {
                        delay(exitAnimationDuration.toLong())
                        onCloseRequested()
                }
        }
        LaunchedEffect(animationToken) {
                canPlayEnterAnimation = false
                // Delay enter animation slightly so it plays after the window is visible.
                delay(OVERLAY_ENTER_START_DELAY_MS.toLong())
                canPlayEnterAnimation = true
        }

        BackHandler(enabled = isVisible) { handleClose() }

        val context = LocalContext.current
        val overlaySnackbarHostState = remember { SnackbarHostState() }
        var overlayManualNumberKeyboard by remember { mutableStateOf(false) }
        var overlayImeVisible by remember { mutableStateOf(false) }
        var overlayOperatorRowHeightPx by remember { mutableStateOf(0) }
        var wallpaperChangeVersion by remember { mutableIntStateOf(0) }

        DisposableEffect(context) {
                val appContext = context.applicationContext
                @Suppress("DEPRECATION")
                val wallpaperChangedAction = Intent.ACTION_WALLPAPER_CHANGED
                val receiver =
                        object : BroadcastReceiver() {
                                override fun onReceive(
                                        context: Context?,
                                        intent: Intent?,
                                ) {
                                        if (intent?.action != wallpaperChangedAction) return
                                        WallpaperUtils.invalidateWallpaperCache()
                                        wallpaperChangeVersion++
                                }
                        }
                val filter = IntentFilter(wallpaperChangedAction)
                ContextCompat.registerReceiver(
                        appContext,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                onDispose {
                        appContext.unregisterReceiver(receiver)
                }
        }

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
                        val uiState by viewModel.uiState.collectAsState()
                        val density = LocalDensity.current
                        val layoutDirection = LocalLayoutDirection.current
                        val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                        val imeBottomPadding =
                                WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                        val shouldShowOverlayOperatorRow =
                                overlayImeVisible &&
                                        (overlayManualNumberKeyboard ||
                                                uiState.calculatorState.isCalculatorMode)
                        val targetOperatorRowReservedHeight =
                                if (shouldShowOverlayOperatorRow) {
                                        if (overlayOperatorRowHeightPx > 0) {
                                                with(density) { overlayOperatorRowHeightPx.toDp() }
                                        } else {
                                                OVERLAY_OPERATOR_ROW_ESTIMATED_HEIGHT
                                        }
                                } else {
                                        0.dp
                                }
                        val overlayOperatorRowReservedHeight by animateDpAsState(
                                targetValue = targetOperatorRowReservedHeight,
                                animationSpec =
                                        tween(
                                                durationMillis = OVERLAY_CONTENT_RESIZE_ANIMATION_MS,
                                                easing = LinearOutSlowInEasing,
                                        ),
                                label = "operatorRowReservedHeight",
                        )
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
                                (maxHeight -
                                                overlayTopPadding -
                                                bottomSafePadding -
                                                overlayOperatorRowReservedHeight)
                                        .coerceAtLeast(
                                        0.dp
                                )
                        val availableWidth =
                                (maxWidth - leftSafePadding - rightSafePadding).coerceAtLeast(0.dp)
                        val maxOverlayHeight =
                                (availableHeight * OVERLAY_MAX_HEIGHT_PERCENT).coerceAtLeast(0.dp)
                        val overlayWidthPercent =
                                if (isTablet()) {
                                        if (isLandscape()) {
                                                OVERLAY_TABLET_WIDTH_PERCENT
                                        } else {
                                                OVERLAY_TABLET_PORTRAIT_WIDTH_PERCENT
                                        }
                                } else {
                                        OVERLAY_WIDTH_PERCENT
                                }
                        val targetOverlayWidth =
                                (availableWidth * overlayWidthPercent).coerceAtLeast(0.dp)
                        val overlayWidth = targetOverlayWidth

                        val overlayWallpaperBitmap by
                                produceState<ImageBitmap?>(
                                        initialValue = null,
                                        key1 = uiState.backgroundSource,
                                        key2 = wallpaperChangeVersion,
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
                        val useMonoThemeFallback =
                                uiState.backgroundSource != BackgroundSource.THEME &&
                                        overlayImageBitmap == null

                        val overlayEntryProgress by animateFloatAsState(
                                targetValue = if (canPlayEnterAnimation && isVisible) 1f else 0f,
                                animationSpec =
                                        tween(
                                                durationMillis =
                                                        if (isVisible) {
                                                                OVERLAY_ENTER_ANIMATION_MS
                                                        } else {
                                                                OVERLAY_EXIT_ANIMATION_MS
                                                        },
                                                easing = FastOutSlowInEasing,
                                        ),
                                label = "overlayEntryProgress",
                        )

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
                                val slideOffsetPx = with(density) { OVERLAY_ENTER_START_OFFSET.toPx() }
                                Box(
                                        modifier =
                                                Modifier.width(overlayWidth)
                                                        .heightIn(max = maxOverlayHeight)
                                                        .animateContentSize(
                                                                animationSpec =
                                                                        tween(
                                                                                durationMillis =
                                                                                        OVERLAY_CONTENT_RESIZE_ANIMATION_MS,
                                                                                easing = LinearOutSlowInEasing,
                                                                        ),
                                                        )
                                                        .graphicsLayer {
                                                                alpha = overlayEntryProgress
                                                                val scale =
                                                                        OVERLAY_ENTER_START_SCALE +
                                                                                ((1f -
                                                                                                OVERLAY_ENTER_START_SCALE) *
                                                                                        overlayEntryProgress)
                                                                scaleX = scale
                                                                scaleY = scale
                                                                translationY =
                                                                        slideOffsetPx *
                                                                                (1f -
                                                                                        overlayEntryProgress)
                                                        }
                                                        .clip(DesignTokens.ExtraLargeCardShape)
                                                        .border(
                                                                width = OVERLAY_BORDER_WIDTH,
                                                                color = AppColors.SearchChromeOutlineBorder,
                                                                shape = DesignTokens.ExtraLargeCardShape,
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
                                                wallpaperBlurRadius = uiState.wallpaperBlurRadius,
                                                backgroundTransitionDurationMillis =
                                                        DesignTokens.WallpaperFadeInDuration + 220,
                                                animateBlurRadius = false,
                                                fallbackBackgroundAlpha =
                                                        OVERLAY_FALLBACK_GRADIENT_ALPHA,
                                                useGradientFallback =
                                                        uiState.backgroundSource ==
                                                                        BackgroundSource.THEME ||
                                                                useMonoThemeFallback,
                                                appTheme =
                                                        if (useMonoThemeFallback) {
                                                                AppTheme.MONOCHROME
                                                        } else {
                                                                uiState.appTheme
                                                        },
                                                overlayThemeIntensity =
                                                        uiState.overlayThemeIntensity,
                                                modifier = Modifier.fillMaxSize(),
                                        )

                                        SearchRoute(
                                                modifier = Modifier.fillMaxSize(),
                                                viewModel = viewModel,
                                                isOverlayPresentation = true,
                                                overlaySnackbarHostState = overlaySnackbarHostState,
                                                onOverlayNumberKeyboardUiChanged = {
                                                    manuallySwitched, isImeOpen ->
                                                    overlayManualNumberKeyboard = manuallySwitched
                                                    overlayImeVisible = isImeOpen
                                                },
                                                onWelcomeAnimationCompleted = {
                                                        viewModel.onSearchBarWelcomeAnimationCompleted()
                                                },
                                                onOverlayDismissRequest = { handleClose() },
                                                onSettingsClick = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
                                                                settingsDetailType =
                                                                        SettingsNavigationMemory
                                                                                .getLastOpenedSettingsDetail(),
                                                        )
                                                        handleClose()
                                                },
                                                onOpenSearchHistorySettings = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
                                                                settingsDetailType =
                                                                        SettingsDetailType
                                                                                .SEARCH_RESULTS,
                                                        )
                                                        handleClose()
                                                },
                                                onSearchEngineLongPress = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
                                                                settingsDetailType =
                                                                        SettingsDetailType
                                                                                .SEARCH_ENGINES,
                                                        )
                                                        handleClose()
                                                },
                                                onCustomizeSearchEnginesClick = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
                                                                settingsDetailType =
                                                                        SettingsDetailType
                                                                                .SEARCH_ENGINES,
                                                        )
                                                        handleClose()
                                                },
                                                onOpenDirectSearchConfigure = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
                                                                settingsDetailType =
                                                                        SettingsDetailType
                                                                                .GEMINI_API_CONFIG,
                                                        )
                                                        handleClose()
                                                },
                                                onOpenReleaseNotesFeatures = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
                                                                settingsDetailType =
                                                                        SettingsDetailType.FEATURES_LIST,
                                                        )
                                                        handleClose()
                                                },
                                                onOpenAppSettingDestination = { destination ->
                                                        handleOverlayAppSettingDestination(
                                                                context = context,
                                                                destination = destination,
                                                                viewModel = viewModel,
                                                                autoCloseOverlay = uiState.autoCloseOverlay,
                                                                onCloseRequested = handleClose,
                                                        )
                                                },
                                        )
                                }
                        }

                        AnimatedVisibility(
                                visible = shouldShowOverlayOperatorRow,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                enter = fadeIn(animationSpec = tween(durationMillis = OVERLAY_CONTENT_RESIZE_ANIMATION_MS)),
                                exit = fadeOut(animationSpec = tween(durationMillis = OVERLAY_CONTENT_RESIZE_ANIMATION_MS)),
                        ) {
                                NumberKeyboardOperatorPills(
                                        modifier =
                                                Modifier.imePadding()
                                                        .fillMaxWidth()
                                                        .onSizeChanged { size ->
                                                                overlayOperatorRowHeightPx = size.height
                                                        },
                                        onOperatorClick = { operator ->
                                                viewModel.onQueryChange(uiState.query + operator)
                                        },
                                        isOverlayPresentation = true,
                                        extendToScreenEdges = false,
                                        showWallpaperBackground = uiState.showWallpaperBackground,
                                )
                        }

                        ExcludeUndoSnackbarHost(
                                hostState = overlaySnackbarHostState,
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(
                                                        start = DesignTokens.SpacingLarge,
                                                        end = DesignTokens.SpacingLarge,
                                                        bottom = DesignTokens.SpacingHuge,
                                ),
                        )
                }
}
