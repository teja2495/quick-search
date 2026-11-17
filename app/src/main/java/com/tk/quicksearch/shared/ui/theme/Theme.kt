@file:Suppress("DEPRECATION")

package com.tk.quicksearch.shared.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.tatarka.google.material.palettes.CorePalette
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.shared.util.ImageAppearance
import com.tk.quicksearch.shared.util.WallpaperUtils

// ============================================================================
// Base Color Schemes
// ============================================================================

/**
 * Dark theme color scheme following Material Design 3 specifications.
 * Accent (primary) colors are overridden per-theme at runtime.
 */
private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        outline = md_theme_dark_outline,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        outline = md_theme_light_outline,
    )

private fun accentForTheme(appTheme: com.tk.quicksearch.search.core.AppTheme): ThemeAccentColors =
    ThemeColorRegistry.accent(appTheme)

private data class ImageAccentSlots(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
)

private fun resolveImageAccentSlots(
    baseColorArgb: Int,
    useDarkTheme: Boolean,
): ImageAccentSlots {
    val palette = CorePalette.of(baseColorArgb)
    val primaryPalette = palette.a1
    val secondaryPalette = palette.a2
    val tertiaryPalette = palette.a3

    val primaryTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouPrimaryToneDark else ThemeColorRegistry.MaterialYouPrimaryToneLight
    val onPrimaryTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouOnPrimaryToneDark else ThemeColorRegistry.MaterialYouOnPrimaryToneLight
    val tertiaryTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouPrimaryToneDark else ThemeColorRegistry.MaterialYouPrimaryToneLight
    val onTertiaryTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouOnPrimaryToneDark else ThemeColorRegistry.MaterialYouOnPrimaryToneLight
    val primaryContainerTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouContainerToneDark else ThemeColorRegistry.MaterialYouContainerToneLight
    val onPrimaryContainerTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouOnContainerToneDark else ThemeColorRegistry.MaterialYouOnContainerToneLight
    val secondaryTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouPrimaryToneDark else ThemeColorRegistry.MaterialYouPrimaryToneLight
    val onSecondaryTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouOnPrimaryToneDark else ThemeColorRegistry.MaterialYouOnPrimaryToneLight
    val secondaryContainerTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouContainerToneDark else ThemeColorRegistry.MaterialYouContainerToneLight
    val onSecondaryContainerTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouOnContainerToneDark else ThemeColorRegistry.MaterialYouOnContainerToneLight
    val tertiaryContainerTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouContainerToneDark else ThemeColorRegistry.MaterialYouContainerToneLight
    val onTertiaryContainerTone =
        if (useDarkTheme) ThemeColorRegistry.MaterialYouOnContainerToneDark else ThemeColorRegistry.MaterialYouOnContainerToneLight

    return ImageAccentSlots(
        primary = Color(primaryPalette.tone(primaryTone)),
        onPrimary = Color(primaryPalette.tone(onPrimaryTone)),
        secondary = Color(secondaryPalette.tone(secondaryTone)),
        onSecondary = Color(secondaryPalette.tone(onSecondaryTone)),
        tertiary = Color(tertiaryPalette.tone(tertiaryTone)),
        onTertiary = Color(tertiaryPalette.tone(onTertiaryTone)),
        primaryContainer = Color(primaryPalette.tone(primaryContainerTone)),
        onPrimaryContainer = Color(primaryPalette.tone(onPrimaryContainerTone)),
        secondaryContainer = Color(secondaryPalette.tone(secondaryContainerTone)),
        onSecondaryContainer = Color(secondaryPalette.tone(onSecondaryContainerTone)),
        tertiaryContainer = Color(tertiaryPalette.tone(tertiaryContainerTone)),
        onTertiaryContainer = Color(tertiaryPalette.tone(onTertiaryContainerTone)),
    )
}

private fun withImageAccent(
    baseScheme: ColorScheme,
    slots: ImageAccentSlots,
): ColorScheme =
    baseScheme.copy(
        primary = slots.primary,
        onPrimary = slots.onPrimary,
        secondary = slots.secondary,
        onSecondary = slots.onSecondary,
        tertiary = slots.tertiary,
        onTertiary = slots.onTertiary,
        primaryContainer = slots.primaryContainer,
        onPrimaryContainer = slots.onPrimaryContainer,
        secondaryContainer = slots.secondaryContainer,
        onSecondaryContainer = slots.onSecondaryContainer,
        tertiaryContainer = slots.tertiaryContainer,
        onTertiaryContainer = slots.onTertiaryContainer,
    )

// ============================================================================
// Theme Composable
// ============================================================================

/**
 * QuickSearch application theme composable.
 *
 * Provides Material 3 color schemes and app-specific semantic color tokens.
 * When image backgrounds are active and accent mode is From Wallpaper, accents are derived
 * directly from the current wallpaper/custom image. Custom mode uses a user-picked color.
 *
 * @param content The composable content to be themed.
 */
@Composable
fun QuickSearchTheme(
    fontScaleMultiplier: Float = 1f,
    useSystemFont: Boolean = false,
    appTheme: com.tk.quicksearch.search.core.AppTheme = com.tk.quicksearch.search.core.AppTheme.MONOCHROME,
    appThemeMode: com.tk.quicksearch.search.core.AppThemeMode = com.tk.quicksearch.search.core.AppThemeMode.SYSTEM,
    backgroundSource: com.tk.quicksearch.search.core.BackgroundSource = com.tk.quicksearch.search.core.BackgroundSource.THEME,
    customImageUri: String? = null,
    accentColorMode: AccentColorMode = AccentColorMode.FROM_WALLPAPER,
    customAccentColorArgb: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
    deviceThemeEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseDensity = LocalDensity.current
    val appDensity =
        remember(baseDensity, fontScaleMultiplier) {
            Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * fontScaleMultiplier,
            )
        }
    val isSystemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme =
        when (appThemeMode) {
            com.tk.quicksearch.search.core.AppThemeMode.LIGHT -> false
            com.tk.quicksearch.search.core.AppThemeMode.DARK -> true
            com.tk.quicksearch.search.core.AppThemeMode.SYSTEM -> isSystemDarkTheme
        }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val useDeviceDynamicColors = deviceThemeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useWallpaperDerivedAccent =
        !useDeviceDynamicColors &&
            accentColorMode == AccentColorMode.FROM_WALLPAPER &&
            backgroundSource != BackgroundSource.THEME
    val useCustomAccent =
        !useDeviceDynamicColors &&
            accentColorMode == AccentColorMode.CUSTOM &&
            backgroundSource != BackgroundSource.THEME

    var wallpaperChangeVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(context, backgroundSource, accentColorMode) {
        if (!useWallpaperDerivedAccent || backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
            onDispose { }
        } else {
            val appContext = context.applicationContext
            @Suppress("DEPRECATION")
            val wallpaperChangedAction = Intent.ACTION_WALLPAPER_CHANGED
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        if (intent?.action != wallpaperChangedAction) return
                        WallpaperUtils.invalidateWallpaperCache()
                        wallpaperChangeVersion++
                    }
                }
            val filter = IntentFilter(wallpaperChangedAction)
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                appContext.unregisterReceiver(receiver)
            }
        }
    }

    DisposableEffect(lifecycleOwner, backgroundSource, accentColorMode) {
        if (!useWallpaperDerivedAccent || backgroundSource != BackgroundSource.SYSTEM_WALLPAPER) {
            onDispose { }
        } else {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        wallpaperChangeVersion++
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    val imageAppearance by
        produceState<ImageAppearance?>(
            null,
            backgroundSource,
            customImageUri,
            accentColorMode,
            wallpaperChangeVersion,
        ) {
            value =
                if (useWallpaperDerivedAccent) {
                    WallpaperUtils.getBackgroundAppearance(
                        context = context,
                        backgroundSource = backgroundSource,
                        customImageUri = customImageUri,
                    )
                } else {
                    null
                }
        }

    val accentBaseColorArgb = if (useCustomAccent) customAccentColorArgb else imageAppearance?.accentColorArgb
    val imageAccentSlots =
        remember(accentBaseColorArgb, useDarkTheme) {
            accentBaseColorArgb?.let { resolveImageAccentSlots(baseColorArgb = it, useDarkTheme = useDarkTheme) }
        }

    // Remembered so a recomposition with unchanged inputs (every resume bumps the wallpaper
    // version) keeps the same ColorScheme instance. A new instance recomposes every color reader in
    // the app, which lands on the first frame of the keyboard animation when the app opens.
    val configuration = LocalConfiguration.current
    val colorScheme =
        remember(
            useDeviceDynamicColors,
            useDarkTheme,
            imageAccentSlots,
            appTheme,
            configuration,
        ) {
            if (useDeviceDynamicColors) {
                val dynamicScheme =
                    if (useDarkTheme) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                val baseTextScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
                dynamicScheme.copy(
                    onBackground = baseTextScheme.onBackground,
                    onSurface = baseTextScheme.onSurface,
                    onSurfaceVariant = baseTextScheme.onSurfaceVariant,
                )
            } else if (imageAccentSlots != null) {
                if (useDarkTheme) {
                    withImageAccent(DarkColorScheme, imageAccentSlots)
                } else {
                    withImageAccent(LightColorScheme, imageAccentSlots)
                }
            } else {
                val accent = accentForTheme(appTheme)
                if (useDarkTheme) {
                    DarkColorScheme.copy(
                        primary = accent.darkPrimary,
                        onPrimary = accent.darkOnPrimary,
                        primaryContainer = accent.darkPrimaryContainer,
                        onPrimaryContainer = accent.darkOnPrimaryContainer,
                        secondaryContainer = accent.darkSecondaryContainer,
                        onSecondaryContainer = accent.darkOnSecondaryContainer,
                    )
                } else {
                    LightColorScheme.copy(
                        primary = accent.lightPrimary,
                        onPrimary = accent.lightOnPrimary,
                        primaryContainer = accent.lightPrimaryContainer,
                        onPrimaryContainer = accent.lightOnPrimaryContainer,
                        secondaryContainer = accent.lightSecondaryContainer,
                        onSecondaryContainer = accent.lightOnSecondaryContainer,
                    )
                }
            }
        }
    val appPalette =
        if (useDarkTheme) {
            DarkQuickSearchAppColorPalette
        } else {
            LightQuickSearchAppColorPalette
        }

    CompositionLocalProvider(
        LocalDensity provides appDensity,
        LocalQuickSearchAppColorPalette provides appPalette,
        LocalAppIsDarkTheme provides useDarkTheme,
        LocalAppTheme provides appTheme,
        LocalDeviceDynamicColorsActive provides useDeviceDynamicColors,
        LocalWallpaperDynamicAccentActive provides (imageAccentSlots != null),
        LocalIsSystemWallpaperActive provides (backgroundSource == BackgroundSource.SYSTEM_WALLPAPER),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = quickSearchTypography(useSystemFont),
            shapes = AppShapes.material,
            content = content,
        )
    }
}

/**
 * Content drawn straight on a wallpaper or image (compact layout has no cards behind it) takes the
 * text and surface colours of the theme that matches [homeBackgroundIsDark] (the Home text colour
 * setting, else the detected wallpaper), so light-theme text never
 * lands on a dark wallpaper (or the reverse).
 */
@Composable
fun HomeTextContrastTheme(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val wantDark = homeBackgroundIsDark()
    if (!enabled || wantDark == null || wantDark == LocalAppIsDarkTheme.current) {
        content()
        return
    }
    val base = if (wantDark) DarkColorScheme else LightColorScheme
    val current = MaterialTheme.colorScheme
    // A light theme's primary container is the pale tint of its accent (and a dark theme's the deep
    // one), which is about where the other theme puts its primary, so the two roles trade places.
    val colorScheme =
        current.copy(
            primary = current.primaryContainer,
            onPrimary = current.onPrimaryContainer,
            primaryContainer = current.primary,
            onPrimaryContainer = current.onPrimary,
            background = base.background,
            onBackground = base.onBackground,
            surface = base.surface,
            onSurface = base.onSurface,
            surfaceVariant = base.surfaceVariant,
            onSurfaceVariant = base.onSurfaceVariant,
            surfaceTint = base.surfaceTint,
            inverseSurface = base.inverseSurface,
            inverseOnSurface = base.inverseOnSurface,
            outline = base.outline,
            outlineVariant = base.outlineVariant,
            scrim = base.scrim,
            surfaceBright = base.surfaceBright,
            surfaceDim = base.surfaceDim,
            surfaceContainer = base.surfaceContainer,
            surfaceContainerHigh = base.surfaceContainerHigh,
            surfaceContainerHighest = base.surfaceContainerHighest,
            surfaceContainerLow = base.surfaceContainerLow,
            surfaceContainerLowest = base.surfaceContainerLowest,
        )
    CompositionLocalProvider(
        LocalAppIsDarkTheme provides wantDark,
        LocalQuickSearchAppColorPalette provides
            if (wantDark) DarkQuickSearchAppColorPalette else LightQuickSearchAppColorPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
