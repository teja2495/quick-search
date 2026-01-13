package com.tk.quicksearch.search.searchEngines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.ui.AppColors
import com.tk.quicksearch.R

// ============================================================================
// Constants
// ============================================================================

private const val SUGGESTION_ICON_SIZE = 36
private const val SUGGESTION_ARROW_ICON_SIZE = 32
private const val SUGGESTION_ICON_START_PADDING = 16
private const val SUGGESTION_TEXT_START_PADDING = 12
private const val SUGGESTION_TEXT_END_PADDING = 16


// ============================================================================
// Public API
// ============================================================================

@Composable
fun WebSuggestionsSection(
    modifier: Modifier = Modifier,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    showWallpaperBackground: Boolean = false,
    reverseOrder: Boolean = false,
    isShortcutDetected: Boolean = false,
    isRecentQuery: Boolean = false,
    onDeleteRecentQuery: ((String) -> Unit)? = null,
    paddingTop: androidx.compose.ui.unit.Dp = 0.dp,
    paddingBottom: androidx.compose.ui.unit.Dp = 0.dp
) {
    if (suggestions.isEmpty()) return

    // Reverse the suggestions list if requested
    val orderedSuggestions = if (reverseOrder) suggestions.reversed() else suggestions

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = paddingTop, bottom = paddingBottom),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WebSuggestionsCard(
            suggestions = orderedSuggestions,
            onSuggestionClick = onSuggestionClick,
            showWallpaperBackground = showWallpaperBackground,
            isShortcutDetected = isShortcutDetected,
            isRecentQuery = isRecentQuery,
            onDeleteRecentQuery = onDeleteRecentQuery
        )
    }
}

// ============================================================================
// Result Card
// ============================================================================

@Composable
private fun WebSuggestionsCard(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    showWallpaperBackground: Boolean = false,
    isShortcutDetected: Boolean = false,
    isRecentQuery: Boolean = false,
    onDeleteRecentQuery: ((String) -> Unit)? = null
) {
    val textColor = if (showWallpaperBackground) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val iconColor = if (showWallpaperBackground) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = AppColors.getCardColors(showWallpaperBackground),
        elevation = AppColors.getCardElevation(showWallpaperBackground)
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
                    isRecentQuery = isRecentQuery,
                    onDeleteClick = if (isRecentQuery && onDeleteRecentQuery != null) {
                        { onDeleteRecentQuery(suggestion) }
                    } else null
                )
                
                // Add divider between items, but not after the last one
                if (index < suggestions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = if (showWallpaperBackground) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
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
    isRecentQuery: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                start = SUGGESTION_ICON_START_PADDING.dp,
                end = SUGGESTION_TEXT_END_PADDING.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val icon = when {
            isRecentQuery -> Icons.Rounded.History
            isShortcutDetected -> Icons.Rounded.Search
            else -> Icons.Rounded.NorthWest
        }

        val iconSize = if (icon == Icons.Rounded.NorthWest) SUGGESTION_ARROW_ICON_SIZE else SUGGESTION_ICON_SIZE

        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.desc_search_icon),
            tint = iconColor,
            modifier = Modifier
                .size(iconSize.dp)
                .padding(end = SUGGESTION_TEXT_START_PADDING.dp)
        )
        
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = if (isRecentQuery) 1 else Int.MAX_VALUE,
            overflow = if (isRecentQuery) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = Modifier.weight(1f)
        )
        
        // Show delete icon for recent queries
        if (onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete recent query",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

