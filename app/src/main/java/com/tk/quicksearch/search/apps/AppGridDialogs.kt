package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.apps.swipeGestures.appSwipeGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.hasNotificationDot
import com.tk.quicksearch.search.apps.privateSpace.privateSpaceItems
import com.tk.quicksearch.search.apps.privateSpace.rememberPrivateSpaceController
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.shared.util.hapticConfirm

@Composable
internal fun AllAppsDrawer(
        title: String,
        apps: List<AppInfo>,
        onDismiss: () -> Unit,
        onAppClick: (AppInfo) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onDisableAppShortcut: (StaticShortcut) -> Unit = {},
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        onOpenInSplitScreen: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        phoneColumnOverride: Int,
        appIconSizeStep: Int,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        notificationDotKeys: Set<String> = emptySet(),
) {
    val dialogColumns = getAppGridColumns(phoneColumnOverride)
    val context = LocalContext.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
    val privateSpace = rememberPrivateSpaceController()
    val privateSpaceState by privateSpace.state.collectAsState()
    // Load every icon in the background up front so they are already cached when scrolled to.
    LaunchedEffect(apps, iconPackPackage, appIconShape) {
        prefetchAppIconRequests(
                context = context.applicationContext,
                requests = apps.map { AppIconRequest(it.packageName, it.userHandleId) },
                iconPackPackage = iconPackPackage,
                forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                parallelism = AllAppsDialogIconPrefetchParallelism,
        )
    }
    AppPickerDrawer(
            title = title,
            onDismiss = onDismiss,
    ) {
        val gridState = rememberLazyGridState()
        var scrollbarDragFraction by remember { mutableStateOf<Float?>(null) }
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(dialogColumns),
                    modifier =
                            Modifier
                                    .fillMaxWidth()
                                    .padding(
                                            end =
                                                    (LazyGridScrollbarTouchWidth - AllAppsDialogSeekEdgeNudge)
                                                            .coerceAtLeast(DesignTokens.SpacingSmall),
                                    ),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AllAppsDialogRowSpacing),
            ) {
                items(
                        items = apps,
                        key = { app -> app.launchCountKey() },
                ) { app ->
                    AllAppsDialogGridItem(
                            app = app,
                            appIconSizeStep = appIconSizeStep,
                            iconPackPackage = iconPackPackage,
                            appIconShape = appIconShape,
                            notificationDotKeys = notificationDotKeys,
                            onClick = { onAppClick(app) },
                            shortcuts = shortcutsByPackage[app.packageName].orEmpty(),
                            appActions =
                                    AppActions(
                                            onClick = { onAppClick(app) },
                                            onShortcutClick = onAppShortcutClick,
                                            onAppInfoClick = { onAppInfoClick(app) },
                                            onUninstallClick = { onUninstallClick(app) },
                                            onHideApp = { onHideApp(app) },
                                            onDisableAppShortcut = onDisableAppShortcut,
                                            onPinApp = { onPinApp(app) },
                                            onUnpinApp = { onUnpinApp(app) },
                                            onNicknameClick = { onNicknameClick(app) },
                                            onTriggerClick = { onTriggerClick(app) },
                                            onAddToHome = { addToHomeHandler.addAppToHome(app) },
                                            onOpenInSplitScreen = { onOpenInSplitScreen(app) },
                                    ),
                            appState =
                                    AppState(
                                            hasNickname =
                                                    !getAppNickname(app.launchCountKey())
                                                            .isNullOrBlank(),
                                            hasTrigger =
                                                    getAppTrigger(app.launchCountKey())
                                                            ?.word
                                                            ?.isNotBlank() == true,
                                            isPinned =
                                                    pinnedPackageNames.contains(
                                                            app.launchCountKey(),
                                                    ),
                                            showUninstall =
                                                    !app.isSystemApp &&
                                                            app.packageName != context.packageName,
                                            showAppLabel = true,
                                            isOverlayPresentation = false,
                                    ),
                    )
                }
                // Private Space apps get no options menu: pinning, nicknames, and the
                // like would persist them outside the locked profile.
                privateSpaceItems(
                        state = privateSpaceState,
                        onLockedChange = privateSpace::setLocked,
                ) { app ->
                    AllAppsDialogGridItem(
                            app = app,
                            appIconSizeStep = appIconSizeStep,
                            iconPackPackage = iconPackPackage,
                            appIconShape = appIconShape,
                            notificationDotKeys = emptySet(),
                            onClick = {
                                onDismiss()
                                privateSpace.launch(context, app)
                            },
                    )
                }
            }
            AllAppsScrollLetterPopup(
                    apps = apps,
                    gridState = gridState,
                    scrollbarDragFraction = scrollbarDragFraction,
            )
            LazyGridVerticalScrollbar(
                    state = gridState,
                    modifier =
                            Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = AllAppsDialogSeekEdgeNudge),
                    onDragFractionChange = { scrollbarDragFraction = it },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AllAppsDialogGridItem(
        app: AppInfo,
        appIconSizeStep: Int,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        notificationDotKeys: Set<String>,
        onClick: () -> Unit,
        shortcuts: List<StaticShortcut> = emptyList(),
        // Null for apps without an options menu or swipe gestures (Private Space).
        appActions: AppActions? = null,
        appState: AppState? = null,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }
    val sizeScale = remember(appIconSizeStep) { UiPreferences.appIconSizeScale(appIconSizeStep) }
    val iconSize = remember(sizeScale) { RegularAppIconSize * sizeScale }
    val iconSurfaceSize = remember(sizeScale) { AllAppsDialogIconSurfaceSize * sizeScale }
    val iconResult =
            rememberAppIcon(
                    packageName = app.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = app.userHandleId,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
            )
    Box(
            modifier =
                    Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
                modifier =
                        Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                        onClick = {
                                            hapticConfirm(view)()
                                            onClick()
                                        },
                                        onLongClick =
                                                if (app.isArchived || appActions == null || appState == null) null
                                                else ({ showOptions = true }),
                                )
                                .padding(
                                        horizontal = DesignTokens.SpacingXSmall,
                                        vertical = DesignTokens.SpacingXSmall,
                                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Box(
                    modifier =
                            Modifier.size(iconSurfaceSize).then(
                                    if (appActions != null) Modifier.appSwipeGestures(app) else Modifier,
                            ),
                    contentAlignment = Alignment.Center,
            ) {
                iconResult.bitmap?.let { icon ->
                    Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                    )
                } ?: Icon(
                        imageVector = MaterialSymbols.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize),
                )
                AppNotificationDot(visible = app.hasNotificationDot(notificationDotKeys))
            }
            Text(
                    text = app.appName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
            )
        }

        if (appActions == null || appState == null) return@Box
        AppItemDropdownMenu(
                expanded = showOptions,
                onDismiss = { showOptions = false },
                isPinned = appState.isPinned,
                showUninstall = appState.showUninstall,
                hasNickname = appState.hasNickname,
                hasTrigger = appState.hasTrigger,
                shortcuts = shortcuts,
                appInfo = app,
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

@Composable
internal fun AppSuggestionTabStrip(
        tabs: List<AppSuggestionTab>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
) {
    val activeColor = homeTextColor()
    val inactiveColor = activeColor.copy(alpha = SuggestionTabInactiveAlpha)
    val leftTab = tabs.getOrNull(selectedIndex - 1)
    val rightTab = tabs.getOrNull(selectedIndex + 1)
    // With the card layout off the strip sits right under a divider, so it gets room to breathe.
    val compactLayout = !LocalSearchCardLayoutEnabled.current

    Row(
            modifier =
                    Modifier
                            .fillMaxWidth(0.86f)
                            .padding(
                                    top = if (compactLayout) DesignTokens.SpacingSmall else 0.dp,
                                    bottom = if (compactLayout) DesignTokens.SpacingMedium else DesignTokens.SpacingXSmall,
                            ),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        EdgeSuggestionTabLabel(
                title = leftTab?.title.orEmpty(),
                color = inactiveColor,
                alignment = TextAlign.Start,
                onClick = {
                    if (selectedIndex > 0) onSelectedIndexChange(selectedIndex - 1)
                },
        )
        Text(
                text = tabs[selectedIndex].title,
                modifier = Modifier.weight(1f),
                color = activeColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
        )
        EdgeSuggestionTabLabel(
                title = rightTab?.title.orEmpty(),
                color = inactiveColor,
                alignment = TextAlign.End,
                onClick = {
                    if (selectedIndex < tabs.lastIndex) onSelectedIndexChange(selectedIndex + 1)
                },
        )
    }
}

@Composable
internal fun RowScope.EdgeSuggestionTabLabel(
        title: String,
        color: Color,
        alignment: TextAlign,
        onClick: () -> Unit,
) {
    Text(
            text = title,
            modifier =
                    Modifier
                            .weight(1f)
                            .clickable(onClick = onClick),
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = alignment,
    )
}

internal fun fillSuggestionGridApps(
        primaryApps: List<AppInfo>,
        fallbackApps: List<AppInfo>,
        minItems: Int,
): List<AppInfo> {
    if (primaryApps.size >= minItems) return primaryApps

    val seen = LinkedHashSet<String>(primaryApps.size + fallbackApps.size)
    val result = ArrayList<AppInfo>(minItems)
    primaryApps.forEach { app ->
        if (seen.add(app.launchCountKey())) {
            result.add(app)
        }
    }
    fallbackApps.forEach { app ->
        if (result.size >= minItems) return@forEach
        if (seen.add(app.launchCountKey())) {
            result.add(app)
        }
    }
    return result
}

internal fun replaceSuggestionAppsWithPinned(
        pinnedApps: List<AppInfo>,
        suggestedApps: List<AppInfo>,
        slotCount: Int,
): List<AppInfo> {
    val seen = LinkedHashSet<String>(slotCount)
    return (pinnedApps + suggestedApps)
            .asSequence()
            .filter { seen.add(it.launchCountKey()) }
            .take(slotCount)
            .toList()
}
