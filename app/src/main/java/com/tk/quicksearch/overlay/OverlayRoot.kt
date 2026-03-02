package com.tk.quicksearch.overlay

import androidx.compose.animation.animateContentSize
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchScreen.ExcludeUndoSnackbarHost
import com.tk.quicksearch.search.searchScreen.NumberKeyboardOperatorPills
import com.tk.quicksearch.search.searchScreen.PersistentSearchField
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.resolveDefaultBrowserPackage
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet
import com.tk.quicksearch.shared.util.WallpaperUtils
import kotlinx.coroutines.delay

private const val OVERLAY_WIDTH_PERCENT = 0.9f
private const val OVERLAY_TABLET_PORTRAIT_WIDTH_PERCENT = 0.7f
private const val OVERLAY_TABLET_WIDTH_PERCENT = 0.5f
private const val OVERLAY_MAX_HEIGHT_PERCENT = 0.95f
private const val OVERLAY_FALLBACK_GRADIENT_ALPHA = 0.98f
private const val OVERLAY_CONTENT_RESIZE_ANIMATION_MS = 140
private const val OVERLAY_ENTER_ANIMATION_MS = 420
private const val OVERLAY_EXIT_ANIMATION_MS = 220
private const val OVERLAY_ENTER_START_DELAY_MS = 32
private const val OVERLAY_ENTER_START_SCALE = 0.9f
private val OVERLAY_TOP_OFFSET = 16.dp
private val OVERLAY_ENTER_START_OFFSET = 56.dp
private val OVERLAY_EXTERNAL_SEARCH_BAR_MIN_RESERVED_HEIGHT = 62.dp
private val OVERLAY_EXTERNAL_SEARCH_BAR_BOTTOM_PADDING = DesignTokens.SpacingMedium
private val OVERLAY_OPERATOR_PILLS_RESERVED_HEIGHT = 48.dp

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
                        var overlayNumberKeyboardSelected by remember { mutableStateOf(false) }
                        var overlayImeVisible by remember { mutableStateOf(false) }
                        var overlayExternalSearchBarHeight by
                                remember { mutableStateOf(OVERLAY_EXTERNAL_SEARCH_BAR_MIN_RESERVED_HEIGHT) }
                        val showOverlayOperatorPills =
                                overlayNumberKeyboardSelected && overlayImeVisible
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
                        val showExternalBottomSearchBar = uiState.bottomSearchBarEnabled
                        val externalBottomBarReservedHeight =
                                if (showExternalBottomSearchBar) {
                                        overlayExternalSearchBarHeight +
                                                if (showOverlayOperatorPills) {
                                                        OVERLAY_OPERATOR_PILLS_RESERVED_HEIGHT
                                                } else {
                                                        0.dp
                                                }
                                } else {
                                        0.dp
                                }
                        val overlayTopPadding = topSafePadding + OVERLAY_TOP_OFFSET
                        val availableHeight =
                                (maxHeight -
                                                overlayTopPadding -
                                                bottomSafePadding -
                                                externalBottomBarReservedHeight)
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
                                uiState.backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                                        overlayImageBitmap == null

                        val density = LocalDensity.current
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
                                                overlayGradientTheme =
                                                        if (useMonoThemeFallback) {
                                                                OverlayGradientTheme.MONOCHROME
                                                        } else {
                                                                uiState.overlayGradientTheme
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
                                                onWelcomeAnimationCompleted = {
                                                        viewModel.onSearchBarWelcomeAnimationCompleted()
                                                },
                                                onOverlayNumberKeyboardUiChanged =
                                                        { isNumberKeyboardSelected, isImeOpen ->
                                                                overlayNumberKeyboardSelected =
                                                                        isNumberKeyboardSelected
                                                                overlayImeVisible = isImeOpen
                                                        },
                                                onOverlayDismissRequest = { handleClose() },
                                                onSettingsClick = {
                                                        OverlayModeController.openMainActivity(
                                                                context,
                                                                openSettings = true,
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
                                                                                .DIRECT_SEARCH_CONFIGURE,
                                                        )
                                                        handleClose()
                                                },
                                        )
                                }
                        }

                        if (showExternalBottomSearchBar) {
                                val enabledTargets: List<SearchTarget> =
                                        remember(uiState.searchTargetsOrder, uiState.disabledSearchTargetIds) {
                                                uiState.searchTargetsOrder.filter {
                                                        it.getId() !in uiState.disabledSearchTargetIds
                                                }
                                        }
                                PersistentSearchField(
                                        query = uiState.query,
                                        onQueryChange = viewModel::onQueryChange,
                                        onClearQuery = viewModel::clearQuery,
                                        onSettingsClick = {
                                                OverlayModeController.openMainActivity(
                                                        context,
                                                        openSettings = true,
                                                )
                                                handleClose()
                                        },
                                        dismissKeyboardBeforeSettingsClick = true,
                                        enabledTargets = enabledTargets,
                                        shouldUseNumberKeyboard = overlayNumberKeyboardSelected,
                                        detectedShortcutTarget = uiState.detectedShortcutTarget,
                                        showWelcomeAnimation = uiState.showSearchBarWelcomeAnimation,
                                        opaqueBackground = true,
                                        onClearDetectedShortcut = viewModel::clearDetectedShortcut,
                                        onWelcomeAnimationCompleted = {
                                                viewModel.onSearchBarWelcomeAnimationCompleted()
                                        },
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .onSizeChanged { size ->
                                                                val measuredHeight = with(density) { size.height.toDp() }
                                                                val reservedHeight =
                                                                        (measuredHeight - bottomSafePadding)
                                                                                .coerceAtLeast(
                                                                                        OVERLAY_EXTERNAL_SEARCH_BAR_MIN_RESERVED_HEIGHT
                                                                                )
                                                                if (reservedHeight != overlayExternalSearchBarHeight) {
                                                                        overlayExternalSearchBarHeight =
                                                                                reservedHeight
                                                                }
                                                        }
                                                        .width(overlayWidth)
                                                        .imePadding()
                                                        .navigationBarsPadding()
                                                        .padding(
                                                                bottom =
                                                                        OVERLAY_EXTERNAL_SEARCH_BAR_BOTTOM_PADDING +
                                                                                if (showOverlayOperatorPills) {
                                                                                        OVERLAY_OPERATOR_PILLS_RESERVED_HEIGHT
                                                                                } else {
                                                                                        0.dp
                                                                                },
                                                        )
                                                        .graphicsLayer { alpha = overlayEntryProgress },
                                        onSearchAction = {
                                                val trimmedQuery = uiState.query.trim()
                                                val isUrlQuery = isLikelyWebUrl(trimmedQuery)
                                                val defaultBrowserPackage =
                                                        resolveDefaultBrowserPackage(context)
                                                if (uiState.query != trimmedQuery) {
                                                        viewModel.onQueryChange(trimmedQuery)
                                                }

                                                if (isUrlQuery && trimmedQuery.isNotBlank()) {
                                                        val browserTarget =
                                                                defaultBrowserTarget(
                                                                        uiState.searchTargetsOrder,
                                                                        defaultBrowserPackage,
                                                                )
                                                        if (browserTarget != null) {
                                                                viewModel.openSearchTarget(
                                                                        trimmedQuery,
                                                                        browserTarget,
                                                                )
                                                                return@PersistentSearchField
                                                        }
                                                }

                                                val firstApp =
                                                        uiState.searchResults.firstOrNull()
                                                                ?: uiState.recentApps.firstOrNull()
                                                                ?: uiState.pinnedApps.firstOrNull()
                                                if (firstApp != null) {
                                                        viewModel.launchApp(firstApp)
                                                        return@PersistentSearchField
                                                }

                                                val firstAppShortcut = uiState.appShortcutResults.firstOrNull()
                                                if (firstAppShortcut != null) {
                                                        viewModel.launchAppShortcut(firstAppShortcut)
                                                        return@PersistentSearchField
                                                }

                                                val firstContact = uiState.contactResults.firstOrNull()
                                                if (firstContact != null) {
                                                        if (firstContact.hasContactMethods) {
                                                                viewModel.showContactMethodsBottomSheet(
                                                                        firstContact
                                                                )
                                                        } else {
                                                                viewModel.openContact(firstContact)
                                                        }
                                                        return@PersistentSearchField
                                                }

                                                val firstFile = uiState.fileResults.firstOrNull()
                                                if (firstFile != null) {
                                                        viewModel.openFile(firstFile)
                                                        return@PersistentSearchField
                                                }

                                                val firstSetting = uiState.settingResults.firstOrNull()
                                                if (firstSetting != null) {
                                                        viewModel.openSetting(firstSetting)
                                                        return@PersistentSearchField
                                                }

                                                val detectedTarget = uiState.detectedShortcutTarget
                                                if (detectedTarget != null) {
                                                        viewModel.openSearchTarget(trimmedQuery, detectedTarget)
                                                        return@PersistentSearchField
                                                }

                                                val primaryTarget = enabledTargets.firstOrNull()
                                                if (primaryTarget != null && trimmedQuery.isNotBlank()) {
                                                        viewModel.openSearchTarget(trimmedQuery, primaryTarget)
                                                }
                                        },
                                )
                        }

                        val overlayBottomFloatingPadding =
                                if (showExternalBottomSearchBar) {
                                        overlayExternalSearchBarHeight +
                                                if (showOverlayOperatorPills) {
                                                        OVERLAY_OPERATOR_PILLS_RESERVED_HEIGHT
                                                } else {
                                                        0.dp
                                                }
                                } else {
                                        0.dp
                                }

                        AnimatedVisibility(
                                visible = showOverlayOperatorPills,
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .imePadding(),
                        ) {
                                NumberKeyboardOperatorPills(
                                        isOverlayPresentation = true,
                                        extendToScreenEdges = false,
                                        modifier =
                                                Modifier.padding(
                                                        top =
                                                                if (showExternalBottomSearchBar) {
                                                                        OVERLAY_EXTERNAL_SEARCH_BAR_BOTTOM_PADDING
                                                                } else {
                                                                        0.dp
                                                                },
                                                        bottom =
                                                                0.dp,
                                                ),
                                        onOperatorClick = { operator ->
                                                viewModel.onQueryChange(uiState.query + operator)
                                        },
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
                                                        bottom =
                                                                DesignTokens.SpacingHuge +
                                                                        overlayBottomFloatingPadding,
                                ),
                        )
                }
}
