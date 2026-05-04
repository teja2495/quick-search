package com.tk.quicksearch.search.apps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.LocalIsSystemWallpaperActive
import com.tk.quicksearch.shared.ui.theme.LocalWallpaperDynamicAccentActive
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.shared.util.hapticConfirm

private const val ROW_COUNT = 2
private const val SuggestionsEnterDurationMillis = 320
private const val SuggestionsEnterOffsetDp = 12f
private val AppGridRowSpacing = DesignTokens.SpacingXSmall
private val RegularAppIconSize = DesignTokens.IconSizeXLarge - DesignTokens.SpacingXXSmall
private val TopResultIndicatorTopPadding = 0.dp
private val TopResultIndicatorBottomPadding = DesignTokens.SpacingSmall
private val TopResultIndicatorHorizontalPadding = DesignTokens.SpacingSmall
private const val TopResultIndicatorBackgroundAlpha = 0.12f
private const val TopResultIndicatorBorderAlpha = 0.22f
private const val LightWallpaperAppIconShadowAmbientAlpha = 0.28f
private const val LightWallpaperAppIconShadowSpotAlpha = 0.45f
private const val ThemedMonochromeGlyphScale = 1.42f
private const val UnsupportedThemedIconGlyphScale = 0.62f
private const val UnsupportedThemedIconGlyphAlpha = 0.72f
private enum class AppIconDisplayMode {
    OVERLAY,
    REGULAR,
}

/** Data class containing all app actions to reduce parameter count in composables. */
private data class AppActions(
        val onClick: () -> Unit,
        val onAppInfoClick: () -> Unit,
        val onUninstallClick: () -> Unit,
        val onHideApp: () -> Unit,
        val onPinApp: () -> Unit,
        val onUnpinApp: () -> Unit,
        val onNicknameClick: () -> Unit,
        val onTriggerClick: () -> Unit,
        val onAddToHome: () -> Unit,
)

/** Data class containing app state information to reduce parameter count in composables. */
private data class AppState(
        val hasNickname: Boolean,
        val hasTrigger: Boolean,
        val isPinned: Boolean,
        val showUninstall: Boolean,
        val showAppLabel: Boolean,
        val isOverlayPresentation: Boolean,
)

@Composable
fun AppGridView(
        apps: List<AppInfo>,
        appShortcuts: List<StaticShortcut>,
        isSearching: Boolean,
        hasAppResults: Boolean,
        onAppClick: (AppInfo) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        disabledShortcutIds: Set<String>,
        modifier: Modifier = Modifier,
        rowCount: Int = ROW_COUNT,
        phoneColumnOverride: Int = 5,
        iconPackPackage: String? = null,
        showAppLabels: Boolean = true,
        oneHandedMode: Boolean = false,
        isInitializing: Boolean = false,
        isOverlayPresentation: Boolean = false,
        startupPhase: StartupPhase = StartupPhase.COMPLETE,
        predictedTarget: PredictedSubmitTarget? = null,
        suppressTopResultIndicator: Boolean = false,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
) {
    val context = LocalContext.current
    val shortcutsByPackage =
            remember(appShortcuts, disabledShortcutIds) {
                appShortcuts
                        .asSequence()
                        .filterNot { shortcut ->
                            disabledShortcutIds.contains(shortcutKey(shortcut))
                        }
                        .groupBy { it.packageName }
            }
    val waitForAppIcons = apps.isNotEmpty()
    val areAppIconsLoaded =
            if (waitForAppIcons) {
                apps.all { app ->
                    val iconResult =
                            rememberAppIcon(
                                    packageName = app.packageName,
                                    iconPackPackage = iconPackPackage,
                                    userHandleId = app.userHandleId,
                                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                            )
                    iconResult.bitmap != null
                }
            } else {
                true
            }

    // Once the grid has appeared with all icons loaded, keep it visible even when new fuzzy-search
    // results arrive with icons still loading — avoids flicker when the list grows mid-search.
    var gridHasBeenVisible by remember { mutableStateOf(false) }

    // Animate the suggestions grid (empty query) when it first appears. Search results should
    // appear immediately without animation.
    val suggestionsAlpha = remember { Animatable(0f) }
    val suggestionsTranslationYDp = remember { Animatable(SuggestionsEnterOffsetDp) }
    val density = LocalDensity.current

    Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
    ) {
        val showAppGrid = apps.isNotEmpty() && (areAppIconsLoaded || gridHasBeenVisible)

        LaunchedEffect(showAppGrid, isSearching) {
            if (!showAppGrid) return@LaunchedEffect
            if (isSearching) {
                suggestionsAlpha.snapTo(1f)
                suggestionsTranslationYDp.snapTo(0f)
            } else if (suggestionsAlpha.value < 1f) {
                suggestionsAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = SuggestionsEnterDurationMillis),
                )
            }
        }
        LaunchedEffect(showAppGrid, isSearching) {
            if (!showAppGrid || isSearching) return@LaunchedEffect
            if (suggestionsTranslationYDp.value != 0f) {
                suggestionsTranslationYDp.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = SuggestionsEnterDurationMillis),
                )
            }
        }

        if (showAppGrid) {
            gridHasBeenVisible = true
            AppGrid(
                    modifier = Modifier.graphicsLayer {
                        alpha = suggestionsAlpha.value
                        translationY = with(density) { suggestionsTranslationYDp.value.dp.toPx() }
                    },
                    apps = apps,
                    isSearching = isSearching,
                    onAppClick = onAppClick,
                    onAppInfoClick = onAppInfoClick,
                    onUninstallClick = onUninstallClick,
                    onHideApp = onHideApp,
                    onPinApp = onPinApp,
                    onUnpinApp = onUnpinApp,
                    onNicknameClick = onNicknameClick,
                    onTriggerClick = onTriggerClick,
                    getAppNickname = getAppNickname,
                    getAppTrigger = getAppTrigger,
                    pinnedPackageNames = pinnedPackageNames,
                    shortcutsByPackage = shortcutsByPackage,
                    rowCount = rowCount,
                    phoneColumnOverride = phoneColumnOverride,
                    iconPackPackage = iconPackPackage,
                    showAppLabels = showAppLabels,
                    oneHandedMode = oneHandedMode,
                    isOverlayPresentation = isOverlayPresentation,
                    predictedTarget = predictedTarget,
                    suppressTopResultIndicator = suppressTopResultIndicator,
                    appIconShape = appIconShape,
                    themedIconsEnabled = themedIconsEnabled,
                    showWallpaperBackground = showWallpaperBackground,
            )
        }
    }
}

@Composable
private fun AppGrid(
        apps: List<AppInfo>,
        isSearching: Boolean,
        modifier: Modifier = Modifier,
        onAppClick: (AppInfo) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        rowCount: Int = ROW_COUNT,
        phoneColumnOverride: Int = 5,
        iconPackPackage: String?,
        showAppLabels: Boolean,
        oneHandedMode: Boolean,
        isOverlayPresentation: Boolean,
        predictedTarget: PredictedSubmitTarget?,
        suppressTopResultIndicator: Boolean,
        appIconShape: AppIconShape,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
) {
    val maxVisibleColumns = getAppGridColumns(phoneColumnOverride)
    val columns =
            remember(apps, maxVisibleColumns) {
                if (apps.isEmpty()) {
                    1
                } else {
                    maxVisibleColumns.coerceAtLeast(1)
                }
            }
    val rows =
            remember(apps, oneHandedMode, columns) {
                // Show all available apps, chunked into rows of the appropriate column count
                val chunked = apps.chunked(columns)
                if (oneHandedMode) chunked.reversed() else chunked
            }
    val firstResultKey = remember(apps, suppressTopResultIndicator) {
        if (suppressTopResultIndicator) null else apps.firstOrNull()?.launchCountKey()
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val horizontalSpacing = DesignTokens.SpacingMedium
        val rowItemWidth =
                if (columns <= 1) {
                    maxWidth
                } else {
                    ((maxWidth - (horizontalSpacing * (columns - 1))) / columns).coerceAtLeast(0.dp)
                }

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
        ) {
            val context = LocalContext.current
            val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
            val createAppActions =
                    remember(
                            onAppClick,
                            onAppInfoClick,
                            onUninstallClick,
                            onHideApp,
                            onPinApp,
                            onUnpinApp,
                            onNicknameClick,
                            onTriggerClick,
                            addToHomeHandler
                    ) {
                        { app: AppInfo ->
                            AppActions(
                                    onClick = { onAppClick(app) },
                                    onAppInfoClick = { onAppInfoClick(app) },
                                    onUninstallClick = { onUninstallClick(app) },
                                    onHideApp = { onHideApp(app) },
                                    onPinApp = { onPinApp(app) },
                                    onUnpinApp = { onUnpinApp(app) },
                                    onNicknameClick = { onNicknameClick(app) },
                                    onTriggerClick = { onTriggerClick(app) },
                                    onAddToHome = { addToHomeHandler.addAppToHome(app) },
                            )
                        }
                    }

            val createAppState =
                    remember(getAppNickname, getAppTrigger, pinnedPackageNames) {
                        { app: AppInfo ->
                            AppState(
                                    hasNickname = !getAppNickname(app.packageName).isNullOrBlank(),
                                    hasTrigger = getAppTrigger(app.packageName)?.word?.isNotBlank() == true,
                                    isPinned = pinnedPackageNames.contains(app.launchCountKey()),
                                    showUninstall = !app.isSystemApp && app.userHandleId == null,
                                    showAppLabel = showAppLabels,
                                    isOverlayPresentation = isOverlayPresentation,
                            )
                        }
                    }

            rows.forEach { rowApps ->
                AppGridRow(
                        apps = rowApps,
                        rowItemWidth = rowItemWidth,
                        shortcutsByPackage = shortcutsByPackage,
                        iconPackPackage = iconPackPackage,
                        createAppActions = createAppActions,
                        createAppState = createAppState,
                        firstResultKey = firstResultKey,
                        oneHandedMode = oneHandedMode,
                        appIconShape = appIconShape,
                        themedIconsEnabled = themedIconsEnabled,
                        showWallpaperBackground = showWallpaperBackground,
                )
            }
        }
    }
}

@Composable
private fun AppGridRow(
        apps: List<AppInfo>,
        rowItemWidth: Dp,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        iconPackPackage: String?,
        createAppActions: (AppInfo) -> AppActions,
        createAppState: (AppInfo) -> AppState,
        firstResultKey: String?,
        oneHandedMode: Boolean,
        appIconShape: AppIconShape,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        apps.forEach { app ->
            key(app.launchCountKey()) {
                val appShortcuts = shortcutsByPackage[app.packageName].orEmpty()
                AppGridItem(
                        modifier = Modifier.width(rowItemWidth),
                        appInfo = app,
                        shortcuts = appShortcuts,
                        appActions = createAppActions(app),
                        appState = createAppState(app),
                        iconPackPackage = iconPackPackage,
                        isPredicted = app.launchCountKey() == firstResultKey,
                        oneHandedMode = oneHandedMode,
                        appIconShape = appIconShape,
                        themedIconsEnabled = themedIconsEnabled,
                        showWallpaperBackground = showWallpaperBackground,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
        modifier: Modifier = Modifier,
        appInfo: AppInfo,
        shortcuts: List<StaticShortcut>,
        appActions: AppActions,
        appState: AppState,
        iconPackPackage: String?,
        isPredicted: Boolean = false,
        oneHandedMode: Boolean = false,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
) {
    val context = LocalContext.current
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
    var showOptions by remember { mutableStateOf(false) }
    val appIconSize =
            remember(appState.isOverlayPresentation) {
                when (
                    if (appState.isOverlayPresentation) {
                        AppIconDisplayMode.OVERLAY
                    } else {
                        AppIconDisplayMode.REGULAR
                    }
                ) {
                    AppIconDisplayMode.OVERLAY -> 40.dp
                    AppIconDisplayMode.REGULAR -> RegularAppIconSize
                }
            }
    val indicatorAlpha = if (isPredicted) 1f else 0f

    Box(
            modifier = modifier.fillMaxWidth(),
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
                    .then(
                        if (showWallpaperBackground) {
                            Modifier.border(
                                width = DesignTokens.BorderWidth,
                                color = indicatorFillBase.copy(alpha = TopResultIndicatorBorderAlpha * indicatorAlpha),
                                shape = DesignTokens.ShapeLarge,
                            )
                        } else Modifier
                    )
                    .padding(
                        top = TopResultIndicatorTopPadding,
                        bottom = TopResultIndicatorBottomPadding,
                        start = if (isPredicted) TopResultIndicatorHorizontalPadding else 0.dp,
                        end = if (isPredicted) TopResultIndicatorHorizontalPadding else 0.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            AppIconSurface(
                    iconBitmap = iconResult.bitmap,
                    iconIsLegacy = iconResult.isLegacy,
                    monochromeData = iconResult.monochromeData,
                    appName = appInfo.appName,
                    onClick = appActions.onClick,
                    onLongClick = { showOptions = true },
                    appIconSize = appIconSize,
                    appIconShape = appIconShape,
                    hasCustomIconPack = iconPackPackage != null,
                    oneHandedMode = oneHandedMode,
                    themedIconsEnabled = themedIconsEnabled,
                    showWallpaperBackground = showWallpaperBackground,
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
                onShortcutClick = { shortcut -> launchStaticShortcut(context, shortcut) },
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onTriggerClick = appActions.onTriggerClick,
                onAddToHome = appActions.onAddToHome,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconSurface(
        iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
        iconIsLegacy: Boolean,
        monochromeData: androidx.compose.ui.graphics.ImageBitmap? = null,
        appName: String,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        appIconSize: Dp,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        hasCustomIconPack: Boolean = false,
        oneHandedMode: Boolean = false,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = LocalAppIsDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val useLightWallpaperShadow = showWallpaperBackground && !isDarkTheme
    val showThemedIcon = themedIconsEnabled && !hasCustomIconPack &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    val useSystemMaterialYouTones =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    (LocalDeviceDynamicColorsActive.current || LocalIsSystemWallpaperActive.current)
    val useWallpaperDerivedAccent = LocalWallpaperDynamicAccentActive.current
    val useCustomThemeDarkThemedIconColors =
            isDarkTheme && !useSystemMaterialYouTones && !useWallpaperDerivedAccent
    val themedIconBackground =
            if (useSystemMaterialYouTones) {
                Color(
                        androidx.core.content.ContextCompat.getColor(
                                context,
                                if (isDarkTheme) {
                                    android.R.color.system_accent2_800
                                } else {
                                    android.R.color.system_accent1_100
                                },
                        ),
                )
            } else if (useCustomThemeDarkThemedIconColors) {
                colorScheme.secondaryContainer
            } else {
                colorScheme.primaryContainer
            }
    val themedIconForeground =
            if (useSystemMaterialYouTones) {
                Color(
                        androidx.core.content.ContextCompat.getColor(
                                context,
                                if (isDarkTheme) {
                                    android.R.color.system_accent1_200
                                } else {
                                    android.R.color.system_accent1_600
                                },
                        ),
                )
            } else if (useCustomThemeDarkThemedIconColors) {
                colorScheme.onSecondaryContainer
            } else {
                colorScheme.primary
            }
    val themedIconContainerShape = CircleShape

    Surface(
            modifier = Modifier.requiredSize(DesignTokens.AppIconSize),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shape = DesignTokens.ShapeLarge,
    ) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .combinedClickable(
                                        onClick = {
                                            hapticConfirm(view)()
                                            onClick()
                                        },
                                        onLongClick = onLongClick,
                                ),
                contentAlignment = Alignment.Center,
        ) {
            if (showThemedIcon && monochromeData != null) {
                Box(
                        modifier = Modifier
                                .then(
                                        if (useLightWallpaperShadow) {
                                            Modifier.shadow(
                                                    elevation = DesignTokens.ElevationLevel2,
                                                    shape = themedIconContainerShape,
                                                    ambientColor =
                                                            Color.Black.copy(
                                                                    alpha = LightWallpaperAppIconShadowAmbientAlpha,
                                                            ),
                                                    spotColor =
                                                            Color.Black.copy(
                                                                    alpha = LightWallpaperAppIconShadowSpotAlpha,
                                                            ),
                                            )
                                        } else {
                                            Modifier
                                        },
                                )
                                .size(appIconSize)
                                .clip(themedIconContainerShape)
                                .background(themedIconBackground),
                        contentAlignment = Alignment.Center,
                ) {
                    Image(
                            bitmap = monochromeData,
                            contentDescription = stringResource(R.string.desc_launch_app, appName),
                            modifier = Modifier.requiredSize(appIconSize * ThemedMonochromeGlyphScale),
                            colorFilter = ColorFilter.tint(themedIconForeground),
                    )
                }
            } else if (iconBitmap != null) {
                val isUnsupportedThemedIcon = showThemedIcon && monochromeData == null
                if (isUnsupportedThemedIcon) {
                    Box(
                            modifier =
                                    Modifier.then(
                                                    if (useLightWallpaperShadow) {
                                                        Modifier.shadow(
                                                                elevation = DesignTokens.ElevationLevel2,
                                                                shape = themedIconContainerShape,
                                                                ambientColor =
                                                                        Color.Black.copy(
                                                                                alpha = LightWallpaperAppIconShadowAmbientAlpha,
                                                                        ),
                                                                spotColor =
                                                                        Color.Black.copy(
                                                                                alpha = LightWallpaperAppIconShadowSpotAlpha,
                                                                        ),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                            )
                                            .size(appIconSize)
                                            .clip(themedIconContainerShape)
                                            .background(themedIconBackground),
                            contentAlignment = Alignment.Center,
                    ) {
                        Image(
                                bitmap = iconBitmap,
                                contentDescription = stringResource(R.string.desc_launch_app, appName),
                                modifier = Modifier.requiredSize(appIconSize * UnsupportedThemedIconGlyphScale),
                                colorFilter = ColorFilter.tint(
                                        themedIconForeground.copy(alpha = UnsupportedThemedIconGlyphAlpha),
                                        BlendMode.SrcAtop,
                                ),
                        )
                    }
                } else {
                    val clipModifier =
                            when {
                                appIconShape == AppIconShape.CIRCLE ->
                                        Modifier.clip(CircleShape)
                                iconIsLegacy -> Modifier.clip(DesignTokens.ShapeLarge)
                                else -> Modifier
                            }
                    val bitmapShadowShape =
                            when {
                                appIconShape == AppIconShape.CIRCLE -> CircleShape
                                iconIsLegacy -> DesignTokens.ShapeLarge
                                else -> DesignTokens.ShapeLarge
                            }
                    Image(
                            bitmap = iconBitmap,
                            contentDescription =
                                    stringResource(
                                            R.string.desc_launch_app,
                                            appName,
                                    ),
                            modifier =
                                    Modifier.then(
                                                    if (useLightWallpaperShadow) {
                                                        Modifier.shadow(
                                                                elevation = DesignTokens.ElevationLevel2,
                                                                shape = bitmapShadowShape,
                                                                ambientColor =
                                                                        Color.Black.copy(
                                                                                alpha =
                                                                                        LightWallpaperAppIconShadowAmbientAlpha,
                                                                        ),
                                                                spotColor =
                                                                        Color.Black.copy(
                                                                                alpha =
                                                                                        LightWallpaperAppIconShadowSpotAlpha,
                                                                        ),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                            )
                                            .size(appIconSize)
                                            .then(clipModifier),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLabelText(
        appName: String,
        isOverlayPresentation: Boolean,
) {
    val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
    val labelColor = when (imageBackgroundIsDark) {
        true -> Color.White
        false -> Color.Black
        null -> MaterialTheme.colorScheme.onSurface
    }
    Spacer(
            modifier =
                    Modifier.height(
                            if (isOverlayPresentation) {
                                4.dp
                            } else {
                                DesignTokens.SpacingXSmall
                            },
                    ),
    )
    Text(
            text = appName,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
    )
}
