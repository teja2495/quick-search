package com.tk.quicksearch.search.apps

import androidx.compose.material3.ExperimentalMaterial3Api
import com.tk.quicksearch.search.apps.swipeGestures.appSwipeGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.search.apps.notificationDots.hasNotificationDot
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.components.rememberPredictedSubmitIndicatorAlpha
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.search.folders.AppFolderMember

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AppGridItem(
        modifier: Modifier = Modifier,
        appInfo: AppInfo,
        shortcuts: List<StaticShortcut>,
        appActions: AppActions,
        appState: AppState,
        iconPackPackage: String?,
        isPredicted: Boolean = false,
        oneHandedMode: Boolean = false,
        appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
        notificationDotKeys: Set<String> = emptySet(),
        showPinnedIndicators: Boolean = false,
        isDragging: Boolean = false,
        dragOffset: IntOffset? = null,
        onItemMeasured: (Int) -> Unit = {},
        onPinnedDragStart: (() -> Unit)? = null,
        onPinnedDrag: ((Float, Float) -> Unit)? = null,
        onPinnedDragEnd: ((Boolean) -> Unit)? = null,
        isMergeSource: Boolean = false,
        fadeMergeSource: Boolean = false,
        isMergeTarget: Boolean = false,
        onHoldChange: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
    val indicatorUseLightFill =
            if (showWallpaperBackground && imageBackgroundIsDark != null) {
                imageBackgroundIsDark
            } else {
                LocalAppIsDarkTheme.current
            }
    val primary = MaterialTheme.colorScheme.primary
    val indicatorFillBase =
            if (indicatorUseLightFill) {
                lerp(Color.White, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
            } else {
                lerp(Color.Black, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
            }
    val iconResult =
            rememberAppIcon(
                    packageName = appInfo.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = appInfo.userHandleId,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
            )
    LaunchedEffect(iconResult.bitmap) {
        if (iconResult.bitmap != null) {
            StartupTrace.mark("QS.Home.AppIconLoaded")
        }
    }
    var showOptions by remember { mutableStateOf(false) }
    val appIconSize =
            remember(appState.isOverlayPresentation, appIconSizeStep) {
                val sizeScale = UiPreferences.appIconSizeScale(appIconSizeStep)
                when (
                    if (appState.isOverlayPresentation) {
                        AppIconDisplayMode.OVERLAY
                    } else {
                        AppIconDisplayMode.REGULAR
                    }
                ) {
                    AppIconDisplayMode.OVERLAY -> OverlayAppIconSize * sizeScale
                    AppIconDisplayMode.REGULAR -> RegularAppIconSize * sizeScale
                }
            }
    val appIconSurfaceSize =
            remember(appState.isOverlayPresentation, appIconSizeStep) {
                val sizeScale = UiPreferences.appIconSizeScale(appIconSizeStep)
                if (appState.isOverlayPresentation) {
                    OverlayAppIconSurfaceSize * sizeScale
                } else {
                    DesignTokens.AppIconSize * sizeScale
                }
            }
    val indicatorAlpha by rememberPredictedSubmitIndicatorAlpha(isPredicted)
    val showTopResultIndicator = indicatorAlpha > 0f
    var isLocalDragging by remember { mutableStateOf(false) }
    val showDraggedPresentation = isDragging || isLocalDragging
    val dragScale by animateFloatAsState(
            targetValue =
                    when {
                        showDraggedPresentation && isMergeSource -> MergeSourcePinnedAppScale
                        showDraggedPresentation -> DraggedPinnedAppScale
                        else -> 1f
                    },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedAppDragScale",
    )
    val dragAlpha by animateFloatAsState(
            targetValue =
                    when {
                        showDraggedPresentation && fadeMergeSource -> 0.35f
                        showDraggedPresentation -> DraggedPinnedAppAlpha
                        else -> 1f
                    },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedAppDragAlpha",
    )
    val dragModifier =
            rememberPinnedGridDragModifier(
                    key = appInfo.launchCountKey(),
                    onClick = appActions.onClick,
                    onShowOptions = { showOptions = true },
                    onLocalDraggingChange = { isLocalDragging = it },
                    onPinnedDragStart = onPinnedDragStart,
                    onPinnedDrag = onPinnedDrag,
                    onPinnedDragEnd = onPinnedDragEnd,
                    onHoldChange = onHoldChange,
            )

    Box(
            modifier =
                    modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.size.height > 0) {
                                    onItemMeasured(coordinates.size.height)
                                }
                            }
                            .zIndex(if (showDraggedPresentation) 1f else 0f)
                            .graphicsLayer {
                                if (showDraggedPresentation && dragOffset != null) {
                                    translationX = dragOffset.x.toFloat()
                                    translationY = dragOffset.y.toFloat()
                                }
                                scaleX = dragScale
                                scaleY = dragScale
                                alpha = dragAlpha
                            },
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                                indicatorFillBase.copy(
                                        alpha =
                                                TopResultIndicatorBackgroundAlpha * indicatorAlpha,
                                ),
                        shape = DesignTokens.ShapeLarge,
                    )
                    .padding(
                        top = TopResultIndicatorTopPadding,
                        bottom = TopResultIndicatorBottomPadding,
                        start = if (showTopResultIndicator) TopResultIndicatorHorizontalPadding else 0.dp,
                        end = if (showTopResultIndicator) TopResultIndicatorHorizontalPadding else 0.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            val isDraggable = onPinnedDragStart != null && onPinnedDrag != null && onPinnedDragEnd != null
            AppIconSurface(
                    iconBitmap = iconResult.bitmap,
                    iconIsLegacy = iconResult.isLegacy,
                    monochromeData = iconResult.monochromeData,
                    appName = appInfo.appName,
                    onClick = { if (!showOptions) appActions.onClick() },
                    onLongClick = if (isDraggable) null else ({ showOptions = true }),
                    gestureModifier =
                            Modifier.appSwipeGestures(appInfo)
                                    .then(dragModifier),
                    clickGesturesEnabled = !isDraggable,
                    appIconSurfaceSize = appIconSurfaceSize,
                    appIconSize = appIconSize,
                    appIconShape = appIconShape,
                    hasCustomIconPack = iconPackPackage != null,
                    iconPackPackage = iconPackPackage,
                    oneHandedMode = oneHandedMode,
                    themedIconsEnabled = themedIconsEnabled,
                    showWallpaperBackground = showWallpaperBackground,
                    showPinnedIndicator = showPinnedIndicators && appState.isPinned,
                    showNotificationDot = appInfo.hasNotificationDot(notificationDotKeys),
                    folderPreviewMember =
                            AppFolderMember.App(appInfo).takeIf { isMergeTarget },
            )
            if (appState.showAppLabel) {
                AppLabelText(
                        appName = appInfo.appName,
                        isOverlayPresentation = appState.isOverlayPresentation,
                )
            }
        }

        AppItemDropdownMenu(
                expanded = showOptions,
                onDismiss = { showOptions = false },
                isPinned = appState.isPinned,
                showUninstall = appState.showUninstall,
                hasNickname = appState.hasNickname,
                hasTrigger = appState.hasTrigger,
                shortcuts = shortcuts,
                appInfo = appInfo,
                iconPackPackage = iconPackPackage,
                appIconShape = appIconShape,
                onShortcutClick = appActions.onShortcutClick,
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onDisableShortcut = appActions.onDisableAppShortcut,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onTriggerClick = appActions.onTriggerClick,
                onAddToHome = appActions.onAddToHome,
                onOpenInSplitScreen = appActions.onOpenInSplitScreen,
        )
    }
}
