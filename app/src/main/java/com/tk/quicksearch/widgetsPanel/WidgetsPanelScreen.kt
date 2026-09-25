package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import android.widget.Toast
import androidx.core.os.bundleOf
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.searchScreen.SearchScreenWallpaperLogic
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalHomeTextColorOverride
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.ImageAppearanceUtils
import android.util.SizeF
import kotlin.math.roundToInt

private const val WIDGET_PANEL_SWIPE_THRESHOLD_PX = 140f
internal val WidgetPanelGridRowHeight = 80.dp
internal val WidgetPanelGridGap = 8.dp
internal val WidgetResizeEdgeHitLong = 64.dp
internal val WidgetResizeEdgeHitShort = 32.dp
internal val WidgetResizeVisualLong = 32.dp
internal val WidgetResizeVisualShort = 8.dp
internal val WidgetActionButtonSize = 30.dp
internal val WidgetEditRingWidth = 2.dp

// Centers action buttons on the 45° point of the 20dp card-corner arc
// (20dp × (1 − 1/√2) ≈ 6dp in from each edge, minus the 15dp button radius).
internal val WidgetActionButtonCornerOffset = 9.dp
internal val WidgetEditBorderWidth = 1.dp

// How far edit badges and resize handles can extend past a widget's top edge.
private val WidgetEditOverhang = 8.dp
private val WidgetPanelBottomScrollSpace = 150.dp
internal val WidgetLayoutMotion =
    spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

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

@Composable
fun WidgetsPanelScreen(
    onNavigateToSearch: () -> Unit,
    uiState: SearchUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val appContext = context.applicationContext
    val packageManager = context.packageManager
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    val appWidgetHost = remember(appContext) { WidgetPanelHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }
    val notesPreferences = remember(appContext) { NotesPreferences(appContext) }
    val wallpaperState = SearchScreenWallpaperLogic(state = uiState)
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
    var isQuickNoteEnabled by remember(appContext) {
        mutableStateOf(notesPreferences.isQuickNoteEnabled())
    }

    var widgets by remember { mutableStateOf(preferences.getWidgets()) }
    var quickNoteWidget by remember { mutableStateOf(preferences.getQuickNoteWidget()) }
    var isQuickNoteFocused by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // An app-widget backup can restore Quick Search's layout metadata, but Android does not
    // restore the corresponding system-owned app-widget IDs. Keep those obsolete records from
    // reserving invisible cells ahead of widgets added on this device.
    var restoredWidgetIdsValidated by remember { mutableStateOf(false) }
    val widgetContentAvailable = remember { mutableStateMapOf<Int, Boolean>() }
    // Keep the first panel frame independent of third-party RemoteViews. Some providers perform
    // expensive work while their host view is created; doing that during navigation blocks the
    // whole panel from appearing.
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

    fun discardInvalidRestoredWidgets() {
        val validWidgets = widgets.filter { appWidgetManager.getAppWidgetInfo(it.appWidgetId) != null }
        if (validWidgets != widgets) {
            persistWidgets(validWidgets)
        }
        restoredWidgetIdsValidated = true
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
        Toast.makeText(context, R.string.widget_pinned_to_home, Toast.LENGTH_SHORT).show()
    }

    fun persistPanelItems(next: List<PanelWidgetInfo>) {
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
        appWidgetHost.onWidgetContentChanged = { appWidgetId, hasContent ->
            widgetContentAvailable[appWidgetId] = hasContent
        }
        appWidgetHost.startListeningShared()
        onDispose {
            appWidgetHost.release()
        }
    }

    LaunchedEffect(appWidgetHost) {
        // Let the host begin listening and the panel draw once before creating provider views.
        withFrameNanos { }
        showHostedWidgets = true
    }

    LaunchedEffect(appWidgetManager) {
        discardInvalidRestoredWidgets()
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

    BackHandler {
        when {
            editingWidgetId != null -> editingWidgetId = null
            showPicker -> showPicker = false
            else -> onNavigateToSearch()
        }
    }

    val swipeBackModifier =
        Modifier.pointerInput(onNavigateToSearch) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount -> totalHorizontalDrag += dragAmount },
                onDragEnd = {
                    if (totalHorizontalDrag <= -WIDGET_PANEL_SWIPE_THRESHOLD_PX) {
                        onNavigateToSearch()
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
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
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {

                WidgetsPanelHeader(
                    inEditMode = editingWidgetId != null,
                    onAddWidget = {
                        focusManager.clearFocus(force = true)
                        showPicker = true
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
                                modifier = Modifier.fillMaxWidth(),
                                fitContentHeight = true,
                                onFocusChanged = { isQuickNoteFocused = it },
                                onDragStart = {
                                    editingWidgetId = QUICK_NOTE_PANEL_WIDGET_ID
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
                                // Grow the scroll viewport upward (and pad its content back down)
                                // so edit badges and handles that overhang the top row aren't
                                // clipped by the scroll container, without moving the layout.
                                .layout { measurable, constraints ->
                                    val overhangPx = WidgetEditOverhang.roundToPx()
                                    val placeable =
                                        measurable.measure(
                                            constraints.copy(
                                                minHeight = constraints.minHeight + overhangPx,
                                                maxHeight =
                                                    if (constraints.hasBoundedHeight) {
                                                        constraints.maxHeight + overhangPx
                                                    } else {
                                                        constraints.maxHeight
                                                    },
                                            ),
                                        )
                                    layout(placeable.width, placeable.height - overhangPx) {
                                        placeable.place(0, -overhangPx)
                                    }
                                }
                                .onGloballyPositioned { coordinates ->
                                    scrollViewportTopPx = coordinates.positionInWindow().y
                                    scrollViewportHeightPx = coordinates.size.height
                                }
                                .verticalScroll(panelScrollState)
                                .padding(top = WidgetEditOverhang),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                    ) {
                        val panelItems =
                            buildList {
                                if (isQuickNoteEnabled) add(quickNoteWidget)
                                addAll(panelWidgets)
                            }
                        if (panelItems.isNotEmpty() && showHostedWidgets && restoredWidgetIdsValidated) {
                            WidgetPanelGrid(
                                widgets = panelItems,
                                appWidgetManager = appWidgetManager,
                                appWidgetHost = appWidgetHost,
                                widgetContentAvailable = widgetContentAvailable,
                                editingWidgetId = editingWidgetId,
                                density = density,
                                panelScrollState = panelScrollState,
                                viewportTopPx = scrollViewportTopPx,
                                viewportHeightPx = scrollViewportHeightPx,
                                onPersist = ::persistPanelItems,
                                onSetEditingWidgetId = { id -> editingWidgetId = id },
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
                                packageManager = packageManager,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
                        }

                        Spacer(modifier = Modifier.height(WidgetPanelBottomScrollSpace))
                    }
                }
            }

            if (showPicker) {
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
