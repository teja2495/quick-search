package com.tk.quicksearch.search

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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

// ============================================================================
// Constants
// ============================================================================

private const val SUGGESTION_ROW_MIN_HEIGHT = 48
private const val SUGGESTION_ICON_SIZE = 26
private const val SUGGESTION_ICON_START_PADDING = 16
private const val SUGGESTION_TEXT_START_PADDING = 12
private const val SUGGESTION_TEXT_END_PADDING = 16
private const val CARD_CORNER_RADIUS = 16

// ============================================================================
// Public API
// ============================================================================

@Composable
fun WebSuggestionsSection(
    modifier: Modifier = Modifier,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    showWallpaperBackground: Boolean = false
) {
    if (suggestions.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WebSuggestionsCard(
            suggestions = suggestions,
            onSuggestionClick = onSuggestionClick,
            showWallpaperBackground = showWallpaperBackground
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
    showWallpaperBackground: Boolean = false
) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surface
    
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
        shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                WebSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                    textColor = textColor,
                    iconColor = iconColor,
                    modifier = Modifier.fillMaxWidth()
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(SUGGESTION_ROW_MIN_HEIGHT.dp)
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
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = stringResource(R.string.desc_search_icon),
            tint = iconColor,
            modifier = Modifier
                .size(SUGGESTION_ICON_SIZE.dp)
                .padding(end = SUGGESTION_TEXT_START_PADDING.dp)
        )
        
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

