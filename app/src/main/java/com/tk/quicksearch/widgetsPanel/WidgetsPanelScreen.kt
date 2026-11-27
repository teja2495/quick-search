package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.theme.AppMotion
import android.appwidget.AppWidgetManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import com.tk.quicksearch.app.navigation.LocalPageSwipeController
import com.tk.quicksearch.app.navigation.LocalRootPageSettled
import com.tk.quicksearch.app.navigation.RootDestination
import com.tk.quicksearch.app.navigation.detectPageSwipe
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberResumeRefreshKey
import com.tk.quicksearch.shared.util.hapticGestureThreshold
import com.tk.quicksearch.app.navigation.PageBackHandler
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchScreen.LocalSharedPageWallpaper
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchScreenWallpaperLogic
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalHomeTextColorOverride
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.ImageAppearanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val WIDGET_PANEL_SWIPE_THRESHOLD_PX = 140f
internal val WidgetPanelGridRowHeight = 80.dp
internal val WidgetPanelGridGap = 8.dp
internal val WidgetResizeEdgeHitLong = 64.dp
internal val WidgetResizeEdgeHitShort = DesignTokens.MinTouchTarget
internal val WidgetResizeVisualLong = 32.dp
internal val WidgetResizeVisualShort = 8.dp
internal val WidgetActionButtonSize = 30.dp
internal val WidgetEditRingWidth = 2.dp

// Edit badges sit inside the widget: each 48dp touch target is flush with the corner, which puts
// the 30dp badge 9dp in from the top and side edges, over a scrim band (WidgetEditBadgeScrim).
internal val WidgetEditBadgeScrimHeight = DesignTokens.MinTouchTarget + DesignTokens.SpacingSmall
internal val WidgetEditBorderWidth = 1.dp

// How far edit badges and resize handles can extend past a widget's top or bottom edge: the
// badge's 9dp corner offset, the edit lift scale on a tall widget, and the badge shadow. The
// edge-fade layer clips to the viewport, so anything past this is cut off. The bottom overhang
// must stay within the screen's bottom padding (SpacingLarge). Each page keeps this much clear at
// its top and bottom.
private val WidgetEditOverhang = 16.dp

// How far the edit scale, side resize handles, and corner badges can extend past a widget's
// left/right edges. Must stay within ContentHorizontalPadding.
private val WidgetEditHorizontalOverhang = 16.dp
private val WidgetPanelPageFlingVelocity = 400.dp
private val WidgetPanelPageIndicatorHalfWidth = 3.dp
internal val WidgetLayoutMotion = AppMotion.settle<Dp>()

internal enum class ResizeEdge(
    val xSign: Int,
    val ySign: Int,
    val alignment: Alignment,
    val isHorizontalAxis: Boolean,
) {
    Top(0, -1, Alignment.TopCenter, isHorizontalAxis = false),
    Bottom(0, 1, Alignment.BottomCenter, isHorizontalAxis = false),
    Start(-1, 0, Alignment.CenterStart, isHorizontalAxis = true),
    End(1, 0, Alignment.CenterEnd, isHorizontalAxis = true),
}

/**
 * Widget host for the panel page. Owned above the page so the hosted widget views (and whether
 * each has content) outlive the page between entries, instead of every entry inflating them again
 * behind a loading placeholder.
 */
@Composable
internal fun rememberWidgetsPanelHost(): WidgetPanelHost {
    val appContext = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val host = remember(appContext) {
        WidgetPanelHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID, isPanelHost = true).apply { setSuspended(true) }
    }
    // Follow the lifecycle here, not in the page: the host keeps listening while the page is
    // closed, so it must still stop when the app goes to the background.
    DisposableEffect(host, lifecycle) {
        val stopFollowingLifecycle = host.listenWhileStarted(lifecycle)
        onDispose { stopFollowingLifecycle() }
    }
    DisposableEffect(host) {
        onDispose { host.release() }
    }
    return host
}

@Composable
internal fun WidgetsPanelScreen(
    onNavigateToSearch: () -> Unit,
    uiState: SearchUiState,
    appWidgetHost: WidgetPanelHost,
    modifier: Modifier = Modifier,
    folderActions: WidgetPanelFolderActions? = null,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val appContext = context.applicationContext
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pageSettled = LocalRootPageSettled.current
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }
    val sharedWallpaper = LocalSharedPageWallpaper.current
    val backdropDrawnByHost = sharedWallpaper != null
    val wallpaperState = sharedWallpaper ?: SearchScreenWallpaperLogic(
        state = uiState,
        allowUnblurredSystemBackdrop = false,
    )
    val imageBackgroundIsDark =
        remember(wallpaperState.imageBitmap, wallpaperState.usesWallpaperBackground) {
            if (wallpaperState.usesWallpaperBackground && wallpaperState.imageBitmap != null) {
                ImageAppearanceUtils.fromImageBitmap(wallpaperState.imageBitmap)?.isDark
            } else {
                null
            }
        }
    val effectiveBackgroundSource =
        if (wallpaperState.usesMonoThemeFallback) BackgroundSource.THEME else uiState.backgroundSource
    val effectiveAppTheme = if (wallpaperState.usesMonoThemeFallback) AppTheme.MONOCHROME else uiState.appTheme
    var widgets by remember(appContext) { mutableStateOf(emptyList<PanelWidgetInfo>()) }
    var isQuickNoteFocused by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var layoutLocked by remember(appContext) { mutableStateOf<Boolean?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var reorderStackOwnerId by rememberSaveable { mutableStateOf<Int?>(null) }
    // Set while the picker adds a page to this stack rather than a new widget to the panel.
    // Saved so a bind or configure screen that recreates the activity still adds to the stack.
    var stackTarget by rememberSaveable(stateSaver = WidgetStackTargetSaver) {
        mutableStateOf<Pair<Int, WidgetStackFit>?>(null)
    }
    // An app-widget backup can restore Quick Search's layout metadata, but Android does not
    // restore the corresponding system-owned app-widget IDs. Keep those obsolete records from
    // reserving invisible cells ahead of widgets added on this device.
    var restoredWidgetIdsValidated by remember { mutableStateOf(false) }
    // Keep the panel slide independent of third-party RemoteViews inflation.
    var showHostedWidgets by remember { mutableStateOf(false) }
    val panelScrollState = rememberScrollState()
    val pager = remember(panelScrollState) { WidgetPanelPagerState(panelScrollState) }
    val pageFling =
        remember(pager, density) { WidgetPanelPageFling(pager, with(density) { WidgetPanelPageFlingVelocity.toPx() }) }
    val scope = rememberCoroutineScope()
    var scrollViewportHeightPx by remember { mutableIntStateOf(0) }
    // Pages keep their height while the keyboard shrinks the viewport, so the layout doesn't reflow.
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val currentImeBottomPx by rememberUpdatedState(imeBottomPx)
    // What imePadding takes from the viewport (the navigation bar inset is already consumed above it).
    val imeShrinkPx = WindowInsets.ime.exclude(WindowInsets.navigationBars).getBottom(density)
    val currentImeShrinkPx by rememberUpdatedState(imeShrinkPx)

    fun persistWidgets(next: List<PanelWidgetInfo>) {
        if (next == widgets) return
        widgets = next
        preferences.setWidgets(next)
        HomePinnedWidgetsStore.publish(next)
    }

    /** Puts a widget just added to the panel on the page in view, or a new page, and shows it. */
    fun placeNewPanelWidget(
        added: List<PanelWidgetInfo>,
        appWidgetId: Int,
    ): List<PanelWidgetInfo> {
        val placed = added.withNewPanelWidgetPlaced(appWidgetId, pager.targetPage, pager.pageRows)
        if (placed != added) preferences.setWidgets(placed)
        val page = placed.firstOrNull { it.isInPanel && it.appWidgetId == appWidgetId }?.pageOrZero()
        if (page != null && page != pager.targetPage) scope.launch { pager.animateToPage(page) }
        return placed
    }

    // Moves the widget (or stack) out of the panel onto Home; the panel and Home never share one.
    fun moveWidgetToHome(widget: PanelWidgetInfo) {
        // Home has no Quick Note.
        if (widget.pageIds.any(::isBuiltInPanelPage)) return
        persistWidgets(
            widgets.map { item ->
                if (item.isOwner(widget.appWidgetId, onHome = false)) {
                    item.copy(isInPanel = false, column = null, row = null, panelPage = null, home = defaultHomePlacement(item, widgets))
                } else {
                    item
                }
            },
        )
        editingWidgetId = null
        AppMessages.show(context, R.string.widget_pinned_to_home)
    }

    fun persistPanelItems(next: List<PanelWidgetInfo>) {
        if (layoutLocked != false) return
        // Home-only widgets are intentionally absent from the panel grid, so preserve them while
        // saving a panel reorder, resize, or other layout update.
        persistWidgets(widgets.filterNot { it.isInPanel } + next)
    }

    fun removeStackPage(
        widget: PanelWidgetInfo,
        pageId: Int,
    ) {
        val next = preferences.removeStackPage(widget.appWidgetId, pageId)
        // Home widgets saved before the panel and Home kept separate instances can still share
        // an id with a panel widget; that id stays bound while Home shows it.
        if (!isBuiltInPanelPage(pageId) && !next.referencesWidgetId(pageId)) appWidgetHost.deleteAppWidgetId(pageId)
        persistWidgets(next)
    }

    val requestAddWidget =
        rememberWidgetAddFlow(appWidgetHost) { appWidgetId, provider, columnSpan, rowSpan ->
            val ownerId = stackTarget?.first
            widgets =
                if (ownerId != null && widgets.any { it.isOwner(ownerId, onHome = false) }) {
                    preferences.addToStack(ownerId, appWidgetId, provider.provider)
                } else {
                    placeNewPanelWidget(
                        preferences.addWidget(
                            appWidgetId = appWidgetId,
                            provider = provider.provider,
                            columnSpan = columnSpan,
                            rowSpan = rowSpan,
                        ),
                        appWidgetId,
                    )
                }
            HomePinnedWidgetsStore.publish(widgets)
            editingWidgetId = null
            showPicker = false
            stackTarget = null
        }

    val configureExistingLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { /* result intentionally ignored */ }

    DisposableEffect(appWidgetHost) {
        appWidgetHost.isScrollInProgressProvider = { panelScrollState.isScrollInProgress }
        appWidgetHost.setSuspended(!showHostedWidgets)
        onDispose {
            // The host outlives this page so its widget views are shown again on the next entry.
            appWidgetHost.detachSurface()
        }
    }

    // Flash stacks' page indicators each time the panel comes into view, so stacks are noticeable.
    val resumeKey = rememberResumeRefreshKey()
    var stackIndicatorRevealKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(pageSettled, resumeKey) {
        if (pageSettled) stackIndicatorRevealKey++
    }

    LaunchedEffect(appWidgetHost, pageSettled) {
        if (!pageSettled || showHostedWidgets) return@LaunchedEffect
        // Also handle direct entries from the shortcut or overlay, which bypass Home's IME hide.
        keyboardController?.hide()
        withFrameNanos { }
        appWidgetHost.setSuspended(false)
        showHostedWidgets = true
    }

    LaunchedEffect(appWidgetHost) {
        layoutLocked = withContext(Dispatchers.IO) { preferences.isLayoutLocked() }
        val storedWidgets = withContext(Dispatchers.IO) { preferences.getWidgets() }
        // A restored ActivityResult can add a widget while this snapshot is loading.
        if (widgets.isEmpty()) widgets = storedWidgets
        // The Quick Note is not a hosted widget, so it has no provider to validate.
        val storedIds = storedWidgets.flatMapTo(HashSet()) { it.pageIds }.filterNotTo(HashSet(), ::isBuiltInPanelPage)
        val invalidIds = storedIds - appWidgetHost.preloadProviderInfo(storedIds)
        if (invalidIds.isNotEmpty()) {
            invalidIds.forEach(appWidgetHost::removeSnapshot)
            persistWidgets(widgets.dropUnboundPages(invalidIds))
        }
        restoredWidgetIdsValidated = true
    }

    // The Search page, which hosts the swipe action picker, is disposed while the panel is shown.
    if (folderActions != null && pageSettled) {
        com.tk.quicksearch.search.apps.swipeGestures.AppSwipeGesturePickerHost(
            searchState = uiState,
            onQueryChange = folderActions.onQueryChange,
        )
    }

    // Folders moved to the panel get a tile; folders moved Home or deleted lose theirs.
    val panelFolders = rememberPanelFolders(uiState)
    val panelFolderIds = remember(uiState.appFolders) { uiState.appFolders.filter { it.inWidgetsPanel }.map { it.id } }
    LaunchedEffect(restoredWidgetIdsValidated, panelFolderIds) {
        if (restoredWidgetIdsValidated) persistWidgets(widgets.withPanelFolders(panelFolderIds))
    }

    // Rest on the page in view again after a resize, or after the keyboard scrolled a note into view.
    LaunchedEffect(pager.pageHeightPx, imeBottomPx == 0) {
        if (imeBottomPx == 0 && pager.pageHeightPx > 0 && pager.dragZones.isEmpty()) {
            pager.animateToPage(pager.targetPage.coerceAtMost(pager.pageCount - 1))
        }
    }
    LaunchedEffect(pager.pageCount) {
        if (pager.targetPage >= pager.pageCount && pager.dragZones.isEmpty()) pager.animateToPage(pager.pageCount - 1)
    }

    LaunchedEffect(appWidgetHost, panelScrollState) {
        snapshotFlow { panelScrollState.isScrollInProgress }
            .collect { inProgress ->
                if (inProgress) appWidgetHost.cancelAllPendingLongPresses()
            }
    }

    DisposableEffect(activity, showPicker) {
        val window = activity?.window
        val originalSoftInputMode = window?.attributes?.softInputMode

        if (showPicker && window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        onDispose {
            if (showPicker && window != null && originalSoftInputMode != null) {
                window.setSoftInputMode(originalSoftInputMode)
            }
        }
    }

    // Leaving the panel previews Search sliding in; closing the editor or picker stays a plain back.
    PageBackHandler(
        enabled = editingWidgetId == null && !showPicker && reorderStackOwnerId == null,
        onBack = onNavigateToSearch,
    )
    BackHandler(enabled = editingWidgetId != null || showPicker || reorderStackOwnerId != null) {
        if (reorderStackOwnerId != null) {
            reorderStackOwnerId = null
        } else if (showPicker) {
            showPicker = false
            stackTarget = null
        } else {
            editingWidgetId = null
        }
    }

    val pageSwipeController = LocalPageSwipeController.current
    val view = LocalView.current
    val latestOnNavigateToSearch by rememberUpdatedState(onNavigateToSearch)
    val swipeBackModifier =
        Modifier.pointerInput(pageSwipeController) {
            detectPageSwipe(
                controller = pageSwipeController,
                thresholdPx = WIDGET_PANEL_SWIPE_THRESHOLD_PX,
                targetFor = { direction -> RootDestination.Search.takeIf { direction < 0 } },
                onSwipe = { direction -> if (direction < 0) latestOnNavigateToSearch() },
                onThresholdCrossed = { hapticGestureThreshold(view, it) },
            )
        }

    val panelTapModifier =
        Modifier.pointerInput(editingWidgetId, focusManager) {
            detectTapGestures(
                onTap = {
                    focusManager.clearFocus(force = true)
                    editingWidgetId = null
                },
            )
        }

    // While a widget can be dropped on the previous page, the header gives way to that page's zone
    // and the panel's viewport reaches up over it, so the widget can be dragged into the zone.
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHiddenForDrag = WidgetPanelPageZone.Previous in pager.dragZones
    val dragTopReachPx =
        if (headerHiddenForDrag) headerHeightPx + with(density) { DesignTokens.SpacingSmall.roundToPx() } else 0

    CompositionLocalProvider(
        LocalImageBackgroundIsDark provides imageBackgroundIsDark,
        LocalHomeTextColorOverride provides uiState.homeTextColorOverride,
    ) {
        SettingsScreenBackground(
            appTheme = effectiveAppTheme,
            overlayThemeIntensity = uiState.overlayThemeIntensity,
            deviceThemeEnabled = uiState.deviceThemeEnabled,
            amoledThemeEnabled = uiState.amoledThemeEnabled,
            backgroundSource = effectiveBackgroundSource,
            wallpaperBitmap = wallpaperState.imageBitmap,
            wallpaperBackgroundAlpha = uiState.wallpaperBackgroundAlpha,
            wallpaperBlurRadius = uiState.wallpaperBlurRadius,
            useSystemWallpaperBackdrop = wallpaperState.usesSystemWallpaperBackdrop,
            drawBackground = !backdropDrawnByHost,
            modifier = modifier.fillMaxSize(),
        ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(swipeBackModifier)
                    .then(panelTapModifier)
                    .navigationBarsPadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .imePadding()
                        .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                        .padding(bottom = DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {

                WidgetsPanelHeader(
                    hidden = headerHiddenForDrag,
                    modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                    inEditMode = editingWidgetId != null,
                    layoutLocked = layoutLocked,
                    onLayoutLockChange = { locked ->
                        layoutLocked = locked
                        preferences.setLayoutLocked(locked)
                        if (locked) {
                            appWidgetHost.cancelAllPendingLongPresses()
                            editingWidgetId = null
                            showPicker = false
                        }
                    },
                    onAddWidget = {
                        if (layoutLocked == false) {
                            focusManager.clearFocus(force = true)
                            stackTarget = null
                            showPicker = true
                        }
                    },
                    onExitEditMode = {
                        focusManager.clearFocus(force = true)
                        editingWidgetId = null
                    },
                )

                val panelWidgets = widgets.filter { it.isInPanel }
                val soloQuickNote =
                    panelWidgets.singleOrNull()?.takeIf { it.isQuickNoteWidget() && it.stack.isEmpty() }
                if (soloQuickNote != null) {
                    BoxWithConstraints(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        val soloCellWidth =
                            (maxWidth - WidgetPanelGridGap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
                        // Wrap the card so the edit outline matches its height rather than the
                        // full remaining panel height.
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CompactQuickNoteWidget(
                                modifier = Modifier.fillMaxWidth().widgetEditAction(
                                    if (layoutLocked == false && editingWidgetId == null) {
                                        { editingWidgetId = QUICK_NOTE_PANEL_WIDGET_ID }
                                    } else null,
                                ),
                                fitContentHeight = true,
                                onFocusChanged = { isQuickNoteFocused = it },
                                onDragStart = if (layoutLocked == false) {
                                    { if (layoutLocked == false) editingWidgetId = QUICK_NOTE_PANEL_WIDGET_ID }
                                } else {
                                    null
                                },
                            )
                            if (editingWidgetId == QUICK_NOTE_PANEL_WIDGET_ID) {
                                QuickNoteEditOverlay(
                                    onRemove = {
                                        persistWidgets(
                                            preferences.removeStackPage(QUICK_NOTE_PANEL_WIDGET_ID, QUICK_NOTE_PANEL_WIDGET_ID),
                                        )
                                        editingWidgetId = null
                                    },
                                    onAddToStack = {
                                        focusManager.clearFocus(force = true)
                                        stackTarget =
                                            QUICK_NOTE_PANEL_WIDGET_ID to
                                            WidgetStackFit(
                                                columnSpan = soloQuickNote.columnSpan ?: WIDGET_PANEL_GRID_COLUMNS,
                                                rowSpan = soloQuickNote.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN,
                                                cellWidthDp = soloCellWidth.value,
                                            )
                                        editingWidgetId = null
                                        showPicker = true
                                    },
                                )
                            }
                        }
                    }
                } else {
                  Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                // Grow the scroll viewport vertically and sideways (and pad its content
                                // back) so edit badges, handles, and the edit scale that overhang
                                // the grid aren't clipped by the scroll container, without moving
                                // the layout. Each page is one viewport tall, with the overhang
                                // kept clear at its top and bottom.
                                .layout { measurable, constraints ->
                                    val overhangPx = WidgetEditOverhang.roundToPx()
                                    val topReachPx = overhangPx + dragTopReachPx
                                    val sideOverhangPx = WidgetEditHorizontalOverhang.roundToPx()
                                    val placeable =
                                        measurable.measure(
                                            constraints.copy(
                                                minWidth = constraints.minWidth + sideOverhangPx * 2,
                                                maxWidth =
                                                    if (constraints.hasBoundedWidth) {
                                                        constraints.maxWidth + sideOverhangPx * 2
                                                    } else {
                                                        constraints.maxWidth
                                                    },
                                                minHeight = constraints.minHeight + overhangPx + topReachPx,
                                                maxHeight =
                                                    if (constraints.hasBoundedHeight) {
                                                        constraints.maxHeight + overhangPx + topReachPx
                                                    } else {
                                                        constraints.maxHeight
                                                    },
                                            ),
                                        )
                                    layout(
                                        placeable.width - sideOverhangPx * 2,
                                        placeable.height - overhangPx - topReachPx,
                                    ) {
                                        placeable.place(-sideOverhangPx, -topReachPx)
                                    }
                                }
                                .onSizeChanged { size ->
                                    // Pages don't count the header area the viewport borrows mid-drag.
                                    val viewportHeight = size.height - dragTopReachPx
                                    scrollViewportHeightPx = viewportHeight
                                    if (currentImeBottomPx == 0 || pager.pageHeightPx == 0) {
                                        // The panel can open while the search keyboard is still up (or
                                        // closing). Measure pages at their keyboard-free height, or fewer
                                        // rows fit and the saved layout gets reflowed onto other pages.
                                        val pageHeight = viewportHeight + currentImeShrinkPx
                                        pager.pageHeightPx = pageHeight
                                        pager.pageRows =
                                            widgetPanelPageRows(
                                                pageHeightPx = pageHeight,
                                                reservedPx = with(density) { (WidgetEditOverhang * 2).roundToPx() },
                                                gridUnitHeightPx =
                                                    with(density) { (WidgetPanelGridRowHeight + WidgetPanelGridGap).toPx() },
                                                gapPx = with(density) { WidgetPanelGridGap.toPx() },
                                            )
                                    }
                                }
                                .verticalScroll(panelScrollState, flingBehavior = pageFling)
                                .padding(
                                    start = WidgetEditHorizontalOverhang,
                                    top = WidgetEditOverhang + with(density) { dragTopReachPx.toDp() },
                                    end = WidgetEditHorizontalOverhang,
                                ),
                    ) {
                        val panelItems = panelWidgets
                        if (panelItems.isNotEmpty() && restoredWidgetIdsValidated && pager.isReady) {
                            WidgetPanelGrid(
                                widgets = panelItems,
                                showHostedWidgets = showHostedWidgets,
                                appWidgetManager = appWidgetManager,
                                appWidgetHost = appWidgetHost,
                                widgetContentAvailable = appWidgetHost.widgetContentAvailable,
                                editingWidgetId = editingWidgetId,
                                layoutEditingEnabled = layoutLocked == false,
                                density = density,
                                pager = pager,
                                viewportHeightPx = scrollViewportHeightPx,
                                topInset = WidgetEditOverhang,
                                onPersist = ::persistPanelItems,
                                onSetEditingWidgetId = { id ->
                                    if (layoutLocked == false) editingWidgetId = id
                                },
                                onWidgetTouch = { widgetId ->
                                    when {
                                        editingWidgetId != null && editingWidgetId != widgetId -> {
                                            focusManager.clearFocus(force = true)
                                            editingWidgetId = null
                                            true
                                        }
                                        isQuickNoteFocused -> {
                                            focusManager.clearFocus(force = true)
                                            true
                                        }
                                        else -> false
                                    }
                                },
                                onQuickNoteFocusChanged = { isQuickNoteFocused = it },
                                onRemoveWidget = { widget, pageId ->
                                    removeStackPage(widget, pageId)
                                    editingWidgetId = null
                                },
                                onAddToStack = { widget, fit ->
                                    focusManager.clearFocus(force = true)
                                    stackTarget = widget.appWidgetId to fit
                                    editingWidgetId = null
                                    showPicker = true
                                },
                                onReorderStack = { widget ->
                                    focusManager.clearFocus(force = true)
                                    reorderStackOwnerId = widget.appWidgetId
                                    editingWidgetId = null
                                },
                                onConfigureWidget = { _, configureIntent ->
                                    runCatching { configureExistingLauncher.launch(configureIntent) }
                                    editingWidgetId = null
                                },
                                onPinWidgetToHome = ::moveWidgetToHome,
                                folderTiles =
                                    folderActions?.let {
                                        PanelFolderTiles(panelFolders, uiState.panelFolderAppearance(), it)
                                    },
                                modifier = Modifier.fillMaxWidth(),
                                stackIndicatorRevealKey = stackIndicatorRevealKey,
                            )
                        }
                    }
                    WidgetPanelPageIndicator(
                        pageCount = pager.pageCount,
                        currentPage = pager.targetPage,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                // Centered in the screen's side padding, clear of the widgets.
                                .offset(x = DesignTokens.ContentHorizontalPadding / 2 + WidgetPanelPageIndicatorHalfWidth),
                    )
                  }
                }
            }

            WidgetPanelPageZones(pager)

            if (showPicker && restoredWidgetIdsValidated && layoutLocked == false) {
                WidgetPickerSheet(
                    appWidgetManager = appWidgetManager,
                    showQuickNote =
                        !widgets.containsQuickNote() &&
                            stackTarget?.let { (_, fit) -> fit.columnSpan >= WIDGET_PANEL_GRID_COLUMNS } != false,
                    stackFit = stackTarget?.second,
                    onDismiss = {
                        showPicker = false
                        stackTarget = null
                    },
                    onAddQuickNote = {
                        val ownerId = stackTarget?.first
                        persistWidgets(
                            if (ownerId != null && widgets.any { it.isOwner(ownerId, onHome = false) }) {
                                preferences.addQuickNoteToStack(ownerId)
                            } else {
                                placeNewPanelWidget(preferences.addQuickNote(), QUICK_NOTE_PANEL_WIDGET_ID)
                            },
                        )
                        editingWidgetId = null
                        showPicker = false
                        stackTarget = null
                    },
                    onSelectWidget = requestAddWidget,
                )
            }

            val reorderOwner = reorderStackOwnerId?.let { id -> widgets.firstOrNull { it.isOwner(id, onHome = false) } }
            if (reorderOwner != null && reorderOwner.stack.isNotEmpty()) {
                WidgetStackReorderSheet(
                    pageIds = reorderOwner.pageIds,
                    appWidgetHost = appWidgetHost,
                    onReorder = { ordered ->
                        persistWidgets(preferences.reorderStack(reorderOwner.appWidgetId, ordered))
                        reorderStackOwnerId = ordered.first()
                    },
                    onRemove = { pageId ->
                        removeStackPage(reorderOwner, pageId)
                        // Removing the first page hands the stack to the next one. A stack left with
                        // one page is a plain widget, which closes the drawer below.
                        reorderStackOwnerId =
                            reorderOwner.pageIds.firstOrNull { it != pageId }
                                ?.let { remaining -> widgets.filter { it.isInPanel }.stackOwnerOf(remaining)?.appWidgetId }
                    },
                    onDismiss = { reorderStackOwnerId = null },
                )
            }
            // The stack was removed or broken up while the sheet was open.
            LaunchedEffect(reorderOwner?.stack?.isNotEmpty(), restoredWidgetIdsValidated) {
                if (reorderStackOwnerId != null && restoredWidgetIdsValidated && reorderOwner?.stack.isNullOrEmpty()) {
                    reorderStackOwnerId = null
                }
            }
        }
        }
    }
}
