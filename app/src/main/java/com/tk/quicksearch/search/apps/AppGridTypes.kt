package com.tk.quicksearch.search.apps

import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.folders.AppFolderMember
import com.tk.quicksearch.search.folders.appFolderMemberKey
import com.tk.quicksearch.search.folders.ResolvedAppFolder
import com.tk.quicksearch.search.folders.shortcutGridKey

internal const val ROW_COUNT = 2
internal const val TabSlideOffsetPx = 64
internal const val SuggestionsEnterDurationMillis = 320
internal const val SuggestionTabInactiveAlpha = 0.34f
internal const val SuggestionTabSwipeThresholdPx = 48f
internal val AppGridRowSpacing = DesignTokens.SpacingXSmall
internal val AppGridWidthRoundingSlack = 1.dp
internal val RegularAppIconSize = DesignTokens.IconSizeXLarge - DesignTokens.SpacingXXSmall
internal val OverlayAppIconSurfaceSize = 52.dp
internal val OverlayAppIconSize = 36.dp
internal val TopResultIndicatorTopPadding = 0.dp
internal val TopResultIndicatorBottomPadding = DesignTokens.SpacingSmall
internal val TopResultIndicatorHorizontalPadding = DesignTokens.SpacingSmall
internal const val TopResultIndicatorBackgroundAlpha = 0.12f
internal const val LightWallpaperAppIconShadowAmbientAlpha = 0.28f
internal const val LightWallpaperAppIconShadowSpotAlpha = 0.45f
internal const val ThemedMonochromeGlyphScale = 1.42f
internal const val UnsupportedThemedIconGlyphScale = 0.62f
internal const val UnsupportedThemedIconGlyphAlpha = 0.72f
internal const val DraggedPinnedAppScale = 1.08f
internal const val DraggedPinnedAppAlpha = 0.92f
internal const val MergeSourcePinnedAppScale = 0.92f
// How long a dragged item must rest on another item's center before a drop creates a folder.
internal const val FolderMergeDwellMillis = 200L
// While merging is possible, reorders wait for the dragged item to rest this long on another
// cell, so the drag can pass over an item's edge toward its center without the item moving away.
internal const val MergeableReorderDelayMillis = 100L
// Movement that counts as no longer resting, restarting the reorder or merge wait.
internal val MergeableReorderRestSlop = 12.dp
// Share of a cell, centered on its item, where a dragged item starts merging instead of reordering
// (roughly where the two icons mostly overlap), and the larger share it can drift within once it is
// resting on that item. Resting anywhere else on the cell reorders.
internal const val FolderMergeEnterZoneFraction = 0.65f
internal const val FolderMergeStayZoneFraction = 0.8f
internal val AllAppsDialogIconSurfaceSize = DesignTokens.AppIconSize
internal val AllAppsDialogRowSpacing = DesignTokens.SpacingXXSmall
internal const val AllAppsDialogIconPrefetchParallelism = 4

internal enum class AppIconDisplayMode {
    OVERLAY,
    REGULAR,
}

internal sealed interface AppGridEntry {
    val key: String

    data class App(val app: AppInfo) : AppGridEntry {
        override val key: String get() = app.launchCountKey()
    }

    data class Shortcut(val shortcut: StaticShortcut) : AppGridEntry {
        override val key: String get() = shortcutGridKey(shortcut)
    }

    data class Folder(val folder: ResolvedAppFolder) : AppGridEntry {
        override val key: String get() = folder.folder.gridKey
    }

    /** An empty cell of the Pinned tab's grid, which dragged items can be dropped into. */
    data class Gap(override val key: String) : AppGridEntry
}

internal fun AppGridEntry.folderMemberKey(): String? =
        when (this) {
            is AppGridEntry.App -> appFolderMemberKey(app)
            is AppGridEntry.Shortcut -> appFolderMemberKey(shortcut)
            is AppGridEntry.Folder, is AppGridEntry.Gap -> null
        }

internal fun AppGridEntry.asFolderMember(): AppFolderMember? =
        when (this) {
            is AppGridEntry.App -> AppFolderMember.App(app)
            is AppGridEntry.Shortcut -> AppFolderMember.Shortcut(shortcut)
            is AppGridEntry.Folder, is AppGridEntry.Gap -> null
        }

/**
 * The cell under the dragged item's center. [offsetFractionX] and [offsetFractionY] are the
 * center's distance from that cell's item center, as a share of the item's width and height.
 */
internal data class DragCellHit(
        val index: Int,
        val isOnItem: Boolean,
        val offsetFractionX: Float,
        val offsetFractionY: Float,
) {
    fun isInMergeZone(zoneFraction: Float): Boolean =
            isOnItem &&
                    offsetFractionX <= zoneFraction / 2f &&
                    offsetFractionY <= zoneFraction / 2f
}

/** An item the dragged one is over, waiting for it to rest near ([anchorX], [anchorY]) to merge. */
internal data class MergeCandidate(
        val key: String,
        val anchorX: Float,
        val anchorY: Float,
)

/** A reorder waiting for the dragged item to rest on cell [index] near ([anchorX], [anchorY]). */
internal data class PendingReorder(
        val index: Int,
        val anchorX: Float,
        val anchorY: Float,
)

/**
 * An area outside a reorderable grid that takes dropped items, hit-tested against the dragged
 * item's center in root coordinates. [onActiveChange] reports an item being held or dragged.
 */
internal class AppGridDropTarget(
        val contains: (rootPosition: Offset) -> Boolean,
        val onActiveChange: (Boolean) -> Unit,
        val onHoverChange: (Boolean) -> Unit,
        val onDrop: (AppGridEntry) -> Unit,
)

internal data class PinnedAppDragState(
        val key: String,
        val startIndex: Int,
        val originIndex: Int,
        val originEntries: List<AppGridEntry>,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
)

internal data class AppSuggestionTab(
        val type: AppSuggestionTabType,
        val title: String,
        val apps: List<AppInfo>,
)

/** Data class containing all app actions to reduce parameter count in composables. */
internal data class AppActions(
        val onClick: () -> Unit,
        val onShortcutClick: (StaticShortcut) -> Unit,
        val onAppInfoClick: () -> Unit,
        val onUninstallClick: () -> Unit,
        val onHideApp: () -> Unit,
        val onDisableAppShortcut: (StaticShortcut) -> Unit,
        val onPinApp: () -> Unit,
        val onUnpinApp: () -> Unit,
        val onNicknameClick: () -> Unit,
        val onTriggerClick: () -> Unit,
        val onAddToHome: () -> Unit,
        val onOpenInSplitScreen: () -> Unit,
)

/** Data class containing app state information to reduce parameter count in composables. */
internal data class AppState(
        val hasNickname: Boolean,
        val hasTrigger: Boolean,
        val isPinned: Boolean,
        val showUninstall: Boolean,
        val showAppLabel: Boolean,
        val isOverlayPresentation: Boolean,
)
