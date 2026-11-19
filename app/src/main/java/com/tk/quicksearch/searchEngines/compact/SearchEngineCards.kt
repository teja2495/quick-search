package com.tk.quicksearch.searchEngines.compact

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.shared.util.isLikelyWebUrl
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardStyleOverrides
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.hapticConfirm

/**
 * Section displaying search engine cards when there are no search results.
 * Shows all enabled search engines as individual cards with icons.
 */
@Composable
fun NoResultsSearchEngineCards(
    query: String,
    enabledEngines: List<SearchTarget>,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onCustomizeClick: () -> Unit,
    onSearchEngineLongPress: (() -> Unit)? = null,
    showCustomizeCard: Boolean = true,
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
    showWallpaperBackground: Boolean = false,
    predictedTarget: PredictedSubmitTarget? = null,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    iconPackPackage: String? = null,
) {
    val currentQuery = rememberUpdatedState(query)
    val actionLabelResId =
        if (isLikelyWebUrl(query)) {
            R.string.open_with_engine
        } else {
            R.string.search_on_engine
        }
    // Reverse the engine list when results are at the bottom
    val orderedEngines =
        if (isReversed) {
            enabledEngines.reversed()
        } else {
            enabledEngines
        }

    val shouldShowCustomizeCard = showCustomizeCard && enabledEngines.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        // When reversed (results at bottom), show customize card at the top
        if (isReversed && shouldShowCustomizeCard) {
            CustomizeSearchEnginesCard(
                onClick = onCustomizeClick,
                showWallpaperBackground = showWallpaperBackground,
            )
        }

        orderedEngines.forEach { engine ->
            key(engine.getId()) {
                val onEngineClick =
                    remember(engine, onSearchEngineClick) {
                        { onSearchEngineClick(currentQuery.value, engine) }
                    }
                SearchTargetCard(
                    target = engine,
                    actionLabelResId = actionLabelResId,
                    onClick = onEngineClick,
                    onLongClick = onSearchEngineLongPress,
                    showWallpaperBackground = showWallpaperBackground,
                    isPredicted =
                        (predictedTarget as? PredictedSubmitTarget.SearchTarget)?.targetId ==
                            engine.getId(),
                    appIconShape = appIconShape,
                    iconPackPackage = iconPackPackage,
                )
            }
        }

        // When not reversed, show customize card at the bottom
        if (!isReversed && shouldShowCustomizeCard) {
            CustomizeSearchEnginesCard(
                onClick = onCustomizeClick,
                showWallpaperBackground = showWallpaperBackground,
            )
        }
    }
}

/**
 * Individual search engine card with icon and name.
 */
@Composable
fun SearchEngineCard(
    target: SearchTarget,
    query: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
    onClear: (() -> Unit)? = null,
    isPredicted: Boolean = false,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    iconPackPackage: String? = null,
) {
    val view = LocalView.current
    val targetName = target.getDisplayName()
    val actionLabelResId =
        if (isLikelyWebUrl(query)) {
            R.string.open_with_engine
        } else {
            R.string.search_on_engine
        }
    SearchResultCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SearchResultCardDefaults.shape)
                .predictedSubmitHighlight(
                    isPredicted = isPredicted,
                    shape = SearchResultCardDefaults.shape,
                    opaqueCardTopResultBorder = true,
                )
                .combinedClickable(
                    role = Role.Button,
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                    onLongClickLabel = stringResource(R.string.customize_search_engines),
                    onLongClick = onLongClick,
                ),
        showWallpaperBackground = showWallpaperBackground,
        styleOverrides = SearchResultCardStyleOverrides(showBorder = false),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SearchTargetConstants.CARD_HORIZONTAL_PADDING,
                        vertical = SearchTargetConstants.CARD_VERTICAL_PADDING,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            SearchTargetIcon(
                target = target,
                iconSize = SearchTargetConstants.DEFAULT_ICON_SIZE,
                style = IconRenderStyle.SIMPLE,
                appIconShape = appIconShape,
                iconPackPackage = iconPackPackage,
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))

            Text(
                text = stringResource(actionLabelResId, targetName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (onClear != null) {
                AppIconButton(
                    onClick = onClear,
                    modifier = Modifier.size(DesignTokens.ButtonHeightSmall),
                    tooltip = stringResource(R.string.desc_clear_search),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = stringResource(R.string.desc_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(SearchTargetConstants.CLEAR_ICON_SIZE),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTargetCard(
    target: SearchTarget,
    actionLabelResId: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
    isPredicted: Boolean = false,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    iconPackPackage: String? = null,
) {
    val view = LocalView.current
    SearchResultCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SearchResultCardDefaults.shape)
                .predictedSubmitHighlight(
                    isPredicted = isPredicted,
                    shape = SearchResultCardDefaults.shape,
                    opaqueCardTopResultBorder = true,
                )
                .combinedClickable(
                    role = Role.Button,
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                    onLongClickLabel = stringResource(R.string.customize_search_engines),
                    onLongClick = onLongClick,
                ),
        showWallpaperBackground = showWallpaperBackground,
        styleOverrides = SearchResultCardStyleOverrides(showBorder = false),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SearchTargetConstants.CARD_HORIZONTAL_PADDING,
                        vertical = SearchTargetConstants.CARD_VERTICAL_PADDING,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            SearchTargetIcon(
                target = target,
                iconSize = SearchTargetConstants.DEFAULT_ICON_SIZE,
                style = IconRenderStyle.SIMPLE,
                appIconShape = appIconShape,
                iconPackPackage = iconPackPackage,
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))

            Text(
                text =
                    stringResource(
                        actionLabelResId,
                        target.getDisplayName(),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Card for customizing search engines - always available at the bottom.
 */
@Composable
private fun CustomizeSearchEnginesCard(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
) {
    val view = LocalView.current
    SearchResultCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SearchResultCardDefaults.shape)
                .clickable(role = Role.Button) {
                    hapticConfirm(view)()
                    onClick()
                },
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SearchTargetConstants.CARD_HORIZONTAL_PADDING,
                        vertical = SearchTargetConstants.CARD_VERTICAL_PADDING,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = MaterialSymbols.Settings,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))

            Text(
                text = stringResource(R.string.customize_search_engines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
