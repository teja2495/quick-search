package com.tk.quicksearch.search.webSuggestions

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardStyleOverrides
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppShapes

private const val SUGGESTION_ICON_SIZE = 36
private const val SUGGESTION_ARROW_ICON_SIZE = 32
private const val SUGGESTION_ICON_START_PADDING = 16
private const val SUGGESTION_TEXT_START_PADDING = 12
private const val SUGGESTION_TEXT_END_PADDING = 16

@Composable
fun WebSuggestionsSection(
    modifier: Modifier = Modifier,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    showWallpaperBackground: Boolean = false,
    reverseOrder: Boolean = false,
    isShortcutDetected: Boolean = false,
) {
    if (suggestions.isEmpty()) return

    val orderedSuggestions = if (reverseOrder) suggestions.reversed() else suggestions

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        WebSuggestionsCard(
            suggestions = orderedSuggestions,
            onSuggestionClick = onSuggestionClick,
            showWallpaperBackground = showWallpaperBackground,
            isShortcutDetected = isShortcutDetected,
        )
    }
}

@Composable
private fun WebSuggestionsCard(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    showWallpaperBackground: Boolean = false,
    isShortcutDetected: Boolean = false,
) {
    val overlayDividerColor = LocalOverlayDividerColor.current
    val dividerColor =
        overlayDividerColor
            ?: if (showWallpaperBackground) {
                AppColors.WallpaperDivider
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
    val textColor =
        if (showWallpaperBackground) AppColors.WallpaperTextPrimary else MaterialTheme.colorScheme.onSurface

    val iconColor =
        if (showWallpaperBackground) AppColors.WallpaperTextSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    if (LocalSearchCardLayoutEnabled.current) {
        // Material 3 Expressive segmented list: each suggestion is its own tile, 2dp apart, with
        // large outer and small inner corners, instead of rows on one card split by dividers.
        Column(modifier = Modifier.fillMaxWidth()) {
            suggestions.forEachIndexed { index, suggestion ->
                val shape = AppShapes.segment(index, suggestions.size)
                SearchResultCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = if (index > 0) DesignTokens.SegmentedListGap else 0.dp),
                    showWallpaperBackground = showWallpaperBackground,
                    styleOverrides =
                        SearchResultCardStyleOverrides(
                            shape = shape,
                            border = BorderStroke(0.dp, Color.Transparent),
                        ),
                ) {
                    WebSuggestionItem(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion) },
                        textColor = textColor,
                        iconColor = iconColor,
                        modifier = Modifier.fillMaxWidth(),
                        isShortcutDetected = isShortcutDetected,
                        shape = shape,
                    )
                }
            }
        }
        return
    }

    SearchResultCard(
        modifier = Modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                WebSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                    textColor = textColor,
                    iconColor = iconColor,
                    modifier = Modifier.fillMaxWidth(),
                    isShortcutDetected = isShortcutDetected,
                )

                if (index < suggestions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.compactEdgeToEdge(),
                        color = dividerColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSuggestionItem(
    suggestion: String,
    onClick: () -> Unit,
    textColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    isShortcutDetected: Boolean = false,
    shape: Shape = AppShapes.card,
) {
    Row(
        modifier =
            modifier
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    start = SUGGESTION_ICON_START_PADDING.dp,
                    end = SUGGESTION_TEXT_END_PADDING.dp,
                    top = DesignTokens.SpacingSmall,
                    bottom = DesignTokens.SpacingSmall,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        val icon =
            when {
                isShortcutDetected -> MaterialSymbols.Search
                else -> MaterialSymbols.NorthWest
            }

        val iconSize = if (icon == MaterialSymbols.NorthWest) SUGGESTION_ARROW_ICON_SIZE else SUGGESTION_ICON_SIZE

        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.common_search),
            tint = iconColor,
            modifier =
                Modifier
                    .size(iconSize.dp)
                    .padding(end = SUGGESTION_TEXT_START_PADDING.dp),
        )

        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
    }
}
