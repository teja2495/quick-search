package com.tk.quicksearch.searchEngines.inline

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

/** Engine count at or below which the strip spaces engines evenly instead of scrolling icons. */
internal const val EVENLY_SPACED_ENGINE_MAX_COUNT = 6

/** Engine count at or below which each engine also shows its name. */
private const val LABELED_ENGINE_MAX_COUNT = 2

private val LABELED_ICON_SIZE = 18.dp
private val ICON_ONLY_ICON_SIZE = DesignTokens.IconSize
private val DIVIDER_HEIGHT = 16.dp
private val HIGHLIGHT_SHAPE = AppShapes.engineHighlight
private val HIGHLIGHT_HORIZONTAL_INSET = DesignTokens.SpacingXSmall
private val HIGHLIGHT_EXTRA_HEIGHT = 12.dp

/**
 * A single row of engines sharing the strip width equally, separated by subtle dividers. Engines
 * carry their name only while there are few enough of them for it to fit.
 */
@Composable
internal fun EvenlySpacedEngineRow(
    query: () -> String,
    enabledEngines: List<SearchTarget>,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    predictedTarget: PredictedSubmitTarget?,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    val predictedTargetId = (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId
    val showLabels = enabledEngines.size <= LABELED_ENGINE_MAX_COUNT
    val dividerColor = AppColors.InlineEngineDivider
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        enabledEngines.forEachIndexed { index, engine ->
            if (index > 0) {
                Box(
                    modifier =
                        Modifier
                            .width(DesignTokens.BorderWidth)
                            .height(DIVIDER_HEIGHT)
                            .background(dividerColor),
                )
            }
            key(engine.getId()) {
                EvenlySpacedEngineItem(
                    engine = engine,
                    query = query,
                    onSearchEngineClick = onSearchEngineClick,
                    onSearchEngineLongPress = onSearchEngineLongPress,
                    isPredicted = predictedTargetId == engine.getId(),
                    showLabel = showLabels,
                    appIconShape = appIconShape,
                    iconPackPackage = iconPackPackage,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Icon and "Search on" label for a strip with only one engine. The whole strip is the tap target
 * (see `SearchEngineContent`).
 */
@Composable
internal fun SingleEngineLabel(
    engine: SearchTarget,
    query: String,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineIconAndLabel(
            engine = engine,
            label =
                stringResource(
                    if (isLikelyWebUrl(query)) R.string.open_with_engine else R.string.search_on_engine,
                    engine.getDisplayName(),
                ),
            appIconShape = appIconShape,
            iconPackPackage = iconPackPackage,
        )
    }
}

@Composable
private fun EvenlySpacedEngineItem(
    engine: SearchTarget,
    query: () -> String,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    isPredicted: Boolean,
    showLabel: Boolean,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val highlightBackgroundColor = AppColors.InlineEngineHighlightBackground
    val highlightBorderColor = AppColors.InlineEngineHighlightBorder
    Row(
        modifier =
            modifier
                .then(
                    if (!isPredicted) {
                        Modifier
                    } else {
                        // Drawn past the item's height, into the strip padding, like the icon row's
                        // highlight, so the prediction doesn't grow the strip.
                        Modifier.drawBehind {
                            val insetPx = HIGHLIGHT_HORIZONTAL_INSET.toPx()
                            val extraHeightPx = HIGHLIGHT_EXTRA_HEIGHT.toPx()
                            val topLeft = Offset(insetPx, -extraHeightPx / 2f)
                            val highlightSize =
                                Size(size.width - (insetPx * 2), size.height + extraHeightPx)
                            val radius = HIGHLIGHT_SHAPE.topStart.toPx(highlightSize, this)
                            drawRoundRect(
                                color = highlightBackgroundColor,
                                topLeft = topLeft,
                                size = highlightSize,
                                cornerRadius = CornerRadius(radius, radius),
                            )
                            drawRoundRect(
                                color = highlightBorderColor,
                                topLeft = topLeft,
                                size = highlightSize,
                                cornerRadius = CornerRadius(radius, radius),
                                style = Stroke(width = DesignTokens.BorderWidth.toPx()),
                            )
                        }
                    },
                ).combinedClickable(
                    role = Role.Button,
                    onClick = {
                        hapticConfirm(view)()
                        onSearchEngineClick(query(), engine)
                    },
                    onLongClick = onSearchEngineLongPress,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLabel) {
            EngineIconAndLabel(
                engine = engine,
                label = engine.getDisplayName(),
                appIconShape = appIconShape,
                iconPackPackage = iconPackPackage,
            )
        } else {
            SearchTargetIcon(
                target = engine,
                iconSize = ICON_ONLY_ICON_SIZE,
                style = IconRenderStyle.ADVANCED,
                appIconShape = appIconShape,
                iconPackPackage = iconPackPackage,
            )
        }
    }
}

@Composable
private fun EngineIconAndLabel(
    engine: SearchTarget,
    label: String,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
) {
    SearchTargetIcon(
        target = engine,
        iconSize = LABELED_ICON_SIZE,
        style = IconRenderStyle.ADVANCED,
        appIconShape = appIconShape,
        iconPackPackage = iconPackPackage,
    )
    Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
