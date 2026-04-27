package com.tk.quicksearch.search.webSuggestions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
                        modifier =
                            Modifier.padding(
                                start = SUGGESTION_ICON_START_PADDING.dp,
                                end = SUGGESTION_TEXT_END_PADDING.dp,
                            ),
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
) {
    Row(
        modifier =
            modifier
                .clip(DesignTokens.CardShape)
                .clickable(onClick = onClick)
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
                isShortcutDetected -> Icons.Rounded.Search
                else -> Icons.Rounded.NorthWest
            }

        val iconSize = if (icon == Icons.Rounded.NorthWest) SUGGESTION_ARROW_ICON_SIZE else SUGGESTION_ICON_SIZE

        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.desc_search_icon),
            tint = iconColor,
            modifier =
                Modifier
                    .size(iconSize.dp)
                    .padding(end = SUGGESTION_TEXT_START_PADDING.dp),
        )

        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
    }
}
