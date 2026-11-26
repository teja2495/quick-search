package com.tk.quicksearch.widgetsPanel

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppLabelText
import com.tk.quicksearch.search.apps.RegularAppIconSize
import com.tk.quicksearch.search.apps.rememberPinnedGridDragModifier
import com.tk.quicksearch.search.apps.swipeGestures.swipeGestures
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.search.apps.speedBump.SpeedBump
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpLaunchWatcher
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpOverlay
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.folders.AppFolderMember
import com.tk.quicksearch.search.folders.AppGridFolderActions
import com.tk.quicksearch.search.folders.FolderContentsPopup
import com.tk.quicksearch.search.folders.FolderItemMenu
import com.tk.quicksearch.search.folders.FolderPreviewIcon
import com.tk.quicksearch.search.folders.FolderPreviewMemberIcon
import com.tk.quicksearch.search.folders.ResolvedAppFolder
import com.tk.quicksearch.search.folders.resolveAppFolders
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.components.ItemMenuRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlin.math.roundToInt

/** Placeholder provider stored for a folder tile, which is not a hosted app widget. */
private const val FOLDER_PANEL_PROVIDER = "app_folder"

/** Folder tiles take ids from here downward, clear of real widget ids and the Quick Note. */
private const val FIRST_FOLDER_PANEL_WIDGET_ID = -1000

private const val FolderMemberColumns = 4
private val FolderTileMaxIconSize = 56.dp
private val FolderMemberIconSize = 48.dp

internal val FolderTileGridSpec = WidgetGridSpec(minColumnSpan = 1, minRowSpan = 1)

internal fun isFolderPanelPage(appWidgetId: Int): Boolean = appWidgetId <= FIRST_FOLDER_PANEL_WIDGET_ID

/** True for panel pages that are not hosted app widgets: the Quick Note and folder tiles. */
internal fun isBuiltInPanelPage(appWidgetId: Int): Boolean =
    isQuickNotePage(appWidgetId) || isFolderPanelPage(appWidgetId)

/** The folder shown by this panel record, or null when it is not a folder tile. */
internal val PanelWidgetInfo.panelFolderId: String?
    get() = providerClassName.takeIf { providerPackage == FOLDER_PANEL_PROVIDER && isFolderPanelPage(appWidgetId) }

/**
 * Gives each Widgets panel folder a panel record, placed in the first free cell, and drops the
 * records of folders that left the panel (moved Home, deleted or dissolved).
 */
internal fun List<PanelWidgetInfo>.withPanelFolders(folderIds: List<String>): List<PanelWidgetInfo> {
    val wanted = folderIds.toSet()
    val kept = filterNot { widget -> widget.panelFolderId?.let { it !in wanted } == true }
    val present = kept.mapNotNullTo(HashSet()) { it.panelFolderId }
    val missing = folderIds.filterNot { it in present }
    if (missing.isEmpty()) return if (kept.size == size) this else kept
    var nextId = minOf(kept.minOfOrNull { it.appWidgetId } ?: 0, FIRST_FOLDER_PANEL_WIDGET_ID + 1) - 1
    return kept +
        missing.map { folderId ->
            PanelWidgetInfo(
                appWidgetId = nextId--,
                providerPackage = FOLDER_PANEL_PROVIDER,
                providerClassName = folderId,
                columnSpan = 1,
                rowSpan = 1,
            )
        }
}

/** What a folder tile in the panel can do; built from the [SearchViewModel]. */
internal class WidgetPanelFolderActions(
    val folderActions: AppGridFolderActions,
    val onMoveToHome: (folderId: String) -> Unit,
    val onDelete: (folderId: String) -> Unit,
    val launchApp: (AppInfo, android.content.Context) -> Unit,
    val launchShortcut: (com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut) -> Unit,
    /** The swipe action picker searches through the shared query. */
    val onQueryChange: (String) -> Unit,
)

@Composable
internal fun rememberWidgetPanelFolderActions(viewModel: SearchViewModel): WidgetPanelFolderActions =
    remember(viewModel) {
        WidgetPanelFolderActions(
            folderActions =
                AppGridFolderActions(
                    onCreateFolder = viewModel::createAppFolder,
                    onAddToFolder = viewModel::addToAppFolder,
                    onRemoveFromFolder = viewModel::removeFromAppFolder,
                    onUnpinFromFolder = viewModel::unpinFromAppFolder,
                    onReorderFolder = viewModel::reorderAppFolder,
                    onRenameFolder = viewModel::renameAppFolder,
                    onSetFolderColor = viewModel::setAppFolderColor,
                    onDeleteFolder = viewModel::deleteAppFolder,
                ),
            onMoveToHome = viewModel::moveAppFolderToHome,
            onDelete = viewModel::deleteWidgetsPanelAppFolder,
            launchApp = viewModel::launchApp,
            launchShortcut = viewModel::launchAppShortcut,
            onQueryChange = viewModel::onQueryChange,
        )
    }

/** Widgets panel folders resolved against the installed apps and enabled shortcuts. */
@Composable
internal fun rememberPanelFolders(uiState: SearchUiState): Map<String, ResolvedAppFolder> =
    remember(uiState.appFolders, uiState.allApps, uiState.allAppShortcuts, uiState.disabledAppShortcutIds) {
        resolveAppFolders(
            uiState.appFolders.filter { it.inWidgetsPanel },
            uiState.allApps,
            uiState.allAppShortcuts,
            uiState.disabledAppShortcutIds,
        ).associateBy { it.id }
    }

/** Everything the panel grid needs to draw folder tiles. */
internal class PanelFolderTiles(
    val folders: Map<String, ResolvedAppFolder>,
    val appearance: PanelFolderAppearance,
    val actions: WidgetPanelFolderActions,
)

/** Appearance shared by folder tiles and their opened popup, read from the Home settings. */
internal data class PanelFolderAppearance(
    val iconPackPackage: String?,
    val appIconShape: com.tk.quicksearch.search.apps.AppIconShape,
    val appIconSizeStep: Int,
    val showLabel: Boolean,
    val showWallpaperBackground: Boolean,
)

internal fun SearchUiState.panelFolderAppearance(): PanelFolderAppearance =
    PanelFolderAppearance(
        iconPackPackage = selectedIconPackPackage,
        appIconShape = appIconShape,
        appIconSizeStep = appIconSizeStep,
        showLabel = showAppLabels,
        showWallpaperBackground = showWallpaperBackground,
    )

/**
 * A folder tile on the panel grid. Tapping opens the folder. Holding it picks it up like a pinned
 * app: dragging moves it around the grid (while the layout is unlocked), and releasing it without
 * moving opens its menu (Swipe Up, Swipe Down, Move to Home Screen, Delete). A swipe runs the
 * folder's assigned swipe action, as on Home. In edit mode it can be
 * resized like a widget, and its remove button sends it back Home rather than deleting it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxScope.FolderPanelGridItem(
    widget: PanelWidgetInfo,
    folder: ResolvedAppFolder,
    appearance: PanelFolderAppearance,
    actions: WidgetPanelFolderActions,
    isEditing: Boolean,
    layoutEditingEnabled: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    onEdit: () -> Unit,
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onEditEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val horizontalSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val column = widget.column ?: 0
    val row = widget.row ?: 0
    val columnSpan = widget.columnSpan ?: 1
    val rowSpan = widget.rowSpan ?: 1
    val width = cellWidth * columnSpan + gap * (columnSpan - 1)
    val height = rowHeight * rowSpan + gap * (rowSpan - 1)
    val animatedX by animateDpAsState((cellWidth + gap) * column, WidgetLayoutMotion, label = "folderTileX")
    val animatedY by animateDpAsState((rowHeight + gap) * row, WidgetLayoutMotion, label = "folderTileY")
    val animatedWidth by animateDpAsState(width, WidgetLayoutMotion, label = "folderTileWidth")
    val animatedHeight by animateDpAsState(height, WidgetLayoutMotion, label = "folderTileHeight")
    val editScale by animateFloatAsState(
        targetValue = if (isEditing) WIDGET_EDIT_LIFT_SCALE else 1f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "folderTileEditScale",
    )
    val folderLabel = folder.name.ifBlank { stringResource(R.string.excluded_item_type_folder) }
    var showMenu by remember { mutableStateOf(false) }
    var isOpen by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    // Where the drag began and how far the finger has moved since, in pixels.
    var dragStartCell by remember { mutableStateOf(0 to 0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val dragModifier =
        rememberPinnedGridDragModifier(
            key = folder.id,
            onClick = { isOpen = true },
            onShowOptions = { showMenu = true },
            onLocalDraggingChange = { isDragging = it },
            onPinnedDragStart =
                {
                    dragStartCell = column to row
                    dragX = 0f
                    dragY = 0f
                    // Lifts the tile and lets the grid auto-scroll while it is dragged.
                    onEdit()
                }.takeIf { layoutEditingEnabled },
            onPinnedDrag = { dx, dy ->
                dragX += dx
                dragY += dy
                val (startColumn, startRow) = dragStartCell
                onMovePreview(
                    (startColumn + (dragX * horizontalSign / gridUnitWidthPx).roundToInt())
                        .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan),
                    (startRow + (dragY / gridUnitHeightPx).roundToInt()).coerceAtLeast(0),
                )
            },
            onPinnedDragEnd = {
                onInteractionEnd()
                onEditEnd()
            },
        )
    // Without layout editing there is no drag, so a plain long press opens the menu.
    val pressModifier =
        if (layoutEditingEnabled) {
            dragModifier
        } else {
            Modifier.combinedClickable(
                enabled = !isEditing,
                role = Role.Button,
                indication = ripple(color = homeTextColor()),
                interactionSource = null,
                onClickLabel = folderLabel,
                onLongClickLabel = stringResource(R.string.desc_show_options),
                onClick = {
                    hapticConfirm(view)()
                    isOpen = true
                },
                onLongClick = { showMenu = true },
            )
        }
    val iconSize =
        (RegularAppIconSize * UiPreferences.appIconSizeScale(appearance.appIconSizeStep))
            .coerceAtMost(FolderTileMaxIconSize)

    Box(
        modifier =
            Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .offset {
                    IntOffset(
                        x = with(density) { animatedX.roundToPx() },
                        y = with(density) { animatedY.roundToPx() },
                    )
                }
                .zIndex(if (isEditing) 1f else 0f)
                .graphicsLayer {
                    scaleX = editScale
                    scaleY = editScale
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .clip(AppShapes.large)
                    .swipeGestures(folder.folder.gridKey)
                    .then(pressModifier)
                    .semantics { contentDescription = folderLabel }
                    .padding(DesignTokens.SpacingXSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FolderPreviewIcon(
                members = folder.members,
                iconSize = iconSize,
                iconPackPackage = appearance.iconPackPackage,
                appIconShape = appearance.appIconShape,
                showWallpaperBackground = appearance.showWallpaperBackground,
                color = folder.color,
            )
            if (appearance.showLabel) AppLabelText(appName = folderLabel, isOverlayPresentation = false)
        }

        // A drag lifts the tile without showing the edit handles.
        AnimatedWidgetEditOverlay(visible = isEditing && !isDragging) {
            if (!layoutEditingEnabled) return@AnimatedWidgetEditOverlay
            WidgetEditOverlay(
                column = column,
                row = row,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                gridUnitWidthPx = gridUnitWidthPx,
                gridUnitHeightPx = gridUnitHeightPx,
                minColumnSpan = FolderTileGridSpec.minColumnSpan,
                minRowSpan = FolderTileGridSpec.minRowSpan,
                hasConfigure = false,
                canPinToHome = false,
                onMoveBy = onMoveBy,
                onMovePreview = onMovePreview,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = { actions.onMoveToHome(folder.id) },
                onConfigure = {},
                onPinToHome = {},
                onAddToStack = null,
            )
        }

        val dismissMenu = { showMenu = false }
        FolderItemMenu(
            folder = folder,
            expanded = showMenu,
            onDismiss = dismissMenu,
            onDelete = { actions.onDelete(folder.id) },
            rows =
                listOf(
                    ItemMenuRow(
                        label = stringResource(R.string.folder_move_to_home_screen),
                        icon = { Icon(imageVector = MaterialSymbols.Home, contentDescription = null) },
                        onClick = {
                            dismissMenu()
                            actions.onMoveToHome(folder.id)
                        },
                    ),
                ),
        )
    }

    if (isOpen) {
        PanelFolderPopup(
            folder = folder,
            appearance = appearance,
            actions = actions,
            onDismiss = { isOpen = false },
        )
    }
}

/**
 * The opened folder: the same popup as on Home, with an editable name and color, and its members
 * in a grid that launches them through the speed bump and app lock like Home does.
 */
@Composable
private fun PanelFolderPopup(
    folder: ResolvedAppFolder,
    appearance: PanelFolderAppearance,
    actions: WidgetPanelFolderActions,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var speedBumpApp by remember { mutableStateOf<AppInfo?>(null) }
    val launchApp: (AppInfo) -> Unit = { app ->
        if (SpeedBump.shouldShow(context, app.packageName)) {
            speedBumpApp = app
        } else {
            AppLockGate.runAfterUnlock(context, app.packageName, app.appName) {
                actions.launchApp(app, context)
            }
        }
    }
    FolderContentsPopup(
        folder = folder,
        actions = actions.folderActions,
        onDismiss = onDismiss,
    ) { _, dismiss ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            folder.members.chunked(FolderMemberColumns).forEach { rowMembers ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowMembers.forEach { member ->
                        PanelFolderMember(
                            member = member,
                            appearance = appearance,
                            onClick = {
                                dismiss()
                                when (member) {
                                    is AppFolderMember.App -> launchApp(member.app)
                                    is AppFolderMember.Shortcut ->
                                        AppLockGate.runAfterUnlock(
                                            context,
                                            member.shortcut.packageName,
                                            member.shortcut.appLabel,
                                        ) { actions.launchShortcut(member.shortcut) }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(FolderMemberColumns - rowMembers.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    speedBumpApp?.let { app ->
        SpeedBumpOverlay(
            appInfo = app,
            iconPackPackage = appearance.iconPackPackage,
            appIconShape = appearance.appIconShape,
            onOpen = {
                speedBumpApp = null
                AppLockGate.runAfterUnlock(context, app.packageName, app.appName) {
                    SpeedBumpLaunchWatcher.allowLaunchFromQuickSearch(app.packageName)
                    actions.launchApp(app, context)
                }
            },
            onCancel = { speedBumpApp = null },
        )
    }
}

@Composable
private fun PanelFolderMember(
    member: AppFolderMember,
    appearance: PanelFolderAppearance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        when (member) {
            is AppFolderMember.App -> member.app.appName
            is AppFolderMember.Shortcut ->
                com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName(member.shortcut)
        }
    Column(
        modifier =
            modifier
                .clip(AppShapes.large)
                .combinedClickable(
                    role = Role.Button,
                    indication = ripple(color = homeTextColor()),
                    interactionSource = null,
                    onClick = onClick,
                )
                .padding(vertical = DesignTokens.SpacingXSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FolderPreviewMemberIcon(
            member = member,
            size = FolderMemberIconSize,
            iconPackPackage = appearance.iconPackPackage,
            appIconShape = appearance.appIconShape,
        )
        AppLabelText(appName = label, isOverlayPresentation = false)
    }
}
