package com.tk.quicksearch.shared.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.core.AppTheme
import me.tatarka.google.material.palettes.TonalPalette

@Immutable
data class ThemedIconColors(
    val background: Color,
    val foreground: Color,
)

@Immutable
data class AppThemeColorBundle(
    val accent: ThemeAccentColors,
    val lightGradient: List<Color>,
    val darkGradient: List<Color>,
    val lightThemedIconColors: ThemedIconColors,
    val darkThemedIconColors: ThemedIconColors,
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
            lightThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFDDF3D9),
                    foreground = Color(0xFF1F6A31),
                ),
            darkThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFC8E6C9),
                    foreground = Color(0xFF2E7D32),
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
            lightThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFD9ECFF),
                    foreground = Color(0xFF0E5AAE),
                ),
            darkThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFBBDEFB),
                    foreground = Color(0xFF1565C0),
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
            lightThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFFFE3D6),
                    foreground = Color(0xFFAA3008),
                ),
            darkThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFFFCCBC),
                    foreground = Color(0xFFBF360C),
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
            lightThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFE8E6E2),
                    foreground = Color(0xFF1F1F1F),
                ),
            darkThemedIconColors =
                ThemedIconColors(
                    background = Color(0xFFE0E0E0),
                    foreground = Color(0xFF2C2C2C),
                ),
        )

    private val lavender =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF6A1B9A),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFE1BEE7),
                    lightOnPrimaryContainer = Color(0xFF4A148C),
                    lightSecondaryContainer = Color(0xFFE1BEE7),
                    lightOnSecondaryContainer = Color(0xFF4A148C),
                    darkPrimary = Color(0xFFCE93D8),
                    darkOnPrimary = Color(0xFF3A0F4F),
                    darkPrimaryContainer = Color(0xFF6A1B9A),
                    darkOnPrimaryContainer = Color(0xFFE1BEE7),
                    darkSecondaryContainer = Color(0xFF3A0F4F),
                    darkOnSecondaryContainer = Color(0xFFE1BEE7),
                ),
            lightGradient = listOf(Color(0xFFE2D0F0), Color(0xFFE8D4EC), Color(0xFFD8CCF2), Color(0xFFDCC8EC)),
            darkGradient = listOf(Color(0xFF33264A), Color(0xFF40305A), Color(0xFF4A2E52), Color(0xFF2A2440)),
            lightThemedIconColors = ThemedIconColors(background = Color(0xFFF0E0F8), foreground = Color(0xFF5E1890)),
            darkThemedIconColors = ThemedIconColors(background = Color(0xFFE1BEE7), foreground = Color(0xFF6A1B9A)),
        )

    private val teal =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF00796B),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFB2DFDB),
                    lightOnPrimaryContainer = Color(0xFF004D40),
                    lightSecondaryContainer = Color(0xFFB2DFDB),
                    lightOnSecondaryContainer = Color(0xFF004D40),
                    darkPrimary = Color(0xFF80CBC4),
                    darkOnPrimary = Color(0xFF003731),
                    darkPrimaryContainer = Color(0xFF00796B),
                    darkOnPrimaryContainer = Color(0xFFB2DFDB),
                    darkSecondaryContainer = Color(0xFF003731),
                    darkOnSecondaryContainer = Color(0xFFB2DFDB),
                ),
            lightGradient = listOf(Color(0xFFC4E8E4), Color(0xFFCCECE6), Color(0xFFC0E2EA), Color(0xFFBEE4DC)),
            darkGradient = listOf(Color(0xFF1F3A38), Color(0xFF24484A), Color(0xFF2A4440), Color(0xFF1A3236)),
            lightThemedIconColors = ThemedIconColors(background = Color(0xFFD6F2EE), foreground = Color(0xFF006B5F)),
            darkThemedIconColors = ThemedIconColors(background = Color(0xFFB2DFDB), foreground = Color(0xFF00796B)),
        )

    private val amber =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF8D6E00),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFFFECB3),
                    lightOnPrimaryContainer = Color(0xFF5C4300),
                    lightSecondaryContainer = Color(0xFFFFECB3),
                    lightOnSecondaryContainer = Color(0xFF5C4300),
                    darkPrimary = Color(0xFFFFD54F),
                    darkOnPrimary = Color(0xFF3E2E00),
                    darkPrimaryContainer = Color(0xFF8D6E00),
                    darkOnPrimaryContainer = Color(0xFFFFECB3),
                    darkSecondaryContainer = Color(0xFF3E2E00),
                    darkOnSecondaryContainer = Color(0xFFFFECB3),
                ),
            lightGradient = listOf(Color(0xFFF5E6B8), Color(0xFFF2EAC0), Color(0xFFF0DEB4), Color(0xFFECE2B0)),
            darkGradient = listOf(Color(0xFF4A3E20), Color(0xFF554828), Color(0xFF4E4024), Color(0xFF3E3420)),
            lightThemedIconColors = ThemedIconColors(background = Color(0xFFFFF2CC), foreground = Color(0xFF7A5E00)),
            darkThemedIconColors = ThemedIconColors(background = Color(0xFFFFECB3), foreground = Color(0xFF8D6E00)),
        )

    private val indigo =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF3949AB),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFC5CAE9),
                    lightOnPrimaryContainer = Color(0xFF1A237E),
                    lightSecondaryContainer = Color(0xFFC5CAE9),
                    lightOnSecondaryContainer = Color(0xFF1A237E),
                    darkPrimary = Color(0xFF9FA8DA),
                    darkOnPrimary = Color(0xFF1A2050),
                    darkPrimaryContainer = Color(0xFF3949AB),
                    darkOnPrimaryContainer = Color(0xFFC5CAE9),
                    darkSecondaryContainer = Color(0xFF1A2050),
                    darkOnSecondaryContainer = Color(0xFFC5CAE9),
                ),
            lightGradient = listOf(Color(0xFFD0D4F0), Color(0xFFD6D8F2), Color(0xFFCCCCF0), Color(0xFFC8CEEC)),
            darkGradient = listOf(Color(0xFF262A4A), Color(0xFF2E3358), Color(0xFF322E55), Color(0xFF20243E)),
            lightThemedIconColors = ThemedIconColors(background = Color(0xFFE0E3F8), foreground = Color(0xFF303F9F)),
            darkThemedIconColors = ThemedIconColors(background = Color(0xFFC5CAE9), foreground = Color(0xFF3949AB)),
        )

    private val slate =
        AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = Color(0xFF455A64),
                    lightOnPrimary = Color(0xFFFFFFFF),
                    lightPrimaryContainer = Color(0xFFCFD8DC),
                    lightOnPrimaryContainer = Color(0xFF263238),
                    lightSecondaryContainer = Color(0xFFCFD8DC),
                    lightOnSecondaryContainer = Color(0xFF263238),
                    darkPrimary = Color(0xFFB0BEC5),
                    darkOnPrimary = Color(0xFF1F2A30),
                    darkPrimaryContainer = Color(0xFF455A64),
                    darkOnPrimaryContainer = Color(0xFFCFD8DC),
                    darkSecondaryContainer = Color(0xFF1F2A30),
                    darkOnSecondaryContainer = Color(0xFFCFD8DC),
                ),
            lightGradient = listOf(Color(0xFFD4DCE0), Color(0xFFD8E0E4), Color(0xFFCCD6DC), Color(0xFFD0D8DE)),
            darkGradient = listOf(Color(0xFF263036), Color(0xFF2E3A42), Color(0xFF2A343C), Color(0xFF20292F)),
            lightThemedIconColors = ThemedIconColors(background = Color(0xFFE3EAED), foreground = Color(0xFF37474F)),
            darkThemedIconColors = ThemedIconColors(background = Color(0xFFCFD8DC), foreground = Color(0xFF455A64)),
        )

    // Palettes generated from a hue and chroma (see generateBundle); lazy so startup only builds
    // the one in use.
    private val rose by lazy { generateBundle(hue = 355.0, chroma = 62.0) }
    private val mocha by lazy { generateBundle(hue = 48.0, chroma = 18.0) }

    /**
     * Builds a palette in the same shape as the hand-tuned presets from one hue and chroma: the
     * accent keeps the full chroma, the background gradient a softer one near the same hue.
     */
    private fun generateBundle(
        hue: Double,
        chroma: Double,
    ): AppThemeColorBundle {
        val primary = TonalPalette.fromHueAndChroma(hue, chroma)
        // Background tones carry less chroma than the accent, like the presets' pastel gradients.
        val backgroundChroma = (chroma * 0.45).coerceAtMost(40.0)
        val backgroundA = TonalPalette.fromHueAndChroma(hue, backgroundChroma)
        val backgroundB = TonalPalette.fromHueAndChroma((hue + 12.0) % 360.0, backgroundChroma * 0.85)
        val backgroundC = TonalPalette.fromHueAndChroma((hue + 348.0) % 360.0, backgroundChroma)
        fun Int.c() = Color(this)
        return AppThemeColorBundle(
            accent =
                ThemeAccentColors(
                    lightPrimary = primary.tone(40).c(),
                    lightOnPrimary = primary.tone(100).c(),
                    lightPrimaryContainer = primary.tone(90).c(),
                    lightOnPrimaryContainer = primary.tone(10).c(),
                    lightSecondaryContainer = primary.tone(90).c(),
                    lightOnSecondaryContainer = primary.tone(10).c(),
                    darkPrimary = primary.tone(80).c(),
                    darkOnPrimary = primary.tone(20).c(),
                    darkPrimaryContainer = primary.tone(30).c(),
                    darkOnPrimaryContainer = primary.tone(90).c(),
                    darkSecondaryContainer = primary.tone(20).c(),
                    darkOnSecondaryContainer = primary.tone(90).c(),
                ),
            lightGradient =
                listOf(backgroundA.tone(87).c(), backgroundB.tone(90).c(), backgroundC.tone(88).c(), backgroundA.tone(85).c()),
            darkGradient =
                listOf(backgroundA.tone(21).c(), backgroundB.tone(27).c(), backgroundC.tone(25).c(), backgroundA.tone(17).c()),
            lightThemedIconColors =
                ThemedIconColors(background = primary.tone(94).c(), foreground = primary.tone(35).c()),
            darkThemedIconColors =
                ThemedIconColors(background = primary.tone(90).c(), foreground = primary.tone(40).c()),
        )
    }

    private fun bundle(theme: AppTheme): AppThemeColorBundle =
        when (theme) {
            AppTheme.FOREST -> forest
            AppTheme.AURORA -> aurora
            AppTheme.SUNSET -> sunset
            AppTheme.MONOCHROME -> monochrome
            AppTheme.LAVENDER -> lavender
            AppTheme.TEAL -> teal
            AppTheme.AMBER -> amber
            AppTheme.INDIGO -> indigo
            AppTheme.SLATE -> slate
            AppTheme.ROSE -> rose
            AppTheme.MOCHA -> mocha
        }

    fun accent(theme: AppTheme): ThemeAccentColors = bundle(theme).accent

    fun gradient(
        theme: AppTheme,
        isDarkMode: Boolean,
    ): List<Color> = if (isDarkMode) bundle(theme).darkGradient else bundle(theme).lightGradient

}
