package com.tk.quicksearch.search.ui

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared color logic for search result cards (Contacts, Files, Web Suggestions, Search Engines).
 */
object SearchResultColors {
    
    val WallpaperEnabledBackgroundColor = Color.Black.copy(alpha = 0.4f)
    
    @Composable
    fun getContainerColor(showWallpaperBackground: Boolean): Color {
        return if (showWallpaperBackground) {
            WallpaperEnabledBackgroundColor
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    }

    @Composable
    fun getCardColors(showWallpaperBackground: Boolean): CardColors {
        return if (showWallpaperBackground) {
            CardDefaults.cardColors(
                containerColor = WallpaperEnabledBackgroundColor
            )
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        }
    }
    
    @Composable
    fun getElevation(showWallpaperBackground: Boolean): Dp {
        return if (showWallpaperBackground) 0.dp else 2.dp
    }

    @Composable
    fun getCardElevation(showWallpaperBackground: Boolean): CardElevation {
        return if (showWallpaperBackground) {
             CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
             CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        }
    }
}
