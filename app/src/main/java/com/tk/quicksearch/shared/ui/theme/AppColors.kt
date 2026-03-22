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
    )

internal val LightQuickSearchAppColorPalette =
    QuickSearchAppColorPalette(
        searchBarBackground = Color.Black.copy(alpha = 0.08f),
        searchBarBorder = Color.Black.copy(alpha = 0.24f),
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
    )

internal val LocalQuickSearchAppColorPalette =
    staticCompositionLocalOf<QuickSearchAppColorPalette> {
        DarkQuickSearchAppColorPalette
    }

internal val LocalAppIsDarkTheme = staticCompositionLocalOf { true }

object AppColors {
    // Theme-aware semantic colors ------------------------------------------------------------

    private val current: QuickSearchAppColorPalette
        @Composable
        get() = LocalQuickSearchAppColorPalette.current

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

    // Shared/static tokens -----------------------------------------------------------------

    val AppBackgroundTransparent: Color = Color.Transparent
    val AppBackgroundDark: Color = Color(0xFF121212)

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
    // OVERLAY GRADIENT PALETTES
    // ============================================================================

    val OverlayForestDarkPalette =
        listOf(
            Color(0xFF27382F),
            Color(0xFF2F4640),
            Color(0xFF435034),
            Color(0xFF1F3340),
        )

    val OverlayAuroraDarkPalette =
        listOf(
            Color(0xFF1F2E4A),
            Color(0xFF1F4A5A),
            Color(0xFF3A3E6B),
            Color(0xFF2A3150),
        )

    val OverlaySunsetDarkPalette =
        listOf(
            Color(0xFF4A2C34),
            Color(0xFF5A3A2A),
            Color(0xFF5C3046),
            Color(0xFF3E2A3B),
        )

    val OverlayMonochromeDarkPalette =
        listOf(
            AppBackgroundDark,
            Color(0xFF2A2A2A),
            Color(0xFF3E3E3E),
            Color(0xFFE8E8E8),
        )

    val OverlayForestLightPalette =
        listOf(
            Color(0xFFE4ECE7),
            Color(0xFFE4ECE9),
            Color(0xFFEBEEE2),
            Color(0xFFE0E9EC),
        )

    val OverlayAuroraLightPalette =
        listOf(
            Color(0xFFDCE8F8),
            Color(0xFFD8F1F0),
            Color(0xFFE2E2FA),
            Color(0xFFDCE6F4),
        )

    val OverlaySunsetLightPalette =
        listOf(
            Color(0xFFF8E1D8),
            Color(0xFFF8E8D8),
            Color(0xFFF4DCE8),
            Color(0xFFF6E1DF),
        )

    val OverlayMonochromeLightPalette =
        listOf(
            Color(0xFFF0F0F0),
            Color(0xFFE2E2E2),
            Color(0xFFD5D5D5),
            Color(0xFFBFBFBF),
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
    fun getCardColors(showWallpaperBackground: Boolean): CardColors =
        if (showWallpaperBackground) {
            CardDefaults.cardColors(
                containerColor = OverlayMedium,
            )
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }

    /**
     * Returns appropriate card elevation based on wallpaper background setting and theme.
     * Cards with wallpaper background or in light mode use no elevation; dark mode uses standard elevation.
     */
    @Composable
    fun getCardElevation(showWallpaperBackground: Boolean): CardElevation =
        if (showWallpaperBackground || !LocalAppIsDarkTheme.current) {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
            CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        }

}
