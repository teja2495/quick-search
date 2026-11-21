package com.tk.quicksearch.shared.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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

    // Accent tints -------------------------------------------------------------------------
    // Translucent accent fills for tonal surfaces (icon badges, tonal buttons, selected rows).
    // They follow the accent through Material You, custom, wallpaper, and monochrome themes and
    // stay readable over wallpaper and AMOLED, so use these instead of `Accent.copy(alpha = ...)`.

    /** 5% accent: disabled tonal fields. */
    val AccentTintFaint: Color
        @Composable
        get() = Accent.copy(alpha = TintFaintAlpha)

    /** 8% accent: quiet panels and unselected chips and slots. */
    val AccentTintSubtle: Color
        @Composable
        get() = Accent.copy(alpha = TintSubtleAlpha)

    /** 12% accent: light fills such as alias chips and progress tracks. */
    val AccentTintLight: Color
        @Composable
        get() = Accent.copy(alpha = TintLightAlpha)

    /** 14% accent: the standard tonal container (icon badges, tonal buttons, selected rows). */
    val AccentTintTonal: Color
        @Composable
        get() = Accent.copy(alpha = TintTonalAlpha)

    /** 24% accent: strong selection (dragged tiles, selected chips, floating tonal actions). */
    val AccentTintStrong: Color
        @Composable
        get() = Accent.copy(alpha = TintStrongAlpha)

    /** 30% accent: emphasised selection over a busy surface. */
    val AccentTintEmphasis: Color
        @Composable
        get() = Accent.copy(alpha = TintEmphasisAlpha)

    /**
     * Container of a secondary floating action beside an accent one (Search beside Add): the strong
     * accent tint made opaque over a raised surface, since it floats over a list.
     */
    val FloatingTonalContainer: Color
        @Composable
        get() = AccentTintStrong.compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)

    /**
     * Slider colours. Monochrome's `secondaryContainer` (the M3 inactive-track role) is a near-match
     * for card surfaces, so Mono draws the inactive track and ticks from `onSurface` instead.
     */
    @Composable
    fun sliderColors(): SliderColors =
        if (LocalAppTheme.current == AppTheme.MONOCHROME) {
            SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TintStrongAlpha),
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SliderDefaults.colors()
        }

    // Tint opacities, also for tonal fills of other roles (error, content colour).
    const val TintFaintAlpha = 0.05f
    const val TintSubtleAlpha = 0.08f
    const val TintLightAlpha = 0.12f
    const val TintTonalAlpha = 0.14f
    const val TintStrongAlpha = 0.24f
    const val TintEmphasisAlpha = 0.3f

    // Content and state opacities. Every alpha applied to a theme color outside this file comes
    // from the tint scale above or this scale, so a new design language retunes them here.

    /** Disabled content (Material state layer). */
    const val DisabledContentAlpha = 0.38f

    /** Faint content: hints, inactive decorations. */
    const val ContentFaintAlpha = 0.4f

    /** Muted content: placeholders, tertiary text. */
    const val ContentMutedAlpha = 0.5f

    /** Secondary content: supporting text on tinted or wallpaper surfaces. */
    const val ContentSecondaryAlpha = 0.6f

    /** High-emphasis translucent content. */
    const val ContentHighAlpha = 0.7f

    /** Strong translucent content and scrims. */
    const val ContentStrongAlpha = 0.8f

    /** Nearly opaque content and scrims. */
    const val ContentNearOpaqueAlpha = 0.9f

    /** Alpha of the secondary container behind a secondary tonal button, so it sits softly on result cards. */
    const val SecondaryTonalContainerAlpha = 0.6f

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
     * on, …). Uses the theme accent, or in monochrome the same blue as [LinkColor] since a grey
     * accent wouldn't read as "active". It is strengthened toward the surface foreground (lighter
     * in dark mode, darker in light mode) only as far as needed to reach 4.5:1 against every
     * neutral popup surface.
     */
    val ItemMenuActiveIconTint: Color
        @Composable
        get() {
            val scheme = MaterialTheme.colorScheme
            return contrastingItemMenuAccent(
                accent = LinkColor,
                foreground = scheme.onSurface,
                backgrounds = listOf(
                    scheme.surfaceContainerLow,
                    scheme.surfaceContainer,
                    scheme.surfaceContainerHigh,
                    scheme.surfaceContainerHighest,
                ),
            )
        }

    // Icon tints ---------------------------------------------------------------------------

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

    // Same fill as the compact engine strip, so the bar and strip read as one surface by default.
    @Composable
    fun getSearchBarBackground(showWallpaperBackground: Boolean): Color =
        getSearchEngineSectionBackground(showWallpaperBackground)

    val SearchBarTextAndIcon: Color
        @Composable
        get() = current.searchBarTextAndIcon

    @Composable
    fun getSearchBarTextAndIconColor(showWallpaperBackground: Boolean): Color =
        if (usesDarkDynamicSearchSurface(showWallpaperBackground)) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            SearchBarTextAndIcon
        }

    @Composable
    private fun usesDarkDynamicSearchSurface(showWallpaperBackground: Boolean): Boolean =
        showWallpaperBackground && LocalDeviceDynamicColorsActive.current && LocalAppIsDarkTheme.current

    @Composable
    fun getSearchBarSecondaryIconTint(showWallpaperBackground: Boolean): Color =
        if (usesDarkDynamicSearchSurface(showWallpaperBackground)) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            SecondaryIconTint
        }

    /** Solid charcoal for elevated cards on AMOLED true-black backgrounds. */
    val AmoledCardBackground: Color = Color(0xFF161616)

    val OverlayMedium: Color
        @Composable
        get() = current.overlayMedium

    val OverlayVeryHigh: Color
        @Composable
        get() = current.overlayVeryHigh

    val DialogBackground: Color
        @Composable
        get() = getDialogContainerColor()

    /**
     * Tile and field fill on a drawer or sheet ([DialogBackground]). The settings card color is
     * tuned for the settings page background and looks wrong on the drawer surface.
     */
    val DrawerTileBackground: Color
        @Composable
        get() = AccentTintSubtle

    val DialogText: Color
        @Composable
        get() = current.dialogText

    val ActionPhone: Color
        @Composable
        get() = current.actionPhone

    val ActionSms: Color
        @Composable
        get() = current.actionSms

    val ActionSignal: Color
        @Composable
        get() = current.actionSignal

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

    // Keyboard action buttons (open / switch) -----------------------------------------------

    // Wallpaper-mode text & icon colors ----------------------------------------------------

    /** Primary text color used in result rows when the wallpaper background is active. */
    val WallpaperTextPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface

    /** Secondary/icon color used in result rows when the wallpaper background is active. */
    val WallpaperTextSecondary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** Folder tile backdrop over a wallpaper/custom image (scrim that keeps the preview icons legible). */
    val FolderWallpaperBackdrop: Color
        @Composable
        get() =
            if (LocalAppIsDarkTheme.current) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.65f)

    /** Hairline border around folder tiles. */
    val FolderTileBorder = Color.White.copy(alpha = 0.25f)

    /**
     * Neutral base that grid indicators blend the primary color into: white on dark surfaces,
     * black on light ones.
     */
    fun indicatorBase(lightFill: Boolean): Color = if (lightFill) Color.White else Color.Black

    /** Accent for open-source / privacy trust messaging; green tuned per surface luminance. */
    val TrustAccent: Color
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) TrustAccentOnDark else TrustAccentOnLight

    private val TrustAccentOnDark = Color(0xFF4ADE80)
    private val TrustAccentOnLight = Color(0xFF15803D)

    /** Border on result cards over a wallpaper/custom image in dark mode. */
    val WallpaperDarkCardBorder = Color.White.copy(alpha = 0.12f)

    /** Divider color used between result rows when the wallpaper background is active. */
    val WallpaperDivider: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    /**
     * Muted empty-state / helper text on the search surface (e.g. global “no results” line).
     * With an image background, uses white or black (per [homeBackgroundIsDark]) at [alpha] like app grid labels;
     * otherwise [MaterialTheme.colorScheme.onSurface] at [alpha].
     */
    @Composable
    fun wallpaperAwareMutedSearchForeground(alpha: Float = 0.6f): Color {
        val imageBackgroundIsDark = homeBackgroundIsDark()
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

    /** Subtle divider between the evenly spaced engines shown when the strip has only a few. */
    val InlineEngineDivider: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

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
        )

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
     * Resolved container [Color] for search result cards.
     */
    @Composable
    fun getSearchResultCardContainerColor(
        showWallpaperBackground: Boolean,
        overlayContainerColor: Color?,
    ): Color = overlayContainerColor ?: getResultCardContainerColor(showWallpaperBackground)

    /**
     * Background color for the custom search engine section strip.
     * In light mode with wallpaper/custom image, uses a slightly stronger scrim for readability.
     */
    @Composable
    fun getSearchEngineSectionBackground(showWallpaperBackground: Boolean): Color {
        if (showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
            // Dark Material You matches the Pixel Launcher's Google bar: a tinted secondary container.
            return if (LocalAppIsDarkTheme.current) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
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

}
