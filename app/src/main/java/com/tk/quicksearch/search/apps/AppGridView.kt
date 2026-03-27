package com.tk.quicksearch.search.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.shared.util.hapticConfirm

private const val ROW_COUNT = 2
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
        val onAddToHome: () -> Unit,
)

/** Data class containing app state information to reduce parameter count in composables. */
private data class AppState(
        val hasNickname: Boolean,
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
        getAppNickname: (String) -> String?,
        pinnedPackageNames: Set<String>,
        disabledShortcutIds: Set<String>,
        modifier: Modifier = Modifier,
        rowCount: Int = ROW_COUNT,
        iconPackPackage: String? = null,
        showAppLabels: Boolean = true,
        oneHandedMode: Boolean = false,
        isInitializing: Boolean = false,
        isOverlayPresentation: Boolean = false,
        startupPhase: StartupPhase = StartupPhase.COMPLETE,
        predictedTarget: PredictedSubmitTarget? = null,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
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
                            )
                    iconResult.bitmap != null
                }
            } else {
                true
            }

    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        val showAppGrid = apps.isNotEmpty() && areAppIconsLoaded
        if (isSearching) {
            if (showAppGrid) {
                AppGrid(
                        apps = apps,
                        isSearching = isSearching,
                        onAppClick = onAppClick,
                        onAppInfoClick = onAppInfoClick,
                        onUninstallClick = onUninstallClick,
                        onHideApp = onHideApp,
                        onPinApp = onPinApp,
                        onUnpinApp = onUnpinApp,
                        onNicknameClick = onNicknameClick,
                        getAppNickname = getAppNickname,
                        pinnedPackageNames = pinnedPackageNames,
                        shortcutsByPackage = shortcutsByPackage,
                        rowCount = rowCount,
                        iconPackPackage = iconPackPackage,
                        showAppLabels = showAppLabels,
                        oneHandedMode = oneHandedMode,
                        isOverlayPresentation = isOverlayPresentation,
                        predictedTarget = predictedTarget,
                        appIconShape = appIconShape,
                )
            }
        } else {
            val appGridVisibilityState = remember { MutableTransitionState(false) }
            appGridVisibilityState.targetState = showAppGrid
            AnimatedVisibility(
                    visibleState = appGridVisibilityState,
                    enter =
                            fadeIn(animationSpec = tween(durationMillis = 200)) +
                                    slideInVertically(
                                            animationSpec =
                                                    tween(durationMillis = 260),
                                            initialOffsetY = { it / 10 },
                                    ) +
                                    scaleIn(
                                            animationSpec = tween(durationMillis = 220),
                                            initialScale = 0.98f,
                                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 120)),
            ) {
                AppGrid(
                        apps = apps,
                        isSearching = isSearching,
                        onAppClick = onAppClick,
                        onAppInfoClick = onAppInfoClick,
                        onUninstallClick = onUninstallClick,
                        onHideApp = onHideApp,
                        onPinApp = onPinApp,
                        onUnpinApp = onUnpinApp,
                        onNicknameClick = onNicknameClick,
                        getAppNickname = getAppNickname,
                        pinnedPackageNames = pinnedPackageNames,
                        shortcutsByPackage = shortcutsByPackage,
                        rowCount = rowCount,
                        iconPackPackage = iconPackPackage,
                        showAppLabels = showAppLabels,
                        oneHandedMode = oneHandedMode,
                        isOverlayPresentation = isOverlayPresentation,
                        predictedTarget = predictedTarget,
                        appIconShape = appIconShape,
                )
            }
        }
    }
}

@Composable
private fun AppGrid(
        apps: List<AppInfo>,
        isSearching: Boolean,
        onAppClick: (AppInfo) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        rowCount: Int = ROW_COUNT,
        iconPackPackage: String?,
        showAppLabels: Boolean,
        oneHandedMode: Boolean,
        isOverlayPresentation: Boolean,
        predictedTarget: PredictedSubmitTarget?,
        appIconShape: AppIconShape,
) {
    val maxVisibleColumns = getAppGridColumns()
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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val horizontalSpacing = DesignTokens.SpacingMedium
        val rowItemWidth =
                if (columns <= 1) {
                    maxWidth
                } else {
                    ((maxWidth - (horizontalSpacing * (columns - 1))) / columns).coerceAtLeast(0.dp)
                }

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
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
                                    onAddToHome = { addToHomeHandler.addAppToHome(app) },
                            )
                        }
                    }

            val createAppState =
                    remember(getAppNickname, pinnedPackageNames) {
                        { app: AppInfo ->
                            AppState(
                                    hasNickname = !getAppNickname(app.packageName).isNullOrBlank(),
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
                        predictedTarget = predictedTarget,
                        appIconShape = appIconShape,
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
        predictedTarget: PredictedSubmitTarget?,
        appIconShape: AppIconShape,
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
                        isPredicted =
                                (predictedTarget as? PredictedSubmitTarget.App)?.let {
                                    it.packageName == app.packageName &&
                                            it.userHandleId == app.userHandleId
                                } == true,
                        appIconShape = appIconShape,
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
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
) {
    val context = LocalContext.current
    val iconResult =
            rememberAppIcon(
                    packageName = appInfo.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = appInfo.userHandleId,
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
                    AppIconDisplayMode.OVERLAY -> 44.dp
                    AppIconDisplayMode.REGULAR -> DesignTokens.IconSizeXLarge - 4.dp
                }
            }
    val indicatorAlpha by animateFloatAsState(
            targetValue = if (isPredicted) 1f else 0f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "topResultIndicatorAlpha",
    )

    Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f * indicatorAlpha),
                        shape = DesignTokens.ShapeLarge,
                    )
                    .padding(
                        horizontal = DesignTokens.SpacingSmall,
                        vertical = DesignTokens.SpacingXSmall,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            AppIconSurface(
                    iconBitmap = iconResult.bitmap,
                    iconIsLegacy = iconResult.isLegacy,
                    appName = appInfo.appName,
                    onClick = appActions.onClick,
                    onLongClick = { showOptions = true },
                    appIconSize = appIconSize,
                    appIconShape = appIconShape,
                    hasCustomIconPack = iconPackPackage != null,
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
                shortcuts = shortcuts,
                onShortcutClick = { shortcut -> launchStaticShortcut(context, shortcut) },
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onAddToHome = appActions.onAddToHome,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconSurface(
        iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
        iconIsLegacy: Boolean,
        appName: String,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        appIconSize: Dp,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        hasCustomIconPack: Boolean = false,
) {
    val view = LocalView.current
    Surface(
            modifier = Modifier.size(DesignTokens.AppIconSize),
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
            if (iconBitmap != null) {
                val clipModifier =
                        when {
                            appIconShape == AppIconShape.CIRCLE ->
                                    Modifier.clip(androidx.compose.foundation.shape.CircleShape)
                            iconIsLegacy -> Modifier.clip(DesignTokens.ShapeLarge)
                            else -> Modifier
                        }
                Image(
                        bitmap = iconBitmap,
                        contentDescription =
                                stringResource(
                                        R.string.desc_launch_app,
                                        appName,
                                ),
                        modifier = Modifier.size(appIconSize).then(clipModifier),
                )
            }
        }
    }
}

@Composable
private fun AppLabelText(
        appName: String,
        isOverlayPresentation: Boolean,
) {
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
    )
}
