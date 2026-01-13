package com.tk.quicksearch.ui.theme

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized color definitions for the QuickSearch app.
 * Contains all colors used throughout the application including backgrounds, text, overlays, and theme colors.
 */
object AppColors {

    // ============================================================================
    // THEME COLORS (PURPLE THEME)
    // ============================================================================

    /** Primary purple theme color - Deep Purple */
    val ThemeDeepPurple = Color(0xFF651FFF)

    /** Secondary purple theme color - Neon Purple */
    val ThemeNeonPurple = Color(0xFFD500F9)

    /** Tertiary purple theme color - Indigo (bridge to purple) */
    val ThemeIndigo = Color(0xFF5E35B1)

    /** Quaternary purple theme color - Purple (bridge to red) */
    val ThemePurple = Color(0xFF9C27B0)

    // ============================================================================
    // APP BACKGROUND COLORS
    // ============================================================================

    /** Main app background color (transparent for wallpaper mode) */
    val AppBackgroundTransparent = Color.Transparent

    /** Dark app background color */
    val AppBackgroundDark = Color(0xFF121212)

    // ============================================================================
    // SEARCH BAR COLORS
    // ============================================================================

    /** Search bar background color with transparency */
    val SearchBarBackground = Color.Black.copy(alpha = 0.5f)

    /** Search bar border color with transparency */
    val SearchBarBorder = Color.White.copy(alpha = 0.3f)

    /** Search bar text and icon color for dark backgrounds */
    val SearchBarTextAndIcon = Color(0xFFE0E0E0)

    // ============================================================================
    // SETTINGS COLORS
    // ============================================================================

    /** Settings background color */
    val SettingsBackground = Color.Transparent

    /** Settings option card background color */
    val SettingsCardBackground = Color.Black.copy(alpha = 0.4f)

    /** Settings text color */
    val SettingsText = Color.White

    // ============================================================================
    // OVERLAY AND TRANSPARENCY COLORS
    // ============================================================================

    /** Standard overlay color with low transparency */
    val OverlayLow = Color.Black.copy(alpha = 0.2f)

    /** Medium overlay color */
    val OverlayMedium = Color.Black.copy(alpha = 0.4f)

    /** High overlay color */
    val OverlayHigh = Color.Black.copy(alpha = 0.5f)

    /** Very high overlay color for dialogs */
    val OverlayVeryHigh = Color.Black.copy(alpha = 0.75f)

    /** Dialog background color */
    val DialogBackground = Color.Black

    /** Dialog text color */
    val DialogText = Color.White

    // ============================================================================
    // TEXT COLORS
    // ============================================================================

    /** Primary text color for light backgrounds */
    val TextPrimaryLight = Color(0xFF1C1B1FL)

    /** Primary text color for dark backgrounds */
    val TextPrimaryDark = Color.White

    /** Secondary text color for dark backgrounds */
    val TextSecondaryDark = Color.White.copy(alpha = 0.7f)

    /** Tertiary text color for dark backgrounds */
    val TextTertiaryDark = Color.White.copy(alpha = 0.3f)

    /** Icon color for light backgrounds */
    val IconLight = Color.White

    /** Icon color for dark backgrounds with transparency */
    val IconDarkTransparent = Color.White.copy(alpha = 0.7f)

    // ============================================================================
    // WIDGET COLORS
    // ============================================================================

    /** Widget border color (always white with transparency) */
    val WidgetBorder = Color.White

    /** Widget dark grey text color for high contrast backgrounds */
    val WidgetDarkGrey = Color(0xFF424242)

    /** Widget background white */
    val WidgetBackgroundWhite = Color.White

    /** Widget background black */
    val WidgetBackgroundBlack = Color.Black

    // ============================================================================
    // SEARCH RESULT COLORS (Legacy - kept for backward compatibility)
    // ============================================================================

    /** Wallpaper enabled background color for search results */
    val WallpaperEnabledBackgroundColor = OverlayMedium

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

    // ============================================================================
    // GRADIENT COLOR PALETTES
    // ============================================================================

    /** Northern Lights (Cool & Mystical) gradient colors */
    val AuroraColors = listOf(
        Color(0xFF00E5FF), // Cyan Accent
        Color(0xFF2979FF), // Royal Blue
        ThemeDeepPurple,   // Deep Purple
        ThemeNeonPurple,   // Neon Violet
        Color(0xFF2979FF), // Back to Blue
        Color(0xFF00E5FF)  // Back to Cyan loop
    )

    /** Electric Cyberpunk (Vibrant & High Energy) gradient colors */
    val ElectricColors = listOf(
        ThemeNeonPurple,   // Neon Purple
        Color(0xFFFF00CC), // Hot Pink
        Color(0xFFFF3D00), // Electric Orange
        Color(0xFFFF00CC), // Hot Pink
        ThemeNeonPurple,   // Neon Purple
        Color(0xFF2979FF), // Electric Blue
        ThemeNeonPurple    // Loop
    )

    /** Golden Luxury (Warm & Premium) gradient colors */
    val GoldenColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF9100), // Deep Orange
        Color(0xFFFFEA00), // Bright Yellow
        Color(0xFFFFD700), // Gold
        Color(0xFFFFA000), // Amber
        Color(0xFFFFD700)  // Loop
    )

    /** Google Brand Colors gradient */
    val GoogleColors = listOf(
        Color(0xFF4285F4), // 1. Blue
        ThemeIndigo,       // 1.1 Indigo (Bridge to Purple)
        ThemePurple,       // 1.2 Purple (Bridge to Red)
        Color(0xFFE91E63), // 1.3 Pink (Bridge to Red)
        Color(0xFFEA4335), // 2. Red
        Color(0xFFFF5722), // 2.1 Deep Orange
        Color(0xFFFF9800), // 2.2 Orange
        Color(0xFFFFC107), // 2.3 Amber
        Color(0xFFFBBC05), // 3. Yellow
        Color(0xFFD4E157), // 3.1 Lime
        Color(0xFFCDDC39), // 3.2 Light Green
        Color(0xFF34A853), // 4. Green
        Color(0xFF00BFA5), // 4.1 Teal Accent
        Color(0xFF00BCD4), // 4.2 Cyan
        Color(0xFF03A9F4), // 4.3 Light Blue
        Color(0xFF4285F4), // 5. Back to Blue

        // End Block: Solid White
        Color.White, Color.White, Color.White, Color.White,
        Color.White, Color.White, Color.White, Color.White,
        Color.White, Color.White, Color.White, Color.White
    )
}