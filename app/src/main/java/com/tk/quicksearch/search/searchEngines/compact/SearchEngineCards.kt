package com.tk.quicksearch.search.searchEngines.compact

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.searchEngines.getDisplayName
import com.tk.quicksearch.search.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.search.searchEngines.shared.SearchTargetConstants
import com.tk.quicksearch.search.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.util.hapticConfirm

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
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
    showWallpaperBackground: Boolean = false,
) {
    // Reverse the engine list when results are at the bottom
    val orderedEngines =
        if (isReversed) {
            enabledEngines.reversed()
        } else {
            enabledEngines
        }

    // Show customize card when there are any enabled engines
    val showCustomizeCard = enabledEngines.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // When reversed (results at bottom), show customize card at the top
        if (isReversed && showCustomizeCard) {
            CustomizeSearchEnginesCard(
                onClick = onCustomizeClick,
                showWallpaperBackground = showWallpaperBackground,
            )
        }

        orderedEngines.forEach { engine ->
            SearchTargetCard(
                target = engine,
                onClick = { onSearchEngineClick(query, engine) },
                showWallpaperBackground = showWallpaperBackground,
            )
        }

        // When not reversed, show customize card at the bottom
        if (!isReversed && showCustomizeCard) {
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
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val targetName = target.getDisplayName()

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable {
                    hapticConfirm(view)()
                    onClick()
                },
        colors = AppColors.getCardColors(showWallpaperBackground),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = AppColors.getCardElevation(showWallpaperBackground),
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
            // Search target icon
            SearchTargetIcon(
                target = target,
                iconSize = SearchTargetConstants.DEFAULT_ICON_SIZE,
                style = IconRenderStyle.SIMPLE,
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))

            // Search engine name
            Text(
                text = stringResource(R.string.search_on_engine, targetName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )

            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
) {
    val view = LocalView.current

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable {
                    hapticConfirm(view)()
                    onClick()
                },
        colors = AppColors.getCardColors(showWallpaperBackground),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = AppColors.getCardElevation(showWallpaperBackground),
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
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))

            Text(
                text =
                    stringResource(
                        R.string.search_on_engine,
                        target.getDisplayName(),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
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

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable {
                    hapticConfirm(view)()
                    onClick()
                    // Navigation is handled by the onClick callback which should navigate to search engine settings
                    // This is passed down from MainActivity -> SearchRoute -> SearchScreenContent -> SearchContentArea -> NoResultsSearchEngineCards -> CustomizeSearchEnginesCard
                },
        colors = AppColors.getCardColors(showWallpaperBackground),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = AppColors.getCardElevation(showWallpaperBackground),
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
            // Settings icon
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.size(SearchTargetConstants.ICON_TEXT_SPACING))

            // Customize text
            Text(
                text = stringResource(R.string.customize_search_engines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
