package com.tk.quicksearch.searchEngines.inline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.compact.SearchEngineCard
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

/** Corner radius shared by the inset strip and the bottom search bar it attaches to. */
private val INSET_CORNER_RADIUS = 28.dp

/**
 * Geometry shared by the bottom search bar and the inset engine strip that wraps around it. The
 * strip paints the card both of them live in, so the bar's insets and the distance the strip has to
 * reach down behind it are derived from the same numbers instead of being tuned separately.
 */
object InsetSearchBarGeometry {
    /**
     * Matches `TextFieldDefaults.MinHeight`. Only a starting point: a large font scale grows the
     * bar past it, and an overlap that falls short leaves the container's bottom edge drawing a
     * hairline inside the bar, so callers pass a measured overlap once the bar has been laid out.
     */
    private val BarHeight = 56.dp

    /** Lets the card sit slightly outside the regular content inset. */
    val ContainerHorizontalExtension = DesignTokens.SpacingXSmall

    /**
     * Cancels the search screen's horizontal content padding so the card reaches the screen edges.
     * Used while the keyboard is open: nothing reserves space for the gesture handle then, so the
     * resting inset would only read as an empty gap around the card.
     */
    private val FullBleedHorizontalExtension = DesignTokens.SpacingLarge

    /** Horizontal extension of the card, [fullBleedFraction] of the way from resting to full bleed. */
    fun containerHorizontalExtension(fullBleedFraction: Float): Dp =
        lerp(ContainerHorizontalExtension, FullBleedHorizontalExtension, fullBleedFraction)

    /** Pulls the bar in from the card's side edges. */
    val BarHorizontalInset = DesignTokens.SpacingSmall

    /**
     * Bar inset while the card is full bleed. The card grows outward then, so the bar widens a
     * little too rather than leaving a wider margin than the resting card has.
     */
    private val FullBleedBarHorizontalInset = 0.dp

    /** Bar inset [fullBleedFraction] of the way from resting to full bleed. */
    fun barHorizontalInset(fullBleedFraction: Float): Dp =
        lerp(BarHorizontalInset, FullBleedBarHorizontalInset, fullBleedFraction)

    /** Slight gap between the engine row and the top of the bar in the combined compact card. */
    val BarTopSpacing = DesignTokens.SpacingXXSmall

    /** Gap between the bottom of the bar and the bottom of the card. */
    val BarBottomSpacing = DesignTokens.SpacingMedium

    val ContainerOverlap = overlapFor(BarHeight)

    /** The distance the container has to reach down to cover a bar of [barHeight]. */
    fun overlapFor(barHeight: Dp): Dp =
        BarTopSpacing + barHeight.coerceAtLeast(BarHeight) + BarBottomSpacing

    /** Keeps the inset search bar as rounded as the standalone search bar. */
    val BarCornerRadius = DesignTokens.Spacing28
}

internal object SearchEngineSectionConstants {
    val ICON_SIZE = SearchTargetConstants.DEFAULT_ICON_SIZE
    val SPACING = 20.dp
    val ROW_SPACING = 10.dp
    val PREDICTION_HIGHLIGHT_HEIGHT_EXTRA = 12.dp
    val PREDICTION_HIGHLIGHT_WIDTH_EXTRA = 8.dp
    val PREDICTION_HIGHLIGHT_CONTENT_PADDING =
        (PREDICTION_HIGHLIGHT_WIDTH_EXTRA / 2) + DesignTokens.BorderWidth
    val COMPACT_TOP_DIVIDER_THICKNESS = DesignTokens.BorderWidth
    val SEARCH_ICON_SIZE = SearchTargetConstants.SEARCH_ICON_SIZE
    val HORIZONTAL_PADDING = SearchTargetConstants.HORIZONTAL_PADDING
    val VERTICAL_PADDING = SearchTargetConstants.VERTICAL_PADDING
    val SEARCH_ICON_SPACING = SearchTargetConstants.SEARCH_ICON_SPACING
    val TOOL_ICON_TEXT_SPACING = DesignTokens.SpacingSmall
    val TOOL_BUTTON_CORNER_RADIUS = 24.dp
    val TOOL_BUTTON_HORIZONTAL_PADDING = DesignTokens.SpacingLarge
    val TOOL_BUTTON_VERTICAL_PADDING = DesignTokens.SpacingMedium

    /**
     * Same radius as the search bar on every corner: the strip's bottom corners land exactly on the
     * bar's top corners, so tucking it behind the bar yields one continuous outline.
     */
    val INSET_CONTAINER_SHAPE = RoundedCornerShape(INSET_CORNER_RADIUS)

    /** Squares off the bottom corners as the card goes full bleed against the keyboard. */
    fun insetContainerShape(fullBleedFraction: Float): RoundedCornerShape {
        if (fullBleedFraction <= 0f) return INSET_CONTAINER_SHAPE
        val bottomRadius = lerp(INSET_CORNER_RADIUS, 0.dp, fullBleedFraction)
        return RoundedCornerShape(
            topStart = INSET_CORNER_RADIUS,
            topEnd = INSET_CORNER_RADIUS,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius,
        )
    }

    /** How far the container reaches down behind the bar it is attached to. */
    val INSET_CONTAINER_OVERLAP = InsetSearchBarGeometry.ContainerOverlap

    /** Trimmed row padding so the full engine row still fits inside the narrower inset strip. */
    val INSET_HORIZONTAL_PADDING = DesignTokens.SpacingSmall
}

/** Draws the inset container wider and behind the search bar without changing sibling placement. */
internal fun Modifier.attachToBottomSearchBar(
    overlap: Dp,
    horizontalExtension: Dp,
): Modifier =
    layout { measurable, constraints ->
        val horizontalExtensionPx = horizontalExtension.roundToPx()
        val attachedWidth = constraints.maxWidth + (horizontalExtensionPx * 2)
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = attachedWidth,
                    maxWidth = attachedWidth,
                ),
            )
        val overlapPx = overlap.roundToPx()
        layout(constraints.maxWidth, (placeable.height - overlapPx).coerceAtLeast(0)) {
            placeable.placeRelative(-horizontalExtensionPx, 0)
        }
    }

@Composable
private fun InlineCardEnterAnimation(
    content: @Composable () -> Unit,
) {
    val enterState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = enterState,
        enter =
            fadeIn(animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { fullHeight -> -(fullHeight / 5) },
                    animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                ),
    ) {
        content()
    }
}
/**
 * Composable section displaying search engine icons in a scrollable row.
 *
 * The section extends to screen edges by compensating for parent padding. Displays a fixed search
 * icon followed by scrollable engine icons.
 *
 * @param modifier Modifier for the section
 * @param query The current search query
 * @param enabledEngines List of enabled search engines to display
 * @param onSearchEngineClick Callback when a search engine is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineIconsSection(
    modifier: Modifier = Modifier,
    query: String,
    enabledEngines: List<SearchTarget>,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    externalScrollState: androidx.compose.foundation.lazy.LazyListState? = null,
    detectedShortcutTarget: SearchTarget? = null,
    onClearDetectedShortcut: () -> Unit = {},
    showWallpaperBackground: Boolean = false,
    showOverlayExpandChevron: Boolean = false,
    onOverlayExpandClick: (() -> Unit)? = null,
    isOverlayExpanded: Boolean = false,
    compactRowCount: Int = 1,
    predictedTarget: PredictedSubmitTarget? = null,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    iconPackPackage: String? = null,
    toolActionLabel: String? = null,
    toolActionIcon: ImageVector? = null,
    toolActionAppIconPackage: String? = null,
    onToolActionClick: (() -> Unit)? = null,
    showOnlyToolAction: Boolean = false,
    useInsetContainer: Boolean = false,
    insetOverlap: Dp = SearchEngineSectionConstants.INSET_CONTAINER_OVERLAP,
    insetFullBleedFraction: Float = 0f,
) {
    val hasToolAction = toolActionLabel != null && onToolActionClick != null
    if (enabledEngines.isEmpty() && detectedShortcutTarget == null && !hasToolAction) return

    val scrollState = externalScrollState ?: rememberLazyListState()

    // Match compact section background with the persistent search bar for visual consistency.
    val backgroundColor =
        if (useInsetContainer) {
            AppColors.getSearchBarBackground(showWallpaperBackground)
        } else {
            AppColors.getSearchEngineSectionBackground(showWallpaperBackground)
        }
    if (detectedShortcutTarget != null) {
        // Check if query starts with the shortcut and remove it
        // The shortcut corresponds to the detected engine
        InlineCardEnterAnimation {
            Box(
                modifier =
                    modifier.extendToScreenEdges().padding(
                        horizontal = DesignTokens.SpacingXLarge,
                        vertical = 8.dp,
                    ),
            ) {
                SearchEngineCard(
                    target = detectedShortcutTarget,
                    query = query,
                    onClick = { onSearchEngineClick(query, detectedShortcutTarget) },
                    onLongClick = onSearchEngineLongPress,
                    onClear = onClearDetectedShortcut,
                    showWallpaperBackground = showWallpaperBackground,
                    isPredicted =
                        (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId ==
                            detectedShortcutTarget.getId(),
                    appIconShape = appIconShape,
                    iconPackPackage = iconPackPackage,
                )
            }
        }
    } else {
        val compactTopDividerColor =
            if (showWallpaperBackground) {
                AppColors.WallpaperDivider
            } else {
                AppColors.Accent.copy(alpha = 0.22f)
            }
        Surface(
            modifier =
                modifier
                    .then(
                        if (useInsetContainer) {
                            Modifier.attachToBottomSearchBar(
                                overlap = insetOverlap,
                                horizontalExtension =
                                    InsetSearchBarGeometry.containerHorizontalExtension(
                                        insetFullBleedFraction,
                                    ),
                            )
                        } else {
                            Modifier.extendToScreenEdges()
                        },
                    )
                    .graphicsLayer {
                        shadowElevation = 0f
                    },
            color = backgroundColor,
            shape =
                if (useInsetContainer) {
                    SearchEngineSectionConstants.insetContainerShape(insetFullBleedFraction)
                } else {
                    RectangleShape
                },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom =
                                if (useInsetContainer) insetOverlap else 0.dp,
                        ),
            ) {
                if (!useInsetContainer) {
                    HorizontalDivider(
                        color = compactTopDividerColor,
                        thickness = SearchEngineSectionConstants.COMPACT_TOP_DIVIDER_THICKNESS,
                    )
                }
                if ((showOnlyToolAction || enabledEngines.isEmpty()) && hasToolAction) {
                    AnimatedVisibility(
                        visible = true,
                        enter =
                            fadeIn(animationSpec = tween(durationMillis = 180)) +
                                expandVertically(animationSpec = tween(durationMillis = 220)),
                        exit =
                            fadeOut(animationSpec = tween(durationMillis = 130)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 180)),
                    ) {
                        CompactToolActionContent(
                            label = toolActionLabel!!,
                            icon = toolActionIcon,
                            appIconPackage = toolActionAppIconPackage,
                            onClick = onToolActionClick!!,
                            addTopPadding = true,
                        )
                    }
                } else {
                    SearchEngineContent(
                        query = query,
                        useInsetContainer = useInsetContainer,
                        enabledEngines = enabledEngines,
                        scrollState = scrollState,
                        onSearchEngineClick = onSearchEngineClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        showOverlayExpandChevron = showOverlayExpandChevron,
                        onOverlayExpandClick = onOverlayExpandClick,
                        isOverlayExpanded = isOverlayExpanded,
                        compactRowCount = compactRowCount,
                        predictedTarget = predictedTarget,
                        appIconShape = appIconShape,
                        iconPackPackage = iconPackPackage,
                    )
                    AnimatedVisibility(
                        visible = hasToolAction,
                        enter =
                            fadeIn(animationSpec = tween(durationMillis = 180)) +
                                expandVertically(animationSpec = tween(durationMillis = 220)),
                        exit =
                            fadeOut(animationSpec = tween(durationMillis = 130)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 180)),
                    ) {
                        CompactToolActionContent(
                            label = toolActionLabel ?: return@AnimatedVisibility,
                            icon = toolActionIcon,
                            appIconPackage = toolActionAppIconPackage,
                            onClick = onToolActionClick ?: return@AnimatedVisibility,
                            addTopPadding = false,
                        )
                    }
                }
            }
        }
    }
}

/** Internal composable for the search engine section content. */
@Composable
private fun SearchEngineContent(
    query: String,
    useInsetContainer: Boolean,
    enabledEngines: List<SearchTarget>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    showOverlayExpandChevron: Boolean,
    onOverlayExpandClick: (() -> Unit)?,
    isOverlayExpanded: Boolean,
    compactRowCount: Int,
    predictedTarget: PredictedSubmitTarget?,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        if (useInsetContainer) {
                            SearchEngineSectionConstants.INSET_HORIZONTAL_PADDING
                        } else {
                            SearchEngineSectionConstants.HORIZONTAL_PADDING
                        },
                    vertical = SearchEngineSectionConstants.VERTICAL_PADDING,
                ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showOverlayExpandChevron) {
            OverlayExpandChevron(
                onOverlayExpandClick = onOverlayExpandClick,
                isOverlayExpanded = isOverlayExpanded,
            )
            Spacer(modifier = Modifier.width(SearchEngineSectionConstants.SEARCH_ICON_SPACING))
        }

        ScrollableEngineIcons(
            query = query,
            enabledEngines = enabledEngines,
            scrollState = scrollState,
            onSearchEngineClick = onSearchEngineClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            compactRowCount = compactRowCount,
            predictedTarget = predictedTarget,
            appIconShape = appIconShape,
            iconPackPackage = iconPackPackage,
        )
    }
}

@Composable
private fun CompactToolActionContent(
    label: String,
    icon: ImageVector?,
    appIconPackage: String?,
    onClick: () -> Unit,
    addTopPadding: Boolean,
) {
    val view = LocalView.current
    val appIconResult = if (appIconPackage != null) rememberAppIcon(appIconPackage) else null
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = SearchEngineSectionConstants.HORIZONTAL_PADDING,
                    end = SearchEngineSectionConstants.HORIZONTAL_PADDING,
                    top =
                        if (addTopPadding) {
                            SearchEngineSectionConstants.VERTICAL_PADDING
                        } else {
                            0.dp
                        },
                    bottom = SearchEngineSectionConstants.VERTICAL_PADDING,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(SearchEngineSectionConstants.TOOL_BUTTON_CORNER_RADIUS),
            color = AppColors.InlineEngineHighlightBackground,
            border = BorderStroke(DesignTokens.BorderWidth, AppColors.InlineEngineHighlightBorder),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            hapticConfirm(view)()
                            onClick()
                        },
                    ),
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = SearchEngineSectionConstants.TOOL_BUTTON_HORIZONTAL_PADDING,
                        vertical = SearchEngineSectionConstants.TOOL_BUTTON_VERTICAL_PADDING,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (appIconResult?.bitmap != null) {
                    Image(
                        bitmap = appIconResult.bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(SearchEngineSectionConstants.ICON_SIZE),
                    )
                    Spacer(modifier = Modifier.width(SearchEngineSectionConstants.TOOL_ICON_TEXT_SPACING))
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(SearchEngineSectionConstants.ICON_SIZE),
                    )
                    Spacer(modifier = Modifier.width(SearchEngineSectionConstants.TOOL_ICON_TEXT_SPACING))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OverlayExpandChevron(
    onOverlayExpandClick: (() -> Unit)?,
    isOverlayExpanded: Boolean,
) {
    val imageVector =
        if (isOverlayExpanded) {
            Icons.Rounded.ExpandLess
        } else {
            Icons.Rounded.ExpandMore
        }
    val contentDescription =
        if (isOverlayExpanded) {
            stringResource(R.string.desc_collapse)
        } else {
            stringResource(R.string.desc_expand)
        }
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier =
            Modifier
                .size(SearchEngineSectionConstants.SEARCH_ICON_SIZE)
                .then(
                    if (onOverlayExpandClick != null) {
                        Modifier.clickable(onClick = onOverlayExpandClick)
                    } else {
                        Modifier
                    },
                ),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Scrollable row of search engine icons. Calculates item width to fit 7 items per visible row on phones. */
@Composable
private fun ScrollableEngineIcons(
    query: String,
    enabledEngines: List<SearchTarget>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    compactRowCount: Int,
    predictedTarget: PredictedSubmitTarget?,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    val resolvedRowCount = compactRowCount.coerceIn(1, 2)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemsPerRow = calculateItemsPerRow()
        val itemWidthDp = calculateItemWidth(maxWidth, itemsPerRow)

        if (resolvedRowCount == 2) {
            val columns = buildTwoRowColumns(enabledEngines, itemsPerRow)
            val hasSecondRowItems = columns.any { it.bottom != null }
            val visibleRowCount = if (hasSecondRowItems) 2 else 1
            val predictedTargetId = (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId
            val hasPredictedItem = predictedTargetId != null && enabledEngines.any { it.getId() == predictedTargetId }
            val predictionHighlightExtraHeight =
                if (hasPredictedItem && visibleRowCount == 1) {
                    SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_HEIGHT_EXTRA
                } else {
                    0.dp
                }
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(SearchEngineSectionConstants.SPACING),
                contentPadding =
                    PaddingValues(
                        horizontal = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_CONTENT_PADDING,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            (SearchEngineSectionConstants.ICON_SIZE * visibleRowCount) +
                                (if (visibleRowCount == 2) SearchEngineSectionConstants.ROW_SPACING else 0.dp) +
                                predictionHighlightExtraHeight,
                        ),
            ) {
                rowItems(
                    items = columns,
                    key = { column ->
                        "${column.top?.getId().orEmpty()}|${column.bottom?.getId().orEmpty()}"
                    },
                ) { column ->
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(SearchEngineSectionConstants.ROW_SPACING),
                    ) {
                        column.top?.let { topEngine ->
                            SearchEngineIconItem(
                                engine = topEngine,
                                query = query,
                                iconSize = SearchEngineSectionConstants.ICON_SIZE,
                                itemWidth = itemWidthDp,
                                onSearchEngineClick = onSearchEngineClick,
                                onSearchEngineLongPress = onSearchEngineLongPress,
                                isPredicted =
                                    (predictedTarget as? PredictedSubmitTarget.SearchTarget)
                                        ?.targetId == topEngine.getId(),
                                appIconShape = appIconShape,
                                iconPackPackage = iconPackPackage,
                            )
                        }
                        column.bottom?.let { bottomEngine ->
                            SearchEngineIconItem(
                                engine = bottomEngine,
                                query = query,
                                iconSize = SearchEngineSectionConstants.ICON_SIZE,
                                itemWidth = itemWidthDp,
                                onSearchEngineClick = onSearchEngineClick,
                                onSearchEngineLongPress = onSearchEngineLongPress,
                                isPredicted =
                                    (predictedTarget as? PredictedSubmitTarget.SearchTarget)
                                        ?.targetId == bottomEngine.getId(),
                                appIconShape = appIconShape,
                                iconPackPackage = iconPackPackage,
                            )
                        }
                    }
                }
            }
        } else {
            LazyRow(
                state = scrollState,
                horizontalArrangement = Arrangement.spacedBy(SearchEngineSectionConstants.SPACING),
                contentPadding =
                    PaddingValues(
                        horizontal = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_CONTENT_PADDING,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowItems(
                    items = enabledEngines,
                    key = { engine -> engine.getId() },
                ) { engine ->
                    SearchEngineIconItem(
                        engine = engine,
                        query = query,
                        iconSize = SearchEngineSectionConstants.ICON_SIZE,
                        itemWidth = itemWidthDp,
                        onSearchEngineClick = onSearchEngineClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        isPredicted =
                            (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId ==
                                engine.getId(),
                        appIconShape = appIconShape,
                        iconPackPackage = iconPackPackage,
                    )
                }
            }
        }
    }
}

/**
 * Calculates the width for each search engine icon item. Formula: (available width - total spacing)
 * / number of items
 */
@Composable
private fun calculateItemWidth(maxWidth: androidx.compose.ui.unit.Dp, itemsPerRow: Int): androidx.compose.ui.unit.Dp {
    val totalSpacing = SearchEngineSectionConstants.SPACING * (itemsPerRow - 1)
    val totalHorizontalContentPadding = SearchEngineSectionConstants.PREDICTION_HIGHLIGHT_CONTENT_PADDING * 2
    return (maxWidth - totalSpacing - totalHorizontalContentPadding) / itemsPerRow
}

@Composable
private fun calculateItemsPerRow(): Int =
    when {
        isTablet() && isLandscape() -> 10
        isTablet() -> 8
        else -> 7
    }

private data class TwoRowColumn(
    val top: SearchTarget?,
    val bottom: SearchTarget?,
)

private fun buildTwoRowColumns(
    engines: List<SearchTarget>,
    itemsPerRow: Int,
): List<TwoRowColumn> {
    if (engines.isEmpty()) return emptyList()

    val pageSize = itemsPerRow * 2
    return buildList {
        engines.chunked(pageSize).forEach { page ->
            val topRow = page.take(itemsPerRow)
            val bottomRow = page.drop(itemsPerRow)
            val columnCount = maxOf(topRow.size, bottomRow.size)
            repeat(columnCount) { index ->
                add(
                    TwoRowColumn(
                        top = topRow.getOrNull(index),
                        bottom = bottomRow.getOrNull(index),
                    ),
                )
            }
        }
    }
}
