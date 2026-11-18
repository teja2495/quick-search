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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
import com.tk.quicksearch.shared.util.hapticGestureThreshold
import com.tk.quicksearch.app.navigation.PageBackHandler
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.preferences.NotesPreferences
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

// Centers action buttons on the 45° point of the 20dp card-corner arc
// (20dp × (1 − 1/√2) ≈ 6dp in from each edge, minus the 15dp button radius).
internal val WidgetActionButtonCornerOffset = DesignTokens.SpacingSmall
internal val WidgetEditBorderWidth = 1.dp

// How far edit badges and resize handles can extend past a widget's top edge: the badge's 9dp
// corner offset, the edit lift scale on a tall widget, and the badge shadow. The edge-fade layer
// clips to the viewport, so anything past this is cut off.
private val WidgetEditOverhang = 16.dp

// How far the edit scale, side resize handles, and corner badges can extend past a widget's
// left/right edges. Must stay within ContentHorizontalPadding.
private val WidgetEditHorizontalOverhang = 16.dp
private val WidgetPanelBottomScrollSpace = 150.dp
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
    val notesPreferences = remember(appContext) { NotesPreferences(appContext) }
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
    var isQuickNoteEnabled by remember(appContext) { mutableStateOf(false) }
    var widgets by remember(appContext) { mutableStateOf(emptyList<PanelWidgetInfo>()) }
    var quickNoteWidget by remember(appContext) { mutableStateOf<PanelWidgetInfo?>(null) }
    var isQuickNoteFocused by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var layoutLocked by remember(appContext) { mutableStateOf<Boolean?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // An app-widget backup can restore Quick Search's layout metadata, but Android does not
    // restore the corresponding system-owned app-widget IDs. Keep those obsolete records from
    // reserving invisible cells ahead of widgets added on this device.
    var restoredWidgetIdsValidated by remember { mutableStateOf(false) }
    // Keep the panel slide independent of third-party RemoteViews inflation.
    var showHostedWidgets by remember { mutableStateOf(false) }
    val panelScrollState = rememberScrollState()
    // Window bounds of the scroll viewport, used to auto-scroll a widget back into view while it is
    // being dragged near the top/bottom edge.
    var scrollViewportTopPx by remember { mutableFloatStateOf(0f) }
    var scrollViewportHeightPx by remember { mutableIntStateOf(0) }

    fun persistWidgets(next: List<PanelWidgetInfo>) {
        if (next == widgets) return
        widgets = next
        preferences.setWidgets(next)
        HomePinnedWidgetsStore.publish(next)
    }

    fun pinWidgetToHome(widget: PanelWidgetInfo) {
        if (widget.isQuickNoteWidget() || widget.home != null) return
        persistWidgets(
            widgets.map { item ->
                if (item.appWidgetId == widget.appWidgetId) {
                    item.copy(home = defaultHomePlacement(item, widgets))
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
        next.firstOrNull { it.isQuickNoteWidget() }?.let { quickNote ->
            quickNoteWidget = quickNote
            preferences.setQuickNoteWidget(quickNote)
        }
        // Home-only widgets are intentionally absent from the panel grid, so preserve them while
        // saving a panel reorder, resize, or other layout update.
        persistWidgets(
            widgets.filterNot { it.isInPanel } + next.filterNot { it.isQuickNoteWidget() },
        )
    }

    val requestAddWidget =
        rememberWidgetAddFlow(appWidgetHost) { appWidgetId, provider, columnSpan, rowSpan ->
            widgets =
                preferences.addWidget(
                    appWidgetId = appWidgetId,
                    provider = provider.provider,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            HomePinnedWidgetsStore.publish(widgets)
            editingWidgetId = null
            showPicker = false
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
        val (storedWidgets, storedQuickNote, quickNoteEnabled) =
            withContext(Dispatchers.IO) {
                Triple(preferences.getWidgets(), preferences.getQuickNoteWidget(), notesPreferences.isQuickNoteEnabled())
            }
        // A restored ActivityResult can add a widget while this snapshot is loading.
        if (widgets.isEmpty()) widgets = storedWidgets
        val storedIds = storedWidgets.mapTo(HashSet()) { it.appWidgetId }
        val invalidIds = storedIds - appWidgetHost.preloadProviderInfo(storedIds)
        if (invalidIds.isNotEmpty()) {
            invalidIds.forEach(appWidgetHost::removeSnapshot)
            persistWidgets(widgets.filterNot { it.appWidgetId in invalidIds })
        }
        quickNoteWidget = storedQuickNote
        isQuickNoteEnabled = quickNoteEnabled
        restoredWidgetIdsValidated = true
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
    PageBackHandler(enabled = editingWidgetId == null && !showPicker, onBack = onNavigateToSearch)
    BackHandler(enabled = editingWidgetId != null || showPicker) {
        if (editingWidgetId != null) editingWidgetId = null else showPicker = false
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
                            showPicker = true
                        }
                    },
                    onExitEditMode = {
                        focusManager.clearFocus(force = true)
                        editingWidgetId = null
                    },
                )

                val panelWidgets = widgets.filter { it.isInPanel }
                val isQuickNoteSolo = isQuickNoteEnabled && panelWidgets.isEmpty()
                if (isQuickNoteSolo) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
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
                                        notesPreferences.setQuickNoteEnabled(false)
                                        isQuickNoteEnabled = false
                                        editingWidgetId = null
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                // Grow the scroll viewport upward and sideways (and pad its content
                                // back) so edit badges, handles, and the edit scale that overhang
                                // the grid aren't clipped by the scroll container or the edge-fade
                                // layer, without moving the layout.
                                .layout { measurable, constraints ->
                                    val overhangPx = WidgetEditOverhang.roundToPx()
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
                                                minHeight = constraints.minHeight + overhangPx,
                                                maxHeight =
                                                    if (constraints.hasBoundedHeight) {
                                                        constraints.maxHeight + overhangPx
                                                    } else {
                                                        constraints.maxHeight
                                                    },
                                            ),
                                        )
                                    layout(
                                        placeable.width - sideOverhangPx * 2,
                                        placeable.height - overhangPx,
                                    ) {
                                        placeable.place(-sideOverhangPx, -overhangPx)
                                    }
                                }
                                .onGloballyPositioned { coordinates ->
                                    scrollViewportTopPx = coordinates.positionInWindow().y
                                    scrollViewportHeightPx = coordinates.size.height
                                }
                                .widgetPanelEdgeFade(panelScrollState)
                                .verticalScroll(panelScrollState)
                                .padding(
                                    start = WidgetEditHorizontalOverhang,
                                    top = WidgetEditOverhang,
                                    end = WidgetEditHorizontalOverhang,
                                ),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                    ) {
                        val panelItems =
                            buildList {
                                if (isQuickNoteEnabled) quickNoteWidget?.let { add(it) }
                                addAll(panelWidgets)
                            }
                        if (panelItems.isNotEmpty() && restoredWidgetIdsValidated) {
                            WidgetPanelGrid(
                                widgets = panelItems,
                                showHostedWidgets = showHostedWidgets,
                                appWidgetManager = appWidgetManager,
                                appWidgetHost = appWidgetHost,
                                widgetContentAvailable = appWidgetHost.widgetContentAvailable,
                                editingWidgetId = editingWidgetId,
                                layoutEditingEnabled = layoutLocked == false,
                                density = density,
                                panelScrollState = panelScrollState,
                                viewportTopPx = scrollViewportTopPx,
                                viewportHeightPx = scrollViewportHeightPx,
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
                                onRemoveWidget = { widget ->
                                    if (widget.isQuickNoteWidget()) {
                                        notesPreferences.setQuickNoteEnabled(false)
                                        isQuickNoteEnabled = false
                                    } else {
                                        if (widget.home == null) {
                                            appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                                        } else {
                                            appWidgetHost.removeSnapshot(widget.appWidgetId)
                                        }
                                        persistWidgets(
                                            preferences.removeWidget(widget.appWidgetId),
                                        )
                                    }
                                    editingWidgetId = null
                                },
                                onConfigureWidget = { _, configureIntent ->
                                    runCatching { configureExistingLauncher.launch(configureIntent) }
                                    editingWidgetId = null
                                },
                                onPinWidgetToHome = ::pinWidgetToHome,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
                        }

                        Spacer(modifier = Modifier.height(WidgetPanelBottomScrollSpace))
                    }
                }
            }

            if (showPicker && restoredWidgetIdsValidated && layoutLocked == false) {
                WidgetPickerSheet(
                    appWidgetManager = appWidgetManager,
                    showQuickNote = !isQuickNoteEnabled,
                    onDismiss = { showPicker = false },
                    onAddQuickNote = {
                        notesPreferences.setQuickNoteEnabled(true)
                        isQuickNoteEnabled = true
                        showPicker = false
                    },
                    onSelectWidget = requestAddWidget,
                )
            }
        }
        }
    }
}
