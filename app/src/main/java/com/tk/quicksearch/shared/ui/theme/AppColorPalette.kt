package com.tk.quicksearch.shared.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.AppTheme

/** Frosted card fill for light app-theme (gradient) result surfaces; mirrors dark wallpaper scrim alpha. */
internal const val LightResultCardFrostAlpha = 0.72f

/** Alpha applied to the compact search engine strip and open-keyboard button in all scenarios. */
internal const val CompactSectionAlpha = 0.8f

/** Search result card scrim over wallpaper / custom image background in light mode. */
internal const val LightWallpaperSearchResultCardAlpha = 0.9f

/** Result cards, compact engine strip, and search color theme surfaces over wallpaper / custom image in dark mode. */
internal const val DarkWallpaperSearchSurfaceAlpha = 0.70f

/** Search bar scrim fill alpha for the dark palette (black). */
internal const val SearchBarBackgroundAlphaDark = 0.65f

/** Search bar scrim fill alpha for the light palette (white). */
internal const val SearchBarBackgroundAlphaLight = 0.9f

/** Keyboard open/switch button alpha for light wallpaper/custom-image mode. */
internal const val LightWallpaperKeyboardButtonAlpha = 0.5f

/**
 * Base alpha for THEME-mode fallback background layers.
 * Shared by search and settings so both screens render the same theme-depth.
 */
internal const val ThemeModeFallbackBackgroundAlpha = 0.6f

/**
 * Theme-aware app-specific color tokens.
 *
 * Material colors remain in [MaterialTheme.colorScheme]. This file centralizes custom semantic
 * colors and non-Material palettes used across feature modules.
 */
@Immutable
internal data class QuickSearchAppColorPalette(
    val searchBarBackground: Color,
    val searchBarBorder: Color,
    val searchBarTextAndIcon: Color,
    val settingsCardBackground: Color,
    val settingsText: Color,
    val overlayLow: Color,
    val overlayMedium: Color,
    val overlayHigh: Color,
    val overlayVeryHigh: Color,
    val dialogBackground: Color,
    val dialogText: Color,
    val onboardingScrimTop: Color,
    val onboardingScrimMiddle: Color,
    val onboardingScrimBottom: Color,
    val onboardingBubbleBorder: Color,
    val onboardingBubbleBodyText: Color,
    val actionPhone: Color,
    val actionSms: Color,
    val actionWhatsApp: Color,
    val actionTelegram: Color,
    val actionSignal: Color,
    val actionEmail: Color,
    val actionVideoCall: Color,
    val actionCustom: Color,
    val actionView: Color,
    val wallpaperOverlayTint: Color,
    val resultCardWallpaperBackground: Color,
    val compactSectionBackground: Color,
)

internal val DarkQuickSearchAppColorPalette =
    QuickSearchAppColorPalette(
        searchBarBackground = Color.Black.copy(alpha = SearchBarBackgroundAlphaDark),
        searchBarBorder = Color.White.copy(alpha = 0.3f),
        searchBarTextAndIcon = Color(0xFFE0E0E0),
        settingsCardBackground = Color.Black.copy(alpha = 0.4f),
        settingsText = Color.White,
        overlayLow = Color.Black.copy(alpha = 0.2f),
        overlayMedium = Color.Black.copy(alpha = 0.4f),
        overlayHigh = Color.Black.copy(alpha = 0.5f),
        overlayVeryHigh = Color.Black.copy(alpha = 0.75f),
        dialogBackground = Color(0xFF1C1C1E),
        dialogText = Color.White,
        onboardingScrimTop = Color.Black.copy(alpha = 0.7f),
        onboardingScrimMiddle = Color.Black.copy(alpha = 0.5f),
        onboardingScrimBottom = Color.Black.copy(alpha = 0.2f),
        onboardingBubbleBorder = Color.White.copy(alpha = 0.3f),
        onboardingBubbleBodyText = Color.White.copy(alpha = 0.9f),
        actionPhone = Color(0xFF4CAF50),
        actionSms = Color(0xFF2196F3),
        actionWhatsApp = Color(0xFF25D366),
        actionTelegram = Color(0xFF0088CC),
        actionSignal = Color(0xFF3B45FD),
        actionEmail = Color(0xFFFF9800),
        actionVideoCall = Color(0xFF9C27B0),
        actionCustom = Color(0xFF607D8B),
        actionView = Color(0xFF9E9E9E),
        wallpaperOverlayTint = Color.Black,
        resultCardWallpaperBackground = Color.Black.copy(alpha = DarkWallpaperSearchSurfaceAlpha),
        compactSectionBackground = Color.Black.copy(alpha = DarkWallpaperSearchSurfaceAlpha),
    )

internal val LightQuickSearchAppColorPalette =
    QuickSearchAppColorPalette(
        searchBarBackground = Color.White.copy(alpha = SearchBarBackgroundAlphaLight),
        searchBarBorder = Color.Black.copy(alpha = 0.3f),
        searchBarTextAndIcon = Color(0xFF1F1B24),
        settingsCardBackground = Color.Black.copy(alpha = 0.06f),
        settingsText = Color(0xFF1F1B24),
        overlayLow = Color.Black.copy(alpha = 0.08f),
        overlayMedium = Color.Black.copy(alpha = 0.16f),
        overlayHigh = Color.Black.copy(alpha = 0.24f),
        overlayVeryHigh = Color.Black.copy(alpha = 0.6f),
        dialogBackground = Color.White,
        dialogText = Color(0xFF1F1B24),
        onboardingScrimTop = Color.Black.copy(alpha = 0.62f),
        onboardingScrimMiddle = Color.Black.copy(alpha = 0.4f),
        onboardingScrimBottom = Color.Black.copy(alpha = 0.14f),
        onboardingBubbleBorder = Color.Black.copy(alpha = 0.2f),
        onboardingBubbleBodyText = Color.White.copy(alpha = 0.92f),
        actionPhone = Color(0xFF4CAF50),
        actionSms = Color(0xFF2196F3),
        actionWhatsApp = Color(0xFF25D366),
        actionTelegram = Color(0xFF0088CC),
        actionSignal = Color(0xFF3B45FD),
        actionEmail = Color(0xFFFF9800),
        actionVideoCall = Color(0xFF9C27B0),
        actionCustom = Color(0xFF607D8B),
        actionView = Color(0xFF9E9E9E),
        wallpaperOverlayTint = Color.White,
        resultCardWallpaperBackground = Color.White.copy(alpha = LightWallpaperSearchResultCardAlpha),
        compactSectionBackground = Color.White.copy(alpha = LightWallpaperSearchResultCardAlpha),
    )

internal val LocalQuickSearchAppColorPalette =
    staticCompositionLocalOf<QuickSearchAppColorPalette> {
        DarkQuickSearchAppColorPalette
    }

val LocalAppIsDarkTheme = staticCompositionLocalOf { true }

val LocalAppTheme = staticCompositionLocalOf { com.tk.quicksearch.search.core.AppTheme.MONOCHROME }

/**
 * True when [QuickSearchTheme] applies an image-derived accent (from wallpaper/custom image)
 * to the Material primary slots.
 */
internal val LocalWallpaperDynamicAccentActive = staticCompositionLocalOf { false }

/**
 * True when the background source is the system wallpaper (not a custom image or app theme).
 * Used to opt themed icons into system dynamic accent colors rather than our wallpaper-derived palette.
 */
val LocalIsSystemWallpaperActive = staticCompositionLocalOf { false }

/** True when Material You (device dynamic colors) is active for the app theme. */
val LocalDeviceDynamicColorsActive = staticCompositionLocalOf { false }

/**
 * Whether the current image background (custom image or system wallpaper) is dark.
 * `true` = dark image → use light (white) text on top.
 * `false` = light image → use dark text on top.
 * `null` = no image background active; fall back to theme defaults.
 */
val LocalImageBackgroundIsDark = staticCompositionLocalOf<Boolean?> { null }

/**
 * Semantic color slots for the current search UI theme.
 *
 * Bundles the background, card container, and keyboard-button colors derived from the active
 * overlay theme. Provided via [LocalSearchColorTheme] and consumed by [AppColors.KeyboardButtonBackground].
 */
@Immutable
data class SearchColorTheme(
    /** Dominant background color for the current theme (gradient base in THEME mode). */
    val background: Color,
    /** Container color for result/suggestion/history/inline-engine cards. */
    val cardBackground: Color,
    /** Background for keyboard-adjacent action buttons (open, switch) — same for both. */
    val keyboardButtonBackground: Color,
)

/** Provides the resolved [SearchColorTheme] for the active search screen. Null outside search context. */
val LocalSearchColorTheme = staticCompositionLocalOf<SearchColorTheme?> { null }

/** True when AMOLED true-black theme surfaces are active (dark Mono + AMOLED toggle). */
val LocalAmoledThemeActive = staticCompositionLocalOf { false }
