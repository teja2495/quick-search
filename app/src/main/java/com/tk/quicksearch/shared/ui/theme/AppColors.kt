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

/** Frosted card fill in light mode (wallpaper / theme), mirrors dark mode's ~0.4f black scrim. */
internal const val LightResultCardFrostAlpha = 0.72f

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
        searchBarBackground = Color.Black.copy(alpha = 0.5f),
        searchBarBorder = Color.White.copy(alpha = 0.3f),
        searchBarTextAndIcon = Color(0xFFE0E0E0),
        settingsCardBackground = Color.Black.copy(alpha = 0.4f),
        settingsText = Color.White,
        overlayLow = Color.Black.copy(alpha = 0.2f),
        overlayMedium = Color.Black.copy(alpha = 0.4f),
        overlayHigh = Color.Black.copy(alpha = 0.5f),
        overlayVeryHigh = Color.Black.copy(alpha = 0.75f),
        dialogBackground = Color.Black,
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
        resultCardWallpaperBackground = Color.Black.copy(alpha = 0.4f),
        compactSectionBackground = Color.Black.copy(alpha = 0.5f),
    )

internal val LightQuickSearchAppColorPalette =
    QuickSearchAppColorPalette(
        searchBarBackground = Color.White,
        searchBarBorder = Color.White,
        searchBarTextAndIcon = Color(0xFF1F1B24),
        settingsCardBackground = Color.Black.copy(alpha = 0.06f),
        settingsText = Color(0xFF1F1B24),
        overlayLow = Color.Black.copy(alpha = 0.08f),
        overlayMedium = Color.Black.copy(alpha = 0.16f),
        overlayHigh = Color.Black.copy(alpha = 0.24f),
        overlayVeryHigh = Color.Black.copy(alpha = 0.6f),
        dialogBackground = Color(0xFFF8F7FB),
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
        resultCardWallpaperBackground = Color.White.copy(alpha = LightResultCardFrostAlpha),
        compactSectionBackground = Color.White.copy(alpha = 0.5f),
    )

internal val LocalQuickSearchAppColorPalette =
    staticCompositionLocalOf<QuickSearchAppColorPalette> {
        DarkQuickSearchAppColorPalette
    }

val LocalAppIsDarkTheme = staticCompositionLocalOf { true }

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

object AppColors {
    // Theme-aware semantic colors ------------------------------------------------------------

    private val current: QuickSearchAppColorPalette
        @Composable
        get() = LocalQuickSearchAppColorPalette.current

    // Accent / brand color -----------------------------------------------------------------
    //
    // To retheme the entire app, change AppAccentLight / AppAccentDark in Color.kt.
    // Use these tokens in composables instead of MaterialTheme.colorScheme.primary directly,
    // so the intent is clear and the origin is traceable.

    /** The app's primary brand accent color (maps to Material `primary`). */
    val Accent: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    /** Color for content drawn on top of an accent-colored surface (maps to `onPrimary`). */
    val OnAccent: Color
        @Composable
        get() = MaterialTheme.colorScheme.onPrimary

    // Icon tints ---------------------------------------------------------------------------

    /**
     * Tint for primary / active / action icons (toggles, selection indicators, FABs).
     * Maps to the accent color.
     */
    val IconTintPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    /**
     * Tint for secondary / decorative / neutral icons (placeholders, trailing arrows, hints).
     * Maps to `onSurfaceVariant`.
     */
    val IconTintSecondary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    // Settings colors ----------------------------------------------------------------------

    val SearchBarBackground: Color
        @Composable
        get() = current.searchBarBackground

    val SearchBarBorder: Color
        @Composable
        get() = current.searchBarBorder

    val SearchBarTextAndIcon: Color
        @Composable
        get() = current.searchBarTextAndIcon

    val SettingsBackground: Color = Color.Transparent

    val SettingsCardBackground: Color
        @Composable
        get() = current.settingsCardBackground

    val SettingsText: Color
        @Composable
        get() = current.settingsText

    /** Tint for icons inside settings rows and cards (e.g. toggle icons, section icons). */
    val SettingsIconTint: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    val OverlayLow: Color
        @Composable
        get() = current.overlayLow

    val OverlayMedium: Color
        @Composable
        get() = current.overlayMedium

    val OverlayHigh: Color
        @Composable
        get() = current.overlayHigh

    val OverlayVeryHigh: Color
        @Composable
        get() = current.overlayVeryHigh

    val DialogBackground: Color
        @Composable
        get() = current.dialogBackground

    val DialogText: Color
        @Composable
        get() = current.dialogText

    val OnboardingScrimTop: Color
        @Composable
        get() = current.onboardingScrimTop

    val OnboardingScrimMiddle: Color
        @Composable
        get() = current.onboardingScrimMiddle

    val OnboardingScrimBottom: Color
        @Composable
        get() = current.onboardingScrimBottom

    val OnboardingBubbleBorder: Color
        @Composable
        get() = current.onboardingBubbleBorder

    val OnboardingBubbleBodyText: Color
        @Composable
        get() = current.onboardingBubbleBodyText

    val ActionPhone: Color
        @Composable
        get() = current.actionPhone

    val ActionSms: Color
        @Composable
        get() = current.actionSms

    val ActionWhatsApp: Color
        @Composable
        get() = current.actionWhatsApp

    val ActionTelegram: Color
        @Composable
        get() = current.actionTelegram

    val ActionSignal: Color
        @Composable
        get() = current.actionSignal

    val ActionEmail: Color
        @Composable
        get() = current.actionEmail

    val ActionVideoCall: Color
        @Composable
        get() = current.actionVideoCall

    val ActionCustom: Color
        @Composable
        get() = current.actionCustom

    val ActionView: Color
        @Composable
        get() = current.actionView

    // Wallpaper & background tinting -------------------------------------------------------

    /** Base tint color applied over the wallpaper (Black in dark mode, White in light mode). */
    val WallpaperOverlayTint: Color
        @Composable
        get() = current.wallpaperOverlayTint

    // Result cards -------------------------------------------------------------------------

    /** Card container color when the wallpaper background is enabled. */
    val ResultCardWallpaperBackground: Color
        @Composable
        get() = current.resultCardWallpaperBackground

    // Compact section (search engine strip) ------------------------------------------------

    /** Background for the compact search engine section strip. */
    val CompactSectionBackground: Color
        @Composable
        get() = current.compactSectionBackground

    // Keyboard action buttons (open / switch) -----------------------------------------------

    /**
     * Background for keyboard open/switch pill buttons.
     * When inside a search screen with an active [LocalSearchColorTheme], uses the theme-derived
     * color so that selecting a theme in settings changes these buttons automatically.
     * Falls back to [compactSectionBackground] outside a themed context.
     */
    val KeyboardButtonBackground: Color
        @Composable
        get() = LocalSearchColorTheme.current?.keyboardButtonBackground
            ?: current.compactSectionBackground

    // Wallpaper-mode text & icon colors ----------------------------------------------------

    /** Primary text color used in result rows when the wallpaper background is active. */
    val WallpaperTextPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)

    /** Secondary/icon color used in result rows when the wallpaper background is active. */
    val WallpaperTextSecondary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    /** Divider color used between result rows when the wallpaper background is active. */
    val WallpaperDivider: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    // Inline search engine highlight -------------------------------------------------------

    /** Fill color for the predicted/highlighted engine icon backdrop. */
    val InlineEngineHighlightBackground: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    /** Border color for the predicted/highlighted engine icon backdrop. */
    val InlineEngineHighlightBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    // Keyboard operator pills --------------------------------------------------------------

    /** Background color for number-keyboard operator pill chips. */
    val KeyboardPillBackground: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    /** Text color for number-keyboard operator pill chips. */
    val KeyboardPillText: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    // Shared/static tokens -----------------------------------------------------------------

    val AppBackgroundTransparent: Color = Color.Transparent
    val AppBackgroundDark: Color = Color.Black

    val ThemeDeepPurple: Color = Color(0xFF651FFF)
    val ThemeNeonPurple: Color = Color(0xFFD500F9)
    val ThemeIndigo: Color = Color(0xFF5E35B1)
    val ThemePurple: Color = Color(0xFF9C27B0)

    val WidgetTextDarkGrey: Color = Color(0xFF424242)
    val WidgetBorderDefault: Color = Color.Black
    val WidgetBorder: Color = Color.White
    val WidgetBackgroundLight: Color = Color.White
    val WidgetBackgroundDark: Color = Color.Black
    val WidgetTextLight: Color = Color.White
    val WidgetTextDark: Color = Color.Black
    val WidgetContactAvatarDarkBackground: Color = md_theme_dark_primaryContainer
    val WidgetContactAvatarDarkOnBackground: Color = md_theme_dark_onPrimaryContainer
    val WidgetContactAvatarLightBackground: Color = md_theme_light_primaryContainer
    val WidgetContactAvatarLightOnBackground: Color = md_theme_light_onPrimaryContainer

    // ============================================================================
    // SEARCH FIELD WELCOME PALETTES
    // ============================================================================

    val SearchFieldAuroraPalette =
        listOf(
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            ThemeDeepPurple,
            ThemeNeonPurple,
            Color(0xFF2979FF),
            Color(0xFF00E5FF),
        )

    val SearchFieldElectricPalette =
        listOf(
            ThemeNeonPurple,
            Color(0xFFFF00CC),
            Color(0xFFFF3D00),
            Color(0xFFFF00CC),
            ThemeNeonPurple,
            Color(0xFF2979FF),
            ThemeNeonPurple,
        )

    val SearchFieldGoldenPalette =
        listOf(
            Color(0xFFFFD700),
            Color(0xFFFF9100),
            Color(0xFFFFEA00),
            Color(0xFFFFD700),
            Color(0xFFFFA000),
            Color(0xFFFFD700),
        )

    val SearchFieldGooglePalette =
        listOf(
            Color(0xFF4285F4),
            ThemeIndigo,
            ThemePurple,
            Color(0xFFE91E63),
            Color(0xFFEA4335),
            Color(0xFFFF5722),
            Color(0xFFFF9800),
            Color(0xFFFFC107),
            Color(0xFFFBBC05),
            Color(0xFFD4E157),
            Color(0xFFCDDC39),
            Color(0xFF34A853),
            Color(0xFF00BFA5),
            Color(0xFF00BCD4),
            Color(0xFF03A9F4),
            Color(0xFF4285F4),
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
        )

    // ============================================================================
    // APP THEME GRADIENT PALETTES
    // ============================================================================

    val ForestDarkPalette =
        listOf(
            Color(0xFF27382F),
            Color(0xFF2F4640),
            Color(0xFF435034),
            Color(0xFF1F3340),
        )

    val AuroraDarkPalette =
        listOf(
            Color(0xFF1F2E4A),
            Color(0xFF1F4A5A),
            Color(0xFF3A3E6B),
            Color(0xFF2A3150),
        )

    val SunsetDarkPalette =
        listOf(
            Color(0xFF4A2C34),
            Color(0xFF5A3A2A),
            Color(0xFF5C3046),
            Color(0xFF3E2A3B),
        )

    val MonochromeDarkPalette =
        listOf(
            AppBackgroundDark,
            Color(0xFF2A2A2A),
            Color(0xFF3E3E3E),
            Color(0xFFE8E8E8),
        )

    val ForestLightPalette =
        listOf(
            Color(0xFFCDE8D8),
            Color(0xFFD2EACC),
            Color(0xFFDAECCA),
            Color(0xFFC4E4D4),
        )

    val AuroraLightPalette =
        listOf(
            Color(0xFFC8DEF8),
            Color(0xFFC4E4F8),
            Color(0xFFCECEF8),
            Color(0xFFC0D8F4),
        )

    val SunsetLightPalette =
        listOf(
            Color(0xFFF5C8B4),
            Color(0xFFF5D8B0),
            Color(0xFFECC4D0),
            Color(0xFFF0C0BC),
        )

    val MonochromeLightPalette =
        listOf(
            Color(0xFFEDEAE4),
            Color(0xFFE0DCD6),
            Color(0xFFD4D0C8),
            Color(0xFFC6C2BA),
        )

    // ============================================================================
    // CARD THEMING UTILITIES
    // ============================================================================

    /**
     * Returns appropriate card colors based on wallpaper background setting.
     * When wallpaper background is enabled, uses a semi-transparent overlay.
     * When disabled, uses standard Material Design surface container color.
     */
    @Composable
    fun getResultCardContainerColor(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground) {
            return ResultCardWallpaperBackground
        }
        val themeCardColor = LocalSearchColorTheme.current?.cardBackground
        val fallback =
            if (!LocalAppIsDarkTheme.current) {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        return themeCardColor ?: fallback
    }

    @Composable
    fun getCardColors(showWallpaperBackground: Boolean): CardColors =
        if (showWallpaperBackground) {
            CardDefaults.cardColors(containerColor = getResultCardContainerColor(true))
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = getResultCardContainerColor(false),
            )
        }

    /**
     * Returns appropriate card elevation based on wallpaper background setting and theme.
     * Cards with wallpaper background or in light mode use no elevation; dark mode uses standard elevation.
     */
    /**
     * Returns the background color for the compact search engine section strip.
     * In light mode with wallpaper, uses white with transparency; otherwise uses black with transparency.
     */
    @Composable
    fun getCompactSectionBackground(showWallpaperBackground: Boolean): Color =
        if (showWallpaperBackground) CompactSectionBackground else Color.Black.copy(alpha = 0.5f)

    @Composable
    fun getCardElevation(showWallpaperBackground: Boolean): CardElevation =
        if (showWallpaperBackground || !LocalAppIsDarkTheme.current) {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
            CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        }

}
