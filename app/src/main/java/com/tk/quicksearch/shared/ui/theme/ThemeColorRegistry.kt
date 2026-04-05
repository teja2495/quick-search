package com.tk.quicksearch.shared.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.core.AppTheme

@Immutable
data class LightModeThemedIconPalette(
    val background: Color,
    val foreground: Color,
)

@Immutable
data class AppThemeColorBundle(
    val accent: ThemeAccentColors,
    val lightGradient: List<Color>,
    val darkGradient: List<Color>,
    val lightThemedIconPalette: LightModeThemedIconPalette,
)

object ThemeColorRegistry {
    // Material You (image-derived accent) tone slots used to build ColorScheme accents.
    const val MaterialYouPrimaryToneDark = 80
    const val MaterialYouOnPrimaryToneDark = 20
    const val MaterialYouContainerToneDark = 30
    const val MaterialYouOnContainerToneDark = 90

    const val MaterialYouPrimaryToneLight = 40
    const val MaterialYouOnPrimaryToneLight = 100
    const val MaterialYouContainerToneLight = 90
    const val MaterialYouOnContainerToneLight = 10

    // Theme-mode fallback vertical scrim overlay on top of the app-theme gradient background.
    val ThemeFallbackScrimDark = listOf(
        Color.Black.copy(alpha = 0.22f),
        Color.Black.copy(alpha = 0.12f),
        Color.Black.copy(alpha = 0.3f),
    )
    val ThemeFallbackScrimLight = listOf(
        Color.White.copy(alpha = 0.12f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.06f),
    )

    private val forest =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF2E7D32),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFC8E6C9),
                    lightOnPrimaryContainer = Color(0xFF1B5E20),
                    lightSecondaryContainer = Color(0xFFC8E6C9),
                    lightOnSecondaryContainer = Color(0xFF1B5E20),
                    darkPrimary = Color(0xFF81C784),
                    darkOnPrimary = Color(0xFF1B3A1D),
                    darkPrimaryContainer = Color(0xFF2E7D32),
                    darkOnPrimaryContainer = Color(0xFFC8E6C9),
                    darkSecondaryContainer = Color(0xFF1B3A1D),
                    darkOnSecondaryContainer = Color(0xFFC8E6C9),
                ),
            lightGradient =
                listOf(
                    Color(0xFFCDE8D8),
                    Color(0xFFD2EACC),
                    Color(0xFFDAECCA),
                    Color(0xFFC4E4D4),
                ),
            darkGradient =
                listOf(
                    Color(0xFF27382F),
                    Color(0xFF2F4640),
                    Color(0xFF435034),
                    Color(0xFF1F3340),
                ),
            lightThemedIconPalette =
                LightModeThemedIconPalette(
                    background = Color(0xFFDDF3D9),
                    foreground = Color(0xFF1F6A31),
                ),
        )

    private val aurora =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF1565C0),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFBBDEFB),
                    lightOnPrimaryContainer = Color(0xFF0D47A1),
                    lightSecondaryContainer = Color(0xFFBBDEFB),
                    lightOnSecondaryContainer = Color(0xFF0D47A1),
                    darkPrimary = Color(0xFF90CAF9),
                    darkOnPrimary = Color(0xFF0D2A5A),
                    darkPrimaryContainer = Color(0xFF1565C0),
                    darkOnPrimaryContainer = Color(0xFFBBDEFB),
                    darkSecondaryContainer = Color(0xFF0D2A5A),
                    darkOnSecondaryContainer = Color(0xFFBBDEFB),
                ),
            lightGradient =
                listOf(
                    Color(0xFFC8DEF8),
                    Color(0xFFC4E4F8),
                    Color(0xFFCECEF8),
                    Color(0xFFC0D8F4),
                ),
            darkGradient =
                listOf(
                    Color(0xFF1F2E4A),
                    Color(0xFF1F4A5A),
                    Color(0xFF3A3E6B),
                    Color(0xFF2A3150),
                ),
            lightThemedIconPalette =
                LightModeThemedIconPalette(
                    background = Color(0xFFD9ECFF),
                    foreground = Color(0xFF0E5AAE),
                ),
        )

    private val sunset =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFFBF360C),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFFFCCBC),
                    lightOnPrimaryContainer = Color(0xFF7B1E00),
                    lightSecondaryContainer = Color(0xFFFFCCBC),
                    lightOnSecondaryContainer = Color(0xFF7B1E00),
                    darkPrimary = Color(0xFFFFAB91),
                    darkOnPrimary = Color(0xFF5C1A06),
                    darkPrimaryContainer = Color(0xFFBF360C),
                    darkOnPrimaryContainer = Color(0xFFFFCCBC),
                    darkSecondaryContainer = Color(0xFF5C1A06),
                    darkOnSecondaryContainer = Color(0xFFFFCCBC),
                ),
            lightGradient =
                listOf(
                    Color(0xFFF5C8B4),
                    Color(0xFFF5D8B0),
                    Color(0xFFECC4D0),
                    Color(0xFFF0C0BC),
                ),
            darkGradient =
                listOf(
                    Color(0xFF4A2C34),
                    Color(0xFF5A3A2A),
                    Color(0xFF5C3046),
                    Color(0xFF3E2A3B),
                ),
            lightThemedIconPalette =
                LightModeThemedIconPalette(
                    background = Color(0xFFFFE3D6),
                    foreground = Color(0xFFAA3008),
                ),
        )

    private val monochrome =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF212121),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFE0E0E0),
                    lightOnPrimaryContainer = Color(0xFF121212),
                    lightSecondaryContainer = Color(0xFFF0EEEB),
                    lightOnSecondaryContainer = Color(0xFF212121),
                    darkPrimary = Color(0xFFD4D0C8),
                    darkOnPrimary = Color(0xFF121212),
                    darkPrimaryContainer = Color(0xFF2C2C2C),
                    darkOnPrimaryContainer = Color(0xFFE0E0E0),
                    darkSecondaryContainer = Color(0xFF1E1E1E),
                    darkOnSecondaryContainer = Color(0xFFD0D0D0),
                ),
            lightGradient =
                listOf(
                    Color(0xFFEDEAE4),
                    Color(0xFFE0DCD6),
                    Color(0xFFD4D0C8),
                    Color(0xFFC6C2BA),
                ),
            darkGradient =
                listOf(
                    Color(0xFF0A0A0A),
                    Color(0xFF1E1E1E),
                    Color(0xFF2C2C2C),
                    Color(0xFF141414),
                ),
            lightThemedIconPalette =
                LightModeThemedIconPalette(
                    background = Color(0xFFE8E6E2),
                    foreground = Color(0xFF1F1F1F),
                ),
        )

    private fun bundle(theme: AppTheme): AppThemeColorBundle =
        when (theme) {
            AppTheme.FOREST -> forest
            AppTheme.AURORA -> aurora
            AppTheme.SUNSET -> sunset
            AppTheme.MONOCHROME -> monochrome
        }

    fun accent(theme: AppTheme): ThemeAccentColors = bundle(theme).accent

    fun gradient(
        theme: AppTheme,
        isDarkMode: Boolean,
    ): List<Color> = if (isDarkMode) bundle(theme).darkGradient else bundle(theme).lightGradient

    fun lightModeThemedIconPalette(theme: AppTheme): LightModeThemedIconPalette =
        bundle(theme).lightThemedIconPalette
}
