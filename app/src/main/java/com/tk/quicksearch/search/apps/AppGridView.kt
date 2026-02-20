package com.tk.quicksearch.search.apps

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.tk.quicksearch.search.core.AppIconSizeOption
import com.tk.quicksearch.search.data.AppShortcutRepository
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.launchStaticShortcut
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.predictedSubmitHighlight
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.getAppGridColumns
import com.tk.quicksearch.util.hapticConfirm

private const val ROW_COUNT = 2

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
)

@Composable
fun AppGridView(
        apps: List<AppInfo>,
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
        appIconSizeOption: AppIconSizeOption = AppIconSizeOption.MEDIUM,
        oneHandedMode: Boolean = false,
        isInitializing: Boolean = false,
        predictedTarget: PredictedSubmitTarget? = null,
) {
    val context = LocalContext.current
    val shortcutRepository = remember(context) { AppShortcutRepository(context) }
    var shortcuts by remember { mutableStateOf<List<StaticShortcut>>(emptyList()) }
    val shortcutsByPackage =
            remember(shortcuts, disabledShortcutIds) {
                shortcuts
                        .asSequence()
                        .filterNot { shortcut ->
                            disabledShortcutIds.contains(shortcutKey(shortcut))
                        }
                        .groupBy { it.packageName }
            }

    LaunchedEffect(shortcutRepository) {
        val cached = shortcutRepository.loadCachedShortcuts()
        if (cached != null) {
            shortcuts = cached
        }

        val loaded = runCatching { shortcutRepository.loadStaticShortcuts() }.getOrNull()
        if (loaded != null) {
            shortcuts = loaded
        }
    }

    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(top = 2.dp)
                            .animateContentSize(
                                    animationSpec =
                                            if (isInitializing) {
                                                // Instant update during initialization to prevent
                                                // "growing" animation on startup
                                                snap()
                                            } else {
                                                spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow,
                                                )
                                            },
                            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (apps.isEmpty()) {
            Box {}
        } else {
            AppGrid(
                    apps = apps,
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
                    appIconSizeOption = appIconSizeOption,
                    oneHandedMode = oneHandedMode,
                    predictedTarget = predictedTarget,
            )
        }
    }
}

@Composable
private fun AppGrid(
        apps: List<AppInfo>,
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
        appIconSizeOption: AppIconSizeOption,
        oneHandedMode: Boolean,
        predictedTarget: PredictedSubmitTarget?,
) {
    val columns = getAppGridColumns()
    val rows =
            remember(apps, oneHandedMode, columns) {
                // Show all available apps, chunked into rows of the appropriate column count
                val chunked = apps.chunked(columns)
                if (oneHandedMode) chunked.reversed() else chunked
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
                        )
                    }
                }

        rows.forEach { rowApps ->
            AppGridRow(
                    apps = rowApps,
                    getAppNickname = getAppNickname,
                    pinnedPackageNames = pinnedPackageNames,
                    shortcutsByPackage = shortcutsByPackage,
                    iconPackPackage = iconPackPackage,
                    appIconSizeOption = appIconSizeOption,
                    createAppActions = createAppActions,
                    createAppState = createAppState,
                    predictedTarget = predictedTarget,
            )
        }
    }
}

@Composable
private fun AppGridRow(
        apps: List<AppInfo>,
        getAppNickname: (String) -> String?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        iconPackPackage: String?,
        appIconSizeOption: AppIconSizeOption,
        createAppActions: (AppInfo) -> AppActions,
        createAppState: (AppInfo) -> AppState,
        predictedTarget: PredictedSubmitTarget?,
) {
    val columns = getAppGridColumns()
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        repeat(columns) { columnIndex ->
            val app = apps.getOrNull(columnIndex)
            if (app != null) {
                val appShortcuts = shortcutsByPackage[app.packageName].orEmpty()
                AppGridItem(
                        modifier = Modifier.weight(1f),
                        appInfo = app,
                        shortcuts = appShortcuts,
                        appActions = createAppActions(app),
                        appState = createAppState(app),
                        iconPackPackage = iconPackPackage,
                        appIconSizeOption = appIconSizeOption,
                        isPredicted =
                                (predictedTarget as? PredictedSubmitTarget.App)?.let {
                                    it.packageName == app.packageName &&
                                            it.userHandleId == app.userHandleId
                                } == true,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
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
        appIconSizeOption: AppIconSizeOption,
        isPredicted: Boolean = false,
) {
    val context = LocalContext.current
    val iconResult =
            rememberAppIcon(
                    packageName = appInfo.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = appInfo.userHandleId,
            )
    var showOptions by remember { mutableStateOf(false) }
    val placeholderLabel =
            remember(appInfo.appName) {
                appInfo.appName.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
            }
    val appIconSize =
            remember(appIconSizeOption) {
                when (appIconSizeOption) {
                    AppIconSizeOption.SMALL -> 44.dp
                    AppIconSizeOption.MEDIUM -> DesignTokens.IconSizeXLarge
                    AppIconSizeOption.BIG -> 60.dp
                }
            }

    Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            AppIconSurface(
                    iconBitmap = iconResult.bitmap,
                    iconIsLegacy = iconResult.isLegacy,
                    placeholderLabel = placeholderLabel,
                    appName = appInfo.appName,
                    onClick = appActions.onClick,
                    onLongClick = { showOptions = true },
                    isPredicted = isPredicted,
                    appIconSize = appIconSize,
            )
            if (appState.showAppLabel) {
                AppLabelText(appInfo.appName)
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
        placeholderLabel: String,
        appName: String,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        appIconSize: Dp,
        isPredicted: Boolean = false,
) {
    val view = LocalView.current
    val predictedIconHorizontalInset =
            if (isPredicted && appIconSize >= 60.dp) {
                DesignTokens.SpacingXSmall
            } else {
                0.dp
            }
    Surface(
            modifier =
                    Modifier.size(DesignTokens.AppIconSize)
                            .predictedSubmitHighlight(
                                    isPredicted = isPredicted,
                                    shape = DesignTokens.ShapeLarge,
                            ),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shape = DesignTokens.ShapeLarge,
    ) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(horizontal = predictedIconHorizontalInset)
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
                Image(
                        bitmap = iconBitmap,
                        contentDescription =
                                stringResource(
                                        R.string.desc_launch_app,
                                        appName,
                                ),
                        modifier =
                                Modifier.size(appIconSize)
                                        .then(
                                                if (iconIsLegacy) {
                                                    Modifier.clip(DesignTokens.ShapeLarge)
                                                } else {
                                                    Modifier
                                                },
                                        ),
                )
            } else {
                Text(
                        text = placeholderLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppLabelText(appName: String) {
    Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
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
