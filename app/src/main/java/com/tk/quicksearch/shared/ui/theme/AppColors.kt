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

    /**
     * Color for interactive/tappable items such as links, clickable text, and alias pills.
     *
     * In monochrome mode the primary accent is grey/black, which doesn't signal interactivity.
     * This token overrides to a recognisable blue in monochrome so links remain distinguishable,
     * unless image-derived accent is active — then [MaterialTheme.colorScheme.primary]
     * already reflects the current background image accent and is used here.
     */
    val LinkColor: Color
        @Composable
        get() =
            if (LocalWallpaperDynamicAccentActive.current) {
                MaterialTheme.colorScheme.primary
            } else if (LocalAppTheme.current == AppTheme.MONOCHROME) {
                if (LocalAppIsDarkTheme.current) Color(0xFF90CAF9) else Color(0xFF1565C0)
            } else {
                MaterialTheme.colorScheme.primary
            }

    /**
     * Tint for long-press menu option icons whose setting is active (pinned, trigger set, SpeedBump
     * on, …). Uses the wallpaper/custom/device accent when one is applied; otherwise a softer green
     * tuned for the light or dark theme.
     */
    val ItemMenuActiveIconTint: Color
        @Composable
        get() =
            if (LocalWallpaperDynamicAccentActive.current || LocalDeviceDynamicColorsActive.current) {
                MaterialTheme.colorScheme.primary
            } else if (LocalAppIsDarkTheme.current) {
                Color(0xFF81C784)
            } else {
                Color(0xFF388E3C)
            }

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

    /**
     * Secondary accent icon tint — the grey used for neutral/secondary icons (search, settings,
     * phone, SMS) in light mode. White in dark mode.
     */
    val SecondaryIconTint: Color
        @Composable
        get() = if (LocalAppIsDarkTheme.current) Color.White else Color(0xFF57515E)

    /**
     * Tint for phone/call icons across all calling action buttons.
     * Delegates to [SecondaryIconTint].
     */
    val CallIconTint: Color
        @Composable
        get() = SecondaryIconTint

    // Settings colors ----------------------------------------------------------------------

    val SearchBarBackground: Color
        @Composable
        get() = current.searchBarBackground

    @Composable
    fun getSearchBarBackground(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            return MaterialTheme.colorScheme.surfaceContainerHigh
        }
        return SearchBarBackground
    }

    val SearchBarBorder: Color
        @Composable
        get() = current.searchBarBorder

    val SearchBarTextAndIcon: Color
        @Composable
        get() = current.searchBarTextAndIcon

    @Composable
    fun getSearchBarTextAndIconColor(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            return MaterialTheme.colorScheme.onSurface
        }
        return SearchBarTextAndIcon
    }

    @Composable
    fun getSearchBarSecondaryIconTint(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            return MaterialTheme.colorScheme.onSurfaceVariant
        }
        return SecondaryIconTint
    }

    val SettingsBackground: Color = Color.Transparent

    /** Solid charcoal for elevated cards on AMOLED true-black backgrounds. */
    val AmoledCardBackground: Color = Color(0xFF161616)

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
        get() = getDialogContainerColor()

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

    /** Compact search engine strip when wallpaper scrim is on; light theme matches result card frost. */
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

    /**
     * Muted empty-state / helper text on the search surface (e.g. global “no results” line).
     * When [LocalImageBackgroundIsDark] is set, uses white or black at [alpha] like app grid labels;
     * otherwise [MaterialTheme.colorScheme.onSurface] at [alpha].
     */
    @Composable
    fun wallpaperAwareMutedSearchForeground(alpha: Float = 0.6f): Color {
        val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
        return when (imageBackgroundIsDark) {
            true -> Color.White.copy(alpha = alpha)
            false -> Color.Black.copy(alpha = alpha)
            null -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        }
    }

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

    /** Border color for keyboard operator pill chips. */
    val KeyboardPillBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    /**
     * Outline for the persistent search bar and number-keyboard operator pills.
     * In dark mode uses [Accent] at the same alpha as [KeyboardPillBorder] so the stroke reads as
     * on-brand accent; in light mode matches [KeyboardPillBorder].
     */
    val SearchChromeOutlineBorder: Color
        @Composable
        get() =
            if (LocalAppIsDarkTheme.current) {
                Accent.copy(alpha = 0.22f)
            } else {
                KeyboardPillBorder
            }

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
    val CustomImageEditButtonBackground: Color = Color.Black
    val CustomImageEditButtonIcon: Color = Color.White

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

    val ForestDarkPalette
        get() = ThemeColorRegistry.gradient(AppTheme.FOREST, isDarkMode = true)

    val AuroraDarkPalette
        get() = ThemeColorRegistry.gradient(AppTheme.AURORA, isDarkMode = true)

    val SunsetDarkPalette
        get() = ThemeColorRegistry.gradient(AppTheme.SUNSET, isDarkMode = true)

    val MonochromeDarkPalette
        get() = ThemeColorRegistry.gradient(AppTheme.MONOCHROME, isDarkMode = true)

    val ForestLightPalette
        get() = ThemeColorRegistry.gradient(AppTheme.FOREST, isDarkMode = false)

    val AuroraLightPalette
        get() = ThemeColorRegistry.gradient(AppTheme.AURORA, isDarkMode = false)

    val SunsetLightPalette
        get() = ThemeColorRegistry.gradient(AppTheme.SUNSET, isDarkMode = false)

    val MonochromeLightPalette
        get() = ThemeColorRegistry.gradient(AppTheme.MONOCHROME, isDarkMode = false)

    val ThemeFallbackGradientScrimColors: List<Color>
        @Composable
        get() = if (LocalAppIsDarkTheme.current) ThemeColorRegistry.ThemeFallbackScrimDark else ThemeColorRegistry.ThemeFallbackScrimLight

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
            if (LocalDeviceDynamicColorsActive.current) {
                val alpha =
                    if (LocalAppIsDarkTheme.current) {
                        DarkWallpaperSearchSurfaceAlpha
                    } else {
                        LightWallpaperSearchResultCardAlpha
                    }
                return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
            }
            return ResultCardWallpaperBackground
        }
        val themeCardColor = LocalSearchColorTheme.current?.cardBackground
        val fallback =
            if (!LocalAppIsDarkTheme.current) {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
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
     * Resolved container colors for search result cards (history, suggestions, section lists,
     * engine cards, AI search). When [overlayContainerColor] is non-null (overlay theme),
     * it wins; otherwise uses [getCardColors] for wallpaper vs standard surfaces.
     */
    @Composable
    fun getSearchResultCardColors(
        showWallpaperBackground: Boolean,
        overlayContainerColor: Color?,
    ): CardColors =
        if (overlayContainerColor != null) {
            CardDefaults.cardColors(containerColor = overlayContainerColor)
        } else {
            getCardColors(showWallpaperBackground)
        }

    /**
     * Resolved container [Color] for search result cards — same rules as [getSearchResultCardColors].
     */
    @Composable
    fun getSearchResultCardContainerColor(
        showWallpaperBackground: Boolean,
        overlayContainerColor: Color?,
    ): Color = overlayContainerColor ?: getResultCardContainerColor(showWallpaperBackground)

    /**
     * Returns flat card elevation for all app cards.
     * Result, suggestions, history, and settings cards are intentionally elevation-free.
     */
    /**
     * Returns the background color for the compact search engine section strip.
     * With wallpaper: palette scrim (matches [ResultCardWallpaperBackground]); solid white (light) or
     * black (dark) otherwise.
     */
    @Composable
    fun getCompactSectionBackground(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            return MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val base = if (!LocalAppIsDarkTheme.current) Color.White else Color.Black
        return base.copy(alpha = CompactSectionAlpha)
    }

    /**
     * Background color for the custom search engine section strip.
     * In light mode with wallpaper/custom image, uses a slightly stronger scrim for readability.
     */
    @Composable
    fun getSearchEngineSectionBackground(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            return MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val isLightWithWallpaper = !LocalAppIsDarkTheme.current && showWallpaperBackground
        val base = if (!LocalAppIsDarkTheme.current) Color.White else Color.Black
        val alpha = if (isLightWithWallpaper) LightWallpaperSearchResultCardAlpha else CompactSectionAlpha
        return base.copy(alpha = alpha)
    }

    /**
     * Background color for settings cards and the management search bar.
     * Mono dark theme matches result cards (surfaceContainer). All other themes use
     * plain White (light) or Black (dark) with the same alpha as result cards.
     */
    /**
     * Divider and border color for settings rows and cards.
     * White at low alpha in dark mode; Material outlineVariant in light mode.
     */
    val SettingsDivider: Color
        @Composable
        get() = if (LocalAppIsDarkTheme.current) {
            Color.White.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    @Composable
    fun getDialogContainerColor(): Color =
        if (LocalDeviceDynamicColorsActive.current) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            current.dialogBackground
        }

    @Composable
    fun getDrawerContainerColor(): Color =
        if (LocalDeviceDynamicColorsActive.current) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else if (LocalAppIsDarkTheme.current) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }

    @Composable
    fun getSettingsCardContainerColor(): Color =
        if (LocalAmoledThemeActive.current) {
            AmoledCardBackground
        } else if (LocalSearchColorTheme.current == null) {
            MaterialTheme.colorScheme.surfaceContainer
        } else if (LocalAppIsDarkTheme.current) {
            Color.Black.copy(alpha = 0.4f)
        } else {
            Color.White.copy(alpha = 0.7f)
        }

    @Composable
    fun getCardElevation(showWallpaperBackground: Boolean): CardElevation =
        CardDefaults.cardElevation(defaultElevation = 0.dp)

}
